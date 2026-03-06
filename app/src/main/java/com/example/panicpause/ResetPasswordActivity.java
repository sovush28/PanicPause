package com.example.panicpause;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText emailET;
    private Button sendLinkBtn;
    private TextView cancelTV;
    private ImageButton backBtn;
    private FirebaseAuth mAuth;

    // Regex для валидации email
    private static final String EMAIL_PATTERN =
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                    "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset_password);

        mAuth=FirebaseAuth.getInstance();

        initializeViews();
        setOnClickListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews(){
        emailET=findViewById(R.id.email_et);
        sendLinkBtn=findViewById(R.id.send_link_btn);
        cancelTV=findViewById(R.id.cancel_tv);
        backBtn=findViewById(R.id.back_btn);
    }

    private void setOnClickListeners(){
        cancelTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        sendLinkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPasswResetLink();
            }
        });
    }

    private void sendPasswResetLink(){
        String email=emailET.getText().toString().trim();

        if(!validateEmail(email)){
            return;
        }

        ProgressDialogFragment progressDialog = new ProgressDialogFragment();
        try {
            progressDialog.show(getSupportFragmentManager(), "progress_dialog");
        }
        catch(IllegalStateException ex){
            // Обработка случая, когда Activity уничтожается
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }

        // ОТПРАВКА ЗАПРОСА В FIREBASE
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();

                    if (task.isSuccessful()) {
                        showMsgDialog();
                    } else {
                        Toast.makeText(this, getErrorMessage(task.getException()), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateEmail(String email) {
        // Проверка на пустоту
        if (email == null || email.isEmpty()) {
            Toast.makeText(this,R.string.enter_email, Toast.LENGTH_SHORT).show();
            return false;
        }

        // Проверка формата через regex
        if (!email.matches(EMAIL_PATTERN) ||
                !email.contains("@") || !email.contains(".")) {
            Toast.makeText(this,R.string.invalid_email_error, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private String getErrorMessage(Exception exception) {
        Log.e("ResetPassword", "Password reset error", exception);

        if (exception == null || exception.getMessage() == null) {
            return getString(R.string.unknown_error);
        }

        String errorMessage = exception.getMessage();
        if (exception instanceof FirebaseAuthInvalidUserException || errorMessage.contains("no user record")) {
            return getString(R.string.email_not_found);
        } else if (errorMessage.contains("badly formatted")) {
            return getString(R.string.invalid_email_error);
        } else if (errorMessage.contains("network")) {
            return getString(R.string.network_error);
        } else if(errorMessage.contains("auth credential is incorrect")){
            return getString(R.string.invalid_auth_data);
        }
        return getString(R.string.an_error_occured) + errorMessage;
    }

    private void showMsgDialog(){
        try {
            ResetPasswMsgDialogFragment dialog=new ResetPasswMsgDialogFragment();
            dialog.show(getSupportFragmentManager(), "reset_passw_dialog");
        }
        catch(IllegalStateException ex){
            // Обработка случая, когда Activity уничтожается
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }

}