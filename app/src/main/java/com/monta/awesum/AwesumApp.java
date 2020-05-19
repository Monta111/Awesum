package com.monta.awesum;

import android.app.Application;
import android.os.Environment;

import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.firebase.database.FirebaseDatabase;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.google.GoogleEmojiProvider;

public class AwesumApp extends Application {

    public static final String DB_POST = "Post";
    public static final String DB_POSTMAIN = "PostMain";
    public static final String DB_FOLLOW = "Follow";
    public static final String DB_SAVE = "Save";
    public static final String DB_USER = "User";
    public static final String DB_USER_SHORT = "UserShort";
    public static final String DB_NOTIFICATION = "Notification";
    public static final String DB_COMMENT = "Comment";
    public static final String DB_LIKE = "Like";
    public static final String DB_FOLLOWING = "Following";
    public static final String DB_FOLLOWER = "Follower";
    public static final String DB_STORY = "Story";
    public static final String DB_STORYMAIN = "StoryMain";
    public static final String DB_CONNECTION = "Connection";
    public static final String STORAGE_POST_IMAGE = "post_images";
    public static final String STORAGE_PROFILE_IMAGE = "profile_images";
    public static final String STORAGE_STORY_IMAGE = "story_images";
    public static final String DEFAULT_PROFILE_IMAGE = "https://firebasestorage.googleapis.com/v0/b/awesum-2.appspot.com/o/defaultavatar.png?alt=media&token=410dc69a-49c1-42bf-a5d3-3e48e026f06b";
    public static final String TOKEN = "token";

    public static SimpleCache simpleCache;
    long exoPlayerCacheSize = 500 * 1024 * 1024;
    private LeastRecentlyUsedCacheEvictor leastRecentlyUsedCacheEvictor;
    private ExoDatabaseProvider exoDatabaseProvider;

    @Override
    public void onCreate() {
        super.onCreate();

        //Enable Firebase Cache
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);


        //Using Google Emoji
        EmojiManager.install(new GoogleEmojiProvider());


        //Create simpleCache for Exoplayer
        if (leastRecentlyUsedCacheEvictor == null) {
            leastRecentlyUsedCacheEvictor = new LeastRecentlyUsedCacheEvictor(exoPlayerCacheSize);
        }

        if (exoDatabaseProvider != null) {
            exoDatabaseProvider = new ExoDatabaseProvider(this);
        }

        if (simpleCache == null) {
            simpleCache = new SimpleCache(getExternalFilesDir(Environment.DIRECTORY_PICTURES), leastRecentlyUsedCacheEvictor, exoDatabaseProvider);
        }
    }
}
