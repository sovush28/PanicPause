package com.example.panicpause;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SetTriggersActivity extends AppCompatActivity implements TriggersRecycleViewAdapter.OnTriggerClickListener{

    ImageButton backBtn;
    RecyclerView triggersListRV;
    TriggersRecycleViewAdapter triggersAdapter;

    private List<TriggerItem> allTriggerItems=new ArrayList<>();
    private Set<String> userTriggers = new HashSet<>(); // user's selected triggers

    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_triggers);

        dataManager = new DataManager(this);

        InitializeViews();

        SetupRecyclerView();

        // Загружаем триггеры из локального файла
        LoadTriggersFromLocal();
        // Загружаем выбранные триггеры из локального хранилища
        LoadUserTriggers();

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void InitializeViews(){
        backBtn=findViewById(R.id.back_btn);
        triggersListRV=findViewById(R.id.triggers_recycler_view);
    }

    private void SetupRecyclerView(){
        // Use LinearLayoutManager for vertical scrolling list
        triggersListRV.setLayoutManager(new LinearLayoutManager(this));

        // Create adapter with empty list initially, will update when data loads
        triggersAdapter=new TriggersRecycleViewAdapter(allTriggerItems,this);
        triggersListRV.setAdapter(triggersAdapter);
    }

    // Load trigger hierarchy from the local file (tags.json)
    private void LoadTriggersFromLocal() {
        allTriggerItems = dataManager.getLocalTagsList();
        triggersAdapter.updateItems(allTriggerItems);
    }

    // Load user's selected triggers from local storage
    private void LoadUserTriggers() {
        userTriggers = new HashSet<>(dataManager.getTriggers());
        triggersAdapter.setUserSelectedTriggers(userTriggers);
    }

    // Handle category expand/collapse clicks
    @Override
    public void onCategoryClick(TriggerItem category, int position) {
        triggersAdapter.toggleCategory(position);
    }

    // Handle trigger plus/minus button clicks
    @Override
    public void onTriggerClick(TriggerItem trigger, ImageButton plusButton, boolean isCurrentlySelected) {
        if (isCurrentlySelected) {
            // Remove trigger
            userTriggers.remove(trigger.getImgTag());
        } else {
            // Add trigger
            userTriggers.add(trigger.getImgTag());
        }

        // Update adapter and save locally
        triggersAdapter.setUserSelectedTriggers(userTriggers);

        // Сохраняем в локальное хранилище (и синхронизируем в Firestore, если пользователь не гость)
        dataManager.saveTriggers(new ArrayList<>(userTriggers));
    }

}
