package com.erievs.totallykickasstube;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;
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
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private static final String BROWSE_POPULAR = "UCF0pVplsI8R5kcAqgtoRqoA";
    private static final String BROWSE_SPORTS = "UCEgdi0XIXXZ-qJOFPf4JSKw";
    private static final String BROWSE_EDUCATION = "UCtFRv9O2AHqOZjjynzrv-xg";
    private static final String BROWSE_FASHION = "UCrpQ4p1Ql_hG8rKXIKM1MOQ";
    private static final String BROWSE_PODCASTS = "FEtopics_more&params=ugdbClkKDUZFdG9waWNzX21vcmUSDwoNRkV0b3BpY3NfbmV3cxIPCg1GRXRvcGljc19saXZlEhEKD0ZFdG9waWNzX3Nwb3J0cxITChFGRXRvcGljc19wb2RjYXN0cw%253D%253D";
    private static final String BROWSE_GAMING = "FEtopics_gaming";
    private boolean isFullscreen = false;

    private static final String PREFERENCES_NAME = "app_preferences";
    private static final String KEY_STREAMING_TYPE = "streaming_type";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

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

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerList = findViewById(R.id.drawer_list);

        String[] drawerMenuItems = getResources().getStringArray(R.array.drawer_menu_items);
        TypedArray drawerMenuIcons = getResources().obtainTypedArray(R.array.drawer_menu_icons);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.list_item_drawer, R.id.drawer_text, drawerMenuItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                ImageView iconView = view.findViewById(R.id.drawer_icon);
                iconView.setImageDrawable(drawerMenuIcons.getDrawable(position));

                return view;
            }
        };

        drawerList.setAdapter(adapter);

        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String browseId = "";

                switch (position) {
                    case 1:
                        browseId = BROWSE_GAMING;
                        break;
                    case 2:
                        browseId = BROWSE_SPORTS;
                        break;
                    case 3:
                        browseId = BROWSE_EDUCATION;
                        break;
                    case 4:
                        browseId = BROWSE_FASHION;
                        break;
                    case 5:
                        Intent settingsIntent = new Intent(VideoPlayerActivity.this, SettingsActivity.class);
                        startActivity(settingsIntent);
                        closeDrawer();
                        return;
                    default:
                        browseId = BROWSE_POPULAR;
                        break;
                }

                Intent intent = new Intent(VideoPlayerActivity.this, MainActivity.class);
                intent.putExtra("BROWSE_ID", browseId);
                startActivity(intent);

                exoPlayer.pause();

                closeDrawer();
            }
        });

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

        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        String streamingType = preferences.getString(KEY_STREAMING_TYPE, "mp4");

        videoHandler.startStream(videoUrl, "webm");

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

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        playerView.setLayoutParams(new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
        ));

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        TextView videoTitle = findViewById(R.id.videoTitle);
        if (videoTitle != null) {
            videoTitle.setVisibility(View.INVISIBLE);
        }

        isFullscreen = true;
    }

    private void exitFullscreen() {

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        int heightInPixels = (int) (200 * getResources().getDisplayMetrics().density);

        playerView.setLayoutParams(new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                heightInPixels
        ));

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

        finish();

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
    private void closeDrawer() {
        drawerLayout.closeDrawers();
    }
}