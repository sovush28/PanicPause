package com.example.safespace;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

/**
 * GroundBreathFragment - This fragment handles the square breathing exercise.
 * 
 * The square breathing exercise consists of 4 phases, each lasting 4 seconds:
 * 1. Inhale (4 seconds) - Square outline becomes darker from bottom-left corner
 * 2. Hold (4 seconds) - Continue drawing to top-left corner
 * 3. Exhale (4 seconds) - Continue drawing to top-right corner  
 * 4. Hold (4 seconds) - Complete the square by drawing to bottom-right corner
 * 
 * The animation shows a light-colored square outline (#CDC6A5) that gradually becomes
 * darker (#6F9283) as if a second square is being drawn over it.
 * 
 * The text in the center changes to show the current phase and countdown.
 */
public class GroundBreathFragment extends Fragment {

    // UI elements
    //private ImageButton backBtn;
    private Button nextBtn, repeatBtn;
    private ImageButton backBtn;
    private TextView instructionText, countdownText;

    ValueAnimator animator;

    //private AnimatedSquareView squareView;
    private View squareView;
    private Handler handler;
    private Runnable countdownRunnable;
    
    // Current phase of the breathing exercise (0-3)
    private int currentPhase = 0;
    
    // Text resources for each phase - will be initialized in onCreateView
    private String[] phaseInstructions;
    
    // Colors for the square animation
    private static final int LIGHT_COLOR = Color.parseColor("#CDC6A5");
    private static final int DARK_COLOR = Color.parseColor("#6F9283");
    
    // Animation duration for each side (4 seconds)
    private static final int ANIMATION_DURATION = 4000;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ground_breath, container, false);

        // Initialize phase instructions
        initializePhaseInstructions();
        
        // Initialize UI elements
        initializeViews(view);
        
        // Set up button click listeners
        setupButtonListeners();
        
        // Start the breathing exercise
        startBreathingExercise();

        return view;
    }

    /**
     * Initializes the phase instruction strings from resources.
     * This method must be called after the fragment is attached to the context.
     */
    private void initializePhaseInstructions() {
        phaseInstructions = new String[]{
            getString(R.string.breath_in),      // Inhale
            getString(R.string.breath_hold),    // Hold
            getString(R.string.breath_out),     // Exhale
            getString(R.string.breath_hold)     // Hold
        };
    }

    /**
     * Initializes all the UI elements from the layout.
     * 
     * @param view The root view of the fragment
     */
    private void initializeViews(View view) {
        backBtn = view.findViewById(R.id.back_btn);
        nextBtn = view.findViewById(R.id.next_btn);
        repeatBtn = view.findViewById(R.id.repeat_ground_btn);
        instructionText = view.findViewById(R.id.instruction_text);
        countdownText = view.findViewById(R.id.countdown_text);
        squareView = view.findViewById(R.id.square_view);
        
        // Initialize handler for countdown updates
        handler = new Handler(Looper.getMainLooper());
        
        // Set initial visibility of buttons
        // Repeat button should only be visible on the last breathing exercise (6th fragment)
        if (isLastBreathingExercise()) {
            repeatBtn.setVisibility(View.VISIBLE);
            nextBtn.setText(getString(R.string.end));
        } else {
            repeatBtn.setVisibility(View.GONE);
            nextBtn.setText(getString(R.string.next));
        }
    }

    /**
     * Sets up click listeners for all buttons.
     */
    private void setupButtonListeners() {
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

        // Repeat button - restarts the entire grounding sequence
        repeatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get reference to the parent activity and call its method
                if (getActivity() instanceof GroundActivity) {
                    GroundActivity activity = (GroundActivity) getActivity();
                    activity.repeatGroundingSequence();
                }
            }
        });
    }

    /**
     * Starts the square breathing exercise animation.
     * This method sets up the initial state and begins the animation cycle.
     */
    private void startBreathingExercise() {
        // Reset the current phase
        currentPhase = 0;
        
        // Set initial square color (light)
        //squareView.setBackgroundColor(LIGHT_COLOR);
        
        // Start the first phase
        startPhase(0);
    }

    /**
     * Starts a specific phase of the breathing exercise.
     * 
     * @param phase The phase number (0-3)
     */
    private void startPhase(int phase) {
        currentPhase = phase;
        
        // Update the instruction text
        instructionText.setText(phaseInstructions[phase]);
        
        // Start the countdown for this phase
        startCountdown();
        
        // Start the square animation for this phase
        //animateSquare();
    }

    /**
     * Starts the countdown timer for the current phase.
     * Updates the countdown text every second.
     */
    private void startCountdown() {
        // Cancel any existing countdown
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }
        
        // Create new countdown runnable
        countdownRunnable = new Runnable() {
            int countdown = 4; // Start from 4 seconds
            
            @Override
            public void run() { //TODO плавное затухание и смена текста
                if (countdown > 0) {
                    // Update countdown text
                    countdownText.setText(String.valueOf(countdown) + "...");
                    countdown--;
                    
                    // Schedule next update in 1 second
                    handler.postDelayed(this, 1000);
                } else {
                    // Phase completed, move to next phase
                    countdownText.setText("");
                    
                    if (currentPhase < 3) {
                        // Move to next phase
                        startPhase(currentPhase + 1);
                    } else {
                        currentPhase = 0;
                        startPhase(currentPhase);

                        //TODO nextBtn.visibility=visible

                        // All phases completed
                        //onBreathingExerciseCompleted();
                    }
                }
            }
        };
        
        // Start the countdown
        handler.post(countdownRunnable);
    }

    /**
     * Animates the square for the current phase.
     * Each phase animates one side of the square becoming darker.
     */
    private void animateSquare() {
        // Cancel any existing animation
        if (animator != null) {
            animator.cancel();
        }
        // Use a ValueAnimator instead
        ValueAnimator animator = ValueAnimator.ofFloat(0, 4 * squareView.getWidth());
        animator.setDuration(ANIMATION_DURATION * 4); // Total time for one full cycle
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedValue = (float) animation.getAnimatedValue();
                ((AnimatedSquareView) squareView).animateStroke(animatedValue);
            }
        });
        animator.start();

        // Create a value animator that will change the background color
        // We'll animate from light color to dark color over 4 seconds
        /*squareAnimator = ObjectAnimator.ofArgb(squareView, "backgroundColor", LIGHT_COLOR, DARK_COLOR);
        squareAnimator.setDuration(ANIMATION_DURATION);
        
        // Add animation listener to handle completion
        squareAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // Animation completed, but we don't need to do anything here
                // The countdown will handle moving to the next phase
            }
        });*/
        
        // Start the animation
        animator.start();
    }

    /**
     * Called when the breathing exercise is completed.
     * This happens after all 4 phases are finished.
     */
    private void onBreathingExerciseCompleted() {
        // Reset the square to light color
        //squareView.setBackgroundColor(LIGHT_COLOR);
        
        // Update instruction text
        instructionText.setText("Упражнение завершено");
        
        // Clear countdown text
        countdownText.setText("...");
        
        // The user can now proceed to the next exercise or repeat
        // The buttons are already set up to handle this
    }

    /**
     * Checks if this is the last breathing exercise in the sequence.
     * The last breathing exercise is the 6th fragment (index 5).
     * 
     * @return true if this is the last breathing exercise, false otherwise
     */
    private boolean isLastBreathingExercise() {
        if (getActivity() instanceof GroundActivity) {
            GroundActivity activity = (GroundActivity) getActivity();
            return activity.isLastFragment();
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Clean up resources to prevent memory leaks
        /*if (animator != null) {
            animator.cancel();
        }*/
        
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }
    }
}