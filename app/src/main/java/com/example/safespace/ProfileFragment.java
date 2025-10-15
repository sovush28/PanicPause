package com.example.safespace;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private Button triggersButton;
    private FirebaseAuth mAuth;
    TextView currentEmailTV;

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
        else{
            currentEmailTV.setText("Email не доступен");
        }


        // Находим кнопку на экране
        //triggersButton = view.findViewById(R.id.triggers_button);

        // Настраиваем обработчик нажатия на кнопку
        /*triggersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Здесь будет код для перехода к управлению триггерами
                // Пока просто оставляем пустым
            }
        });*/

        return view;  // Возвращаем готовый экран
    }
}