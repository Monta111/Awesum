package com.monta.awesum.fragment;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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
import com.google.firebase.database.ValueEventListener;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.activity.EditProfileActivity;
import com.monta.awesum.activity.FollowActivity;
import com.monta.awesum.activity.SettingActivity;
import com.monta.awesum.adapter.ProfileViewPagerAdapter;
import com.monta.awesum.model.User;


public class ProfileFragment extends Fragment {

    private String userId;
    private String url;

    private TextView username;
    private TextView fullname;
    private ImageView profileImage;
    private TextView numberOfPosts;
    private TextView numberOfFollowing;
    private TextView numberOfFollowers;
    private TextView bio;
    private Button editProfile;
    private LinearLayout follower;
    private LinearLayout following;
    private ImageView setting;

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

    public ProfileFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_fragment_profile, container, false);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        viewPager = view.findViewById(R.id.profile_viewpager);
        tabLayout = view.findViewById(R.id.swipe_tab_layout);
        profileViewPagerAdapter = new ProfileViewPagerAdapter(getChildFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

        username = view.findViewById(R.id.username_profile);
        fullname = view.findViewById(R.id.fullname_profile);
        profileImage = view.findViewById(R.id.profile_image);
        numberOfPosts = view.findViewById(R.id.number_of_posts);
        numberOfFollowing = view.findViewById(R.id.number_of_following);
        numberOfFollowers = view.findViewById(R.id.number_of_followers);
        bio = view.findViewById(R.id.bio);
        editProfile = view.findViewById(R.id.edit_profile);
        follower = view.findViewById(R.id.profile_follower);
        following = view.findViewById(R.id.profile_following);
        setting = view.findViewById(R.id.setting);

        setEditProfileButton();
        setFollowersClick();
        setFollowingClick();
        setSettingClick();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadInfo();
    }

    private void loadInfo() {
        setUserInfo();
        setNumberOfPosts();
        setNumberOfFollowing();
        setNumberOfFollowers();
    }


    private void setupViewPager() {
        profileViewPagerAdapter.setNumberOfTabs(2);
        profileViewPagerAdapter.setUserId(userId);
        profileViewPagerAdapter.setUsername(username.getText().toString());
        viewPager.setAdapter(profileViewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_grid);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_save);
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
                        if (getContext() != null)
                            Glide.with(getContext()).load(url).circleCrop().into(profileImage);
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
        editProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    private void setFollowersClick() {
        follower.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), FollowActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("followType", AwesumApp.DB_FOLLOWER);
            startActivity(intent);
        });
    }

    private void setFollowingClick() {
        following.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), FollowActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("followType", AwesumApp.DB_FOLLOWING);
            startActivity(intent);
        });
    }

    private void setProfileImageClick() {
        profileImage.setOnClickListener(v -> {
            if (getContext() != null && getActivity() != null) {
                Dialog dialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                PhotoView image = new PhotoView(getContext());
                Glide.with(getContext()).asBitmap().load(url).into(image);
                image.setOnClickListener(v1 -> dialog.dismiss());
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                dialog.addContentView(image, layoutParams);
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }
        });
    }

    private void setSettingClick() {
        setting.setVisibility(View.VISIBLE);
        setting.setOnClickListener(v -> getContext().startActivity(new Intent(getContext(), SettingActivity.class)));
    }
}
