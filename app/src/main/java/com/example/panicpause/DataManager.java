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
import com.google.firebase.firestore.SetOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";

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

    public static class PhotoData{
        public final String imgUrl;
        public final String word;
        public final List<String> tags;

        public PhotoData(String imgUrl, String word, List<String> tags) {
            this.imgUrl = imgUrl;
            this.word = word;
            this.tags = tags;
        }
    }

    // Имя файла для хранения истории
    private static final String EXERCISE_HISTORY_FILE = "exercise_history.json";

    //Представляет один завершённый набор упражнений (сессию).
    //Хранит дату/время и список фото, использованных в сессии.
    public static class ExerciseSession {
        public final long timestamp;          // Время завершения сессии (миллисекунды)
        public final List<PhotoData> photos;  // Список фото с тегами

        public ExerciseSession(long timestamp, List<PhotoData> photos) {
            this.timestamp = timestamp;
            // ВАЖНО: делаем копию списка, чтобы избежать изменений извне
            this.photos = new ArrayList<>(photos);
        }
        // Почему копия списка?
        // Если сохранить ссылку на currentSessionPhotos,
        // последующие изменения в этом списке
        // (например, при новой сессии)
        // повредят сохранённую историю.
        // Копия гарантирует целостность данных.


        //Форматирует дату для отображения пользователю (например: "30 января 2026, 14:30")
        public String getFormattedDate(Context context) {
            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("d MMMM yyyy, HH:mm",
                    java.util.Locale.getDefault());
            return formatter.format(new java.util.Date(timestamp));
        }
    }

    // Ключ для временной метки истории
    private static final String KEY_EXERCISE_HISTORY_LAST_MODIFIED = "exercise_history_last_modified";



    //Загружает список изображений из локального файла images.json
    public List<PhotoData> getLocalImagesList(){
        List<PhotoData> photos = new ArrayList<>();
        File imagesFile=new File(context.getFilesDir(), "content/images.json");

        if(!imagesFile.exists()){
            Log.w(TAG, "Local images.json not found");
            return photos;
        }
        try (FileInputStream fis = new FileInputStream(imagesFile);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, length);
            }
            String json = bos.toString("UTF-8");
            JSONArray array = new JSONArray(json);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String imgUrl = obj.optString("img_url", null);
                String word = obj.optString("word", null);
                JSONArray tagsArray = obj.optJSONArray("tags");
                List<String> tags = new ArrayList<>();
                if (tagsArray != null) {
                    for (int j = 0; j < tagsArray.length(); j++) {
                        tags.add(tagsArray.getString(j));
                    }
                }
                if (imgUrl != null && word != null) {
                    photos.add(new PhotoData(imgUrl, word, tags));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading images.json", e);
        }
        return photos;
    }

    //Загружает список триггеров из локального файла tags.json
    public List<TriggerItem> getLocalTagsList(){
        List<TriggerItem> tags = new ArrayList<>();
        File tagsFile = new File(context.getFilesDir(), "content/tags.json");

        if (!tagsFile.exists()) {
            Log.w(TAG, "Local tags.json not found");
            return tags;
        }

        try (FileInputStream fis = new FileInputStream(tagsFile);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, length);
            }

            String json = bos.toString("UTF-8");
            JSONArray array = new JSONArray(json);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String imgTag = obj.optString("img_tag", null);
                Boolean isParent = obj.optBoolean("is_parent", false);
                String parentTag = obj.optString("parent_tag", "");
                //String strRes = obj.optString("str_res", "");
                String nameRus = obj.optString("name_rus", "");

                if (imgTag != null) {
                    tags.add(new TriggerItem(imgTag, isParent, parentTag, nameRus));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading tags.json", e);
        }
        return tags;
    }

    public void saveTriggers(List<String> triggers) {
        saveUserSetting(KEY_TRIGGERS, triggers);
    }

    public void saveFaves(List<String> faves){
        saveUserSetting(KEY_FAVES, faves);
    }

    public int getGroundPhotoExAmount() {
        return prefs.getInt(KEY_GROUND_PHOTO_AMOUNT, 2);
    }

    public boolean getUseMath() {
        return prefs.getBoolean(KEY_USE_MATH, true);
    }

    public boolean getUseSearchObjectsColor() {
        return prefs.getBoolean(KEY_USE_COLOR_SEARCH, true);
    }

    public int getBreathRepeatAmount() {
        return prefs.getInt(KEY_BREATH_REPEAT, 1);
    }

    public boolean getUseFavesOnly() {
        return prefs.getBoolean(KEY_USE_FAVES_ONLY, false);
    }

    public boolean getGroundOnLaunch() {
        return prefs.getBoolean(KEY_GROUND_ON_LAUNCH, false);
    }

    public List<String> getTriggers() {
        try {
            String json = prefs.getString(KEY_TRIGGERS, "[]");
            JSONArray array = new JSONArray(json);
            List<String> list = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<String> getFaves() {
        try {
            String json = prefs.getString(KEY_FAVES, "[]");
            JSONArray array = new JSONArray(json);
            List<String> list = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public boolean isOnboardingCompleted() {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public void markOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply();
    }

    // 1. Инициализация контента

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

            // ПРОВЕРКА: убедиться, что файл содержит валидный UTF-8
            if (assetPath.equals("tags.json") || assetPath.equals("images.json")) {
                try (FileInputStream fis = new FileInputStream(destFile)) {
                    byte[] bom = new byte[3];
                    if (fis.read(bom) == 3 && bom[0] == (byte)0xEF && bom[1] == (byte)0xBB && bom[2] == (byte)0xBF) {
                        Log.d(TAG, "Файл " + assetPath + " содержит UTF-8 BOM");
                    }
                }
            }
        }
        /*try (InputStream is = context.getAssets().open(assetPath);
             OutputStream os = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }*/
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


    // === 2. Проверка и обновление контента ===

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
                    fos.write(array.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
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

    //=== 3. Работа с пользователем и настройками ===

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
     * Логика синхронизации:
     * - Новый пользователь (нет данных в облаке): сохраняем локальные данные в облако
     * - Существующий пользователь + гость: ВСЕГДА загружаем из облака (безопасность)
     * - Существующий пользователь + не гость (тот же пользователь): сравниваем временные метки для синхронизации
     * */
    public void handleUserLogin(Runnable onSyncComplete) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            onSyncComplete.run();
            return;
        }

        String firebaseUid = firebaseUser.getUid();
        boolean wasGuest = isGuest();
        long localLastModified = prefs.getLong(KEY_LAST_MODIFIED_LOCAL, System.currentTimeMillis());

        // Загружаем данные из Firestore
        db.collection("users").document(firebaseUid).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        // СЛУЧАЙ 1: НОВЫЙ ПОЛЬЗОВАТЕЛЬ (регистрация)
                        // Нет данных в облаке — сохраняем текущие локальные настройки (гостевые) в Firestore
                        saveLocalUserSettingsToFirestore(firebaseUid, localLastModified, () -> {
                            // Синхронизируем историю упражнений
                            syncExerciseHistoryToFirestore();

                            // Обновляем локальный статус
                            prefs.edit()
                                    .putString(KEY_USER_ID, firebaseUid)
                                    .putBoolean(KEY_IS_GUEST, false)
                                    .apply();

                            onSyncComplete.run();
                        });
                    } else {
                        // СЛУЧАЙ 2: СУЩЕСТВУЮЩИЙ ПОЛЬЗОВАТЕЛЬ (есть данные в облаке)
                        long remoteLastModified = snapshot.getLong("last_modified");

                        if (wasGuest) {
                            // Подслучай 2a: ГОСТЬ входит в существующий аккаунт

                            // очищаем гостевые данные
                            clearLocalUserData();

                            // безопасно загружаем данные из облака
                            loadUserSettingsFromSnapshot(snapshot);

                            // Загружаем историю упражнений из облака
                            syncExerciseHistoryFromFirestore(() -> {
                                prefs.edit()
                                        .putString(KEY_USER_ID, firebaseUid)
                                        .putBoolean(KEY_IS_GUEST, false)
                                        .putLong(KEY_LAST_MODIFIED_LOCAL, remoteLastModified)
                                        .apply();
                                onSyncComplete.run();
                            });

                        } else {
                            // Подслучай 2b: ПОЛЬЗОВАТЕЛЬ (не гость) снова входит в свой аккаунт
                            // Применяем сравнение временных меток для корректной multi-device синхронизации
                            if (localLastModified >= remoteLastModified) {
                                // Локальные данные новее или равны — сохраняем их в облако
                                saveLocalUserSettingsToFirestore(firebaseUid, localLastModified, () -> {
                                    // Синхронизируем историю упражнений
                                    syncExerciseHistoryToFirestore();
                                    prefs.edit()
                                            .putString(KEY_USER_ID, firebaseUid)
                                            .putBoolean(KEY_IS_GUEST, false)
                                            .apply();
                                    onSyncComplete.run();
                                });
                            } else {
                                // Облачные данные новее — загружаем их локально
                                loadUserSettingsFromSnapshot(snapshot);
                                // Загружаем историю упражнений из облака
                                syncExerciseHistoryFromFirestore(() -> {
                                    prefs.edit()
                                            .putString(KEY_USER_ID, firebaseUid)
                                            .putBoolean(KEY_IS_GUEST, false)
                                            .putLong(KEY_LAST_MODIFIED_LOCAL, remoteLastModified)
                                            .apply();
                                    onSyncComplete.run();
                                });
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Не удалось загрузить данные пользователя", e);
                    // При ошибке сохраняем текущие локальные данные, но меняем статус на авторизованный
                    // (безопасный отказоустойчивый режим)
                    prefs.edit()
                            .putString(KEY_USER_ID, firebaseUid)
                            .putBoolean(KEY_IS_GUEST, false)
                            .apply();
                    onSyncComplete.run();
                });

        /*FirebaseUser firebaseUser = mAuth.getCurrentUser();
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

                        if (localLastModified >= remoteLastModified && wasGuest) {
                            // Локальные новее — загружаем их в облако
                            saveLocalUserSettingsToFirestore(firebaseUid, localLastModified, () -> {
                                prefs.edit()
                                        .putString(KEY_USER_ID, firebaseUid)
                                        .putBoolean(KEY_IS_GUEST, false)
                                        .apply();

                                // Также синхронизируем историю
                                syncExerciseHistoryToFirestore();

                                onSyncComplete.run();
                            });
                        } else {
                            // Облачные новее — загружаем их локально
                            loadUserSettingsFromSnapshot(snapshot);
                            // Загружаем историю из облака
                            syncExerciseHistoryFromFirestore(() -> {
                                prefs.edit()
                                        .putString(KEY_USER_ID, firebaseUid)
                                        .putBoolean(KEY_IS_GUEST, false)
                                        .putLong(KEY_LAST_MODIFIED_LOCAL, remoteLastModified)
                                        .apply();
                                onSyncComplete.run();
                            });
                        }
                    } else {
                        // Нет данных в облаке — сохраняем локальные
                        saveLocalUserSettingsToFirestore(firebaseUid, localLastModified, () -> {
                            prefs.edit()
                                    .putString(KEY_USER_ID, firebaseUid)
                                    .putBoolean(KEY_IS_GUEST, false)
                                    .apply();

                            // Также синхронизируем историю
                            syncExerciseHistoryToFirestore();

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
                });*/
    }

    /**
     * Обрабатывает выход из аккаунта.
     * Данные остаются локально, создаётся новый гостевой ID.
     */
    public void handleUserLogout() {
        // Полная очистка данных перед созданием нового гостя
        clearLocalUserData();

        // Создаем нового гостя
        String guestId = "guest_" + System.currentTimeMillis();
        prefs.edit()
                .putString(KEY_USER_ID, guestId)
                .putBoolean(KEY_IS_GUEST, true)
                .apply();
    }

    /**
     * Немедленно очищает ВСЕ пользовательские данные (для нового гостя или при входе в чужой аккаунт).
     * Выполняется синхронно, чтобы UI не показывал старые данные.
     */
    private void clearLocalUserData() {
        SharedPreferences.Editor editor = prefs.edit();

        // Сбрасываем настройки к значениям по умолчанию
        editor.putString(KEY_TRIGGERS, "[]");
        editor.putString(KEY_FAVES, "[]");
        editor.putInt(KEY_BREATH_REPEAT, 1);
        editor.putBoolean(KEY_USE_MATH, true);
        editor.putBoolean(KEY_USE_COLOR_SEARCH, true);
        editor.putInt(KEY_GROUND_PHOTO_AMOUNT, 2);
        editor.putBoolean(KEY_GROUND_ON_LAUNCH, false);
        editor.putBoolean(KEY_USE_FAVES_ONLY, false);
        editor.putLong(KEY_LAST_MODIFIED_LOCAL, System.currentTimeMillis());

        // Сбрасываем историю упражнений
        saveExerciseHistory(new ArrayList<>()); // Сохраняем пустой список СРАЗУ
        editor.putLong(KEY_EXERCISE_HISTORY_LAST_MODIFIED, 0);

        editor.apply();
    }


    // === 4. Сохранение и загрузка настроек ===

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
        if (!isNetworkAvailable()){
            return;
        }

        String userId = getUserId();
        if (isGuest())
            return;

        //JSONObject data = new JSONObject();
        Map<String, Object> data = new HashMap<>();
        try {
            data.put("email", prefs.getString("email", ""));
            data.put("triggers", convertJsonStringToList(prefs.getString(KEY_TRIGGERS, "[]")));
            data.put("faves", convertJsonStringToList(prefs.getString(KEY_FAVES, "[]")));
            data.put("breath_repeat_amount", prefs.getInt(KEY_BREATH_REPEAT, 1));
            data.put("use_math", prefs.getBoolean(KEY_USE_MATH, true));
            data.put("use_search_objects_color", prefs.getBoolean(KEY_USE_COLOR_SEARCH, true));
            data.put("ground_photo_ex_amount", prefs.getInt(KEY_GROUND_PHOTO_AMOUNT, 2));
            data.put("ground_on_launch", prefs.getBoolean(KEY_GROUND_ON_LAUNCH, false));
            data.put("use_faves_only", prefs.getBoolean(KEY_USE_FAVES_ONLY, false));
            data.put("last_modified", lastModified);

            //db.collection("users").document(userId).set(data);
            // set - полная перезапись документа (плохо)
            db.collection("users").document(userId)
                    .set(data, SetOptions.merge())
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Ошибка синхронизации настроек", e)
                    );
        } catch (Exception e) {
            Log.e(TAG, "Ошибка сериализации настроек", e);
        }
    }

    private void saveLocalUserSettingsToFirestore(String userId, long lastModified, Runnable onComplete) {
        if (!isNetworkAvailable()) {
            onComplete.run();
            return;
        }

        //JSONObject data = new JSONObject();
        Map<String, Object> data = new HashMap<>();
        try {
            // Собираем все текущие настройки
            data.put("email", prefs.getString("email", ""));
            data.put("triggers", convertJsonStringToList(prefs.getString(KEY_TRIGGERS, "[]")));
            data.put("faves", convertJsonStringToList(prefs.getString(KEY_FAVES, "[]")));
            data.put("breath_repeat_amount", prefs.getInt(KEY_BREATH_REPEAT, 1));
            data.put("use_math", prefs.getBoolean(KEY_USE_MATH, true));
            data.put("use_search_objects_color", prefs.getBoolean(KEY_USE_COLOR_SEARCH, true));
            data.put("ground_photo_ex_amount", prefs.getInt(KEY_GROUND_PHOTO_AMOUNT, 2));
            data.put("ground_on_launch", prefs.getBoolean(KEY_GROUND_ON_LAUNCH, false));
            data.put("use_faves_only", prefs.getBoolean(KEY_USE_FAVES_ONLY, false));
            data.put("last_modified", lastModified);

            db.collection("users").document(userId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(unused -> onComplete.run())
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Не удалось сохранить настройки в Firestore", e);
                        onComplete.run();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при сохранении настроек", e);
            onComplete.run();
        }
    }

    private void loadUserSettingsFromSnapshot(DocumentSnapshot snapshot) {
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("email", snapshot.getString("email"));

        @SuppressWarnings("unchecked")
        List<String> triggers = (List<String>) snapshot.get("triggers");
        editor.putString(KEY_TRIGGERS, triggers != null ? new JSONArray(triggers).toString() : "[]");

        @SuppressWarnings("unchecked")
        List<String> faves = (List<String>) snapshot.get("faves");
        editor.putString(KEY_FAVES, faves != null ? new JSONArray(faves).toString() : "[]");

        editor.putInt(KEY_BREATH_REPEAT, snapshot.getLong("breath_repeat_amount").intValue());
        editor.putBoolean(KEY_USE_MATH, snapshot.getBoolean("use_math"));
        editor.putBoolean(KEY_USE_COLOR_SEARCH, snapshot.getBoolean("use_search_objects_color"));
        editor.putInt(KEY_GROUND_PHOTO_AMOUNT, snapshot.getLong("ground_photo_ex_amount").intValue());
        editor.putBoolean(KEY_GROUND_ON_LAUNCH, snapshot.getBoolean("ground_on_launch"));
        editor.putBoolean(KEY_USE_FAVES_ONLY, snapshot.getBoolean("use_faves_only"));

        editor.apply();
    }


    // ИСТОРИЯ

    // Загружает историю сессий из локального файла.
    // Возвращает список последних сессий (максимум 3) или пустой список при ошибке/отсутствии файла.
    public List<ExerciseSession> loadExerciseHistory() {
        List<ExerciseSession> sessions = new ArrayList<>();
        File historyFile = new File(context.getFilesDir(), EXERCISE_HISTORY_FILE);

        if (!historyFile.exists()) {
            // Файл ещё не создан — возвращаем пустой список
            return sessions;
        }

        try (FileInputStream fis = new FileInputStream(historyFile);
             java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, length);
            }

            String json = bos.toString("UTF-8");
            JSONArray jsonArray = new JSONArray(json);

            // Читаем максимум 3 сессии (защита от повреждённых данных)
            int limit = Math.min(jsonArray.length(), 3);
            for (int i = 0; i < limit; i++) {
                JSONObject sessionObj = jsonArray.getJSONObject(i);
                long timestamp = sessionObj.getLong("timestamp");

                // Загружаем фото из сессии
                JSONArray photosArray = sessionObj.getJSONArray("photos");
                List<PhotoData> photos = new ArrayList<>();
                for (int j = 0; j < photosArray.length(); j++) {
                    JSONObject photoObj = photosArray.getJSONObject(j);
                    String imgUrl = photoObj.getString("img_url");
                    String word = photoObj.getString("word");
                    JSONArray tagsArray = photoObj.getJSONArray("tags");
                    List<String> tags = new ArrayList<>();
                    for (int k = 0; k < tagsArray.length(); k++) {
                        tags.add(tagsArray.getString(k));
                    }
                    photos.add(new PhotoData(imgUrl, word, tags));
                }

                sessions.add(new ExerciseSession(timestamp, photos));
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки истории упражнений", e);
            // При ошибке возвращаем пустой список — безопаснее, чем повреждённые данные
            return new ArrayList<>();
        }

        return sessions;
    }

    // Сохраняет список сессий в локальный файл.
    // Автоматически обрезает список до 3 элементов (самые свежие в начале).
    public void saveExerciseHistory(List<ExerciseSession> sessions) {
        // Обрезаем до 3 самых свежих сессий (они должны быть в начале списка)
        if (sessions.size() > 3) {
            sessions = sessions.subList(0, 3);
        }

        JSONArray jsonArray = new JSONArray();
        for (ExerciseSession session : sessions) {
            try {
                JSONObject sessionObj = new JSONObject();
                sessionObj.put("timestamp", session.timestamp);

                // Сохраняем фото
                JSONArray photosArray = new JSONArray();
                for (PhotoData photo : session.photos) {
                    JSONObject photoObj = new JSONObject();
                    photoObj.put("img_url", photo.imgUrl);
                    photoObj.put("word", photo.word);
                    JSONArray tagsArray = new JSONArray();
                    for (String tag : photo.tags) {
                        tagsArray.put(tag);
                    }
                    photoObj.put("tags", tagsArray);
                    photosArray.put(photoObj);
                }
                sessionObj.put("photos", photosArray);
                jsonArray.put(sessionObj);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка сериализации сессии", e);
            }
        }

        // Записываем в файл
        File historyFile = new File(context.getFilesDir(), EXERCISE_HISTORY_FILE);
        try (FileOutputStream fos = new FileOutputStream(historyFile)) {
            fos.write(jsonArray.toString(2).getBytes("UTF-8")); // toString(2) для читаемого формата
            Log.d(TAG, "История упражнений сохранена: " + sessions.size() + " сессий");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка сохранения истории упражнений", e);
        }
    }

    // Добавляет новую сессию в историю и сохраняет обновлённый список.
    // Автоматически ограничивает историю 3 последними сессиями.
    public void addExerciseSession(ExerciseSession newSession) {
        // Загружаем существующую историю
        List<ExerciseSession> sessions = loadExerciseHistory();

        // Добавляем новую сессию В НАЧАЛО списка (самые свежие — первые)
        sessions.add(0, newSession);

        // Сохраняем обновлённый список
        saveExerciseHistory(sessions);
    }

    //Добавляет новую сессию в историю и синхронизирует с облаком
    public void addExerciseSessionAndSync(DataManager.ExerciseSession newSession) {
        // Добавляем сессию в локальную историю
        addExerciseSession(newSession);

        // Синхронизируем с облаком (если пользователь не гость и есть интернет)
        if (!isGuest()) {
            syncExerciseHistoryToFirestore();
        }
    }

    // Синхронизирует историю упражнений с Firestore
    private void syncExerciseHistoryToFirestore() {
        if (!isNetworkAvailable()) {
            return;
        }

        String userId = getUserId();
        if (isGuest()) {
            return;
        }

        // Загружаем локальную историю
        List<ExerciseSession> localHistory = loadExerciseHistory();
        if (localHistory.isEmpty()) {
            return;
        }

        // Подготавливаем данные для отправки
        List<Map<String, Object>> historyData = new ArrayList<>();
        for (ExerciseSession session : localHistory) {
            Map<String, Object> sessionMap = new HashMap<>();
            sessionMap.put("timestamp", session.timestamp);

            // Преобразуем фото в формат для Firestore
            List<Map<String, Object>> photosData = new ArrayList<>();
            for (PhotoData photo : session.photos) {
                Map<String, Object> photoMap = new HashMap<>();
                photoMap.put("img_url", photo.imgUrl);
                photoMap.put("word", photo.word);
                photoMap.put("tags", photo.tags);
                photosData.add(photoMap);
            }
            sessionMap.put("photos", photosData);
            historyData.add(sessionMap);
        }

        // Сохраняем в Firestore
        Map<String, Object> data = new HashMap<>();
        data.put("exercise_history", historyData);
        data.put("exercise_history_last_modified", System.currentTimeMillis());

        db.collection("users").document(userId)
                .update(data)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "История упражнений синхронизирована с облаком");
                    // Обновляем локальную временную метку
                    prefs.edit()
                            .putLong(KEY_EXERCISE_HISTORY_LAST_MODIFIED, System.currentTimeMillis())
                            .apply();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка синхронизации истории упражнений", e);
                });
    }

    //Загружает историю упражнений из облака (если она новее локальной)
    public void syncExerciseHistoryFromFirestore(Runnable onComplete) {
        if (!isNetworkAvailable() || isGuest()) {
            onComplete.run();
            return;
        }

        String userId = getUserId();
        long localLastModified = prefs.getLong(KEY_EXERCISE_HISTORY_LAST_MODIFIED, 0);

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists() && snapshot.contains("exercise_history_last_modified")) {
                        long remoteLastModified = snapshot.getLong("exercise_history_last_modified");

                        // Если облачная версия новее — загружаем её
                        if (remoteLastModified > localLastModified) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> remoteHistory =
                                    (List<Map<String, Object>>) snapshot.get("exercise_history");

                            if (remoteHistory != null && !remoteHistory.isEmpty()) {
                                // Преобразуем данные из Firestore в локальный формат
                                List<ExerciseSession> sessions = new ArrayList<>();
                                for (Map<String, Object> sessionMap : remoteHistory) {
                                    long timestamp = (long) sessionMap.get("timestamp");

                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> photosData =
                                            (List<Map<String, Object>>) sessionMap.get("photos");

                                    List<PhotoData> photos = new ArrayList<>();
                                    if (photosData != null) {
                                        for (Map<String, Object> photoMap : photosData) {
                                            String imgUrl = (String) photoMap.get("img_url");
                                            String word = (String) photoMap.get("word");
                                            @SuppressWarnings("unchecked")
                                            List<String> tags = (List<String>) photoMap.get("tags");

                                            if (imgUrl != null && word != null && tags != null) {
                                                photos.add(new PhotoData(imgUrl, word, tags));
                                            }
                                        }
                                    }

                                    sessions.add(new ExerciseSession(timestamp, photos));
                                }

                                // Сохраняем локально
                                saveExerciseHistory(sessions);
                                prefs.edit()
                                        .putLong(KEY_EXERCISE_HISTORY_LAST_MODIFIED, remoteLastModified)
                                        .apply();

                                Log.d(TAG, "История упражнений загружена из облака");
                            }
                        }
                    }
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Не удалось загрузить историю упражнений из облака", e);
                    onComplete.run();
                });
    }



    // === Вспомогательные методы ===

    public static String getFilenameFromUrl(String url) {
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
        return true; // полагаемся на onFailure Firebase
    }

    private List<String> convertJsonStringToList(String jsonString) {
        try {
            JSONArray array = new JSONArray(jsonString);
            List<String> list = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
            return list;
        } catch (Exception e) {
            return new ArrayList<>(); // возвращаем пустой список при ошибке
        }
    }

}
