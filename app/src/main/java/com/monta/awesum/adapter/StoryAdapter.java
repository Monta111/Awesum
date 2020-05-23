package com.monta.awesum.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.activity.PickMediaActivity;
import com.monta.awesum.activity.StoryActivity;
import com.monta.awesum.model.Story;
import com.monta.awesum.model.User;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryHolder> {

    private Context context;
    private List<String> idUserStoryList;
    private String userId;
    private DatabaseReference storyRef;

    private List<DatabaseReference> seenStatusRef;
    private List<ValueEventListener> seenStatusListener;

    public StoryAdapter(Context context, List<String> idUserStoryList) {

        this.context = context;
        this.idUserStoryList = idUserStoryList;
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        storyRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_STORYMAIN).child(userId);

        seenStatusListener = new ArrayList<>();
        seenStatusRef = new ArrayList<>();
    }

    @NonNull
    @Override
    public StoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_story, parent, false);
        return new StoryHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryHolder holder, int position) {

        if (position == 0) {
            holder.addStory.setVisibility(View.VISIBLE);
            setAddStory(holder);
        } else {
            holder.addStory.setVisibility(View.GONE);
            setClickOpenStory(holder);
            setSeenStatus(holder);
        }

        setUserStoryInfo(holder);

    }

    private void setClickOpenStory(StoryHolder holder) {
        holder.imageView.setOnClickListener(v -> {
            Intent intent = new Intent(context, StoryActivity.class);
            intent.putExtra("position", holder.getAdapterPosition() - 1);
            intent.putStringArrayListExtra("idUserStoryList", (ArrayList<String>) idUserStoryList);
            context.startActivity(intent);
        });
    }

    private void setSeenStatus(StoryHolder holder) {
        holder.seenRef = storyRef.child(idUserStoryList.get(holder.getAdapterPosition()));
        if (holder.seenListener == null)
            holder.seenListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    boolean isSeen = true;
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        if (data.getKey() != null && !data.getKey().equals("lastest") && Long.parseLong(data.getKey()) > (System.currentTimeMillis() - 86400000)) {
                            if (data.getValue() != null && !((Boolean) data.getValue())) {
                                isSeen = false;
                                break;
                            }
                        }
                    }
                    if (isSeen)
                        holder.layout.setBackgroundResource(R.color.colorWhite);
                    else
                        holder.layout.setBackgroundResource(R.drawable.circular_bordershape);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };

        seenStatusRef.add(holder.seenRef);
        seenStatusListener.add(holder.seenListener);
        holder.seenRef.addValueEventListener(holder.seenListener);
    }

    @Override
    public void onViewRecycled(@NonNull StoryHolder holder) {
        super.onViewRecycled(holder);
        if (holder.seenRef != null && holder.seenListener != null) {
            holder.seenRef.removeEventListener(holder.seenListener);
            seenStatusRef.remove(holder.seenRef);
            seenStatusListener.remove(holder.seenListener);
        }
    }

    private void setUserStoryInfo(StoryHolder holder) {
        FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER).child(idUserStoryList.get(holder.getAdapterPosition()))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.getValue(User.class);
                        if (user != null) {
                            Glide.with(context).load(user.getAvatarUrl()).placeholder(R.drawable.defaultavatar).into(holder.imageView);
                            if (holder.getAdapterPosition() == 0)
                                holder.username.setText(context.getString(R.string.your_story));
                            else
                                holder.username.setText(user.getUsername());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void setAddStory(StoryHolder holder) {
        holder.imageView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PickMediaActivity.class);
            intent.putExtra("itemType", Story.TYPE_ITEM);
            context.startActivity(intent);
        });
    }


    @Override
    public int getItemCount() {
        return idUserStoryList.size();
    }

    public List<DatabaseReference> getSeenStatusRef() {
        return seenStatusRef;
    }

    public List<ValueEventListener> getSeenStatusListener() {
        return seenStatusListener;
    }

    public static class StoryHolder extends RecyclerView.ViewHolder {

        private CircleImageView imageView;
        private ImageView addStory;
        private TextView username;
        private RelativeLayout layout;

        private ValueEventListener seenListener;
        private DatabaseReference seenRef;

        public StoryHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.profile_image);
            username = itemView.findViewById(R.id.username);
            addStory = itemView.findViewById(R.id.add);
            layout = itemView.findViewById(R.id.layout);
        }
    }
}
