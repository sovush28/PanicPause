package com.example.panicpause;

import android.app.Dialog;
import android.content.Intent;
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

public class NotEnoughPhotosDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                              @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // "Надуваем" кастомный макет
        View view = inflater.inflate(R.layout.dialog_no_photos_for_ground, container, false);

        Button setTriggersBtn=view.findViewById(R.id.go_to_set_triggers_from_home_btn);
        Button startDefaultGroundBtn=view.findViewById(R.id.start_ground_with_default_settings_btn);

        setTriggersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SetTriggersActivity.class);
                dismiss();
                startActivity(intent);
            }
        });

        startDefaultGroundBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), GroundActivity.class);
                intent.putExtra("default_settings", true);
                dismiss();
                startActivity(intent);
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
