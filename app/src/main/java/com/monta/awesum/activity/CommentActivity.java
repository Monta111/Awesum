package com.monta.awesum.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.adapter.CommentAdapter;
import com.monta.awesum.model.Comment;
import com.monta.awesum.model.Notification;
import com.monta.awesum.model.User;
import com.monta.awesum.ultility.Ultility;
import com.vanniktech.emoji.EmojiPopup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommentActivity extends AppCompatActivity {
    private ImageView currentUserProfileImage;
    private ImageView sendComment;
    private EditText currentUserComment;
    private ImageView smile;
    private EmojiPopup emojiPopup;

    private List<Comment> commentList;
    private CommentAdapter commentAdapter;
    private RecyclerView commentRecyclerView;
    private LinearLayoutManager layoutManager;
    private SwipeRefreshLayout swipeRefreshLayout;

    private boolean needLoadMore = true;
    private boolean stopLoad = false;
    private long endTime;
    private long lastestCommentTimeStamp;
    private String userId;
    private String postId;
    private String publisherId;

    private DatabaseReference commentRef;
    private DatabaseReference notificationRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        postId = getIntent().getStringExtra("postId");
        publisherId = getIntent().getStringExtra("publisherId");

        Toolbar commentBar = findViewById(R.id.comment_bar);
        setSupportActionBar(commentBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        commentRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POST).child(postId).child(AwesumApp.DB_COMMENT);
        notificationRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_NOTIFICATION).child(publisherId);

        currentUserProfileImage = findViewById(R.id.current_user_profile_image);
        commentRecyclerView = findViewById(R.id.comment_recyclerview);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_comments);
        sendComment = findViewById(R.id.send_comment);
        currentUserComment = findViewById(R.id.current_user_comment);
        smile = findViewById(R.id.smile);

        emojiPopup = EmojiPopup.Builder.fromRootView(findViewById(android.R.id.content)).build(currentUserComment);

        currentUserComment.requestFocus();
        Ultility.showKeyboard(this);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        setUserCommentInfor();

        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(this, commentList, true);
        commentAdapter.setInfo(postId, publisherId);
        commentRecyclerView.setAdapter(commentAdapter);
        layoutManager = new LinearLayoutManager(this);
        commentRecyclerView.setLayoutManager(layoutManager);

        setRefreshAction();

        setLoadMoreAction();

        setSendCommentAction();

        setOnClickSmile();

        initializeComments();
    }

    private void setUserCommentInfor() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER).child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    Glide.with(CommentActivity.this).load(user.getAvatarUrl()).circleCrop().into(currentUserProfileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setRefreshAction() {
        swipeRefreshLayout.setColorSchemeResources(R.color.colorRed, R.color.colorYellow, R.color.colorGreen, R.color.colorBlue);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            commentList.clear();
            commentAdapter.notifyDataSetChanged();
            initializeComments();
        });
    }

    private void setLoadMoreAction() {
        swipeRefreshLayout.setRefreshing(true);
        commentRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (needLoadMore && (!stopLoad) && layoutManager.findLastVisibleItemPosition() == commentList.size() - 1 && dy > 0) {
                    needLoadMore = false;
                    loadMoreComment();
                } else if (dy < 0) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    private void setSendCommentAction() {
        sendComment.setOnClickListener(v -> {
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
                    getLastestComment();
                    Ultility.hideKeyboard(CommentActivity.this);
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

    private void initializeComments() {
        Query query = commentRef.limitToLast(15);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    Comment comment = data.getValue(Comment.class);
                    if (comment != null)
                        endTime = comment.getId() - 1;
                    break;
                }
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    Comment comment = data.getValue(Comment.class);
                    if (comment != null) {
                        commentList.add(0, comment);
                        lastestCommentTimeStamp = comment.getId() + 1;
                    }
                }
                commentAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getLastestComment() {
        Query getNewComment = commentRef.orderByChild("id").startAt(lastestCommentTimeStamp);
        getNewComment.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        Comment comment = data.getValue(Comment.class);
                        if (comment != null) {
                            commentList.add(0, comment);
                            lastestCommentTimeStamp = comment.getId() + 1;
                        }
                    }
                    commentAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadMoreComment() {
        swipeRefreshLayout.setRefreshing(true);
        Query query = commentRef.orderByChild("id").endAt(endTime).limitToLast(10);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    swipeRefreshLayout.setRefreshing(false);
                    stopLoad = true;
                } else {
                    List<Comment> reverseList = new ArrayList<>();
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        Comment comment = data.getValue(Comment.class);
                        reverseList.add(comment);
                    }
                    Collections.reverse(reverseList);
                    endTime = reverseList.get(reverseList.size() - 1).getId() - 1;
                    commentList.addAll(reverseList);
                    commentAdapter.notifyDataSetChanged();
                    swipeRefreshLayout.setRefreshing(false);
                }
                needLoadMore = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            commentAdapter = null;
            commentRecyclerView.setAdapter(null);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        commentAdapter = null;
        commentRecyclerView.setAdapter(null);
    }

    @Override
    public void onPause() {
        super.onPause();
        Ultility.hideKeyboard(this);
    }
}
