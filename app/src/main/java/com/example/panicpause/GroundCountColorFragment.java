package com.example.panicpause;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.Random;

public class GroundCountColorFragment extends Fragment {
    private TextView colorText;
    private Button anotherColorBtn, nextBtn;
    ImageButton backBtn;

    private String[] colorNames;
    private String[] colorStringIds;
    private String currentColor;

    private Random random;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ground_count_color, container, false);

        initializeViews(view);
        initializeColorArrays();
        random = new Random();
        setupButtonListeners();

        generateNewRandomColor();

        return view;
    }

    private void initializeViews(View view) {
        backBtn = view.findViewById(R.id.back_btn);
        colorText = view.findViewById(R.id.count_color_tv);
        anotherColorBtn = view.findViewById(R.id.another_color_btn);
        nextBtn = view.findViewById(R.id.next_btn);
    }

    private void initializeColorArrays() {
        colorStringIds = new String[]{
            "ground_color_green",
            "ground_color_red", 
            "ground_color_yellow",
            "ground_color_blue",
            "ground_color_light_blue",
            "ground_color_white",
            "ground_color_black",
            "ground_color_gray",
            "ground_color_brown",
            "ground_color_orange",
            "ground_color_pink",
            "ground_color_purple"
        };

        colorNames = new String[colorStringIds.length];

        for (int i = 0; i < colorStringIds.length; i++) {
            int resourceId = getResources().getIdentifier(colorStringIds[i], "string", getContext().getPackageName());
            if (resourceId != 0) {
                colorNames[i] = getString(resourceId);
            } else {
                colorNames[i] = "цвета";
            }
        }
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
        anotherColorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateNewRandomColor();
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

    private void generateNewRandomColor() {
        if (colorNames.length == 0) {
            colorText.setText("цвета");
            currentColor = "цвета";
            return;
        }

        int randomIndex = random.nextInt(colorNames.length);
        currentColor = colorNames[randomIndex];

        colorText.setText(getString(R.string.ground_count_color1) +
        " " + currentColor + " "+ getString(R.string.ground_count_color2_bold)+
        " "+ getString(R.string.ground_count_color3));
    }

}