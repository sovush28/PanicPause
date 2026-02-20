package com.example.panicpause;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
    private Button nextBtn, repeatBtn;
    private ImageButton backBtn;
    private TextView instructionText, countdownText;

    ValueAnimator squareAnimator;
    private AnimatedSquareView squareView;
    private Handler handler;
    private Runnable countdownRunnable;
    
    // Current phase of the breathing exercise (0-3)
    private int currentPhase = 0;
    
    // Text resources for each phase - will be initialized in onCreateView
    private String[] phaseInstructions;
    
    // Animation duration for each side (4 seconds)
    private static final int ANIMATION_DURATION = 4000;

    private boolean breathingStarted = false; // флаг, чтобы не запускать дважды

    private int breathRepeatCount;
    private int userBreathRepeatAmount;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ground_breath, container, false);

        initializePhaseInstructions();
        initializeViews(view);
        setupButtonListeners();

        breathRepeatCount = 0;

        DataManager dataManager = new DataManager(requireContext());
        userBreathRepeatAmount= dataManager.getBreathRepeatAmount();

        return view;
    }

    public void updateButtonsForPosition() {
        if (isAdded() && getActivity() != null) {
            updateButtonVisibility();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateButtonVisibility();

        // Запускаем упражнение ТОЛЬКО после создания View
        if (!breathingStarted) {
            startBreathingExercise();
            squareAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (currentPhase == 3) {
                        // Если последний этап

                        //начинаем анимацию заново
                        startSquareAnimation();
                    }
                }
            });
            breathingStarted = true;
        }
    }

    /**
     * Initializes the phase instruction strings from resources.
     * This method must be called after the fragment is attached to the context.
     */
    private void initializePhaseInstructions() {
        // Убедимся, что фрагмент прикреплён к активности
        if (getActivity() == null) {
            // Безопасный fallback — но на практике не должно происходить
            phaseInstructions = new String[]{
                    getString(R.string.breath_in),      // Inhale
                    getString(R.string.breath_hold),    // Hold
                    getString(R.string.breath_out),     // Exhale
                    getString(R.string.breath_hold)     // Hold
            };
            return;
        }

        phaseInstructions = new String[]{
            getString(R.string.breath_in),      // Inhale
            getString(R.string.breath_hold),    // Hold
            getString(R.string.breath_out),     // Exhale
            getString(R.string.breath_hold)     // Hold
        };
    }

    private void initializeViews(View view) {
        backBtn = view.findViewById(R.id.back_btn);
        nextBtn = view.findViewById(R.id.next_btn);
        repeatBtn = view.findViewById(R.id.repeat_ground_btn);
        instructionText = view.findViewById(R.id.instruction_text);
        countdownText = view.findViewById(R.id.countdown_text);
        squareView = view.findViewById(R.id.square_view);
        
        // Initialize handler for countdown updates
        handler = new Handler(Looper.getMainLooper());
    }

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

    private void updateButtonVisibility() {
        if (getActivity() instanceof GroundActivity) {
            GroundActivity activity = (GroundActivity) getActivity();

            if (activity.isLastFragment()){
                nextBtn.setText(getString(R.string.end));

                if (breathRepeatCount >= userBreathRepeatAmount){
                    repeatBtn.setVisibility(View.VISIBLE);
                    nextBtn.setVisibility(View.VISIBLE);
                }
                else{
                    repeatBtn.setVisibility(View.INVISIBLE);
                    nextBtn.setVisibility(View.INVISIBLE);
                }
            }
            else{
                repeatBtn.setVisibility(View.GONE);

                if (breathRepeatCount >= userBreathRepeatAmount){
                    nextBtn.setVisibility(View.VISIBLE);
                }
                else{
                    nextBtn.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    /*private void updateButtonVisibility() {
        if (getActivity() instanceof GroundActivity) {
            GroundActivity activity = (GroundActivity) getActivity();
            if (activity.isLastFragment()) {
                repeatBtn.setVisibility(View.VISIBLE);
                nextBtn.setText(getString(R.string.end));
            } else {
                repeatBtn.setVisibility(View.GONE);
                nextBtn.setText(getString(R.string.next));
            }
        }
    }*/

    public void onFragmentResumed(){
        super.onResume();
        updateButtonVisibility();
        //startBreathingExercise();
    }

    public void onFragmentPaused() {
        super.onPause();
        // Остановка таймеров, анимаций
    }

    /**
     * Starts the square breathing exercise animation.
     * This method sets up the initial state and begins the animation cycle.
     */
    private void startBreathingExercise() {

        // Reset the current phase
        currentPhase = 0;

        // Сбрасываем анимацию
        if (squareAnimator != null) {
            squareAnimator.cancel();
        }
        
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
        startSquareAnimation();
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
                        // All phases completed - restart from beginning
                        currentPhase = 0;
                        startPhase(currentPhase);

                        breathRepeatCount++;

                        updateButtonVisibility();

                    }
                }
            }
        };
        
        // Start the countdown
        handler.post(countdownRunnable);
    }

    /**
     * Starts the square animation for the current phase.
     */
    private void startSquareAnimation() {
        // Останавливаем предыдущую анимацию
        if (squareAnimator != null) {
            squareAnimator.cancel();
        }

        // Создаем новую анимацию
        squareAnimator = ValueAnimator.ofFloat(0f, 1f);
        squareAnimator.setDuration(ANIMATION_DURATION);

        // Рассчитываем прогресс для текущего этапа
        float startProgress = currentPhase * 0.25f;
        float endProgress = startProgress + 0.25f;

        squareAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                float animatedValue = (Float) animation.getAnimatedValue();
                // Вычисляем общий прогресс анимации
                float progress = startProgress + animatedValue * 0.25f;
                // Устанавливаем прогресс для квадрата
                squareView.setProgress(progress);
            }
        });

        // Запускаем анимацию
        squareAnimator.start();
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