package com.example.safespace;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// Фрагмент - "мини-активность", которая может быть частью экрана
public class HomeFragment extends Fragment {

    private FirebaseAuth mAuth;
    private Button panicBtn, tempDeleteUserBtn, tempSignOutBtn;

    // метод создает внешний вид фрагмента
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // "Надуваем" макет из XML-файла fragment_home.xml
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        panicBtn = view.findViewById(R.id.panic_btn);
        panicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: переход к упражнениям "Заземление" (GroundActivity.GroundBreathFragment)
                Intent intent = new Intent(getActivity(), GroundActivity.class);
                startActivity(intent);
            }
        });

        tempSignOutBtn=view.findViewById(R.id.temp_sign_out_btn);
        tempSignOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //signOutUser();
                showSignOutConfirmationDialog();
            }
        });

        tempDeleteUserBtn=view.findViewById(R.id.temp_delete_user_btn);
        tempDeleteUserBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //deleteUserAccount();
                showDeleteAccountConfirmationDialog();
                /*FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                user.delete()
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "User account deleted.");

                                    Toast.makeText(HomeFragment.this, R.string.user_deleted, Toast.LENGTH_SHORT);

                                    Intent intent=new Intent(this, LoginActivity.class);
                                    startActivity(intent);

                                    finish();

                                }
                            }
                        });*/
            }
        });

        return view;  // Возвращаем готовый экран
    }

    /////////////////////////// конец oncreate

    // ВЫХОД из аккаунта
    private void signOutUser() {

        // Выходим из Firebase Auth
        mAuth.signOut();

        Toast.makeText(getContext(), R.string.signout_success, Toast.LENGTH_SHORT).show();

        // Переходим на экран входа
        Intent intent = new Intent(getActivity(), LoginActivity.class);

        // Очищаем историю навигации
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);

        // Завершаем текущую активность
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    //TODO: ошибка (необходим недавний вход?)
    // УДАЛЕНИЕ аккаунта пользователя
    private void deleteUserAccount() {
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
                                Toast.makeText(HomeFragment.this.getContext(), R.string.user_deleted, Toast.LENGTH_SHORT).show();

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
                                Toast.makeText(HomeFragment.this.getContext(),
                                        R.string.acc_deletion_error + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        } else {
            // Пользователь не авторизован
            Toast.makeText(getContext(), R.string.user_isnt_signed_in, Toast.LENGTH_SHORT).show();
        }
    }


    private void showSignOutConfirmationDialog() {
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
                Toast.makeText(getContext(), "Выход отменен", Toast.LENGTH_SHORT).show();
                //TODO: тост убрать, тут он только для тестирования нужен думаю
            }
        });

        // Показываем диалог через FragmentManager
        if (getParentFragmentManager() != null) {
            dialog.show(getParentFragmentManager(), "sign_out_dialog");
        }
    }

    private void showDeleteAccountConfirmationDialog() {
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
                Toast.makeText(getContext(), "Удаление отменено", Toast.LENGTH_SHORT).show();
                //TODO: то же самое что и повыше, тост потом убрать
            }
        });

        if (getParentFragmentManager() != null) {
            dialog.show(getParentFragmentManager(), "delete_account_dialog");
        }
    }


    ////////
}