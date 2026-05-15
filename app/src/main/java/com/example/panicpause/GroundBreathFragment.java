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
 * GroundBreathFragment - фрагмент, отвечающий за упражнение "Дыхание по квадрату".
 * Упражнение состоит из 4 фаз, каждая по 4 секунды.
 */
public class GroundBreathFragment extends Fragment {
    private Button nextBtn, repeatBtn;
    private ImageButton backBtn;
    private TextView instructionText, countdownText;

    ValueAnimator squareAnimator;
    private AnimatedSquareView squareView;
    private Handler handler;
    private Runnable countdownRunnable;
    
    // текущая фаза (0-3)
    private int currentPhase = 0;
    
    // текст для каждой фазы
    private String[] phaseInstructions;
    
    // длительность анимации для каждой стороны квадрата
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

        // запуск упражнение только после создания View
        if (!breathingStarted) {
            startBreathingExercise();
            squareAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (currentPhase == 3) {
                        // если последний этап, начинаем анимацию заново
                        startSquareAnimation();
                    }
                }
            });
            breathingStarted = true;
        }
    }

    /**
     * Метод initializePhaseInstructions достает строки для инструкции из strings.res
     */
    private void initializePhaseInstructions() {
        // убедиться, что фрагмент прикреплён к активности
        if (getActivity() == null) {
            // безопасный fallback
            phaseInstructions = new String[]{
                    getString(R.string.breath_in),
                    getString(R.string.breath_hold),
                    getString(R.string.breath_out),
                    getString(R.string.breath_hold)
            };
            return;
        }
        phaseInstructions = new String[]{
            getString(R.string.breath_in),
            getString(R.string.breath_hold),
            getString(R.string.breath_out),
            getString(R.string.breath_hold)
        };
    }

    private void initializeViews(View view) {
        backBtn = view.findViewById(R.id.back_btn);
        nextBtn = view.findViewById(R.id.next_btn);
        repeatBtn = view.findViewById(R.id.repeat_ground_btn);
        instructionText = view.findViewById(R.id.instruction_text);
        countdownText = view.findViewById(R.id.countdown_text);
        squareView = view.findViewById(R.id.square_view);

        handler = new Handler(Looper.getMainLooper());
    }

    private void setupButtonListeners() {
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof GroundActivity) {
                    GroundActivity activity = (GroundActivity) getActivity();
                    activity.goToPreviousFragment();
                }
            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof GroundActivity) {
                    GroundActivity activity = (GroundActivity) getActivity();
                    activity.goToNextFragment();
                }
            }
        });

        repeatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

    public void onFragmentResumed(){
        super.onResume();
        updateButtonVisibility();
    }

    public void onFragmentPaused() {
        super.onPause(); // остановка таймеров, анимаций
    }

    private void startBreathingExercise() {
        currentPhase = 0;
        if (squareAnimator != null) {
            squareAnimator.cancel();
        }
        startPhase(0);
    }

    private void startPhase(int phase) {
        currentPhase = phase;

        instructionText.setText(phaseInstructions[phase]);

        startCountdown();

        startSquareAnimation();
    }

    /**
     * Метод startCountdown запускает таймер для текущей фазы.
     * Обновляет отсчет (текст) каждую секунду.
     */
    private void startCountdown() {
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }

        countdownRunnable = new Runnable() {
            int countdown = 4;
            
            @Override
            public void run() {
                if (countdown > 0) {
                    countdownText.setText(String.valueOf(countdown) + "...");
                    countdown--;
                    // установить следующее обновление через 1 секунду
                    handler.postDelayed(this, 1000);
                } else {
                    countdownText.setText("");
                    if (currentPhase < 3) {
                        startPhase(currentPhase + 1);
                    } else {
                        currentPhase = 0;
                        startPhase(currentPhase);

                        breathRepeatCount++;

                        updateButtonVisibility();
                    }
                }
            }
        };
        handler.post(countdownRunnable);
    }

    /**
     * Начинает анимацию квадрата для тек. фазы
     */
    private void startSquareAnimation() {
        if (squareAnimator != null) {
            squareAnimator.cancel();
        }

        squareAnimator = ValueAnimator.ofFloat(0f, 1f);
        squareAnimator.setDuration(ANIMATION_DURATION);

        // рассчитать прогресс для текущего этапа
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
        squareAnimator.start();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }
    }
}