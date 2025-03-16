package com.erievs.totallykickasstube;

import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
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
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private static final String BROWSE_POPULAR = "UCF0pVplsI8R5kcAqgtoRqoA";
    private static final String BROWSE_SPORTS = "UCEgdi0XIXXZ-qJOFPf4JSKw";
    private static final String BROWSE_EDUCATION = "UCtFRv9O2AHqOZjjynzrv-xg";
    private static final String BROWSE_FASHION = "UCrpQ4p1Ql_hG8rKXIKM1MOQ";
    //private static final String BROWSE_PODCASTS = "FEtopics_more&params=ugdbClkKDUZFdG9waWNzX21vcmUSDwoNRkV0b3BpY3NfbmV3cxIPCg1GRXRvcGljc19saXZlEhEKD0ZFdG9waWNzX3Nwb3J0cxITChFGRXRvcGljc19wb2RjYXN0cw%253D%253D";
    private static final String BROWSE_GAMING = "FEtopics_gaming";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchView = findViewById(R.id.searchView);
        recyclerView = findViewById(R.id.relatedVideosRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        videoList = new ArrayList<>();

        videoAdapter = new SearchVideoAdapter(videoList, this::navigateToVideoPlayerActivity);

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerList = findViewById(R.id.drawer_list);

        String[] drawerMenuItems = getResources().getStringArray(R.array.drawer_menu_items);
        TypedArray drawerMenuIcons = getResources().obtainTypedArray(R.array.drawer_menu_icons);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.list_item_drawer, R.id.drawer_text, drawerMenuItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                ImageView iconView = view.findViewById(R.id.drawer_icon);
                iconView.setImageDrawable(drawerMenuIcons.getDrawable(position));

                return view;
            }
        };

        drawerList.setAdapter(adapter);

        drawerList.setOnItemClickListener((parent, view, position, id) -> {
            String browseId = "";

            switch (position) {
                case 1:
                    browseId = BROWSE_GAMING;
                    break;
                case 2:
                    browseId = BROWSE_SPORTS;
                    break;
                case 3:
                    browseId = BROWSE_EDUCATION;
                    break;
                case 4:
                    browseId = BROWSE_FASHION;
                    break;
                case 5:
                    Intent settingsIntent = new Intent(SearchActivity.this, SettingsActivity.class);
                    startActivity(settingsIntent);
                    closeDrawer();
                    return;
                default:
                    browseId = BROWSE_POPULAR;
                    break;
            }

            Intent intent = new Intent(SearchActivity.this, MainActivity.class);
            intent.putExtra("BROWSE_ID", browseId);
            startActivity(intent);

            closeDrawer();
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
    private void closeDrawer() {
        drawerLayout.closeDrawers();
    }

}