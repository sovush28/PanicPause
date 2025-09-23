package com.example.safespace;

import android.content.Intent;
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

import org.w3c.dom.Text;

public class SignInActivity extends AppCompatActivity implements View.OnClickListener {

    EditText emailET, newPasswET, codeET;
    TextView logInTV;
    Button getCodeBtn, signInBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);

        emailET=(EditText)findViewById(R.id.signin_email_et);
        newPasswET=(EditText)findViewById(R.id.signin_passw_et);
        codeET=(EditText)findViewById(R.id.code_et);
        logInTV=(TextView)findViewById(R.id.to_login_tv);
        getCodeBtn=(Button)findViewById(R.id.get_code_btn);
        signInBtn=(Button)findViewById(R.id.sign_in_btn);

        logInTV.setOnClickListener(this);
        getCodeBtn.setOnClickListener(this);
        signInBtn.setOnClickListener(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onClick(View v){
        if(v.getId()==R.id.to_login_tv){
            Intent intent=new Intent(this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // Плавное появление/исчезание
            finish();
        }
    }
}