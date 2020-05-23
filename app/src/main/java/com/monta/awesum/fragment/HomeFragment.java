package com.monta.awesum.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.activity.MainActivity;
import com.monta.awesum.adapter.PostAdapter;
import com.monta.awesum.adapter.StoryAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class HomeFragment extends Fragment {

    private DatabaseReference postMainRef;
    private DatabaseReference storyMainRef;
    private String userId;

    private PostAdapter postAdapter;
    private ArrayList<String> idPostList;
    private LinearLayoutManager layoutManager;
    private RecyclerView postRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout lonely;

    private RecyclerView storyRecyclerView;
    private StoryAdapter storyAdapter;
    private List<String> idUserStoryList;

    private Query initPost;
    private ValueEventListener initPostListener;
    private long endIdPost;

    private Query storyQuery;
    private ValueEventListener storyQueryListener;
    private long startIdStory = System.currentTimeMillis() - 86400000;

    private boolean needLoadMore;
    private boolean stopLoad;
    private boolean firstLoad = true;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        postRecyclerView = view.findViewById(R.id.post_recyclerview);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_posts);
        lonely = view.findViewById(R.id.lonely);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        postMainRef = FirebaseDatabase.getInstance().getReference().child(AwesumApp.DB_POSTMAIN).child(userId);
        storyMainRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_STORYMAIN).child(userId);

        idPostList = new ArrayList<>();
        if (postAdapter == null)
            postAdapter = new PostAdapter(view.getContext(), idPostList);
        postRecyclerView.setAdapter(postAdapter);
        layoutManager = new LinearLayoutManager(view.getContext());
        postRecyclerView.setLayoutManager(layoutManager);

        storyRecyclerView = view.findViewById(R.id.story_recyclerview);
        idUserStoryList = new ArrayList<>();
        if (storyAdapter == null)
            storyAdapter = new StoryAdapter(view.getContext(), idUserStoryList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        storyRecyclerView.setAdapter(storyAdapter);
        storyRecyclerView.setLayoutManager(linearLayoutManager);

        setRefreshAction();
        setLoadMoreAction();
        setMediaStateChangeListener();

        load();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!firstLoad)
            attachListener();
    }

    private void load() {
        needLoadMore = true;
        stopLoad = false;

        releasePlayer();
        detachListener();

        getStory();
        initializePosts();
    }


    private void setMediaStateChangeListener() {
        postRecyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {

            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                PlayerView playerView = view.findViewById(R.id.video);
                if (playerView.getPlayer() != null)
                    playerView.getPlayer().setPlayWhenReady(false);
            }
        });
    }

    private void getStory() {
        storyQuery = storyMainRef.orderByChild("lastest").startAt(String.valueOf(startIdStory));
        if (storyQueryListener == null)
            storyQueryListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    idUserStoryList.clear();
                    idUserStoryList.add(userId);
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        if (Long.parseLong((String) data.child("lastest").getValue()) > startIdStory)
                            idUserStoryList.add(1, data.getKey());
                    }
                    storyAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };

        storyQuery.addValueEventListener(storyQueryListener);
    }

    private void setRefreshAction() {
        swipeRefreshLayout.setColorSchemeResources(R.color.colorRed, R.color.colorYellow, R.color.colorGreen, R.color.colorBlue);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            load();
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).refreshPostBadge();
        });


    }

    private void setLoadMoreAction() {
        swipeRefreshLayout.setRefreshing(true);
        postRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (needLoadMore && (!stopLoad) && layoutManager.findLastVisibleItemPosition() == idPostList.size() - 1 && dy > 0) {
                    needLoadMore = false;
                    loadMorePost();
                } else if (dy < 0) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    private void initializePosts() {
        initPost = postMainRef.limitToLast(5);
        if (initPostListener == null)
            initPostListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    idPostList.clear();
                    if (dataSnapshot.getValue() == null) {
                        lonely.setVisibility(View.VISIBLE);
                        swipeRefreshLayout.setRefreshing(false);
                    } else {
                        List<String> reverseList = new ArrayList<>();
                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                            if (data.getValue() != null && (Boolean) data.getValue()) {
                                reverseList.add(data.getKey());
                            }
                        }
                        Collections.reverse(reverseList);
                        if (reverseList.size() == 0)
                            lonely.setVisibility(View.VISIBLE);
                        else {
                            lonely.setVisibility(View.GONE);
                            endIdPost = Long.parseLong(reverseList.get(reverseList.size() - 1)) - 1;
                        }
                        idPostList.addAll(reverseList);
                        postAdapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
        initPost.addValueEventListener(initPostListener);
    }

    private void loadMorePost() {
        swipeRefreshLayout.setRefreshing(true);
        Query query = postMainRef.orderByKey().endAt(String.valueOf(endIdPost)).limitToLast(10);
        query.keepSynced(true);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    swipeRefreshLayout.setRefreshing(false);
                    stopLoad = true;
                } else {
                    List<String> reverseList = new ArrayList<>();
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        if (data.getValue() != null && (Boolean) data.getValue())
                            reverseList.add(data.getKey());
                    }
                    Collections.reverse(reverseList);
                    if (reverseList.size() >= 1)
                        endIdPost = Long.parseLong(reverseList.get(reverseList.size() - 1)) - 1;
                    idPostList.addAll(reverseList);
                    postAdapter.notifyItemRangeInserted(postAdapter.getItemCount(), reverseList.size());
                    swipeRefreshLayout.setRefreshing(false);
                    needLoadMore = true;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

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

        List<DatabaseReference> seenStatusStoryRef = storyAdapter.getSeenStatusRef();
        List<ValueEventListener> seenStatusListener = storyAdapter.getSeenStatusListener();

        if (seenStatusStoryRef != null) {
            for (int i = 0; i < seenStatusStoryRef.size(); ++i) {
                if (seenStatusListener.get(i) != null)
                    seenStatusStoryRef.get(i).removeEventListener(seenStatusListener.get(i));
            }
        }

        if (initPost != null && initPostListener != null)
            initPost.removeEventListener(initPostListener);

        if (storyQuery != null && storyQueryListener != null)
            storyQuery.removeEventListener(storyQueryListener);
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

        List<DatabaseReference> seenStatusStoryRef = storyAdapter.getSeenStatusRef();
        List<ValueEventListener> seenStatusListener = storyAdapter.getSeenStatusListener();

        if (seenStatusStoryRef != null) {
            for (int i = 0; i < seenStatusStoryRef.size(); ++i) {
                if (seenStatusListener.get(i) != null)
                    seenStatusStoryRef.get(i).addValueEventListener(seenStatusListener.get(i));
            }
        }

        if (initPost != null && initPostListener != null)
            initPost.addValueEventListener(initPostListener);

        if (storyQuery != null && storyQueryListener != null)
            storyQuery.addValueEventListener(storyQueryListener);
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
    public void onDestroyView() {
        super.onDestroyView();
        for (Player player : postAdapter.getPlayers()) {
            if (player != null) {
                player.setPlayWhenReady(false);
            }
        }

        detachListener();
        firstLoad = false;
        postRecyclerView.setAdapter(null);
        storyRecyclerView.setAdapter(null);
    }
}