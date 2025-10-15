package com.example.safespace;

import static android.content.ContentValues.TAG;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    EditText emailET, passwET;
    TextView forgotPasswTV, createAccTV;
    Button logInBtn;

    private FirebaseAuth mAuth; //Declare an instance of FirebaseAuth
    private FirebaseFirestore db;

    // Для показа прогресса в build.gradle (module:app) dependencies добавить:
    //    implementation 'com.github.ybq:Android-SpinKit:1.4.0'
    // далее отмечено так /////////////////////

    //private LoadingDialog loadingDialog; //////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        emailET = (EditText) findViewById(R.id.login_email_et);
        passwET = (EditText) findViewById(R.id.login_passw_et);
        forgotPasswTV = (TextView) findViewById(R.id.forgot_passw_tv);
        createAccTV = (TextView) findViewById(R.id.create_acc_tv);
        logInBtn = (Button) findViewById(R.id.log_in_btn);

        logInBtn.setOnClickListener(this);
        forgotPasswTV.setOnClickListener(this);
        createAccTV.setOnClickListener(this);

        //если перебросило сюда после регистрации то подгружается логин
        Intent intent=getIntent();
        if(intent.hasExtra("user_email")){
            String createdUserEmail=intent.getStringExtra("user_email");
            emailET.setText(createdUserEmail);
            //String createdUserPassw=intent.getStringExtra("user_passw");
            //passwET.setText(createdUserPassw);
        }

        // Инициализация Firebase
        mAuth = FirebaseAuth.getInstance(); //initialize the FirebaseAuth instance
        db = FirebaseFirestore.getInstance();
        //loadingDialog = new LoadingDialog(this); ///////////////////////////

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish(); // чтобы пользователь не вернулся сюда нажав назад
    }

    private void checkCurrentUser() {
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToMainActivity();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        checkCurrentUser();
    }

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
            // TODO: Реализовать восстановление пароля

        }
    }

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
        } else if (errorMessage.contains("network error")) {
            return getString(R.string.network_error);
        }
        return getString(R.string.an_error_occured) + errorMessage;

    }

    private void LogInUser() {
        String email = emailET.getText().toString().trim();
        String password = passwET.getText().toString().trim();

        // Валидация полей
        if(email.isEmpty() && password.isEmpty()){
            Toast.makeText(LoginActivity.this, R.string.enter_all, Toast.LENGTH_SHORT).show();
            emailET.requestFocus(); // Фокус на поле email
            return;
        } else if (email.isEmpty()) {
            Toast.makeText(LoginActivity.this, R.string.enter_email, Toast.LENGTH_SHORT).show();
            emailET.requestFocus();
            return;
        } else if(password.isEmpty()){
            Toast.makeText(LoginActivity.this, R.string.enter_passw, Toast.LENGTH_SHORT).show();
            passwET.requestFocus();
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
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {
                                Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();

                                //TODO: перенести вошедшего юзера на главную, подгрузив его данные

                                goToMainActivity();
                            }
                            else{
                                String errorMessage = getErrorMessage(task.getException());
                                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }

                            /*// ПРОВЕРЯЕМ, ПОДТВЕРЖДЕН ЛИ EMAIL
                            if (user != null && user.isEmailVerified()) {
                                Toast.makeText(LoginActivity.this, "Добро пожаловать!", Toast.LENGTH_SHORT).show();
                                goToMainActivity(); //только здесь можно перейти на главную

                            } else {
                                // Email НЕ подтвержден
                                mAuth.signOut(); // Выходим из аккаунта //?

                                Toast.makeText(LoginActivity.this,
                                        getString(R.string.email_verification_needed),
                                        Toast.LENGTH_LONG).show();

                                // Предлагаем отправить письмо повторно
                                showResendVerificationDialog(email);
                            }*/

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

    // Диалог для повторной отправки верификационного письма
    /*private void showResendVerificationDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.email_not_verified))
                .setMessage(getString(R.string.send_ver_link_again) + email + "?")
                .setCancelable(true); // если false, то тогда пользователь не сможет закрыть диалог нажав "назад"

        // Создаем диалог
        AlertDialog verDialog = builder.create();

        // Устанавливаем кнопки после создания диалога
        verDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.send), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Эта кнопка будет заменена, поэтому обработчик здесь не нужен
            }
        });

        verDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        // Показываем диалог
        verDialog.show();

        // Получаем ссылку на кнопку "Отправить" и настраиваем обратный отсчет
        Button sendButton = verDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        startCountdown(sendButton, email, verDialog);

    }

    // Метод для запуска обратного отсчета
    private void startCountdown(Button button, String email, AlertDialog dialog) {
        // Блокируем кнопку сразу
        button.setEnabled(false);

        final int[] countdownTime = {60}; // 60 секунд обратного отсчета

        // Создаем таймер для обновления текста кнопки каждую секунду
        CountDownTimer countDownTimer = new CountDownTimer(60000, 1000) { // 60 секунд, интервал 1 секунда
            @Override
            public void onTick(long millisUntilFinished) {
                // Обновляем текст кнопки
                countdownTime[0]--;
                String buttonText = getString(R.string.send) + " (" + countdownTime[0] + " " + getString(R.string.seconds) + ")";
                button.setText(buttonText);
            }

            @Override
            public void onFinish() {
                // Восстанавливаем кнопку
                button.setEnabled(true);
                button.setText(getString(R.string.send)); // Возвращаем оригинальный текст

                // Устанавливаем новый обработчик для отправки письма
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendEmailVerification(email);
                        dialog.dismiss(); // Закрываем диалог после отправки
                    }
                });
            }
        };

        // Запускаем таймер
        countDownTimer.start();

        // Устанавливаем первоначальный текст
        String initialText = getString(R.string.send) + " (" + countdownTime[0] + " " + getString(R.string.seconds) + ")";
        button.setText(initialText);
    }*/

    // Метод для отправки верификационного письма
    /*private void sendEmailVerification(String email) {
        // временный вход для отправки письма
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, "temporary_password")
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Отправляем письмо верификации
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                user.sendEmailVerification()
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(LoginActivity.this,
                                                            getString(R.string.ver_link_sent) + user.getEmail(),
                                                            Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(LoginActivity.this,
                                                            getString(R.string.email_sending_error) + task.getException().getMessage(),
                                                            Toast.LENGTH_LONG).show();
                                                }
                                                // Выход из временного аккаунта
                                                FirebaseAuth.getInstance().signOut();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    getString(R.string.login_error),
                                    Toast.LENGTH_LONG).show();
                            //"Для повторной отправки войдите в аккаунт"
                        }
                    }
                });
    }*/

}