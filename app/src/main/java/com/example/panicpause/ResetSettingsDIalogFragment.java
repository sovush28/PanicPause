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

public class ResetSettingsDIalogFragment extends DialogFragment {

    private ResetSettingsDIalogFragment.OnResetSettingsListener resetSettingsListener;
    public interface OnResetSettingsListener{
        void onResetSettingsConfirmed();
        void onResetSettingsCancelled();
    }

    public void setResetSettingsListener(ResetSettingsDIalogFragment.OnResetSettingsListener listener){
        this.resetSettingsListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_reset_settings, container, false);

        setupViews(view);

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // прозрачный фон
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // закрытие при клике вне диалога
        dialog.setCanceledOnTouchOutside(true);

        return dialog;
    }

    private void setupViews(View view) {
        Button resetSettingsBtn = view.findViewById(R.id.yes_reset_settings_btn);
        Button cancelBtn = view.findViewById(R.id.cancel_btn);

        resetSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (resetSettingsListener != null) {
                    resetSettingsListener.onResetSettingsConfirmed();
                }
                dismiss(); // Закрываем диалог
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (resetSettingsListener != null) {
                    resetSettingsListener.onResetSettingsCancelled();
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
                // ничего не делаем
            }
        });
    }

}
