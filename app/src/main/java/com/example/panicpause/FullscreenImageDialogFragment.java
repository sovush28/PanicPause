package com.example.panicpause;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import java.io.File;

/**
 * FullscreenImageDialogFragment - DialogFragment для полноэкранного просмотра фото.
 * Показывает изображение по URL, поддерживает локальные и сетевые файлы.
 * Закрывается по клику на изображение или фон.
 */
public class FullscreenImageDialogFragment extends DialogFragment {
    private String imageUrl;
    private Context context;

    /**
     * Метод newInstance создаёт новый экземпляр диалога.
     * @param imageUrl URL или путь к изображению
     * @return Новый экземпляр диалога
     */
    public static FullscreenImageDialogFragment newInstance(String imageUrl) {
        FullscreenImageDialogFragment fragment = new FullscreenImageDialogFragment();
        Bundle args = new Bundle();
        args.putString("image_url", imageUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // стиль без заголовка
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        // получение данных из аргументов
        if (getArguments() != null) {
            imageUrl = getArguments().getString("image_url");
        }
        context = requireContext();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // убрать фон окна
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return inflater.inflate(R.layout.dialog_fullscreen_image, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        androidx.appcompat.widget.AppCompatImageView fullscreenImageView =
                view.findViewById(R.id.fullscreen_image_view);

        // загрузка изображения
        loadPhoto(imageUrl, fullscreenImageView);

        // закрытие по клику на изображение
        fullscreenImageView.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        // настройка размеров диалога
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private void loadPhoto(String imageUrl, androidx.appcompat.widget.AppCompatImageView imageView) {
        if (imageUrl == null) return;

        String filename = DataManager.getFilenameFromUrl(imageUrl);
        if (filename == null) return;

        File photoFile = new File(context.getFilesDir(), "photos/" + filename);

        if (photoFile.exists()) {
            Glide.with(this)
                    .load(photoFile)
                    .into(imageView);
        } else {
            Glide.with(this)
                    .load(imageUrl)
                    .into(imageView);
        }
    }
}
