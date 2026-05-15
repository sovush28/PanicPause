package com.example.panicpause;

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

import java.util.Random;

public class GroundMathFragment extends Fragment {
    private ImageButton backBtn;
    private TextView instructionText;
    private TextView mathProblemText;
    private Button anotherExpressionBtn;
    private Button nextBtn;

    private int number1, number2;
    private String operation;

    private Random random;

    private Handler handler;
    private Runnable showButtonRunnable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ground_math, container, false);

        initializeViews(view);
        initializeComponents();
        setupButtonListeners();

        generateNewMathProblem();

        return view;
    }

    private void initializeViews(View view) {
        backBtn = view.findViewById(R.id.back_btn);
        instructionText = view.findViewById(R.id.instruction_text);
        mathProblemText = view.findViewById(R.id.math_problem_text);
        anotherExpressionBtn = view.findViewById(R.id.another_expression_btn);
        nextBtn = view.findViewById(R.id.next_btn);
    }
    private void initializeComponents() {
        random = new Random();
        handler = new Handler(Looper.getMainLooper());
        anotherExpressionBtn.setVisibility(View.INVISIBLE);
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
        anotherExpressionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateNewMathProblem();
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
    }

    public void onFragmentPaused() {
        // остановка таймеров, анимаций
    }

    /**
     * Метод generateNewMathProblem генерирует случайный математический пример, отображает его
     * и устанавливает появление кнопки "Другой пример" через 2 секунды
     */
    private void generateNewMathProblem() {
        // отменить существующие отсчеты
        if (showButtonRunnable != null) {
            handler.removeCallbacks(showButtonRunnable);
        }
        // спрятать кнопку "Другой пример"
        anotherExpressionBtn.setVisibility(View.INVISIBLE);
        
        // сгенерировать тип примера (0=сложение, 1=вычитание, 2=умножение)
        int operationType = random.nextInt(3);
        // сгенерировать числа
        switch (operationType) {
            case 0: // сложение
                operation = "+";
                number1 = random.nextInt(199) - 99; // от -99 до 99
                number2 = random.nextInt(199) - 99; // от -99 до 99
                break;
            case 1: // вычитание
                operation = "-";
                number1 = random.nextInt(199) - 99; // от -99 до 99
                number2 = random.nextInt(199) - 99; // от -99 до 99
                break;
            case 2: // умножение
                operation = "×";
                number1 = random.nextInt(199) - 99; // от -99 до 99
                number2 = random.nextInt(19) - 9;    // от -9 до 9
                break;
        }
        displayMathProblem();
        
        // настроить появление кнопки "Другой пример" через 2 секунды
        showButtonRunnable = new Runnable() {
            @Override
            public void run() {
                anotherExpressionBtn.setVisibility(View.VISIBLE);
            }
        };
        handler.postDelayed(showButtonRunnable, 2000); // задержка в 2 секунды
    }

    /**
     * Метод отображает математический пример. Формат: "number1 операция number2 = ?"
     */
    private void displayMathProblem() {
        String problemText;
        if(number2 < 0){
            problemText=number1 + " " + operation + " (" + number2 + ") = ?";
        }
        else{
            problemText=number1 + " " + operation + " " + number2 + " = ?";
        }
        mathProblemText.setText(problemText);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // очистить ресурсы для предотвращения утечек памяти
        if (showButtonRunnable != null) {
            handler.removeCallbacks(showButtonRunnable);
        }
    }
}