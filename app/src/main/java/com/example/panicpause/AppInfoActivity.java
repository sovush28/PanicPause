package com.example.panicpause;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AppInfoActivity extends AppCompatActivity {

    LinearLayout goToGroundSettings, goToSetTriggers, whatsTrigger;

    ImageButton backBtn;

    Button goToMainBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_app_info);

        InitializeViews();
        SetOnClickListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    private void InitializeViews(){
        backBtn=findViewById(R.id.back_btn);
        goToMainBtn=findViewById(R.id.go_to_main_btn);
        goToGroundSettings=findViewById(R.id.go_to_ground_settings_layout);
        goToSetTriggers=findViewById(R.id.go_to_set_triggers_layout);
        whatsTrigger=findViewById(R.id.whats_trigger_layout);
    }

    private void SetOnClickListeners(){
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GoBackToProfile();
            }
        });

        goToMainBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GoToMainActivity();
            }
        });

        goToGroundSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToGroundSettingsActivity();
            }
        });

        goToSetTriggers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToSetTriggersActivity();
            }
        });

        whatsTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWhatsTriggerDialog();
            }
        });
    }

    private void GoToMainActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("go_to_home", true);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
    /*Флаг FLAG_ACTIVITY_CLEAR_TOP убирает все активности над MainActivity
    (включая текущую AppInfoActivity после finish()), а FLAG_ACTIVITY_SINGLE_TOP гарантирует,
    что если MainActivity уже в стеке — она не будет создана заново, а получит вызов onNewIntent().*/

    private void GoBackToProfile(){
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("go_to_profile", true);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        GoBackToProfile();
    }

    private void goToGroundSettingsActivity(){
        Intent intent = new Intent(this, GroundSettingsActivity.class);
        startActivity(intent);
        //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        //finish();
    }

    private void goToSetTriggersActivity(){
        Intent intent = new Intent(this, SetTriggersActivity.class);
        startActivity(intent);
        //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        //finish();
    }

    private void showWhatsTriggerDialog(){
        try{
            WhatsTriggerDialogFragment dialog=new WhatsTriggerDialogFragment();
            dialog.show(getSupportFragmentManager(), "whats_trigger_dialog");
        }
        catch(IllegalStateException ex){
            // Обработка случая, когда Activity уничтожается
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }
}