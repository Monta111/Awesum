package com.monta.awesum.fragment;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.monta.awesum.R;
import com.monta.awesum.adapter.GridMediaGalleryAdapter;
import com.monta.awesum.model.Post;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoGalleryFragment extends Fragment implements GridMediaGalleryAdapter.ItemClickListener {

    private ImageView close;
    private ImageView done;
    private GridMediaGalleryAdapter adapter;

    private List<Uri> selectedList;
    private List<Uri> uriGalleryList;

    private VideoGalleryInterface videoGalleryListener;

    public void setVideoGalleryListener(VideoGalleryInterface videoGalleryListener) {
        this.videoGalleryListener = videoGalleryListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_gallery, container, false);

        close = view.findViewById(R.id.close);
        done = view.findViewById(R.id.done);
        RecyclerView rv = view.findViewById(R.id.grid_image);


        selectedList = new ArrayList<>();
        uriGalleryList = new ArrayList<>();

        adapter = new GridMediaGalleryAdapter(getContext(), uriGalleryList);
        adapter.setItemType(Post.VIDEO_TYPE_ITEM);
        adapter.setListener(this);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 3));

        getAllVideoFromGallery();
        setCloseAction();
        setDoneAction();
        return view;
    }

    private void setCloseAction() {
        close.setOnClickListener(v -> getActivity().finish());
    }

    private void setDoneAction() {
        done.setOnClickListener(v -> {
            if (selectedList.size() == 0)
                Toast.makeText(getContext(), getString(R.string.zero_image), Toast.LENGTH_SHORT).show();
            else {
                ArrayList<String> uriStringList = new ArrayList<>();
                Uri uri = selectedList.get(0);
                uriStringList.add(uri.toString());

                videoGalleryListener.setDoneChooseVideo(uriStringList);
            }
        });
    }

    private void getAllVideoFromGallery() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            if (getContext() != null) {
                Cursor cursor = getContext().getApplicationContext().getContentResolver().query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Video.Media._ID},
                        null,
                        null,
                        MediaStore.Video.Media.DATE_ADDED + " DESC"
                );

                int fieldIndex;
                long id;
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        fieldIndex = cursor.getColumnIndex(MediaStore.Video.Media._ID);
                        id = cursor.getLong(fieldIndex);
                        uriGalleryList.add(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id));
                    }
                    cursor.close();

                    getActivity().runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                    });

                }
            }
        });


    }

    @Override
    public void setItemClickListener(GridMediaGalleryAdapter.GridMediaHolder holder) {
        if (holder.isSelected) {
            selectedList.remove(uriGalleryList.get(holder.getAdapterPosition()));
            holder.icon.setVisibility(View.GONE);
            holder.isSelected = false;

        } else {
            if (selectedList.size() < 1) {
                selectedList.add(uriGalleryList.get(holder.getAdapterPosition()));
                holder.icon.setVisibility(View.VISIBLE);
                Glide.with(getContext()).load(R.drawable.ic_image_selected).into(holder.icon);
                holder.isSelected = true;
            } else {
                Toast.makeText(getContext(), getContext().getString(R.string.max_video), Toast.LENGTH_SHORT).show();
            }

        }

    }

    public interface VideoGalleryInterface {
        void setDoneChooseVideo(ArrayList<String> uriStringList);
    }
}
