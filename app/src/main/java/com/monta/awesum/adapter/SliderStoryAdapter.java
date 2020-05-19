package com.monta.awesum.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.monta.awesum.R;
import com.monta.awesum.activity.StoryActivity;

import java.util.List;

public class SliderStoryAdapter extends PagerAdapter {

    private Context context;
    private List<String> idUserStoryList;

    public SliderStoryAdapter(Context context, List<String> idUserStoryList) {
        this.context = context;
        this.idUserStoryList = idUserStoryList;
    }

    @Override
    public int getCount() {
        return idUserStoryList.size();
    }


    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_story_full, container, false);
        view.setTag(position);
        ViewPager viewPager = (ViewPager) container;
        viewPager.addView(view, 0);

        AppCompatActivity activity = (AppCompatActivity) context;
        if (activity instanceof StoryActivity && viewPager.getCurrentItem() == position)
            ((StoryActivity) activity).updateView(position, view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        ViewPager viewPager = (ViewPager) container;
        View view = (View) object;
        viewPager.removeView(view);
    }
}
