package com.monta.awesum.adapter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.monta.awesum.fragment.CameraFragment;
import com.monta.awesum.fragment.ImageGalleryFragment;
import com.monta.awesum.fragment.VideoGalleryFragment;
import com.monta.awesum.model.Story;

public class GalleryViewPagerAdapter extends FragmentPagerAdapter {

    private static final String[] TITLE = {"Camera", "Photo", "Video"};

    private CameraFragment cameraFragment;
    private int itemType;

    public GalleryViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        Bundle bundle = new Bundle();
        bundle.putInt("itemType", itemType);
        switch (position) {
            case 1:
                return new ImageGalleryFragment();
            case 2:
                return new VideoGalleryFragment();
            default:
                if (cameraFragment == null)
                    cameraFragment = new CameraFragment();
                cameraFragment.setArguments(bundle);
                return cameraFragment;
        }
    }

    @Override
    public int getCount() {
        if (itemType != Story.TYPE_ITEM)
            return 3;
        return 2;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return TITLE[position];
    }

    public void setItemType(int itemType) {
        this.itemType = itemType;
    }
}
