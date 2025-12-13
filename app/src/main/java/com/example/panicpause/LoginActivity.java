package com.example.panicpause;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/*
После входа/регистрации вызывается dataManager.handleUserLogin().
Все переходы в MainActivity — только после завершения синхронизации.
Гость — просто закрытие активности (локальные данные уже управляются DataManager).
 */

public class LoginActivity extends AppCompatActivity {

    ImageButton backBtn;
    EditText emailET, passwET;
    TextView forgotPasswTV, createAccTV;
    Button logInBtn, logInAsGuestBtn;
    LinearLayout guestBtnLayout;

    private FirebaseAuth mAuth; //Declare an instance of FirebaseAuth
    private DataManager dataManager;

    //private FirebaseFirestore db;

    private boolean fromAccSettings=false;

    // Для показа прогресса в build.gradle (module:app) dependencies добавить:
    // implementation 'com.github.ybq:Android-SpinKit:1.4.0'
    // далее отмечено так /////////////////////

    //private LoadingDialog loadingDialog; //////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Инициализация Firebase
        mAuth = FirebaseAuth.getInstance();
        //db = FirebaseFirestore.getInstance();
        dataManager=new DataManager(this);
        //loadingDialog = new LoadingDialog(this); //////////////////////

        InitializeViews();
        SetOnClickListeners();

        Intent intent= getIntent();
        if(intent.hasExtra("from_acc_settings")){
            fromAccSettings = intent.getBooleanExtra("from_acc_settings", false);
            if(fromAccSettings){
                ShowBackHideGuest();
            } else {
                HideBackBtnShowGuest();
            }
        } else{
            HideBackBtnShowGuest();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void HideBackBtnShowGuest(){
        backBtn.setVisibility(View.GONE);
        guestBtnLayout.setVisibility(View.VISIBLE);
    }

    private void ShowBackHideGuest(){
        backBtn.setVisibility(View.VISIBLE);
        guestBtnLayout.setVisibility(View.GONE);
    }

    private void InitializeViews(){
        backBtn = findViewById(R.id.back_btn);
        emailET = findViewById(R.id.login_email_et);
        passwET = findViewById(R.id.login_passw_et);
        forgotPasswTV = findViewById(R.id.forgot_passw_tv);
        createAccTV = findViewById(R.id.create_acc_tv);
        logInBtn = findViewById(R.id.log_in_btn);
        logInAsGuestBtn=findViewById(R.id.log_in_as_guest_btn);
        guestBtnLayout=findViewById(R.id.guest_btn_layout);
    }

    private void SetOnClickListeners(){
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        forgotPasswTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: восстановление пароля
                Toast.makeText(LoginActivity.this, R.string.in_development, Toast.LENGTH_SHORT).show();
            }
        });

        logInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogInUser();
            }
        });

        logInAsGuestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataManager.markOnboardingCompleted();
                Intent intent=new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // Плавное появление/исчезание
                finish();
            }
        });

        createAccTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(LoginActivity.this, SignInActivity.class);
                if(fromAccSettings){
                    intent.putExtra("from_acc_settings", true);
                }
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // Плавное появление/исчезание
                finish();
            }
        });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish(); // чтобы пользователь не вернулся сюда нажав назад
    }

    //убрано тк DataManager сам управляет состоянием
    /*private void checkCurrentUser() {
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToMainActivity();
        }
    }*/

    @Override
    public void onStart() {
        super.onStart();
        //checkCurrentUser();
    }

    /*
    @Override
    public void onClick(View v){
        if(v.getId()==R.id.log_in_btn){
            LogInUser();
        }
        else if(v.getId()==R.id.create_acc_tv){
            Intent intent=new Intent(this, SignInActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // Плавное появление/исчезание
            finish();
        }
        else if (v.getId()==R.id.forgot_passw_tv) {
            // TODO: восстановление пароля
            Toast.makeText(LoginActivity.this, R.string.in_development, Toast.LENGTH_SHORT).show();
        }
        else if (v.getId()==R.id.back_btn) {
            finish();
        }
    }
*/

    private String getErrorMessage(Exception exception) {

        if (exception == null || exception.getMessage() == null) {
            return getString(R.string.unknown_error);
        }

        String errorMessage = exception.getMessage();
        if (errorMessage.contains("password is invalid")) {
            return getString(R.string.invalid_passw_error);
        } else if (errorMessage.contains("no user record")) {
            return getString(R.string.email_not_found);
        } else if (errorMessage.contains("badly formatted")) {
            return getString(R.string.invalid_email_error);
        } else if (errorMessage.contains("failed to connect") && errorMessage.contains("network")) {
            return getString(R.string.network_error);
        } else if(errorMessage.contains("auth credential is incorrect")){
            return getString(R.string.invalid_auth_data);
        }
        return getString(R.string.an_error_occured) + errorMessage;

    }

    private void LogInUser() {
        String email = emailET.getText().toString().trim();
        String password = passwET.getText().toString().trim();

        // Валидация полей
        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(LoginActivity.this, R.string.enter_all, Toast.LENGTH_SHORT).show();
            emailET.requestFocus(); // Фокус на поле email
            return;
        } else if (password.length() < 8) {
            Toast.makeText(LoginActivity.this, R.string.passw_length, Toast.LENGTH_SHORT).show();
            passwET.requestFocus();
            return;
        }

        // Дополнительная проверка формата email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(LoginActivity.this, R.string.invalid_email, Toast.LENGTH_SHORT).show();
            emailET.requestFocus();
            return;
        }

        //loadingDialog.show("Вход в систему..."); ////////////////////////////////

        // авторизация через Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {
                                dataManager.saveUserSetting("email", user.getEmail());

                                Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();

                                //dataManager.handleUserLogin(LoginActivity.this::goToMainActivity);
                                dataManager.handleUserLogin(() -> {
                                    dataManager.markOnboardingCompleted();
                                    goToMainActivity();
                                });

                                //goToMainActivity();
                            }
                            else{
                                String errorMessage = getErrorMessage(task.getException());
                                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }

                            return;
                        }
                        else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            //Toast.makeText(LoginActivity.this, R.string.login_error, Toast.LENGTH_SHORT).show();

                            String errorMessage = getErrorMessage(task.getException());
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();

                            return;
                        }
                    }
                });
    }
}