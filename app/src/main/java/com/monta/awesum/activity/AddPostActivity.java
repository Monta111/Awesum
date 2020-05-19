package com.monta.awesum.activity;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.adapter.GridMediaCropAdapter;
import com.monta.awesum.model.Post;
import com.monta.awesum.model.Story;
import com.monta.awesum.ultility.Ultility;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.vanniktech.emoji.EmojiPopup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AddPostActivity extends AppCompatActivity implements GridMediaCropAdapter.ImageClickCropListener {

    private ImageView close;
    private TextView done;
    private EditText description;
    private EmojiPopup emojiPopup;
    private ImageView smile;
    private RecyclerView allImageRecyclerView;
    private GridMediaCropAdapter adapter;
    private CropImageView cropImageView;
    private PlayerView video;
    private ProgressDialog progressDialog;

    private String userId;

    private Uri[] input;
    private Uri[] output;

    private StorageReference storageRef;
    private DatabaseReference postRef;
    private DatabaseReference postMainRef;
    private DatabaseReference userRef;
    private DatabaseReference storyMainRef;
    private DatabaseReference storyRef;

    private int itemType;
    private List<Uri> selectedList;
    private Uri videoUri;
    private Uri thumb;
    private SimpleExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        close = findViewById(R.id.close);
        done = findViewById(R.id.done);
        description = findViewById(R.id.description);
        smile = findViewById(R.id.smile);
        allImageRecyclerView = findViewById(R.id.all_image_recyc);
        cropImageView = findViewById(R.id.image_crop);
        video = findViewById(R.id.video);

        selectedList = new ArrayList<>();

        itemType = getIntent().getIntExtra("itemType", 0);
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        ArrayList<String> selectedListString = getIntent().getStringArrayListExtra("selectedList");
        for (String uriString : selectedListString)
            selectedList.add(Uri.parse(uriString));

        if (itemType == Post.VIDEO_TYPE_ITEM)
            thumb = Uri.parse(getIntent().getStringExtra("thumb"));

        if (selectedList != null && itemType != 0) {
            switch (itemType) {
                case Post.IMAGE_TYPE_ITEM:
                    emojiPopup = EmojiPopup.Builder.fromRootView(findViewById(android.R.id.content)).build(description);

                    storageRef = FirebaseStorage.getInstance().getReference(AwesumApp.STORAGE_POST_IMAGE).child(userId);
                    postRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POST);
                    postMainRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POSTMAIN).child(userId);
                    userRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER).child(userId);

                    initializeInputOutputArray();
                    if (input.length > 1)
                        setupRecyclerView();

                    setCropImageSquare(true);
                    setUploadPost();
                    setOnClickSmile();
                    break;
                case Post.VIDEO_TYPE_ITEM:
                    emojiPopup = EmojiPopup.Builder.fromRootView(findViewById(android.R.id.content)).build(description);

                    storageRef = FirebaseStorage.getInstance().getReference(AwesumApp.STORAGE_POST_IMAGE).child(userId);
                    postRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POST);
                    postMainRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POSTMAIN).child(userId);
                    userRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER).child(userId);

                    cropImageView.setVisibility(View.GONE);
                    video.setVisibility(View.VISIBLE);

                    int[] displayMetric = Ultility.getDisplayMetric(this);
                    video.setLayoutParams(new LinearLayout.LayoutParams(displayMetric[1], displayMetric[1]));

                    setUploadPost();
                    setOnClickSmile();
                    break;
                case Story.TYPE_ITEM:
                    description.setVisibility(View.GONE);
                    smile.setVisibility(View.GONE);

                    storageRef = FirebaseStorage.getInstance().getReference(AwesumApp.STORAGE_STORY_IMAGE).child(userId);
                    storyRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_STORY);
                    storyMainRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_STORYMAIN).child(userId);
                    userRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER).child(userId);

                    initializeInputOutputArray();
                    if (input.length > 1)
                        setupRecyclerView();
                    setCropImageSquare(false);
                    setUploadStory();
                    break;
            }
        }

        setCloseAction();
        Ultility.showKeyboard(this);
        description.requestFocus();

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (itemType == Post.VIDEO_TYPE_ITEM) {
            videoUri = selectedList.get(0);
            setPlayerView();
        }
    }

    private void initializeInputOutputArray() {
        input = new Uri[selectedList.size()];
        output = new Uri[selectedList.size()];
        for (int i = 0; i < selectedList.size(); ++i) {
            input[i] = selectedList.get(i);
        }

        cropImageView.setImageUriAsync(input[0]);
    }

    private void setPlayerView() {
        player = new SimpleExoPlayer.Builder(this).build();
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "Awesum"));
        MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(videoUri);
        player.prepare(videoSource);
        video.setPlayer(player);
    }

    private void setCropImageSquare(boolean isSquare) {
        int[] displayMetric = Ultility.getDisplayMetric(this);
        cropImageView.setLayoutParams(new LinearLayout.LayoutParams(displayMetric[1], displayMetric[1]));
        if (isSquare)
            cropImageView.setAspectRatio(1, 1);
    }

    private void setupRecyclerView() {
        adapter = new GridMediaCropAdapter(this, input);
        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                        ItemTouchHelper.DOWN | ItemTouchHelper.UP | ItemTouchHelper.START | ItemTouchHelper.END);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {

                Uri temp = output[viewHolder.getAdapterPosition()];
                output[viewHolder.getAdapterPosition()] = output[target.getAdapterPosition()];
                output[target.getAdapterPosition()] = temp;

                temp = input[viewHolder.getAdapterPosition()];
                input[viewHolder.getAdapterPosition()] = input[target.getAdapterPosition()];
                input[target.getAdapterPosition()] = temp;

                adapter.notifyDataSetChanged();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(allImageRecyclerView);

        adapter.setImageClickCropListener(this);
        allImageRecyclerView.setAdapter(adapter);
        allImageRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
    }

    private void setOnClickSmile() {
        smile.setOnClickListener(v -> {
            if (!emojiPopup.isShowing()) {
                emojiPopup.toggle();
                smile.setImageResource(R.drawable.ic_keyboard);
            } else {
                emojiPopup.toggle();
                smile.setImageResource(R.drawable.ic_smile);
            }
        });
    }

    private void setCloseAction() {
        close.setOnClickListener(v -> {
            finish();
            Ultility.hideKeyboard(this);
        });
    }

    private void setUploadPost() {
        done.setOnClickListener(v -> {
            Ultility.hideKeyboard(this);
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.please_wait));
            progressDialog.show();
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);

            if (TextUtils.isEmpty(description.getText().toString().trim())) {
                description.setError(getString(R.string.required));
                progressDialog.dismiss();
            } else if (itemType != Post.VIDEO_TYPE_ITEM) {
                cropLastImage();
            } else {
                player.setPlayWhenReady(false);
                uploadVideo();
            }
        });
    }

    private void uploadVideo() {
        long t = System.currentTimeMillis();
        StorageReference ref = storageRef.child(String.valueOf(t)).child("0");
        StorageReference refThumb = storageRef.child(String.valueOf(t)).child("1");

        UploadTask uploadTask = ref.putFile(videoUri);
        uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (!task.isSuccessful()) {
                    Toast.makeText(AddPostActivity.this, "Failed to upload!", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    finish();
                }
                return ref.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    String downloadUrl = task.getResult().toString();
                    Post post = new Post(userId, description.getText().toString().trim(), t, Post.VIDEO_TYPE_ITEM);
                    String postId = String.valueOf(t);

                    postRef.child(postId).setValue(post).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            postRef.child(postId).child("url").setValue(downloadUrl);

                            UploadTask uploadThumb = refThumb.putFile(thumb);
                            uploadThumb.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                                @Override
                                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                    return refThumb.getDownloadUrl();
                                }
                            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    if (task.isSuccessful()) {
                                        String thumbUrl = task.getResult().toString();
                                        postRef.child(postId).child("thumbUrl").setValue(thumbUrl);
                                    }
                                }
                            });


                            userRef.child(AwesumApp.DB_POST).child(postId).setValue(true);
                            postMainRef.child(postId).setValue(true);
                            player.setPlayWhenReady(false);
                            player.release();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    finish();
                                }
                            }, 1000);

                        }
                    });
                }
            }
        });
    }

    private void uploadPost() {
        long t = System.currentTimeMillis();
        StorageReference ref = storageRef.child(String.valueOf(t)).child("0");
        UploadTask uploadTask = ref.putFile(output[0]);
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(AddPostActivity.this, "Failed to upload!", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
                finish();
            }
            return ref.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String downloadUrl = task.getResult().toString();
                Post post = new Post(userId, description.getText().toString().trim(), t, Post.IMAGE_TYPE_ITEM);
                String postId = String.valueOf(t);
                postRef.child(postId).setValue(post).addOnCompleteListener(task1 -> {
                    postRef.child(postId).child("url").child("0").setValue(downloadUrl);
                    userRef.child(AwesumApp.DB_POST).child(postId).setValue(true);
                    postMainRef.child(postId).setValue(true);

                    if (output.length == 1) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                finish();
                            }
                        }, 500);
                    } else {
                        for (int i = 1; i < output.length; ++i) {
                            Uri uri = output[i];
                            StorageReference ref1 = storageRef.child(String.valueOf(t)).child(String.valueOf(i));
                            UploadTask uploadTask1 = ref1.putFile(uri);
                            int finalI = i;
                            uploadTask1.continueWithTask(task2 -> {
                                if (!task2.isSuccessful()) {
                                    Toast.makeText(AddPostActivity.this, "Failed to upload photo!", Toast.LENGTH_SHORT).show();
                                }
                                return ref1.getDownloadUrl();
                            }).addOnCompleteListener(task3 -> {
                                if (task3.isSuccessful() && task3.getResult() != null) {
                                    postRef.child(postId).child("url").child(String.valueOf(finalI))
                                            .setValue(task3.getResult().toString());
                                    if (finalI == output.length - 1) {
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                progressDialog.dismiss();
                                                finish();
                                            }
                                        }, 1000);

                                    }
                                }
                            });
                        }
                    }
                });
            }

        });
    }

    private void setUploadStory() {
        done.setOnClickListener(v -> {
            Ultility.hideKeyboard(this);
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.please_wait));
            progressDialog.show();
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);

            cropLastImage();
        });
    }

    private void uploadStory() {
        for (int i = 0; i < output.length; ++i) {
            long t = System.currentTimeMillis();
            StorageReference ref = storageRef.child(String.valueOf(t));
            UploadTask uploadTask = ref.putFile(output[i]);
            int finalI = i;
            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    Toast.makeText(AddPostActivity.this, "Failed to upload photo!", Toast.LENGTH_SHORT).show();

                    progressDialog.dismiss();
                    finish();
                }
                return ref.getDownloadUrl();
            }).addOnCompleteListener(task1 -> {
                if (task1.isSuccessful() && task1.getResult() != null) {
                    String url = task1.getResult().toString();
                    Story story = new Story(String.valueOf(t), url, userId);
                    storyRef.child(String.valueOf(t)).setValue(story);
                    storyMainRef.child(userId).child(String.valueOf(t)).setValue(true);
                    userRef.child(AwesumApp.DB_STORY).child(String.valueOf(t)).setValue(true);
                    storyMainRef.child(userId).child("lastest").setValue(String.valueOf(t));

                    if (finalI == output.length - 1) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                finish();
                            }
                        }, 1000);
                    }
                } else {
                    Toast.makeText(AddPostActivity.this, "Failed to upload photo!", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    finish();
                }
            });
        }
    }

    private void cropLastImage() {
        File temp = null;
        try {
            temp = createImageFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (temp != null) {
            cropImageView.setOnCropImageCompleteListener((view, result) -> {
                output[getLastPostition()] = result.getUri();
                cropAtPosition(0);
            });
            cropImageView.saveCroppedImageAsync(Uri.fromFile(temp));
        }
    }

    private void cropAtPosition(int position) {
        if (position < output.length && output[position] == null) {
            cropImageView.setOnSetImageUriCompleteListener((view, uri, error) -> {
                File temp = null;
                try {
                    temp = createImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (temp != null) {
                    cropImageView.setOnCropImageCompleteListener((view1, result) -> {
                        output[position] = result.getUri();
                        cropAtPosition(position + 1);
                    });
                    cropImageView.saveCroppedImageAsync(Uri.fromFile(temp));
                }
            });
            cropImageView.setImageUriAsync(input[position]);
        } else if (position + 1 < output.length) {
            cropImageView.setOnSetImageUriCompleteListener((view, uri, error) -> cropAtPosition(position + 1));
            cropImageView.setImageUriAsync(input[position + 1]);
        } else {
            if (itemType == Post.IMAGE_TYPE_ITEM)
                uploadPost();
            else
                uploadStory();
        }
    }

    private int getLastPostition() {
        Uri last = cropImageView.getImageUri();
        for (int i = 0; i < input.length; ++i) {
            if (input[i].equals(last))
                return i;
        }
        return 0;
    }

    private File createImageFile() throws IOException {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("temp", ".jpg", storageDir);
    }

    @Override
    public void cropPrevious(int position) {
        int current = getLastPostition();
        File temp = null;
        try {
            temp = createImageFile();
        } catch (IOException e) {
            Toast.makeText(AddPostActivity.this, "Error!", Toast.LENGTH_SHORT).show();
        }
        if (temp != null) {
            cropImageView.setOnCropImageCompleteListener((view, result) -> {
                if (result.getUri() == null)
                    Toast.makeText(AddPostActivity.this, "Error!", Toast.LENGTH_SHORT).show();
                else {
                    output[current] = result.getUri();
                    cropImageView.setImageUriAsync(input[position]);
                }
            });
            cropImageView.saveCroppedImageAsync(Uri.fromFile(temp));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Ultility.hideKeyboard(this);
        if (player != null) {
            player.setPlayWhenReady(false);
            player.release();
        }
    }
}
