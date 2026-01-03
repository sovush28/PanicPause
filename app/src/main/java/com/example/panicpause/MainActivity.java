package com.example.panicpause;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity{

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationV;
    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.view_pager);
        bottomNavigationV = findViewById(R.id.bottom_navigation);

        setupViewPager();
        setupBottomNavigation();

        dataManager=new DataManager(this);
        // Инициализируем контент (фото + JSON)
        dataManager.initializeContent(this::onContentReady);

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getBooleanExtra("go_to_profile", false)) {
                viewPager.post(() -> viewPager.setCurrentItem(1, false));
            } else if (intent.getBooleanExtra("go_to_home", false)) {
                viewPager.post(() -> viewPager.setCurrentItem(0, false));
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
        
        // Exclude bottom navigation from window insets to prevent extra padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_navigation), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // важно: обновить текущий intent

        if (intent.getBooleanExtra("go_to_profile", false)) {
            // Переключиться на ProfileFragment
            viewPager.setCurrentItem(1, false); // false = без анимации
        }
        else if (intent.getBooleanExtra("go_to_home", false)) {
            // Переключиться на HomeFragment
            viewPager.setCurrentItem(0, false);
        }

        // проверка нет ли новых фото в базе
        dataManager.initializeContent(this::onContentReady);
    }

    private void onContentReady() {
        if (dataManager.isOnboardingCompleted()) {
            // Пользователь уже прошёл онбординг — показываем главное меню
            viewPager.setCurrentItem(0);
        } else {
            // Первый запуск — показываем выбор: гость или вход
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish(); // чтобы нельзя было вернуться назад
        }
    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // отключение свайпов
        // viewPager.setUserInputEnabled(false);

        // Слушатель изменения страницы для синхронизации с BottomNavigationV
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0:
                        bottomNavigationV.setSelectedItemId(R.id.navigation_home);
                        break;
                    case 1:
                        bottomNavigationV.setSelectedItemId(R.id.navigation_profile);
                        break;
                }
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationV.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home) {
                    viewPager.setCurrentItem(0, true); // true для плавной анимации
                    return true;
                } else if (itemId == R.id.navigation_profile) {
                    viewPager.setCurrentItem(1, true);
                    return true;
                }
                return false;
            }
        });
    }

    // Блокируем кнопку "Назад" на главном экране
    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            // Если на главной - выходим из приложения
            super.onBackPressed();
        } else {
            // Если на профиле - возвращаем на главную
            viewPager.setCurrentItem(0, true);
        }
    }

}