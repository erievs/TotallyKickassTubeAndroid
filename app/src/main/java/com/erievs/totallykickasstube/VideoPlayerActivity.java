package com.erievs.totallykickasstube;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;

import java.util.ArrayList;
import java.util.List;

public class VideoPlayerActivity extends AppCompatActivity implements YouTubeFetcher.FetchRelatedVideosCallback {

    private TextView videoTitle;
    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private VideoHandler videoHandler;

    private RecyclerView relatedVideosRecyclerView;
    private RelatedVideosAdapter relatedVideosAdapter;

    private boolean isFullscreen = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        videoTitle = findViewById(R.id.videoTitle);
        playerView = findViewById(R.id.playerView);
        relatedVideosRecyclerView = findViewById(R.id.relatedVideosRecyclerView);

        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);

        videoHandler = new VideoHandler(exoPlayer, this);

        relatedVideosRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        relatedVideosAdapter = new RelatedVideosAdapter(new ArrayList<>(), contentItem -> {
            String videoId = contentItem.videoRenderer.videoId;
            String videoUrl = "http://youtu.be/" + videoId;

            Log.d("VideoUrl", "Video URL clicked: " + videoUrl);
            navigateToVideoPlayerActivity(videoUrl);
        });
        relatedVideosRecyclerView.setAdapter(relatedVideosAdapter);

        Intent intent = getIntent();
        String videoUrl = intent.getStringExtra("VIDEO_URL");

        if (videoUrl == null || videoUrl.isEmpty()) {
            Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show();
            return;
        }

        YouTubeFetcher fetcher = new YouTubeFetcher(this);

        fetcher.fetchVideoTitle(videoUrl, new YouTubeFetcher.FetchVideoTitleCallback() {
            @Override
            public void onTitleFetched(String title) {
                if (title != null) {
                    videoTitle.setText(title);
                } else {
                    Toast.makeText(VideoPlayerActivity.this, "Failed to fetch video title", Toast.LENGTH_SHORT).show();
                }
            }
        });

        videoHandler.startStream(videoUrl);

        String videoId = YouTubeUtils.extractVideoId(videoUrl);
        if (videoId != null) {
            fetcher.fetchRelatedVideos(videoId);
        } else {
            Toast.makeText(this, "Invalid video ID", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (exoPlayer != null) {
            outState.putLong("EXO_PLAYER_POSITION", exoPlayer.getCurrentPosition());
            outState.putBoolean("EXO_PLAYER_PLAY_WHEN_READY", exoPlayer.getPlayWhenReady());
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            long position = savedInstanceState.getLong("EXO_PLAYER_POSITION", 0);
            boolean playWhenReady = savedInstanceState.getBoolean("EXO_PLAYER_PLAY_WHEN_READY", true);

            exoPlayer.seekTo(position);
            exoPlayer.setPlayWhenReady(playWhenReady);
        }
    }
    @Override
    public void onRelatedVideosFetched(List<YouTubeResponse.ContentItem> relatedVideos) {
        if (relatedVideos == null || relatedVideos.isEmpty()) {
            Toast.makeText(this, "No related videos found", Toast.LENGTH_SHORT).show();
        } else {
            relatedVideosAdapter.updateVideos(relatedVideos);
        }
    }

    @Override
    public void onFetchFailed(String errorMessage) {
        Toast.makeText(this, "Failed to fetch related videos: " + errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.d("VideoPlayerActivity", "onConfigurationChanged: Orientation = " + newConfig.orientation);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d("VideoPlayerActivity", "Orientation changed to LANDSCAPE. Entering fullscreen.");
            enterFullscreen();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d("VideoPlayerActivity", "Orientation changed to PORTRAIT. Exiting fullscreen.");
            exitFullscreen();
        }
    }
    private void enterFullscreen() {

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        playerView.setLayoutParams(new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT));

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        isFullscreen = true;
    }

    private void exitFullscreen() {

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        int heightInPixels = (int) (200 * getResources().getDisplayMetrics().density);

        playerView.setLayoutParams(new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                heightInPixels));

        if (getSupportActionBar() != null) {
            getSupportActionBar().show();
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        TextView videoTitle = findViewById(R.id.videoTitle);
        if (videoTitle != null) {
            videoTitle.setVisibility(View.VISIBLE);
        }

        isFullscreen = false;
    }

    private void navigateToVideoPlayerActivity(String videoUrl) {

        String currentUrl = getIntent().getStringExtra("VIDEO_URL");
        if (currentUrl != null && currentUrl.equals(videoUrl)) {
            return;
        }

        Intent intent = new Intent(VideoPlayerActivity.this, VideoPlayerActivity.class);
        intent.putExtra("VIDEO_URL", videoUrl);
        startActivity(intent);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        videoHandler.shutdownExecutor();
    }
}