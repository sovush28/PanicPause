package com.example.safespace;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * GroundingActivity - This is the main activity that manages the grounding exercises sequence.
 * 
 * The grounding sequence consists of 6 exercises in this order:
 * 1. GroundBreathFragment - Square breathing exercise
 * 2. GroundPhotoFragment - Counting objects in a photo
 * 3. GroundMathFragment - Solving math problems
 * 4. GroundCountColorFragment - Finding objects of a specific color
 * 5. GroundPhotoFragment - Another photo counting exercise
 * 6. GroundBreathFragment - Another breathing exercise with repeat option
 * 
 * This activity handles navigation between fragments using buttons (back/next/repeat)
 * and manages the exercise sequence flow.
 */
public class GroundActivity extends AppCompatActivity {

    // Fragment manager to handle fragment transactions
    private FragmentManager fragmentManager;
    
    // Current fragment index in the sequence (0-5)
    private int currentFragmentIndex = 0;
    
    // Array of fragment classes in the correct order
    private Class<? extends Fragment>[] fragmentClasses = new Class[]{
        GroundBreathFragment.class,
        GroundPhotoFragment.class,
        GroundMathFragment.class,
        GroundCountColorFragment.class,
        GroundPhotoFragment.class,
        GroundBreathFragment.class
    };
    
    // Back button that will be shared across all fragments
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ground);

        // Initialize fragment manager
        fragmentManager = getSupportFragmentManager();
        
        // Get reference to the back button from the layout
        backButton = findViewById(R.id.back_btn);
        
        // Set up the back button click listener
        setupBackButton();
        
        // Start the grounding sequence with the first fragment
        startGroundingSequence();

        // Handle system window insets (for edge-to-edge display)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Sets up the back button functionality.
     * The back button behavior depends on which fragment is currently displayed:
     * - First fragment (index 0): Goes back to MainActivity
     * - Other fragments: Goes to the previous fragment in the sequence
     */
    private void setupBackButton() {
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFragmentIndex == 0) {
                    // If we're on the first fragment, go back to MainActivity
                    finish(); // This closes the current activity and returns to the previous one
                } else {
                    // Otherwise, go to the previous fragment
                    goToPreviousFragment();
                }
            }
        });
    }

    /**
     * Starts the grounding sequence by displaying the first fragment.
     * This method is called when the activity is created.
     */
    private void startGroundingSequence() {
        currentFragmentIndex = 0;
        showFragment(0);
    }

    /**
     * Shows a specific fragment based on its index in the sequence.
     * 
     * @param fragmentIndex The index of the fragment to show (0-5)
     */
    private void showFragment(int fragmentIndex) {
        try {
            // Create a new instance of the fragment class
            Fragment fragment = fragmentClasses[fragmentIndex].newInstance();
            
            // Start a fragment transaction
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            
            // Replace the current fragment with the new one
            // We use replace because we want only one fragment visible at a time
            transaction.replace(R.id.fragment_container, fragment);
            
            // Commit the transaction to make the changes visible
            transaction.commit();
            
            // Update the current fragment index
            currentFragmentIndex = fragmentIndex;
            
        } catch (Exception e) {
            // If something goes wrong, print the error
            e.printStackTrace();
        }
    }

    /**
     * Moves to the next fragment in the sequence.
     * This method is called by fragments when the "Next" button is pressed.
     */
    public void goToNextFragment() {
        if (currentFragmentIndex < fragmentClasses.length - 1) {
            // If we're not on the last fragment, go to the next one
            showFragment(currentFragmentIndex + 1);
        } else {
            // If we're on the last fragment, this shouldn't happen
            // because the last fragment should show "Finish" button instead of "Next"
            finish(); // Go back to MainActivity
        }
    }

    /**
     * Moves to the previous fragment in the sequence.
     * This method is called by the back button when not on the first fragment.
     */
    public void goToPreviousFragment() {
        if (currentFragmentIndex > 0) {
            // If we're not on the first fragment, go to the previous one
            showFragment(currentFragmentIndex - 1);
        }
    }

    /**
     * Restarts the grounding sequence from the beginning.
     * This method is called by the last fragment when "Repeat" button is pressed.
     * It generates new exercises by restarting the sequence.
     */
    public void repeatGroundingSequence() {
        startGroundingSequence();
    }

    /**
     * Finishes the grounding activity and returns to MainActivity.
     * This method is called by the last fragment when "Finish" button is pressed.
     */
    public void finishGrounding() {
        finish(); // Close this activity and return to MainActivity
    }

    /**
     * Gets the current fragment index.
     * This can be useful for fragments to know their position in the sequence.
     * 
     * @return The current fragment index (0-5)
     */
    public int getCurrentFragmentIndex() {
        return currentFragmentIndex;
    }

    /**
     * Checks if the current fragment is the last one in the sequence.
     * 
     * @return true if this is the last fragment, false otherwise
     */
    public boolean isLastFragment() {
        return currentFragmentIndex == fragmentClasses.length - 1;
    }
}