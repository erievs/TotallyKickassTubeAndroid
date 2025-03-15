package com.erievs.totallykickasstube;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoFormat;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.util.List;
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
                Log.d("VideoHandler", "Requesting stream info for URL: " + url);

                YoutubeDLRequest request = new YoutubeDLRequest(url);
                VideoInfo streamInfo = YoutubeDL.getInstance().getInfo(request);
                Log.d("VideoHandler", "Stream info retrieved for URL: " + url);

                List<VideoFormat> formatsList = streamInfo.getFormats();
                Log.d("VideoHandler", "Number of formats retrieved: " + formatsList.size());

                String videoUrl = null;
                String audioUrl = null;

                int[] preferredResolutions = {1280, 854, 640, 480, 360, 240, 144};

                String selectedVideoUrl = null;
                String selectedAudioUrl = null;

                for (VideoFormat format : formatsList) {
                    String formatId = format.getFormatId();
                    String urlStr = format.getUrl();
                    String vcodec = format.getVcodec();
                    String acodec = format.getAcodec();
                    int width = format.getWidth();

                    Log.d("VideoHandler", "Format ID: " + formatId);
                    Log.d("VideoHandler", "Video Codec: " + vcodec);
                    Log.d("VideoHandler", "Audio Codec: " + acodec);
                    Log.d("VideoHandler", "URL: " + urlStr);
                    Log.d("VideoHandler", "Width: " + width);

                    if (urlStr != null && vcodec != null && vcodec.contains("avc1") && !urlStr.endsWith(".m3u8")) {
                        for (int resolution : preferredResolutions) {
                            if (width == resolution && selectedVideoUrl == null) {
                                selectedVideoUrl = urlStr;
                                Log.d("VideoHandler", "Selected video URL (" + resolution + "p): " + selectedVideoUrl);
                                break;
                            }
                        }
                    }

                    if (urlStr != null && acodec != null && acodec.contains("mp4a") && formatId.equals("140")) {
                        if (selectedAudioUrl == null) {
                            selectedAudioUrl = urlStr;
                            Log.d("VideoHandler", "Selected audio URL (format 140): " + selectedAudioUrl);
                        }
                    }

                    if (urlStr != null && acodec != null && acodec.contains("mp4a") && selectedAudioUrl == null) {
                        selectedAudioUrl = urlStr;
                        Log.d("VideoHandler", "Fallback audio URL (format mp4a): " + selectedAudioUrl);
                    }
                }

                if (TextUtils.isEmpty(selectedVideoUrl) || TextUtils.isEmpty(selectedAudioUrl)) {
                    Log.d("VideoHandler", "Failed to find both video and audio streams");
                    showToast("Failed to get stream URL");
                } else {
                    Log.d("VideoHandler", "Video and audio URLs found. Playing video.");
                    playVideo(selectedVideoUrl, selectedAudioUrl);
                }

            } catch (Exception e) {
                Log.e("VideoHandler", "Error while fetching streams", e);
                showToast("An error occurred while fetching the streams");
            }
        });
    }

    private void playVideo(String videoUrl, String audioUrl) {
        Log.d("VideoHandler", "Playing video from URL: " + videoUrl);
        Log.d("VideoHandler", "Playing audio from URL: " + audioUrl);

        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(Util.getUserAgent(context, "TotallyKickAssTube"));

        MediaItem videoItem = new MediaItem.Builder()
                .setUri(Uri.parse(videoUrl))
                .setMimeType("video/mp4")
                .build();

        MediaItem audioItem = new MediaItem.Builder()
                .setUri(Uri.parse(audioUrl))
                .setMimeType("audio/mp4")
                .build();

        ProgressiveMediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(videoItem);

        ProgressiveMediaSource audioSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(audioItem);

        Log.d("VideoHandler", "Video source: " + videoSource);
        Log.d("VideoHandler", "Audio source: " + audioSource);

        MergingMediaSource mergedSource = new MergingMediaSource(videoSource, audioSource);

        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            exoPlayer.setMediaSource(mergedSource);
            exoPlayer.prepare();
            exoPlayer.play();
            Log.d("VideoHandler", "Video and audio streams are playing together");
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