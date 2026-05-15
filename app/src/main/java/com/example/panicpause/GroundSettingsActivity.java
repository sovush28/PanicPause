package com.example.panicpause;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
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
    LinearLayout photoExAmountLayout, useMathLayout, useColorLayout, useFavesOnlyLayout;
    Switch useMathSwitch, useColorSwitch, useFavesOnlySwitch;

    private DataManager dataManager;

    private long userBreathRepeatAmount, userPhotoExAmount;
    private boolean userUseMath, userUseCountColor, userUseFavesOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ground_settings);

        initializeViews();
        setListeners();

        dataManager=new DataManager(this);

        loadUserSettings();
        updateUI();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void finishActivity(){
        finish();
    }

    private void loadUserSettings(){
        userBreathRepeatAmount=dataManager.getBreathRepeatAmount();
        userUseMath=dataManager.getUseMath();
        userUseCountColor=dataManager.getUseSearchObjectsColor();
        userPhotoExAmount=dataManager.getGroundPhotoExAmount();
        userUseFavesOnly=dataManager.getUseFavesOnly();
    }

    private void updateUI(){
        breathQET.setText(String.valueOf(userBreathRepeatAmount));
        useMathSwitch.setChecked(userUseMath);
        useColorSwitch.setChecked(userUseCountColor);
        photoQET.setText(String.valueOf(userPhotoExAmount));
        useFavesOnlySwitch.setChecked(userUseFavesOnly);

        if(!userUseMath && !userUseCountColor){
            photoExAmountLayout.setAlpha(1);
            photoQIncreaseIV.setEnabled(true);
            photoQDecreaseIV.setEnabled(true);
        }
        else{
            photoExAmountLayout.setAlpha((float)0.5);
            photoQIncreaseIV.setEnabled(false);
            photoQDecreaseIV.setEnabled(false);
        }

        if(!userUseFavesOnly && dataManager.getFaves().size() < dataManager.getGroundPhotoExAmount()){
            useFavesOnlyLayout.setAlpha((float)0.5);
            useFavesOnlySwitch.setEnabled(false);
        }
        else{
            useFavesOnlyLayout.setAlpha(1);
            useFavesOnlySwitch.setEnabled(true);
        }
        updatePhotoQIncreaseArrow();
    }

    private void updatePhotoQIncreaseArrow(){
        if(userUseFavesOnly && dataManager.getFaves().size() <= userPhotoExAmount){
            photoQIncreaseIV.setEnabled(false);
            photoQIncreaseIV.setAlpha((float)0.5);
        }
        else{
            photoQIncreaseIV.setEnabled(true);
            photoQIncreaseIV.setAlpha((float)1);
        }
    }

    private void initializeViews(){
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
        useFavesOnlySwitch =findViewById(R.id.use_faves_only_toggle_sc);
        photoExAmountLayout=findViewById(R.id.photo_ex_amount_layout);
        useMathLayout=findViewById(R.id.use_math_toggle_layout);
        useColorLayout=findViewById(R.id.use_color_toggle_layout);
        useFavesOnlyLayout=findViewById(R.id.use_faves_only_toggle_layout);
    }

    private void setListeners(){
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAllSettings();
                if(!userUseMath && !userUseCountColor && userPhotoExAmount == 0){
                    showNoExsExceptBreathDialog();
                }
                else{
                    finish();
                }
            }
        });
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResetSettingsConfirmationDialog();
            }
        });
        breathQIncreaseIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userBreathRepeatAmount < 9) {
                    userBreathRepeatAmount++;
                    breathQET.setText(String.valueOf(userBreathRepeatAmount));
                    dataManager.saveUserSetting("breath_repeat_amount", (int) userBreathRepeatAmount);
                }
            }
        });
        breathQDecreaseIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userBreathRepeatAmount > 0) {
                    userBreathRepeatAmount--;
                    breathQET.setText(String.valueOf(userBreathRepeatAmount));
                    dataManager.saveUserSetting("breath_repeat_amount", (int) userBreathRepeatAmount);
                }
            }
        });
        photoQIncreaseIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userPhotoExAmount < 9) {
                    userPhotoExAmount++;
                    photoQET.setText(String.valueOf(userPhotoExAmount));
                    dataManager.saveUserSetting("ground_photo_ex_amount", (int) userPhotoExAmount);
                }
                updateUI();
            }
        });
        photoQDecreaseIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userPhotoExAmount > 0) {
                    userPhotoExAmount--;
                    photoQET.setText(String.valueOf(userPhotoExAmount));
                    dataManager.saveUserSetting("ground_photo_ex_amount", (int) userPhotoExAmount);
                }
                updateUI();
            }
        });
        useMathLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                useMathSwitch.setChecked(!useMathSwitch.isChecked());
            }
        });
        useColorLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                useColorSwitch.setChecked(!useColorSwitch.isChecked());
            }
        });
        useFavesOnlyLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!userUseFavesOnly && dataManager.getFaves().size() < dataManager.getGroundPhotoExAmount()){
                    Toast.makeText(GroundSettingsActivity.this, getString(R.string.not_enough_faves), Toast.LENGTH_SHORT).show();
                }
                else{
                    useFavesOnlySwitch.setChecked(!useFavesOnlySwitch.isChecked());
                }
                updateUI();
            }
        });
        useMathSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                userUseMath = isChecked;
                if(isChecked && userPhotoExAmount != 2){
                    userPhotoExAmount=2;
                    photoQET.setText(String.valueOf(userPhotoExAmount));
                    dataManager.saveUserSetting("ground_photo_ex_amount", 2);
                }
                dataManager.saveUserSetting("use_math", isChecked);
                updateUI();
            }
        });
        useColorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                userUseCountColor = isChecked;
                if(isChecked && userPhotoExAmount != 2){
                    userPhotoExAmount=2;
                    photoQET.setText(String.valueOf(userPhotoExAmount));
                    dataManager.saveUserSetting("ground_photo_ex_amount", 2);
                }
                dataManager.saveUserSetting("use_search_objects_color", isChecked);
                updateUI();
            }
        });
        useFavesOnlySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                userUseFavesOnly = isChecked;
                dataManager.saveUserSetting("use_faves_only", isChecked);
                updatePhotoQIncreaseArrow();
            }
        });
    }

    private void showResetSettingsConfirmationDialog(){
        try{
            ResetSettingsDIalogFragment dialog = new ResetSettingsDIalogFragment();
            dialog.setResetSettingsListener(new ResetSettingsDIalogFragment.OnResetSettingsListener() {
                @Override
                public void onResetSettingsConfirmed() {
                    allSettingsToDefault();
                    saveAllSettings();
                    updateUI();
                }
                @Override
                public void onResetSettingsCancelled() {
                    dialog.dismiss();
                }
            });
            dialog.show(getSupportFragmentManager(), "reset_settings_dialog");
        }
        catch(IllegalStateException ex){
            // обработка случая, когда Activity уничтожается
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }

    private void allSettingsToDefault(){
        userBreathRepeatAmount=1;
        userUseMath=true;
        userUseCountColor=true;
        userPhotoExAmount=2;
        userUseFavesOnly=false;
    }

    private void saveAllSettings(){
        dataManager.saveUserSetting("breath_repeat_amount", userBreathRepeatAmount);
        dataManager.saveUserSetting("use_math", userUseMath);
        dataManager.saveUserSetting("use_search_objects_color", userUseCountColor);
        dataManager.saveUserSetting("ground_photo_ex_amount", userPhotoExAmount);
        dataManager.saveUserSetting("use_faves_only", userUseFavesOnly);
    }

    private void showNoExsExceptBreathDialog(){
        try{
            NoExExceptBreathDialogFragment dialog = new NoExExceptBreathDialogFragment();
            dialog.show(getSupportFragmentManager(),"dialog_no_ex_except_breath");
        }
        catch (IllegalStateException e){
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        saveAllSettings();
    }

}