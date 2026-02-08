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

public class DeleteFromFavesDialogFragment extends DialogFragment {

    private DeleteFromFavesDialogFragment.OnDeleteFaveListener deleteFaveListener;

    // Интерфейс для обработки действий
    public interface OnDeleteFaveListener {
        void onDeleteFaveConfirmed();
        void onDeleteFaveCancelled();
    }

    // Устанавливаем слушатель
    public void setOnDeleteFaveListener(DeleteFromFavesDialogFragment.OnDeleteFaveListener listener) {
        this.deleteFaveListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // "Надуваем" кастомный макет
        View view = inflater.inflate(R.layout.dialog_delete_from_favs, container, false);

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
        Button deleteFaveBtn = view.findViewById(R.id.yes_delete_fav_btn);
        Button cancelBtn = view.findViewById(R.id.cancel_btn);

        // Обработчик кнопки подтверждения
        deleteFaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deleteFaveListener != null) {
                    deleteFaveListener.onDeleteFaveConfirmed();
                }
                dismiss(); // Закрываем диалог
            }
        });

        // Обработчик кнопки отмены
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deleteFaveListener != null) {
                    deleteFaveListener.onDeleteFaveCancelled();
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
