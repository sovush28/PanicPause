package com.example.panicpause;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Диалог для отображения деталей выбранной сессии
// Показывает все фото из сессии с их тегами
public class HistorySessionDetailsDialog extends DialogFragment {

    // Ключи для передачи данных через аргументы
    private static final String ARG_SESSION = "session";
    private static final String ARG_DATA_MANAGER = "data_manager";

    // Данные сессии
    private DataManager.ExerciseSession session;
    private DataManager dataManager;

    /**
     * Создаёт новый экземпляр диалога с данными сессии.
     *
     * @param session Сессия для отображения
     * @param dataManager Менеджер данных для доступа к триггерам
     * @return Новый экземпляр диалога
     */
    public static HistorySessionDetailsDialog newInstance(
            DataManager.ExerciseSession session,
            DataManager dataManager) {
        HistorySessionDetailsDialog fragment = new HistorySessionDetailsDialog();
        //Bundle args = new Bundle();
        //args.putSerializable(ARG_SESSION, session);
        // DataManager не сериализуется, поэтому передаём через сеттер
        fragment.setSession(session);
        fragment.setDataManager(dataManager);
        //fragment.setArguments(args);
        return fragment;
    }

    // Устанавливает сессию для отображения.
    public void setSession(DataManager.ExerciseSession session) {
        this.session = session;
    }

    // Устанавливает менеджер данных.
    public void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Устанавливаем стиль диалога — без заголовка и с прозрачным фоном
        //setStyle(STYLE_NO_TITLE, R.style.HistoryDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Убираем стандартный фон диалога
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Инфлейтим макет
        View view = inflater.inflate(R.layout.dialog_history_item_details, container, false);

        // Получаем список фото из сессии
        RecyclerView photoRecyclerView = view.findViewById(R.id.history_dialog_main_rv);

        // Создаём адаптер для отображения фото
        HistoryDialogRVAdapter adapter = new HistoryDialogRVAdapter(
                session.photos,
                requireContext(),
                dataManager
        );

        // Настраиваем список
        photoRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        photoRecyclerView.setAdapter(adapter);

        // Получаем контейнер диалога для обработки кликов на фон
        LinearLayout dialogContainer = view.findViewById(R.id.dialog_container);
        dialogContainer.setOnClickListener(v -> {
            // Закрываем диалог при клике на фон
            dismiss();
        });

        return view;
    }
}
