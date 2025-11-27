package com.example.safespace;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * GroundingActivity - the main activity that manages the grounding exercises sequence

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

    // Менеджер фрагментов для работы с экранами
    private FragmentManager fragmentManager;

    // Текущий индекс упражнения в последовательности
    private int currentFragmentIndex = 0;

    // Список для хранения созданных фрагментов
    private List<Fragment> fragmentInstances = new ArrayList<>();

    // Список классов фрагментов в правильном порядке
    private List<Class<? extends Fragment>> fragmentClasses = new ArrayList<>();
    
    // Array of fragment classes in the correct order
    /*private Class<? extends Fragment>[] fragmentClasses = new Class[]{
        GroundBreathFragment.class,
        GroundPhotoFragment.class,
        GroundMathFragment.class,
        GroundCountColorFragment.class,
        GroundPhotoFragment.class,
        GroundBreathFragment.class
    };*/

    // Настройки пользователя из Firestore
    private int groundPhotoExAmount = 2;
    private boolean useMath = true;
    private boolean useSearchObjectsColor = true;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Ключи для сохранения состояния
    private static final String KEY_CURRENT_INDEX = "current_fragment_index";
    private static final String KEY_FRAGMENT_TAGS = "fragment_tags";

    // Текущий активный фрагмент
    private Fragment currentFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ground);

        // Инициализация Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize fragment manager
        fragmentManager = getSupportFragmentManager();

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
        }

        // Handle system window insets (for edge-to-edge display)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Сохраняем текущее состояние при повороте экрана/изменении конфигурации
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_INDEX, currentFragmentIndex);
    }

    /**
     * Пытается восстановить ранее созданные фрагменты
     */
    private void restoreFragments() {
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
    }


    /**
     * Загружает настройки пользователя из Firestore и запускает последовательность упражнений
     */
    private void loadUserSettingsAndStartSequence(boolean useDefaultSettings) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated");
            finish();
            return;
        }
        if(!useDefaultSettings){
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();

                            // Получаем настройки пользователя, используя значения по умолчанию если данных нет
                            if(document.getLong("ground_photo_ex_amount") != null){
                                this.groundPhotoExAmount = document.getLong("ground_photo_ex_amount").intValue();
                            } else{
                                this.groundPhotoExAmount = 2;
                            }
                            if(document.getBoolean("use_math") != null){
                                this.useMath = document.getBoolean("use_math");
                            } else{
                                this.useMath = true;
                            }
                            if(document.getBoolean("use_search_objects_color") != null){
                                this.useSearchObjectsColor = document.getBoolean("use_search_objects_color");
                            } else{
                                this.useSearchObjectsColor = true;
                            }
                        /*
                        this.groundPhotoExAmount = document.getLong("ground_photo_ex_amount") != null ?
                                document.getLong("ground_photo_ex_amount").intValue() : 2;
                        this.useMath = document.getBoolean("use_math") != null ?
                                document.getBoolean("use_math") : true;
                        this.useSearchObjectsColor = document.getBoolean("use_search_objects_color") != null ?
                                document.getBoolean("use_search_objects_color") : true;
*/

                            // Строим последовательность упражнений согласно настройкам
                            buildExerciseSequence();

                            // Запускаем последовательность
                            startGroundingSequence();

                        } else {
                            Log.e(TAG, "Failed to load user settings", task.getException());
                            // Используем настройки по умолчанию если не удалось загрузить
                            buildExerciseSequence();
                            startGroundingSequence();
                        }
                    });
        }
        else{
            // Используем настройки по умолчанию
            buildExerciseSequence();
            startGroundingSequence();
        }
    }

    /**
     * Строит последовательность упражнений на основе настроек пользователя
     * - Дыхание всегда первое и последнее (breathRepeatAmount раз)
     * - Упражнения с фото: количество = groundPhotoExAmount
     * - Математика: если useMath = true
     * - Поиск цвета: если useSearchObjectsColor = true
     */
    private void buildExerciseSequence() {
        fragmentClasses.clear();
        fragmentInstances.clear();

        // 1. Первое упражнение - дыхание
        fragmentClasses.add(GroundBreathFragment.class);

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

    /**
     * Создает и сохраняет все фрагменты заранее с уникальными тегами
     */
    private void startGroundingSequence() {
        fragmentInstances.clear();
        try {
            // Создаем только первый фрагмент, остальные будут создаваться по мере необходимости
            // Это предотвращает одновременный запуск всех таймеров и анимаций
            if (!fragmentClasses.isEmpty()) {
                createAndAddFragment(0);
                showFragment(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating fragment instances", e);
        }
        /*fragmentInstances.clear();
        try {
            for (int i = 0; i < fragmentClasses.size(); i++) {
                createAndAddFragment(i);
            }
            showFragment(0);
        } catch (Exception e) {
            Log.e(TAG, "Error creating fragment instances", e);
        }*/
    }

    /**
     * Создает один фрагмент и добавляет его в список с уникальным тегом
     */
    private void createAndAddFragment(int index) {
        try {
            // Проверяем, не создан ли уже фрагмент для этого индекса
            if (index < fragmentInstances.size() && fragmentInstances.get(index) != null) {
                return; // Фрагмент уже создан
            }

            Fragment fragment = fragmentClasses.get(index).newInstance();

            // Устанавливаем уникальный тег для каждого фрагмента
            String tag = "exercise_fragment_" + index;

            // Если фрагмент еще не добавлен в FragmentManager, добавляем его
            if (fragmentManager.findFragmentByTag(tag) == null) {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.add(R.id.fragment_container, fragment, tag);
                transaction.hide(fragment); // Сначала скрываем
                transaction.commit();
                fragmentManager.executePendingTransactions();
            }

            // Добавляем в список, заполняя пропуски если нужно
            while (fragmentInstances.size() <= index) {
                fragmentInstances.add(null);
            }
            fragmentInstances.set(index, fragment);

            /*
            Fragment fragment = fragmentClasses.get(index).newInstance();

            // Устанавливаем уникальный тег для каждого фрагмента
            String tag = "exercise_fragment_" + index;

            // Если фрагмент еще не добавлен в FragmentManager, добавляем его
            if (fragmentManager.findFragmentByTag(tag) == null) {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.add(R.id.fragment_container, fragment, tag);
                transaction.hide(fragment); // Сначала скрываем
                transaction.commit();
                fragmentManager.executePendingTransactions();
            }

            fragmentInstances.add(fragment);
*/

        }
        catch (Exception e) {
            Log.e(TAG, "Error creating fragment at index " + index, e);
        }
    }

    /**
     * Создает экземпляры всех фрагментов и сохраняет их в список
     * (предотвращает пересоздание фрагментов при навигации)
     */
    private void createFragmentInstances() {
        try {
            for (Class<? extends Fragment> fragmentClass : fragmentClasses) {
                Fragment fragment = fragmentClass.newInstance();
                fragmentInstances.add(fragment);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating fragment instances", e);
        }
    }

    /**
     * Показывает фрагмент по указанному индексу
     * @param fragmentIndex индекс фрагмента в последовательности
     */
    private void showFragment(int fragmentIndex) {
        if (fragmentIndex < 0 || fragmentIndex >= fragmentClasses.size()) {
            Log.e(TAG, "Invalid fragment index: " + fragmentIndex);
            return;
        }

        try {
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            // Скрываем текущий фрагмент, если он существует
            if (currentFragment != null) {
                transaction.hide(currentFragment);

                // Оповещаем текущий фрагмент о том, что он становится невидимым
                // Это позволяет остановить таймеры и анимации
                if (currentFragment instanceof GroundBreathFragment) {
                    ((GroundBreathFragment) currentFragment).onFragmentPaused();
                }
                /*if (currentFragment instanceof GroundMathFragment) {
                    ((GroundMathFragment) currentFragment).onFragmentPaused();
                }*/
                // Добавьте аналогичные вызовы для других типов фрагментов
            }

            // Создаем фрагмент если он еще не создан
            if (fragmentIndex >= fragmentInstances.size() || fragmentInstances.get(fragmentIndex) == null) {
                createAndAddFragment(fragmentIndex);
            }

            // Получаем целевой фрагмент
            Fragment targetFragment = fragmentInstances.get(fragmentIndex);

            // Показываем целевой фрагмент
            transaction.show(targetFragment);
            transaction.commit();
            fragmentManager.executePendingTransactions();

            // Обновляем текущий фрагмент
            currentFragment = targetFragment;
            currentFragmentIndex = fragmentIndex;

            // Оповещаем новый фрагмент о том, что он стал видимым
            // Это позволяет запустить таймеры и анимации
            if (targetFragment instanceof GroundBreathFragment) {
                ((GroundBreathFragment) targetFragment).onFragmentResumed();
            }
            /*if (currentFragment instanceof GroundMathFragment) {
                ((GroundMathFragment) currentFragment).onFragmentResumed();
            }*/
            // Добавьте аналогичные вызовы для других типов фрагментов

            Log.d(TAG, "Showing fragment at index: " + fragmentIndex);
        }
        catch (Exception e) {
            Log.e(TAG, "Error showing fragment", e);
        }
    }

    public void goToNextFragment() {
        if (currentFragmentIndex < fragmentClasses.size() - 1) {
            // Переход к следующему упражнению
            showFragment(currentFragmentIndex + 1);
        } else {
            // Конец последовательности - сохраняем историю и закрываем
            saveExerciseHistory();
            finish();
        }
    }

    public void goToPreviousFragment() {
        if (currentFragmentIndex == 0) {
            // На первом упражнении - закрываем активность
            finish();
        } else {
            // Переход к предыдущему упражнению
            showFragment(currentFragmentIndex - 1);
        }
    }

    /**
     * Перезапускает всю последовательность упражнений
     * Вызывается из последнего фрагмента по нажатию "Повторить"
     */
    public void repeatGroundingSequence() {
        // Останавливаем текущий фрагмент
        if (currentFragment instanceof GroundBreathFragment) {
            ((GroundBreathFragment) currentFragment).onFragmentPaused();
        }

        // Очищаем все фрагменты
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        for (Fragment fragment : fragmentInstances) {
            if (fragment != null) {
                transaction.remove(fragment);
            }
        }
        transaction.commit();
        fragmentManager.executePendingTransactions();

        // Очищаем списки
        fragmentInstances.clear();
        currentFragment = null;
        currentFragmentIndex = 0;

        // Запускаем новую последовательность
        startGroundingSequence();
    }

    /**
     * Сохраняет историю выполнения упражнений в Firestore
     * метод будет расширен когда добавим функционал истории
     */
    private void saveExerciseHistory() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null)
            return;

        // TODO: Реализовать сохранение истории упражнений (данных о пройденных упражнениях)

        Log.d(TAG, "Saving exercise history for user: " + currentUser.getUid());
    }



    /**
     * Gets the current fragment index.
     * This can be useful for fragments to know their position in the sequence.
     * 
     * @return The current fragment index (0-5)
     */
    public int getCurrentFragmentIndex() {
        return currentFragmentIndex;
    }

    // Checks if the current fragment is the last one in the sequence
    public boolean isLastFragment() {
        return currentFragmentIndex == fragmentClasses.size() - 1;
    }

    public int getTotalExercisesCount() {
        return fragmentClasses.size();
    }

    @Override
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
    }

}