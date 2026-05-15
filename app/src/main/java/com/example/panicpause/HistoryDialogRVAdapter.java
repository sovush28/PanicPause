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

/**
 * HistoryDialogRVAdapter - адаптер для отображения списка фото в диалоге деталей пройденной сессии.
 * Каждый элемент показывает одно фото и его теги.
 */
public class HistoryDialogRVAdapter extends RecyclerView.Adapter<HistoryDialogRVAdapter.HistoryDialogRVViewHolder> {
    private final List<DataManager.PhotoData> photos;    // список фото для отображения
    private final Context context;    // контекст для загрузки изображений
    private final DataManager dataManager;
    private final List<TriggerItem> allTriggers;
    private OnTriggersChangedListener triggersChangedListener;    // слушатель для обновления всего списка при изменении триггеров

    // интерфейс для уведомления об изменении триггеров
    public interface OnTriggersChangedListener {
        void onTriggersChanged();
    }

    // Метод для установки слушателя (вызывается из диалога/фрагмента)

    /**
     * setOnTriggersChangedListener - метод для установки слушателя (вызывается из диалога/фрагмента)
     * @param listener слушатель
     */
    public void setOnTriggersChangedListener(OnTriggersChangedListener listener) {
        this.triggersChangedListener = listener;
    }

    public HistoryDialogRVAdapter(List<DataManager.PhotoData> photos, Context context, DataManager dataManager) {
        this.photos = photos;
        this.context = context;
        this.dataManager = dataManager;
        this.allTriggers = dataManager.getLocalTagsList();
    }

    @NonNull
    @Override
    public HistoryDialogRVViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.history_dialog_main_rv_item, parent, false);
        return new HistoryDialogRVViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryDialogRVViewHolder holder, int position) {
        // получение фото для текущей позиции
        DataManager.PhotoData photo = photos.get(position);
        // загрузка фото
        loadPhoto(photo, holder.historyPhotoIV);

        List<TriggerItem> photoTriggerItems = new ArrayList<>();
        for (String tagKey : photo.tags) {
            TriggerItem item = findTriggerByImgTag(tagKey);
            if (item != null) {
                photoTriggerItems.add(item);
            }
        }

        // создание адаптера для тегов этого фото
        HistoryTriggerRVAdapter triggerAdapter = new HistoryTriggerRVAdapter(
                photoTriggerItems, // List<TriggerItem>
                new ArrayList<>(dataManager.getTriggers()),
                new HistoryTriggerRVAdapter.OnHistoryTriggerActionListener() {
                    @Override
                    public void onTriggerClick(String tag, boolean isSelected) {
                        // обновление список триггеров пользователя
                        List<String> userTriggers = dataManager.getTriggers();
                        if (isSelected) {
                            userTriggers.remove(tag);
                        } else {
                            userTriggers.add(tag);
                        }
                        dataManager.saveTriggers(userTriggers);

                        // уведомление родительского адаптера об изменении триггеров
                        // post() для безопасности - отложить обновление до завершения текущего цикла обработки событий
                        // (предотвращает возможные исключения при вызове notifyDataSetChanged() во время привязки данных)
                        holder.itemView.post(() -> {
                            if (triggersChangedListener != null) {
                                triggersChangedListener.onTriggersChanged();
                            }
                        });
                    }
                }
        );

        // настройка списка тегов
        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager(context);
        flexboxLayoutManager.setFlexDirection(FlexDirection.ROW); // горизонтальное направление, слева направо
        flexboxLayoutManager.setFlexWrap(FlexWrap.WRAP); // автоматический перенос на новую строку
        flexboxLayoutManager.setJustifyContent(JustifyContent.FLEX_START); // выравнивание по левому краю
        flexboxLayoutManager.setAlignItems(AlignItems.FLEX_START); // выравнивание по верху
        holder.historyTriggerRV.setLayoutManager(flexboxLayoutManager);
        holder.historyTriggerRV.setAdapter(triggerAdapter);

        holder.historyPhotoIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FullscreenImageDialogFragment dialog = FullscreenImageDialogFragment.newInstance(photo.imgUrl);
                dialog.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "fullscreen_image");
            }
        });

        if(dataManager.getFaves().contains(photo.imgUrl)){
            holder.favoriteBtn.setImageResource(R.drawable.fav_heart_selected);
        }
        else{
            holder.favoriteBtn.setImageResource(R.drawable.fav_heart_unselected);
        }

        holder.favoriteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> faves = dataManager.getFaves();
                String photoUrl = photo.imgUrl;

                if (faves.contains(photoUrl)) {
                    // удаление из избранных
                    faves.remove(photoUrl);
                    holder.favoriteBtn.setImageResource(R.drawable.fav_heart_unselected);
                } else {
                    // добавление в избранные
                    faves.add(photoUrl);
                    holder.favoriteBtn.setImageResource(R.drawable.fav_heart_selected);
                }
                dataManager.saveFaves(faves);
            }
        });

    }

    /**
     * Метод loadPhoto загружает фото в изображение с помощью Glide.
     * Сначала пытается загрузить локальный файл, если его нет - загружает по URL.
     * @param photo данные о фото
     * @param imageView imageView, куда загружается фото
     */
    private void loadPhoto(DataManager.PhotoData photo, com.google.android.material.imageview.ShapeableImageView imageView) {
        String filename = DataManager.getFilenameFromUrl(photo.imgUrl);
        if (filename == null) {
            return;
        }
        // проверка наличия локального файла
        File photoFile = new File(context.getFilesDir(), "photos/" + filename);
        if (photoFile.exists()) {
            // загрузка локального файла
            Glide.with(context)
                    .load(photoFile)
                    //.placeholder(R.drawable.placeholder_image) // заглушка при загрузке
                    //.error(R.drawable.error_image) // изображение при ошибке
                    .into(imageView);
        } else {
            // загрузка по интернету (если есть)
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

    static class HistoryDialogRVViewHolder extends RecyclerView.ViewHolder {
        com.google.android.material.imageview.ShapeableImageView historyPhotoIV;
        RecyclerView historyTriggerRV;
        ImageButton favoriteBtn;
        HistoryDialogRVViewHolder(@NonNull View itemView) {
            super(itemView);
            historyPhotoIV = itemView.findViewById(R.id.history_dialog_photo_iv);
            historyTriggerRV = itemView.findViewById(R.id.history_dialog_trigger_rv);
            favoriteBtn = itemView.findViewById(R.id.history_dialog_fav_ib);
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
