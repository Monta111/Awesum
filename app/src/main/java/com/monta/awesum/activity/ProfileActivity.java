package com.monta.awesum.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.adapter.ProfileViewPagerAdapter;
import com.monta.awesum.model.Notification;
import com.monta.awesum.model.User;


public class ProfileActivity extends AppCompatActivity {

    private String userId;
    private String url;
    private String currentUserId;

    private TextView username;
    private TextView fullname;
    private ImageView profileImage;
    private TextView numberOfPosts;
    private TextView numberOfFollowing;
    private TextView numberOfFollowers;
    private TextView bio;
    private Button editProfile;
    private Button follow;
    private LinearLayout following;
    private LinearLayout follower;


    private ViewPager viewPager;
    private TabLayout tabLayout;
    private ProfileViewPagerAdapter profileViewPagerAdapter;

    private DatabaseReference userRef;
    private DatabaseReference postRef;
    private DatabaseReference followingRef;
    private DatabaseReference followerRef;
    private ValueEventListener userListener;
    private ValueEventListener postListener;
    private ValueEventListener followingListener;
    private ValueEventListener followerListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_profile);

        Toolbar toolbar = findViewById(R.id.profile_bar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        userId = getIntent().getStringExtra("userId");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        viewPager = findViewById(R.id.profile_viewpager);
        tabLayout = findViewById(R.id.swipe_tab_layout);

        username = findViewById(R.id.username_profile);
        fullname = findViewById(R.id.fullname_profile);
        profileImage = findViewById(R.id.profile_image);
        numberOfPosts = findViewById(R.id.number_of_posts);
        numberOfFollowing = findViewById(R.id.number_of_following);
        numberOfFollowers = findViewById(R.id.number_of_followers);
        bio = findViewById(R.id.bio);
        editProfile = findViewById(R.id.edit_profile);
        following = findViewById(R.id.profile_following);
        follower = findViewById(R.id.profile_follower);
        follow = findViewById(R.id.follow_button);

        setEditProfileButton();
        setFollowersClick();
        setFollowingClick();
        setFollowButton();
    }

    private void loadInfo() {
        setUserInfo();
        setNumberOfPosts();
        setNumberOfFollowing();
        setNumberOfFollowers();
        setFollowStatus();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadInfo();
    }

    private void setupViewPager() {
        profileViewPagerAdapter = new ProfileViewPagerAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        profileViewPagerAdapter.setUsername(username.getText().toString());
        if (userId.equals(currentUserId)) {
            profileViewPagerAdapter.setUserId(currentUserId);
            profileViewPagerAdapter.setNumberOfTabs(2);
        } else {
            profileViewPagerAdapter.setUserId(userId);
            profileViewPagerAdapter.setNumberOfTabs(1);
        }
        viewPager.setAdapter(profileViewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        if (userId.equals(currentUserId)) {
            tabLayout.getTabAt(0).setIcon(R.drawable.ic_grid);
            tabLayout.getTabAt(1).setIcon(R.drawable.ic_save);
        } else
            tabLayout.getTabAt(0).setIcon(R.drawable.ic_grid);

    }

    private void setUserInfo() {
        userRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_USER).child(userId);
        if (userListener == null)
            userListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        username.setText(user.getUsername());
                        fullname.setText(user.getFullname());
                        setupViewPager();
                        url = user.getAvatarUrl();
                        Glide.with(ProfileActivity.this).load(url).circleCrop().into(profileImage);
                        if (TextUtils.isEmpty(user.getBio()))
                            bio.setVisibility(View.GONE);
                        else
                            bio.setText(user.getBio());
                        if (!url.equals(AwesumApp.DEFAULT_PROFILE_IMAGE))
                            setProfileImageClick();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };

        userRef.addListenerForSingleValueEvent(userListener);
    }

    private void setNumberOfPosts() {
        postRef = userRef.child(AwesumApp.DB_POST);
        if (postListener == null)
            postListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    numberOfPosts.setText(String.valueOf(dataSnapshot.getChildrenCount()));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
        postRef.addListenerForSingleValueEvent(postListener);
    }

    private void setNumberOfFollowing() {
        followingRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_FOLLOW)
                .child(userId).child(AwesumApp.DB_FOLLOWING);
        if (followingListener == null)
            followingListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    numberOfFollowing.setText(String.valueOf(dataSnapshot.getChildrenCount()));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
        followingRef.addListenerForSingleValueEvent(followingListener);
    }

    private void setNumberOfFollowers() {
        followerRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_FOLLOW)
                .child(userId).child(AwesumApp.DB_FOLLOWER);
        if (followerListener == null)
            followerListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    numberOfFollowers.setText(String.valueOf(dataSnapshot.getChildrenCount()));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
        followerRef.addListenerForSingleValueEvent(followerListener);
    }

    private void setEditProfileButton() {
        if (userId.equals(currentUserId)) {
            editProfile.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            });
        } else {
            editProfile.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    private void setFollowersClick() {
        follower.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, FollowActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("followType", AwesumApp.DB_FOLLOWER);
            startActivity(intent);
        });
    }

    private void setFollowingClick() {
        following.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, FollowActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("followType", AwesumApp.DB_FOLLOWING);
            startActivity(intent);
        });
    }

    private void setProfileImageClick() {
        profileImage.setOnClickListener(v -> {
            Dialog dialog = new Dialog(ProfileActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            PhotoView image = new PhotoView(ProfileActivity.this);
            Glide.with(ProfileActivity.this).asBitmap().load(url).into(image);
            image.setOnClickListener(v1 -> dialog.dismiss());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.addContentView(image, layoutParams);
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();
        });
    }

    private void setFollowButton() {
        if (!userId.equals(currentUserId)) {
            follow.setVisibility(View.VISIBLE);
            follow.setOnClickListener(v -> {
                ProgressDialog progressDialog = new ProgressDialog(ProfileActivity.this);
                progressDialog.setMessage(getString(R.string.please_wait));
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();

                DatabaseReference followRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_FOLLOW);
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_NOTIFICATION).child(userId);
                ref.keepSynced(true);
                long t = System.currentTimeMillis();
                if (follow.getText().toString().equals(getString(R.string.follow))) {
                    followRef.child(currentUserId).child(AwesumApp.DB_FOLLOWING).child(userId).setValue(t).addOnCompleteListener(task ->
                            followRef.child(userId).child(AwesumApp.DB_FOLLOWER).child(currentUserId).setValue(t).addOnCompleteListener(task12 -> {
                                follow.setText(getString(R.string.following));
                                String contentCombine = Notification.FOLLOW_TYPE + userId + currentUserId;
                                Notification notification = new Notification(Notification.FOLLOW_TYPE, t, currentUserId, Notification.FOLLOW_TYPE_CONTENT, "null", false, false, contentCombine);
                                ref.child(String.valueOf(t)).setValue(notification);
                                progressDialog.dismiss();
                                loadInfo();
                            }));

                } else if (follow.getText().toString().equals(getString(R.string.following))) {
                    followRef.child(currentUserId).child(AwesumApp.DB_FOLLOWING).child(userId).removeValue().addOnCompleteListener(task ->
                            followRef.child(userId).child(AwesumApp.DB_FOLLOWER).child(currentUserId).removeValue().addOnCompleteListener(task1 -> {
                                follow.setText(getString(R.string.follow));
                                String contentCombine = Notification.FOLLOW_TYPE + userId + currentUserId;
                                Query query = ref.orderByChild("contentCombine").equalTo(contentCombine);
                                query.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                                            data.getRef().removeValue();
                                        }
                                        progressDialog.dismiss();
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                                loadInfo();
                            }));

                }
            });
        }
    }

    private void setFollowStatus() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_FOLLOW).child(currentUserId).child(AwesumApp.DB_FOLLOWING);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(userId)) {
                    follow.setText(getString(R.string.following));
                } else {
                    follow.setText(getString(R.string.follow));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
