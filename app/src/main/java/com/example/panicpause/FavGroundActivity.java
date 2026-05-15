package com.example.panicpause;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FavGroundActivity extends AppCompatActivity {

    ImageButton backBtn, exitBtn;
    Button nextBtn;
    com.google.android.material.imageview.ShapeableImageView photoIV;
    TextView instructionTB;

    private DataManager dataManager;
    private List<String> userFaves = new ArrayList<>();
    private List<DataManager.PhotoData> userFavesPhotoData = new ArrayList<>();
    private int currentExIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fav_ground);

        initializeViews();
        setOnClickListeners();

        dataManager=new DataManager(this);

        prepareExSequence();

        displayExercise();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews(){
        backBtn=findViewById(R.id.back_btn);
        exitBtn=findViewById(R.id.exit_btn);
        nextBtn=findViewById(R.id.next_btn);
        photoIV=findViewById(R.id.photo_iv);
        instructionTB=findViewById(R.id.count_things_tv);
    }

    private void setOnClickListeners(){
        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToPrevEx();
            }
        });
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToNextEx();
            }
        });
    }

    private void goToPrevEx(){
        if(currentExIndex!=0){
            currentExIndex--;
            displayExercise();
        }
    }

    private void goToNextEx(){
        if(!isLastExercise()){
            currentExIndex++;
            displayExercise();
        }
        else{
            finish();
        }
    }

    private void prepareExSequence(){
        userFaves = dataManager.getFaves();

        for (DataManager.PhotoData photo : dataManager.getLocalImagesList()){
            if(userFaves.contains(photo.imgUrl)){
                userFavesPhotoData.add(photo);
            }
        }

        if(!userFaves.isEmpty() && !userFavesPhotoData.isEmpty()){
            if(userFavesPhotoData.size()>1){
                Collections.shuffle(userFavesPhotoData);
            }
        }
    }

    private void displayExercise(){
        displayPhoto();
        updateBtns();
    }

    private void displayPhoto(){
        try{
            DataManager.PhotoData currentPhoto = userFavesPhotoData.get(currentExIndex);

            String filename = DataManager.getFilenameFromUrl(currentPhoto.imgUrl);
            File photoFile = new File(this.getFilesDir(), "photos/" + filename);

            if (photoFile.exists()) {
                Glide.with(this).load(photoFile)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(photoIV);
            } else {
                Glide.with(this).load(currentPhoto.imgUrl)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(photoIV);
            }

            String instruction = getString(R.string.ground_count_img1) +
                    " " + currentPhoto.word + " " + getString(R.string.ground_count_img2);
            instructionTB.setText(instruction);
        }
        catch(Exception ex){
            Log.e("FavGroundActivity", "Не найдено упражнение с индексом " + currentExIndex);
        }
    }

    private void updateBtns(){
        // если упражнение первое, то спрятать кнопку назад
        if(currentExIndex==0){
            backBtn.setVisibility(View.GONE);
        }
        else{
            backBtn.setVisibility(View.VISIBLE);
        }
        // если упражнение последнее, то кнопка "дальше" -> "завершить"
        if(isLastExercise()){
            nextBtn.setText(R.string.finish);
        }
        else{
            nextBtn.setText(R.string.next);
        }
    }

    private boolean isLastExercise(){
        return currentExIndex == userFavesPhotoData.size() - 1;
    }
}