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

public class NoExExceptBreathDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_no_ex_except_breath, container, false);

        Button exitSettingsBtn=view.findViewById(R.id.yes_close_ground_settings_btn);
        Button stayInSettingsBtn=view.findViewById(R.id.no_stay_in_ground_settings_btn);

        exitSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                GroundSettingsActivity activity = (GroundSettingsActivity) getActivity();
                if (activity!=null)
                    activity.finishActivity();
            }
        });

        stayInSettingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

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
}
