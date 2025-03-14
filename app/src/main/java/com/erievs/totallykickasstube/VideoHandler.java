package com.erievs.totallykickasstube;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoHandler {

    private final ExoPlayer exoPlayer;
    private final Context context;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public VideoHandler(ExoPlayer exoPlayer, Context context) {
        this.exoPlayer = exoPlayer;
        this.context = context;
    }

    public void startStream(String url) {
        if (TextUtils.isEmpty(url)) {
            showToast("Please enter a valid URL");
            return;
        }

        executorService.execute(() -> {
            try {

                YoutubeDLRequest request = new YoutubeDLRequest(url);
                request.addOption("-f", "best");

                String videoUrl = YoutubeDL.getInstance().getInfo(request).getUrl();

                if (TextUtils.isEmpty(videoUrl)) {
                    showToast("Failed to get stream URL");
                } else {

                    playVideo(videoUrl);
                }
            } catch (YoutubeDLException e) {
                Log.e("VideoHandler", "Failed to get stream info", e);
                showToast("Streaming failed. Unable to get stream info");
            } catch (Exception e) {
                Log.e("VideoHandler", "Unexpected error", e);
                showToast("An unexpected error occurred");
            }
        });
    }

    private void playVideo(String videoUrl) {

        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            try {

                MediaItem mediaItem = MediaItem.fromUri(videoUrl);
                exoPlayer.setMediaItem(mediaItem);
                exoPlayer.prepare();
                exoPlayer.play();
            } catch (Exception e) {
                Log.e("VideoHandler", "Error while playing video", e);
                showToast("Error while playing the video");
            }
        });
    }

    private void showToast(String message) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }

    public void shutdownExecutor() {
        executorService.shutdown();
    }
}