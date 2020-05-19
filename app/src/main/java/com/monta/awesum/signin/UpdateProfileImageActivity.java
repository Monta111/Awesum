package com.monta.awesum.signin;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.activity.MainActivity;
import com.theartofdev.edmodo.cropper.CropImage;

public class UpdateProfileImageActivity extends AppCompatActivity {

    private ImageView avatar;
    private Button nextButton;
    private String id;
    private Uri avatarUri;
    private boolean isUpdateAvatar = false;
    private DatabaseReference userRef;

    private boolean doubleBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile_image);

        id = getIntent().getStringExtra("id");

        avatar = findViewById(R.id.avatar_imageview);
        nextButton = findViewById(R.id.next_button);

        userRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER);

        loadDefaultAvatar();
        setUpdateAvatarAction();
        setNextButtonAction();
    }

    private void loadDefaultAvatar() {
        Glide.with(this).load(R.drawable.defaultavatar).circleCrop().into(avatar);
    }

    private void setUpdateAvatarAction() {
        avatar.setOnClickListener(v -> CropImage.activity().setAspectRatio(1, 1).start(UpdateProfileImageActivity.this));
    }

    private void setNextButtonAction() {
        nextButton.setOnClickListener(v -> {

            ProgressDialog progressDialog = new ProgressDialog(UpdateProfileImageActivity.this);
            progressDialog.setMessage(getString(R.string.please_wait));
            progressDialog.show();
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);

            //If user update avatar, upload it to storage and update info in database
            if (isUpdateAvatar) {

                //Upload to storage
                final StorageReference ref = FirebaseStorage.getInstance().getReference(AwesumApp.STORAGE_PROFILE_IMAGE).child(id);
                UploadTask uploadTask = ref.putFile(avatarUri);
                uploadTask.continueWithTask(task -> ref.getDownloadUrl())
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                String downloadUrl = task.getResult().toString();

                                //Update info in database
                                userRef.child(id).child("avatarUrl").setValue(downloadUrl);
                                progressDialog.dismiss();
                                Intent intent = new Intent(UpdateProfileImageActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                progressDialog.dismiss();
                                Intent intent = new Intent(UpdateProfileImageActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
            } else {
                //If not
                progressDialog.dismiss();
                Intent intent = new Intent(UpdateProfileImageActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            CropImage.ActivityResult activityResult = CropImage.getActivityResult(data);
            if (activityResult != null) {
                avatarUri = activityResult.getUri();
            }
            Glide.with(this).load(avatarUri).circleCrop().into(avatar);
            isUpdateAvatar = true;
        }
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
