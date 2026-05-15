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
import org.json.JSONException;
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
 * Приложение работает offline-first: всё хранится локально, облако - опционально.
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
    private static final String KEY_APP_INFO_VIEWED = "app_info_viewed";

    // Поля пользователя
    private static final String KEY_TRIGGERS = "triggers";
    private static final String KEY_FAVES = "faves";
    private static final String KEY_BREATH_REPEAT = "breath_repeat_amount";
    private static final String KEY_USE_MATH = "use_math";
    private static final String KEY_USE_COLOR_SEARCH = "use_search_objects_color";
    private static final String KEY_GROUND_PHOTO_AMOUNT = "ground_photo_ex_amount";
    private static final String KEY_USE_FAVES_ONLY = "use_faves_only";

    // Имя файла для хранения истории
    private static final String EXERCISE_HISTORY_FILE = "exercise_history.json";
    // Ключ для временной метки истории
    private static final String KEY_EXERCISE_HISTORY_LAST_MODIFIED = "exercise_history_last_modified";

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
        // создать папки для контента и фото, если их нет
        contentDir.mkdirs();
        photosDir.mkdirs();
    }

    /**
     * PhotoData - класс для описания упражнения с фото.
     * imgUrl - ссылка на фото
     * word - слово, вставляемое в текст упражнения (напр. "котят")
     * tags - список тегов фото
     */
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

    /**
     * Объекты класса ExerciseSession представляют один завершённый набор упражнений (сессию)
     * Хранят дату/время и список фото, использованных в сессии
     */
    public static class ExerciseSession {
        public final long timestamp;          // время завершения сессии (миллисекунды)
        public final List<PhotoData> photos;  // список фото с тегами (пройденный набор упражнений)
        public ExerciseSession(long timestamp, List<PhotoData> photos) {
            this.timestamp = timestamp;
            this.photos = new ArrayList<>(photos); // копия списка (чтобы избежать изменений извне)
        }
        /*
        (если сохранить ссылку на currentSessionPhotos, а не копию,
        то последующие изменения в этом списке (напр., при новой сессии)
        повредят сохранённую историю.
        Копия гарантирует целостность данных)
        */

        /**
         * Метод getFormattedDate форматирует дату для отображения пользователю
         * (напр. "30 января 2026, 14:30")
         * @param context контекст временной метки
         * @return отформатированное время прохождения набора упражнений
         */
        public String getFormattedDate(Context context) {
            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("d MMMM yyyy, HH:mm",
                    java.util.Locale.getDefault());
            return formatter.format(new java.util.Date(timestamp));
        }
    }


    // ЗАГРУЗКА СПИСКОВ ИЗОБРАЖЕНИЙ И ТЕГОВ

    /**
     * Метод getLocalImagesList загружает список изображений из локального файла images.json
     * @return PhotoData-список всех записей о изображениях из локального файла
     */
    public List<PhotoData> getLocalImagesList(){
        List<PhotoData> photos = new ArrayList<>();
        File imagesFile=new File(context.getFilesDir(), "content/images.json");

        // если файл images.json уже есть готовый к работе, то попытаться достать список из него
        if (imagesFile.exists()) {
            try {
                String json = readFileToString(imagesFile);
                if (json != null && !json.isEmpty()) {
                    JSONArray array = new JSONArray(json);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = safeGetJSONObject(array, i);
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
                    Log.d(TAG, "Загружено " + photos.size() + " изображений из локального файла");
                    return photos;
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка парсинга локального images.json. Файл поврежден.", e);
                imagesFile.delete();
                Log.w(TAG, "Поврежденный файл images.json удален. Восстановление из Assets...");
            }
        }

        // если файла нет, попытаться достать список из папки assets
        Log.d(TAG, "Загрузка изображений из Assets");
        try {
            InputStream is = context.getAssets().open("images.json");
            String json = streamToString(is);
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
            // сохранить корректную копию в локальную папку
            copyAssetToFile("images.json", new File(context.getFilesDir(), "content/images.json"));
            Log.d(TAG, "Изображения успешно загружены из Assets и сохранены локально");
        } catch (Exception e) {
            Log.e(TAG, "Критическая ошибка: не удалось загрузить изображения из Assets", e);
        }
        return photos;
    }

    /**
     * Метод getLocalTagsList загружает список записей о тегах из локального файла tags.json
     * @return TriggerItem-список всех записей о тегах из локального файла
     */
    public List<TriggerItem> getLocalTagsList(){
        List<TriggerItem> tags = new ArrayList<>();
        File tagsFile = new File(context.getFilesDir(), "content/tags.json");

        // попытка загрузить из локального файла
        if (tagsFile.exists()) {
            try {
                String json = readFileToString(tagsFile);
                if (json != null && !json.isEmpty()) {
                    JSONArray array = new JSONArray(json);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = safeGetJSONObject(array, i);
                        String imgTag = obj.optString("img_tag", null);
                        boolean isParent = obj.optBoolean("is_parent", false);
                        String parentTag = obj.optString("parent_tag", "");
                        String nameRus = obj.optString("name_rus", "");
                        if (imgTag != null && !imgTag.isEmpty()) {
                            tags.add(new TriggerItem(imgTag, isParent, parentTag, nameRus));
                        }
                    }
                    Log.d(TAG, "Загружено " + tags.size() + " тегов из локального файла");
                    return tags;
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка парсинга локального tags.json. Файл поврежден.", e);
                // логирование начала файла для диагностики
                try {
                    String badContent = readFileToString(tagsFile);
                    if (badContent != null) {
                        Log.e(TAG, "Содержимое поврежденного файла (первые 200 сим): " +
                                badContent.substring(0, Math.min(200, badContent.length())));
                    }
                } catch (Exception ex) { /* ignore */ }

                // удалить поврежденный файл
                tagsFile.delete();
                Log.w(TAG, "Поврежденный файл tags.json удален. Восстановление из Assets...");
            }
        }

        // если локального файла нет или он был удален, загрузить из assets
        Log.d(TAG, "Загрузка тегов из Assets");
        try {
            InputStream is = context.getAssets().open("tags.json");
            String json = streamToString(is);
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String imgTag = obj.optString("img_tag", null);
                boolean isParent = obj.optBoolean("is_parent", false);
                String parentTag = obj.optString("parent_tag", "");
                String nameRus = obj.optString("name_rus", "");
                if (imgTag != null && !imgTag.isEmpty()) {
                    tags.add(new TriggerItem(imgTag, isParent, parentTag, nameRus));
                }
            }
            // сохранить корректную копию в локальную папку
            copyAssetToFile("tags.json", new File(context.getFilesDir(), "content/tags.json"));
            Log.d(TAG, "Теги успешно загружены из Assets и сохранены локально");
        } catch (Exception e) {
            Log.e(TAG, "Критическая ошибка: не удалось загрузить теги из Assets", e);
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

    public boolean isAppInfoViewed(){
        return prefs.getBoolean(KEY_APP_INFO_VIEWED, false);
    }

    public void markAppInfoViewed(){
        prefs.edit().putBoolean(KEY_APP_INFO_VIEWED, true).apply();
    }


    // ИНИЦИАЛИЗАЦИЯ КОНТЕНТА

    /**
     * Метод initializeContent запускает инициализацию контента.
     * Если контент ещё не скопирован, копирует из assets.
     * Если есть интернет, проверяет наличие обновлений в облачной БД.
     * @param onReady вызывается, когда контент готов (в т. ч. без интернета)
     */
    public void initializeContent(Runnable onReady) {
        boolean contentReady = prefs.getBoolean(KEY_CONTENT_READY, false);
        if (!contentReady) {
            // первый запуск: скопировать стартовый набор контента из assets
            copyInitialContent(() -> {
                prefs.edit().putBoolean(KEY_CONTENT_READY, true).putInt(KEY_LOCAL_CONTENT_VERSION, 1).apply();
                // после копирования: проверить обновления (если есть интернет)
                checkForContentUpdates(onReady);
            });
        } else {
            // уже есть локальный контент: только проверка обновлений
            checkForContentUpdates(onReady);
        }
    }

    /**
     * Метод copyInitialContent копирует стартовый набор контента из папки assets
     * @param onComplete вызывается при завершении копирования
     */
    private void copyInitialContent(Runnable onComplete) {
        new Thread(() -> {
            try {
                // скопировать tags.json
                copyAssetToFile("tags.json", new File(contentDir, "tags.json"));
                // скопировать images.json
                copyAssetToFile("images.json", new File(contentDir, "images.json"));
                // скопировать все фото из assets/photos
                copyAssetsPhotos();
                Log.d(TAG, "Начальный контент скопирован в " + contentDir.getAbsolutePath());
                onComplete.run();
            }
            catch (Exception e) {
                Log.e(TAG, "Ошибка копирования начального контента", e);
                onComplete.run();
            }
        }).start();
    }

    /**
     * Метод copyAssetToFile копирует файл (images.json или tags.json) из папки assets в указанный файл
     * @param assetPath имя файла в папке assets
     * @param destFile создаваемая копия файла
     * @throws IOException исключение при копировании
     */
    private void copyAssetToFile(String assetPath, File destFile) throws IOException {
        try (InputStream is = context.getAssets().open(assetPath);
             OutputStream os = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            // проверить, что файл содержит валидный UTF-8
            if (assetPath.equals("tags.json") || assetPath.equals("images.json")) {
                try (FileInputStream fis = new FileInputStream(destFile)) {
                    byte[] bom = new byte[3];
                    if (fis.read(bom) == 3 && bom[0] == (byte)0xEF && bom[1] == (byte)0xBB && bom[2] == (byte)0xBF) {
                        Log.d(TAG, "Файл " + assetPath + " содержит UTF-8 BOM");
                    }
                }
            }
        }
    }

    /**
     * Метод copyAssetsPhotos копирует все фото из папки assets/photos
     * @throws IOException исключение при копировании
     */
    private void copyAssetsPhotos() throws IOException {
        String[] files = context.getAssets().list("photos");
        if (files != null) {
            for (String filename : files) {
                File dest = new File(photosDir, filename);
                copyAssetToFile("photos/" + filename, dest);
            }
        }
    }


    // ПРОВЕРКА И ОБНОВЛЕНИЕ КОНТЕНТА

    /**
     * Метод checkForContentUpdates сравнивает значение версии контента в облачной БД
     * с версией локального контента и вызывает загрузку нового контента, если он есть
     * @param onReady вызывается при завершении проверки (и загрузки, когда нужна)
     */
    private void checkForContentUpdates(Runnable onReady) {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "[SYNC] Нет интернета. Используются локальные данные.");
            onReady.run();
            return;
        }
        Log.d(TAG, "[SYNC] Проверка версии контента в облаке...");
        db.collection("meta").document("version")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists() && snapshot.contains("version")) {
                        long remoteVersion = snapshot.getLong("version");
                        int localVersion = prefs.getInt(KEY_LOCAL_CONTENT_VERSION, 1);
                        Log.d(TAG, "[SYNC] Облачная версия: " + remoteVersion + ", Локальная: " + localVersion);
                        if (remoteVersion > localVersion) {
                            Log.d(TAG, "[SYNC] Найдено обновление. Запуск загрузки...");
                            downloadAndApplyContentUpdate((int) remoteVersion, onReady);
                        } else {
                            Log.d(TAG, "[SYNC] Контент актуален.");
                            onReady.run();
                        }
                    } else {
                        Log.w(TAG, "[SYNC] Документ meta/version не содержит поле 'version'");
                        onReady.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "[SYNC] Ошибка подключения к Firestore. Код: " +
                            (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                    onReady.run(); // продолжить работу с лок. данными без обновления
                });
    }

    /**
     * Метод downloadAndApplyContentUpdate скачивает новый контент из облака
     * @param newVersion значение новейшей версии контента в облаке
     * @param onReady вызывается при завершении скачивания
     */
    private void downloadAndApplyContentUpdate(int newVersion, Runnable onReady) {
        // скачивание коллекции тегов
        db.collection("tags_collection").get().addOnSuccessListener(tagsSnapshot -> {
            Log.d(TAG, "[SYNC] Теги получены (количество: " + tagsSnapshot.size() + ")");
            saveCollectionAsJson("tags.json", tagsSnapshot, () -> {
                // скачивание коллекции изображений
                db.collection("images").get().addOnSuccessListener(imagesSnapshot -> {
                    Log.d(TAG, "[SYNC] Изображения получены (количество: " + imagesSnapshot.size() + ")");
                    saveCollectionAsJson("images.json", imagesSnapshot, () -> {
                        // скачивание недостающих фото
                        downloadMissingPhotos(imagesSnapshot, () -> {
                            // обновление локальной версии контента
                            prefs.edit().putInt(KEY_LOCAL_CONTENT_VERSION, newVersion).apply();
                            Log.d(TAG, "[SYNC] Контент успешно обновлён до версии " + newVersion);
                            onReady.run();
                        });
                    });
                }).addOnFailureListener(e ->{
                    Log.e(TAG, "[SYNC] Ошибка загрузки коллекции images: ", e);
                    onReady.run();
                });
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "[SYNC] Ошибка загрузки коллекции tags: ", e);
            onReady.run();
        });
    }

    /**
     * Метод saveCollectionAsJson сохраняет результат запроса к коллекции Firestore в файл json
     * @param filename имя файла json
     * @param snapshot результат запроса к коллекции
     * @param onComplete вызывается при завершении сохранения
     */
    private void saveCollectionAsJson(String filename, QuerySnapshot snapshot, Runnable onComplete) {
        new Thread(() -> {
            try {
                JSONArray array = new JSONArray();
                for (DocumentSnapshot doc : snapshot) {
                    // явно создать JSONObject из Map данных документа
                    // (гарантирует, что в файл запишется корректный JSON, а не toString() Map)
                    JSONObject jsonObj = new JSONObject(doc.getData());
                    array.put(jsonObj);
                }
                File file = new File(contentDir, filename);
                // записывать файл атомарно (сначала во временный, потом переименовывать)
                // чтобы избежать повреждения при обрыве записи
                File tempFile = new File(contentDir, filename + ".tmp");
                // запись данных во временный файл
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(array.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                // если запись успешна, заменить старый файл новым
                if (file.exists()) {
                    file.delete();
                }
                boolean success = tempFile.renameTo(file);
                if (success) {
                    Log.d(TAG, "Файл " + filename + " успешно сохранен (" + array.length() + " записей)");
                } else {
                    Log.w(TAG, "Переименование не сработало. Файл сохранен напрямую.");
                    // Fallback на случай, если rename запрещен на некоторых устройствах
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(array.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                }
                onComplete.run();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка сохранения JSON: " + filename, e);
                // удалить временный файл, если он остался
                File tempFile = new File(contentDir, filename + ".tmp");
                if (tempFile.exists()) tempFile.delete();
                onComplete.run();
            }
        }).start();
    }

    /**
     * Метод downloadMissingPhotos скачивает новые фото из облачного хранилища
     * @param imagesSnapshot результат результат запроса к коллекции images
     * @param onComplete вызывается при завершении скачивания
     */
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


    // РАБОТА С ПОЛЬЗОВАТЕЛЕМ И НАСТРОЙКАМИ

    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    public boolean isGuest() {
        return prefs.getBoolean(KEY_IS_GUEST, true);
    }

    /**
     * Метод getUserId возвращает ID пользователя
     * @return guest_... или firebase UID
     */
    public String getUserId() {
        String savedId = prefs.getString(KEY_USER_ID, null);
        if (savedId != null)
            return savedId;
        // при первом запуске создать гостя
        String guestId = "guest_" + System.currentTimeMillis();
        prefs.edit().putString(KEY_USER_ID, guestId).putBoolean(KEY_IS_GUEST, true).apply();
        return guestId;
    }

    /**
     * Метод handleUserLogin обрабатывает вход пользователя в аккаунт.
     * Логика синхронизации:
     * - Новый пользователь (регистрация; нет данных в облаке): сохраняет локальные данные в облако
     * - Существующий пользователь: загружает данные из облака
     * - Существующий пользователь, уже вошедший на другом устройстве: сравнивает временные метки для синхронизации
     * @param onSyncComplete вызывается при завершении синхронизации данных
     */
    public void handleUserLogin(Runnable onSyncComplete) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            onSyncComplete.run();
            return;
        }
        String firebaseUid = firebaseUser.getUid();
        boolean wasGuest = isGuest();
        long localLastModified = prefs.getLong(KEY_LAST_MODIFIED_LOCAL, System.currentTimeMillis());
        // загрузка данных из Firestore
        db.collection("users").document(firebaseUid).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        // СЛУЧАЙ 1: НОВЫЙ ПОЛЬЗОВАТЕЛЬ (регистрация)
                        // сохранить текущие локальные настройки (гостевые) в Firestore
                        saveLocalUserSettingsToFirestore(firebaseUid, localLastModified, () -> {
                            // синхронизировать историю упражнений
                            syncExerciseHistoryToFirestore();
                            // обновить локальный статус пользователя
                            prefs.edit()
                                    .putString(KEY_USER_ID, firebaseUid)
                                    .putBoolean(KEY_IS_GUEST, false)
                                    .apply();
                            onSyncComplete.run();
                        });
                    } else {
                        // СЛУЧАЙ 2: СУЩЕСТВУЮЩИЙ ПОЛЬЗОВАТЕЛЬ
                        long remoteLastModified = snapshot.getLong("last_modified");
                        if (wasGuest) {
                            // Подслучай 2a: ГОСТЬ входит в существующий аккаунт
                            // очистить гостевые данные
                            clearLocalUserData();
                            // загрузить данные из облака
                            loadUserSettingsFromSnapshot(snapshot);
                            // загрузить историю упражнений из облака
                            syncExerciseHistoryFromFirestore(() -> {
                                prefs.edit()
                                        .putString(KEY_USER_ID, firebaseUid)
                                        .putBoolean(KEY_IS_GUEST, false)
                                        .putLong(KEY_LAST_MODIFIED_LOCAL, remoteLastModified)
                                        .apply();
                                onSyncComplete.run();
                            });
                        } else {
                            // Подслучай 2b: ПОЛЬЗОВАТЕЛЬ снова входит в свой аккаунт (одновременно)
                            // сравнить временные метки для корректной синхронизации на нескольких устрайствах
                            if (localLastModified >= remoteLastModified) {
                                // локальные данные новее или равны - сохранить их в облако
                                saveLocalUserSettingsToFirestore(firebaseUid, localLastModified, () -> {
                                    // синхронизировать историю упражнений
                                    syncExerciseHistoryToFirestore();
                                    prefs.edit()
                                            .putString(KEY_USER_ID, firebaseUid)
                                            .putBoolean(KEY_IS_GUEST, false)
                                            .apply();
                                    onSyncComplete.run();
                                });
                            } else {
                                // облачные данные новее - загрузить их локально
                                loadUserSettingsFromSnapshot(snapshot);
                                // загрузить историю упражнений из облака
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
                    // при ошибке сохранить текущие локальные данные, но изменить статус на авторизованный
                    prefs.edit()
                            .putString(KEY_USER_ID, firebaseUid)
                            .putBoolean(KEY_IS_GUEST, false)
                            .apply();
                    onSyncComplete.run();
                });
    }

    /**
     * Метод handleUserLogout обрабатывает выход пользователя из аккаунта.
     * Данные остаются локально, создаётся новый гостевой ID
     */
    public void handleUserLogout() {
        // полная очистка данных перед созданием нового гостя
        clearLocalUserData();
        // создание нового гостя
        String guestId = "guest_" + System.currentTimeMillis();
        prefs.edit()
                .putString(KEY_USER_ID, guestId)
                .putBoolean(KEY_IS_GUEST, true)
                .apply();
    }

    /**
     * Метод clearLocalUserData очищает все пользовательские данные (для нового гостя или при входе в чужой аккаунт).
     * Выполняется синхронно, чтобы UI не показывал старые данные.
     */
    private void clearLocalUserData() {
        SharedPreferences.Editor editor = prefs.edit();
        // сброс настроек к значениям по умолчанию
        editor.putString(KEY_TRIGGERS, "[]");
        editor.putString(KEY_FAVES, "[]");
        editor.putInt(KEY_BREATH_REPEAT, 1);
        editor.putBoolean(KEY_USE_MATH, true);
        editor.putBoolean(KEY_USE_COLOR_SEARCH, true);
        editor.putInt(KEY_GROUND_PHOTO_AMOUNT, 2);
        editor.putBoolean(KEY_USE_FAVES_ONLY, false);
        editor.putLong(KEY_LAST_MODIFIED_LOCAL, System.currentTimeMillis());
        // сброс истории упражнений
        saveExerciseHistory(new ArrayList<>()); // сохранение пустого списка
        editor.putLong(KEY_EXERCISE_HISTORY_LAST_MODIFIED, 0);
        editor.apply();
    }


    // СОХРАНЕНИЕ И ЗАГРУЗКА НАСТРОЕК

    /**
     * Метод saveUserSetting сохраняет пользовательскую настройку и обновляет временную метку
     * @param key ключ-название настройки
     * @param value новое значение настройки
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
            // списки сохраняются как JSON-строки
            JSONArray array = new JSONArray((List<?>) value);
            editor.putString(key, array.toString());
        }
        editor.putLong(KEY_LAST_MODIFIED_LOCAL, now);
        editor.apply();
        if (!isGuest()) {
            syncUserSettingsToFirestore(now);
        }
    }

    /**
     * Метод syncUserSettingsToFirestore синхронизирует польз. настройки в фоне
     * @param lastModified время последнего изменения локальных настроек
     */
    private void syncUserSettingsToFirestore(long lastModified) {
        if (!isNetworkAvailable()){
            return;
        }
        String userId = getUserId();
        if (isGuest())
            return;
        Map<String, Object> data = new HashMap<>();
        try {
            data.put("email", prefs.getString("email", ""));
            data.put("triggers", convertJsonStringToList(prefs.getString(KEY_TRIGGERS, "[]")));
            data.put("faves", convertJsonStringToList(prefs.getString(KEY_FAVES, "[]")));
            data.put("breath_repeat_amount", prefs.getInt(KEY_BREATH_REPEAT, 1));
            data.put("use_math", prefs.getBoolean(KEY_USE_MATH, true));
            data.put("use_search_objects_color", prefs.getBoolean(KEY_USE_COLOR_SEARCH, true));
            data.put("ground_photo_ex_amount", prefs.getInt(KEY_GROUND_PHOTO_AMOUNT, 2));
            data.put("use_faves_only", prefs.getBoolean(KEY_USE_FAVES_ONLY, false));
            data.put("last_modified", lastModified);
            // вместо .set(data); использовать .set(data, SetOptions.merge())
            // (т.к. set - полная перезапись документа)
            db.collection("users").document(userId)
                    .set(data, SetOptions.merge())
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Ошибка синхронизации настроек", e)
                    );
        } catch (Exception e) {
            Log.e(TAG, "Ошибка сериализации настроек", e);
        }
    }

    /**
     * Метод saveLocalUserSettingsToFirestore сохраняет локальные польз. настройки в БД
     * @param userId ID пользователя
     * @param lastModified время последнего изменения локальных настроек
     * @param onComplete вызывается при завершении синхронизации
     */
    private void saveLocalUserSettingsToFirestore(String userId, long lastModified, Runnable onComplete) {
        if (!isNetworkAvailable()) {
            onComplete.run();
            return;
        }
        Map<String, Object> data = new HashMap<>();
        try {
            data.put("email", prefs.getString("email", ""));
            data.put("triggers", convertJsonStringToList(prefs.getString(KEY_TRIGGERS, "[]")));
            data.put("faves", convertJsonStringToList(prefs.getString(KEY_FAVES, "[]")));
            data.put("breath_repeat_amount", prefs.getInt(KEY_BREATH_REPEAT, 1));
            data.put("use_math", prefs.getBoolean(KEY_USE_MATH, true));
            data.put("use_search_objects_color", prefs.getBoolean(KEY_USE_COLOR_SEARCH, true));
            data.put("ground_photo_ex_amount", prefs.getInt(KEY_GROUND_PHOTO_AMOUNT, 2));
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

    /**
     * Метод loadUserSettingsFromSnapshot загружает настройки пользователя из БД
     * @param snapshot результат запроса к коллекции users (документ с данными пользователя
     */
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
        editor.putBoolean(KEY_USE_FAVES_ONLY, snapshot.getBoolean("use_faves_only"));
        editor.apply();
    }

    /**
     * Метод handleAccountDeletion обрабатывает полное удаление аккаунта пользователя.
     * Вызывается после успешного удаления аккаунта из Firebase Auth и Firestore.
     * 1. Очищает все пользовательские данные из SharedPreferences.
     * 2. Сбрасывает все настройки к значениям по умолчанию.
     * 3. Создаёт нового гостя с новым ID.
     * 4. Очищает историю упражнений.
     * 5. Сбрасывает флаги состояния.
     */
    public void handleAccountDeletion() {
        Log.d(TAG, "Начало обработки удаления аккаунта");
        // полная очистка всех пользовательских данных
        clearAllUserData();
        // создание нового гостя с новым ID
        String guestId = "guest_" + System.currentTimeMillis();
        prefs.edit()
                .putString(KEY_USER_ID, guestId)
                .putBoolean(KEY_IS_GUEST, true)
                .putLong(KEY_LAST_MODIFIED_LOCAL, System.currentTimeMillis())
                .apply();
        Log.d(TAG, "Аккаунт удалён, создан новый гость: " + guestId);
    }

    /**
     * Метод clearAllUserData полностью очищает все пользовательские данные из SharedPreferences.
     * В отличие от clearLocalUserData(), этот метод также сбрасывает флаги онбординга и другие системные настройки.
     * Используется при удалении аккаунта.
     */
    private void clearAllUserData() {
        SharedPreferences.Editor editor = prefs.edit();
        // польз. настройки
        editor.putString(KEY_TRIGGERS, "[]");
        editor.putString(KEY_FAVES, "[]");
        editor.putInt(KEY_BREATH_REPEAT, 1);
        editor.putBoolean(KEY_USE_MATH, true);
        editor.putBoolean(KEY_USE_COLOR_SEARCH, true);
        editor.putInt(KEY_GROUND_PHOTO_AMOUNT, 2);
        editor.putBoolean(KEY_USE_FAVES_ONLY, false);
        // история упражнений
        saveExerciseHistory(new ArrayList<>());
        editor.putLong(KEY_EXERCISE_HISTORY_LAST_MODIFIED, 0);
        // флаги состояния
        editor.putBoolean(KEY_ONBOARDING_COMPLETED, false);
        editor.putBoolean(KEY_APP_INFO_VIEWED, false);
        // очистка email
        editor.remove("email");
        // очистка старого user_id
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_IS_GUEST);
        editor.remove(KEY_LAST_MODIFIED_LOCAL);
        editor.apply();
        Log.d(TAG, "Все пользовательские данные очищены");
    }


    // ИСТОРИЯ ПРОЙДЕННЫХ УПРАЖНЕНИЙ С ФОТО

    /**
     * Метод loadExerciseHistory загружает историю сессий из локального файла.
     * @return ExerciseSession-список последних сессий (максимум 3) или пустой список при ошибке/отсутствии файла
     */
    public List<ExerciseSession> loadExerciseHistory() {
        List<ExerciseSession> sessions = new ArrayList<>();
        File historyFile = new File(context.getFilesDir(), EXERCISE_HISTORY_FILE);
        if (!historyFile.exists()) {
            // файл ещё не создан - возвратить пустой список
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
            // читать максимум 3 сессии
            int limit = Math.min(jsonArray.length(), 3);
            for (int i = 0; i < limit; i++) {
                JSONObject sessionObj = jsonArray.getJSONObject(i);
                long timestamp = sessionObj.getLong("timestamp");
                // загрузка фото из сессии
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
            // возвратить пустой список
            return new ArrayList<>();
        }
        return sessions;
    }

    /**
     * Метод saveExerciseHistory сохраняет список сессий в локальный файл.
     * Автоматически обрезает список до 3 элементов (самые свежие в начале).
     * @param sessions ExerciseSessions-список - пройденный набор упражнений с фото
     */
    public void saveExerciseHistory(List<ExerciseSession> sessions) {
        // обрезать до 3 самых свежих сессий (они должны быть в начале списка)
        if (sessions.size() > 3) {
            sessions = sessions.subList(0, 3);
        }
        JSONArray jsonArray = new JSONArray();
        for (ExerciseSession session : sessions) {
            try {
                JSONObject sessionObj = new JSONObject();
                sessionObj.put("timestamp", session.timestamp);
                // сохранение фото
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
        // запись в файл
        File historyFile = new File(context.getFilesDir(), EXERCISE_HISTORY_FILE);
        try (FileOutputStream fos = new FileOutputStream(historyFile)) {
            fos.write(jsonArray.toString(2).getBytes("UTF-8")); // toString(2) для читаемого формата
            Log.d(TAG, "История упражнений сохранена: " + sessions.size() + " сессий");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка сохранения истории упражнений", e);
        }
    }

    /**
     * Метод addExerciseSession добавляет новую сессию в историю и локально сохраняет обновлённый список.
     * Автоматически ограничивает историю 3 последними сессиями.
     * @param newSession новый пройденный набор упражнений
     */
    public void addExerciseSession(ExerciseSession newSession) {
        // загрузка списка существующей истории
        List<ExerciseSession> sessions = loadExerciseHistory();
        // добавление новой сессии в начало списка (самые свежие первые)
        sessions.add(0, newSession);
        // сохранение обновлённого списка
        saveExerciseHistory(sessions);
    }

    /**
     * Метод addExerciseSessionAndSync добавляет новую сессию в историю и синхронизирует с облаком.
     * @param newSession новый пройденный набор упражнений
     */
    public void addExerciseSessionAndSync(DataManager.ExerciseSession newSession) {
        // добавление сессии в локальную историю
        addExerciseSession(newSession);
        // синхронизация с облаком
        if (!isGuest()) {
            syncExerciseHistoryToFirestore();
        }
    }

    /**
     * Метод syncExerciseHistoryToFirestore синхронизирует историю упражнений с Firestore.
     */
    private void syncExerciseHistoryToFirestore() {
        if (!isNetworkAvailable())
            return;
        String userId = getUserId();
        if (isGuest())
            return;
        // загрузка локальной истории
        List<ExerciseSession> localHistory = loadExerciseHistory();
        if (localHistory.isEmpty())
            return;
        // подготавка данных для отправки
        List<Map<String, Object>> historyData = new ArrayList<>();
        for (ExerciseSession session : localHistory) {
            Map<String, Object> sessionMap = new HashMap<>();
            sessionMap.put("timestamp", session.timestamp);
            // преобразование фото в формат для Firestore
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
        // сохранение в Firestore
        Map<String, Object> data = new HashMap<>();
        data.put("exercise_history", historyData);
        data.put("exercise_history_last_modified", System.currentTimeMillis());
        db.collection("users").document(userId)
                .update(data)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "История упражнений синхронизирована с облаком");
                    // обновление локальной временной метки
                    prefs.edit()
                            .putLong(KEY_EXERCISE_HISTORY_LAST_MODIFIED, System.currentTimeMillis())
                            .apply();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка синхронизации истории упражнений", e);
                });
    }

    /**
     * Метод syncExerciseHistoryFromFirestore загружает историю упражнений из облака (если она новее локальной).
     * @param onComplete вызывается при завершении загрузки
     */
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
                        // если облачная версия новее, загрузить её
                        if (remoteLastModified > localLastModified) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> remoteHistory =
                                    (List<Map<String, Object>>) snapshot.get("exercise_history");

                            if (remoteHistory != null && !remoteHistory.isEmpty()) {
                                // преобразование данных из Firestore в локальный формат
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
                                // сохранить локально
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


    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ

    /**
     * Метод getFilenameFromUrl получает ссылку на изображение и возвращает имя файла.
     * Пример ссылки:
     * "https://raw.githubusercontent.com/sovush28/PanicPauseImages/refs/heads/main/michael-myers-FvuisLAN-rA-unsplash.jpg",
     * где "michael-myers-FvuisLAN-rA-unsplash.jpg" - имя файла.
     * @param url ссылка на изображение
     * @return имя файла
     */
    public static String getFilenameFromUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            return new File(Uri.parse(url).getPath()).getName();
        } catch (Exception e) {
            int lastSlash = url.lastIndexOf('/');
            return (lastSlash != -1) ? url.substring(lastSlash + 1) : url;
        }
    }

    /**
     * Метод isNetworkAvailable проверяет наличие подключения к сети Интернет.
     * Является заглушкой; проверка подключения осуществляется с помощью onFailure Firebase
     * при произведении операций с Firebase.
     * Метод может быть улучшен с помощью ConnectivityManager при необходимости.
     * @return true, если Интернет доступен; false, если Интернет не доступен
     */
    private boolean isNetworkAvailable() {
        return true;
    }

    /**
     * Метод convertJsonStringToList преобразует json-строку в список строк.
     * Нужен для обеспечения безопасного преобразования данных.
     * @param jsonString json-строка
     * @return список строк или пустой список при ошибке
     */
    private List<String> convertJsonStringToList(String jsonString) {
        try {
            JSONArray array = new JSONArray(jsonString);
            List<String> list = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * safeGetJSONObject - метод для безопасного получения JSONObject из JSONArray.
     * Если элемент является строкой, метод пытается распарсить её как JSON.
     * @param array данный JSONArray
     * @param index индекс JSONObject в JSONArray
     * @return требуемый JSONObject
     * @throws JSONException исключение при возникновении ошибки извлечения элемента из массива
     */
    private JSONObject safeGetJSONObject(JSONArray array, int index) throws JSONException {
        Object item = array.get(index);
        if (item instanceof JSONObject) {
            return (JSONObject) item;
        } else if (item instanceof String) {
            String str = (String) item;
            // если строка начинается с '{', попытаться распарсить её как JSON
            if (str.trim().startsWith("{")) {
                return new JSONObject(str);
            }
        }
        throw new JSONException("Неожиданный тип элемента в массиве по индексу " + index + ": " + item.getClass().getName());
    }

    /**
     * Метод readFileToString читает файл в строку.
     * Используется в методах getLocalTagsList и getLocalImagesList для чтения файлов json, готовых к работе.
     * @param file читаемый файл
     * @return строка - содержание файла
     * @throws IOException исключение при чтении файла
     */
    private String readFileToString(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, length);
            }
            String content = bos.toString("UTF-8");
            // удалить Byte Order Mark, если есть
            if (content.startsWith("\ufeff")) {
                content = content.substring(1);
            }
            return content;
        }
    }

    /**
     * Метод streamToString читает поток InputStream в строку.
     * Используется в методах getLocalTagsList и getLocalImagesList для чтения файлов json, находящихся в папке assets.
     * @param is поток InputStream
     * @return строка
     * @throws IOException исключение при чтении потока
     */
    private String streamToString(InputStream is) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                bos.write(buffer, 0, length);
            }
            return bos.toString("UTF-8");
        }
    }

    /**
     * Метод testFirestoreConnection проверяет подключение к Firebase
     * и соответствующе логирует результат проверки.
     * Необходим для мониторинга подключения во время тестирования приложения.
     */
    public void testFirestoreConnection() {
        db.collection("meta").document("version").get()
                .addOnSuccessListener(s -> Log.d("FIREBASE_TEST", "[ПРОВЕРКА FIREBASE] Подключение успешно! Версия: " + s.getLong("version")))
                .addOnFailureListener(e -> Log.e("FIREBASE_TEST", "[ПРОВЕРКА FIREBASE] Ошибка подключения: " + e.getMessage()));
    }

}