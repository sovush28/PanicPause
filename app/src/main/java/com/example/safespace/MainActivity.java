package com.example.safespace;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity{

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationV;

    //private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        //// Initialize Firebase Auth
        //mAuth = FirebaseAuth.getInstance();
        ////If a user has signed in successfully you can get their account data at any point with the getCurrentUser method.
        /*
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Name, email address, and profile photo Url
            String name = user.getDisplayName();
            String email = user.getEmail();
            Uri photoUrl = user.getPhotoUrl();

            // Check if user's email is verified
            boolean emailVerified = user.isEmailVerified();

            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getIdToken() instead.
            String uid = user.getUid();
        }
        */

        // initialize views
        viewPager = findViewById(R.id.view_pager);
        bottomNavigationV = findViewById(R.id.bottom_navigation);

        setupViewPager();
        setupBottomNavigation();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Отключаем свайпы если нужно
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