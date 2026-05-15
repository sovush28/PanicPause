package com.example.panicpause;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    Button panicBtn;
    LinearLayout historyLayout, groundSettingsLayout, whatsPALayout, howHelpYourselfLayout, whatsTriggerLayout;
    private DataManager dataManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        dataManager=new DataManager(requireContext());

        InitializeViews(view);
        SetOnClickListeners();

        return view;
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
                if(!dataManager.getUseFavesOnly()){
                    checkIfEnoughPhotos();
                }
                else{
                    checkIfEnoughFaves();
                }
            }
        });
        historyLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToHistoryActivity();
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

    private void checkIfEnoughFaves(){
        if(dataManager.getFaves().size() >= dataManager.getGroundPhotoExAmount()){
            Intent intent = new Intent(getActivity(), GroundActivity.class);
            intent.putExtra("faves_only", true);
            startActivity(intent);
        }
        else{
            showNotEnoughFavesDialog();
        }
    }

    private void showNotEnoughFavesDialog(){
        try {
            NotEnoughFavesDialogFragment dialog = new NotEnoughFavesDialogFragment();
            dialog.show(getChildFragmentManager(),"dialog_no_faves_for_ground");
        }
        catch (IllegalStateException e){
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }

    private void checkIfEnoughPhotos(){
        try {
            // 1. получичть настройки пользователя
            int requiredCount = dataManager.getGroundPhotoExAmount();
            List<String> userTriggers = dataManager.getTriggers();

            // 2. загрузить все фото локально
            List<DataManager.PhotoData> allPhotos = dataManager.getLocalImagesList();

            List<DataManager.PhotoData> safePhotos = new ArrayList<>(allPhotos);
            // 3. отфильтровать, если есть триггеры
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

            // 4. проверить количество
            if (safePhotos.size() >= requiredCount) {
                Intent intent = new Intent(getActivity(), GroundActivity.class);
                intent.putExtra("default_settings", false);
                startActivity(intent);
            } else {
                showNotEnoughPhotosDialog();
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке безопасных фото", e);
            // на всякий случай - разрешить вход, если что-то пошло не так
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
            // обработка случая, когда Activity уничтожается
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }

    private void showWhatsPADialog(){
        try{
            WhatsPADialogFragment dialog=new WhatsPADialogFragment();
            dialog.show(getActivity().getSupportFragmentManager(), "whats_pa_dialog");
        }
        catch(IllegalStateException ex){
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }

    private void showWhatsTriggerDialog(){
        try{
            WhatsTriggerDialogFragment dialog=new WhatsTriggerDialogFragment();
            dialog.show(getActivity().getSupportFragmentManager(), "whats_trigger_dialog");
        }
        catch(IllegalStateException ex){
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }

    private void goToHistoryActivity(){
        Intent intent = new Intent(getActivity(), HistoryActivity.class);
        startActivity(intent);
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