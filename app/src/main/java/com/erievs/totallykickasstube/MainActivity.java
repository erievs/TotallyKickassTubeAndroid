package com.erievs.totallykickasstube;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SearchView;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.aria2c.Aria2c;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private SearchView searchView;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // Background thread

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                navigateToSearchActivity(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        executorService.execute(this::initializeYoutubeDL);
    }

    private void initializeYoutubeDL() {
        try {
            // Initialize yt-dl, FFmpeg, and Aria2c
            YoutubeDL.getInstance().init(this);
            FFmpeg.getInstance().init(this);
            Aria2c.getInstance().init(this);

            YoutubeDL.getInstance().updateYoutubeDL(this, YoutubeDL.UpdateChannel._STABLE);

            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "yt-dlp updated successfully!", Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to update yt-dlp", Toast.LENGTH_LONG).show());
        }
    }

    private void navigateToSearchActivity(String query) {
        Intent intent = new Intent(MainActivity.this, SearchActivity.class);
        intent.putExtra("SEARCH_QUERY", query);
        startActivity(intent);
    }

    /*
    private void navigateToVideoPlayerActivity(String videoUrl) {
        Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);
        intent.putExtra("VIDEO_URL", videoUrl);
        startActivity(intent);
    }
    */

}
