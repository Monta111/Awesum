package com.monta.awesum.activity;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.video.VideoListener;
import com.monta.awesum.R;
import com.monta.awesum.ultility.CacheDataSourceFactory;

public class VideoActivity extends Activity {

    private ImageView exitFullScreen;
    private PlayerView video;
    private SimpleExoPlayer player;
    private VideoListener videoListener;

    private String uri;
    private long position;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        video = findViewById(R.id.video);
        exitFullScreen = video.findViewById(R.id.exo_fullscreen_icon);

        uri = getIntent().getStringExtra("uri");

        if (savedInstanceState == null)
            position = getIntent().getLongExtra("position", 0);
        else
            position = savedInstanceState.getLong("position");

        setPlayer();
        setExitFullScreen();
    }

    private void setPlayer() {
        player = new SimpleExoPlayer.Builder(this).build();

        videoListener = new VideoListener() {
            @Override
            public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
                if (height > width)
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                else
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        };

        player.addVideoListener(videoListener);

        MediaSource mediaSource = new ExtractorMediaSource(Uri.parse(uri),
                new CacheDataSourceFactory(this, 10 * 1024 * 1024),
                new DefaultExtractorsFactory(), null, null);

        player.prepare(mediaSource);
        video.setPlayer(player);
        player.seekTo(position);
        player.setPlayWhenReady(true);
    }

    private void setExitFullScreen() {
        exitFullScreen.setImageDrawable(getDrawable(R.drawable.ic_exit_full_screen));
        exitFullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                releasePlayer();
                finish();
            }
        });
    }

    private void releasePlayer() {
        player.removeVideoListener(videoListener);
        player.release();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("position", player.getCurrentPosition());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        releasePlayer();
    }
}
