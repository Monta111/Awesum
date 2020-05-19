package com.monta.awesum.adapter;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.monta.awesum.R;
import com.monta.awesum.model.Post;
import com.monta.awesum.ultility.Ultility;

import java.util.ArrayList;
import java.util.List;

public class GridMediaGalleryAdapter extends RecyclerView.Adapter<GridMediaGalleryAdapter.GridMediaHolder> {

    private Context context;
    private List<Uri> uriGalleryList;
    private int itemType;

    private List<Uri> selectedMediaToPost;


    public GridMediaGalleryAdapter(Context context, List<Uri> uriGalleryList) {
        this.context = context;
        this.uriGalleryList = uriGalleryList;

        selectedMediaToPost = new ArrayList<>();
    }


    @NonNull
    @Override
    public GridMediaHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View itemView = layoutInflater.inflate(R.layout.item_image_multi, parent, false);
        return new GridMediaHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull GridMediaHolder holder, int position) {
        setGridLayout(holder);

        Glide.with(context).load(uriGalleryList.get(position)).thumbnail(0.1f).into(holder.image);

        if (itemType == Post.IMAGE_TYPE_ITEM)
            holder.icon.setVisibility(View.GONE);
        else {
            holder.icon.setVisibility(View.GONE);
            holder.duration.setVisibility(View.VISIBLE);
            setVideoInfo(holder);
        }

        setMediaClickChooseToPost(holder);
    }

    private void setVideoInfo(GridMediaHolder holder) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, uriGalleryList.get(holder.getAdapterPosition()));
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long timeInMillisec = Long.parseLong(time);
        int second = (int) (timeInMillisec / 1000);
        int minute = second / 60;
        if (minute <= 0) {
            if (second < 10)
                holder.duration.setText("00:0" + second);
            else
                holder.duration.setText("00:" + second);
        } else {
            int remain = second - minute * 60;
            holder.duration.setText(minute + ":" + remain);
        }
        retriever.release();
    }

    private void setGridLayout(GridMediaHolder holder) {
        AppCompatActivity activity = (AppCompatActivity) context;
        int[] displayMetric = Ultility.getDisplayMetric(activity);
        int width = displayMetric[1] / 3;
        holder.image.setLayoutParams(new RelativeLayout.LayoutParams(width, width));
    }


    private void setMediaClickChooseToPost(GridMediaHolder holder) {
        holder.image.setOnClickListener(v -> {
            if (holder.isSelected) {
                selectedMediaToPost.remove(uriGalleryList.get(holder.getAdapterPosition()));
                holder.icon.setVisibility(View.GONE);
                holder.isSelected = false;

            } else {
                switch (itemType) {
                    case Post.IMAGE_TYPE_ITEM:
                        boolean lowerThanSix = selectedMediaToPost.size() < 6;
                        if (lowerThanSix) {
                            selectedMediaToPost.add(uriGalleryList.get(holder.getAdapterPosition()));
                            holder.icon.setVisibility(View.VISIBLE);
                            Glide.with(context).load(R.drawable.ic_image_selected).into(holder.icon);
                            holder.isSelected = true;
                        } else {
                            Toast.makeText(context, context.getString(R.string.max_image), Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case Post.VIDEO_TYPE_ITEM:
                        boolean lowerThanOne = selectedMediaToPost.size() < 1;
                        if (lowerThanOne) {
                            selectedMediaToPost.add(uriGalleryList.get(holder.getAdapterPosition()));
                            holder.icon.setVisibility(View.VISIBLE);
                            Glide.with(context).load(R.drawable.ic_image_selected).into(holder.icon);
                            holder.isSelected = true;
                        } else
                            Toast.makeText(context, context.getString(R.string.max_video), Toast.LENGTH_SHORT).show();
                        break;

                }

            }
        });
    }

    @Override
    public int getItemCount() {
        return uriGalleryList.size();
    }

    public void setItemType(int itemType) {
        this.itemType = itemType;
    }


    public List<Uri> getSelectedMediaToPost() {
        return selectedMediaToPost;
    }


    static class GridMediaHolder extends RecyclerView.ViewHolder {

        private ImageView image;
        private ImageView icon;
        private TextView duration;

        private boolean isSelected = false;

        GridMediaHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image_post);
            icon = itemView.findViewById(R.id.multi_icon);
            duration = itemView.findViewById(R.id.video_duration);
        }
    }
}
