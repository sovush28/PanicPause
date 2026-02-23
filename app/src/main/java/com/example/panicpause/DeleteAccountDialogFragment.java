package com.example.panicpause;

import static android.content.ContentValues.TAG;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class DeleteAccountDialogFragment extends DialogFragment {

    private Button deleteBtn, cancelBtn;
    private EditText passwET;

    private boolean isDeleting = false; // Флаг защиты от повторных нажатий
    private String userIdToDelete; // Сохраняем UID до удаления Auth

    private DataManager dataManager;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_delete_account, container, false);
        dataManager = new DataManager(requireContext());
        setupViews(view);
        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    private void setupViews(View view) {
        deleteBtn = view.findViewById(R.id.yes_delete_acc_btn);
        cancelBtn = view.findViewById(R.id.cancel_btn);
        passwET = view.findViewById(R.id.delete_acc_passw_et);

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isDeleting) {
                    return; // Защита от повторных нажатий
                }
                deleteUserAccount();

            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        view.findViewById(R.id.dialog_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        view.findViewById(R.id.dialog_content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Блокируем закрытие при клике на контент
            }
        });
    }

    private void deleteUserAccount(){
        DataManager dataManager = new DataManager(requireContext());
        if(dataManager.isGuest()){
            return;
        }

        String password = passwET.getText().toString().trim();

        if(password.isEmpty()){
            Toast.makeText(requireContext(), R.string.enter_passw, Toast.LENGTH_SHORT).show();
            return;
        }

        isDeleting = true;
        deleteBtn.setEnabled(false); // Блокируем кнопку во время удаления

        try{
            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user == null || user.getEmail() == null) {
                Toast.makeText(requireContext(), R.string.user_isnt_signed_in, Toast.LENGTH_LONG).show();
                deleteBtn.setEnabled(true);
                return;
            }

            // Сохраняем UID ДО удаления Auth (после удаления getCurrentUser() вернёт null)
            userIdToDelete = user.getUid();
            String userEmail = user.getEmail();

            if (userEmail == null) {
                Toast.makeText(requireContext(), R.string.user_isnt_signed_in, Toast.LENGTH_LONG).show();
                isDeleting = false;
                deleteBtn.setEnabled(true);
                dismiss();
                return;
            }

            // Шаг 1: Ре-аутентификация пользователя (требуется Firebase для удаления)
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
            user.reauthenticate(credential)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                // Шаг 2: Удаляем документ пользователя из Firestore
                                deleteUserRecordFromFirestore(userIdToDelete);
                            } else {
                                deleteBtn.setEnabled(true);
                                Toast.makeText(requireContext(), getErrorMessage(task.getException()), Toast.LENGTH_LONG).show();
                            }
                        }
                    });

            /*if (user != null) {
                user.reauthenticate(credential)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                user.delete()
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    // Шаг 2: Удаляем документ пользователя из Firestore
                                                    deleteUserRecordFromFirestore(user.getUid());
                                                    Log.d("TAG", "User account deleted.");
                                                    Toast.makeText(requireContext(), getString(R.string.user_deleted), Toast.LENGTH_LONG).show();
                                                    //dismiss();
                                                }
                                                else{
                                                    deleteBtn.setEnabled(true);
                                                    Toast.makeText(requireContext(), getErrorMessage(task.getException()), Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });
                            }
                        });
            }*/
        }
        catch(Exception exception){
            isDeleting = false;
            deleteBtn.setEnabled(true);
            Toast.makeText(requireContext(), getErrorMessage(exception), Toast.LENGTH_LONG).show();
        }

    }

    private void deleteUserRecordFromFirestore(String userID){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        try{
            // Шаг 2: Удаляем документ пользователя из Firestore
            db.collection("users").document(userID)
                    .delete()
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "User DocumentSnapshot successfully deleted from Firestore");
                        // Шаг 3: Удаляем аккаунт Firebase Auth
                        deleteFirebaseAuthAccount();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error deleting user document from Firestore", e);
                        // Продолжаем удаление Auth даже если Firestore не удалился (пользователь может попробовать снова)
                        deleteFirebaseAuthAccount();
                    });

            /*db.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d(TAG, "User DocumentSnapshot successfully deleted");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error deleting user document", e);
                        }
                    });*/
        }
        catch(Exception exception){
            Toast.makeText(requireContext(), getErrorMessage(exception), Toast.LENGTH_LONG).show();
        }
    }

    private void deleteFirebaseAuthAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            // Пользователь уже удалён, переходим к очистке локальных данных
            cleanupAfterDeletion();
            return;
        }

        // Шаг 3: Удаляем аккаунт Firebase Auth
        user.delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User account deleted from Firebase Auth.");
                            cleanupAfterDeletion();
                        } else {
                            deleteBtn.setEnabled(true);
                            Toast.makeText(requireContext(), getErrorMessage(task.getException()), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void cleanupAfterDeletion() {
        // Шаг 4: Очищаем все локальные данные пользователя и создаём нового гостя
        dataManager.handleAccountDeletion();

        // Шаг 5: Показываем сообщение об успешном удалении
        Toast.makeText(requireContext(), getString(R.string.user_deleted), Toast.LENGTH_LONG).show();

        // Шаг 6: Закрываем диалог
        dismiss();

        // Шаг 7: Завершаем AccountSettingsActivity и возвращаемся в MainActivity
        // Используем Intent с флагами для очистки стека активностей
        if (getActivity() != null) {
            /*Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);*/
            getActivity().finish();
        }
    }

    private String getErrorMessage(Exception exception) {

        if (exception == null || exception.getMessage() == null) {
            return getString(R.string.unknown_error);
        }

        String errorMessage = exception.getMessage();
        if (errorMessage.contains("password is invalid")) {
            return getString(R.string.invalid_passw_error);
        } else if (errorMessage.contains("failed to connect") && errorMessage.contains("network")) {
            return getString(R.string.network_error);
        } else if(errorMessage.contains("auth credential is incorrect")){
            return getString(R.string.invalid_auth_data);
        }
        return getString(R.string.an_error_occured) + errorMessage;
    }

}
