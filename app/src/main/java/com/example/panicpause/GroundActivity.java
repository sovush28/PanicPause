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
 * GroundActivity - the main activity that manages the grounding exercises sequence

 * The grounding sequence consists of 6 exercises in this order:
 * 1. GroundBreathFragment - Square breathing exercise
 * 2. GroundPhotoFragment - Counting objects in a photo
 * 3. GroundMathFragment - Solving math problems
 * 4. GroundCountColorFragment - Finding objects of a specific color
 * 5. GroundPhotoFragment - Another photo counting exercise
 * 6. GroundBreathFragment - Another breathing exercise with repeat option

 * This activity handles navigation between fragments using buttons (back/next/repeat)
 * and manages the exercise sequence flow.
 */
public class GroundActivity extends AppCompatActivity {
    private static final String TAG = "GroundActivity";

    // Менеджер фрагментов для работы с экранами
    private FragmentManager fragmentManager;

    // Текущий индекс упражнения в последовательности
    private int currentFragmentIndex = 0;

    // Список для хранения созданных фрагментов
    private List<Fragment> fragmentInstances = new ArrayList<>();

    // Список классов фрагментов в правильном порядке
    private List<Class<? extends Fragment>> fragmentClasses = new ArrayList<>();

    private DataManager dataManager;

    // Список использованных фото в текущей сессии
    private List<DataManager.PhotoData> currentSessionPhotos = new ArrayList<>();

    // поле для хранения доступных фото
    private List<DataManager.PhotoData> availablePhotosForSession = new ArrayList<>();


    private boolean useDefaultSettings = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ground);

        dataManager=new DataManager(this);
        fragmentManager = getSupportFragmentManager();



        Intent intent = getIntent();
        if(intent!=null && intent.hasExtra("default_settings")){
            useDefaultSettings = intent.getBooleanExtra("default_settings", false);
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

        // инициализация уникального пула фото для сессии
        initializeAvailablePhotos();

        startGroundingSequence();
    }

    private void initializeAvailablePhotos() {
        List<DataManager.PhotoData> allPhotos = dataManager.getLocalImagesList();
        availablePhotosForSession = new ArrayList<>(allPhotos);

        // Фильтрация по триггерам — только если НЕ используется режим по умолчанию
        if (!useDefaultSettings) {
            List<String> triggers = dataManager.getTriggers();
            if (triggers != null && !triggers.isEmpty()) {
                availablePhotosForSession.removeIf(photo ->
                        photo.tags.stream().anyMatch(triggers::contains)
                );
            }
        }

        // Перемешиваем, чтобы порядок был случайным
        Collections.shuffle(availablePhotosForSession);
    }

    /**
     * Метод для получения уникального фото
     * Возвращает следующее уникальное фото из пула или null, если фото закончились.
     */
    public DataManager.PhotoData getNextUniquePhoto() {
        if (availablePhotosForSession.isEmpty()) {
            return null;
        }
        // Берём первое и удаляем, чтобы не повторилось
        return availablePhotosForSession.remove(0);
    }

    //Пытается восстановить ранее созданные фрагменты
    /*private void restoreFragments() {
        fragmentInstances.clear();

        // Ищем фрагменты в FragmentManager по тегам
        for (int i = 0; i < fragmentClasses.size(); i++) {
            String tag = "exercise_fragment_" + i;
            Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment != null) {
                fragmentInstances.add(fragment);
            } else {
                // Если фрагмент не найден, создаем новый
                createAndAddFragment(i);
            }
        }

        // Показываем текущий фрагмент
        if (!fragmentInstances.isEmpty() && currentFragmentIndex < fragmentInstances.size()) {
            showFragment(currentFragmentIndex);
        }
    }*/

    //Строит последовательность упражнений на основе настроек пользователя
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

            // 3. Математические примеры (если включены)
            if (useMath) {
                fragmentClasses.add(GroundMathFragment.class);
            }

            // 4. Поиск предметов по цвету (если включен)
            if (useSearchObjectsColor) {
                fragmentClasses.add(GroundCountColorFragment.class);
            }

            // 5. Упражнение с фотографией
            fragmentClasses.add(GroundPhotoFragment.class);
        }

        // Последнее упражнение - дыхание
        fragmentClasses.add(GroundBreathFragment.class);

        Log.d(TAG, "Built exercise sequence with " + fragmentClasses.size() + " exercises");
    }

    //Создает и сохраняет все фрагменты заранее с уникальными тегами
    private void startGroundingSequence() {
        if (fragmentClasses.isEmpty()) {
            Toast.makeText(this, "Ошибка: нет упражнений", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        try {
            // Создаём все фрагменты заранее
            fragmentInstances.clear();
            for (int i = 0; i < fragmentClasses.size(); i++) {
                try {
                    Fragment fragment = fragmentClasses.get(i).newInstance();
                    if (fragment instanceof GroundPhotoFragment) {
                        DataManager.PhotoData assignedPhoto = getNextUniquePhoto();
                        ((GroundPhotoFragment) fragment).assignPhoto(assignedPhoto);
                    }

                    /*Fragment fragment = fragmentClasses.get(i).newInstance();
                    // Передаём флаг useDefaultSettings в фрагменты, если они его поддерживают
                    if (fragment instanceof GroundPhotoFragment) {
                        ((GroundPhotoFragment) fragment).setUseDefaultSettings(useDefaultSettings);
                    }*/

                    fragmentInstances.add(fragment);
                }
                catch (Exception e) {
                    Log.e(TAG, "Error creating fragment at index " + i, e);
                    finish();
                    return;
                }
            }
            showFragment(0);

            // Создаем только первый фрагмент, остальные будут создаваться по мере необходимости
            /*if (!fragmentClasses.isEmpty()) {
                //createAndAddFragment(0);
                showFragment(0);
            }
            else{
                Toast.makeText(this, "Ошибка: нет упражнений", Toast.LENGTH_SHORT).show();
                finish();
            }*/
        } catch (Exception e) {
            Log.e(TAG, "Error creating fragment instances", e);
        }
    }

    //Создает один фрагмент и добавляет его в список с уникальным тегом
    /*private void createAndAddFragment(int index) {
        try {
            // не создан ли уже фрагмент для этого индекса
            if (index < fragmentInstances.size() && fragmentInstances.get(index) != null) {
                return; // уже создан
            }

            Fragment fragment = fragmentClasses.get(index).newInstance();

            // уникальный тег для каждого фрагмента
            String tag = "exercise_fragment_" + index;

            // добавление фрагмента в FragmentManager, если ещё не добавлен
            if (fragmentManager.findFragmentByTag(tag) == null) {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.add(R.id.fragment_container, fragment, tag);
                transaction.hide(fragment); // Сначала скрываем
                transaction.commit();
                fragmentManager.executePendingTransactions();
            }

            // Добавление в список, заполняя пропуски если нужно
            while (fragmentInstances.size() <= index) {
                fragmentInstances.add(null);
            }
            fragmentInstances.set(index, fragment);

        }
        catch (Exception e) {
            Log.e(TAG, "Error creating fragment at index " + index, e);
        }
    }*/

    //Показывает фрагмент по указанному индексу в последовательности
    private void showFragment(int fragmentIndex) {
        if (fragmentIndex < 0 || fragmentIndex >= fragmentClasses.size()) {
            Log.e(TAG, "Invalid fragment index: " + fragmentIndex);
            return;
        }

        Fragment targetFragment = fragmentInstances.get(fragmentIndex);
        androidx.fragment.app.FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Скрываем текущий фрагмент (если есть)
        if (currentFragmentIndex < fragmentInstances.size()) {
            Fragment current = fragmentInstances.get(currentFragmentIndex);
            if (current.isAdded()) {
                transaction.hide(current);
                if (current instanceof GroundBreathFragment) {
                    ((GroundBreathFragment) current).onFragmentPaused();
                }
            }
        }

        // Показываем целевой
        if (targetFragment.isAdded()) {
            transaction.show(targetFragment);
        } else {
            transaction.add(R.id.fragment_container, targetFragment);
        }

        transaction.commit();
        fragmentManager.executePendingTransactions();

        // сначала обновляем индекс
        currentFragmentIndex = fragmentIndex;

        // ЗАТЕМ обновляем кнопки — теперь isLastFragment() вернёт правильное значение
        if (targetFragment instanceof GroundBreathFragment) {
            ((GroundBreathFragment) targetFragment).onFragmentResumed();
            ((GroundBreathFragment) targetFragment).updateButtonsForPosition();
        }

        currentFragmentIndex = fragmentIndex;
        Log.d(TAG, "Showing fragment at index: " + fragmentIndex);

        /*try {
            Fragment currentFragment = fragmentClasses.get(fragmentIndex).newInstance();

            FragmentTransaction transaction = fragmentManager.beginTransaction();

            // Скрытие текущего фрагмента, если он существует
            if (currentFragment != null) {
                transaction.hide(currentFragment);

                // Оповещаем текущий фрагмент о том, что он становится невидимым
                // Это позволяет остановить таймеры и анимации
                if (currentFragment instanceof GroundBreathFragment) {
                    ((GroundBreathFragment) currentFragment).onFragmentPaused();
                }

            }

            // Создание фрагмента, если еще не создан
            if (fragmentIndex >= fragmentInstances.size() || fragmentInstances.get(fragmentIndex) == null) {
                createAndAddFragment(fragmentIndex);
            }

            // Получение целевого фрагмента
            Fragment targetFragment = fragmentInstances.get(fragmentIndex);

            // Отображение целевого фрагмента
            transaction.show(targetFragment);
            transaction.commit();
            fragmentManager.executePendingTransactions();

            // Обновление текущего фрагмента
            currentFragment = targetFragment;
            currentFragmentIndex = fragmentIndex;

            // Оповещаем новый фрагмент о том, что он стал видимым
            // Это позволяет запустить таймеры и анимации
            if (targetFragment instanceof GroundBreathFragment) {
                ((GroundBreathFragment) targetFragment).onFragmentResumed();
            }

            Log.d(TAG, "Showing fragment at index: " + fragmentIndex);
        }
        catch (Exception e) {
            Log.e(TAG, "Error showing fragment at index"+fragmentIndex, e);
            finish();
        }*/
    }

    public void goToNextFragment() {
        if (currentFragmentIndex < fragmentClasses.size() - 1) {
            // Переход к следующему упражнению
            showFragment(currentFragmentIndex + 1);
        } else {
            Log.d(TAG, "Grounding sequence completed");
            // Конец последовательности - сохранение истории и закрытие
            saveExerciseHistory();
            finish();
        }
    }

    public void goToPreviousFragment() {
        if (currentFragmentIndex == 0) {
            // На первом упражнении - закрытие активности
            finish();
        } else {
            // Переход к предыдущему упражнению
            showFragment(currentFragmentIndex - 1);
        }
    }

    //Перезапускает всю последовательность упражнений
    //вызывается из последнего фрагмента по нажатию "Повторить"
    public void repeatGroundingSequence() {
        saveExerciseHistory();
        currentSessionPhotos.clear();

        // === УДАЛЯЕМ ВСЕ СУЩЕСТВУЮЩИЕ ФРАГМЕНТЫ ИЗ КОНТЕЙНЕРА ===
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        for (Fragment fragment : fragmentInstances) {
            if (fragment != null && fragment.isAdded()) {
                transaction.remove(fragment);
            }
        }
        transaction.commitNow(); // commitNow() — синхронно, чтобы избежать race condition
        // commitNow() вместо commit(),
        // чтобы гарантировать, что фрагменты удалятся до создания новых


        // Очищаем списки
        fragmentInstances.clear();
        availablePhotosForSession.clear();

        // Сбрасываем индекс
        currentFragmentIndex = 0;

        // Запускаем новую сессию
        buildAndStartGroundSequence();

        // Остановка текущего фрагмента
        /*if (currentFragment instanceof GroundBreathFragment) {
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

    // получение данных о фото из фрагмента
    public void onPhotoUsed(DataManager.PhotoData photo) {
        if (photo != null) {
            currentSessionPhotos.add(photo);
            Log.d(TAG, "Photo used: " + photo.imgUrl);
        }
    }

    //Сохраняет историю выполнения упражнений в Firestore
    private void saveExerciseHistory() {

        // TODO: Реализовать сохранение истории упражнений (данных о пройденных упражнениях: какие изображения пройдены и в какое время)
        Log.d(TAG, "Saving exercise history with " + currentSessionPhotos.size() + " photos");
    }

    // Checks if the current fragment is the last one in the sequence
    public boolean isLastFragment() {
        return currentFragmentIndex == fragmentClasses.size() - 1;
    }

    // Геттер для фрагментов (альтернатива, если не передавать через setUseDefaultSettings)
    public boolean isUsingDefaultSettings() {
        return useDefaultSettings;
    }

    /*public int getTotalExercisesCount() {
        return fragmentClasses.size();
    }*/

    /*@Override
    protected void onPause() {
        super.onPause();
        // При сворачивании приложения останавливаем текущий фрагмент
        if (currentFragment instanceof GroundBreathFragment) {
            ((GroundBreathFragment) currentFragment).onFragmentPaused();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // При возобновлении приложения запускаем текущий фрагмент
        if (currentFragment instanceof GroundBreathFragment) {
            ((GroundBreathFragment) currentFragment).onFragmentResumed();
        }
    }*/

}