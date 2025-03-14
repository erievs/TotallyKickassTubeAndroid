package com.erievs.totallykickasstube;

public class ContentItem {
    private String title;
    private String thumbnailUrl;

    // Constructor
    public ContentItem(String title, String thumbnailUrl) {
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
    }

    // Getter for title
    public String getTitle() {
        return title;
    }

    // Getter for thumbnail URL
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
}
