package com.example.panicpause;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private ImageButton backBtn;
    private RecyclerView historyRV;

    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        initializeViews();
        setOnClickListeners();

        dataManager = new DataManager(this);

        // настройка списка
        historyRV.setLayoutManager(new LinearLayoutManager(this));

        dataManager.syncExerciseHistoryFromFirestore(()->{
            // загрузка истории из локального хранилища
            List<DataManager.ExerciseSession> sessions = dataManager.loadExerciseHistory();
            if (sessions.isEmpty()) {
                Toast.makeText(this, getText(R.string.history_empty), Toast.LENGTH_SHORT).show();
            } else {
                // создание и установка адаптера
                HistoryRVAdapter historyRVAdapter = new HistoryRVAdapter(
                        sessions,
                        this,
                        this::showHistorySessionDetailsDialog
                );
                historyRV.setAdapter(historyRVAdapter);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    private void initializeViews(){
        backBtn=findViewById(R.id.back_btn);
        historyRV=findViewById(R.id.history_rv);
    }

    private void setOnClickListeners(){
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * Метод showHistorySessionDetailsDialog показывает диалог с деталями выбранной сессии.
     * @param session сессия для отображения
     */
    private void showHistorySessionDetailsDialog(DataManager.ExerciseSession session) {
        HistorySessionDetailsDialogFragment dialog = HistorySessionDetailsDialogFragment.newInstance(session, dataManager);
        dialog.show(getSupportFragmentManager(), "history_session_details");
    }

}