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

public class CommentOption extends BottomSheetDialogFragment {

    private TextView editComment;
    private TextView deleteComment;
    private String publisherID;
    private String userId;
    private String commentPublisherId;

    private ItemOptionCommentClickListener listener;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_option, null);
        editComment = view.findViewById(R.id.option_1);
        TextView option2 = view.findViewById(R.id.option_2);
        option2.setVisibility(View.GONE);
        deleteComment = view.findViewById(R.id.option_3);

        if (userId.equals(publisherID) || userId.equals(commentPublisherId)) {
            deleteComment.setText(view.getContext().getString(R.string.delete_comment));
            deleteComment.setVisibility(View.VISIBLE);
            deleteComment.setOnClickListener(v -> {
                View confirmView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_confirmation, null);
                TextView title = confirmView.findViewById(R.id.title_confirmation);
                TextView cancel = confirmView.findViewById(R.id.cancel);
                TextView ok = confirmView.findViewById(R.id.ok);

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setView(confirmView);
                Dialog a = builder.create();
                if (a.getWindow() != null)
                    a.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                title.setText(view.getContext().getString(R.string.delete_comment_ques));
                cancel.setOnClickListener(v12 -> a.dismiss());

                ok.setOnClickListener(v1 -> {
                    listener.deleteComment();
                    a.dismiss();
                    CommentOption.this.dismiss();
                });
                a.show();
            });
        }

        if (userId.equals(commentPublisherId)) {
            editComment.setText(view.getContext().getString(R.string.edit_comment));
            editComment.setOnClickListener(v -> listener.editComment());
        } else
            editComment.setVisibility(View.GONE);
        return view;
    }

    public void setInfo(String userId, String publisherID, String commentPublisherId) {
        this.userId = userId;
        this.publisherID = publisherID;
        this.commentPublisherId = commentPublisherId;
    }

    public void setListener(ItemOptionCommentClickListener listener) {
        this.listener = listener;
    }

    public interface ItemOptionCommentClickListener {
        void editComment();

        void deleteComment();
    }
}
