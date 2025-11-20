package com.example.safespace;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SelfHelpActivity extends AppCompatActivity {

    ImageButton backBtn;
    LinearLayout duringPALayoutExpanded, afterPALayoutExpanded, duringPATitleLayout, afterPATitleLayout;
    TextView duringPATitleTV, afterPATitleTV;
    ImageView duringPATriangleIV, afterPATriangleIV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_self_help);

        backBtn=findViewById(R.id.back_btn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO add smooth transition? theres the default one though
                finish();
            }
        });

        InitializeViews();

        duringPALayoutExpanded.setVisibility(GONE);
        afterPALayoutExpanded.setVisibility(GONE);

        duringPATitleLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (duringPALayoutExpanded.getVisibility()==GONE){
                    ShowDuringPAText();
                }
                else if (duringPALayoutExpanded.getVisibility()==VISIBLE){
                    HideDuringPAExpandedText();
                }
            }
        });

        afterPATitleLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(afterPALayoutExpanded.getVisibility()==GONE){
                    ShowAfterPAText();
                }
                else if(afterPALayoutExpanded.getVisibility()==VISIBLE){
                    HideAfterPAExpandedText();
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void InitializeViews(){
        duringPALayoutExpanded=findViewById(R.id.during_pa_text_expanded);
        afterPALayoutExpanded=findViewById(R.id.after_pa_text_expanded);

        duringPATitleTV=findViewById(R.id.during_pa_tv);
        afterPATitleTV=findViewById(R.id.after_pa_tv);

        duringPATriangleIV=findViewById(R.id.during_pa_triangle_iv);
        afterPATriangleIV=findViewById(R.id.after_pa_triangle_iv);

        duringPATitleLayout=findViewById(R.id.during_pa_title_lin_layout);
        afterPATitleLayout=findViewById(R.id.after_pa_title_lin_layout);
    }

    private void HideDuringPAExpandedText(){
        duringPATitleTV.setTypeface(null, Typeface.NORMAL);
        duringPATriangleIV.setRotation(0);
        //RotateTriangle(duringPATriangleIV, 0, 90);

        CollapseView(duringPALayoutExpanded);
    }

    private void HideAfterPAExpandedText(){
        afterPATitleTV.setTypeface(null, Typeface.NORMAL);
        afterPATriangleIV.setRotation(0);
        //RotateTriangle(afterPATriangleIV, 0, 90);

        CollapseView(afterPALayoutExpanded);
    }

    private void ShowDuringPAText(){
        ExpandView(duringPALayoutExpanded);
        duringPATitleTV.setTypeface(null, Typeface.BOLD);
        duringPATriangleIV.setRotation(90);
        //RotateTriangleDown(duringPATriangleIV);
    }

    private void ShowAfterPAText(){
        ExpandView(afterPALayoutExpanded);
        afterPATitleTV.setTypeface(null, Typeface.BOLD);
        afterPATriangleIV.setRotation(90);
        //RotateTriangleDown(afterPATriangleIV);
    }


/*
    //doesnt work as well!!
    private void RotateTriangle(View triangleIV, int animResID){
        Animation animation = AnimationUtils.loadAnimation(SelfHelpActivity.this, animResID);
        triangleIV.startAnimation(animation);
    }

    private void RotateTriangleDown(View triangleIV){
        triangleIV.setRotation(0);
        RotateTriangle(triangleIV, R.anim.rotate_small_triangle_down_anim);
        triangleIV.setRotation(90);
    }
*/


/*
    //doesnt work!! damn!!!
    private void RotateTriangle(View triangleIV, float fromDegrees, float toDegrees){
        RotateAnimation rotateAnim = new RotateAnimation(fromDegrees, toDegrees,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnim.setDuration(500);
        rotateAnim.setRepeatCount(0);
        triangleIV.setAnimation(rotateAnim);
        triangleIV.setRotation(toDegrees);
    }
*/

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