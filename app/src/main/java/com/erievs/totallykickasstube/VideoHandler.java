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
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
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

    public void startStream(String url, String type) {

        // we want it as a url, http://youtube.be/(videoId), is what do if I just have a video id

        if (TextUtils.isEmpty(url)) {
            showToast("Please enter a valid URL");
            return;
        }

        if (TextUtils.isEmpty(type) || (!type.equals("mp4") && !type.equals("webm"))) {
            type = "mp4";
            Log.d("VideoHandler", "Invalid type provided. Defaulting to 'mp4'.");
        }

        final String finalType = type;

        executorService.execute(() -> {
            try {
                Log.d("VideoHandler", "Requesting stream info for URL: " + url);

                YoutubeDLRequest request = new YoutubeDLRequest(url);

                VideoInfo streamInfo = YoutubeDL.getInstance().getInfo(request);
                Log.d("VideoHandler", "Stream info retrieved for URL: " + url);

                List<VideoFormat> formatsList = streamInfo.getFormats();
                Log.d("VideoHandler", "Number of formats retrieved: " + formatsList.size());

                String selectedVideoUrl = null;
                String selectedAudioUrl = null;
                String muxedUrl = null;

                // will let you change in settings latter
                int[] preferredResolutions = {1280, 854, 640, 480, 360, 240, 144};

                // pretty much what this does is
                // it looks through every format
                // it ignores m3u8, since we cannot filter out the protocol sadly this we gotta check the url
                // it checks the codec, if the type is webm it'll look for opus for audio and vp9 for video
                // while for mp4s it'll look for avc1 for video and well mp4a (I think ACC) for audio (both containers are mp4s)
                // it'll select the highest quality with in the preferredResolutions (we have to check via width as that's what the lib let's us)
                // if that fails we'll look for itag 18, which ALL videos should have, it is a premuxed format
                // itag 18 is ACC for audio, and AVC1 for video (one file).
                // the issue with itag 18 is well, its days are numbered, they used to be itag 22 and more, but they're gone now
                // they're forced unmuxed formats

                // I have plans for AV1 support at some point

                int selectedResolution = -1;

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

                    if (urlStr != null) {
                        if (finalType.equals("mp4")) {
                            if (vcodec != null && vcodec.contains("avc1") && !urlStr.endsWith(".m3u8")) {
                                for (int resolution : preferredResolutions) {
                                    if (width == resolution && width > selectedResolution) {
                                        selectedResolution = width;
                                        selectedVideoUrl = urlStr;
                                        Log.d("VideoHandler", "Selected video URL (" + resolution + "p): " + selectedVideoUrl);
                                        break;
                                    }
                                }
                            }

                            if (acodec != null && acodec.contains("mp4a") && formatId.equals("140")) {
                                if (selectedAudioUrl == null) {
                                    selectedAudioUrl = urlStr;
                                    Log.d("VideoHandler", "Selected audio URL (format 140): " + selectedAudioUrl);
                                }
                            }
                        } else if (finalType.equals("webm")) {
                            if (vcodec != null && vcodec.contains("vp9")) {
                                for (int resolution : preferredResolutions) {
                                    if (width == resolution && width > selectedResolution) {
                                        selectedResolution = width;
                                        selectedVideoUrl = urlStr;
                                        Log.d("VideoHandler", "Selected video URL (" + resolution + "p): " + selectedVideoUrl);
                                        break;
                                    }
                                }
                            }

                            if (acodec != null && acodec.contains("opus")) {
                                if (selectedAudioUrl == null) {
                                    selectedAudioUrl = urlStr;
                                    Log.d("VideoHandler", "Selected audio URL: " + selectedAudioUrl);
                                }
                            }
                        }

                        if (formatId.equals("18")) {
                            muxedUrl = urlStr;
                            Log.d("VideoHandler", "Muxed (itag 18) URL found: " + muxedUrl);
                        }
                    }
                }

                if (!TextUtils.isEmpty(selectedVideoUrl) && !TextUtils.isEmpty(selectedAudioUrl)) {
                    Log.d("VideoHandler", "Video and audio URLs found. Playing video.");
                    playVideo(selectedVideoUrl, selectedAudioUrl, finalType);
                } else if (!TextUtils.isEmpty(muxedUrl) && (TextUtils.isEmpty(selectedVideoUrl) || TextUtils.isEmpty(selectedAudioUrl))) {
                    Log.d("VideoHandler", "Falling back to muxed URL (itag 18). Playing video.");
                    playMuxedVideo(muxedUrl);
                } else {
                    Log.d("VideoHandler", "Failed to find both video and audio streams");
                    showToast("Failed to get stream URL");
                }
            } catch (Exception e) {
                Log.e("VideoHandler", "Error while fetching streams", e);
                showToast("An error occurred while fetching the streams");
            }
        });
    }

    private void playMuxedVideo(String muxedUrl) {
        Log.d("VideoHandler", "Playing muxed video from URL: " + muxedUrl);

        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(Util.getUserAgent(context, "TotallyKickAssTube"))
                .setConnectTimeoutMs(5000)
                .setReadTimeoutMs(5000)
                .setAllowCrossProtocolRedirects(true);

        ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(muxedUrl)));

        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            exoPlayer.setMediaSource(mediaSource);
            exoPlayer.prepare();
            exoPlayer.play();
            Log.d("VideoHandler", "Muxed video stream is playing");
        });
    }

    private void playVideo(String videoUrl, String audioUrl, String type) {
        Log.d("VideoHandler", "Playing video from URL: " + videoUrl);
        Log.d("VideoHandler", "Playing audio from URL: " + audioUrl);

        String videoMimeType = "video/mp4";
        String audioMimeType = "audio/mp4";

        if ("webm".equalsIgnoreCase(type)) {
            videoMimeType = "video/webm";
            audioMimeType = "audio/webm";
        }

        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(Util.getUserAgent(context, "TotallyKickAssTube"))
                .setConnectTimeoutMs(5000)
                .setReadTimeoutMs(5000)
                .setAllowCrossProtocolRedirects(true);


        MediaItem videoItem = new MediaItem.Builder()
                .setUri(Uri.parse(videoUrl))
                .setMimeType(videoMimeType)
                .build();

        MediaItem audioItem = new MediaItem.Builder()
                .setUri(Uri.parse(audioUrl))
                .setMimeType(audioMimeType)
                .build();

        ProgressiveMediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(videoItem);

        ProgressiveMediaSource audioSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(audioItem);

        Log.d("VideoHandler", "Video source: " + videoSource);
        Log.d("VideoHandler", "Audio source: " + audioSource);

        // we gotta merge
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