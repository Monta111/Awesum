package com.monta.awesum.signin;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.activity.MainActivity;
import com.monta.awesum.ultility.Ultility;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerTextView;
    private ProgressDialog progressDialog;

    private FirebaseAuth auth;
    private boolean doubleBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.email_login);
        passwordEditText = findViewById(R.id.password_login);
        loginButton = findViewById(R.id.login_button);
        registerTextView = findViewById(R.id.register_textview);


        auth = FirebaseAuth.getInstance();

        setLoginButtonAction();
        setRedirectToRegister();

    }

    private void setLoginButtonAction() {
        loginButton.setOnClickListener(v -> {
            if (validateForm()) {
                if (progressDialog == null)
                    progressDialog = new ProgressDialog(v.getContext());
                progressDialog.setMessage(getString(R.string.please_wait));
                progressDialog.show();
                progressDialog.setCancelable(false);
                progressDialog.setCanceledOnTouchOutside(false);

                String emailRaw = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString();
                if (emailRaw.contains("@")) {
                    loginWithEmail(emailRaw, password);
                } else {
                    DatabaseReference userShortRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER_SHORT)
                            .child(emailRaw);
                    userShortRef.keepSynced(true);
                    userShortRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() == null) {
                                Toast.makeText(LoginActivity.this, getString(R.string.username_not_exist), Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            } else {
                                String email = (String) dataSnapshot.getValue();
                                loginWithEmail(email, password);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }


            }
        });
    }

    //Authenticating log in info
    private void loginWithEmail(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(LoginActivity.this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseInstanceId.getInstance().getInstanceId()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.getResult() != null) {

                                        //Preserve user notification Token both local and server
                                        String token = task1.getResult().getToken();
                                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                        //Local side
                                        SharedPreferences sharedPreferences = getSharedPreferences(AwesumApp.TOKEN, Context.MODE_PRIVATE);
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putString(AwesumApp.TOKEN, token);
                                        editor.apply();

                                        //Server side
                                        FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER)
                                                .child(userId)
                                                .child("notificationToken")
                                                .child(token)
                                                .setValue(System.currentTimeMillis());

                                        //Make needed data always fresh
                                        String id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                        Ultility.keepSyncData(id);

                                        //Go to Main Activity
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        progressDialog.dismiss();
                                        finish();
                                    }
                                });

                    } else {
                        Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });
    }

    //Go go register activity if user dont have account
    private void setRedirectToRegister() {
        registerTextView.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    //Validate input
    private boolean validateForm() {
        boolean valid = true;
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError(getString(R.string.required));
            valid = false;
        } else if (TextUtils.isEmpty(password)) {
            passwordEditText.setError(getString(R.string.required));
            valid = false;
        }
        return valid;
    }

    //Double back
    @Override
    public void onBackPressed() {
        if (doubleBack)
            finish();

        doubleBack = true;
        Toast.makeText(this, getString(R.string.click_back), Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> doubleBack = false, 2000);
    }
}
