package com.erievs.totallykickasstube;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class VideoPlayerActivity extends AppCompatActivity {

    private TextView videoTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        // Initialize the TextView for the video title
        videoTitle = findViewById(R.id.videoTitle); // Make sure to use the correct ID

        // Get video URL and title from intent (if any)
        Intent intent = getIntent();
        String videoUrl = intent.getStringExtra("VIDEO_URL");
        String title = intent.getStringExtra("VIDEO_TITLE");

        // Check if videoUrl is null or empty
        if (videoUrl == null || videoUrl.isEmpty()) {
            Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show();
        }

        // Set the video title if it is not null
        if (title != null) {
            videoTitle.setText(title);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // No player to release, no handlers to shut down
    }

    @Override
    protected void onPause() {
        super.onPause();
        // No player to pause
    }

    @Override
    protected void onStop() {
        super.onStop();
        // No player to release
    }
}
