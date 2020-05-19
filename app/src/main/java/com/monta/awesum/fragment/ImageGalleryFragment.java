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
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.monta.awesum.R;
import com.monta.awesum.adapter.GridMediaGalleryAdapter;
import com.monta.awesum.model.Post;

import java.util.ArrayList;
import java.util.List;

public class ImageGalleryFragment extends Fragment {

    private ImageView close;
    private ImageView done;
    private GridMediaGalleryAdapter adapter;
    private List<Uri> selectedList;

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
        // adapter = new GridMediaAdapter(getContext(), null, getAllImageFromGallery(), null);
        adapter = new GridMediaGalleryAdapter(getContext(), getAllImageFromGallery());
        adapter.setItemType(Post.IMAGE_TYPE_ITEM);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 3));

        selectedList = new ArrayList<>();
        setCloseAction();
        setDoneAction();

        return view;
    }

    private void setCloseAction() {
        close.setOnClickListener(v -> getActivity().finish());
    }

    private void setDoneAction() {
        done.setOnClickListener(v -> {
            selectedList = adapter.getSelectedMediaToPost();
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

    private List<Uri> getAllImageFromGallery() {

        List<Uri> allImages = new ArrayList<>();
        String[] projection = {
                MediaStore.Files.FileColumns._ID,
        };

        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

        Uri queryUri = MediaStore.Files.getContentUri("external");

        CursorLoader cursorLoader = new CursorLoader(
                getContext(),
                queryUri,
                projection,
                selection,
                null, // Selection args (none).
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC" // Sort order.
        );

        Cursor cursor = cursorLoader.loadInBackground();
        int fieldIndex;
        long id;
        if (cursor != null) {
            cursor.moveToFirst();
            fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
            id = cursor.getLong(fieldIndex);
            allImages.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id));
            while (cursor.moveToNext()) {
                fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                id = cursor.getLong(fieldIndex);
                allImages.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id));
            }
            cursor.close();
        }
        return allImages;
    }

    public interface ImageGalleryInterface {
        void setDoneChooseImage(ArrayList<String> uriStringList);
    }
}
