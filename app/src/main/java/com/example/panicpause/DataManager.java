package com.example.panicpause;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * DataManager — центральный класс для:
 * - инициализации контента (tags, images, фото),
 * - управления пользовательскими настройками (гость / авторизованный),
 * - синхронизации с Firestore при наличии интернета.
 *
 * Работает offline-first: всё хранится локально, облако — опционально.
 */
public class DataManager {
    private static final String TAG = "DataManager";
    private static final String PREFS_NAME = "app_data";
    private static final String CONTENT_DIR = "content";
    private static final String PHOTOS_DIR = "photos";

    // Ключи для SharedPreferences
    private static final String KEY_CONTENT_READY = "content_ready";
    private static final String KEY_LOCAL_CONTENT_VERSION = "local_content_version";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_IS_GUEST = "is_guest";
    private static final String KEY_LAST_MODIFIED_LOCAL = "last_modified_local";
    //TODO add last_modified field to firestore users

    // Поля пользователя (сохраняются как строки/числа/булевы)
    private static final String KEY_TRIGGERS = "triggers";
    private static final String KEY_FAVES = "faves";
    private static final String KEY_BREATH_REPEAT = "breath_repeat_amount";
    private static final String KEY_USE_MATH = "use_math";
    private static final String KEY_USE_COLOR_SEARCH = "use_search_objects_color";
    private static final String KEY_GROUND_PHOTO_AMOUNT = "ground_photo_ex_amount";
    private static final String KEY_GROUND_ON_LAUNCH = "ground_on_launch";
    private static final String KEY_USE_FAVES_ONLY = "use_faves_only";

    private final Context context;
    private final SharedPreferences prefs;
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;
    private final File contentDir;
    private final File photosDir;
    private final OkHttpClient httpClient;

    public DataManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
        this.contentDir = new File(context.getFilesDir(), CONTENT_DIR);
        this.photosDir = new File(context.getFilesDir(), PHOTOS_DIR);
        this.httpClient = new OkHttpClient();

        // Создаём папки, если их нет
        contentDir.mkdirs();
        photosDir.mkdirs();
    }

    //region === 1. Инициализация контента ===

    /**
     * Запускает инициализацию контента.
     * Если контент ещё не скопирован — копирует из assets.
     * Если есть интернет — проверяет обновления.
     *
     * @param onReady вызывается, когда контент готов (всегда, даже без интернета)
     */
    public void initializeContent(Runnable onReady) {
        boolean contentReady = prefs.getBoolean(KEY_CONTENT_READY, false);

        if (!contentReady) {
            // Первый запуск: копируем стартовый набор из assets
            copyInitialContent(() -> {
                prefs.edit().putBoolean(KEY_CONTENT_READY, true).putInt(KEY_LOCAL_CONTENT_VERSION, 1).apply();
                // После копирования — проверяем обновления (если есть интернет)
                checkForContentUpdates(onReady);
            });
        } else {
            // Уже есть локальный контент — просто проверяем обновления
            checkForContentUpdates(onReady);
        }
    }

    private void copyInitialContent(Runnable onComplete) {
        new Thread(() -> {
            try {
                // Копируем tags.json
                copyAssetToFile("tags.json", new File(contentDir, "tags.json"));
                // Копируем images.json
                copyAssetToFile("images.json", new File(contentDir, "images.json"));
                // Копируем все фото из assets/photos
                copyAssetsPhotos();

                Log.d(TAG, "Начальный контент скопирован в " + contentDir.getAbsolutePath());
                onComplete.run();
            }
            catch (Exception e) {
                Log.e(TAG, "Ошибка копирования начального контента", e);
                onComplete.run(); // всё равно продолжаем
            }
        }).start();
    }

    private void copyAssetToFile(String assetPath, File destFile) throws IOException {
        try (InputStream is = context.getAssets().open(assetPath);
             OutputStream os = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    private void copyAssetsPhotos() throws IOException {
        String[] files = context.getAssets().list("photos");
        if (files != null) {
            for (String filename : files) {
                File dest = new File(photosDir, filename);
                copyAssetToFile("photos/" + filename, dest);
            }
        }
    }


    //region === 2. Проверка и обновление контента ===

    private void checkForContentUpdates(Runnable onReady) {
        if (!isNetworkAvailable()) {
            // Нет интернета — работаем с тем, что есть
            onReady.run();
            return;
        }

        // Загружаем версию из Firestore
        db.collection("meta").document("version")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists() && snapshot.contains("version")) {
                        long remoteVersion = snapshot.getLong("version");
                        int localVersion = prefs.getInt(KEY_LOCAL_CONTENT_VERSION, 1);

                        if (remoteVersion > localVersion) {
                            downloadAndApplyContentUpdate((int) remoteVersion, onReady);
                        } else {
                            onReady.run();
                        }
                    } else {
                        onReady.run(); // нет мета-данных — работаем локально
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Не удалось проверить обновления контента", e);
                    onReady.run(); // продолжаем без обновления
                });
    }

    private void downloadAndApplyContentUpdate(int newVersion, Runnable onReady) {
        // Скачиваем tags
        db.collection("tags_collection").get().addOnSuccessListener(tagsSnapshot -> {
            saveCollectionAsJson("tags.json", tagsSnapshot, () -> {
                // Скачиваем images
                db.collection("images").get().addOnSuccessListener(imagesSnapshot -> {
                    saveCollectionAsJson("images.json", imagesSnapshot, () -> {
                        // Скачиваем недостающие фото
                        downloadMissingPhotos(imagesSnapshot, () -> {
                            // Обновляем версию
                            prefs.edit().putInt(KEY_LOCAL_CONTENT_VERSION, newVersion).apply();
                            Log.d(TAG, "Контент обновлён до версии " + newVersion);
                            onReady.run();
                        });
                    });
                });
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Ошибка загрузки tags при обновлении", e);
            onReady.run(); // всё равно продолжаем
        });
    }

    private void saveCollectionAsJson(String filename, QuerySnapshot snapshot, Runnable onComplete) {
        new Thread(() -> {
            try {
                JSONArray array = new JSONArray();
                for (DocumentSnapshot doc : snapshot) {
                    array.put(doc.getData()); // сохраняем только данные, без ID
                }
                File file = new File(contentDir, filename);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(array.toString(2).getBytes());
                }
                onComplete.run();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка сохранения JSON: " + filename, e);
                onComplete.run();
            }
        }).start();
    }

    private void downloadMissingPhotos(QuerySnapshot imagesSnapshot, Runnable onComplete) {
        List<String> urlsToDownload = new ArrayList<>();
        for (DocumentSnapshot doc : imagesSnapshot) {
            String url = doc.getString("img_url");
            if (url != null) {
                String filename = getFilenameFromUrl(url);
                if (filename != null && !new File(photosDir, filename).exists()) {
                    urlsToDownload.add(url);
                }
            }
        }

        if (urlsToDownload.isEmpty()) {
            onComplete.run();
            return;
        }

        // Скачиваем все недостающие фото
        AtomicBoolean allDone = new AtomicBoolean(false);
        int total = urlsToDownload.size();
        int[] completed = {0};

        for (String url : urlsToDownload) {
            String filename = getFilenameFromUrl(url);
            File destFile = new File(photosDir, filename);

            Request request = new Request.Builder().url(url).build();
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.w(TAG, "Не удалось скачать фото: " + url, e);
                    synchronized (completed) {
                        completed[0]++;
                        if (completed[0] == total && !allDone.get()) {
                            allDone.set(true);
                            onComplete.run();
                        }
                    }
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try (InputStream is = response.body().byteStream();
                             FileOutputStream fos = new FileOutputStream(destFile)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                        } catch (IOException e) {
                            Log.w(TAG, "Ошибка сохранения фото: " + destFile, e);
                        }
                    }
                    synchronized (completed) {
                        completed[0]++;
                        if (completed[0] == total && !allDone.get()) {
                            allDone.set(true);
                            onComplete.run();
                        }
                    }
                }
            });
        }
    }

    //region === 3. Работа с пользователем и настройками ===

    /**
     * Проверяет, авторизован ли пользователь в Firebase.
     */
    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    /**
     * Возвращает true, если текущий пользователь — гость.
     */
    public boolean isGuest() {
        return prefs.getBoolean(KEY_IS_GUEST, true);
    }

    /**
     * Возвращает ID пользователя (guest_... или firebase uid).
     */
    public String getUserId() {
        String savedId = prefs.getString(KEY_USER_ID, null);
        if (savedId != null) return savedId;

        // Первый запуск — создаём гостя
        String guestId = "guest_" + System.currentTimeMillis();
        prefs.edit().putString(KEY_USER_ID, guestId).putBoolean(KEY_IS_GUEST, true).apply();
        return guestId;
    }

    /**
     * Обрабатывает вход пользователя в аккаунт.
     * Сравнивает локальные и облачные данные по времени и сохраняет самые свежие.
     */
    public void handleUserLogin(Runnable onSyncComplete) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            onSyncComplete.run();
            return;
        }

        String firebaseUid = firebaseUser.getUid();
        String currentLocalUserId = getUserId();
        boolean wasGuest = isGuest();
        long localLastModified = prefs.getLong(KEY_LAST_MODIFIED_LOCAL, System.currentTimeMillis());

        // Загружаем данные из Firestore
        db.collection("users").document(firebaseUid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // Есть данные в облаке — сравниваем время
                        long remoteLastModified = snapshot.getLong("last_modified");
                        //long remoteLastModified = snapshot.getLong("last_modified", localLastModified); //???

                        if (localLastModified >= remoteLastModified && wasGuest) {
                            // Локальные новее — загружаем их в облако
                            saveLocalUserSettingsToFirestore(firebaseUid, localLastModified, () -> {
                                prefs.edit()
                                        .putString(KEY_USER_ID, firebaseUid)
                                        .putBoolean(KEY_IS_GUEST, false)
                                        .apply();
                                onSyncComplete.run();
                            });
                        } else {
                            // Облачные новее — загружаем их локально
                            loadUserSettingsFromSnapshot(snapshot);
                            prefs.edit()
                                    .putString(KEY_USER_ID, firebaseUid)
                                    .putBoolean(KEY_IS_GUEST, false)
                                    .putLong(KEY_LAST_MODIFIED_LOCAL, remoteLastModified)
                                    .apply();
                            onSyncComplete.run();
                        }
                    } else {
                        // Нет данных в облаке — сохраняем локальные
                        saveLocalUserSettingsToFirestore(firebaseUid, localLastModified, () -> {
                            prefs.edit()
                                    .putString(KEY_USER_ID, firebaseUid)
                                    .putBoolean(KEY_IS_GUEST, false)
                                    .apply();
                            onSyncComplete.run();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Не удалось загрузить данные пользователя", e);
                    // В случае ошибки — остаёмся гостем, но меняем ID на firebaseUid
                    prefs.edit()
                            .putString(KEY_USER_ID, firebaseUid)
                            .putBoolean(KEY_IS_GUEST, false)
                            .apply();
                    onSyncComplete.run();
                });
    }

    /**
     * Обрабатывает выход из аккаунта.
     * Данные остаются локально, создаётся новый гостевой ID.
     */
    public void handleUserLogout() {
        String guestId = "guest_" + System.currentTimeMillis();
        prefs.edit()
                .putString(KEY_USER_ID, guestId)
                .putBoolean(KEY_IS_GUEST, true)
                .apply();
    }

    //region === 4. Сохранение и загрузка настроек ===

    /**
     * Сохраняет настройку и обновляет временную метку.
     */
    public void saveUserSetting(String key, Object value) {
        SharedPreferences.Editor editor = prefs.edit();
        long now = System.currentTimeMillis();

        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof List) {
            // Списки сохраняем как JSON-строки
            JSONArray array = new JSONArray((List<?>) value);
            editor.putString(key, array.toString());
        }

        editor.putLong(KEY_LAST_MODIFIED_LOCAL, now);
        editor.apply();

        // Если пользователь не гость — синхронизируем в фоне
        if (!isGuest()) {
            syncUserSettingsToFirestore(now);
        }
    }

    private void syncUserSettingsToFirestore(long lastModified) {
        if (!isNetworkAvailable()) return;

        String userId = getUserId();
        if (isGuest()) return;

        JSONObject data = new JSONObject();
        try {
            data.put("email", prefs.getString("email", ""));
            data.put("triggers", new JSONArray(prefs.getString(KEY_TRIGGERS, "[]")));
            data.put("faves", new JSONArray(prefs.getString(KEY_FAVES, "[]")));
            data.put("breath_repeat_amount", prefs.getInt(KEY_BREATH_REPEAT, 1));
            data.put("use_math", prefs.getBoolean(KEY_USE_MATH, true));
            data.put("use_search_objects_color", prefs.getBoolean(KEY_USE_COLOR_SEARCH, true));
            data.put("ground_photo_ex_amount", prefs.getInt(KEY_GROUND_PHOTO_AMOUNT, 2));
            data.put("ground_on_launch", prefs.getBoolean(KEY_GROUND_ON_LAUNCH, false));
            data.put("use_faves_only", prefs.getBoolean(KEY_USE_FAVES_ONLY, false));
            data.put("last_modified", lastModified);

            db.collection("users").document(userId).set(data);
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка сериализации настроек", e);
        }
    }

    private void saveLocalUserSettingsToFirestore(String userId, long lastModified, Runnable onComplete) {
        if (!isNetworkAvailable()) {
            onComplete.run();
            return;
        }

        JSONObject data = new JSONObject();
        try {
            // Собираем все текущие настройки
            data.put("email", prefs.getString("email", ""));
            data.put("triggers", new JSONArray(prefs.getString(KEY_TRIGGERS, "[]")));
            data.put("faves", new JSONArray(prefs.getString(KEY_FAVES, "[]")));
            data.put("breath_repeat_amount", prefs.getInt(KEY_BREATH_REPEAT, 1));
            data.put("use_math", prefs.getBoolean(KEY_USE_MATH, true));
            data.put("use_search_objects_color", prefs.getBoolean(KEY_USE_COLOR_SEARCH, true));
            data.put("ground_photo_ex_amount", prefs.getInt(KEY_GROUND_PHOTO_AMOUNT, 2));
            data.put("ground_on_launch", prefs.getBoolean(KEY_GROUND_ON_LAUNCH, false));
            data.put("use_faves_only", prefs.getBoolean(KEY_USE_FAVES_ONLY, false));
            data.put("last_modified", lastModified);

            db.collection("users").document(userId).set(data)
                    .addOnSuccessListener(unused -> onComplete.run())
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Не удалось сохранить настройки в Firestore", e);
                        onComplete.run(); // всё равно завершаем
                    });
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при сохранении настроек", e);
            onComplete.run();
        }
    }

    private void loadUserSettingsFromSnapshot(DocumentSnapshot snapshot) {
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("email", snapshot.getString("email"));
        editor.putString(KEY_TRIGGERS, snapshot.getString("triggers"));
        editor.putString(KEY_FAVES, snapshot.getString("faves"));
        editor.putInt(KEY_BREATH_REPEAT, snapshot.getLong("breath_repeat_amount").intValue());
        editor.putBoolean(KEY_USE_MATH, snapshot.getBoolean("use_math"));
        editor.putBoolean(KEY_USE_COLOR_SEARCH, snapshot.getBoolean("use_search_objects_color"));
        editor.putInt(KEY_GROUND_PHOTO_AMOUNT, snapshot.getLong("ground_photo_ex_amount").intValue());
        editor.putBoolean(KEY_GROUND_ON_LAUNCH, snapshot.getBoolean("ground_on_launch"));
        editor.putBoolean(KEY_USE_FAVES_ONLY, snapshot.getBoolean("use_faves_only"));

        editor.apply();
    }

    //region === Вспомогательные методы ===

    private String getFilenameFromUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            return new File(Uri.parse(url).getPath()).getName();
        } catch (Exception e) {
            int lastSlash = url.lastIndexOf('/');
            return (lastSlash != -1) ? url.substring(lastSlash + 1) : url;
        }
    }

    private boolean isNetworkAvailable() {
        // Простая проверка (можно улучшить через ConnectivityManager)
        // Для MVP достаточно попытки Firestore — он сам обработает отсутствие сети
        return true; // полагаемся на onFailure Firebase
    }


}
