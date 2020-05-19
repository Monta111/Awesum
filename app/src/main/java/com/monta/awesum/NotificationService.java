package com.monta.awesum;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.monta.awesum.activity.CommentActivity;
import com.monta.awesum.activity.ProfileActivity;
import com.monta.awesum.activity.SinglePostActivity;
import com.monta.awesum.model.Notification;

import java.util.Map;

public class NotificationService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "1";
    private NotificationChannel channel;
    private NotificationManagerCompat notificationManagerCompat;

    @Override
    public void onCreate() {
        createNotificationChannel();
        super.onCreate();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Uri sound = Uri.parse("android.resource://"
                + getPackageName() + "/" + R.raw.notification);

        if (remoteMessage.getData().size() > 0) {
            Map<String, String> map = remoteMessage.getData();
            String userId = map.get("userId");
            String username = map.get("username");
            int type = Integer.parseInt(map.get("type"));
            String url = map.get("urlImage");
            String contentRaw = map.get("content");
            String content;

            if (contentRaw.equals(Notification.FOLLOW_TYPE_CONTENT))
                content = getString(R.string.follow_you);
            else if (contentRaw.equals(Notification.LIKE_TYPE_CONTENT))
                content = getString(R.string.like_your_post);
            else {
                String[] temp = contentRaw.split(": ");
                content = getString(R.string.commented_on_your_post) + temp[1];
            }
            String contentId = map.get("contentId");

            Glide.with(this).asBitmap().load(url).circleCrop().into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                    switch (type) {
                        case Notification.FOLLOW_TYPE: {
                            Intent intent = new Intent(NotificationService.this, ProfileActivity.class);
                            intent.putExtra("userId", userId);
                            intent.setAction(Intent.ACTION_MAIN);
                            intent.addCategory(Intent.CATEGORY_LAUNCHER);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            PendingIntent pendingIntent = PendingIntent.getActivity(NotificationService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                            android.app.Notification notification = new NotificationCompat.Builder(NotificationService.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_liked)
                                    .setColor(ContextCompat.getColor(NotificationService.this, R.color.colorRed))
                                    .setContentTitle(username)
                                    .setContentText(content)
                                    .setLargeIcon(resource)
                                    .setSound(sound)
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true)
                                    .build();
                            notificationManagerCompat.notify((int) System.currentTimeMillis(), notification);
                            break;
                        }
                        case Notification.LIKE_TYPE: {
                            Intent intent = new Intent(NotificationService.this, SinglePostActivity.class);
                            intent.putExtra("postId", contentId);
                            intent.setAction(Intent.ACTION_MAIN);
                            intent.addCategory(Intent.CATEGORY_LAUNCHER);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            PendingIntent pendingIntent = PendingIntent.getActivity(NotificationService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                            android.app.Notification notification = new NotificationCompat.Builder(NotificationService.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_liked)
                                    .setColor(ContextCompat.getColor(NotificationService.this, R.color.colorRed))
                                    .setContentTitle(username)
                                    .setContentText(content)
                                    .setLargeIcon(resource)
                                    .setSound(sound)
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true)
                                    .build();
                            notificationManagerCompat.notify((int) System.currentTimeMillis(), notification);
                            break;
                        }
                        case Notification.COMMENT_TYPE: {
                            Intent intent = new Intent(NotificationService.this, CommentActivity.class);
                            intent.putExtra("postId", contentId);
                            intent.putExtra("publisherId", userId);
                            intent.setAction(Intent.ACTION_MAIN);
                            intent.addCategory(Intent.CATEGORY_LAUNCHER);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            PendingIntent pendingIntent = PendingIntent.getActivity(NotificationService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                            android.app.Notification notification = new NotificationCompat.Builder(NotificationService.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_comment_dark)
                                    .setColor(ContextCompat.getColor(NotificationService.this, R.color.colorBlue))
                                    .setContentTitle(username)
                                    .setContentText(content)
                                    .setLargeIcon(resource)
                                    .setSound(sound)
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true)
                                    .build();
                            notificationManagerCompat.notify((int) System.currentTimeMillis(), notification);
                            break;
                        }
                    }

                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {

                }
            });
        }
    }

    private void createNotificationChannel() {
        notificationManagerCompat = NotificationManagerCompat.from(NotificationService.this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            Uri sound = Uri.parse("android.resource://"
                    + getPackageName() + "/" + R.raw.notification);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();
            channel.setSound(sound, audioAttributes);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
