package com.example.safespace;

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
import androidx.core.content.ContextCompat;
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
import com.google.firebase.firestore.auth.User;

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

    /**
     * Loads photos from Firestore database.
     * This method connects to the "images" collection and retrieves all documents.
     */
    private void loadPhotoFromFirestore() {
        // Show loading state
        countThingsTV.setText(getString(R.string.photo_loading));
        
        // Query the "images" collection in Firestore
        firestore.collection("images")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
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
                                        PhotoData photoData = new PhotoData(imgUrl, word, tags);
                                        photoList.add(photoData);
                                    }
                                } catch (Exception e) {
                                    // If there's an error processing a document, skip it
                                    e.printStackTrace();
                                }
                            }

                            // Check if we're using default settings first
                            Intent intent = getActivity().getIntent();
                            boolean useDefaultSettings = intent != null &&
                                    intent.hasExtra("default_settings") &&
                                    intent.getBooleanExtra("default_settings", false);

                            if (useDefaultSettings) {
                                // If using default settings, display photo immediately without filtering
                                if (!photoList.isEmpty()) {
                                    displayRandomPhoto();
                                } else {
                                    countThingsTV.setText(getString(R.string.photo_not_found));
                                }
                            } else {
                                // Only filter if NOT using default settings
                                filterTriggerPhotos();
                            }

                            // After loading all photos, display a random one
                            if (!photoList.isEmpty()) {
                                //displayRandomPhoto(); //НЕ НУЖНО (иначе фото генерируется два раза - тут первый неотфильтррованный и позже второй после фильтрации)
                            } else {
                                // No photos found, show error message
                                countThingsTV.setText(getString(R.string.photo_not_found));
                            }
                            
                        } else {
                            // Error loading photos from Firestore
                            countThingsTV.setText(getString(R.string.photo_load_error));
                        }
                    }
                });
    }

    //removes the photos with current user's triggers tags from the photoList
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
                                    // User has triggers, filter photos
                                    removePhotosWithTriggers(userTriggerList);
                                }
                                // Whether we filtered or not, display a photo
                                if (!photoList.isEmpty()) {
                                    displayRandomPhoto();
                                } else {
                                    //countThingsTV.setText(getString(R.string.no_safe_photos));
                                }
                            } else {
                                // User document doesn't exist, display any photo
                                if (!photoList.isEmpty()) {
                                    displayRandomPhoto();
                                } else {
                                    countThingsTV.setText(getString(R.string.photo_not_found));
                                }
                            }
                        } else {
                            // Error loading user document, display any photo as fallback
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

    private void removePhotosWithTriggers(List<String> userTriggerList){
        // Use iterator for safe removal during iteration
        Iterator<PhotoData> iterator = photoList.iterator();

        while (iterator.hasNext()) {
            PhotoData photoData = iterator.next();
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

        // After filtering, display a random photo
        if (!photoList.isEmpty()) {
            displayRandomPhoto();
        } else {
            // All photos were filtered out - show appropriate message
            //countThingsTV.setText(getString(R.string.no_safe_photos));
            // You might want to show a default image or handle this case differently
        }
    }

    /**
     * Displays a randomly selected photo from the loaded photo list.
     * This method selects a random photo and loads it into the ImageView.
     */
    private void displayRandomPhoto() {
        if (photoList.isEmpty()) {
            countThingsTV.setText(getString(R.string.photo_not_found));
            return;
        }
        
        // Select a random photo from the list
        int randomIndex = random.nextInt(photoList.size());
        currentPhoto = photoList.get(randomIndex);
        
        // Update instruction text with the word to count
        String instruction = getString(R.string.ground_count_img1) +
            " " + currentPhoto.word + " " + getString(R.string.ground_count_img2);
        countThingsTV.setText(instruction);

        // Load the image using Glide library
        // Glide handles image loading, caching, and error handling automatically
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