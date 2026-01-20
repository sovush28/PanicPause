package com.example.panicpause;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * GroundPhotoFragment - This fragment displays a random photo from Firestore
 * and asks the user to count specific objects in the photograph.
 * 
 * The fragment:
 * 1. Connects to Firestore database
 * 2. Retrieves a collection of images with metadata
 * 3. Randomly selects one image to display
 * 4. Shows the image with instructions to count specific objects
 * 5. Provides navigation buttons (back/next)
 * 
 * Each document in the Firestore collection should contain:
 * - img_url (string): URL of the image
 * - tags (string array): Tags describing the image
 * - word (string): Word describing what objects to count
 */
public class GroundPhotoFragment extends Fragment {

    private ImageView photoIV;
    private TextView countThingsTV;
    private Button nextBtn;
    ImageButton backBtn;

    private DataManager dataManager;
    //private List<DataManager.PhotoData> photoList;
    private DataManager.PhotoData currentPhoto;
    private DataManager.PhotoData assignedPhoto = null;

    //private Random random;

    private boolean useDefaultSettings = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ground_photo, container, false);

        InitializeViews(view);

        dataManager = new DataManager(requireContext());
        //photoList = new ArrayList<>();
        //random = new Random();

        setupButtonListeners();

        //loadLocalPhotos();

        // Отображаем назначенное фото (или сообщение об ошибке)
        displayAssignedPhoto();

        return view;
    }

    // позволяет активности задать режим
    /*public void setUseDefaultSettings(boolean useDefault) {
        this.useDefaultSettings = useDefault;
    }
    */

    private void InitializeViews(View view){
        backBtn = view.findViewById(R.id.back_btn);
        photoIV = view.findViewById(R.id.photo_iv);
        countThingsTV = view.findViewById(R.id.count_things_tv);
        nextBtn = view.findViewById(R.id.next_btn);
    }

    private void setupButtonListeners() {
        // Back button - handled by the activity
         backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get reference to the parent activity and call its method
                if (getActivity() instanceof GroundActivity) {
                    GroundActivity activity = (GroundActivity) getActivity();
                    activity.goToPreviousFragment();
                }
            }
        });
        // Next button - moves to the next fragment
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get reference to the parent activity and call its method
                if (getActivity() instanceof GroundActivity) {
                    GroundActivity activity = (GroundActivity) getActivity();
                    activity.goToNextFragment();
                }
            }
        });
    }

    private void displayAssignedPhoto() {
        if (!isAdded())
            return;

        if (assignedPhoto == null) {
            countThingsTV.setText(getString(R.string.photo_not_found));
            return;
        }

        // Обновляем текст
        String instruction = getString(R.string.ground_count_img1) +
                " " + assignedPhoto.word + " " + getString(R.string.ground_count_img2);
        countThingsTV.setText(instruction);

        // Загружаем изображение
        String filename = DataManager.getFilenameFromUrl(assignedPhoto.imgUrl);
        File photoFile = new File(requireContext().getFilesDir(), "photos/" + filename);

        if (photoFile.exists()) {
            Glide.with(this).load(photoFile).into(photoIV);
        } else {
            Glide.with(this).load(assignedPhoto.imgUrl).into(photoIV);
        }

        // Передаём фото в активность для истории
        if (getActivity() instanceof GroundActivity) {
            ((GroundActivity) getActivity()).onPhotoUsed(assignedPhoto);
        }
    }

    /**
     * Назначает фото для этого фрагмента. Вызывается из активности.
     */
    public void assignPhoto(DataManager.PhotoData photo) {
        this.assignedPhoto = photo;
    }

    /*
    private void loadLocalPhotos() {
        if (!isAdded())
            return; // защита от вызова после onDestroy

        countThingsTV.setText(getString(R.string.photo_loading));

        // Загружаем ВСЕ фото
        List<DataManager.PhotoData> allPhotos = dataManager.getLocalImagesList();
        photoList = new ArrayList<>(allPhotos);

        // пропускаем фильтрацию, если useDefaultSettings == true
        if (!useDefaultSettings) {
            // Фильтруем по триггерам
            List<String> triggers = dataManager.getTriggers();
            if (triggers != null && !triggers.isEmpty()) {
                Iterator<DataManager.PhotoData> it = photoList.iterator();
                while (it.hasNext()) {
                    DataManager.PhotoData photo = it.next();
                    for (String trigger : triggers) {
                        if (photo.tags.contains(trigger)) {
                            it.remove();
                            break;
                        }
                    }
                }
            }
        }

        if (photoList.isEmpty()) {
            if (isAdded()) {
                countThingsTV.setText(getString(R.string.photo_not_found));
            }
        } else {
            displayRandomPhoto();
        }
    }

    private void displayRandomPhoto() {
        if (photoList.isEmpty() || !isAdded())
            return;

        int index = random.nextInt(photoList.size());
        currentPhoto = photoList.get(index);

        String instruction = getString(R.string.ground_count_img1) +
                " " + currentPhoto.word + " " + getString(R.string.ground_count_img2);
        countThingsTV.setText(instruction);

        // Получаем локальный путь к фото
        String filename = DataManager.getFilenameFromUrl(currentPhoto.imgUrl);
        File photoFile = new File(requireContext().getFilesDir(), "photos/" + filename);

        if (photoFile.exists()) {
            // Загружаем локальный файл через Glide
            Glide.with(this)
                    .load(photoFile)
                    .into(photoIV);
        } else {
            // Резерв: пробуем загрузить по URL (если интернет есть)
            Glide.with(this)
                    .load(currentPhoto.imgUrl)
                    .into(photoIV);
        }

        // передача фото в активность
        if (getActivity() instanceof GroundActivity) {
            ((GroundActivity) getActivity()).onPhotoUsed(currentPhoto);
        }

    }*/

}