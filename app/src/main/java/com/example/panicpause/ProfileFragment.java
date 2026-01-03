package com.example.panicpause;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment{
    TextView  currentEmailTV;
    LinearLayout favoritesLayout, setTriggersLayout, accountSettingsLayout, appInfoLayout;

    private FirebaseAuth mAuth;
    private DataManager dataManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // "Надуваем" макет из XML-файла fragment_profile.xml
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        InitializeViews(view);
        SetOnClickListeners();

        dataManager=new DataManager(requireContext());
        mAuth = FirebaseAuth.getInstance();

        updateCurrentEmailTV();

        return view;  // Возвращаем готовый экран
    }

    private void inDevelopmentToast(){
        Toast.makeText(getActivity(), R.string.in_development, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume(){
        super.onResume();
        updateCurrentEmailTV();
    }

    private void updateCurrentEmailTV(){
        if(dataManager.isGuest()){
            currentEmailTV.setText(getString(R.string.guest));
        }
        else{
            FirebaseUser user = mAuth.getCurrentUser();
            if(user != null && user.getEmail() != null){
                currentEmailTV.setText(user.getEmail());
            }
            else{
                currentEmailTV.setText(getString(R.string.guest));
            }
        }
        //юид для отладки
        //currentEmailTV.setText(dataManager.getUserId());
    }

    private void InitializeViews(View view){
        currentEmailTV=view.findViewById(R.id.temp_user_email_tv);
        favoritesLayout=view.findViewById(R.id.favorites_layout);
        accountSettingsLayout=view.findViewById(R.id.go_to_acc_settings_layout);
        setTriggersLayout=view.findViewById(R.id.set_triggers_layout);
        appInfoLayout=view.findViewById(R.id.app_info_layout);
    }

    private void SetOnClickListeners(){
        favoritesLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inDevelopmentToast();
                //TODO
            }
        });

        accountSettingsLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AccountSettingsActivity.class);
                startActivity(intent);
            }
        });

        setTriggersLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getActivity(), SetTriggersActivity.class);
                startActivity(intent);
            }
        });

        appInfoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getActivity(), AppInfoActivity.class);
                startActivity(intent);
                //getActivity().finish();
            }
        });
    }

}