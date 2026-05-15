package com.example.panicpause;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GroundActivity - активность, отвечающая за отображение упражнений.
 * Состоит из 4 видов упражнений:
 * 1. Дыхание по квадрату (GroundBreathFragment)
 * 2. Подсчет предметов на изображении
 * 3. Математический пример
 * 4. Подсчет предметов определенного цвета вокруг себя
 */
public class GroundActivity extends AppCompatActivity {
    private static final String TAG = "GroundActivity";

    // менеджер фрагментов для работы с экранами
    private FragmentManager fragmentManager;

    // текущий индекс упражнения в последовательности
    private int currentFragmentIndex = 0;

    // список для хранения созданных фрагментов
    private List<Fragment> fragmentInstances = new ArrayList<>();

    // список классов фрагментов в правильном порядке
    private List<Class<? extends Fragment>> fragmentClasses = new ArrayList<>();

    private DataManager dataManager;

    // список использованных фото в текущей сессии
    private List<DataManager.PhotoData> currentSessionPhotos = new ArrayList<>();

    // поле для хранения доступных фото
    private List<DataManager.PhotoData> availablePhotosForSession = new ArrayList<>();

    private boolean useDefaultSettings = false;
    private boolean useFavesOnly=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ground);

        dataManager=new DataManager(this);
        fragmentManager = getSupportFragmentManager();

        Intent intent = getIntent();
        if(intent!=null){
            if(intent.hasExtra("default_settings")){
                useDefaultSettings = intent.getBooleanExtra("default_settings", false);
            }
            if(intent.hasExtra("faves_only")){
                useFavesOnly = intent.getBooleanExtra("faves_only", false);
            }
        }

        buildAndStartGroundSequence();

        /*
        // Восстанавливаем состояние если активность была пересоздана
        if (savedInstanceState != null) {
            currentFragmentIndex = savedInstanceState.getInt(KEY_CURRENT_INDEX, 0);
            // пытаемся найти сохраненные фрагменты
            restoreFragments();
        } else {
            Intent intent = getIntent();
            if(intent.hasExtra("default_settings")){
                Boolean useDefaultSettings = intent.getBooleanExtra("default_settings", true);
                loadUserSettingsAndStartSequence(useDefaultSettings);
            }
            else {
                // Загружаем настройки и запускаем новую сессию
                loadUserSettingsAndStartSequence(true);
            }
        }*/

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void buildAndStartGroundSequence(){
        buildExerciseSequence();

        initializeAvailablePhotos();

        startGroundingSequence();
    }

    private void initializeAvailablePhotos() {
        availablePhotosForSession = new ArrayList<>();
        List<DataManager.PhotoData> allPhotos = dataManager.getLocalImagesList();
        if(!useFavesOnly){
            availablePhotosForSession = new ArrayList<>(allPhotos);

            if (!useDefaultSettings) {
                // фильтрация по триггерам
                List<String> triggers = dataManager.getTriggers();
                if (triggers != null && !triggers.isEmpty()) {
                    availablePhotosForSession.removeIf(photo ->
                            photo.tags.stream().anyMatch(triggers::contains)
                    );
                }
            }
        }
        else{
            List<String> userFaves = dataManager.getFaves();
            for (DataManager.PhotoData photo : allPhotos){
                if(userFaves.contains(photo.imgUrl)){
                    availablePhotosForSession.add(photo);
                }
            }
        }

        Collections.shuffle(availablePhotosForSession);
    }

    /**
     * getNextUniquePhoto - метод для получения уникального фото
     * Возвращает следующее уникальное фото из пула или null, если фото закончились.
     */
    public DataManager.PhotoData getNextUniquePhoto() {
        if (availablePhotosForSession.isEmpty()) {
            return null;
        }
        // взять первое и удалить, чтобы не повторилось
        return availablePhotosForSession.remove(0);
    }

    /**
     * Метод buildExerciseSequence строит последовательность упражнений на основе настроек пользователя.
     */
    private void buildExerciseSequence() {
        fragmentClasses.clear();

        // 1. Первое упражнение - дыхание
        fragmentClasses.add(GroundBreathFragment.class);

        boolean useMath = useDefaultSettings ? true : dataManager.getUseMath();
        boolean useSearchObjectsColor = useDefaultSettings ? true : dataManager.getUseSearchObjectsColor();
        int groundPhotoExAmount = useDefaultSettings ? 2 : dataManager.getGroundPhotoExAmount();

        if(!useMath && !useSearchObjectsColor){
            // 2. Упражнения с фотографиями
            for (int i = 0; i < groundPhotoExAmount; i++) {
                fragmentClasses.add(GroundPhotoFragment.class);
            }
        }
        else {
            // 2. Упражнение с фотографией
            fragmentClasses.add(GroundPhotoFragment.class);

            // 3. Математические примеры
            if (useMath) {
                fragmentClasses.add(GroundMathFragment.class);
            }

            // 4. Поиск предметов по цвету
            if (useSearchObjectsColor) {
                fragmentClasses.add(GroundCountColorFragment.class);
            }

            // 5. Упражнение с фотографией
            fragmentClasses.add(GroundPhotoFragment.class);
        }

        // последнее упражнение - дыхание
        fragmentClasses.add(GroundBreathFragment.class);
        Log.d(TAG, "Последовательность упражнений построена с " + fragmentClasses.size() + " упражнениями");
    }

    /**
     * Метод startGroundingSequence создает и сохраняет все фрагменты заранее с уникальными тегами.
     */
    private void startGroundingSequence() {
        if (fragmentClasses.isEmpty()) {
            Toast.makeText(this, "Ошибка: нет упражнений", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        try {
            // создание всех фрагментов заранее
            fragmentInstances.clear();
            for (int i = 0; i < fragmentClasses.size(); i++) {
                try {
                    Fragment fragment = fragmentClasses.get(i).newInstance();
                    if (fragment instanceof GroundPhotoFragment) {
                        DataManager.PhotoData assignedPhoto = getNextUniquePhoto();
                        ((GroundPhotoFragment) fragment).assignPhoto(assignedPhoto);
                    }
                    fragmentInstances.add(fragment);
                }
                catch (Exception e) {
                    Log.e(TAG, "Ошибка при создании фрагмента с индексом " + i, e);
                    finish();
                    return;
                }
            }
            showFragment(0);
        } catch (Exception e) {
            Log.e(TAG, "Error creating fragment instances", e);
        }
    }

    /**
     * Метод showFragment показывает фрагмент по указанному индексу в последовательности.
     * @param fragmentIndex индекс фрагмента
     */
    private void showFragment(int fragmentIndex) {
        if (fragmentIndex < 0 || fragmentIndex >= fragmentClasses.size()) {
            Log.e(TAG, "Invalid fragment index: " + fragmentIndex);
            return;
        }

        Fragment targetFragment = fragmentInstances.get(fragmentIndex);
        androidx.fragment.app.FragmentTransaction transaction = fragmentManager.beginTransaction();

        // скрыть текущий фрагмент (если есть)
        if (currentFragmentIndex < fragmentInstances.size()) {
            Fragment current = fragmentInstances.get(currentFragmentIndex);
            if (current.isAdded()) {
                transaction.hide(current);
                if (current instanceof GroundBreathFragment) {
                    ((GroundBreathFragment) current).onFragmentPaused();
                }
            }
        }

        // показать целевой
        if (targetFragment.isAdded()) {
            transaction.show(targetFragment);
        } else {
            transaction.add(R.id.fragment_container, targetFragment);
        }

        transaction.commit();
        fragmentManager.executePendingTransactions();

        // сначала обновить текщий индекс
        currentFragmentIndex = fragmentIndex;

        // затем обновить кнопки (таким образом isLastFragment() возвращает правильное значение)
        if (targetFragment instanceof GroundBreathFragment) {
            ((GroundBreathFragment) targetFragment).onFragmentResumed();
            ((GroundBreathFragment) targetFragment).updateButtonsForPosition();
        }

        currentFragmentIndex = fragmentIndex;
        Log.d(TAG, "Отображение фрагмента с индексом: " + fragmentIndex);
    }

    public void goToNextFragment() {
        if (currentFragmentIndex < fragmentClasses.size() - 1) {
            // переход к следующему упражнению
            showFragment(currentFragmentIndex + 1);
        } else {
            Log.d(TAG, "Последовательность упражнений завершена");
            // конец последовательности
            saveExerciseHistory();
            finish();
        }
    }

    public void goToPreviousFragment() {
        if (currentFragmentIndex == 0) {
            // на первом упражнении - закрытие активности
            finish();
        } else {
            // переход к предыдущему упражнению
            showFragment(currentFragmentIndex - 1);
        }
    }

    /**
     * Метод repeatGroundingSequence перезапускает всю последовательность упражнений.
     * Вызывается из последнего фрагмента по нажатию "Повторить".
     */
    public void repeatGroundingSequence() {
        saveExerciseHistory();
        currentSessionPhotos.clear();

        // удалить все существующие фрагменты из контейнера
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        for (Fragment fragment : fragmentInstances) {
            if (fragment != null && fragment.isAdded()) {
                transaction.remove(fragment);
            }
        }
        transaction.commitNow(); // commitNow() — синхронно, чтобы избежать гонки
        // (commitNow() вместо commit(),
        // чтобы гарантировать, что фрагменты удалятся до создания новых)

        // очистка списков
        fragmentInstances.clear();
        availablePhotosForSession.clear();

        // сброс индекса
        currentFragmentIndex = 0;

        // запуск новой сессии
        buildAndStartGroundSequence();

        /*
        // остановка текущего фрагмента
        if (currentFragment instanceof GroundBreathFragment) {
            ((GroundBreathFragment) currentFragment).onFragmentPaused();
        }

        // Очистка всех фрагментов
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        for (Fragment fragment : fragmentInstances) {
            if (fragment != null) {
                transaction.remove(fragment);
            }
        }
        transaction.commit();
        fragmentManager.executePendingTransactions();

        // Очистка списков
        fragmentInstances.clear();
        currentFragment = null;
        currentFragmentIndex = 0;

        // Запуск новой последовательности
        startGroundingSequence();*/
    }

    public void onPhotoUsed(DataManager.PhotoData photo) {
        if (photo != null) {
            currentSessionPhotos.add(photo);
            Log.d(TAG, "Фото использовано: " + photo.imgUrl);
        }
    }

    /**
     * Метод saveExerciseHistory сохраняет историю упражнений локально (данные о пройденных упражнениях:
     * какие изображения пройдены и в какое время)
     */
    private void saveExerciseHistory() {
        // сохранить только если есть фото
        if (currentSessionPhotos.isEmpty()) {
            Log.w(TAG, "Пропуск сохранения: последовательность упражнений не содержит фото");
            return;
        }

        // создать новую сессию с текущим временем и списком фото
        DataManager.ExerciseSession newSession = new DataManager.ExerciseSession(
                System.currentTimeMillis(),
                currentSessionPhotos // копия создаётся внутри конструктора
        );

        dataManager.addExerciseSessionAndSync(newSession);

        Log.d(TAG, "Сессия сохранена: " + currentSessionPhotos.size() + " фото");
    }

    public boolean isLastFragment() {
        return currentFragmentIndex == fragmentClasses.size() - 1;
    }

}