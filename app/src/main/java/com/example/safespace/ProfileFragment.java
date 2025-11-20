package com.example.safespace;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment{

    private Button triggersButton;
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

        currentEmailTV=view.findViewById(R.id.temp_user_email_tv);

        if(user!=null && user.getEmail()!=null){
            currentEmailTV.setText(user.getEmail());
        }

        accountSettingsTV=view.findViewById(R.id.acc_settings_tv);
        accountSettingsTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AccountSettingsActivity.class);
                startActivity(intent);
                //TODO плавный переход
            }
        });

        setTriggersTV=view.findViewById(R.id.set_triggers_tv);
        setTriggersTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getActivity(), SetTriggersActivity.class);
                startActivity(intent);
            }
        });

        return view;  // Возвращаем готовый экран
    }
}