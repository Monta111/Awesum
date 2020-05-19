package com.monta.awesum.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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
import com.monta.awesum.model.User;
import com.monta.awesum.ultility.Ultility;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private UserAdapter userAdapter;
    private List<User> userList;

    private EditText searchEditText;
    private SwipeRefreshLayout swipeRefreshLayout;

    private DatabaseReference userRef;
    private String lastResult;
    private boolean getAll;
    private boolean needLoadMore = true;
    private boolean stopLoad = false;

    private Query query;
    private ValueEventListener listener;

    public SearchFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        userRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER);

        recyclerView = view.findViewById(R.id.search_recyclerview);
        searchEditText = view.findViewById(R.id.search_edittext);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_search);

        searchEditText.requestFocus();
        if (getContext() != null)
            Ultility.showKeyboard(getContext());

        userList = new ArrayList<>();
        if (userAdapter == null)
            userAdapter = new UserAdapter(getContext(), userList, null);
        recyclerView.setAdapter(userAdapter);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        setTextChangeListener();
        setRefreshAction();
        setLoadMoreAction();

        return view;
    }


    private void setTextChangeListener() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUser(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void setRefreshAction() {
        swipeRefreshLayout.setColorSchemeResources(R.color.colorRed, R.color.colorYellow, R.color.colorGreen, R.color.colorBlue);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            searchUser(searchEditText.getText().toString().trim());
        });
    }

    private void setLoadMoreAction() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (needLoadMore && (!stopLoad) && layoutManager.findLastVisibleItemPosition() == userList.size() - 1 && dy > 0) {
                    needLoadMore = false;
                    swipeRefreshLayout.setRefreshing(true);
                    loadMoreResult();
                } else if (dy < 0)
                    swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void loadMoreResult() {
        Query query;
        if (getAll)
            query = userRef.orderByChild("username").startAt(lastResult).endAt("\uf8ff").limitToFirst(10);
        else
            query = userRef.orderByChild("username").startAt(lastResult).endAt(lastResult + "\uf8ff").limitToFirst(10);
        query.keepSynced(true);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    swipeRefreshLayout.setRefreshing(false);
                    stopLoad = true;
                } else {
                    int count = 0;
                    List<User> addList = new ArrayList<>();
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        User user = data.getValue(User.class);
                        ++count;
                        if (count > 1) {
                            addList.add(user);
                            lastResult = user.getUsername();
                        }
                    }

                    userList.addAll(addList);
                    userAdapter.notifyItemRangeInserted(userAdapter.getItemCount(), addList.size());
                    swipeRefreshLayout.setRefreshing(false);
                    needLoadMore = true;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        attachListener();
    }

    private void searchUser(String input) {
        getAll = input.equals("");

        lastResult = "";
        stopLoad = false;

        userList.clear();
        userAdapter.notifyDataSetChanged();

        swipeRefreshLayout.setRefreshing(true);

        if (query != null && listener != null)
            query.removeEventListener(listener);

        query = userRef.orderByChild("username").startAt(input).endAt(input + "\uf8ff").limitToFirst(20);
        if (listener == null)
            listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        User user = data.getValue(User.class);
                        userList.add(user);
                        lastResult = user.getUsername();
                    }
                    userAdapter.notifyDataSetChanged();
                    swipeRefreshLayout.setRefreshing(false);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };

        query.addValueEventListener(listener);
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
    public void onPause() {
        super.onPause();
        if (getContext() != null)
            Ultility.hideKeyboard(getContext());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        detachListener();
        if (query != null && listener != null)
            query.removeEventListener(listener);
        userAdapter = null;
        recyclerView.setAdapter(null);
    }
}
