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

public class SavePostFragment extends Fragment {

    private GridMediaPostAdapter adapter;
    private RecyclerView savePostRecyclerView;
    private List<String> idPostList;
    private String userId;
    private String username;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_grid_post, container, false);

        if (getArguments() != null) {
            userId = getArguments().getString("userId");
            username = getArguments().getString("username");

            savePostRecyclerView = view.findViewById(R.id.grid_post_recyclerview);
            idPostList = new ArrayList<>();
            if (adapter == null)
                adapter = new GridMediaPostAdapter(getActivity(), idPostList);
            adapter.setUsername(username);
            savePostRecyclerView.setAdapter(adapter);
            savePostRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

            getAllSavePost();
        }
        return view;
    }

    private void getAllSavePost() {
        FirebaseDatabase.getInstance().getReference(AwesumApp.DB_SAVE).child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                            idPostList.add(data.getKey());
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
        savePostRecyclerView.setAdapter(null);
    }
}
