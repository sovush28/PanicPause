package com.example.safespace;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SignInActivity extends AppCompatActivity implements View.OnClickListener {

    EditText emailET, passwET, repeatPasswET;
    TextView logInTV;
    Button signInBtn;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);

        InitializeViews();

        logInTV.setOnClickListener(this);
        signInBtn.setOnClickListener(this);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void InitializeViews(){
        emailET=(EditText)findViewById(R.id.signin_email_et);
        repeatPasswET=(EditText)findViewById(R.id.signin_repeat_passw_et);
        passwET=(EditText)findViewById(R.id.signin_passw_et);
        logInTV=(TextView)findViewById(R.id.to_login_tv);
        signInBtn=(Button)findViewById(R.id.sign_in_btn);
    }

    @Override
    public void onClick(View v){
        if(v.getId()==R.id.to_login_tv){
            Intent intent=new Intent(this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // Плавное появление/исчезание
            finish();
        }
        else if (v.getId()==R.id.sign_in_btn) {
            String passw=passwET.getText().toString();
            String passwRepeat=repeatPasswET.getText().toString();
            if(!passw.trim().equals(passwRepeat.trim())) {
                Toast.makeText(this, getString(R.string.passws_not_same),Toast.LENGTH_SHORT).show();
                return;
            }
            signInUser();
        }
    }

    ///

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
        } else if (errorMessage.contains("email address is already") ||
                errorMessage.contains("The email address is already in use")) {
            return getString(R.string.user_email_exists);
        }
        return getString(R.string.an_error_occured) + errorMessage;

    }

    ///

    private void signInUser(){
        String email=emailET.getText().toString().trim();
        String password=passwET.getText().toString().trim();
        String repeatPassw=repeatPasswET.getText().toString().trim();

        // валидация полей
        if(email.isEmpty() || password.isEmpty() || repeatPassw.isEmpty()){
            Toast.makeText(SignInActivity.this, R.string.enter_all, Toast.LENGTH_SHORT).show();
            return;
        } else if (password.length()<8) {
            Toast.makeText(SignInActivity.this, R.string.passw_length, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(SignInActivity.this, R.string.invalid_email, Toast.LENGTH_SHORT).show();
            return;
        }

        // loadingDialog.show("Регистрация...");

        // регистрация
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "createUserWithEmail:success");

                            FirebaseUser user = mAuth.getCurrentUser();

                            if(user!=null){
                                saveUserToFirestore(user);

                                Toast.makeText(SignInActivity.this,
                                        getString(R.string.signin_success),
                                        Toast.LENGTH_SHORT).show();

                                goToLoginActivity();
                            }
                            else{
                                Log.w(TAG, "Failed to save user to Firestore");
                                Toast.makeText(SignInActivity.this,
                                        getString(R.string.user_data_saving_error),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                        else{
                            String errorMessage = getErrorMessage(task.getException());
                            Toast.makeText(SignInActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String errorMessage = getErrorMessage(e);
                        Toast.makeText(SignInActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Registration failed", e);
                    }
                });

        /*
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");

                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user!=null){
                                saveUserToFirestore(user, new OnUserSavedListener() {
                                    @Override
                                    public void onUserSaved() {
                                        //sendEmailVerification(user);

                                        Toast.makeText(SignInActivity.this,
                                                getString(R.string.signin_success),
                                                Toast.LENGTH_SHORT).show();

                                        //sendEmailVerification(user);

                                        goToLoginActivity();
                                    }

                                    @Override
                                    public void onUserSaveFailed(Exception e) {
                                        // не сохранилось в Firestore
                                        Log.w(TAG, "Failed to save user to Firestore", e);
                                        Toast.makeText(SignInActivity.this,
                                                "Не удалось сохранить пользователя", //то же самое что и signin_error ?
                                                Toast.LENGTH_LONG).show();
                                    }
                                });

                            }
                            else {
                                Toast.makeText(SignInActivity.this,
                                        getString(R.string.user_create_error), //то же самое что и signin_error ?
                                        Toast.LENGTH_LONG).show();
                            }
                            return;
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());

                            String errorMessage = getErrorMessage(task.getException());
                            Toast.makeText(SignInActivity.this, errorMessage, Toast.LENGTH_LONG).show();

                            return;
                        }
                    }
                });*/
    }

    ////

    //коллбэки для асинхронных операций
    // Интерфейс для коллбэка сохранения пользователя
    /*interface OnUserSavedListener{
        void onUserSaved();
        void onUserSaveFailed(Exception e);
    }*/

    /////

    private void saveUserToFirestore(FirebaseUser user) { //, OnUserSavedListener listener
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("created_at", System.currentTimeMillis());
        userData.put("triggers", new ArrayList<String>());
        userData.put("faves", new ArrayList<String>());
        userData.put("ground_photo_ex_amount", 2); //?
        userData.put("breath_repeat_amount", 1);
        userData.put("use_faves_only", false);
        userData.put("use_math", true);
        userData.put("use_search_objects_color", true);
        userData.put("ground_on_launch", false);
        //userData.put("displayName", ""); // поле для имени

        db.collection("users")
                .document(user.getUid())
                .set(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User data saved to Firestore");
                        //listener.onUserSaved();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error saving user data", e);
                        //listener.onUserSaveFailed(e);
                    }
                });
    }

    /////

    ////

    private void goToLoginActivity(){
        Intent intent = new Intent(SignInActivity.this,LoginActivity.class);
        intent.putExtra("user_email", emailET.getText().toString().trim());
        //intent.putExtra("user_passw", passwET.getText().toString().trim());
        startActivity(intent);

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    /////

    /*private void sendEmailVerification(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            // Письмо отправлено успешно
                            Log.d(TAG, "Verification email sent to: " + user.getEmail());

                            mAuth.signOut();

                            Toast.makeText(SignInActivity.this,
                                    getString(R.string.ver_link_sent) + user.getEmail(),
                                    Toast.LENGTH_LONG).show();

                            goToLoginActivity();
                        }
                        else{
                            // Ошибка отправки письма
                            Log.w(TAG, "Failed to send verification email", task.getException());

                            // Все равно выходим и переходим на вход, но с предупреждением
                            mAuth.signOut();

                            Toast.makeText(SignInActivity.this,
                                    "Аккаунт создан, но не удалось отправить письмо подтверждения. " +
                                            "Вы можете запросить его повторно на странице входа.",
                                    Toast.LENGTH_LONG).show();

                            goToLoginActivity();
                        }

                    }
                });

    }*/


    // Обработчик кнопки "Назад"
    /*@Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }*/

    ////////////////////

}