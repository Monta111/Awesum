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

public class ImageGalleryFragment extends Fragment implements GridMediaGalleryAdapter.ItemClickListener {

    private ImageView close;
    private ImageView done;
    private GridMediaGalleryAdapter adapter;
    private List<Uri> selectedList;
    private List<Uri> uriGalleryList;

    private ImageGalleryInterface imageGalleryListener;

    public void setImageGalleryListener(ImageGalleryInterface imageGalleryListener) {
        this.imageGalleryListener = imageGalleryListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_gallery, container, false);

        close = view.findViewById(R.id.close);
        done = view.findViewById(R.id.done);
        RecyclerView rv = view.findViewById(R.id.grid_image);

        uriGalleryList = new ArrayList<>();
        selectedList = new ArrayList<>();

        adapter = new GridMediaGalleryAdapter(getContext(), uriGalleryList);
        adapter.setItemType(Post.IMAGE_TYPE_ITEM);
        adapter.setListener(this);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 3));

        getAllImageFromGallery();
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

                for (Uri uri : selectedList)
                    uriStringList.add(uri.toString());
                imageGalleryListener.setDoneChooseImage(uriStringList);
            }
        });
    }

    private void getAllImageFromGallery() {

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            if (getContext() != null) {
                Cursor cursor = getContext().getApplicationContext().getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Images.Media._ID},
                        null,
                        null,
                        MediaStore.Images.Media.DATE_ADDED + " DESC"
                );

                int fieldIndex;
                long id;
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                        id = cursor.getLong(fieldIndex);
                        uriGalleryList.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id));
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
            if (selectedList.size() < 6) {
                selectedList.add(uriGalleryList.get(holder.getAdapterPosition()));
                holder.icon.setVisibility(View.VISIBLE);
                Glide.with(getContext()).load(R.drawable.ic_image_selected).into(holder.icon);
                holder.isSelected = true;
            } else {
                Toast.makeText(getContext(), getContext().getString(R.string.max_image), Toast.LENGTH_SHORT).show();

            }

        }
    }

    public interface ImageGalleryInterface {
        void setDoneChooseImage(ArrayList<String> uriStringList);
    }
}
