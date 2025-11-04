package com.example.safespace;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.Random;

/**
 * GroundCountColorFragment - This fragment displays a color counting exercise.
 * 
 * The fragment:
 * 1. Displays text asking the user to find 5 objects of a specific color
 * 2. Randomly selects a color from the available color names in strings.xml
 * 3. Shows the color name in bold text
 * 4. Provides buttons for navigation (back, another color, next)
 * 
 * The exercise helps users focus on their surroundings and practice mindfulness
 * by looking for specific colored objects around them.
 */
public class GroundCountColorFragment extends Fragment {

    // UI elements
    private TextView colorText;
    private Button anotherColorBtn;
    private Button nextBtn;
    ImageButton backBtn;
    
    // Color names from strings.xml
    private String[] colorNames;
    private String[] colorStringIds;
    
    // Current selected color
    private String currentColor;
    
    // Random number generator
    private Random random;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ground_count_color, container, false);

        // Initialize UI elements
        initializeViews(view);
        
        // Initialize color arrays
        initializeColorArrays();
        
        // Initialize random number generator
        random = new Random();
        
        // Set up button click listeners
        setupButtonListeners();
        
        // Generate and display the first random color
        generateNewRandomColor();

        return view;
    }

    /**
     * Initializes all the UI elements from the layout.
     * 
     * @param view The root view of the fragment
     */
    private void initializeViews(View view) {
        backBtn = view.findViewById(R.id.back_btn);
        colorText = view.findViewById(R.id.count_color_tv);
        anotherColorBtn = view.findViewById(R.id.another_color_btn);
        nextBtn = view.findViewById(R.id.next_btn);
    }

    /**
     * Initializes the arrays containing color names and their string resource IDs.
     * This method sets up all the available colors that can be randomly selected.
     */
    private void initializeColorArrays() {
        // Array of color names (these should match the string resource names)
        colorStringIds = new String[]{
            "ground_color_green",
            "ground_color_red", 
            "ground_color_yellow",
            "ground_color_blue",
            "ground_color_light_blue",
            "ground_color_white",
            "ground_color_black",
            "ground_color_gray",
            "ground_color_brown",
            "ground_color_orange",
            "ground_color_pink",
            "ground_color_purple"
        };
        
        // Array to store the actual color names retrieved from strings.xml
        colorNames = new String[colorStringIds.length];
        
        // Load color names from string resources
        for (int i = 0; i < colorStringIds.length; i++) {
            int resourceId = getResources().getIdentifier(colorStringIds[i], "string", getContext().getPackageName());
            if (resourceId != 0) {
                colorNames[i] = getString(resourceId);
            } else {
                // Fallback color names if string resources are not found
                colorNames[i] = "цвета"; //????
            }
        }
    }

    /**
     * Sets up click listeners for all buttons.
     */
    private void setupButtonListeners() {
        // Back button - handled by the activity
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
        // Another Color button - generates a new random color
        anotherColorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateNewRandomColor();
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
     * Generates a new random color and updates the display.
     * This method randomly selects a color from the available colors and
     * updates the UI to show the new color name.
     */
    private void generateNewRandomColor() {
        if (colorNames.length == 0) {
            // No colors available, show default text
            colorText.setText("цвета");
            currentColor = "цвета";
            return;
        }
        
        // Select a random color index
        int randomIndex = random.nextInt(colorNames.length);
        currentColor = colorNames[randomIndex];
        
        // Update the color text to show the selected color
        colorText.setText(getString(R.string.ground_count_color1) +
        " " + currentColor + " "+ getString(R.string.ground_count_color2_bold)+
        " "+ getString(R.string.ground_count_color3));
        //TODO сделать current color + ground_count_color2_bold жирными
    }

    /**
     * Gets the currently selected color name.
     * 
     * @return The current color name
     */
    public String getCurrentColor() {
        return currentColor;
    }

    /**
     * Gets the number of available colors.
     * 
     * @return The number of colors in the color array
     */
    public int getColorCount() {
        return colorNames.length;
    }

    /**
     * Gets all available color names.
     * 
     * @return Array of all available color names
     */
    public String[] getAllColors() {
        return colorNames.clone(); // Return a copy to prevent external modification
    }

    /**
     * Sets a specific color by name.
     * This method can be used to set a particular color instead of a random one.
     * 
     * @param colorName The name of the color to set
     * @return true if the color was found and set, false otherwise
     */
    public boolean setSpecificColor(String colorName) {
        for (String color : colorNames) {
            if (color.equals(colorName)) {
                currentColor = colorName;
                colorText.setText(currentColor);
                return true;
            }
        }
        return false; // Color not found
    }
}