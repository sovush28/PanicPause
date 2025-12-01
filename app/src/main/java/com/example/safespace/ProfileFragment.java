package com.example.safespace;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment{

    private FirebaseAuth mAuth;
    TextView currentEmailTV;

    TextView favoritesTV, setTriggersTV, accountSettingsTV, appInfoTV;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // "Надуваем" макет из XML-файла fragment_profile.xml
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        InitializeViews(view);
        SetOnClickListeners();

        if(user!=null && user.getEmail()!=null){
            currentEmailTV.setText(user.getEmail());
        }

        return view;  // Возвращаем готовый экран
    }

    private void inDevelopmentToast(){
        Toast.makeText(getActivity(), R.string.in_development, Toast.LENGTH_SHORT).show();
    }

    private void InitializeViews(View view){
        currentEmailTV=view.findViewById(R.id.temp_user_email_tv);

        favoritesTV=view.findViewById(R.id.favorites_tv);
        accountSettingsTV=view.findViewById(R.id.go_to_acc_settings_tv);
        setTriggersTV=view.findViewById(R.id.set_triggers_tv);
        appInfoTV=view.findViewById(R.id.app_info_tv);
    }

    private void SetOnClickListeners(){
        favoritesTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inDevelopmentToast();
                //TODO
            }
        });

        accountSettingsTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AccountSettingsActivity.class);
                startActivity(intent);
            }
        });

        setTriggersTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getActivity(), SetTriggersActivity.class);
                startActivity(intent);
            }
        });

        appInfoTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //inDevelopmentToast();
                Intent intent=new Intent(getActivity(), AppInfoActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });
    }

}