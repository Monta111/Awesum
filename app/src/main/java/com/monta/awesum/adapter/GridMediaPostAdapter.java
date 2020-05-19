package com.monta.awesum.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.activity.SinglePostActivity;
import com.monta.awesum.model.Post;
import com.monta.awesum.ultility.Ultility;

import java.util.List;

public class GridMediaPostAdapter extends RecyclerView.Adapter<GridMediaPostAdapter.GridMediaPostHolder> {

    private Context context;
    private List<String> idPostList;
    private DatabaseReference postRef;
    private String username;

    public GridMediaPostAdapter(Context context, List<String> idPostList) {
        this.context = context;
        this.idPostList = idPostList;

        postRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POST);
    }


    @NonNull
    @Override
    public GridMediaPostHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View itemView = layoutInflater.inflate(R.layout.item_image_multi, parent, false);
        return new GridMediaPostHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull GridMediaPostHolder holder, int position) {
        setImageLayout(holder);

        holder.postId = idPostList.get(position);

        setImage(holder);
        setImageClickOpenPost(holder);
    }

    private void setImage(GridMediaPostHolder holder) {
        postRef.child(holder.postId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if ((long) dataSnapshot.child("type").getValue() == Post.IMAGE_TYPE_ITEM) {
                    if (dataSnapshot.child("url").getChildrenCount() == 1) {
                        holder.icon.setVisibility(View.GONE);
                        Glide.with(context).load(dataSnapshot.child("url").child("0").getValue()).into(holder.image);
                    } else {
                        holder.icon.setVisibility(View.VISIBLE);
                        for (DataSnapshot data : dataSnapshot.child("url").getChildren()) {
                            Glide.with(context).load(data.getValue()).into(holder.image);
                            break;
                        }
                    }

                } else {
                    holder.icon.setVisibility(View.VISIBLE);
                    Glide.with(context).load(R.drawable.ic_play).into(holder.icon);
                    Glide.with(context).load(dataSnapshot.child("thumbUrl").getValue()).into(holder.image);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void setImageLayout(GridMediaPostHolder holder) {
        AppCompatActivity activity = (AppCompatActivity) context;
        int[] displayMetric = Ultility.getDisplayMetric(activity);
        int width = displayMetric[1] / 3;
        holder.image.setLayoutParams(new RelativeLayout.LayoutParams(width, width));
    }

    private void setImageClickOpenPost(GridMediaPostHolder holder) {
        holder.image.setOnClickListener(v -> {
            Intent intent = new Intent(context, SinglePostActivity.class);
            intent.putExtra("postId", holder.postId);
            intent.putExtra("username", username);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return idPostList.size();
    }

    public void setUsername(String username) {
        this.username = username;
    }


    static class GridMediaPostHolder extends RecyclerView.ViewHolder {

        private ImageView image;
        private ImageView icon;
        private String postId;


        GridMediaPostHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image_post);
            icon = itemView.findViewById(R.id.multi_icon);
        }
    }
}
