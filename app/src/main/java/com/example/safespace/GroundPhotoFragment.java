package com.example.safespace;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
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
    private List<PhotoData> photoList;
    private PhotoData currentPhoto;
    
    // Random number generator for selecting photos
    private Random random;

    /**
     * Data class to hold photo information from Firestore
     */
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
        initializeViews(view);
        
        // Initialize Firebase and other components
        initializeComponents();
        
        // Set up button click listeners
        setupButtonListeners();
        
        // Load photos from Firestore
        loadPhotoFromFirestore();

        return view;
    }

    /**
     * Initializes all the UI elements from the layout.
     * 
     * @param view The root view of the fragment
     */
    private void initializeViews(View view) {
        backBtn = view.findViewById(R.id.back_btn);
        photoIV = view.findViewById(R.id.photo_iv);
        countThingsTV = view.findViewById(R.id.count_things_tv);
        nextBtn = view.findViewById(R.id.next_btn);
    }

    /**
     * Initializes Firebase Firestore and other components.
     */
    private void initializeComponents() {
        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance();
        
        // Initialize photo list
        photoList = new ArrayList<>();
        
        // Initialize random number generator
        random = new Random();
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
                            
                            // After loading all photos, display a random one
                            if (!photoList.isEmpty()) {
                                displayRandomPhoto();
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
     * Gets the number of photos loaded from Firestore.
     * 
     * @return The number of photos in the photo list
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