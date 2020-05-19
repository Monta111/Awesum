package com.monta.awesum.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.signin.StartActivity;

public class SettingActivity extends AppCompatActivity {

    private Switch aSwitch;
    private ImageView signOut;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        Toolbar toolbar = findViewById(R.id.setting_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        aSwitch = findViewById(R.id.a_switch);
        signOut = findViewById(R.id.sign_out);

        setSwitchAction();
        setSignOutAction();
    }

    private void setSwitchAction() {
        aSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                Toast.makeText(SettingActivity.this, "Checked", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(SettingActivity.this, "Unchecked", Toast.LENGTH_SHORT).show();
        });
    }

    private void setSignOutAction() {
        signOut.setOnClickListener(v -> {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.please_wait));
            progressDialog.show();
            SharedPreferences sharedPreferences = getSharedPreferences(AwesumApp.TOKEN, Context.MODE_PRIVATE);
            String token = sharedPreferences.getString(AwesumApp.TOKEN, null);
            if (token != null)
                FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER)
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("notificationToken")
                        .child(token).removeValue().addOnCompleteListener(task -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.clear();
                    editor.apply();
                    FirebaseAuth.getInstance().signOut();
                    progressDialog.dismiss();
                    Intent intent = new Intent(SettingActivity.this, StartActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    System.gc();
                    finish();
                });
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
