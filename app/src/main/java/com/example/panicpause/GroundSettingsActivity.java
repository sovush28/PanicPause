package com.example.panicpause;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class GroundSettingsActivity extends AppCompatActivity {

    ImageButton backBtn;
    Button resetBtn;
    ImageView breathQIncreaseIV, breathQDecreaseIV, photoQIncreaseIV, photoQDecreaseIV;
    EditText breathQET, photoQET;
    Switch useMathSwitch, useColorSwitch, favesOnlySwitch, groundOnLaunchSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ground_settings);

        InitializeViews();
        SetListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void inDevelopmentToast(){
        Toast.makeText(this, R.string.in_development, Toast.LENGTH_SHORT).show();
    }

    private void InitializeViews(){
        backBtn=findViewById(R.id.back_btn);
        resetBtn=findViewById(R.id.reset_settings_btn);
        breathQIncreaseIV=findViewById(R.id.breath_amount_increase_iv);
        breathQDecreaseIV=findViewById(R.id.breath_amount_decrease_iv);
        photoQIncreaseIV=findViewById(R.id.photo_ex_amount_increase_iv);
        photoQDecreaseIV=findViewById(R.id.photo_ex_amount_decrease_iv);
        breathQET=findViewById(R.id.breath_repeat_amount_et);
        photoQET=findViewById(R.id.photo_ex_amount_et);
        useMathSwitch=findViewById(R.id.use_math_toggle_sc);
        useColorSwitch=findViewById(R.id.use_color_toggle_sc);
        favesOnlySwitch=findViewById(R.id.use_faves_only_toggle_sc);
        groundOnLaunchSwitch=findViewById(R.id.ground_on_launch_toggle_sc);
    }

    private void SetListeners(){
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inDevelopmentToast();
                //TODO
            }
        });

        breathQIncreaseIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        breathQDecreaseIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        photoQIncreaseIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        photoQDecreaseIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        //set on text change listeners

        useMathSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(useMathSwitch.isChecked()){
                    //use math = true
                }
                else{
                    //use math = false
                }
            }
        });
        useColorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(useColorSwitch.isChecked()){
                    //use count color = true
                }
                else{
                    //use count color = false
                }
            }
        });
        groundOnLaunchSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(groundOnLaunchSwitch.isChecked()){
                    //ground on launch=true
                }
                else{
                    //ground on launch=false
                }
            }
        });
    }
}