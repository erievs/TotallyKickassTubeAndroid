package com.erievs.totallykickasstube;

import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public class YouTubeFetcher {

    // you can also use googleapi end point, but this is nicer looking
    private static final String BASE_URL = "https://www.youtube.com/youtubei/v1/";

    // idk if this is REALLY needed I always do just in case
    private static final String API_KEY = "AIzaSyDCU8hByM-4DrUqRUYnGn-3llEO78bcxq8";
    private static final String TAG = "YouTubeFetcher";

    private YouTubeApiService apiService;
    private FetchRelatedVideosCallback callback;
    private ObjectMapper objectMapper;

    // too lazy to change the name to fit what we do here now
    public YouTubeFetcher(FetchRelatedVideosCallback callback) {

        objectMapper = new ObjectMapper();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .build();

        apiService = retrofit.create(YouTubeApiService.class);
        this.callback = callback;
    }
    public interface FetchVideoTitleCallback {
        void onTitleFetched(String title);
    }

    // going to improve this later, probbaly just use /next and get the description and such
    // I was just lazy and wanted to add the title
    public static void fetchVideoTitle(String videoUrl, FetchVideoTitleCallback callback) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                String videoTitle = null;
                try {
                    VideoInfo streamInfo = YoutubeDL.getInstance().getInfo(params[0]);
                    videoTitle = streamInfo.getTitle();
                } catch (YoutubeDLException e) {
                    Log.e("VideoFetcher", "Error fetching video info: " + e.getMessage());
                } catch (YoutubeDL.CanceledException | InterruptedException e) {
                    Log.e("VideoFetcher", "Video fetching canceled or interrupted: " + e.getMessage());

                }
                return videoTitle;
            }

            @Override
            protected void onPostExecute(String title) {

                if (callback != null) {
                    callback.onTitleFetched(title);
                }
            }
        }.execute(videoUrl);
    }
    public interface YouTubeApiService {
        @Headers({
                "Content-Type: application/json",
                "Accept: application/json"
        })

        // fyi the next can do a whole lot more, get the description (iirc), author profiles and such
        @POST("next")
        Call<JsonNode> getRelatedVideos(@Query("key") String apiKey, @Body RequestBody body);

        @POST("search")
        Call<JsonNode> getSearchResults(@Query("key") String apiKey, @Body RequestBody body);

        // browse is USED a lot, it is used for channels, topics, playlists, and probbaly more.
        // with that being said, you use search to search stuff
        @POST("browse")
        Call<JsonNode> getBrowseVideos(@Query("key") String apiKey, @Body RequestBody body);

    }
    public void fetchBrowseVideos(String browseId) {
        Log.d(TAG, "Fetching browse results for browseId: " + browseId);

        JsonNode postData = createPostDataBrowse(browseId);
        if (postData == null) {
            Log.e(TAG, "Error creating POST data for browse request.");
            callback.onFetchFailed("Failed to create request data.");
            return;
        }

        Log.d(TAG, "Post data for browse request: " + postData.toString());

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"), postData.toString()
        );

        Log.d(TAG, "Sending API request with POST data: " + postData.toString());

        apiService.getBrowseVideos(API_KEY, requestBody).enqueue(new Callback<JsonNode>() {
            @Override
            public void onResponse(Call<JsonNode> call, Response<JsonNode> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Response received: " + response.body().toString());

                    List<YouTubeResponse.ContentItem> browseResults = extractBrowseVideos(response.body());

                    if (browseResults != null && !browseResults.isEmpty()) {
                        Log.d(TAG, "Extracted browse results: " + browseResults.size() + " items found.");
                        callback.onRelatedVideosFetched(browseResults);
                    } else {
                        Log.d(TAG, "No browse results found.");
                        callback.onFetchFailed("No browse results found.");
                    }
                } else {
                    Log.e(TAG, "Failed to fetch browse results: " + response.message());
                    callback.onFetchFailed("Failed to fetch browse results.");
                }
            }

            @Override
            public void onFailure(Call<JsonNode> call, Throwable t) {
                Log.e(TAG, "Error fetching browse results: " + t.getMessage(), t);
                callback.onFetchFailed("API call failed: " + t.getMessage());
            }
        });
    }
    private JsonNode createPostDataBrowse(String browseId) {

        // you need send the right client contex for the the data
        // I use TV html, since I am used to it, and you can easly get
        // access_codes for it (idk if sign in will be public since I don't want google to fuck me up)

        String jsonString = "{"
                + "\"context\": {"
                + "    \"client\": {"
                + "        \"screenWidthPoints\": 1920,"
                + "        \"screenHeightPoints\": 1050,"
                + "        \"screenPixelDensity\": 1,"
                + "        \"utcOffsetMinutes\": -300,"
                + "        \"hl\": \"en\","
                + "        \"gl\": \"US\","
                + "        \"deviceMake\": \"Samsung\","
                + "        \"deviceModel\": \"SmartTV\","
                + "        \"visitorData\": \"CgsxVi1janRGNC02TSjp8rC9BjIKCgJVUxIEGgAgMQ%3D%3D\","
                + "        \"userAgent\": \"Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/5.0 NativeTVAds Safari/538.1,gzip(gfe)\","
                + "        \"clientName\": \"TVHTML5\","
                + "        \"clientVersion\": \"7.20250210.17.00\","
                + "        \"osName\": \"Tizen\","
                + "        \"osVersion\": \"5.0\","
                + "        \"originalUrl\": \"https://www.youtube.com/tv\","
                + "        \"theme\": \"CLASSIC\","
                + "        \"platform\": \"TV\","
                + "        \"clientFormFactor\": \"UNKNOWN_FORM_FACTOR\","
                + "        \"webpSupport\": false"
                + "    },"
                + "    \"user\": { \"enableSafetyMode\": false },"
                + "    \"request\": { \"internalExperimentFlags\": [], \"consistencyTokenJars\": [] }"
                + "},"
                + "\"browseId\": \"" + browseId + "\","
                + "\"params\": \"EgIQAQ%3D%3D\""
                + "}";

        try {
            return objectMapper.readTree(jsonString);
        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON request body: " + e.getMessage());
            return null;
        }
    }
    private List<YouTubeResponse.ContentItem> extractBrowseVideos(JsonNode response) {
        List<YouTubeResponse.ContentItem> relatedVideos = new ArrayList<>();
        List<JsonNode> tileRenderers = new ArrayList<>();

        // this is a little different than the rest since
        // you have to deal with mutiple entry points for channels and stuff

        try {

            Log.d(TAG, "Full JSON Response: " + response.toString());

            JsonNode topicRenderer = response.at("/contents/tvBrowseRenderer/content/tvSurfaceContentRenderer/content/sectionListRenderer/contents/0/shelfRenderer/content/horizontalListRenderer");

            JsonNode specialChannelSections = response.at("/contents/tvBrowseRenderer/content/tvSecondaryNavRenderer/sections/0/tvSecondaryNavSectionRenderer/tabs/0/tabRenderer/content/tvSurfaceContentRenderer/content/sectionListRenderer/contents");

            JsonNode items = topicRenderer.path("items");
            boolean isTopic = !items.isMissingNode();

            if (!isTopic) {
                Log.w(TAG, "No shelfRenderer found, checking itemSectionRenderer for special channels.");
                items = null;
            }

            if (items == null) {

                for (JsonNode section : specialChannelSections) {
                    JsonNode sectionItems = section.path("itemSectionRenderer").path("contents");
                    if (sectionItems.isArray()) {
                        for (JsonNode item : sectionItems) {
                            if (item.has("tileRenderer")) {
                                processTileRenderer(item.path("tileRenderer"), relatedVideos, tileRenderers);
                            }
                        }
                    }
                }
            } else if (items.isArray()) {

                for (JsonNode item : items) {
                    JsonNode videoNode = item.path("tileRenderer");
                    if (!videoNode.isMissingNode()) {
                        processTileRenderer(videoNode, relatedVideos, tileRenderers);
                    }
                }
            }

            Log.d(TAG, "Total extracted browse videos: " + relatedVideos.size());
        } catch (Exception e) {
            Log.e(TAG, "Error parsing browse videos JSON: " + e.getMessage(), e);
        }

        return relatedVideos;
    }
    private void processTileRenderer(JsonNode videoNode, List<YouTubeResponse.ContentItem> relatedVideos, List<JsonNode> tileRenderers) {
        if (videoNode.isObject()) {
            tileRenderers.add(videoNode);
            Log.d(TAG, "Logged TileRenderer: " + videoNode.toString());

            String videoId = videoNode.path("onSelectCommand")
                    .path("watchEndpoint")
                    .path("videoId")
                    .asText();
            Log.d(TAG, "Video ID: " + videoId);

            String title = videoNode.path("metadata")
                    .path("tileMetadataRenderer")
                    .path("title")
                    .path("simpleText").asText();
            Log.d(TAG, "Title: " + title);

            String author = videoNode.path("metadata")
                    .path("tileMetadataRenderer")
                    .path("lines")
                    .path(0)
                    .path("lineRenderer")
                    .path("items")
                    .path(0)
                    .path("lineItemRenderer")
                    .path("text")
                    .path("runs")
                    .path(0)
                    .path("text").asText();

            Log.d(TAG, "Author: " + author);

            String thumbnailUrl = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
            Log.d(TAG, "Thumbnail URL: " + thumbnailUrl);

            relatedVideos.add(new YouTubeResponse.ContentItem(videoId, title, thumbnailUrl, author));
        } else {
            Log.w(TAG, "TileRenderer is null or not an object: " + videoNode.toString());
        }
    }
    public void fetchSearchResults(String query) {
        Log.d(TAG, "Fetching search results for query: " + query);

        JsonNode postData = createPostDataSearch(query);
        Log.d(TAG, "Post data for search request: " + postData.toString());

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"), postData.toString()
        );

        Log.d(TAG, "Sending API request with POST data: " + postData.toString());

        apiService.getSearchResults(API_KEY, requestBody).enqueue(new Callback<JsonNode>() {
            @Override
            public void onResponse(Call<JsonNode> call, Response<JsonNode> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Response received: " + response.body().toString());

                    List<YouTubeResponse.ContentItem> searchResults = extractSearchResults(response.body());

                    Log.d(TAG, "Extracted search results: " + searchResults.size() + " items found.");

                    if (searchResults != null && !searchResults.isEmpty()) {
                        callback.onRelatedVideosFetched(searchResults);
                        Log.d(TAG, "Search results successfully fetched.");
                    } else {
                        callback.onFetchFailed("No search results found.");
                        Log.d(TAG, "No search results found.");
                    }
                } else {
                    Log.e(TAG, "Failed to fetch search results: " + response.message());
                    callback.onFetchFailed("Failed to fetch search results.");
                }
            }

            @Override
            public void onFailure(Call<JsonNode> call, Throwable t) {
                Log.e(TAG, "Error fetching search results: " + t.getMessage(), t);
                callback.onFetchFailed("API call failed: " + t.getMessage());
            }
        });
    }
    private JsonNode createPostDataSearch(String query) {
        String jsonString = "{"
                + "\"context\": {"
                + "    \"client\": {"
                + "        \"screenWidthPoints\": 1920,"
                + "        \"screenHeightPoints\": 1050,"
                + "        \"screenPixelDensity\": 1,"
                + "        \"utcOffsetMinutes\": -300,"
                + "        \"hl\": \"en\","
                + "        \"gl\": \"US\","
                + "        \"deviceMake\": \"Samsung\","
                + "        \"deviceModel\": \"SmartTV\","
                + "        \"visitorData\": \"CgsxVi1janRGNC02TSjp8rC9BjIKCgJVUxIEGgAgMQ%3D%3D\","
                + "        \"userAgent\": \"Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/5.0 NativeTVAds Safari/538.1,gzip(gfe)\","
                + "        \"clientName\": \"TVHTML5\","
                + "        \"clientVersion\": \"7.20250210.17.00\","
                + "        \"osName\": \"Tizen\","
                + "        \"osVersion\": \"5.0\","
                + "        \"originalUrl\": \"https://www.youtube.com/tv\","
                + "        \"theme\": \"CLASSIC\","
                + "        \"platform\": \"TV\","
                + "        \"clientFormFactor\": \"UNKNOWN_FORM_FACTOR\","
                + "        \"webpSupport\": false"
                + "    },"
                + "    \"user\": { \"enableSafetyMode\": false },"
                + "    \"request\": { \"internalExperimentFlags\": [], \"consistencyTokenJars\": [] }"
                + "},"
                + "\"query\": \"" + query + "\","
                + "\"params\": \"6gILVWtfVEZTVG5MTmvqAgtxS3A1RkJDZjh1WeoCC181OXkwRzdhczFN6gILOEhfT19mUkFTd0X6AgpMb2NhbCBuZXdz\""
                + "}";

        try {
            return objectMapper.readTree(jsonString);
        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON request body: " + e.getMessage());
            return null;
        }
    }
    private List<YouTubeResponse.ContentItem> extractSearchResults(JsonNode response) {
        List<YouTubeResponse.ContentItem> searchResults = new ArrayList<>();

        try {
            JsonNode sectionListRenderer = response.path("contents").path("sectionListRenderer").path("contents");

            if (sectionListRenderer.isArray()) {
                for (JsonNode section : sectionListRenderer) {
                    JsonNode shelfRenderer = section.path("shelfRenderer");
                    if (!shelfRenderer.isMissingNode()) {
                        JsonNode horizontalListRenderer = shelfRenderer.path("content").path("horizontalListRenderer");
                        JsonNode items = horizontalListRenderer.path("items");

                        if (items.isArray()) {
                            for (JsonNode item : items) {
                                JsonNode tileRenderer = item.path("tileRenderer");
                                if (!tileRenderer.isMissingNode() && tileRenderer.isObject()) {

                                    String videoId = tileRenderer.path("onSelectCommand")
                                            .path("watchEndpoint")
                                            .path("videoId")
                                            .asText();

                                    String title = tileRenderer.path("metadata")
                                            .path("tileMetadataRenderer")
                                            .path("title")
                                            .path("simpleText").asText();

                                    String author = tileRenderer.path("metadata")
                                            .path("tileMetadataRenderer")
                                            .path("lines")
                                            .path(0)
                                            .path("lineRenderer")
                                            .path("items")
                                            .path(0)
                                            .path("lineItemRenderer")
                                            .path("text")
                                            .path("runs")
                                            .path(0)
                                            .path("text").asText();

                                    String thumbnailUrl = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";

                                    if (!videoId.isEmpty() && !thumbnailUrl.isEmpty()) {
                                        Log.d(TAG, "Video ID: " + videoId);
                                        Log.d(TAG, "Title: " + title);
                                        Log.d(TAG, "Author: " + author);
                                        Log.d(TAG, "Thumbnail URL: " + thumbnailUrl);

                                        searchResults.add(new YouTubeResponse.ContentItem(videoId, title, thumbnailUrl, author));
                                    } else {
                                        Log.d(TAG, "Skipping item due to missing videoId or thumbnail");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing search results JSON: " + e.getMessage(), e);
        }

        Log.d(TAG, "Total search results: " + searchResults.size());

        return searchResults;
    }
    public interface FetchRelatedVideosCallback {
        void onRelatedVideosFetched(List<YouTubeResponse.ContentItem> relatedVideos);
        void onFetchFailed(String errorMessage);
    }
    public void fetchRelatedVideos(String videoId) {
        Log.d(TAG, "Fetching related videos for videoId: " + videoId);

        JsonNode postData = createPostDataRelatedVideos(videoId);
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"), postData.toString()
        );

        apiService.getRelatedVideos(API_KEY, requestBody).enqueue(new Callback<JsonNode>() {

            @Override
            public void onResponse(Call<JsonNode> call, Response<JsonNode> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Response body: " + response.body().toString());
                    List<YouTubeResponse.ContentItem> relatedVideos = extractRelatedVideos(response.body());
                    if (relatedVideos != null && !relatedVideos.isEmpty()) {
                        callback.onRelatedVideosFetched(relatedVideos);
                    } else {
                        callback.onFetchFailed("No related videos found.");
                    }
                } else {
                    String errorBody = response.errorBody() != null ? response.errorBody().toString() : "Unknown error";
                    Log.e(TAG, "Failed to fetch related videos: " + response.message() + " | " + errorBody);
                    callback.onFetchFailed("Failed to fetch related videos: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<JsonNode> call, Throwable t) {
                Log.e(TAG, "Error fetching related videos: " + t.getMessage());
                callback.onFetchFailed("API call failed: " + t.getMessage());
            }
        });
    }
    private JsonNode createPostDataRelatedVideos(String videoId) {
        String jsonString = "{"
                + "\"context\": {"
                + "    \"client\": {"
                + "        \"screenWidthPoints\": 1920,"
                + "        \"screenHeightPoints\": 1050,"
                + "        \"screenPixelDensity\": 1,"
                + "        \"utcOffsetMinutes\": -300,"
                + "        \"hl\": \"en\","
                + "        \"gl\": \"US\","
                + "        \"deviceMake\": \"Samsung\","
                + "        \"deviceModel\": \"SmartTV\","
                + "        \"visitorData\": \"CgsxVi1janRGNC02TSjp8rC9BjIKCgJVUxIEGgAgMQ%3D%3D\","
                + "        \"userAgent\": \"Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/5.0 NativeTVAds Safari/538.1,gzip(gfe)\","
                + "        \"clientName\": \"TVHTML5\","
                + "        \"clientVersion\": \"7.20250210.17.00\","
                + "        \"osName\": \"Tizen\","
                + "        \"osVersion\": \"5.0\","
                + "        \"originalUrl\": \"https://www.youtube.com/tv\","
                + "        \"theme\": \"CLASSIC\","
                + "        \"platform\": \"TV\","
                + "        \"clientFormFactor\": \"UNKNOWN_FORM_FACTOR\","
                + "        \"webpSupport\": false"
                + "    },"
                + "    \"user\": { \"enableSafetyMode\": false },"
                + "    \"request\": { \"internalExperimentFlags\": [], \"consistencyTokenJars\": [] }"
                + "},"
                + "\"videoId\": \"" + videoId + "\","
                + "\"racyCheckOk\": true,"
                + "\"contentCheckOk\": true,"
                + "\"playbackContext\": { \"lactMilliseconds\": \"528\" },"
                + "\"autonavState\": \"STATE_NONE\""
                + "}";

        try {
            return objectMapper.readTree(jsonString);
        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON request body: " + e.getMessage());
            return null;
        }
    }
    private List<YouTubeResponse.ContentItem> extractRelatedVideos(JsonNode response) {
        List<YouTubeResponse.ContentItem> relatedVideos = new ArrayList<>();
        Set<String> videoIdsSet = new HashSet<>();

        // you have to look though some shelf renders
        // since how on the tv app, they're 3 videos per row
        // if you just pick the first one you're stuck with 3 videos and that's lame

        try {

            Log.d(TAG, "Full JSON Response: " + response.toString());

            JsonNode contents = response.path("contents")
                    .path("singleColumnWatchNextResults")
                    .path("pivot")
                    .path("sectionListRenderer")
                    .path("contents");

            if (contents.isArray()) {
                Log.d(TAG, "Total 'shelfRenderer' sections found: " + contents.size());

                for (JsonNode section : contents) {

                    Log.d(TAG, "Section Node: " + section.toString());

                    JsonNode shelfRenderer = section.path("shelfRenderer");
                    if (!shelfRenderer.isNull()) {
                        JsonNode horizontalListRenderer = shelfRenderer.path("content")
                                .path("horizontalListRenderer");

                        if (!horizontalListRenderer.isNull()) {

                            Log.d(TAG, "HorizontalListRenderer Node: " + horizontalListRenderer.toString());

                            JsonNode items = horizontalListRenderer.path("items");
                            if (items.isArray()) {
                                int totalVideos = items.size();
                                Log.d(TAG, "Total videos found in 'items': " + totalVideos);

                                for (JsonNode item : items) {
                                    Log.d(TAG, "Item Node: " + item.toString());

                                    JsonNode videoNode = item.path("tileRenderer");
                                    if (!videoNode.isNull() && videoNode.isObject()) {

                                        String videoId = videoNode.path("onSelectCommand")
                                                .path("watchEndpoint")
                                                .path("videoId")
                                                .asText();

                                        if (videoIdsSet.contains(videoId)) {
                                            continue;
                                        }

                                        videoIdsSet.add(videoId);

                                        Log.d(TAG, "Video ID: " + videoId);

                                        String title = videoNode.path("metadata")
                                                .path("tileMetadataRenderer")
                                                .path("title")
                                                .path("simpleText").asText();
                                        Log.d(TAG, "Title: " + title);

                                        String author = videoNode.path("metadata")
                                                .path("tileMetadataRenderer")
                                                .path("lines")
                                                .path(0)
                                                .path("lineRenderer")
                                                .path("items")
                                                .path(0)
                                                .path("lineItemRenderer")
                                                .path("text")
                                                .path("runs")
                                                .path(0)
                                                .path("text").asText();
                                        Log.d(TAG, "Author: " + author);

                                        String thumbnailUrl = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
                                        Log.d(TAG, "Thumbnail URL: " + thumbnailUrl);

                                        relatedVideos.add(new YouTubeResponse.ContentItem(videoId, title, thumbnailUrl, author));
                                    } else {
                                        Log.w(TAG, "TileRenderer is null or not an object in item: " + item.toString());
                                    }
                                }
                            } else {
                                Log.w(TAG, "'items' is not an array in horizontalListRenderer: " + horizontalListRenderer.toString());
                            }
                        }
                    }
                }
            } else {
                Log.w(TAG, "'contents' is not an array in the response: " + response.toString());
            }

            Log.d(TAG, "Collected TileRenderers: " + relatedVideos.toString());

        } catch (Exception e) {
            Log.e(TAG, "Error parsing related videos JSON: " + e.getMessage(), e);
        }

        return relatedVideos;
    }

}