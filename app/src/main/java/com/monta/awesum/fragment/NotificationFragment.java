package com.monta.awesum.fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.monta.awesum.AwesumApp;
import com.monta.awesum.R;
import com.monta.awesum.adapter.NotificationAdapter;
import com.monta.awesum.model.Notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationFragment extends Fragment {
    private RecyclerView notificationRecyclerView;
    private LinearLayoutManager layoutManager;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageView option;

    private DatabaseReference notificationRef;
    private String userId;
    private long startTime;
    private long endTime;
    private boolean needLoadMore = true;
    private boolean stopLoad = false;


    public NotificationFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        notificationRecyclerView = view.findViewById(R.id.notification_recyclerview);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_notification);
        option = view.findViewById(R.id.option_notification);

        startTime = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000;
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        notificationRef = FirebaseDatabase.getInstance().getReference(AwesumApp.DB_NOTIFICATION).child(userId);

        notificationList = new ArrayList<>();
        if (notificationAdapter == null)
            notificationAdapter = new NotificationAdapter(view.getContext(), notificationList);
        layoutManager = new LinearLayoutManager(view.getContext());
        notificationRecyclerView.setAdapter(notificationAdapter);
        notificationRecyclerView.setLayoutManager(layoutManager);

        initializeNotification();

        setRefreshAction();

        setLoadMoreAction();

        setOptionClickAction();
        return view;
    }

    private void setRefreshAction() {
        swipeRefreshLayout.setColorSchemeResources(R.color.colorRed, R.color.colorYellow, R.color.colorGreen, R.color.colorBlue);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            notificationList.clear();
            notificationAdapter.notifyDataSetChanged();
            initializeNotification();
        });
    }

    private void setLoadMoreAction() {
        swipeRefreshLayout.setRefreshing(true);
        notificationRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (needLoadMore && (!stopLoad) && layoutManager.findLastVisibleItemPosition() == notificationList.size() - 1 && dy > 0) {
                    needLoadMore = false;
                    loadMoreNotification();
                } else if (dy < 0)
                    swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void initializeNotification() {
        swipeRefreshLayout.setRefreshing(true);
        Query query = notificationRef.limitToLast(20);
        query.keepSynced(true);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    Notification notification = data.getValue(Notification.class);
                    if (notification != null) {
                        endTime = notification.getNotificationId() - 1;
                    }
                    break;
                }
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    Notification notification = data.getValue(Notification.class);
                    notificationList.add(0, notification);
                }
                notificationAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadMoreNotification() {
        swipeRefreshLayout.setRefreshing(true);
        Query query = notificationRef.orderByChild("notificationId").startAt(startTime).endAt(endTime).limitToLast(10);
        query.keepSynced(true);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    swipeRefreshLayout.setRefreshing(false);
                    stopLoad = true;
                } else {
                    List<Notification> reverseList = new ArrayList<>();
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        Notification notification = data.getValue(Notification.class);
                        reverseList.add(notification);
                    }
                    Collections.reverse(reverseList);
                    endTime = reverseList.get(reverseList.size() - 1).getNotificationId() - 1;
                    notificationList.addAll(reverseList);
                    notificationAdapter.notifyItemRangeInserted(notificationAdapter.getItemCount(), reverseList.size());
                    swipeRefreshLayout.setRefreshing(false);
                }
                needLoadMore = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setOptionClickAction() {
        option.setOnClickListener(v -> {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_confirmation, null);
            TextView title = view.findViewById(R.id.title_confirmation);
            TextView cancel = view.findViewById(R.id.cancel);
            TextView ok = view.findViewById(R.id.ok);
            title.setText(getString(R.string.mark_all_read));
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setView(view);
            Dialog a = builder.create();
            if (a.getWindow() != null)
                a.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            cancel.setOnClickListener(v12 -> a.dismiss());

            ok.setOnClickListener(v1 -> notificationRef.orderByChild("seen").equalTo(false)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot data : dataSnapshot.getChildren()) {
                                data.getRef().child("seen").setValue(true);
                            }
                            for (Notification noti : notificationList) {
                                noti.setSeen(true);
                            }
                            notificationAdapter.notifyDataSetChanged();
                            a.dismiss();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    }));
            a.show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        notificationAdapter = null;
        notificationRecyclerView.setAdapter(null);
    }
}
