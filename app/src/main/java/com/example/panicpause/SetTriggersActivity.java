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
    private Set<String> userTriggers = new HashSet<>();

    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_triggers);

        dataManager = new DataManager(this);

        initializeViews();
        setupRecyclerView();

        loadTriggersFromLocal();
        loadUserTriggers();

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

    private void initializeViews(){
        backBtn=findViewById(R.id.back_btn);
        triggersListRV=findViewById(R.id.triggers_recycler_view);
    }

    private void setupRecyclerView(){
        // LinearLayoutManager для вертикального прокручиваемого списка
        triggersListRV.setLayoutManager(new LinearLayoutManager(this));
        // изначально адаптер создается с пустым списком, позже обновляется после загрузки данных
        triggersAdapter=new TriggersRecycleViewAdapter(allTriggerItems,this);
        triggersListRV.setAdapter(triggersAdapter);
    }

    private void loadTriggersFromLocal() {
        allTriggerItems = dataManager.getLocalTagsList();
        triggersAdapter.updateItems(allTriggerItems);
    }

    private void loadUserTriggers() {
        userTriggers = new HashSet<>(dataManager.getTriggers());
        triggersAdapter.setUserSelectedTriggers(userTriggers);
    }

    /**
     * Метод onCategoryClick отвечает за клики для раскрытия/скрытия
     * @param category категория тега (родительский, дочерний)
     * @param position позиция тега
     */
    @Override
    public void onCategoryClick(TriggerItem category, int position) {
        triggersAdapter.toggleCategory(position);
    }

    /**
     * Метод onTriggerClick отвечает за клики по кнопкам плюс/галочка
     * @param trigger тег
     * @param plusButton кнопка плюс
     * @param isCurrentlySelected выбран ли тег сейчас триггером
     */
    @Override
    public void onTriggerClick(TriggerItem trigger, ImageButton plusButton, boolean isCurrentlySelected) {
        if (isCurrentlySelected) {
            // удалить триггер
            userTriggers.remove(trigger.getImgTag());
        } else {
            // добавить триггер
            userTriggers.add(trigger.getImgTag());
        }
        // обновить адаптер и сохранить данные локально
        triggersAdapter.setUserSelectedTriggers(userTriggers);
        // сохранить в локальное хранилище (и синхронизировать в Firestore, если пользователь не гость)
        dataManager.saveTriggers(new ArrayList<>(userTriggers));
    }

}
