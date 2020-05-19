package com.monta.awesum.activity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.monta.awesum.R;
import com.monta.awesum.adapter.GalleryViewPagerAdapter;
import com.monta.awesum.fragment.ImageGalleryFragment;
import com.monta.awesum.fragment.VideoGalleryFragment;
import com.monta.awesum.model.Post;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class PickMediaActivity extends AppCompatActivity implements VideoGalleryFragment.VideoGalleryInterface, ImageGalleryFragment.ImageGalleryInterface {

    private ViewPager viewPager;
    private TabLayout tabLayout;
    private GalleryViewPagerAdapter adapter;

    private int itemType;

    public PickMediaActivity() {
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_media);
        viewPager = findViewById(R.id.pick_viewpager);
        tabLayout = findViewById(R.id.pick_tab_layout);

        itemType = getIntent().getIntExtra("itemType", 0);

        adapter = new GalleryViewPagerAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

        setupViewPagerAdapter();
    }

    private void setupViewPagerAdapter() {
        adapter.setItemType(itemType);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void setDoneChooseImage(ArrayList<String> uriStringList) {
        Intent intent = new Intent(this, AddPostActivity.class);
        intent.putExtra("itemType", itemType);
        intent.putStringArrayListExtra("selectedList", uriStringList);
        startActivity(intent);
        finish();
    }

    @Override
    public void setDoneChooseVideo(ArrayList<String> uriStringList) {
        Uri uri = Uri.parse(uriStringList.get(0));

        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();

        Intent intent = new Intent(this, AddPostActivity.class);
        intent.putExtra("itemType", Post.VIDEO_TYPE_ITEM);

        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(picturePath, MediaStore.Video.Thumbnails.MINI_KIND);
        File temp = null;
        try {
            temp = createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (temp != null) {
            try {
                FileOutputStream outputStream = new FileOutputStream(temp);
                if (bitmap != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    intent.putExtra("thumb", Uri.fromFile(temp).toString());

                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }

        intent.putStringArrayListExtra("selectedList", uriStringList);
        startActivity(intent);
        finish();
    }

    private File createImageFile() throws IOException {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("temp", ".jpg", storageDir);
    }

    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof VideoGalleryFragment)
            ((VideoGalleryFragment) fragment).setVideoGalleryListener(this);
        else if (fragment instanceof ImageGalleryFragment)
            ((ImageGalleryFragment) fragment).setImageGalleryListener(this);
    }
}
