package com.example.safespace;

import static android.content.ContentValues.TAG;

import static java.security.AccessController.getContext;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AccountSettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    ImageButton backBtn;
    Button tempSignOutBtn, tempDeleteAccBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account_settings);

        mAuth = FirebaseAuth.getInstance();

        backBtn=findViewById(R.id.back_btn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        tempSignOutBtn=findViewById(R.id.temp_sign_out_btn);
        tempSignOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSignOutConfirmationDialog();
            }
        });

        /*tempDeleteAccBtn=findViewById(R.id.temp_delete_user_btn);
        tempDeleteAccBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteAccountConfirmationDialog();
            }
        });*/

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    ///////

    // ВЫХОД из аккаунта
    private void signOutUser() {

        FirebaseUser user = mAuth.getCurrentUser();
        if(user!=null){
            // Выходим из Firebase Auth
            mAuth.signOut();

            Toast.makeText(AccountSettingsActivity.this, R.string.signout_success, Toast.LENGTH_SHORT).show();

            // Переходим на экран входа
            Intent intent = new Intent(this, LoginActivity.class);

            // Очищаем историю навигации
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);

            // Завершаем текущую активность
            finish();
        }
        else{
            Toast.makeText(AccountSettingsActivity.this, "Пользователь не обнаружен",
                    Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(AccountSettingsActivity.this, "Выход отменен", Toast.LENGTH_SHORT).show();
                    //TODO: тост убрать, тут он только для тестирования нужен думаю
                }
            });

            dialog.show(getSupportFragmentManager(), "sign_out_dialog");
        }
        catch(IllegalStateException ex){
            // Обработка случая, когда Activity уничтожается
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }


    //TODO: ошибка (необходим недавний вход?)
    // УДАЛЕНИЕ аккаунта пользователя
    /*private void deleteUserAccount() {
        // Получаем текущего пользователя
        FirebaseUser user = mAuth.getCurrentUser();

        // Проверяем, что пользователь существует (вошел в систему)
        if (user != null) {

            user.delete()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "User account deleted.");

                                // HomeFragment.this.getContext() - получаем контекст фрагмента
                                Toast.makeText(AccountSettingsActivity.this, R.string.user_deleted, Toast.LENGTH_SHORT).show();

                                // getActivity() - получаем активность, в которой находится фрагмент
                                Intent intent = new Intent(getActivity(), LoginActivity.class);

                                // Очищаем историю навигации, чтобы пользователь не мог вернуться назад
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                                startActivity(intent);

                                // завершаем активность
                                if (getActivity() != null) {
                                    getActivity().finish();
                                }

                            } else {
                                // ошибка удаления
                                Log.w(TAG, "Failed to delete user account", task.getException());
                                Toast.makeText(AccountSettingsActivity.this,
                                        R.string.acc_deletion_error + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        } else {
            // Пользователь не авторизован
            Toast.makeText(AccountSettingsActivity.this, R.string.user_isnt_signed_in, Toast.LENGTH_SHORT).show();
        }
    }*/


    /*private void showDeleteAccountConfirmationDialog() {
        DeleteAccountDialogFragment dialog = new DeleteAccountDialogFragment();
        dialog.setOnDeleteAccountListener(new DeleteAccountDialogFragment.OnDeleteAccountListener() {
            @Override
            public void onDeleteConfirmed() {
                // Пользователь подтвердил удаление
                deleteUserAccount();
            }

            @Override
            public void onDeleteCancelled() {
                // Пользователь отменил удаление
                Toast.makeText(AccountSettingsActivity.this, "Удаление отменено", Toast.LENGTH_SHORT).show();
                //TODO: то же самое что и повыше, тост потом убрать
            }
        });

        if (getParentFragmentManager() != null) {
            dialog.show(getParentFragmentManager(), "delete_account_dialog");
        }
    }*/



}