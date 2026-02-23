package com.example.panicpause;

import static android.widget.Toast.LENGTH_SHORT;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AccountSettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DataManager dataManager;
    ImageButton backBtn;
    LinearLayout guestMsgLayout, goToLogInSignInLayout,
            emailPasswResetLayout, passwResetLayout,
            techSupportLayout, signOutDeleteAccLayout;
    TextView userEmailTV, signOutTV, deleteAccTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account_settings);

        InitializeViews();
        SetOnClickListeners();

        dataManager=new DataManager(this);
        mAuth = FirebaseAuth.getInstance();

        updateUI();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    ///////

    private void updateUI() {
        if (dataManager == null) {
            dataManager = new DataManager(this);
        }
        if (dataManager.isGuest()) {
            // Гость
            guestMsgLayout.setVisibility(View.VISIBLE);
            emailPasswResetLayout.setVisibility(View.GONE);
            signOutDeleteAccLayout.setVisibility(View.GONE);
        } else {
            // Авторизованный
            guestMsgLayout.setVisibility(View.GONE);
            emailPasswResetLayout.setVisibility(View.VISIBLE);
            signOutDeleteAccLayout.setVisibility(View.VISIBLE);

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null && user.getEmail() != null) {
                userEmailTV.setText(user.getEmail());
            }
        }
    }

    private void InitializeViews(){
        backBtn=findViewById(R.id.back_btn);

        guestMsgLayout=findViewById(R.id.logged_in_as_guest_layout);
        goToLogInSignInLayout=findViewById(R.id.go_to_login_signin_layout);

        emailPasswResetLayout=findViewById(R.id.email_passw_reset_layout);
        userEmailTV=findViewById(R.id.user_email_tv);

        passwResetLayout=findViewById(R.id.go_to_passw_reset_layout);
        techSupportLayout=findViewById(R.id.tech_support_layout);

        signOutDeleteAccLayout=findViewById(R.id.sign_out_delete_acc_layout);
        signOutTV=findViewById(R.id.sign_out_tv);
        deleteAccTV=findViewById(R.id.delete_acc_tv);
    }

    private void SetOnClickListeners(){
        goToLogInSignInLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // go to sign in activity with intent Extra
                Intent intent=new Intent(AccountSettingsActivity.this, SignInActivity.class);
                intent.putExtra("from_acc_settings", true);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // Плавное появление/исчезание
                finish();
            }
        });

        passwResetLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(AccountSettingsActivity.this, ResetPasswordActivity.class);
                startActivity(intent);
            }
        });

        techSupportLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/yuureisen"));
                startActivity(browserIntent);
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        signOutTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSignOutConfirmationDialog();
            }
        });

        deleteAccTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteAccountConfirmationDialog();
            }
        });
    }

    // выход из аккаунта
    private void signOutUser() {
        FirebaseUser user = mAuth.getCurrentUser();
        if(user!=null){
            // Выход из Firebase Auth
            mAuth.signOut();

            // Обновление локального состояния через DataManager
            dataManager.handleUserLogout();

            Toast.makeText(AccountSettingsActivity.this, R.string.signout_success, LENGTH_SHORT).show();

            // Перезапуск активности для обновления UI
            Intent intent = getIntent();
            finish();
            startActivity(intent);

            /*
            // Переходим на экран входа
            Intent intent = new Intent(this, LoginActivity.class);

            // Очищаем историю навигации
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);

            // Завершаем текущую активность
            finish();
*/
        }
        else{
            Toast.makeText(AccountSettingsActivity.this, "Пользователь не обнаружен",
                    LENGTH_SHORT).show();
        }
    }

    private void showSignOutConfirmationDialog() {
        try{
            SignOutDialogFragment dialog = new SignOutDialogFragment();
            dialog.setOnSignOutListener(new SignOutDialogFragment.OnSignOutListener() {
                @Override
                public void onSignOutConfirmed() {
                    // Пользователь подтвердил выход
                    signOutUser();
                }

                @Override
                public void onSignOutCancelled() {
                    // Пользователь отменил выход
                    //Toast.makeText(AccountSettingsActivity.this, "Выход отменен", LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });

            dialog.show(getSupportFragmentManager(), "sign_out_dialog");
        }
        catch(IllegalStateException ex){
            // Обработка случая, когда Activity уничтожается
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }

    private void showDeleteAccountConfirmationDialog(){
        DeleteAccountDialogFragment dialog = new DeleteAccountDialogFragment();

        if (getSupportFragmentManager() != null) {
            dialog.show(getSupportFragmentManager(), "delete_account_dialog");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем UI при возврате в активность (важно после удаления аккаунта)
        updateUI();
    }

}