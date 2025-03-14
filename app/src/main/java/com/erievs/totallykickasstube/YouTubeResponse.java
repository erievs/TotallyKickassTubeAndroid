package com.erievs.totallykickasstube;

import java.util.List;

public class YouTubeResponse {
    public Contents contents;

    public static class Contents {
        public SingleColumnWatchNextResults singleColumnWatchNextResults;
    }

    public static class SingleColumnWatchNextResults {
        public Results results;
    }

    public static class Results {
        public List<ContentItem> contents;
    }

    public static class ContentItem {
        public VideoRenderer videoRenderer;

        public ContentItem(String videoId, String title, String thumbnailUrl) {
            this.videoRenderer = new VideoRenderer(videoId, title, thumbnailUrl);
        }
    }

    public static class VideoRenderer {
        public String videoId;
        public Thumbnail thumbnails;
        public Title title;

        public VideoRenderer(String videoId, String title, String thumbnailUrl) {
            this.videoId = videoId;
            this.title = new Title(title);
            this.thumbnails = new Thumbnail(thumbnailUrl);
        }
    }

    public static class Thumbnail {
        public List<ThumbnailItem> thumbnails;

        public Thumbnail(String url) {
            this.thumbnails = List.of(new ThumbnailItem(url));
        }
    }

    public static class ThumbnailItem {
        public String url;

        public ThumbnailItem(String url) {
            this.url = url;
        }
    }

    public static class Title {
        public String simpleText;

        public Title(String text) {
            this.simpleText = text;
        }
    }
}