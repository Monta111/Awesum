package com.monta.awesum.signin;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.monta.awesum.R;

//Splash Screen
public class StartActivity extends AppCompatActivity {

    private Button loginButton;
    private Button registerButton;
    private boolean doubleBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        loginButton = findViewById(R.id.login_button);
        registerButton = findViewById(R.id.register_button);

        loginButton.setOnClickListener(v -> {
            startActivity(new Intent(StartActivity.this, LoginActivity.class));
            finish();
        });

        registerButton.setOnClickListener(v -> {
            startActivity(new Intent(StartActivity.this, RegisterActivity.class));
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        if (doubleBack)
            finish();

        doubleBack = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(() -> doubleBack = false, 2000);
    }
}
