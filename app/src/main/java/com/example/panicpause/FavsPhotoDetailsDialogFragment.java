package com.example.panicpause;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FavsPhotoDetailsDialogFragment extends DialogFragment {
    private DataManager.PhotoData photo;
    private DataManager dataManager;
    private Context context;

    private List<TriggerItem> allTriggers;

    // интерфейс для уведомления основной активности об удалении элемента
    public interface OnFavDeletedListener {
        void onFavDeleted(DataManager.PhotoData deletedPhoto);
    }

    private OnFavDeletedListener deleteListener;

    public static FavsPhotoDetailsDialogFragment newInstance(
            DataManager.PhotoData photo, DataManager dataManager,
            OnFavDeletedListener listener) {
        FavsPhotoDetailsDialogFragment fragment = new FavsPhotoDetailsDialogFragment();
        fragment.photo = photo;
        fragment.dataManager = dataManager;
        fragment.deleteListener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Аналогично HistorySessionDetailsDialog
        // Но показываем только одно фото и его теги
        // Используем HistoryTriggerRVAdapter для тегов

        // Убираем стандартный фон диалога
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Инфлейтим макет
        View view = inflater.inflate(R.layout.dialog_favs_photo_info, container, false);

        // Получаем контейнер диалога для обработки кликов на фон
        LinearLayout dialogContainer = view.findViewById(R.id.dialog_container);
        dialogContainer.setOnClickListener(v -> {
            // Закрываем диалог при клике на фон
            dismiss();
        });

        com.google.android.material.imageview.ShapeableImageView photoIV = view.findViewById(R.id.favs_view_photo_iv);
        RelativeLayout viewPhotoLayout = view.findViewById(R.id.favs_view_photo_dialog_layout);
        TextView exTextTV = view.findViewById(R.id.favs_ex_text_tv);
        RecyclerView photoTagsRV = view.findViewById(R.id.favs_photo_tags_rv);
        Button deleteFromFavsBtn = view.findViewById(R.id.delete_from_favs_btn);

        context = getContext();

        loadPhoto(photo, photoIV);

        viewPhotoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FullscreenImageDialogFragment dialog = FullscreenImageDialogFragment.newInstance(photo.imgUrl);
                dialog.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "fullscreen_image");
            }
        });

        String exText = getString(R.string.ground_count_img1) + " " + photo.word + " " + getString(R.string.ground_count_img2);
        exTextTV.setText(exText);

        allTriggers = dataManager.getLocalTagsList();

        List<TriggerItem> photoTriggerItems = new ArrayList<>();
        for (String tagKey : photo.tags) {
            TriggerItem item = findTriggerByImgTag(tagKey);
            if (item != null) {
                photoTriggerItems.add(item);
            }
        }

        HistoryTriggerRVAdapter photoTagsAdapter = new HistoryTriggerRVAdapter(
                //photo.tags,
                photoTriggerItems, // List<TriggerItem>
                dataManager.getTriggers(), // List<String> — ключи выбранных триггеров
                new HistoryTriggerRVAdapter.OnHistoryTriggerActionListener() {
                    @Override
                    public void onTriggerClick(String tag, boolean isSelected) {
                        // Обновляем список триггеров пользователя
                        List<String> userTriggers = dataManager.getTriggers();
                        if (isSelected) {
                            userTriggers.remove(tag);
                        } else {
                            userTriggers.add(tag);
                        }
                        // Сохраняем обновлённый список
                        dataManager.saveTriggers(userTriggers);

                        // ОБНОВЛЯЕМ АДАПТЕР ТЕГА НЕМЕДЛЕННО
                        HistoryTriggerRVAdapter photoTagsAdapter =
                                (HistoryTriggerRVAdapter) photoTagsRV.getAdapter();
                        if (photoTagsAdapter != null) {
                            photoTagsAdapter.updateUserTriggers(dataManager.getTriggers());
                        }
                    }
                }
        );

        // Настраиваем список тегов
        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager(context);
        flexboxLayoutManager.setFlexDirection(FlexDirection.ROW); // Горизонтальное направление, слева направо
        flexboxLayoutManager.setFlexWrap(FlexWrap.WRAP); // Автоматический перенос на новую строку
        flexboxLayoutManager.setJustifyContent(JustifyContent.FLEX_START); // Выравнивание по левому краю
        flexboxLayoutManager.setAlignItems(AlignItems.FLEX_START); // Выравнивание по верху
        photoTagsRV.setLayoutManager(flexboxLayoutManager);
        photoTagsRV.setAdapter(photoTagsAdapter);

        deleteFromFavsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteFaveConfirmationDialog(photo);

            }
        });

        return view;
    }

    private void loadPhoto(DataManager.PhotoData photo, com.google.android.material.imageview.ShapeableImageView imageView) {
        // Получаем имя файла из URL
        String filename = DataManager.getFilenameFromUrl(photo.imgUrl);
        if (filename == null) {
            return;
        }

        // Проверяем наличие локального файла
        File photoFile = new File(context.getFilesDir(), "photos/" + filename);

        if (photoFile.exists()) {
            // Загружаем локальный файл
            Glide.with(context)
                    .load(photoFile)
                    //.placeholder(R.drawable.placeholder_image) // Заглушка при загрузке
                    //.error(R.drawable.error_image) // Изображение при ошибке
                    .into(imageView);
        } else {
            // Загружаем по интернету (если есть)
            Glide.with(context)
                    .load(photo.imgUrl)
                    //.placeholder(R.drawable.placeholder_image)
                    //.error(R.drawable.error_image)
                    .into(imageView);
        }
    }

    private TriggerItem findTriggerByImgTag(String imgTag) {
        if (allTriggers == null || imgTag == null) return null;
        for (TriggerItem trigger : allTriggers) {
            if (imgTag.equals(trigger.getImgTag())) {
                return trigger;
            }
        }
        return null;
    }

    private void showDeleteFaveConfirmationDialog(DataManager.PhotoData photo) {
        try{
            DeleteFromFavesDialogFragment dialog = new DeleteFromFavesDialogFragment();
            dialog.setOnDeleteFaveListener(new DeleteFromFavesDialogFragment.OnDeleteFaveListener() {
                @Override
                public void onDeleteFaveConfirmed() { // Пользователь подтвердил удаление из избр
                    List<String> faves = dataManager.getFaves();
                    String photoUrl = photo.imgUrl;

                    if (faves.contains(photoUrl)) {
                        // Удаляем из избранных
                        faves.remove(photoUrl);
                    }

                    // Сохраняем обновлённый список
                    dataManager.saveFaves(faves);

                    if (deleteListener != null) {
                        deleteListener.onFavDeleted(photo);
                    }
                    dismiss();
                }

                @Override
                public void onDeleteFaveCancelled() {
                    // Пользователь отменил удаление
                    dialog.dismiss();
                }
            });

            dialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "delete_fave_dialog");
        }
        catch(IllegalStateException ex){
            // Обработка случая, когда Activity уничтожается
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }

}
