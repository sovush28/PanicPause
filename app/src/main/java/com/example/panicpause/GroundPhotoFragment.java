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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.io.File;

/**
 * GroundPhotoFragment - фрагмент отображает упражнение с фото.
 * Для каждого фото в БД и в соотв. файле json должны храниться:
 * - img_url (string): URL изображения
 * - tags (string array): теги изображения
 * - word (string): слово, вставляемое в текст упражнения (название подсчитываемых предметов)
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

        initializeViews(view);
        setupButtonListeners();

        displayAssignedPhoto();

        return view;
    }

    private void initializeViews(View view){
        backBtn = view.findViewById(R.id.back_btn);
        photoIV = view.findViewById(R.id.photo_iv);
        countThingsTV = view.findViewById(R.id.count_things_tv);
        nextBtn = view.findViewById(R.id.next_btn);
    }

    private void setupButtonListeners() {
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof GroundActivity) {
                    GroundActivity activity = (GroundActivity) getActivity();
                    activity.goToPreviousFragment();
                }
            }
        });
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof GroundActivity) {
                    GroundActivity activity = (GroundActivity) getActivity();
                    activity.goToNextFragment();
                }
            }
        });
    }

    /**
     * Метод displayAssignedPhoto отображает назначенное активностью фото
     */
    private void displayAssignedPhoto() {
        if (!isAdded())
            return;
        if (assignedPhoto == null) {
            countThingsTV.setText(getString(R.string.photo_not_found));
            return;
        }

        // загрузить изображение
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

        String instruction = getString(R.string.ground_count_img1) +
                " " + assignedPhoto.word + " " + getString(R.string.ground_count_img2);
        countThingsTV.setText(instruction);

        // передать фото в активность для составления истории
        if (getActivity() instanceof GroundActivity) {
            ((GroundActivity) getActivity()).onPhotoUsed(assignedPhoto);
        }
    }

    /**
     * Метод assignPhoto назначает фото для этого фрагмента. Вызывается из активности.
     * @param photo назначаемое фото
     */
    public void assignPhoto(DataManager.PhotoData photo) {
        this.assignedPhoto = photo;
    }

}