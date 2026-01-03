package com.example.panicpause;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// Фрагмент - "мини-активность", которая может быть частью экрана
public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    Button panicBtn;
    LinearLayout historyLayout, groundSettingsLayout, whatsPALayout, howHelpYourselfLayout, whatsTriggerLayout;

    private DataManager dataManager;


    // метод создает внешний вид фрагмента
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // "Надуваем" макет из XML-файла fragment_home.xml
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        dataManager=new DataManager(requireContext());

        InitializeViews(view);
        SetOnClickListeners();

        return view;  // Возвращаем готовый экран
    }

    private void inDevelopmentToast(){
        Toast.makeText(getActivity(), R.string.in_development, Toast.LENGTH_SHORT).show();
    }

    private void InitializeViews(View view){
        panicBtn = view.findViewById(R.id.panic_btn);

        historyLayout=view.findViewById(R.id.history_layout);
        groundSettingsLayout=view.findViewById(R.id.ground_settings_layout);
        whatsPALayout=view.findViewById(R.id.whats_pa_layout);
        howHelpYourselfLayout=view.findViewById(R.id.how_to_help_yourself_layout);
        whatsTriggerLayout=view.findViewById(R.id.whats_trigger_layout);
    }

    private void SetOnClickListeners(){
        panicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkIfEnoughPhotos();
            }
        });

        historyLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inDevelopmentToast();
                //TODO
            }
        });

        groundSettingsLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToGroundSettingsActivity();
            }
        });

        whatsPALayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWhatsPADialog();
            }
        });

        howHelpYourselfLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToSelfHelpActivity();
            }
        });

        whatsTriggerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWhatsTriggerDialog();
            }
        });
    }

    private void checkIfEnoughPhotos(){
        try {
            // 1. Получаем настройки пользователя
            int requiredCount = dataManager.getGroundPhotoExAmount(); // ← нужно добавить метод в DataManager
            List<String> userTriggers = dataManager.getTriggers();     // ← нужно добавить метод в DataManager

            // 2. Загружаем все фото локально
            List<DataManager.PhotoData> allPhotos = dataManager.getLocalImagesList();

            // 3. Фильтруем, если есть триггеры
            List<DataManager.PhotoData> safePhotos = new ArrayList<>(allPhotos);
            if (userTriggers != null && !userTriggers.isEmpty()) {
                Iterator<DataManager.PhotoData> iterator = safePhotos.iterator();
                while (iterator.hasNext()) {
                    DataManager.PhotoData photo = iterator.next();
                    for (String trigger : userTriggers) {
                        if (photo.tags.contains(trigger)) {
                            iterator.remove();
                            break;
                        }
                    }
                }
            }

            // 4. Проверяем количество
            if (safePhotos.size() >= requiredCount) {
                Intent intent = new Intent(getActivity(), GroundActivity.class);
                intent.putExtra("default_settings", false);
                startActivity(intent);
            } else {
                showNotEnoughPhotosDialog();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking photos", e);
            // На всякий случай — разрешаем вход, если что-то пошло не так
            Intent intent = new Intent(getActivity(), GroundActivity.class);
            intent.putExtra("default_settings", true);
            startActivity(intent);
        }
    }

    private void showNotEnoughPhotosDialog(){
        try{
            NotEnoughPhotosDialogFragment dialog = new NotEnoughPhotosDialogFragment();
            dialog.show(getChildFragmentManager(),"dialog_no_photos_for_ground");
        }
        catch (IllegalStateException e){
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }

    private void showWhatsPADialog(){
        try{
            WhatsPADialogFragment dialog=new WhatsPADialogFragment();
            dialog.show(getActivity().getSupportFragmentManager(), "whats_pa_dialog");
        }
        catch(IllegalStateException ex){
            // Обработка случая, когда Activity уничтожается
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }

    private void showWhatsTriggerDialog(){
        try{
            WhatsTriggerDialogFragment dialog=new WhatsTriggerDialogFragment();
            dialog.show(getActivity().getSupportFragmentManager(), "whats_trigger_dialog");
        }
        catch(IllegalStateException ex){
            // Обработка случая, когда Activity уничтожается
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }

    private void goToSelfHelpActivity(){
        Intent intent = new Intent(getActivity(), SelfHelpActivity.class);
        startActivity(intent);
    }

    private void goToGroundSettingsActivity(){
        Intent intent = new Intent(getActivity(), GroundSettingsActivity.class);
        startActivity(intent);
    }

}