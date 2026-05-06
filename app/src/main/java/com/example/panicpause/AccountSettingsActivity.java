package com.example.panicpause;

import static android.widget.Toast.LENGTH_SHORT;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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

    private ProgressDialogFragment progressDialog;

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

    private void updateUI() {
        if (dataManager == null) {
            dataManager = new DataManager(this);
        }
        if (dataManager.isGuest()) {
            // гость
            guestMsgLayout.setVisibility(View.VISIBLE);
            emailPasswResetLayout.setVisibility(View.GONE);
            signOutDeleteAccLayout.setVisibility(View.GONE);
        } else {
            // авторизованный
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
                // запуск SignInActivity с extra
                Intent intent=new Intent(AccountSettingsActivity.this, SignInActivity.class);
                intent.putExtra("from_acc_settings", true);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // плавное появление/исчезание
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
                //Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/yuureisen"));
                //startActivity(browserIntent);
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Email","ign_yu103@mail.ru");
                clipboard.setPrimaryClip(clip);
                Toast.makeText(AccountSettingsActivity.this, "Email тех. поддержки скопирован", LENGTH_SHORT).show();
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
            progressDialog = new ProgressDialogFragment();
            try {
                progressDialog.show(getSupportFragmentManager(), "progress_dialog");
            }
            catch(IllegalStateException ex){
                // обработка случая, когда Activity уничтожается
                Log.e("Dialog", "Cannot show dialog - activity state invalid");
            }

            // выход из Firebase Auth
            mAuth.signOut();

            // обновление локального состояния через DataManager
            dataManager.handleUserLogout();

            Toast.makeText(AccountSettingsActivity.this, R.string.signout_success, LENGTH_SHORT).show();

            progressDialog.dismiss();

            // перезапуск активности для обновления UI
            Intent intent = getIntent();
            finish();
            startActivity(intent);
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
                    // пользователь подтвердил выход
                    signOutUser();
                }

                @Override
                public void onSignOutCancelled() {
                    // пользователь отменил выход
                    dialog.dismiss();
                }
            });

            dialog.show(getSupportFragmentManager(), "sign_out_dialog");
        }
        catch(IllegalStateException ex){
            // обработка случая, когда Activity уничтожается
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
        // обновление UI при возврате в активность (важно после удаления аккаунта)
        updateUI();
    }

}