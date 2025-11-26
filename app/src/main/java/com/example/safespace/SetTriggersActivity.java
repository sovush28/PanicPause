package com.example.safespace;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SetTriggersActivity extends AppCompatActivity implements TriggersRecycleViewAdapter.OnTriggerClickListener{

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    ImageButton backBtn;
    RecyclerView triggersListRV;
    TriggersRecycleViewAdapter triggersAdapter;

    List<TriggerItem> allTriggerItems=new ArrayList<>();
    Set<String> userTriggers = new HashSet<>(); // user's selected triggers

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_triggers);

        mAuth=FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        InitializeViews();

        SetupRecyclerView();

        // Load both triggers data and user's selected triggers
        LoadTriggersFromFirestore(); //triggers data
        if (user != null) {
            LoadUserTriggers(user); //user's selected triggers
        }

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
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
        triggersListRV=findViewById(R.id.triggers_recycler_view);
    }

    private void SetupRecyclerView(){
        // Use LinearLayoutManager for vertical scrolling list
        triggersListRV.setLayoutManager(new LinearLayoutManager(this));

        // Create adapter with empty list initially, will update when data loads
        triggersAdapter=new TriggersRecycleViewAdapter(allTriggerItems,this);
        triggersListRV.setAdapter(triggersAdapter);
    }

    // Load trigger hierarchy from Firestore
    private void LoadTriggersFromFirestore(){
        db = FirebaseFirestore.getInstance();
        db.collection("tags_collection")
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        allTriggerItems.clear();

                        for(QueryDocumentSnapshot document : task.getResult()){
                            // Convert Firestore document to TriggerItem object
                            try {
                                TriggerItem item = document.toObject(TriggerItem.class);
                                if (item != null) {
                                    allTriggerItems.add(item);
                                }
                            } catch (Exception e) {
                                // Log conversion error but continue with other items
                                e.printStackTrace();
                            }
                        }

                        triggersAdapter.updateItems(allTriggerItems);
                    }
                    else{
                        // Handle database error
                        Exception e = task.getException();
                        if (e != null) {
                            e.printStackTrace();
                        }
                        // TODO: Show error message to user
                    }
                });

    }

    /** Load user's selected triggers from their Firestore document */
    private void LoadUserTriggers(FirebaseUser user) {
        db.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Get the triggers array from Firestore
                            List<String> triggersList = (List<String>) document.get("triggers");
                            if (triggersList != null) {
                                userTriggers = new HashSet<>(triggersList);
                                // Update adapter with user's selected triggers
                                triggersAdapter.setUserSelectedTriggers(userTriggers);
                            }
                        } else {
                            // User document doesn't exist, create it with empty triggers
                            CreateUserDocument(user);
                        }
                    } else {
                        // Handle error
                        Exception e = task.getException();
                        if (e != null) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    /** Create user document if it doesn't exist */
    private void CreateUserDocument(FirebaseUser user) {
        db.collection("users").document(user.getUid())
                .set(new java.util.HashMap<String, Object>() {{
                    put("triggers", new ArrayList<String>());
                }})
                .addOnSuccessListener(aVoid -> {
                    // Document created successfully
                    userTriggers.clear();
                    triggersAdapter.setUserSelectedTriggers(userTriggers);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                });
    }


    // Handle category expand/collapse clicks
    @Override
    public void onCategoryClick(TriggerItem category, int position) {
        triggersAdapter.toggleCategory(position);
    }

    // Handle trigger plus/minus button clicks
    @Override
    public void onTriggerClick(TriggerItem trigger, ImageButton plusButton,boolean isCurrentlySelected) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (isCurrentlySelected) {
                // Trigger is currently selected, so remove it
                RemoveTriggerFromUser(user, trigger.getImgTag());
            } else {
                // Trigger is not selected, so add it
                AddTriggerToUser(user, trigger.getImgTag());
            }
        }
    }

    /** Add a trigger to user's triggers array in Firestore */
    private void AddTriggerToUser(FirebaseUser user, String triggerTag) {
        db.collection("users").document(user.getUid())
                .update("triggers", FieldValue.arrayUnion(triggerTag))
                .addOnSuccessListener(aVoid -> {
                    // Successfully added to Firestore, update local state
                    userTriggers.add(triggerTag);
                    triggersAdapter.addSelectedTrigger(triggerTag);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    // TODO: Show error message to user
                });
    }

    /** Remove a trigger from user's triggers array in Firestore */
    private void RemoveTriggerFromUser(FirebaseUser user, String triggerTag) {
        db.collection("users").document(user.getUid())
                .update("triggers", FieldValue.arrayRemove(triggerTag))
                .addOnSuccessListener(aVoid -> {
                    // Successfully removed from Firestore, update local state
                    userTriggers.remove(triggerTag);
                    triggersAdapter.removeSelectedTrigger(triggerTag);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    // TODO: Show error message to user
                });
    }

    // ChangeDrawablePlusOrCheckMark method is handled by adapter
    // SetChosenTriggersDrawables method is handled by LoadUserTriggers
    /*
    private void SetOrRemoveTrigger(FirebaseUser user, String triggerTag, ImageButton imgBtn) {
        // TODO: Implement Firestore update logic
        // Add or remove the triggerTag from user's triggers array in Firestore

        ChangeDrawablePlusOrCheckMark(imgBtn);
    }
    // TODO: add or delete the tag (triggerTag which will or wont be in the user's document
    // (users/[uid]/triggers (string array)) in the users collection


    // TODO: set initial checkmarks based on user's saved triggers
    private void SetChosenTriggersDrawables(FirebaseUser user){
        //TODO change plus drawables to the checkmark drawable on the tags that are in the user's "triggers" array
    }

    private void ChangeDrawablePlusOrCheckMark(ImageButton imgBtn){
        if(imgBtn.getBackground().getConstantState() == getDrawable(R.drawable.plus).getConstantState()){     //AppCompatResources.getDrawable(this, R.drawable.plus))
            imgBtn.setBackground(getDrawable(R.drawable.check_mark)); //getBackground()/getDrawable(...).getConstantState()
        }
        else if(imgBtn.getBackground().getConstantState()==getDrawable(R.drawable.check_mark).getConstantState()){
            imgBtn.setBackground(getDrawable(R.drawable.plus));
        }
    }
*/

/*
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
*/

}








/*
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
*/
