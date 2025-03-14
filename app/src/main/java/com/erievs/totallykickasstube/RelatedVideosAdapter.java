package com.erievs.totallykickasstube;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class RelatedVideosAdapter extends RecyclerView.Adapter<RelatedVideosAdapter.ViewHolder> {

    private List<YouTubeResponse.ContentItem> relatedVideos;
    private OnItemClickListener onItemClickListener;

    // Constructor
    public RelatedVideosAdapter(List<YouTubeResponse.ContentItem> relatedVideos, OnItemClickListener onItemClickListener) {
        this.relatedVideos = relatedVideos;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use your existing layout for related videos
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_related_video, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YouTubeResponse.ContentItem relatedVideo = relatedVideos.get(position);
        String videoTitle = relatedVideo.videoRenderer.title.simpleText;
        String thumbnailUrl = relatedVideo.videoRenderer.thumbnails.thumbnails.get(0).url;

        holder.videoTitle.setText(videoTitle);

        // Load the thumbnail using Picasso
        Picasso.get().load(thumbnailUrl).into(holder.thumbnailImage);

        // Extract video ID from the video URL
        String videoId = extractVideoIdFromThumbnailUrl(thumbnailUrl);

        // Set up the click listener for each item
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null && videoId != null) {
                String videoUrl = "http://youtu.be/" + videoId; // Construct the video URL
                onItemClickListener.onItemClick(relatedVideo); // Pass the related video
            }
        });
    }

    private String extractVideoIdFromThumbnailUrl(String thumbnailUrl) {
        String videoId = null;
        if (thumbnailUrl != null && thumbnailUrl.contains("/vi/")) {
            int startIdx = thumbnailUrl.indexOf("/vi/") + 4;
            int endIdx = thumbnailUrl.indexOf("/hqdefault.jpg");
            if (startIdx > 0 && endIdx > startIdx) {
                videoId = thumbnailUrl.substring(startIdx, endIdx);
            }
        }
        return videoId;
    }

    @Override
    public int getItemCount() {
        return relatedVideos != null ? relatedVideos.size() : 0;
    }

    // ViewHolder class for related video items
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView videoTitle;
        ImageView thumbnailImage;

        public ViewHolder(View itemView) {
            super(itemView);
            videoTitle = itemView.findViewById(R.id.videoTitle); // Use the existing TextView
            thumbnailImage = itemView.findViewById(R.id.thumbnailImage); // Use the existing ImageView
        }
    }

    // Method to update the list of videos
    public void updateVideos(List<YouTubeResponse.ContentItem> relatedVideos) {
        this.relatedVideos = relatedVideos;
        notifyDataSetChanged();
    }

    // Define the click listener interface
    public interface OnItemClickListener {
        void onItemClick(YouTubeResponse.ContentItem relatedVideo);
    }
}
