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

// Адаптер для отображения списка тегов у фото.
// Показывает теги с разным стилем в зависимости от того, выбраны ли они как триггеры.
public class HistoryTriggerRVAdapter extends RecyclerView.Adapter<HistoryTriggerRVAdapter.HistoryTriggerRVViewHolder> {

    // Список тегов фото
    private final List<TriggerItem> photoTags;
    // Список триггеров пользователя
    private List<String> userTriggers;
    // Слушатель кликов на тегах
    private final OnHistoryTriggerActionListener listener;

    // Слушатель действий с тегами
    public interface OnHistoryTriggerActionListener {
        /**
         * Вызывается при клике на тег.
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
        // Создаём представление из макета элемента тега
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_dialog_trigger_rv_item, parent, false);
        return new HistoryTriggerRVViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryTriggerRVViewHolder holder, int position) {
        TriggerItem tagItem = photoTags.get(position); // Теперь это объект

        // ОТОБРАЖЕНИЕ ЛОКАЛИЗОВАННОЙ СТРОКИ
        /*String displayText = tagItem.getStrRes(); // Ключ строки, например "animals_label"

        try {
            // Преобразуем ключ в строку из resources
            int stringId = holder.itemView.getContext().getResources()
                    .getIdentifier(displayText, "string", holder.itemView.getContext().getPackageName());
            if (stringId != 0) {
                holder.tagTV.setText(stringId);
            } else {
                holder.tagTV.setText(displayText); // fallback
            }
        } catch (Exception e) {
            holder.tagTV.setText(displayText);
        }*/

        if (tagItem.getNameRus()!=null)
            holder.tagTV.setText(tagItem.getNameRus());

        // ПРОВЕРКА: ВЫБРАН ЛИ ТРИГГЕР
        // Сравниваем по imgTag (ключу), а не по отображаемому тексту
        boolean isSelected = userTriggers.contains(tagItem.getImgTag());

        // === ЦВЕТ И ФОН ===
        if (isSelected) {
            holder.itemView.setBackgroundResource(R.drawable.history_trigger_outlined_selected_shape);
            holder.crossIV.setVisibility(View.VISIBLE);
            holder.tagTV.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorBeige));
        } else {
            holder.itemView.setBackgroundResource(R.drawable.history_trigger_filled_unselected_shape);
            holder.crossIV.setVisibility(View.GONE);
            holder.tagTV.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorDarkBlueish));
        }

        // === ОБРАБОТКА КЛИКА ===
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTriggerClick(tagItem.getImgTag(), isSelected); // Передаём ключ
            }
        });


        /*// Получаем тег для текущей позиции
        String tag = photoTags.get(position);

        // Отображаем текст тега
        holder.tagTV.setText(tag);

        // Проверяем, является ли тег триггером пользователя
        boolean isSelected = userTriggers.contains(tag);

        if (isSelected) {
            // Тег выбран — показываем крестик и другой фон
            holder.itemView.setBackgroundResource(R.drawable.history_trigger_outlined_selected_shape);
            holder.crossIV.setVisibility(View.VISIBLE);
            holder.tagTV.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorBeige));
        } else {
            // Тег не выбран — скрываем крестик и показываем другой фон
            holder.itemView.setBackgroundResource(R.drawable.history_trigger_filled_unselected_shape);
            holder.crossIV.setVisibility(View.GONE);
            holder.tagTV.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorDarkBlueish));
        }

        // Обработка клика на тег
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTriggerClick(tag, isSelected);
            }
        });*/
    }

    @Override
    public int getItemCount() {
        return photoTags.size();
    }

    // ViewHolder для элемента тега.
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
        this.userTriggers = new ArrayList<>(newUserTriggers); // Создаём копию для безопасности
        notifyDataSetChanged(); // Можно использовать notifyItemRangeChanged(0, getItemCount()) для оптимизации
    }

}
