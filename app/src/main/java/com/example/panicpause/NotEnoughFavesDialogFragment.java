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

public class NotEnoughFavesDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // "Надуваем" кастомный макет
        View view = inflater.inflate(R.layout.dialog_no_faves_for_ground, container, false);

        Button turnOffFavesOnlyBtn=view.findViewById(R.id.turn_off_faves_only_btn);
        Button ignoreFavesOnlyBtn=view.findViewById(R.id.launch_ground_faves_only_turned_off_btn);
        Button goToHistoryBtn=view.findViewById(R.id.go_to_history_btn);

        DataManager dataManager = new DataManager(requireContext());

        turnOffFavesOnlyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataManager.saveUserSetting("use_faves_only", false);
                Intent intent = new Intent(getActivity(), GroundActivity.class);
                intent.putExtra("faves_only", false);
                dismiss();
                startActivity(intent);
            }
        });

        ignoreFavesOnlyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), GroundActivity.class);
                intent.putExtra("faves_only", false);
                dismiss();
                startActivity(intent);
            }
        });

        goToHistoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), HistoryActivity.class);
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
