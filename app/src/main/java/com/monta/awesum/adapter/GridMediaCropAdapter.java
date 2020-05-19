package com.monta.awesum.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.monta.awesum.R;
import com.monta.awesum.ultility.Ultility;

public class GridMediaCropAdapter extends RecyclerView.Adapter<GridMediaCropAdapter.GridImageCropHolder> {

    private Context context;
    private Uri[] uriCropList;

    private ImageClickCropListener imageClickCropListener;


    public GridMediaCropAdapter(Context context, Uri[] uriCropList) {
        this.context = context;
        this.uriCropList = uriCropList;
    }


    @NonNull
    @Override
    public GridImageCropHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View itemView = layoutInflater.inflate(R.layout.item_image_multi, parent, false);
        return new GridImageCropHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull GridImageCropHolder holder, int position) {
        setImageLayout(holder);
        holder.uri = uriCropList[position];
        Glide.with(context).load(holder.uri).into(holder.image);
        holder.image.setOnClickListener(v -> imageClickCropListener.cropPrevious(position));
    }


    private void setImageLayout(GridImageCropHolder holder) {
        AppCompatActivity activity = (AppCompatActivity) context;
        int[] displayMetric = Ultility.getDisplayMetric(activity);
        int width = displayMetric[1] / 3;
        holder.image.setLayoutParams(new RelativeLayout.LayoutParams(width, width));
    }


    @Override
    public int getItemCount() {
        return uriCropList.length;
    }

    public void setImageClickCropListener(ImageClickCropListener imageClickCropListener) {
        this.imageClickCropListener = imageClickCropListener;
    }


    public interface ImageClickCropListener {
        void cropPrevious(int position);
    }

    static class GridImageCropHolder extends RecyclerView.ViewHolder {

        private ImageView image;
        private String postId;
        private Uri uri;

        private boolean isSelected = false;

        GridImageCropHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image_post);
        }
    }
}
