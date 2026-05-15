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

/**
 * HistoryRVAdapter - адаптер для отображения списка сессий в истории упражнений.
 * Каждый элемент показывает дату и первые 2 фото из сессии.
 */
public class HistoryRVAdapter extends RecyclerView.Adapter<HistoryRVAdapter.HistoryRVViewHolder> {
    private final List<DataManager.ExerciseSession> sessions;    // список сессий для отображения
    private final Context context;    // контекст для загрузки изображений

    private final OnHistoryItemClickListener listener;
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
        View view = LayoutInflater.from(context)
                .inflate(R.layout.history_item_layout, parent, false);
        return new HistoryRVViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryRVViewHolder holder, int position) {
        DataManager.ExerciseSession session = sessions.get(position);
        holder.dateBtn.setText(session.getFormattedDate(context));

        // обработка фото: отображение первых 2 фото из сессии
        List<DataManager.PhotoData> photos = session.photos;

        if (photos.isEmpty()) {
            // нет фото - скрыть оба изображения
            holder.imageView1.setVisibility(View.GONE);
            holder.imageView2.setVisibility(View.GONE);
        } else if (photos.size() == 1) {
            // одно фото - показать только первое
            loadPhoto(photos.get(0), holder.imageView1);
            holder.imageView1.setVisibility(View.VISIBLE);
            holder.imageView2.setVisibility(View.INVISIBLE);
        } else {
            // два или больше фото - показать первые два
            loadPhoto(photos.get(0), holder.imageView1);
            loadPhoto(photos.get(1), holder.imageView2);
            holder.imageView1.setVisibility(View.VISIBLE);
            holder.imageView2.setVisibility(View.VISIBLE);
        }

        holder.itemLayout.setOnClickListener(v -> {
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

    private void loadPhoto(DataManager.PhotoData photo, com.google.android.material.imageview.ShapeableImageView imageView) {
        String filename = DataManager.getFilenameFromUrl(photo.imgUrl);
        if (filename == null) {
            return;
        }

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
        return sessions.size();
    }

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
