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

    public RelatedVideosAdapter(List<YouTubeResponse.ContentItem> relatedVideos, OnItemClickListener onItemClickListener) {
        this.relatedVideos = relatedVideos;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_related_video, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YouTubeResponse.ContentItem relatedVideo = relatedVideos.get(position);

        String videoTitle = relatedVideo.videoRenderer.title.simpleText;
        String videoAuthor = relatedVideo.videoRenderer.author;
        String thumbnailUrl = relatedVideo.videoRenderer.thumbnails.thumbnails.get(0).url;

        holder.videoTitle.setText(videoTitle);
        holder.videoAuthor.setText("By " + videoAuthor);

        Picasso.get().load(thumbnailUrl).into(holder.thumbnailImage);

        String videoId = extractVideoIdFromThumbnailUrl(thumbnailUrl);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            private boolean isClicked = false;

            @Override
            public void onClick(View v) {
                if (!isClicked && onItemClickListener != null && videoId != null) {
                    isClicked = true;
                    String videoUrl = "http://youtu.be/" + videoId;
                    onItemClickListener.onItemClick(relatedVideo);

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
        return relatedVideos != null ? relatedVideos.size() : 0;
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView videoTitle;
        TextView videoAuthor;
        ImageView thumbnailImage;

        public ViewHolder(View itemView) {
            super(itemView);
            videoTitle = itemView.findViewById(R.id.videoTitle);
            videoAuthor = itemView.findViewById(R.id.authorTitle);
            thumbnailImage = itemView.findViewById(R.id.thumbnailImage);
        }
    }

    public void updateVideos(List<YouTubeResponse.ContentItem> relatedVideos) {
        this.relatedVideos = relatedVideos;
        notifyDataSetChanged();
    }
    public interface OnItemClickListener {
        void onItemClick(YouTubeResponse.ContentItem relatedVideo);
    }
}
