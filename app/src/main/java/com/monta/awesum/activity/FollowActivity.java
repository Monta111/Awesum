package com.monta.awesum.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

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

public class FollowActivity extends AppCompatActivity {

    private List<String> idList;
    private UserAdapter userAdapter;
    private LinearLayoutManager layoutManager;
    private RecyclerView followerRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView title;

    private String userId;
    private long endTime;
    private DatabaseReference followRef;
    private boolean needLoadMore = true;
    private boolean stopLoad = false;
    private String followType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow);

        Toolbar toolbar = findViewById(R.id.follower_bar);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_followers);
        followerRecyclerView = findViewById(R.id.follower_recyclerview);
        title = findViewById(R.id.title);


        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        followType = getIntent().getStringExtra("followType");
        if (followType != null) {
            if (followType.equals("Follower"))
                title.setText(getString(R.string.followers));
            else
                title.setText(getString(R.string.following));
        }

        idList = new ArrayList<>();
        userAdapter = new UserAdapter(this, null, idList);
        followerRecyclerView.setAdapter(userAdapter);
        layoutManager = new LinearLayoutManager(this);
        followerRecyclerView.setLayoutManager(layoutManager);

        userId = getIntent().getStringExtra("userId");
        followRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_FOLLOW)
                .child(userId).child(followType);
        initializeFollow();
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
            initializeFollow();
        });
    }

    private void setLoadMoreAction() {
        swipeRefreshLayout.setRefreshing(true);
        followerRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (needLoadMore && (!stopLoad) && layoutManager.findLastVisibleItemPosition() == idList.size() - 1 && dy > 0) {
                    needLoadMore = false;
                    loadMoreFollow();
                } else if (dy < 0)
                    swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void initializeFollow() {
        Query query = followRef.orderByValue().limitToLast(20);
        query.keepSynced(true);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    endTime = (long) data.getValue() - 1;
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

    private void loadMoreFollow() {
        swipeRefreshLayout.setRefreshing(true);
        Query query = followRef.orderByValue().endAt(endTime).limitToLast(20);
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
            userAdapter = null;
            followerRecyclerView.setAdapter(null);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        userAdapter = null;
        followerRecyclerView.setAdapter(null);
    }
}
