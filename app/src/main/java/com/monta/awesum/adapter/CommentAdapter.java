package com.monta.awesum.adapter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.activity.CommentActivity;
import com.monta.awesum.activity.LikeActivity;
import com.monta.awesum.activity.ProfileActivity;
import com.monta.awesum.dialogfragment.CommentOption;
import com.monta.awesum.model.Comment;
import com.monta.awesum.model.Notification;
import com.monta.awesum.model.User;
import com.monta.awesum.ultility.Ultility;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentHolder> {
    private Context context;
    private List<Comment> commentList;
    private String userId;
    private String postId;
    private String publisherId;
    private boolean isFullFeature;
    private DatabaseReference commentRef;

    public CommentAdapter(Context context, List<Comment> commentList, boolean isFullFeature) {
        this.context = context;
        this.commentList = commentList;
        this.isFullFeature = isFullFeature;
    }

    @NonNull
    @Override
    public CommentHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View itemView = layoutInflater.inflate(R.layout.item_comment, parent, false);
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return new CommentHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.comment = comment;

        setInfo(holder, comment);
        setTimeStamp(holder, comment);
        setCommentLikeAction(holder, comment);
        setLikeStatus(holder, comment);
        setNumberOfLikes(holder, comment);
        if (isFullFeature) {
            setNumberOfLikesClick(holder, comment);
            setCommentClickAction(holder, comment);
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    private void setInfo(CommentHolder holder, Comment comment) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER).child(comment.getPublisherId());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    holder.username = user.getUsername();
                    String full = user.getUsername() + " " + comment.getComment();

                    String shortCmt = full;
                    char[] a = full.toCharArray();
                    for (int i = 80; i < a.length; ++i)
                        if (a[i] == ' ') {
                            shortCmt = full.substring(0, i);
                            break;
                        }

                    String s = context.getString(R.string.see_more);

                    SpannableString fullComment = new SpannableString(full);
                    fullComment.setSpan(new StyleSpan(Typeface.BOLD), 0, user.getUsername().length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

                    SpannableString shortComment = new SpannableString(shortCmt + s);
                    shortComment.setSpan(new StyleSpan(Typeface.BOLD), 0, user.getUsername().length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

                    ClickableSpan clickableSpan = new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            holder.usernameWithComment.setText(fullComment);
                        }

                        @Override
                        public void updateDrawState(@NonNull TextPaint ds) {
                            super.updateDrawState(ds);
                            ds.setUnderlineText(false);
                            ds.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                        }
                    };

                    shortComment.setSpan(clickableSpan, shortCmt.length(), shortCmt.length() + s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    if (fullComment.length() < 80)
                        holder.usernameWithComment.setText(fullComment);
                    else
                        holder.usernameWithComment.setText(shortComment);
                    holder.usernameWithComment.setMovementMethod(LinkMovementMethod.getInstance());

                    Glide.with(context).load(user.getAvatarUrl()).placeholder(R.drawable.defaultavatar).circleCrop().into(holder.profileImage);
                    holder.profileImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(context, ProfileActivity.class);
                            intent.putExtra("userId", comment.getPublisherId());
                            context.startActivity(intent);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setTimeStamp(CommentHolder holder, Comment comment) {
        long currentTimeStamp = System.currentTimeMillis();
        long deltaMiliSec = currentTimeStamp - comment.getId();
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

    private void setLikeStatus(CommentHolder holder, Comment comment) {
        commentRef.child(String.valueOf(comment.getId())).child(AwesumApp.DB_LIKE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(userId)) {
                            holder.isLike = true;
                            holder.like.setImageDrawable(context.getDrawable(R.drawable.ic_liked));
                        } else {
                            holder.isLike = false;
                            holder.like.setImageDrawable(context.getDrawable(R.drawable.ic_love));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void setNumberOfLikes(CommentHolder holder, Comment comment) {
        commentRef.child(String.valueOf(comment.getId())).child(AwesumApp.DB_LIKE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        holder.numberOfLike.setText(context.getString(R.string.number_of_likes, dataSnapshot.getChildrenCount()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void setCommentLikeAction(CommentHolder holder, Comment comment) {
        holder.like.setOnClickListener(v -> {
            Ultility.hideKeyboard(context);
            if (holder.isLike) {
                commentRef.child(String.valueOf(comment.getId())).child(AwesumApp.DB_LIKE)
                        .child(userId).removeValue().addOnCompleteListener(task -> {
                    holder.isLike = false;
                    holder.like.setImageDrawable(context.getDrawable(R.drawable.ic_love));
                    setNumberOfLikes(holder, comment);
                });
            } else {
                commentRef.child(String.valueOf(comment.getId())).child(AwesumApp.DB_LIKE)
                        .child(userId).setValue(System.currentTimeMillis()).addOnCompleteListener(task -> {
                    holder.isLike = true;
                    holder.like.setImageDrawable(context.getDrawable(R.drawable.ic_liked));
                    setNumberOfLikes(holder, comment);
                });
            }
        });
    }

    private void setNumberOfLikesClick(CommentHolder holder, Comment comment) {
        Ultility.setSelectedBackground(holder.numberOfLike, context);
        holder.numberOfLike.setOnClickListener(v -> {
            Intent intent = new Intent(context, LikeActivity.class);
            intent.putExtra("postId", postId);
            intent.putExtra("commentId", String.valueOf(comment.getId()));
            context.startActivity(intent);
        });
    }

    private void setCommentClickAction(CommentHolder holder, Comment comment) {
        Ultility.setSelectedBackground(holder.item, context);

        holder.item.setOnLongClickListener(v -> {
            Ultility.hideKeyboard(context);
            showOptionDialog(holder, comment);
            return true;
        });
    }

    private void showOptionDialog(CommentHolder holder, Comment comment) {
        CommentOption optionDialog = new CommentOption();
        optionDialog.setInfo(userId, publisherId, comment.getPublisherId());
        optionDialog.setListener(holder);
        AppCompatActivity activity = (AppCompatActivity) context;
        optionDialog.show(activity.getSupportFragmentManager(), "optionCommentDialog");
    }

    private void removeOptionDialog(AppCompatActivity activity) {
        BottomSheetDialogFragment f = (BottomSheetDialogFragment) activity.getSupportFragmentManager().findFragmentByTag("optionCommentDialog");
        if (f != null)
            f.dismiss();
    }

    public void setInfo(String postId, String publisherId) {
        this.postId = postId;
        this.publisherId = publisherId;
        commentRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POST)
                .child(postId).child(AwesumApp.DB_COMMENT);
    }


    class CommentHolder extends RecyclerView.ViewHolder implements CommentOption.ItemOptionCommentClickListener {

        private ImageView profileImage;
        private TextView usernameWithComment;
        private TextView timestamp;
        private ImageView like;
        private TextView numberOfLike;
        private LinearLayout item;
        private boolean isLike;

        private Comment comment;
        private String username;

        CommentHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image_comment);
            usernameWithComment = itemView.findViewById(R.id.username_comment);
            timestamp = itemView.findViewById(R.id.timestamp_comment);
            like = itemView.findViewById(R.id.like_comment);
            numberOfLike = itemView.findViewById(R.id.number_of_likes);
            item = itemView.findViewById(R.id.item_comment);
        }

        @Override
        public void editComment() {
            AppCompatActivity activity = (AppCompatActivity) context;
            removeOptionDialog(activity);
            if (activity instanceof CommentActivity) {
                CommentActivity commentActivity = (CommentActivity) activity;
                LinearLayout layout = activity.findViewById(R.id.comment_activity);
                ImageView editDone = commentActivity.findViewById(R.id.edit_done);
                ImageView cancelEdit = commentActivity.findViewById(R.id.cancel_edit);
                ImageView send = commentActivity.findViewById(R.id.send_comment);
                EditText content = commentActivity.findViewById(R.id.current_user_comment);

                layout.setBackground(context.getResources().getDrawable(R.color.colorAccent));
                content.setText(comment.getComment());

                send.setVisibility(View.GONE);
                editDone.setVisibility(View.VISIBLE);
                cancelEdit.setVisibility(View.VISIBLE);
                content.requestFocus();
                Ultility.showKeyboard(context);

                editDone.setOnClickListener(v -> {

                    ProgressDialog progressDialog = new ProgressDialog(context);
                    progressDialog.setMessage(context.getString(R.string.please_wait));
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.show();

                    String s = content.getText().toString().trim();
                    if (!TextUtils.isEmpty(s)) {
                        FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POST)
                                .child(postId).child(AwesumApp.DB_COMMENT).child(String.valueOf(comment.getId()))
                                .child("comment")
                                .setValue(s);
                        FirebaseDatabase.getInstance().getReference(AwesumApp.DB_NOTIFICATION)
                                .child(publisherId)
                                .child(String.valueOf(comment.getId()))
                                .child("content")
                                .setValue(Notification.COMMENT_TYPE_CONTENT + s);

                        String full = username + " " + s;
                        SpannableString fullComment = new SpannableString(full);
                        fullComment.setSpan(new StyleSpan(Typeface.BOLD), 0, username.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                        usernameWithComment.setText(fullComment);

                        progressDialog.dismiss();
                        send.setVisibility(View.VISIBLE);
                        editDone.setVisibility(View.GONE);
                        cancelEdit.setVisibility(View.GONE);
                        layout.setBackground(context.getResources().getDrawable(R.color.colorWhite));
                        Ultility.hideKeyboard(context);
                    } else
                        content.setError("Required");
                });

                cancelEdit.setOnClickListener(v -> {
                    content.setText("");
                    send.setVisibility(View.VISIBLE);
                    editDone.setVisibility(View.GONE);
                    cancelEdit.setVisibility(View.GONE);
                    layout.setBackground(context.getResources().getDrawable(R.color.colorWhite));
                    Ultility.hideKeyboard(context);
                });
            }

        }

        @Override
        public void deleteComment() {
            FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POST).child(postId)
                    .child(AwesumApp.DB_COMMENT).child(String.valueOf(comment.getId())).removeValue();
            FirebaseDatabase.getInstance().getReference(AwesumApp.DB_NOTIFICATION).child(publisherId)
                    .child(String.valueOf(comment.getId())).removeValue();
            commentList.remove(getAdapterPosition());
            notifyItemRemoved(getAdapterPosition());
            notifyItemRangeChanged(getAdapterPosition(), commentList.size());
        }
    }
}
