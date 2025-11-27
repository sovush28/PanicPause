package com.example.safespace;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// Фрагмент - "мини-активность", которая может быть частью экрана
public class HomeFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    Button panicBtn;

    TextView whatsPATV, howHelpYourselfTV, whatsTriggerTV;

    // Data class to hold photo information from Firestore
    private static class PhotoData {
        String imgUrl;
        String word;
        List<String> tags;

        PhotoData(String imgUrl, String word, List<String> tags) {
            this.imgUrl = imgUrl;
            this.word = word;
            this.tags = tags;
        }
    }
    private List<PhotoData> photoList = new ArrayList<>();
    //int countSafePhotos = 0;
    //int countAllPhotos = 0, countUnsafePhotos = 0;
    //boolean areTherePhotosToShow = true;

    // Interface for async photo check callback
    interface PhotoCheckCallback {
        void onResult(boolean enoughPhotos);
    }

    // метод создает внешний вид фрагмента
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // "Надуваем" макет из XML-файла fragment_home.xml
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        panicBtn = view.findViewById(R.id.panic_btn);
        panicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if there are enough photos asynchronously
                checkIfEnoughPhotos(new PhotoCheckCallback() {
                    @Override
                    public void onResult(boolean enoughPhotos) {
                        if (enoughPhotos){
                            Intent intent = new Intent(getActivity(), GroundActivity.class);
                            intent.putExtra("default_settings", false);
                            startActivity(intent);
                        }
                        else{
                            showNotEnoughPhotosDialog();
                        }
                    }
                });
            }
        });

        whatsPATV=view.findViewById(R.id.whats_pa_tv);
        whatsPATV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWhatsPADialog();
            }
        });

        howHelpYourselfTV=view.findViewById(R.id.how_to_help_yourself_tv);
        howHelpYourselfTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToSelfHelpActivity();
            }
        });

        whatsTriggerTV=view.findViewById(R.id.whats_trigger_tv);
        whatsTriggerTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWhatsTriggerDialog();
            }
        });

        return view;  // Возвращаем готовый экран
    }

    /////////////////////////// конец oncreate

    private void showNotEnoughPhotosDialog(){
        try{
            NotEnoughPhotosDialogFragment dialog = new NotEnoughPhotosDialogFragment();
            dialog.show(getChildFragmentManager(),"dialog_no_photos_for_ground");
        }
        catch (IllegalStateException e){
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }

    /** Asynchronously check if there are enough photos for grounding exercises */
    private void checkIfEnoughPhotos(PhotoCheckCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            callback.onResult(false);
            return;
        }

        db.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                List<String> userTriggerList = (List<String>) document.get("triggers");

                                // Handle Long to int conversion properly
                                Long groundPhotoExAmountLong = document.getLong("ground_photo_ex_amount");
                                int userPhotoExerciseAmount = 2; // default value

                                if (groundPhotoExAmountLong != null) {
                                    userPhotoExerciseAmount = groundPhotoExAmountLong.intValue();
                                }

                                if (userTriggerList != null && !userTriggerList.isEmpty()) {
                                    // User has triggers, need to filter photos
                                    loadAndFilterPhotos(userTriggerList, userPhotoExerciseAmount, callback);
                                } else {
                                    // User has no triggers, use all photos
                                    loadAllPhotos(userPhotoExerciseAmount, callback);
                                }
                            } else {
                                // User document doesn't exist, use default
                                loadAllPhotos(2, callback);
                            }
                        } else {
                            // Error loading user document, use default
                            loadAllPhotos(2, callback);
                            //TODO log
                        }
                    }
                });
    }

    // Load all photos and check count without filtering
    private void loadAllPhotos(int requiredCount, PhotoCheckCallback callback) {
        db.collection("images")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int totalPhotos = task.getResult().size();
                            callback.onResult(totalPhotos >= requiredCount);
                        } else {
                            // Error loading photos
                            callback.onResult(false);
                        }
                    }
                });
    }

    /** Load and filter photos based on user triggers */
    private void loadAndFilterPhotos(List<String> userTriggerList, int requiredCount, PhotoCheckCallback callback) {
        db.collection("images")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            photoList.clear();

                            // Load all photos
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    String imgUrl = document.getString("img_url");
                                    String word = document.getString("word");
                                    List<String> tags = (List<String>) document.get("tags");

                                    if (imgUrl != null && word != null && tags != null) {
                                        PhotoData photoData = new PhotoData(imgUrl, word, tags);
                                        photoList.add(photoData);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            // Filter out unsafe photos
                            removeUnsafePhotos(userTriggerList);

                            // Check if we have enough safe photos
                            boolean enoughPhotos = photoList.size() >= requiredCount;
                            callback.onResult(enoughPhotos);

                        } else {
                            // Error loading photos
                            callback.onResult(false);
                        }
                    }
                });
    }

    // removed due to asynchronous issues
    /*
    private boolean areTherePhotosToShow(){
        //boolean areTherePhotosToShow = true;

        FirebaseUser user=mAuth.getCurrentUser();

        db.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful() && task.getResult() != null){
                            DocumentSnapshot document = task.getResult();
                            if(document.exists()){
                                List<String> userTriggerList = (List<String>) document.get("triggers");
                                int userPhotoExerciseAmount = (int)document.get("ground_photo_ex_amount");
                                if(userTriggerList!=null){ //если у юзера выбраны триггеры
                                    countAllPhotos();
                                    removeUnsafePhotos(userTriggerList);
                                    countSafePhotos = photoList.size();

                                    if (countSafePhotos < userPhotoExerciseAmount){
                                        areTherePhotosToShow=false;
                                    }
                                }
                            }
                        }
                    }
                });

        return areTherePhotosToShow;
    }
*/

    // removed due to being called separately
    /*
    private void countAllPhotos(){

        db.collection("images")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            // Clear existing photo list
                            photoList.clear();

                            // Process each document in the collection
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    // Extract data from the document
                                    String imgUrl = document.getString("img_url");
                                    String word = document.getString("word");

                                    // Get tags array (convert to List<String>)
                                    List<String> tags = (List<String>) document.get("tags");

                                    // Validate that all required fields are present
                                    if (imgUrl != null && word != null && tags != null) {
                                        // Create PhotoData object and add to list
                                        HomeFragment.PhotoData photoData = new HomeFragment.PhotoData(imgUrl, word, tags);
                                        photoList.add(photoData);
                                    }
                                }
                                catch (Exception e) {
                                    // If there's an error processing a document, skip it
                                    e.printStackTrace();
                                }

                                //countAllPhotos = photoList.size();

                            }
                        }
                    }
                });
    }
    */

    private void removeUnsafePhotos(List<String> userTriggerList){
        Iterator<HomeFragment.PhotoData> iterator = photoList.iterator();

        while (iterator.hasNext()) {
            HomeFragment.PhotoData photoData = iterator.next();
            boolean containsTrigger = false;

            // Check if this photo has ANY tag that matches ANY user trigger
            for (String trigger : userTriggerList) {
                if (photoData.tags.contains(trigger)) {
                    containsTrigger = true;
                    break; // No need to check other triggers for this photo
                }
            }

            // Remove photo if it contains ANY matching trigger
            if (containsTrigger) {
                iterator.remove();
            }
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
        //TODO плавный переход
    }

    ////////
}