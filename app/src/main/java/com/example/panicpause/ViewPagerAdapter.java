package com.example.panicpause;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

// класс ViewPagerAdapter, который будет управлять экранами
// Он "наследует" от FragmentStateAdapter - это готовый шаблон для работы с ViewPager2
public class ViewPagerAdapter extends FragmentStateAdapter {

    // конструктор класса - специальный метод, который вызывается при создании адаптера
    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);  // Передаем активность родительскому классу
    }

    // метод создает фрагменты (экраны) для каждой позиции
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // position - номер страницы (0 = первая страница, 1 = вторая страница)
        // Если position = 0 (первая страница), создаем и возвращаем фрагмент главной страницы
        // Если position = 1 (вторая страница), создаем и возвращаем фрагмент профиля
        // Если position имеет другое значение (на всякий случай), все равно возвращаем главную страницу
        switch (position) {
            case 0:
                return new HomeFragment();
            case 1:
                return new ProfileFragment();
            default:
                return new HomeFragment();
        }
    }

    // метод возвращает общее количество страниц
    @Override
    public int getItemCount() {
        return 2;
    }
}