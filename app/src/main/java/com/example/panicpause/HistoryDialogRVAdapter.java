package com.example.panicpause;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
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

// Адаптер для отображения списка фото в диалоге деталей сессии.
// Каждый элемент показывает одно фото и его теги.
public class HistoryDialogRVAdapter extends RecyclerView.Adapter<HistoryDialogRVAdapter.HistoryDialogRVViewHolder> {

    // Список фото для отображения
    private final List<DataManager.PhotoData> photos;
    // Контекст для загрузки изображений
    private final Context context;
    // Менеджер данных для получения триггеров пользователя
    private final DataManager dataManager;

    private final List<TriggerItem> allTriggers;

    public HistoryDialogRVAdapter(List<DataManager.PhotoData> photos, Context context, DataManager dataManager) {
        this.photos = photos;
        this.context = context;
        this.dataManager = dataManager;
        this.allTriggers = dataManager.getLocalTagsList();
    }

    @NonNull
    @Override
    public HistoryDialogRVViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Создаём представление из макета элемента диалога
        View view = LayoutInflater.from(context)
                .inflate(R.layout.history_dialog_main_rv_item, parent, false);
        return new HistoryDialogRVViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryDialogRVViewHolder holder, int position) {
        // Получаем фото для текущей позиции
        DataManager.PhotoData photo = photos.get(position);

        // Загружаем фото
        loadPhoto(photo, holder.historyPhotoIV);

        List<TriggerItem> photoTriggerItems = new ArrayList<>();
        for (String tagKey : photo.tags) {
            TriggerItem item = findTriggerByImgTag(tagKey);
            if (item != null) {
                photoTriggerItems.add(item);
            }
        }

        // Создаём адаптер для тегов этого фото
        HistoryTriggerRVAdapter triggerAdapter = new HistoryTriggerRVAdapter(
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
                        HistoryTriggerRVAdapter triggerAdapter =
                                (HistoryTriggerRVAdapter) holder.historyTriggerRV.getAdapter();
                        if (triggerAdapter != null) {
                            triggerAdapter.updateUserTriggers(dataManager.getTriggers());
                        }
                    }
                }
        );

        // Настраиваем список тегов
        //holder.historyTriggerRV.setLayoutManager(new LinearLayoutManager(context));
        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager(context);
        flexboxLayoutManager.setFlexDirection(FlexDirection.ROW); // Горизонтальное направление, слева направо
        flexboxLayoutManager.setFlexWrap(FlexWrap.WRAP); // Автоматический перенос на новую строку
        flexboxLayoutManager.setJustifyContent(JustifyContent.FLEX_START); // Выравнивание по левому краю
        flexboxLayoutManager.setAlignItems(AlignItems.FLEX_START); // Выравнивание по верху
        holder.historyTriggerRV.setLayoutManager(flexboxLayoutManager);

        holder.historyTriggerRV.setAdapter(triggerAdapter);

        holder.historyPhotoIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FullscreenImageDialog dialog = FullscreenImageDialog.newInstance(photo.imgUrl);
                dialog.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "fullscreen_image");
            }
        });

    }

    // Загружает фото в изображение с помощью Glide
    // Сначала пытаемся загрузить локальный файл, если его нет — загружаем по URL
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

    @Override
    public int getItemCount() {
        return photos.size();
    }

    // ViewHolder для элемента диалога.
    static class HistoryDialogRVViewHolder extends RecyclerView.ViewHolder {
        com.google.android.material.imageview.ShapeableImageView historyPhotoIV;
        RecyclerView historyTriggerRV;
        ImageButton favoriteBtn;

        HistoryDialogRVViewHolder(@NonNull View itemView) {
            super(itemView);
            historyPhotoIV = itemView.findViewById(R.id.history_dialog_photo_iv);
            historyTriggerRV = itemView.findViewById(R.id.history_dialog_trigger_rv);
            favoriteBtn = itemView.findViewById(R.id.history_dialog_fav_ib);

            // TODO: функционал избранного

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

}
