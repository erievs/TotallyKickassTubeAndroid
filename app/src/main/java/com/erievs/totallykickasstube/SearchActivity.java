package com.erievs.totallykickasstube;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private static final String TAG = "SearchActivity";
    private SearchView searchView;
    private RecyclerView recyclerView;
    private SearchVideoAdapter videoAdapter;
    private List<YouTubeResponse.ContentItem> videoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchView = findViewById(R.id.searchView);
        recyclerView = findViewById(R.id.relatedVideosRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        videoList = new ArrayList<>();

        videoAdapter = new SearchVideoAdapter(videoList, new SearchVideoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String videoUrl) {
                navigateToVideoPlayerActivity(videoUrl);
            }
        });


        recyclerView.setAdapter(videoAdapter);

        searchView.setIconified(false);
        searchView.clearFocus();

        String searchQuery = getIntent().getStringExtra("SEARCH_QUERY");
        Log.d(TAG, "Received search query: " + searchQuery);

        if (searchQuery != null && !searchQuery.isEmpty()) {
            searchView.setQuery(searchQuery, false);
            fetchSearchResults(searchQuery);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "Search submitted: " + query);
                fetchSearchResults(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "Search text changed: " + newText);
                filterResults(newText);
                return true;
            }
        });
    }

    private void filterResults(String query) {
        List<YouTubeResponse.ContentItem> filteredList = new ArrayList<>();
        for (YouTubeResponse.ContentItem item : videoList) {

            String videoTitle = item.videoRenderer != null && item.videoRenderer.title != null ? item.videoRenderer.title.simpleText : "";
            if (videoTitle != null && videoTitle.toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(item);
            }
        }

        videoAdapter.updateVideos(filteredList);

        if (filteredList.isEmpty()) {
            Log.d(TAG,"No may have been results found, or it is just waiting.");
        }
    }

    private void fetchSearchResults(String query) {

        if (query == null || query.trim().isEmpty()) {
            Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show();
            return;
        }

        YouTubeFetcher youTubeFetcher = new YouTubeFetcher(new YouTubeFetcher.FetchRelatedVideosCallback() {
            @Override
            public void onRelatedVideosFetched(List<YouTubeResponse.ContentItem> searchResults) {
                videoList.clear();
                videoList.addAll(searchResults);
                videoAdapter.updateVideos(videoList);

                if (searchResults.isEmpty()) {
                    Toast.makeText(SearchActivity.this, "No results found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFetchFailed(String errorMessage) {
                Log.e(TAG, "Error fetching search results: " + errorMessage);
                Toast.makeText(SearchActivity.this, "Error fetching results. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });

        youTubeFetcher.fetchSearchResults(query);
    }

    private void navigateToVideoPlayerActivity(String videoUrl) {
        Intent intent = new Intent(SearchActivity.this, VideoPlayerActivity.class);
        intent.putExtra("VIDEO_URL", videoUrl);
        startActivity(intent);
    }


}