package com.example.panicpause;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    ImageButton backBtn;
    LinearLayout randomOrderLayout;
    CheckBox randomOrderCB;
    RecyclerView favoritesRV;

    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_favorites);

        initializeViews();
        setOnClickListeners();

        dataManager = new DataManager(this);

        // Получаем избранные фото
        List<String> faves = dataManager.getFaves();
        List<DataManager.PhotoData> favoritePhotos = new ArrayList<>();

        // Преобразуем URL в PhotoData
        List<DataManager.PhotoData> allPhotos = dataManager.getLocalImagesList();
        for (String url : faves) {
            for (DataManager.PhotoData photo : allPhotos) {
                if (photo.imgUrl.equals(url)) {
                    favoritePhotos.add(photo);
                    break;
                }
            }
        }

        if(favoritePhotos.isEmpty()){
            Toast.makeText(this, getText(R.string.no_favs), Toast.LENGTH_SHORT).show();
        }
        else{
            // Создаём адаптер
            FavoritesRVAdapter adapter = new FavoritesRVAdapter(favoritePhotos, this, dataManager);
            favoritesRV.setAdapter(adapter);

            // Настройка сетки
            GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
            favoritesRV.setLayoutManager(layoutManager);

            // Добавляем отступы
            int spacing = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 22, getResources().getDisplayMetrics());
            favoritesRV.addItemDecoration(new GridSpacingItemDecoration(spacing));
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    private void initializeViews(){
        backBtn=findViewById(R.id.back_btn);
        randomOrderLayout=findViewById(R.id.favs_random_order_layout);
        randomOrderCB=findViewById(R.id.favs_random_order_checkbox);
        favoritesRV=findViewById(R.id.favorites_rv);
    }

    private void setOnClickListeners(){
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        randomOrderLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO checkbox checked/unchecked and random order for groundfav activity
            }
        });
    }
}