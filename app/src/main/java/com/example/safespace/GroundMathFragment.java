package com.example.safespace;

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

/**
 * GroundMathFragment - This fragment displays random math problems for the user to solve.
 * 
 * The fragment generates random math problems with the following rules:
 * - Operations: addition (+), subtraction (-), or multiplication (×)
 * - For addition/subtraction: both numbers are random between -99 and 99
 * - For multiplication: one number is between -99 and 99, the other is between -9 and 9
 * 
 * Features:
 * - Displays "Try to solve:" instruction
 * - Shows the math problem in format: "number1 operation number2 = ?"
 * - After 2 seconds, shows "Another Expression" button to generate new problem
 * - Provides navigation buttons (back/next)
 */
public class GroundMathFragment extends Fragment {


    private ImageButton backBtn;
    private TextView instructionText;
    private TextView mathProblemText;
    private Button anotherExpressionBtn;
    private Button nextBtn;
    
    // Math problem components
    private int number1, number2;
    private String operation;
    private int correctAnswer; /////////////
    
    // Random number generator
    private Random random;
    
    // Handler for delayed button appearance
    private Handler handler;
    private Runnable showButtonRunnable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ground_math, container, false);

        // Initialize UI elements
        initializeViews(view);
        
        // Initialize components
        initializeComponents();
        
        // Set up button click listeners
        setupButtonListeners();
        
        // Generate and display the first math problem
        generateNewMathProblem();

        return view;
    }

    /**
     * Initializes all the UI elements from the layout.
     * 
     * @param view The root view of the fragment
     */
    private void initializeViews(View view) {
        backBtn = view.findViewById(R.id.back_btn);
        instructionText = view.findViewById(R.id.instruction_text);
        mathProblemText = view.findViewById(R.id.math_problem_text);
        anotherExpressionBtn = view.findViewById(R.id.another_expression_btn);
        nextBtn = view.findViewById(R.id.next_btn);
    }

    /**
     * Initializes random number generator and handler.
     */
    private void initializeComponents() {
        // Initialize random number generator
        random = new Random();
        
        // Initialize handler for delayed operations
        handler = new Handler(Looper.getMainLooper());
        
        // Initially hide the "Another Expression" button
        anotherExpressionBtn.setVisibility(View.INVISIBLE);
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
        // Another Expression button - generates a new math problem
        anotherExpressionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateNewMathProblem();
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

    public void onFragmentResumed() {
        generateNewMathProblem();
    }

    public void onFragmentPaused() {
        // Остановка таймеров, анимаций
    }

    /**
     * Generates a new random math problem and displays it.
     * This method creates a new problem, displays it, and schedules the "Another Expression"
     * button to appear after 2 seconds.
     */
    private void generateNewMathProblem() {
        // Cancel any existing delayed button appearance
        if (showButtonRunnable != null) {
            handler.removeCallbacks(showButtonRunnable);
        }
        
        // Hide the "Another Expression" button initially
        anotherExpressionBtn.setVisibility(View.INVISIBLE);
        
        // Generate random operation (0=addition, 1=subtraction, 2=multiplication)
        int operationType = random.nextInt(3);
        
        // Generate numbers based on operation type
        switch (operationType) {
            case 0: // Addition
                operation = "+";
                number1 = random.nextInt(199) - 99; // -99 to 99
                number2 = random.nextInt(199) - 99; // -99 to 99
                correctAnswer = number1 + number2; //////////////////
                break;
                
            case 1: // Subtraction
                operation = "-";
                number1 = random.nextInt(199) - 99; // -99 to 99
                number2 = random.nextInt(199) - 99; // -99 to 99
                correctAnswer = number1 - number2; ///////////////////////
                break;
                
            case 2: // Multiplication
                operation = "×";
                number1 = random.nextInt(199) - 99; // -99 to 99
                number2 = random.nextInt(19) - 9;    // -9 to 9
                correctAnswer = number1 * number2; //////////////////////
                break;
        }
        
        // Display the math problem
        displayMathProblem();
        
        // Schedule the "Another Expression" button to appear after 2 seconds
        showButtonRunnable = new Runnable() {
            @Override
            public void run() {
                anotherExpressionBtn.setVisibility(View.VISIBLE);
            }
        };
        handler.postDelayed(showButtonRunnable, 2000); // 2 seconds delay
    }

    /**
     * Displays the current math problem in the text view.
     * The format is: "number1 operation number2 = ?"
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

    /**
     * Gets the correct answer to the current math problem.
     * This can be useful for debugging or if the app needs to verify answers.
     * 
     * @return The correct answer to the current math problem
     */
    public int getCorrectAnswer() {
        return correctAnswer;
    }

    /**
     * Gets the current math problem as a string.
     * 
     * @return The current math problem in format "number1 operation number2 = ?"
     */
    public String getCurrentProblem() {
        return number1 + " " + operation + " " + number2 + " = ?";
    }

    /**
     * Checks if the given answer is correct.
     * 
     * @param answer The answer to check
     * @return true if the answer is correct, false otherwise
     */
    public boolean isAnswerCorrect(int answer) {
        return answer == correctAnswer;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Clean up resources to prevent memory leaks
        if (showButtonRunnable != null) {
            handler.removeCallbacks(showButtonRunnable);
        }
    }
}