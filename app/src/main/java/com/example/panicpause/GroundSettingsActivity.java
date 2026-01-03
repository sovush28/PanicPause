package com.example.panicpause;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
    LinearLayout photoExAmountLayout, useMathLayout, useColorLayout, useFavesOnlyLayout, groundOnLaunchLayout;
    Switch useMathSwitch, useColorSwitch, useFavesOnlySwitch, groundOnLaunchSwitch;

    private DataManager dataManager;

    private long userBreathRepeatAmount, userPhotoExAmount;
    private boolean userUseMath, userUseCountColor, userUseFavesOnly, userGroundOnLaunch;

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
        userGroundOnLaunch=dataManager.getGroundOnLaunch();
    }

    private void updateUI(){
        breathQET.setText(String.valueOf(userBreathRepeatAmount));
        useMathSwitch.setChecked(userUseMath);
        useColorSwitch.setChecked(userUseCountColor);
        photoQET.setText(String.valueOf(userPhotoExAmount));
        useFavesOnlySwitch.setChecked(userUseFavesOnly);
        groundOnLaunchSwitch.setChecked(userGroundOnLaunch);

        if(!userUseMath && !userUseCountColor){
            photoExAmountLayout.setAlpha(1);
            photoQIncreaseIV.setEnabled(true);
            photoQDecreaseIV.setEnabled(true);
            photoQET.setEnabled(true);
        }
        else{
            photoExAmountLayout.setAlpha((float)0.5);
            photoQIncreaseIV.setEnabled(false);
            photoQDecreaseIV.setEnabled(false);
            photoQET.setEnabled(false);
        }
    }

    private void inDevelopmentToast(){
        Toast.makeText(this, R.string.in_development, Toast.LENGTH_SHORT).show();
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
        groundOnLaunchSwitch=findViewById(R.id.ground_on_launch_toggle_sc);
        photoExAmountLayout=findViewById(R.id.photo_ex_amount_layout);

        useMathLayout=findViewById(R.id.use_math_toggle_layout);
        useColorLayout=findViewById(R.id.use_color_toggle_layout);
        useFavesOnlyLayout=findViewById(R.id.use_faves_only_toggle_layout);
        groundOnLaunchLayout=findViewById(R.id.ground_on_launch_toggle_layout);
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
                allSettingsToDefault();
                saveAllSettings();
                updateUI();
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
            }
        });

        setupEditTextWatcher(breathQET, "breath_repeat_amount", value -> {
            userBreathRepeatAmount = value;
            dataManager.saveUserSetting("breath_repeat_amount", (long) value);
        });

        setupEditTextWatcher(photoQET, "ground_photo_ex_amount", value -> {
            userPhotoExAmount = value;
            dataManager.saveUserSetting("ground_photo_ex_amount", (long) value);
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
        groundOnLaunchLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                groundOnLaunchSwitch.setChecked(!groundOnLaunchSwitch.isChecked());
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
                inDevelopmentToast();
            }
        });
        groundOnLaunchSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                userGroundOnLaunch = isChecked;
                dataManager.saveUserSetting("ground_on_launch", isChecked);
                inDevelopmentToast();
            }
        });
    }

    private void setupEditTextWatcher(EditText editText, String settingName, java.util.function.LongConsumer onValueChanged) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false; // Флаг для предотвращения рекурсии

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return; // Защита от рекурсии

                String input = s.toString();

                // Случай 1: пользователь очистил поле — разрешаем, ничего не делаем
                if (input.isEmpty()) {
                    return;
                }

                // Случай 2: больше одного символа — оставляем ТОЛЬКО последнюю цифру
                if (input.length() > 1) {
                    char lastChar = input.charAt(input.length() - 1);
                    if (Character.isDigit(lastChar)) {
                        isUpdating = true;
                        editText.setText(String.valueOf(lastChar));
                        editText.setSelection(1);
                        isUpdating = false;
                        onValueChanged.accept(Character.getNumericValue(lastChar));
                    } else {
                        // На всякий случай — если вдруг не цифра
                        isUpdating = true;
                        editText.setText("0");
                        editText.setSelection(1);
                        isUpdating = false;
                        onValueChanged.accept(0);
                    }
                    return;
                }

                // Случай 3: один символ
                char c = input.charAt(0);
                if (Character.isDigit(c)) {
                    int value = Character.getNumericValue(c);
                    onValueChanged.accept(value);
                } else {
                    // Очень маловероятно из-за inputType="number", но на всякий
                    isUpdating = true;
                    editText.setText("0");
                    editText.setSelection(1);
                    isUpdating = false;
                    onValueChanged.accept(0);
                }
            }
        });

        // При потере фокуса: если поле пустое — ставим "0"
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String text = editText.getText().toString();
                if (text.isEmpty()) {
                    editText.setText("0");
                    onValueChanged.accept(0);
                }
            }
        });
    }

    private void allSettingsToDefault(){
        userBreathRepeatAmount=1;
        userUseMath=true;
        userUseCountColor=true;
        userPhotoExAmount=2;
        userUseFavesOnly=false;
        userGroundOnLaunch=false;
    }

    private void saveAllSettings(){
        dataManager.saveUserSetting("breath_repeat_amount", userBreathRepeatAmount);
        dataManager.saveUserSetting("use_math", userUseMath);
        dataManager.saveUserSetting("use_search_objects_color", userUseCountColor);
        dataManager.saveUserSetting("ground_photo_ex_amount", userPhotoExAmount);
        dataManager.saveUserSetting("use_faves_only", userUseFavesOnly);
        dataManager.saveUserSetting("ground_on_launch", userGroundOnLaunch);
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
        //finish();
    }

    /*private void toggleSwitchSetting(Switch switchV, boolean boolSetting, String settingDBName){
        if(switchV.isChecked()){
            boolSetting = true;
        }
        else{
            boolSetting = false;
        }
        dataManager.saveUserSetting(settingDBName, boolSetting);
    }

    private void editTextListener(EditText editText, long longSetting, String settingDBName){
        if(editText.getText().length()<1){
            editText.setText(String.valueOf(0));
        }
        else if (editText.getText().length()>1){
            editText.setText(String.valueOf(9));
        }
        longSetting = Long.parseLong(String.valueOf(editText.getText()));

        dataManager.saveUserSetting(settingDBName, longSetting);
    }

    private void increaseDecrease(long longSetting, boolean increase, EditText editText, String settingDBName){
        long number = Long.parseLong(String.valueOf(editText.getText()));
        if(increase && number<=8 && number>=0)
            number++;
        else if(!increase && number<=9 && number>=1)
            number--;

        editText.setText(String.valueOf(number));

        longSetting=number;
        dataManager.saveUserSetting(settingDBName, longSetting);
    }

    private void increaseDecreaseBreathQ(boolean increase){
        increaseDecrease(userBreathRepeatAmount, increase, breathQET, "breath_repeat_amount");
    }

    private void increaseDecreasePhotoExQ(boolean increase){
        increaseDecrease(userPhotoExAmount, increase, photoQET, "ground_photo_ex_amount");
    }*/
}