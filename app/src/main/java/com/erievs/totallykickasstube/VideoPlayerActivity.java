package com.erievs.totallykickasstube;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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