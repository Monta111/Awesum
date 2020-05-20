package com.monta.awesum.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.adapter.GridMediaPostAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllPostFragment extends Fragment {

    private String userId;
    private String username;
    private GridMediaPostAdapter adapter;
    private RecyclerView allPost;
    private List<String> idPostList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_grid_post, container, false);

        if (getArguments() != null) {
            userId = getArguments().getString("userId", "");
            username = getArguments().getString("username", "");

            allPost = view.findViewById(R.id.grid_post_recyclerview);
            idPostList = new ArrayList<>();
            if (adapter == null)
                adapter = new GridMediaPostAdapter(getActivity(), idPostList);
            adapter.setUsername(username);
            allPost.setAdapter(adapter);
            allPost.setLayoutManager(new GridLayoutManager(getActivity(), 3));

            getAllPost();
        }

        return view;
    }

    private void getAllPost() {
        FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER).child(userId).child(AwesumApp.DB_POST)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                            String id = data.getKey();
                            idPostList.add(id);
                        }
                        Collections.reverse(idPostList);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        allPost.setAdapter(null);
    }
}
