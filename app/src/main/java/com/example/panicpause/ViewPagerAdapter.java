package com.example.panicpause;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Класс ViewPagerAdapter упрлавляет страницами на главной активности.
 */
public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // position - номер страницы (0 = первая страница, 1 = вторая страница)
        // если position = 0 (первая страница), создать и возвращать фрагмент главной страницы
        // если position = 1 (вторая страница), создать и возвращать фрагмент профиля
        // если position имеет другое значение, возвращать главную страницу
        switch (position) {
            case 0:
                return new HomeFragment();
            case 1:
                return new ProfileFragment();
            default:
                return new HomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}