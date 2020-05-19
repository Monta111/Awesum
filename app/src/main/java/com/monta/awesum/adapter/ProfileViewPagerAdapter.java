package com.monta.awesum.adapter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.monta.awesum.fragment.AllPostFragment;
import com.monta.awesum.fragment.SavePostFragment;

public class ProfileViewPagerAdapter extends FragmentPagerAdapter {

    private int numberOfTabs;
    private String userId;
    private String username;

    public ProfileViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        Bundle bundle = new Bundle();
        bundle.putString("userId", userId);
        bundle.putString("username", username);
        if (numberOfTabs == 2) {
            if (position == 0) {
                AllPostFragment fragment = new AllPostFragment();
                fragment.setArguments(bundle);
                return fragment;
            } else {
                SavePostFragment fragment = new SavePostFragment();
                fragment.setArguments(bundle);
                return fragment;
            }
        } else {
            AllPostFragment fragment = new AllPostFragment();
            fragment.setArguments(bundle);
            return fragment;
        }
    }

    @Override
    public int getCount() {
        return numberOfTabs;
    }

    public void setNumberOfTabs(int numberOfTabs) {
        this.numberOfTabs = numberOfTabs;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
