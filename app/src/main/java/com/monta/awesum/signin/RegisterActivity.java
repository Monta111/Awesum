package com.monta.awesum.signin;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.model.User;
import com.monta.awesum.ultility.Ultility;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText fullnameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button registerButton;
    private TextView loginTextView;
    private FirebaseAuth auth;
    private boolean doubleBack = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        usernameEditText = findViewById(R.id.username_resgister);
        fullnameEditText = findViewById(R.id.fullname_register);
        emailEditText = findViewById(R.id.email_register);
        passwordEditText = findViewById(R.id.password_register);
        registerButton = findViewById(R.id.register_button);
        loginTextView = findViewById(R.id.login_textview);

        setRegisterButtonAction();
        setRedirectToLogin();
    }

    private void setRegisterButtonAction() {
        registerButton.setOnClickListener(v -> {
            if (validateForm()) {
                createAccount(usernameEditText.getText().toString().trim(),
                        fullnameEditText.getText().toString().trim(),
                        emailEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        });
    }

    //Go to login activity if user already have an account
    private void setRedirectToLogin() {
        loginTextView.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }


    //Validate input
    private boolean validateForm() {
        boolean valid = true;

        String username = usernameEditText.getText().toString().trim();
        String fullname = fullnameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError(getString(R.string.required));
            valid = false;
        } else if (TextUtils.isEmpty(fullname)) {
            fullnameEditText.setError(getString(R.string.required));
            valid = false;
        } else if (TextUtils.isEmpty(email)) {
            emailEditText.setError(getString(R.string.required));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError(getString(R.string.invalid_email));
            valid = false;
        } else if (TextUtils.isEmpty(password)) {
            passwordEditText.setError(getString(R.string.required));
            valid = false;
        } else if (password.length() < 6) {
            passwordEditText.setError(getString(R.string.invalid_password));
            valid = false;
        }

        return valid;
    }

    //Create new account
    private void createAccount(final String username, final String fullname, final String email, final String password) {

        ProgressDialog progressDialog = new ProgressDialog(RegisterActivity.this);
        progressDialog.setMessage(getString(R.string.please_wait));
        progressDialog.show();
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        //Check exist username
        Query userRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER)
                .orderByChild("username")
                .equalTo(username);
        userRef.keepSynced(true);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    //if username haven't used by other, allow register
                    createUser(email, password, username, fullname, progressDialog);
                } else {
                    //show error
                    Toast.makeText(RegisterActivity.this, getString(R.string.username_exist), Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    //Register in server
    private void createUser(String email, String password, String username, String fullname, ProgressDialog progressDialog) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        //Write user info to database
                        String uid = auth.getCurrentUser().getUid();
                        User user = new User(uid, username, fullname, email, AwesumApp.DEFAULT_PROFILE_IMAGE, null);
                        FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER).child(uid).setValue(user);

                        //Get notification Token && preserve at local and server
                        FirebaseInstanceId.getInstance().getInstanceId()
                                .addOnCompleteListener(task1 -> {
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
                                    Intent intent = new Intent(RegisterActivity.this, UpdateProfileImageActivity.class);
                                    intent.putExtra("id", uid);
                                    progressDialog.dismiss();
                                    startActivity(intent);
                                    finish();
                                });
                    } else {
                        Toast.makeText(this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });
    }

    //Double back
    @Override
    public void onBackPressed() {
        if (doubleBack)
            finish();

        doubleBack = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(() -> doubleBack = false, 2000);
    }
}
