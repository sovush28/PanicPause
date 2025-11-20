package com.example.safespace;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SetTriggersActivity extends AppCompatActivity {

    FirebaseAuth mAuth;

    ImageButton backBtn;
    LinearLayout peopleTitleLayout, peopleLayoutExpanded,
            animalsTitleLayout, animalsLayoutExpanded,
            animalsBirdsTitleLayout, animalsBirdsLayoutExpanded,
            foodTitleLayout, foodLayoutExpanded,
            foodBerriesTitleLayout, foodBerriesLayoutExpanded,
            foodVegsTitleLayout, foodVegsLayoutExpanded;

    TextView peopleTV, animalsTV, birdsTV, foodTV, berriesTV, vegsTV;

    ImageView peopleTriangleIV, animalsTriangleIV, animalsBirdsTriangleIV,
            foodTriangleIV, foodBerriesTriangleIV, foodVegsTriangleIV;

    ImageButton womenIB, menIB, childrenIB, catsIB, dogsIB, bunniesIB; //TODO .....................

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_triggers);

        InitializeViews();

        mAuth=FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();


        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        peopleTitleLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowOrHideLayoutExpanded(peopleLayoutExpanded, peopleTV, peopleTriangleIV);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void InitializeViews(){
        backBtn=findViewById(R.id.back_btn);

        //lin layouts
        peopleTitleLayout=findViewById(R.id.tr_people_layout_title);
        peopleLayoutExpanded=findViewById(R.id.tr_people_layout_expanded);

        animalsTitleLayout=findViewById(R.id.tr_animals_layout_title);
        animalsLayoutExpanded=findViewById(R.id.tr_animals_layout_expanded);

        animalsBirdsTitleLayout=findViewById(R.id.tr_animals_birds_layout_title);
        animalsBirdsLayoutExpanded=findViewById(R.id.tr_animals_birds_layout_expanded);

        foodTitleLayout=findViewById(R.id.tr_food_layout_title);
        foodLayoutExpanded=findViewById(R.id.tr_food_layout_expanded);

        foodBerriesTitleLayout =findViewById(R.id.tr_food_berries_layout_title);
        foodBerriesLayoutExpanded=findViewById(R.id.tr_food_berries_layout_expanded);

        foodVegsTitleLayout=findViewById(R.id.tr_food_vegetables_layout_title);
        foodVegsLayoutExpanded=findViewById(R.id.tr_food_vegetables_layout_expanded);

        //title TextViews
        peopleTV=findViewById(R.id.tr_people_tv);
        animalsTV=findViewById(R.id.tr_animals_tv);
        birdsTV=findViewById(R.id.tr_animals_birds_tv);
        foodTV=findViewById(R.id.tr_food_tv);
        berriesTV=findViewById(R.id.tr_food_berries_tv);
        vegsTV=findViewById(R.id.tr_food_vegetables_tv);

        //triangle ImageViews
        peopleTriangleIV=findViewById(R.id.tr_people_triangle_iv);
        animalsTriangleIV=findViewById(R.id.tr_animals_triangle_iv);
        animalsBirdsTriangleIV=findViewById(R.id.tr_animals_birds_triangle_iv);
        foodTriangleIV=findViewById(R.id.tr_food_triangle_iv);
        foodBerriesTriangleIV=findViewById(R.id.tr_food_berries_triangle_iv);
        foodVegsTriangleIV=findViewById(R.id.tr_food_vegetables_triangle_iv);

        //plus ImageViews



        //non-title TextViews



    }

    private void SetOrRemoveTrigger(FirebaseUser user, View imgView, String triggerTag){
        //TODO: add or delete the tag (triggerTag which will or wont be in the user's document (users/[uid]/triggers (string array))
        // in the users collection
        ChangeDrawablePlusOrCheckMark(imgView);
    }

    private void SetChosenTriggersDrawables(FirebaseUser user){
        //TODO change plus drawables to the checkmark drawable on the tags that are in the user's "triggers" array
    }

    private void ChangeDrawablePlusOrCheckMark(View imgView){
        if(imgView.getBackground()== getDrawable(R.drawable.plus)){     //AppCompatResources.getDrawable(this, R.drawable.plus))
            imgView.setBackground(getDrawable(R.drawable.check_mark));
        }
        else if(imgView.getBackground()==getDrawable(R.drawable.check_mark)){
            imgView.setBackground(getDrawable(R.drawable.plus));
        }
    }

    private void ShowOrHideLayoutExpanded(View layoutExpanded, TextView titleTV, View triangleIV){
        if (layoutExpanded.getVisibility()==View.GONE){
            ShowLayoutExpanded(layoutExpanded, titleTV, triangleIV);
        }
        else {
            HideLayoutExpanded(layoutExpanded, titleTV, triangleIV);
        }
    }

    private void ShowLayoutExpanded(View layoutExpanded, TextView titleTV, View triangleIV){
        ExpandView(layoutExpanded);
        titleTV.setTypeface(null, Typeface.BOLD);
        triangleIV.setRotation(90);
    }

    private void HideLayoutExpanded(View layoutExpanded, TextView titleTV, View triangleIV){
        titleTV.setTypeface(null, Typeface.NORMAL);
        triangleIV.setRotation(0);
        CollapseView(layoutExpanded);
    }


    private void ExpandView(View v){
        int matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec(((View) v.getParent()).getWidth(), View.MeasureSpec.EXACTLY);
        int wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        v.measure(matchParentMeasureSpec, wrapContentMeasureSpec);
        final int targetHeight = v.getMeasuredHeight();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation(){
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? LinearLayout.LayoutParams.WRAP_CONTENT
                        : (int)(targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        // Expansion speed of 1dp/ms
        a.setDuration((int)(targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    private void CollapseView(View v){
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if(interpolatedTime == 1){
                    v.setVisibility(View.GONE);
                }else{
                    v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // Collapse speed of 1dp/ms
        a.setDuration((int)(initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);

        //you can obtain a smoother animation by changing the duration (and hence the speed) of the animation
    }

}