package com.example.panicpause;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
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
import java.util.Random;

/**
 * GroundPhotoFragment - This fragment displays a random photo from Firestore
 * and asks the user to count specific objects in the photograph.
 * 
 * The fragment:
 * 1. Connects to Firestore database
 * 2. Retrieves a collection of images with metadata
 * 3. Randomly selects one image to display
 * 4. Shows the image with instructions to count specific objects
 * 5. Provides navigation buttons (back/next)
 * 
 * Each document in the Firestore collection should contain:
 * - img_url (string): URL of the image
 * - tags (string array): Tags describing the image
 * - word (string): Word describing what objects to count
 */
public class GroundPhotoFragment extends Fragment {

    // UI elements
    private ImageView photoIV;
    private TextView countThingsTV;
    private Button nextBtn;
    ImageButton backBtn;
    
    // Firebase and data
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private List<PhotoData> photoList;
    private PhotoData currentPhoto;
    
    // Random number generator for selecting photos
    private Random random;

    // класс для данных об изображениях из БД Firestore
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ground_photo, container, false);

        // Initialize UI elements
        backBtn = view.findViewById(R.id.back_btn);
        photoIV = view.findViewById(R.id.photo_iv);
        countThingsTV = view.findViewById(R.id.count_things_tv);
        nextBtn = view.findViewById(R.id.next_btn);

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize photo list
        photoList = new ArrayList<>();

        // Initialize random number generator
        random = new Random();
        
        // Set up button click listeners
        setupButtonListeners();

        // Load photos from Firestore
        loadPhotoFromFirestore();

        return view;
    }

    /**
     * Sets up click listeners for all buttons.
     */
    private void setupButtonListeners() {
        // Back button - handled by the activity, but we can add fragment-specific logic here if needed
         backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get reference to the parent activity and call its method
                if (getActivity() instanceof GroundActivity) {
                    GroundActivity activity = (GroundActivity) getActivity();
                    activity.goToPreviousFragment();
                }
            }
        });
        // Next button - moves to the next fragment
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get reference to the parent activity and call its method
                if (getActivity() instanceof GroundActivity) {
                    GroundActivity activity = (GroundActivity) getActivity();
                    activity.goToNextFragment();
                }
            }
        });
    }

    // загружает данные о всех фото из Firestore в список photoList
    private void loadPhotoFromFirestore() {
        // отображение состояния загрузки
        countThingsTV.setText(getString(R.string.photo_loading));
        
        // запрос к коллекции "images"
        firestore.collection("images")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            // очистка существующего списка
                            photoList.clear();
                            
                            // для каждого документа в коллекции
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    // взять данные из документа
                                    String imgUrl = document.getString("img_url");
                                    String word = document.getString("word");
                                    List<String> tags = (List<String>) document.get("tags");
                                    
                                    // убедиться что все поля есть
                                    if (imgUrl != null && word != null && tags != null) {
                                        // создать объект класса PhotoData и добавить в список
                                        PhotoData photoData = new PhotoData(imgUrl, word, tags);
                                        photoList.add(photoData);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            // запустил ли пользователь активность с настройками по умолчанию
                            Intent intent = getActivity().getIntent();
                            boolean useDefaultSettings = intent != null &&
                                    intent.hasExtra("default_settings") &&
                                    intent.getBooleanExtra("default_settings", false);

                            if (useDefaultSettings) {
                                // не фильтровать список фото
                                if (!photoList.isEmpty()) {
                                    displayRandomPhoto();
                                } else {
                                    countThingsTV.setText(getString(R.string.photo_not_found));
                                }
                            } else {
                                filterTriggerPhotos();
                            }

                            if (photoList.isEmpty()) {
                                countThingsTV.setText(getString(R.string.photo_not_found));
                            }
                            
                        } else {
                            // ошибка загрузки фото из БД
                            countThingsTV.setText(getString(R.string.photo_load_error));
                        }
                    }
                });
    }

    private void filterTriggerPhotos(){
        FirebaseUser user = mAuth.getCurrentUser();
        if(user==null){
            Toast.makeText(getActivity(), "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = getActivity().getIntent();
        if(intent!=null & intent.hasExtra("default_settings")){
            if(intent.getBooleanExtra("default_settings", true)==true)
                return;
        }

        firestore.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                List<String> userTriggerList = (List<String>) document.get("triggers");
                                if (userTriggerList != null && !userTriggerList.isEmpty()) {
                                    // у пользователя выбраны триггеры, нужно отфильтровать список фото
                                    removePhotosWithTriggers(userTriggerList);
                                }
                                // отображение фото
                                if (!photoList.isEmpty()) {
                                    displayRandomPhoto();
                                }
                            } else {
                                // документа пользователя не существует
                                if (!photoList.isEmpty()) {
                                    displayRandomPhoto();
                                } else {
                                    countThingsTV.setText(getString(R.string.photo_not_found));
                                }
                            }
                        } else {
                            // ошибка загрузки документа пользователя
                            if (!photoList.isEmpty()) {
                                displayRandomPhoto();
                            } else {
                                countThingsTV.setText(getString(R.string.photo_not_found));
                            }
                            Log.e("GroundPhotoFragment", "Error loading user triggers", task.getException());
                        }
                    }
                });
    }

    // удаление объектов, содержащих теги-триггеры
    private void removePhotosWithTriggers(List<String> userTriggerList){
        Iterator<PhotoData> iterator = photoList.iterator();

        while (iterator.hasNext()) {
            PhotoData photoData = iterator.next();
            boolean containsTrigger = false;

            // проверка, есть ли у этого фото какой-либо тег, который совпадает с каким-либо триггером пользователя
            for (String trigger : userTriggerList) {
                if (photoData.tags.contains(trigger)) {
                    containsTrigger = true;
                    break;
                }
            }

            // удаление фото из списка
            if (containsTrigger) {
                iterator.remove();
            }
        }

        // отображение случайного фото
        if (!photoList.isEmpty()) {
            displayRandomPhoto();
        }
    }

    // выбирает случайный объект из photoList и отображает соотв. фото в ImageView
    private void displayRandomPhoto() {
        if (photoList.isEmpty()) {
            countThingsTV.setText(getString(R.string.photo_not_found));
            return;
        }
        
        // случайное фото из списка
        int randomIndex = random.nextInt(photoList.size());
        currentPhoto = photoList.get(randomIndex);
        
        // обновление текста упражнения
        String instruction = getString(R.string.ground_count_img1) +
            " " + currentPhoto.word + " " + getString(R.string.ground_count_img2);
        countThingsTV.setText(instruction);

        // загрузка изображения с помощью библиотеки Glide
        Glide.with(this)
                .load(currentPhoto.imgUrl)
                //.placeholder(R.drawable.placeholder_image) // Show placeholder while loading
                //.error(R.drawable.error_image) // Show error image if loading fails
                .into(photoIV);

        //тест с енотами (ЗАМЕНИТЬ НА ЗАКОМЕНТИРОВАННОЕ СВЕРХУ)
        /*
        Glide.with(this)
                .load("https://raw.githubusercontent.com/sovush28/PanicPauseImages/refs/heads/main/fr0ggy5-MZrt80_mD7M-unsplash.jpg")
                //.placeholder(R.drawable.placeholder_image) // Show placeholder while loading
                //.error(R.drawable.error_image) // Show error image if loading fails
                .into(photoIV);
        */
    }

    /**
     * Gets the current photo data.
     * This can be useful for debugging or if other parts of the app need this information.
     * 
     * @return The current PhotoData object, or null if no photo is loaded
     */
    public PhotoData getCurrentPhoto() {
        return currentPhoto;
    }

    /**
     * Gets the number of photos loaded from Firestore
     */
    public int getPhotoCount() {
        return photoList.size();
    }

    /**
     * Reloads a new random photo.
     * This method can be called if the user wants to see a different photo.
     */
    public void loadNewRandomPhoto() {
        if (!photoList.isEmpty()) {
            displayRandomPhoto();
        }
    }
}