package com.example.panicpause;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class SignOutDialogFragment extends DialogFragment {

    private OnSignOutListener signOutListener;

    // Интерфейс для обработки действий
    public interface OnSignOutListener {
        void onSignOutConfirmed();
        void onSignOutCancelled();
    }

    // Устанавливаем слушатель
    public void setOnSignOutListener(OnSignOutListener listener) {
        this.signOutListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // "Надуваем" кастомный макет
        View view = inflater.inflate(R.layout.dialog_sign_out, container, false);

        // Set transparent background and no title
        /*if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }*/

        setupViews(view);

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // Делаем прозрачный фон у диалога
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Разрешаем закрытие при клике вне диалога
        dialog.setCanceledOnTouchOutside(true);

        return dialog;
    }

    private void setupViews(View view) {
        Button signOutBtn = view.findViewById(R.id.yes_sign_out_btn);
        Button cancelBtn = view.findViewById(R.id.cancel_btn);

        // Обработчик кнопки подтверждения
        signOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (signOutListener != null) {
                    signOutListener.onSignOutConfirmed();
                }
                dismiss(); // Закрываем диалог
            }
        });

        // Обработчик кнопки отмены
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (signOutListener != null) {
                    signOutListener.onSignOutCancelled();
                }
                dismiss(); // Закрываем диалог
            }
        });

        // закрытие при клике на затемненную область
        view.findViewById(R.id.dialog_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        // клик на само содержимое диалога
        view.findViewById(R.id.dialog_content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ничего не делаем - предотвращаем закрытие
            }
        });
    }
}
