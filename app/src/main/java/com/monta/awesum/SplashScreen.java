package com.monta.awesum;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.monta.awesum.activity.MainActivity;
import com.monta.awesum.signin.StartActivity;
import com.monta.awesum.ultility.Ultility;

import java.util.ArrayList;
import java.util.List;

public class SplashScreen extends AppCompatActivity {


    private static final int PERMISSION_REQUEST_CODE = 10;

    private static final String[] PERMISSION = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Check permission
        if (requestPermission())
            //Validate user already log in or not
            readyCheck();
    }

    private void readyCheck() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String id = user.getUid();
            Ultility.keepSyncData(id);

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            startActivity(new Intent(this, StartActivity.class));
            finish();
        }
    }

    private boolean requestPermission() {
        List<String> needPermission = new ArrayList<>();
        for (String per : PERMISSION) {
            if (ContextCompat.checkSelfPermission(this, per) != PackageManager.PERMISSION_GRANTED)
                needPermission.add(per);
        }

        if (!needPermission.isEmpty()) {
            ActivityCompat.requestPermissions(this, needPermission.toArray(new String[0])
                    , PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean check = true;
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    check = false;
                    break;
                }
            }
            if (check)
                readyCheck();
            else
                finish();
        }
    }
}
