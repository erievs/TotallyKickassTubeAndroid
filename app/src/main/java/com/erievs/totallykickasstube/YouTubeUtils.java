package com.erievs.totallykickasstube;

import android.net.Uri;

public class YouTubeUtils {

    public static String extractVideoId(String url) {
        try {
            Uri uri = Uri.parse(url);

            if (uri.getHost().equals("www.youtube.com") || uri.getHost().equals("youtube.com")) {
                String videoId = uri.getQueryParameter("v");
                if (videoId != null) {
                    return videoId;
                }
            }

            if (uri.getHost().equals("youtu.be")) {
                return uri.getLastPathSegment();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
