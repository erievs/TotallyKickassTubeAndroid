package com.erievs.totallykickasstube;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class HomePageVideoAdapter extends RecyclerView.Adapter<HomePageVideoAdapter.ViewHolder> {

    private List<YouTubeResponse.ContentItem> videoList;
    private OnItemClickListener onItemClickListener;

    public HomePageVideoAdapter(List<YouTubeResponse.ContentItem> videoList, OnItemClickListener onItemClickListener) {
        this.videoList = videoList;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_result_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YouTubeResponse.ContentItem videoItem = videoList.get(position);

        if (videoItem.videoRenderer != null && videoItem.videoRenderer.title != null) {
            String videoTitle = videoItem.videoRenderer.title.simpleText;
            holder.videoTitle.setText(videoTitle);
        } else {
            holder.videoTitle.setText("Unknown Title");
        }

        String thumbnailUrl = null;
        if (videoItem.videoRenderer != null && videoItem.videoRenderer.thumbnails != null
                && !videoItem.videoRenderer.thumbnails.thumbnails.isEmpty()) {
            thumbnailUrl = videoItem.videoRenderer.thumbnails.thumbnails.get(0).url;
        }

        if (thumbnailUrl != null) {
            Picasso.get().load(thumbnailUrl).into(holder.thumbnailImage);
        }

        String videoId = extractVideoIdFromThumbnailUrl(thumbnailUrl);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            private boolean isClicked = false;

            @Override
            public void onClick(View v) {
                if (!isClicked && onItemClickListener != null && videoId != null) {
                    isClicked = true;
                    String videoUrl = "http://youtu.be/" + videoId;
                    onItemClickListener.onItemClick(videoUrl);

                    holder.itemView.postDelayed(() -> isClicked = false, 5000);
                }
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
        return videoList != null ? videoList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView videoTitle;
        ImageView thumbnailImage;

        public ViewHolder(View itemView) {
            super(itemView);
            videoTitle = itemView.findViewById(R.id.videoTitle);
            thumbnailImage = itemView.findViewById(R.id.thumbnailImage);
        }
    }

    public void updateVideos(List<YouTubeResponse.ContentItem> videoList) {
        Log.d("VideoAdapter", "Updating videos: " + videoList.size());
        this.videoList = new ArrayList<>(videoList);
        notifyDataSetChanged();
    }


    public interface OnItemClickListener {
        void onItemClick(String videoUrl);
    }
}