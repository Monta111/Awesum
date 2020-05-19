package com.monta.awesum.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.Player;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.adapter.PostAdapter;
import com.monta.awesum.model.Comment;
import com.monta.awesum.model.Notification;
import com.monta.awesum.model.User;
import com.vanniktech.emoji.EmojiPopup;

import java.util.ArrayList;
import java.util.List;

public class SinglePostActivity extends AppCompatActivity {

    private ImageView currentUserProfileImage;
    private ImageView sendComment;
    private EditText currentUserComment;
    private ImageView smile;
    private EmojiPopup emojiPopup;

    private String userId;
    private String postId;
    private String publisher;
    private String publisherId;
    private PostAdapter postAdapter;
    private RecyclerView postRecyclerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_post);

        Toolbar toolbar = findViewById(R.id.single_post_bar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        currentUserProfileImage = findViewById(R.id.current_user_profile_image);
        sendComment = findViewById(R.id.send_comment);
        currentUserComment = findViewById(R.id.current_user_comment);
        smile = findViewById(R.id.smile);

        emojiPopup = EmojiPopup.Builder.fromRootView(findViewById(android.R.id.content)).build(currentUserComment);

        postRecyclerView = findViewById(R.id.post_recyclerview);
        TextView title = findViewById(R.id.title_single_post);

        postId = getIntent().getStringExtra("postId");

        FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POST).child(postId).child("publisherId")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        publisherId = (String) dataSnapshot.getValue();
                        FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER)
                                .child(publisherId).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                publisher = (String) dataSnapshot.getValue();
                                title.setText(publisher);
                                title.append("\'s Post");
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        setUserCommentInfor();
        setSendCommentAction();
        setOnClickSmile();

        List<String> idPostList = new ArrayList<>();
        postAdapter = new PostAdapter(this, idPostList);
        postRecyclerView.setAdapter(postAdapter);
        postRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        idPostList.add(postId);
        postAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachListener();
    }

    private void setUserCommentInfor() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER).child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    Glide.with(SinglePostActivity.this).load(user.getAvatarUrl()).circleCrop().into(currentUserProfileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setSendCommentAction() {
        sendComment.setOnClickListener(v -> {
            DatabaseReference commentRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POST)
                    .child(postId).child(AwesumApp.DB_COMMENT);
            DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_NOTIFICATION)
                    .child(publisherId);
            String comment = currentUserComment.getText().toString().trim();
            if (!TextUtils.isEmpty(comment)) {
                long t = System.currentTimeMillis();
                Comment newComment = new Comment(comment, t, userId);
                commentRef.child(String.valueOf(t)).setValue(newComment).addOnCompleteListener(task -> {
                    currentUserComment.setText("");
                    if (!userId.equals(publisherId)) {
                        Notification notification = new Notification(Notification.COMMENT_TYPE, t, userId, Notification.COMMENT_TYPE_CONTENT + comment, postId, false, false, "null");
                        notificationRef.child(String.valueOf(t)).setValue(notification);
                    }
                });
            }
        });

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

    private void detachListener() {
        List<Query> followStatusRef = postAdapter.getFollowStatusRef();
        List<Query> hideStatusRef = postAdapter.getHideStatusRef();
        List<Query> newestCommentRef = postAdapter.getNewestCommentRef();
        List<ValueEventListener> followListener = postAdapter.getFollowListener();
        List<ValueEventListener> hideListener = postAdapter.getHideListener();
        List<ValueEventListener> newestCommentListener = postAdapter.getNewestCommentListener();

        if (followStatusRef != null)
            for (int i = 0; i < followStatusRef.size(); ++i) {
                if (followListener.get(i) != null)
                    followStatusRef.get(i).removeEventListener(followListener.get(i));
            }

        if (hideStatusRef != null)
            for (int i = 0; i < hideStatusRef.size(); ++i) {
                if (hideListener.get(i) != null)
                    hideStatusRef.get(i).removeEventListener(hideListener.get(i));
            }

        if (newestCommentRef != null)
            for (int i = 0; i < newestCommentRef.size(); ++i) {
                if (newestCommentListener.get(i) != null)
                    newestCommentRef.get(i).removeEventListener(newestCommentListener.get(i));
            }
    }

    private void attachListener() {
        List<Query> followStatusRef = postAdapter.getFollowStatusRef();
        List<Query> hideStatusRef = postAdapter.getHideStatusRef();
        List<Query> newestCommentRef = postAdapter.getNewestCommentRef();
        List<ValueEventListener> followListener = postAdapter.getFollowListener();
        List<ValueEventListener> hideListener = postAdapter.getHideListener();
        List<ValueEventListener> newestCommentListener = postAdapter.getNewestCommentListener();
        if (followStatusRef != null)
            for (int i = 0; i < followStatusRef.size(); ++i) {
                if (followListener.get(i) != null)
                    followStatusRef.get(i).addValueEventListener(followListener.get(i));
            }

        if (hideStatusRef != null)
            for (int i = 0; i < hideStatusRef.size(); ++i) {
                if (hideListener.get(i) != null)
                    hideStatusRef.get(i).addValueEventListener(hideListener.get(i));
            }

        if (newestCommentRef != null)
            for (int i = 0; i < newestCommentRef.size(); ++i) {
                if (newestCommentListener.get(i) != null)
                    newestCommentRef.get(i).addValueEventListener(newestCommentListener.get(i));
            }
    }

    private void releasePlayer() {
        for (Player player : postAdapter.getPlayers()) {
            if (player != null) {
                player.setPlayWhenReady(false);
                player.release();
            }
        }
        postAdapter.getPlayers().clear();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            releaseResource();
            finish();
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        releaseResource();
    }

    private void releaseResource() {
        detachListener();
        releasePlayer();
        postRecyclerView.setAdapter(null);
    }


    @Override
    protected void onStop() {
        super.onStop();
        detachListener();
        for (Player player : postAdapter.getPlayers()) {
            if (player != null) {
                player.setPlayWhenReady(false);
            }
        }
    }
}
