package com.example.safespace;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    EditText emailET, passwET;
    TextView forgotPasswTV, createAccTV;
    Button logInBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        emailET=(EditText)findViewById(R.id.login_email_et);
        passwET=(EditText)findViewById(R.id.login_passw_et);
        forgotPasswTV=(TextView)findViewById(R.id.forgot_passw_tv);
        createAccTV=(TextView)findViewById(R.id.create_acc_tv);
        logInBtn=(Button)findViewById(R.id.log_in_btn);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onClick(View v){

    }
}