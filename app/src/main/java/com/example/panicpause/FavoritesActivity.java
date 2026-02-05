package com.example.panicpause;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

public class FavoritesActivity extends AppCompatActivity {

    ImageButton backBtn;
    LinearLayout randomOrderLayout;
    CheckBox randomOrderCB;
    RecyclerView favoritesRV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_favorites);

        initializeViews();
        setOnClickListeners();



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