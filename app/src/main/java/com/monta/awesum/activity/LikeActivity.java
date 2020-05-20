package com.monta.awesum.activity;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.adapter.UserAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LikeActivity extends AppCompatActivity {

    private List<String> idList;
    private UserAdapter userAdapter;
    private LinearLayoutManager layoutManager;
    private RecyclerView likeRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private String postId;
    private String commentId;
    private DatabaseReference likeRef;
    private long endTime;
    private boolean needLoadMore = true;
    private boolean stopLoad = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_like);

        Toolbar toolbar = findViewById(R.id.like_bar);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_likes);
        likeRecyclerView = findViewById(R.id.like_recyclerview);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        idList = new ArrayList<>();
        userAdapter = new UserAdapter(this, null, idList);
        likeRecyclerView.setAdapter(userAdapter);
        layoutManager = new LinearLayoutManager(this);
        likeRecyclerView.setLayoutManager(layoutManager);

        postId = getIntent().getStringExtra("postId");
        commentId = getIntent().getStringExtra("commentId");

        if (commentId == null)
            likeRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POST)
                    .child(postId).child(AwesumApp.DB_LIKE);
        else
            likeRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POST)
                    .child(postId).child(AwesumApp.DB_COMMENT)
                    .child(commentId).child(AwesumApp.DB_LIKE);

        initializeLikes();

        setRefreshAction();

        setLoadMoreAction();

    }

    @Override
    protected void onStart() {
        super.onStart();
        attachListener();
    }

    private void setRefreshAction() {
        swipeRefreshLayout.setColorSchemeResources(R.color.colorRed, R.color.colorYellow, R.color.colorGreen, R.color.colorBlue);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            idList.clear();
            userAdapter.notifyDataSetChanged();
            initializeLikes();
        });
    }

    private void setLoadMoreAction() {
        swipeRefreshLayout.setRefreshing(true);
        likeRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (needLoadMore && (!stopLoad) && layoutManager.findLastVisibleItemPosition() == idList.size() - 1 && dy > 0) {
                    needLoadMore = false;
                    loadMoreLikes();
                } else if (dy < 0)
                    swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void initializeLikes() {
        Query likeQuery = likeRef.orderByValue().limitToLast(20);
        likeQuery.keepSynced(true);
        likeQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    endTime = (long) data.getValue() - 1;
                    break;
                }
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    idList.add(0, data.getKey());
                }
                userAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadMoreLikes() {
        swipeRefreshLayout.setRefreshing(true);
        Query query = likeRef.orderByValue().endAt(endTime).limitToLast(20);
        query.keepSynced(true);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    swipeRefreshLayout.setRefreshing(false);
                    stopLoad = true;
                } else {
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        endTime = (long) data.getValue() - 1;
                        break;
                    }
                    List<String> reverseList = new ArrayList<>();
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        reverseList.add(data.getKey());
                    }
                    Collections.reverse(reverseList);
                    idList.addAll(reverseList);
                    userAdapter.notifyItemRangeInserted(userAdapter.getItemCount(), reverseList.size());
                }
                needLoadMore = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void attachListener() {
        List<DatabaseReference> followStatusRef = userAdapter.getFollowStatusRef();
        List<ValueEventListener> followStatusListener = userAdapter.getFollowStatusListener();

        if (followStatusRef != null && followStatusListener != null) {
            for (int i = 0; i < followStatusRef.size(); ++i) {
                followStatusRef.get(i).addValueEventListener(followStatusListener.get(i));
            }
        }
    }

    private void detachListener() {
        List<DatabaseReference> followStatusRef = userAdapter.getFollowStatusRef();
        List<ValueEventListener> followStatusListener = userAdapter.getFollowStatusListener();

        if (followStatusRef != null && followStatusListener != null) {
            for (int i = 0; i < followStatusRef.size(); ++i) {
                followStatusRef.get(i).removeEventListener(followStatusListener.get(i));
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachListener();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            likeRecyclerView.setAdapter(null);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        likeRecyclerView.setAdapter(null);
    }
}
