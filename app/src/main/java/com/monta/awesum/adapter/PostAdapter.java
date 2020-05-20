package com.monta.awesum.adapter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.activity.CommentActivity;
import com.monta.awesum.activity.LikeActivity;
import com.monta.awesum.activity.MainActivity;
import com.monta.awesum.activity.ProfileActivity;
import com.monta.awesum.activity.SinglePostActivity;
import com.monta.awesum.activity.VideoActivity;
import com.monta.awesum.dialogfragment.PostOption;
import com.monta.awesum.model.Comment;
import com.monta.awesum.model.Notification;
import com.monta.awesum.model.Post;
import com.monta.awesum.model.User;
import com.monta.awesum.ultility.CacheDataSourceFactory;
import com.monta.awesum.ultility.Ultility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostHolder> {
    private Context context;
    private List<String> idPostList;
    private String userId;
    private DatabaseReference postRef;

    private List<Player> players;
    private CacheDataSourceFactory cacheDataSourceFactory;
    private DefaultLoadControl loadControl;

    private List<Query> followStatusRef;
    private List<Query> hideStatusRef;
    private List<Query> newestCommentRef;
    private List<ValueEventListener> followListener;
    private List<ValueEventListener> hideListener;
    private List<ValueEventListener> newestCommentListener;

    public PostAdapter(Context context, List<String> idPostList) {
        this.context = context;
        this.idPostList = idPostList;
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        postRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POST);

        loadControl = new DefaultLoadControl.Builder().setBufferDurationsMs(2000, 5000, 1500, 2000)
                .createDefaultLoadControl();
        cacheDataSourceFactory = new CacheDataSourceFactory(context, 10 * 2014 * 2014);

        players = new ArrayList<>();
        followStatusRef = new ArrayList<>();
        hideStatusRef = new ArrayList<>();
        newestCommentRef = new ArrayList<>();
        followListener = new ArrayList<>();
        hideListener = new ArrayList<>();
        newestCommentListener = new ArrayList<>();
    }

    @NonNull
    @Override
    public PostHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View itemView = layoutInflater.inflate(R.layout.item_post, parent, false);
        return new PostHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PostHolder holder, int position) {
        holder.postId = idPostList.get(position);

        setHidePostStatus(holder);

        postRef.child(holder.postId).keepSynced(true);
        postRef.child(holder.postId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Post post = dataSnapshot.getValue(Post.class);
                if (post != null) {
                    holder.publisherId = post.getPublisherId();
                    holder.notificationRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_NOTIFICATION).child(holder.publisherId);
                    holder.notificationRef.keepSynced(true);

                    if (post.getType() == Post.IMAGE_TYPE_ITEM) {
                        holder.video.setVisibility(View.GONE);
                        holder.layoutSlider.setVisibility(View.VISIBLE);
                        setImageSlider(holder);
                    } else {
                        holder.layoutSlider.setVisibility(View.GONE);
                        holder.video.setVisibility(View.VISIBLE);
                        holder.indicator.setVisibility(View.GONE);
                        setVideo(holder);
                    }

                    setFollowStatus(holder);
                    setUserInformation(holder);
                    holder.description.setText(post.getDescription());
                    setViewAllLikes(holder);
                    setLikeButtonStatus(holder);
                    setNumberOfLikes(holder);
                    setLikeButtonAction(holder);
                    setCommentButtonAction(holder);
                    setTimeStamp(holder);
                    setNumberOfComment(holder);
                    displayNewestComment(holder);
                    setSaveButtonStatus(holder);
                    setSaveButtonAction(holder);
                    setUsernameClick(holder);
                    setOptionClickAction(holder);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        setItemClickListener(holder);
    }

    @Override
    public void onViewRecycled(@NonNull PostHolder holder) {
        if (holder.queryComment != null) {
            holder.queryComment.removeEventListener(holder.listenerComment);

            newestCommentRef.remove(holder.queryComment);
            newestCommentListener.remove(holder.listenerComment);
        }
        if (holder.queryFollowStatus != null) {
            holder.queryFollowStatus.removeEventListener(holder.listenerFollowStatus);

            followStatusRef.remove(holder.queryFollowStatus);
            followListener.remove(holder.listenerFollowStatus);
        }
        if (holder.queryHideStatus != null) {
            holder.queryHideStatus.removeEventListener(holder.listenerHideStatus);

            hideStatusRef.remove(holder.queryHideStatus);
            hideListener.remove(holder.listenerHideStatus);
        }
        if (holder.player != null) {
            holder.player.release();
            players.remove(holder.player);
        }
    }

    public List<Player> getPlayers() {
        return players;
    }

    public List<Query> getFollowStatusRef() {
        return followStatusRef;
    }

    public List<Query> getHideStatusRef() {
        return hideStatusRef;
    }

    public List<Query> getNewestCommentRef() {
        return newestCommentRef;
    }

    public List<ValueEventListener> getFollowListener() {
        return followListener;
    }

    public List<ValueEventListener> getHideListener() {
        return hideListener;
    }

    public List<ValueEventListener> getNewestCommentListener() {
        return newestCommentListener;
    }

    private void setUserInformation(PostHolder holder) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER).child(holder.publisherId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    Glide.with(context).load(user.getAvatarUrl()).placeholder(R.drawable.defaultavatar).circleCrop().into(holder.profileImage);
                    holder.username.setText(user.getUsername());
                    holder.publisher.setText(user.getUsername());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setVideo(PostHolder holder) {
        int[] displayMetric = Ultility.getDisplayMetric((AppCompatActivity) context);
        holder.video.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, displayMetric[1]));
        postRef.child(holder.postId).child("url").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Uri videoUri = Uri.parse((String) dataSnapshot.getValue());

                holder.player = new SimpleExoPlayer.Builder(context).setLoadControl(loadControl).build();
                players.add(holder.player);


                MediaSource mediaSource = new ExtractorMediaSource(videoUri,
                        cacheDataSourceFactory, new DefaultExtractorsFactory(), null, null);

                holder.player.prepare(mediaSource);
                holder.video.setPlayer(holder.player);
                holder.video.setControllerAutoShow(false);
                holder.video.setControllerHideOnTouch(true);

                ImageView fullScreen = holder.video.findViewById(R.id.exo_fullscreen_icon);
                fullScreen.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        holder.player.setPlayWhenReady(false);
                        Intent intent = new Intent(context, VideoActivity.class);
                        intent.putExtra("uri", videoUri.toString());
                        intent.putExtra("position", holder.player.getCurrentPosition());
                        context.startActivity(intent);
                    }
                });
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setImageSlider(PostHolder holder) {
        int[] displayMetric = Ultility.getDisplayMetric((AppCompatActivity) context);
        holder.layoutSlider.setLayoutParams(new LinearLayout.LayoutParams(displayMetric[1], displayMetric[1]));

        postRef.child(holder.postId).child("url").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                List<String> urlList = new ArrayList<>();
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    urlList.add((String) data.getValue());
                }
                holder.slider.setAdapter(new SliderImageAdapter(context, urlList));
                if (urlList.size() > 1) {
                    holder.indicator.setVisibility(View.VISIBLE);
                    holder.indicator.setupWithViewPager(holder.slider, true);
                } else
                    holder.indicator.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setLikeButtonStatus(PostHolder holder) {
        DatabaseReference likeRef = postRef.child(holder.postId).child(AwesumApp.DB_LIKE);
        likeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(userId)) {
                    holder.isLike = true;
                    holder.likeButton.setImageResource(R.drawable.ic_liked);
                } else {
                    holder.isLike = false;
                    holder.likeButton.setImageResource(R.drawable.ic_love);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setNumberOfLikes(PostHolder holder) {
        DatabaseReference likeRef = postRef.child(holder.postId).child(AwesumApp.DB_LIKE);
        likeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int numberOfLikes = (int) dataSnapshot.getChildrenCount();
                holder.numberOfLikes.setText(context.getString(R.string.number_of_likes, numberOfLikes));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setLikeButtonAction(PostHolder holder) {
        holder.likeButton.setOnClickListener(v -> {
            if (!holder.isLike) {
                holder.likeButton.setImageResource(R.drawable.ic_liked);
                long t = System.currentTimeMillis();
                postRef.child(holder.postId).child(AwesumApp.DB_LIKE).child(userId).setValue(t);
                if (!userId.equals(holder.publisherId)) {
                    String contentCombine = Notification.LIKE_TYPE + holder.postId + userId;
                    Notification notification = new Notification(Notification.LIKE_TYPE, t, userId, Notification.LIKE_TYPE_CONTENT, holder.postId, false, false, contentCombine);
                    holder.notificationRef.child(String.valueOf(t)).setValue(notification);
                }
                setNumberOfLikes(holder);
                holder.isLike = true;
            } else {
                holder.likeButton.setImageResource(R.drawable.ic_love);
                postRef.child(holder.postId).child(AwesumApp.DB_LIKE).child(userId).removeValue();
                String contentCombine = Notification.LIKE_TYPE + holder.postId + userId;
                Query query = holder.notificationRef.orderByChild("contentCombine").equalTo(contentCombine);
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

                setNumberOfLikes(holder);
                holder.isLike = false;
            }
        });
    }

    private void setViewAllLikes(PostHolder holder) {
        holder.numberOfLikes.setOnClickListener(v -> {
            Intent intent = new Intent(context, LikeActivity.class);
            intent.putExtra("postId", holder.postId);
            context.startActivity(intent);
        });
    }

    private void setCommentButtonAction(PostHolder holder) {
        holder.commentButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommentActivity.class);
            intent.putExtra("postId", holder.postId);
            intent.putExtra("publisherId", holder.publisherId);
            context.startActivity(intent);
        });
    }

    private void displayNewestComment(PostHolder holder) {
        holder.queryComment = postRef.child(holder.postId).child(AwesumApp.DB_COMMENT).limitToLast(2);
        if (holder.listenerComment == null)
            holder.listenerComment = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<Comment> list = new ArrayList<>();
                    CommentAdapter commentAdapter = new CommentAdapter(context, list, false);
                    commentAdapter.setInfo(holder.postId, holder.publisherId);
                    holder.newestComment.setAdapter(commentAdapter);
                    holder.newestComment.setLayoutManager(new LinearLayoutManager(context));
                    if (dataSnapshot.getValue() != null) {
                        for (DataSnapshot data : dataSnapshot.getChildren())
                            list.add(data.getValue(Comment.class));
                        Collections.reverse(list);
                        commentAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
        holder.queryComment.addValueEventListener(holder.listenerComment);

        newestCommentRef.add(holder.queryComment);
        newestCommentListener.add(holder.listenerComment);
    }

    private void setNumberOfComment(PostHolder holder) {
        holder.viewAllComment.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommentActivity.class);
            intent.putExtra("postId", holder.postId);
            intent.putExtra("publisherId", holder.publisherId);
            context.startActivity(intent);
        });

        postRef.child(holder.postId).child(AwesumApp.DB_COMMENT)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        holder.viewAllComment.setVisibility(View.GONE);
                        if (dataSnapshot.getChildrenCount() > 2) {
                            holder.viewAllComment.setVisibility(View.VISIBLE);
                            holder.viewAllComment.setText(context.getString(R.string.all_comment, dataSnapshot.getChildrenCount()));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void setTimeStamp(PostHolder holder) {
        long t = Long.parseLong(holder.postId);
        long currentTimeStamp = System.currentTimeMillis();
        long deltaMiliSec = currentTimeStamp - t;
        int deltaMinute = Math.round((float) deltaMiliSec / (60 * 1000));
        int deltaHour = Math.round((float) deltaMiliSec / (60 * 60 * 1000));
        int deltaDay = Math.round((float) deltaMiliSec / (24 * 60 * 60 * 1000));
        if (deltaDay > 0)
            holder.timestamp.setText(context.getString(R.string.days_ago, deltaDay));
        else if (deltaDay == 0 && deltaHour > 0)
            holder.timestamp.setText(context.getString(R.string.hours_ago, deltaHour));
        else
            holder.timestamp.setText(context.getString(R.string.minutes_ago, deltaMinute));
    }

    private void setSaveButtonStatus(PostHolder holder) {
        DatabaseReference saveRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_SAVE)
                .child(userId);
        saveRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(holder.postId)) {
                    holder.isSave = true;
                    holder.saveButton.setImageResource(R.drawable.ic_saved);
                } else {
                    holder.isSave = false;
                    holder.saveButton.setImageResource(R.drawable.ic_save);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setSaveButtonAction(PostHolder holder) {
        holder.saveButton.setOnClickListener(v -> {
            if (holder.isSave) {
                FirebaseDatabase.getInstance().getReference(AwesumApp.DB_SAVE)
                        .child(userId).child(holder.postId).removeValue()
                        .addOnCompleteListener(task -> {
                            holder.saveButton.setImageResource(R.drawable.ic_save);
                            holder.isSave = false;
                        });
            } else {
                FirebaseDatabase.getInstance().getReference(AwesumApp.DB_SAVE)
                        .child(userId).child(holder.postId).setValue(true)
                        .addOnCompleteListener(task -> {
                            holder.saveButton.setImageResource(R.drawable.ic_saved);
                            holder.isSave = true;
                        });
            }
        });
    }

    private void setUsernameClick(PostHolder holder) {
        holder.username.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("userId", holder.publisherId);
            context.startActivity(intent);
        });
        holder.publisher.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("userId", holder.publisherId);
            context.startActivity(intent);
        });
    }

    private void setHidePostStatus(PostHolder holder) {
        holder.queryHideStatus = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POSTMAIN).child(userId).child(holder.postId);
        if (holder.listenerHideStatus == null)
            holder.listenerHideStatus = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null)
                        holder.isHide = !((boolean) dataSnapshot.getValue());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
        holder.queryHideStatus.addValueEventListener(holder.listenerHideStatus);

        hideStatusRef.add(holder.queryHideStatus);
        hideListener.add(holder.listenerHideStatus);
    }

    private void setFollowStatus(PostHolder holder) {
        holder.queryFollowStatus = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_FOLLOW).child(userId).child(AwesumApp.DB_FOLLOWING);
        if (holder.listenerFollowStatus == null)
            holder.listenerFollowStatus = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    holder.isFollow = dataSnapshot.hasChild(holder.publisherId);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };


        if (userId.equals(holder.publisherId))
            holder.isFollow = true;
        else {
            holder.queryFollowStatus.addValueEventListener(holder.listenerFollowStatus);

            followStatusRef.add(holder.queryFollowStatus);
            followListener.add(holder.listenerFollowStatus);
        }
    }

    private void setItemClickListener(PostHolder holder) {
        holder.itemPost.setOnLongClickListener(v -> {
            showOptionDialog(holder);
            return true;
        });

        holder.itemPost.setOnClickListener(v -> {
            AppCompatActivity activity = (AppCompatActivity) context;
            if (activity instanceof MainActivity) {
                Intent intent = new Intent(context, SinglePostActivity.class);
                intent.putExtra("postId", holder.postId);
                intent.putExtra("username", holder.username.getText().toString());
                context.startActivity(intent);
            }
        });
    }

    private void showOptionDialog(PostHolder holder) {
        PostOption optionDialog = new PostOption();
        optionDialog.setInfo(holder.publisherId, userId, holder.isHide, holder.isFollow, holder.username.getText().toString());
        optionDialog.setListener(holder);
        AppCompatActivity activity = (AppCompatActivity) context;
        optionDialog.show(activity.getSupportFragmentManager(), "optionDialog");
    }

    private void setOptionClickAction(PostHolder holder) {
        holder.option.setOnClickListener(v -> showOptionDialog(holder));
    }

    private void removeOptionDialog() {
        AppCompatActivity activity = (AppCompatActivity) context;
        BottomSheetDialogFragment f = (BottomSheetDialogFragment) activity.getSupportFragmentManager().findFragmentByTag("optionDialog");
        if (f != null)
            f.dismiss();
    }

    @Override
    public int getItemCount() {
        return idPostList.size();
    }

    class PostHolder extends RecyclerView.ViewHolder implements PostOption.ItemOptionPostClickListener {
        private LinearLayout itemPost;
        private ImageView profileImage;
        private TextView username;
        private TextView publisher;
        private TextView description;
        private TextView numberOfLikes;
        private ImageView likeButton;
        private ImageView commentButton;
        private TextView timestamp;
        private ImageView saveButton;
        private RecyclerView newestComment;
        private TextView viewAllComment;
        private ViewPager slider;
        private TabLayout indicator;
        private RelativeLayout layoutSlider;
        private PlayerView video;
        private boolean isLike;
        private boolean isSave;
        private boolean isHide;
        private boolean isFollow;
        private ImageView option;
        private String publisherId;
        private String postId;
        private DatabaseReference notificationRef;

        private Query queryComment;
        private DatabaseReference queryFollowStatus;
        private DatabaseReference queryHideStatus;
        private ValueEventListener listenerComment;
        private ValueEventListener listenerFollowStatus;
        private ValueEventListener listenerHideStatus;

        private SimpleExoPlayer player;

        PostHolder(@NonNull View itemView) {
            super(itemView);
            itemPost = itemView.findViewById(R.id.item_post);
            profileImage = itemView.findViewById(R.id.profile_image_post);
            username = itemView.findViewById(R.id.username_post);
            publisher = itemView.findViewById(R.id.publisher);
            description = itemView.findViewById(R.id.description);
            numberOfLikes = itemView.findViewById(R.id.number_of_likes);
            likeButton = itemView.findViewById(R.id.like_button);
            commentButton = itemView.findViewById(R.id.comment_button);
            timestamp = itemView.findViewById(R.id.timestamp_post);
            saveButton = itemView.findViewById(R.id.save_button);
            option = itemView.findViewById(R.id.post_option);
            newestComment = itemView.findViewById(R.id.newest_comment);
            viewAllComment = itemView.findViewById(R.id.view_all_comment);
            slider = itemView.findViewById(R.id.slider);
            indicator = itemView.findViewById(R.id.indicator);
            layoutSlider = itemView.findViewById(R.id.layout_slider);
            video = itemView.findViewById(R.id.video);
        }

        @Override
        public void setHidePostAction() {
            if (isHide) {
                FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POSTMAIN).child(userId).child(postId).setValue(true);
                removeOptionDialog();
            } else {
                FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POSTMAIN).child(userId).child(postId).setValue(false);
                AppCompatActivity activity = (AppCompatActivity) context;
                if (activity instanceof MainActivity) {
                    idPostList.remove(getAdapterPosition());
                    notifyItemRemoved(getAdapterPosition());
                    notifyItemRangeChanged(getAdapterPosition(), idPostList.size());
                }
                removeOptionDialog();
            }
        }

        @Override
        public void setFollowAction() {
            ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(context.getString(R.string.please_wait));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            long t = System.currentTimeMillis();
            DatabaseReference followRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_FOLLOW);
            if (!isFollow) {
                followRef.child(userId).child(AwesumApp.DB_FOLLOWING).child(publisherId).setValue(t).addOnCompleteListener(task ->
                        followRef.child(publisherId).child(AwesumApp.DB_FOLLOWER).child(userId).setValue(t).addOnCompleteListener(task12 -> {
                            String contentCombine = Notification.FOLLOW_TYPE + userId + publisherId;
                            Notification notification = new Notification(Notification.FOLLOW_TYPE, t, userId, Notification.FOLLOW_TYPE_CONTENT, "null", false, false, contentCombine);
                            notificationRef.child(String.valueOf(t)).setValue(notification);
                        }));
                removeOptionDialog();
                progressDialog.dismiss();
            } else {
                followRef.child(userId).child(AwesumApp.DB_FOLLOWING).child(publisherId).removeValue().addOnCompleteListener(task ->
                        followRef.child(publisherId).child(AwesumApp.DB_FOLLOWER).child(userId).removeValue().addOnCompleteListener(task1 -> {
                            String contentCombine = Notification.FOLLOW_TYPE + userId + publisherId;
                            Query query = notificationRef.orderByChild("contentCombine").equalTo(contentCombine);
                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                                        data.getRef().removeValue();
                                        progressDialog.dismiss();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }));
                removeOptionDialog();
            }
        }

        @Override
        public void setDeletePostAction() {
            postRef.child(postId).removeValue();
            FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER).child(userId).child(AwesumApp.DB_POST)
                    .child(postId).removeValue();
            FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POSTMAIN).child(userId).child(postId).removeValue();
            AppCompatActivity activity = (AppCompatActivity) context;
            if (activity instanceof MainActivity) {
                idPostList.remove(getAdapterPosition());
                notifyItemRemoved(getAdapterPosition());
                notifyDataSetChanged();
            } else {
                activity.finish();
            }
            removeOptionDialog();
        }
    }

}
