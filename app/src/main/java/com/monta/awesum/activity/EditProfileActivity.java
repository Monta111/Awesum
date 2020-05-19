package com.monta.awesum.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.model.User;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.vanniktech.emoji.EmojiPopup;


public class EditProfileActivity extends AppCompatActivity {

    private static final int PERMISSION_CAMERA_CODE = 10;

    private ImageView closeEdit;
    private TextView doneEdit;
    private ImageView profileImageEdit;
    private EditText fullnameEdit;
    private EditText usernameEdit;
    private EditText bioEdit;
    private EditText emailEdit;
    private ImageView smile;

    private String userId;
    private String oldUsername;
    private DatabaseReference userRef;
    private DatabaseReference userShortRef;
    private Uri profileImageUri;
    private boolean isEditProfileImage;
    private EmojiPopup emojiPopup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        userId = getIntent().getStringExtra("userId");

        closeEdit = findViewById(R.id.close_edit);
        doneEdit = findViewById(R.id.done_edit);
        profileImageEdit = findViewById(R.id.profile_image_edit);
        fullnameEdit = findViewById(R.id.fullname_edit);
        usernameEdit = findViewById(R.id.username_edit);
        bioEdit = findViewById(R.id.bio_edit);
        emailEdit = findViewById(R.id.email_edit);
        smile = findViewById(R.id.smile);

        isEditProfileImage = false;
        emojiPopup = EmojiPopup.Builder.fromRootView(findViewById(android.R.id.content)).build(bioEdit);

        userRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER).child(userId);
        userShortRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER_SHORT);

        setCurrentInfo();
        setCloseEditAction();
        setOnClickSmile();
        setDoneEditAction();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA_CODE);
        } else {
            setEditProfileImageAction();
        }
    }

    private void setCurrentInfo() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    Glide.with(EditProfileActivity.this).load(user.getAvatarUrl()).into(profileImageEdit);
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    int width = displayMetrics.widthPixels;
                    profileImageEdit.setLayoutParams(new LinearLayout.LayoutParams(width, width));
                    fullnameEdit.setText(user.getFullname());
                    oldUsername = user.getUsername();
                    usernameEdit.setText(oldUsername);
                    bioEdit.setText(user.getBio());
                    emailEdit.setText(user.getEmail());
                    emailEdit.setEnabled(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setCloseEditAction() {
        closeEdit.setOnClickListener(v -> finish());
    }

    private void setDoneEditAction() {
        doneEdit.setOnClickListener(v -> {
            if (TextUtils.isEmpty(usernameEdit.getText()))
                usernameEdit.setError(getString(R.string.required));
            else if (TextUtils.isEmpty(fullnameEdit.getText()))
                fullnameEdit.setError(getString(R.string.required));
            else {
                ProgressDialog progressDialog = new ProgressDialog(EditProfileActivity.this);
                progressDialog.setMessage(getString(R.string.please_wait));
                progressDialog.show();
                progressDialog.setCancelable(false);
                progressDialog.setCanceledOnTouchOutside(false);

                String username = usernameEdit.getText().toString().trim();
                if (!username.equals(oldUsername)) {
                    userShortRef.child(username).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() == null) {
                                userRef.child("username").setValue(username);
                                userShortRef.child(oldUsername).removeValue();
                                userShortRef.child(username).setValue(emailEdit.getText().toString());
                                userRef.child("fullname").setValue(fullnameEdit.getText().toString());
                                userRef.child("bio").setValue(bioEdit.getText().toString());
                                if (isEditProfileImage) {
                                    StorageReference ref = FirebaseStorage.getInstance().getReference(AwesumApp.STORAGE_PROFILE_IMAGE).child(userId);
                                    ref.putFile(profileImageUri)
                                            .continueWithTask(task -> ref.getDownloadUrl())
                                            .addOnCompleteListener(task1 -> {
                                                if (task1.isSuccessful() && task1.getResult() != null) {
                                                    String downloadUrl = task1.getResult().toString();
                                                    userRef.child("avatarUrl").setValue(downloadUrl);
                                                    progressDialog.dismiss();
                                                    finish();
                                                }
                                            });
                                } else {
                                    progressDialog.dismiss();
                                    finish();
                                }
                            } else {
                                Toast.makeText(EditProfileActivity.this, getString(R.string.username_exist), Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                } else {
                    userRef.child("fullname").setValue(fullnameEdit.getText().toString());
                    userRef.child("bio").setValue(bioEdit.getText().toString());
                    if (isEditProfileImage) {
                        StorageReference ref = FirebaseStorage.getInstance().getReference(AwesumApp.STORAGE_PROFILE_IMAGE).child(userId);
                        ref.putFile(profileImageUri)
                                .continueWithTask(task -> ref.getDownloadUrl())
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful() && task1.getResult() != null) {
                                        String downloadUrl = task1.getResult().toString();
                                        userRef.child("avatarUrl").setValue(downloadUrl);
                                        progressDialog.dismiss();
                                        finish();
                                    }
                                });
                    } else {
                        progressDialog.dismiss();
                        finish();
                    }
                }

            }
        });
    }

    private void setEditProfileImageAction() {
        profileImageEdit.setOnClickListener(v -> CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .start(EditProfileActivity.this));
    }


    private void setOnClickSmile() {
        smile.setOnClickListener(v -> {
            if (emojiPopup.isShowing()) {
                emojiPopup.toggle();
                smile.setImageDrawable(getDrawable(R.drawable.ic_smile));
            } else {
                emojiPopup.toggle();
                smile.setImageDrawable(getDrawable(R.drawable.ic_keyboard));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            CropImage.ActivityResult activityResult = CropImage.getActivityResult(data);
            if (activityResult != null) {
                profileImageUri = activityResult.getUri();
                Glide.with(EditProfileActivity.this).load(profileImageUri).into(profileImageEdit);
                isEditProfileImage = true;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CAMERA_CODE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED)
            setEditProfileImageAction();
        else
            finish();
    }
}
