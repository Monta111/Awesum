package com.monta.awesum.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.adapter.SliderStoryAdapter;
import com.monta.awesum.model.Story;
import com.monta.awesum.model.User;
import com.monta.awesum.ultility.Ultility;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class StoryActivity extends AppCompatActivity {

    private List<String> idUserStoryList;
    private ViewPager sliderStory;

    private DatabaseReference userRef;
    private DatabaseReference storyMainRef;
    private DatabaseReference storyRef;

    private long minimumTime = System.currentTimeMillis() - 86400000;
    private Timer t;
    private int currentPositionImage;
    private int[] displayMetric;

    private ViewPager.OnPageChangeListener pageChangeListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);

        userRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER);
        storyMainRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_STORYMAIN)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        storyRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_STORY);

        idUserStoryList = getIntent().getStringArrayListExtra("idUserStoryList");
        int position = getIntent().getIntExtra("position", 0);
        displayMetric = Ultility.getDisplayMetric(this);

        if (idUserStoryList != null) {
            idUserStoryList.remove(0);
        }

        sliderStory = findViewById(R.id.slider_story);
        SliderStoryAdapter adapter = new SliderStoryAdapter(this, idUserStoryList);
        sliderStory.setAdapter(adapter);
        sliderStory.setCurrentItem(position);

        if (pageChangeListener == null)
            pageChangeListener = new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    View view = sliderStory.findViewWithTag(position);
                    if (t != null)
                        t.cancel();
                    currentPositionImage = 0;
                    updateView(position, view);
                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            };
        sliderStory.addOnPageChangeListener(pageChangeListener);
    }

    public void updateView(int position, View view) {
        LinearLayout layoutProgress = view.findViewById(R.id.layout_progress);
        ImageView profileImage = view.findViewById(R.id.profile_image);
        TextView username = view.findViewById(R.id.username);
        TextView timestamp = view.findViewById(R.id.timestamp_story);
        ImageView storyImage = view.findViewById(R.id.image_story);

        layoutProgress.removeAllViews();

        String currentUserId = idUserStoryList.get(position);
        userRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    username.setText(user.getUsername());
                    Glide.with(StoryActivity.this).load(user.getAvatarUrl()).circleCrop().into(profileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        storyMainRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> idStoryList = new ArrayList<>();
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    String key = data.getKey();
                    if (key != null && !key.equals("lastest") && Long.parseLong(key) > minimumTime) {
                        idStoryList.add(0, data.getKey());
                    }
                }

                ProgressBar[] progressBars = new ProgressBar[idStoryList.size()];
                for (int i = 0; i < idStoryList.size(); ++i) {
                    progressBars[i] = (ProgressBar) LayoutInflater.from(StoryActivity.this).inflate(R.layout.progress_bar, layoutProgress, false);
                    layoutProgress.addView(progressBars[i]);
                }
                setTimer(0, storyImage, timestamp, progressBars, idStoryList, currentUserId);

                storyImage.setOnTouchListener((v, event) -> {
                    int leftPercent = displayMetric[1] * 20 / 100;
                    int rightPercent = displayMetric[1] * 80 / 100;
                    if (event.getX() <= leftPercent && currentPositionImage >= 1) {
                        t.cancel();
                        progressBars[currentPositionImage].setProgress(0);
                        setTimer(currentPositionImage - 1, storyImage, timestamp, progressBars, idStoryList, currentUserId);
                    } else if (event.getX() >= rightPercent && currentPositionImage < idStoryList.size() - 1) {
                        t.cancel();
                        progressBars[currentPositionImage].setProgress(100);
                        setTimer(currentPositionImage + 1, storyImage, timestamp, progressBars, idStoryList, currentUserId);
                    }
                    return false;
                });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void setTimer(int i, ImageView storyImage, TextView timestamp, ProgressBar[] progressBars, List<String> idStoryList, String currentUserId) {
        if (i < progressBars.length) {
            currentPositionImage = i;
            String idStory = idStoryList.get(i);
            storyMainRef.child(currentUserId).child(idStory).setValue(true);
            storyRef.child(idStory).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    final int[] count = {0};
                    Story story = dataSnapshot.getValue(Story.class);
                    if (story != null)
                        Glide.with(StoryActivity.this).load(story.getImageUrl()).fitCenter().into(storyImage);
                    t = new Timer();
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            ++count[0];
                            progressBars[i].setProgress(count[0]);
                            if (count[0] == 100) {
                                t.cancel();
                                setTimer(i + 1, storyImage, timestamp, progressBars, idStoryList, currentUserId);
                            }
                        }
                    };
                    t.schedule(task, 200, 50);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            runOnUiThread(() -> {
                long t = Long.parseLong(idStory);
                long delta = System.currentTimeMillis() - t;
                int minute = (int) (delta / (60 * 1000));
                int hour = minute / 60;
                if (hour > 0)
                    timestamp.setText(getString(R.string.hours_ago, hour));
                else
                    timestamp.setText(getString(R.string.minutes_ago, minute));
            });


        } else {
            runOnUiThread(() -> {
                if (sliderStory.getCurrentItem() + 1 < idUserStoryList.size())
                    sliderStory.setCurrentItem(sliderStory.getCurrentItem() + 1);
            });

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        t.cancel();
        sliderStory.removeOnPageChangeListener(pageChangeListener);
        sliderStory.setAdapter(null);
    }
}
