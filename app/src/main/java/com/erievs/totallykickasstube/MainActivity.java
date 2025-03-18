package com.erievs.totallykickasstube;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SearchView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.aria2c.Aria2c;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private SearchView searchView;
    private RecyclerView recyclerView;
    private HomePageVideoAdapter videoAdapter;
    private List<YouTubeResponse.ContentItem> videoList;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private static final String PREFERENCES_NAME = "app_preferences";
    private static final String KEY_YT_DLP_BRANCH = "yt_dlp_branch";
    private static final String BROWSE_POPULAR = "UCF0pVplsI8R5kcAqgtoRqoA";
    private static final String BROWSE_SPORTS = "UCEgdi0XIXXZ-qJOFPf4JSKw";
    private static final String BROWSE_EDUCATION = "UCtFRv9O2AHqOZjjynzrv-xg";
    private static final String BROWSE_FASHION = "UCrpQ4p1Ql_hG8rKXIKM1MOQ";
    private static final String BROWSE_SPOTLIGHT = "UCBR8-60-B28hp2BmDPdntcQ";
    private static final String BROWSE_GAMING = "FEtopics_gaming";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        recyclerView = findViewById(R.id.homePageVideosRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        videoList = new ArrayList<>();

        videoAdapter = new HomePageVideoAdapter(videoList, new HomePageVideoAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String videoUrl) {
                navigateToVideoPlayerActivity(videoUrl);
            }
        });

        recyclerView.setAdapter(videoAdapter);

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

        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
                        browseId = BROWSE_SPOTLIGHT;
                        break;
                    case 6:
                        Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(settingsIntent);
                        closeDrawer();
                        return;
                    default:
                        browseId = BROWSE_POPULAR;
                        break;
                }

                fetchBrowseVideos(browseId);
                closeDrawer();
            }
        });

        String browseId = getIntent().getStringExtra("BROWSE_ID");

        if (browseId == null) {
            browseId = BROWSE_POPULAR;
        }

        fetchBrowseVideos(browseId);

        searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                navigateToSearchActivity(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        executorService.execute(this::initializeYoutubeDL);
    }

    private void initializeYoutubeDL() {
        try {

            SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
            String ytDlpBranch = preferences.getString(KEY_YT_DLP_BRANCH, "NIGHTLY");

            YoutubeDL.getInstance().init(this);
            FFmpeg.getInstance().init(this);
            Aria2c.getInstance().init(this);

            switch (ytDlpBranch) {
                case "STABLE":
                    YoutubeDL.getInstance().updateYoutubeDL(this, YoutubeDL.UpdateChannel._STABLE);
                    break;
                case "MASTER":
                    YoutubeDL.getInstance().updateYoutubeDL(this, YoutubeDL.UpdateChannel._MASTER);
                    break;
                case "NIGHTLY":
                default:
                    YoutubeDL.getInstance().updateYoutubeDL(this, YoutubeDL.UpdateChannel._NIGHTLY);
                    break;
            }

            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "yt-dlp updated successfully!", Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {

            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to update yt-dlp", Toast.LENGTH_LONG).show());
        }
    }
    private void fetchBrowseVideos(String browseID) {

        if (browseID == null || browseID.trim().isEmpty()) {
            Toast.makeText(this, "Please enter a browseID", Toast.LENGTH_SHORT).show();
            return;
        }

        YouTubeFetcher youTubeFetcher = new YouTubeFetcher(new YouTubeFetcher.FetchRelatedVideosCallback() {
            @Override
            public void onRelatedVideosFetched(List<YouTubeResponse.ContentItem> searchResults) {
                videoList.clear();
                videoList.addAll(searchResults);
                videoAdapter.updateVideos(videoList);

                if (searchResults.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No videos found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFetchFailed(String errorMessage) {
                Log.e(TAG, "Error fetching search results: " + errorMessage);
                Toast.makeText(MainActivity.this, "Error fetching videos. Please try again.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVideoDetailsFetched(YouTubeResponse.VideoDetails videoDetails) {

            }

        });

        youTubeFetcher.fetchBrowseVideos(browseID);
    }

    private void navigateToSearchActivity(String query) {
        Intent intent = new Intent(MainActivity.this, SearchActivity.class);
        intent.putExtra("SEARCH_QUERY", query);
        startActivity(intent);
    }
    private void navigateToVideoPlayerActivity(String videoUrl) {
        Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);
        intent.putExtra("VIDEO_URL", videoUrl);

        startActivity(intent);
    }
    private void closeDrawer() {
        drawerLayout.closeDrawers();
    }

}
