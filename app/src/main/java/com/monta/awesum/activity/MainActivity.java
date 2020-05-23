package com.monta.awesum.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.fragment.HomeFragment;
import com.monta.awesum.fragment.NotificationFragment;
import com.monta.awesum.fragment.ProfileFragment;
import com.monta.awesum.fragment.SearchFragment;
import com.monta.awesum.model.Post;


public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private BottomNavigationMenuView bottomNavigationMenuView;
    private BottomNavigationItemView postBadge;
    private BottomNavigationItemView notiBadge;
    private TextView numberOfNewPosts;
    private TextView numberOfNewNotifications;

    private String userId;
    private DatabaseReference postMainRef;
    private DatabaseReference notificationRef;
    private Query queryNewPost;
    private Query queryNewNotification;
    private ValueEventListener listenerNewPost;
    private ValueEventListener listenerNewNotification;

    private Fragment selectedFragment;

    private boolean doubleBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new HomeFragment()).commit();
        }

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        bottomNavigationView = findViewById(R.id.bottom_nav);

        bottomNavigationMenuView = (BottomNavigationMenuView) bottomNavigationView.getChildAt(0);
        postBadge = (BottomNavigationItemView) bottomNavigationMenuView.getChildAt(0);
        notiBadge = (BottomNavigationItemView) bottomNavigationMenuView.getChildAt(3);

        postMainRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_POSTMAIN).child(userId);
        notificationRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_NOTIFICATION).child(userId);

        View postBadgeView = LayoutInflater.from(this).inflate(R.layout.badge, postBadge, true);
        numberOfNewPosts = postBadgeView.findViewById(R.id.badge);
        View notiBadgeView = LayoutInflater.from(this).inflate(R.layout.badge, notiBadge, true);
        numberOfNewNotifications = notiBadgeView.findViewById(R.id.badge);

        setBottomNavigation();
        //manageConnection();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setNumberOfNewPostsBadge();
        setNumberOfNewNotificationsBadge();
    }

    private void setBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_home: {
                    selectedFragment = new HomeFragment();
                    refreshPostBadge();
                    break;
                }
                case R.id.nav_search: {
                    selectedFragment = new SearchFragment();
                    break;
                }
                case R.id.nav_add: {
                    Intent intent = new Intent(this, PickMediaActivity.class);
                    intent.putExtra("itemType", Post.IMAGE_TYPE_ITEM);
                    startActivity(intent);
                    break;
                }
                case R.id.nav_profile: {
                    selectedFragment = new ProfileFragment();
                    break;
                }
                case R.id.nav_notification: {
                    selectedFragment = new NotificationFragment();
                    queryNewNotification.removeEventListener(listenerNewNotification);
                    numberOfNewNotifications.setVisibility(View.GONE);
                    notificationRef.orderByChild("ignore").equalTo(false)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                                        data.getRef().child("ignore").setValue(true);
                                    }
                                    queryNewNotification.addValueEventListener(listenerNewNotification);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                    break;
                }
            }
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
            }
            return true;
        });
    }

//    private void manageConnection() {
//
//        FirebaseDatabase.getInstance().getReference(".info/connected").addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                boolean connected = (boolean) dataSnapshot.getValue();
//                if (connected) {
//                    DatabaseReference connectRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_CONNECTION)
//                            .child(userId).child("online");
//                    DatabaseReference lastOnlineRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_CONNECTION)
//                            .child(userId).child("lastOnline");
//                    connectRef.setValue(true);
//                    connectRef.onDisconnect().setValue(false);
//                    lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP);
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//            }
//        });
//    }

    private void setNumberOfNewPostsBadge() {
        long queryTime = System.currentTimeMillis();
        queryNewPost = postMainRef.orderByKey().startAt(String.valueOf(queryTime));
        if (listenerNewPost == null)
            listenerNewPost = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    long count = dataSnapshot.getChildrenCount();
                    if (count != 0) {
                        if (count < 9) {
                            numberOfNewPosts.setText(String.valueOf(count));
                            numberOfNewPosts.setVisibility(View.VISIBLE);
                        } else {
                            numberOfNewPosts.setText("9+");
                            numberOfNewPosts.setVisibility(View.VISIBLE);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
        queryNewPost.addValueEventListener(listenerNewPost);
    }

    private void setNumberOfNewNotificationsBadge() {
        queryNewNotification = notificationRef.orderByChild("ignore").equalTo(false);
        if (listenerNewNotification == null)
            listenerNewNotification = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    long count = dataSnapshot.getChildrenCount();
                    if (count != 0) {
                        if (count < 10) {
                            numberOfNewNotifications.setVisibility(View.VISIBLE);
                            numberOfNewNotifications.setText(String.valueOf(count));
                        } else {
                            numberOfNewNotifications.setVisibility(View.VISIBLE);
                            numberOfNewNotifications.setText("9+");
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
        queryNewNotification.addValueEventListener(listenerNewNotification);
    }

    public void refreshPostBadge() {
        numberOfNewPosts.setVisibility(View.GONE);
        queryNewPost.removeEventListener(listenerNewPost);
        long queryTime = System.currentTimeMillis();
        queryNewPost = postMainRef.orderByKey().startAt(String.valueOf(queryTime));
        queryNewPost.addValueEventListener(listenerNewPost);
    }

    private void detachListener() {
        queryNewPost.removeEventListener(listenerNewPost);
        queryNewNotification.removeEventListener(listenerNewNotification);
    }

    @Override
    public void onBackPressed() {
        if (doubleBack) {
            detachListener();
            finish();
        }
        doubleBack = true;
        Toast.makeText(this, getString(R.string.click_back), Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(() -> doubleBack = false, 2000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachListener();
    }

}



