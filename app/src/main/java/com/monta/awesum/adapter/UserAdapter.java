package com.monta.awesum.adapter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
import com.monta.awesum.activity.ProfileActivity;
import com.monta.awesum.model.Notification;
import com.monta.awesum.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserHolder> {

    private Context context;
    private List<User> userList;
    private List<String> idList;
    private String userId;
    private DatabaseReference followRef;
    private DatabaseReference notificationRef;

    private List<DatabaseReference> followStatusRef;
    private List<ValueEventListener> followStatusListener;

    public UserAdapter(Context context, List<User> userList, List<String> idList) {
        this.context = context;
        this.userList = userList;
        this.idList = idList;

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        followRef = FirebaseDatabase.getInstance().getReference().child(AwesumApp.DB_FOLLOW);
        notificationRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_NOTIFICATION);

        followStatusRef = new ArrayList<>();
        followStatusListener = new ArrayList<>();
    }

    @NonNull
    @Override
    public UserHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View itemView = layoutInflater.inflate(R.layout.item_search_user, parent, false);
        return new UserHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull UserHolder holder, int position) {
        if (userList != null) {
            User current = userList.get(position);
            holder.id = current.getId();
            holder.notiRef = notificationRef.child(holder.id);
            holder.notiRef.keepSynced(true);

            setUserInfo(holder, current);
            setFollowStatus(holder);
            setFollowButtonAction(holder);
            setItemClick(holder);
        } else {
            holder.id = idList.get(holder.getAdapterPosition());
            holder.notiRef = notificationRef.child(holder.id);

            FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER).child(holder.id)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            User current = dataSnapshot.getValue(User.class);
                            if (current != null) {
                                setUserInfo(holder, current);
                                setFollowStatus(holder);
                                setFollowButtonAction(holder);
                                setItemClick(holder);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }
    }

    @Override
    public void onViewRecycled(@NonNull UserHolder holder) {
        super.onViewRecycled(holder);
        if (holder.followRef != null && holder.eventListener != null) {
            holder.followRef.removeEventListener(holder.eventListener);
            followStatusRef.remove(holder.followRef);
            followStatusListener.remove(holder.eventListener);
        }
    }

    @Override
    public int getItemCount() {
        if (userList != null)
            return userList.size();
        else
            return idList.size();
    }

    private void setUserInfo(UserHolder holder, User current) {
        holder.username.setText(current.getUsername());
        holder.fullname.setText(current.getFullname());
        Glide.with(context).load(current.getAvatarUrl()).placeholder(R.drawable.defaultavatar).circleCrop().into(holder.profileImage);
    }

    private void setFollowStatus(UserHolder holder) {
        holder.followRef = followRef.child(userId).child(AwesumApp.DB_FOLLOWING);

        if (holder.eventListener == null)
            holder.eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (holder.id.equals(userId)) {
                        holder.followButton.setVisibility(View.GONE);
                    } else if (dataSnapshot.hasChild(holder.id)) {
                        holder.followButton.setVisibility(View.VISIBLE);
                        holder.followButton.setText(context.getString(R.string.following));
                    } else {
                        holder.followButton.setVisibility(View.VISIBLE);
                        holder.followButton.setText(context.getString(R.string.follow));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };

        followStatusRef.add(holder.followRef);
        followStatusListener.add(holder.eventListener);

        holder.followRef.addValueEventListener(holder.eventListener);
    }

    private void setFollowButtonAction(UserHolder holder) {
        holder.followButton.setOnClickListener(v -> {
            ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(context.getString(R.string.please_wait));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            long t = System.currentTimeMillis();
            if (holder.followButton.getText().toString().equals(context.getString(R.string.follow))) {
                followRef.child(userId).child(AwesumApp.DB_FOLLOWING).child(holder.id).setValue(t).addOnCompleteListener(task ->
                        followRef.child(holder.id).child(AwesumApp.DB_FOLLOWER).child(userId).setValue(t).addOnCompleteListener(task12 -> {
                            holder.followButton.setText(context.getString(R.string.following));
                            String contentCombine = Notification.FOLLOW_TYPE + userId + holder.id;
                            Notification notification = new Notification(Notification.FOLLOW_TYPE, t, userId, Notification.FOLLOW_TYPE_CONTENT, "null", false, false, contentCombine);
                            holder.notiRef.child(String.valueOf(t)).setValue(notification);
                            new Handler().postDelayed(() -> progressDialog.dismiss(), 500);

                        }));

            } else if (holder.followButton.getText().toString().equals(context.getString(R.string.following))) {
                followRef.child(userId).child(AwesumApp.DB_FOLLOWING).child(holder.id).removeValue().addOnCompleteListener(task ->
                        followRef.child(holder.id).child(AwesumApp.DB_FOLLOWER).child(userId).removeValue().addOnCompleteListener(task1 -> {
                            holder.followButton.setText(context.getString(R.string.follow));
                            String contentCombine = Notification.FOLLOW_TYPE + userId + holder.id;
                            Query query = holder.notiRef.orderByChild("contentCombine").equalTo(contentCombine);
                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                                        data.getRef().removeValue();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }));
                new Handler().postDelayed(() -> progressDialog.dismiss(), 500);

            }
        });
    }

    private void setItemClick(UserHolder holder) {
        holder.item.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("userId", holder.id);
            context.startActivity(intent);
        });
    }

    public List<DatabaseReference> getFollowStatusRef() {
        return followStatusRef;
    }

    public List<ValueEventListener> getFollowStatusListener() {
        return followStatusListener;
    }

    static class UserHolder extends RecyclerView.ViewHolder {

        private ImageView profileImage;
        private TextView username;
        private TextView fullname;
        private Button followButton;
        private LinearLayout item;
        private DatabaseReference notiRef;
        private ValueEventListener eventListener;
        private DatabaseReference followRef;

        private String id;

        UserHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image_search);
            username = itemView.findViewById(R.id.username_search);
            fullname = itemView.findViewById(R.id.fullname_search);
            followButton = itemView.findViewById(R.id.follow_button);
            item = itemView.findViewById(R.id.item_search_user);
        }
    }

}
