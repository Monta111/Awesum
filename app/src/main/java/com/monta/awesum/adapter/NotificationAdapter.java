package com.monta.awesum.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.database.ValueEventListener;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.activity.CommentActivity;
import com.monta.awesum.activity.ProfileActivity;
import com.monta.awesum.activity.SinglePostActivity;
import com.monta.awesum.model.Notification;
import com.monta.awesum.model.Post;
import com.monta.awesum.model.User;
import com.monta.awesum.ultility.Ultility;

import java.util.List;


public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationHolder> {

    private Context context;
    private List<Notification> notificationList;

    public NotificationAdapter(Context context, List<Notification> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View itemView = layoutInflater.inflate(R.layout.item_notification, parent, false);
        return new NotificationHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationHolder holder, int position) {
        Notification notification = notificationList.get(position);

        setItemBackground(notification, holder);
        setUserInfo(holder, notification);

        if (notification.getContent().equals(Notification.FOLLOW_TYPE_CONTENT))
            holder.content.setText(context.getString(R.string.follow_you));
        else if (notification.getContent().equals(Notification.LIKE_TYPE_CONTENT))
            holder.content.setText(context.getString(R.string.like_your_post));
        else {
            String[] temp = notification.getContent().split(": ");
            String s = context.getString(R.string.commented_on_your_post) + " " + temp[1];
            holder.content.setText(s);
        }

        setContentImage(notification, holder);
        setItemClickAction(holder, notification);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    private void setItemBackground(Notification notification, NotificationHolder holder) {
        if (notification.isSeen()) {
            Ultility.setSelectedBackground(holder.notificationLayout, context);
        } else
            holder.notificationLayout.setBackgroundColor(context.getResources().getColor(R.color.colorGrey));
    }

    private void setUserInfo(NotificationHolder holder, Notification notification) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER).child(notification.getUserId());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    holder.username.setText(user.getUsername());
                    Glide.with(context).load(user.getAvatarUrl()).placeholder(R.drawable.defaultavatar).circleCrop().into(holder.profileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setContentImage(Notification notification, NotificationHolder holder) {
        String contentId = notification.getContentId();
        if (!contentId.equals("null")) {
            FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POST).child(contentId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if ((long) dataSnapshot.child("type").getValue() == Post.IMAGE_TYPE_ITEM) {
                                if (dataSnapshot.child("url").getChildrenCount() == 1) {
                                    Glide.with(context).load(dataSnapshot.child("url").child("0").getValue()).into(holder.contentImage);
                                } else {
                                    for (DataSnapshot data : dataSnapshot.child("url").getChildren()) {
                                        Glide.with(context).load(data.getValue()).into(holder.contentImage);
                                        break;
                                    }
                                }

                            } else {
                                Glide.with(context).load(dataSnapshot.child("thumbUrl").getValue()).into(holder.contentImage);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
            FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POST).child(contentId).child("url")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            holder.contentImage.setVisibility(View.VISIBLE);
                            for (DataSnapshot data : dataSnapshot.getChildren()) {
                                Glide.with(context).load(data.getValue()).placeholder(R.drawable.ic_image).into(holder.contentImage);
                                break;
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        } else
            holder.contentImage.setVisibility(View.INVISIBLE);
    }

    private void setItemClickAction(NotificationHolder holder, Notification notification) {
        holder.notificationLayout.setOnClickListener(v -> {
            if (!notification.isSeen()) {
                Ultility.setSelectedBackground(holder.notificationLayout, context);
                FirebaseDatabase.getInstance().getReference(AwesumApp.DB_NOTIFICATION)
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child(String.valueOf(notification.getNotificationId()))
                        .child("seen")
                        .setValue(true);
            }
            switch (notification.getType()) {
                case Notification.FOLLOW_TYPE: {
                    String followerId = notification.getUserId();
                    Intent intent = new Intent(context, ProfileActivity.class);
                    intent.putExtra("userId", followerId);
                    context.startActivity(intent);
                    break;
                }
                case Notification.LIKE_TYPE: {
                    String postId = notification.getContentId();
                    Intent intent = new Intent(context, SinglePostActivity.class);
                    intent.putExtra("postId", postId);
                    intent.putExtra("username", holder.username.getText().toString());
                    context.startActivity(intent);
                    break;
                }
                case Notification.COMMENT_TYPE: {
                    String postId = notification.getContentId();
                    Intent intent = new Intent(context, CommentActivity.class);
                    intent.putExtra("postId", postId);
                    intent.putExtra("publisherId", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    context.startActivity(intent);
                    break;
                }
            }
        });
    }

    static class NotificationHolder extends RecyclerView.ViewHolder {

        private LinearLayout notificationLayout;
        private ImageView profileImage;
        private TextView username;
        private TextView content;
        private ImageView contentImage;

        NotificationHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image_notification);
            username = itemView.findViewById(R.id.username_notification);
            content = itemView.findViewById(R.id.content);
            contentImage = itemView.findViewById(R.id.content_image);
            notificationLayout = itemView.findViewById(R.id.notification);
        }
    }
}
