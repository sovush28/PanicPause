package com.example.safespace;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// Фрагмент - "мини-активность", которая может быть частью экрана
public class HomeFragment extends Fragment {

    private FirebaseAuth mAuth;
    Button panicBtn;

    TextView whatsPATV, howHelpYourselfTV, whatsTriggerTV;

    // метод создает внешний вид фрагмента
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // "Надуваем" макет из XML-файла fragment_home.xml
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        panicBtn = view.findViewById(R.id.panic_btn);
        panicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: переход к упражнениям "Заземление" (GroundActivity.GroundBreathFragment)
                Intent intent = new Intent(getActivity(), GroundActivity.class);
                startActivity(intent);
            }
        });

        whatsPATV=view.findViewById(R.id.whats_pa_tv);
        whatsPATV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWhatsPADialog();
            }
        });

        howHelpYourselfTV=view.findViewById(R.id.how_to_help_yourself_tv);
        howHelpYourselfTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToSelfHelpActivity();
            }
        });

        whatsTriggerTV=view.findViewById(R.id.whats_trigger_tv);
        whatsTriggerTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWhatsTriggerDialog();
            }
        });

        return view;  // Возвращаем готовый экран
    }

    /////////////////////////// конец oncreate

    private void showWhatsPADialog(){
        try{
            WhatsPADialogFragment dialog=new WhatsPADialogFragment();
            dialog.show(getActivity().getSupportFragmentManager(), "whats_pa_dialog");
        }
        catch(IllegalStateException ex){
            // Обработка случая, когда Activity уничтожается
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }

    private void showWhatsTriggerDialog(){
        try{
            WhatsTriggerDialogFragment dialog=new WhatsTriggerDialogFragment();
            dialog.show(getActivity().getSupportFragmentManager(), "whats_trigger_dialog");
        }
        catch(IllegalStateException ex){
            // Обработка случая, когда Activity уничтожается
            Log.e("Dialog", "Cannot show dialog - activity state invalid");
        }
    }

    private void goToSelfHelpActivity(){

    }

    ////////
}