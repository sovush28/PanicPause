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

/**
 * HistorySessionDetailsDialogFragment - диалог для отображения деталей выбранной сессии.
 * Показывает все фото из сессии с их тегами.
 */
public class HistorySessionDetailsDialogFragment extends DialogFragment {

    // ключи для передачи данных через аргументы
    private static final String ARG_SESSION = "session";
    private static final String ARG_DATA_MANAGER = "data_manager";

    // данные пройденной сессии
    private DataManager.ExerciseSession session;
    private DataManager dataManager;

    public static HistorySessionDetailsDialogFragment newInstance(
            DataManager.ExerciseSession session,
            DataManager dataManager) {
        HistorySessionDetailsDialogFragment fragment = new HistorySessionDetailsDialogFragment();
        fragment.setSession(session);
        fragment.setDataManager(dataManager);
        return fragment;
    }

    public void setSession(DataManager.ExerciseSession session) {
        this.session = session;
    }

    public void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // убрать стандартный фон диалога
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View view = inflater.inflate(R.layout.dialog_history_item_details, container, false);

        // получить список фото из сессии
        RecyclerView photoRecyclerView = view.findViewById(R.id.history_dialog_main_rv);

        // создать адаптер для отображения фото
        HistoryDialogRVAdapter adapter = new HistoryDialogRVAdapter(
                session.photos,
                requireContext(),
                dataManager
        );

        // слушатель для обновления всего списка при изменении триггеров
        adapter.setOnTriggersChangedListener(() -> {
            // полное обновление списка фото перестроит все списки тегов с актуальными данными
            adapter.notifyDataSetChanged();
        });

        photoRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        photoRecyclerView.setAdapter(adapter);

        LinearLayout dialogContainer = view.findViewById(R.id.dialog_container);
        dialogContainer.setOnClickListener(v -> {
            // закрывать диалог при клике на фон
            dismiss();
        });
        return view;
    }
}
