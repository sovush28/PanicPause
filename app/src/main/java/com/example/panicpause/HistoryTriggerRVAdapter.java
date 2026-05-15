package com.example.panicpause;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * HistoryTriggerRVAdapter - адаптер для отображения списка тегов у фото.
 * Показывает теги с разным стилем в зависимости от того, выбраны ли они как триггеры.
 */
public class HistoryTriggerRVAdapter extends RecyclerView.Adapter<HistoryTriggerRVAdapter.HistoryTriggerRVViewHolder> {
    private final List<TriggerItem> photoTags;
    private List<String> userTriggers;

    private final OnHistoryTriggerActionListener listener;
    public interface OnHistoryTriggerActionListener {
        /**
         * Метод onTriggerClick вызывается при клике на тег.
         * @param tag Тег, на который кликнули
         * @param isSelected true, если тег уже выбран как триггер
         */
        void onTriggerClick(String tag, boolean isSelected);
    }

    public HistoryTriggerRVAdapter(List<TriggerItem> photoTags, List<String> userTriggers,
                          OnHistoryTriggerActionListener listener) {
        this.photoTags = photoTags;
        this.userTriggers = userTriggers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryTriggerRVViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_dialog_trigger_rv_item, parent, false);
        return new HistoryTriggerRVViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryTriggerRVViewHolder holder, int position) {
        TriggerItem tagItem = photoTags.get(position);

        if (tagItem.getNameRus()!=null)
            holder.tagTV.setText(tagItem.getNameRus());

        // проверка, выбран ли тег (отмечен ли тег как триггер)
        boolean isSelected = userTriggers.contains(tagItem.getImgTag());

        // установка цвета и фона
        if (isSelected) {
            holder.itemView.setBackgroundResource(R.drawable.history_trigger_outlined_selected_shape);
            holder.crossIV.setVisibility(View.VISIBLE);
            holder.tagTV.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorBeige));
        } else {
            holder.itemView.setBackgroundResource(R.drawable.history_trigger_filled_unselected_shape);
            holder.crossIV.setVisibility(View.GONE);
            holder.tagTV.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorDarkBlueish));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTriggerClick(tagItem.getImgTag(), isSelected);
            }
        });
    }

    @Override
    public int getItemCount() {
        return photoTags.size();
    }

    static class HistoryTriggerRVViewHolder extends RecyclerView.ViewHolder {
        TextView tagTV;
        ImageView crossIV;

        HistoryTriggerRVViewHolder(@NonNull View itemView) {
            super(itemView);
            tagTV = itemView.findViewById(R.id.history_trigger_tv);
            crossIV = itemView.findViewById(R.id.history_trigger_x_iv);
        }
    }

    public void updateUserTriggers(List<String> newUserTriggers) {
        this.userTriggers = new ArrayList<>(newUserTriggers); // копия для безопасности
        notifyDataSetChanged(); // можно использовать notifyItemRangeChanged(0, getItemCount()) для оптимизации
    }

}
