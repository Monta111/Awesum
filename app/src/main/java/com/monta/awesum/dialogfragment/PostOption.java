package com.monta.awesum.dialogfragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.monta.awesum.R;

public class PostOption extends BottomSheetDialogFragment {

    private String publisherId;
    private String userId;
    private boolean isHide;
    private boolean isFollow;
    private String username;
    private ItemOptionPostClickListener listener;

    private TextView hidePost;
    private TextView follow;
    private TextView deletePost;


    public void setListener(ItemOptionPostClickListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_option, container, false);
        hidePost = view.findViewById(R.id.option_1);
        follow = view.findViewById(R.id.option_2);
        deletePost = view.findViewById(R.id.option_3);

        if (isHide)
            hidePost.setText(view.getContext().getString(R.string.unhide_post));
        else
            hidePost.setText(view.getContext().getString(R.string.hide_post));

        if (!isFollow) {
            follow.setText(view.getContext().getString(R.string.follow));
            hidePost.setVisibility(View.GONE);
        } else
            follow.setText(view.getContext().getString(R.string.unfollow));

        setHidePost();

        if (userId.equals(publisherId)) {
            follow.setVisibility(View.GONE);
            deletePost.setVisibility(View.VISIBLE);
            setDeletePost();
        } else {
            follow.setVisibility(View.VISIBLE);
            deletePost.setVisibility(View.GONE);
            setFollow();
        }

        return view;
    }

    private void setHidePost() {
        hidePost.setOnClickListener(v -> {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            View confirmView = layoutInflater.inflate(R.layout.dialog_confirmation, null);
            TextView title = confirmView.findViewById(R.id.title_confirmation);
            TextView cancel = confirmView.findViewById(R.id.cancel);
            TextView ok = confirmView.findViewById(R.id.ok);

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setView(confirmView);
            Dialog a = builder.create();
            if (a.getWindow() != null)
                a.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            if (isFollow)
                if (isHide)
                    title.setText(confirmView.getContext().getString(R.string.unhide_post_ques));
                else
                    title.setText(confirmView.getContext().getString(R.string.hide_post_ques));

            cancel.setOnClickListener(v12 -> a.dismiss());

            ok.setOnClickListener(v1 -> {
                listener.setHidePostAction();
                a.dismiss();
            });

            a.show();
        });
    }

    private void setFollow() {
        follow.setOnClickListener(v -> {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            View confirmView = layoutInflater.inflate(R.layout.dialog_confirmation, null);
            TextView title = confirmView.findViewById(R.id.title_confirmation);
            TextView cancel = confirmView.findViewById(R.id.cancel);
            TextView ok = confirmView.findViewById(R.id.ok);

            if (isFollow) {
                title.setText(confirmView.getContext().getString(R.string.unfollow));
                title.append(" ");
                title.append(username);
                title.append("?");
            } else {
                title.setText(confirmView.getContext().getString(R.string.follow));
                title.append(" ");
                title.append(username);
                title.append("?");
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setView(confirmView);
            Dialog a = builder.create();
            if (a.getWindow() != null)
                a.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            a.show();

            cancel.setOnClickListener(v12 -> a.dismiss());

            ok.setOnClickListener(v1 -> {
                listener.setFollowAction();
                a.dismiss();
            });
        });
    }

    private void setDeletePost() {
        deletePost.setOnClickListener(v -> {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            View confirmView = layoutInflater.inflate(R.layout.dialog_confirmation, null);
            TextView title = confirmView.findViewById(R.id.title_confirmation);
            TextView cancel = confirmView.findViewById(R.id.cancel);
            TextView ok = confirmView.findViewById(R.id.ok);

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setView(confirmView);
            Dialog a = builder.create();
            if (a.getWindow() != null)
                a.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            title.setText(confirmView.getContext().getString(R.string.delete_post_ques));

            cancel.setOnClickListener(v12 -> a.dismiss());

            ok.setOnClickListener(v1 -> {
                listener.setDeletePostAction();
                a.dismiss();
            });

            a.show();
        });
    }

    public void setInfo(String publisherId, String userId, boolean isHide, boolean isFollow, String username) {
        this.publisherId = publisherId;
        this.userId = userId;
        this.isHide = isHide;
        this.isFollow = isFollow;
        this.username = username;
    }

    public interface ItemOptionPostClickListener {
        void setHidePostAction();

        void setFollowAction();

        void setDeletePostAction();
    }

}
