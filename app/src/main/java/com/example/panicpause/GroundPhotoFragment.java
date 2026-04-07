package com.example.panicpause;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;

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
    private DataManager.PhotoData assignedPhoto = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ground_photo, container, false);

        InitializeViews(view);

        setupButtonListeners();

        // Отображаем назначенное фото (или сообщение об ошибке)
        displayAssignedPhoto();

        return view;
    }

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

        // Загружаем изображение
        String filename = DataManager.getFilenameFromUrl(assignedPhoto.imgUrl);
        File photoFile = new File(requireContext().getFilesDir(), "photos/" + filename);

        if (photoFile.exists()) {
            Glide.with(this).load(photoFile)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(photoIV);
        } else {
            Glide.with(this).load(assignedPhoto.imgUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(photoIV);
        }

        // Ставим текст
        String instruction = getString(R.string.ground_count_img1) +
                " " + assignedPhoto.word + " " + getString(R.string.ground_count_img2);
        countThingsTV.setText(instruction);

        // Передаём фото в активность для истории
        if (getActivity() instanceof GroundActivity) {
            ((GroundActivity) getActivity()).onPhotoUsed(assignedPhoto);
        }
    }

    // Назначает фото для этого фрагмента. Вызывается из активности.
    public void assignPhoto(DataManager.PhotoData photo) {
        this.assignedPhoto = photo;
    }

}