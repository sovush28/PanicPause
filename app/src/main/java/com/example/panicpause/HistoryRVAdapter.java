package com.example.panicpause;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

// Адаптер для отображения списка сессий в истории упражнений
// Каждый элемент показывает дату и первые 2 фото из сессии
public class HistoryRVAdapter extends RecyclerView.Adapter<HistoryRVAdapter.HistoryRVViewHolder> {

    // Список сессий для отображения
    private final List<DataManager.ExerciseSession> sessions;
    // Контекст для загрузки изображений
    private final Context context;
    // Слушатель кликов на элементах
    private final OnHistoryItemClickListener listener;

    // Слушатель кликов на элементах истории.
    public interface OnHistoryItemClickListener {
        void onItemClick(DataManager.ExerciseSession session);
    }

    public HistoryRVAdapter(List<DataManager.ExerciseSession> sessions, Context context,
                          OnHistoryItemClickListener listener) {
        this.sessions = sessions;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryRVViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Создаём представление из макета элемента истории
        View view = LayoutInflater.from(context)
                .inflate(R.layout.history_item_layout, parent, false);
        return new HistoryRVViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryRVViewHolder holder, int position) {
        // Получаем сессию для текущей позиции
        DataManager.ExerciseSession session = sessions.get(position);

        // Отображаем дату сессии
        holder.dateBtn.setText(session.getFormattedDate(context));

        // Обработка фото: показываем первые 2 фото из сессии
        List<DataManager.PhotoData> photos = session.photos;

        if (photos.isEmpty()) {
            // Нет фото — скрываем оба изображения
            holder.imageView1.setVisibility(View.GONE);
            holder.imageView2.setVisibility(View.GONE);
        } else if (photos.size() == 1) {
            // Одно фото — показываем только первое
            loadPhoto(photos.get(0), holder.imageView1);
            holder.imageView1.setVisibility(View.VISIBLE);
            holder.imageView2.setVisibility(View.INVISIBLE);
        } else {
            // Два или больше фото — показываем первые два
            loadPhoto(photos.get(0), holder.imageView1);
            loadPhoto(photos.get(1), holder.imageView2);
            holder.imageView1.setVisibility(View.VISIBLE);
            holder.imageView2.setVisibility(View.VISIBLE);
        }

        // Обработка клика на элемент
        holder.itemLayout.setOnClickListener(v -> { //itemview -> itemlayout
            if (listener != null) {
                listener.onItemClick(session);
            }
        });
        holder.dateBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(session);
            }
        });
    }

    // Загружает фото в изображение с помощью Glide.
    // Сначала пытаемся загрузить локальный файл, если его нет — загружаем по URL.
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
        return sessions.size();
    }

    // ViewHolder для элемента истории
    static class HistoryRVViewHolder extends RecyclerView.ViewHolder {
        Button dateBtn;
        com.google.android.material.imageview.ShapeableImageView imageView1, imageView2;
        LinearLayout itemLayout;

        HistoryRVViewHolder(@NonNull View itemView) {
            super(itemView);
            dateBtn = itemView.findViewById(R.id.history_item_date_btn);
            imageView1 = itemView.findViewById(R.id.history_item_image_1);
            imageView2 = itemView.findViewById(R.id.history_item_image_2);
            itemLayout = itemView.findViewById(R.id.history_item_layout);
        }
    }


}
