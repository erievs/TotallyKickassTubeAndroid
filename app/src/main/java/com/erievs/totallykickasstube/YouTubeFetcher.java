package com.erievs.totallykickasstube;

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

import java.util.List;
import java.util.ArrayList;

public class YouTubeFetcher {
    private static final String BASE_URL = "https://www.youtube.com/youtubei/v1/";
    private static final String API_KEY = "AIzaSyDCU8hByM-4DrUqRUYnGn-3llEO78bcxq8";
    private static final String TAG = "YouTubeFetcher";

    private YouTubeApiService apiService;
    private FetchRelatedVideosCallback callback;
    private ObjectMapper objectMapper;

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
    public void fetchVideoTitle(String videoUrl, FetchVideoTitleCallback callback) {
        try {

            VideoInfo streamInfo = YoutubeDL.getInstance().getInfo(videoUrl);

            String title = streamInfo.getTitle();

            callback.onTitleFetched(title);

        } catch (YoutubeDLException e) {

            e.printStackTrace();
            callback.onTitleFetched(null);
        } catch (YoutubeDL.CanceledException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public interface FetchRelatedVideosCallback {
        void onRelatedVideosFetched(List<YouTubeResponse.ContentItem> relatedVideos);
        void onFetchFailed(String errorMessage);
    }
    public interface YouTubeApiService {
        @Headers({
                "Content-Type: application/json",
                "Accept: application/json"
        })

        @POST("next")
        Call<JsonNode> getRelatedVideos(@Query("key") String apiKey, @Body RequestBody body);

        @POST("search")
        Call<JsonNode> getSearchResults(@Query("key") String apiKey, @Body RequestBody body);

    }

    public void fetchSearchResults(String query) {
        Log.d(TAG, "Fetching search results for query: " + query);

        JsonNode postData = createPostDataSearch(query);
        Log.d(TAG, "Post data for search request: " + postData.toString()); // Log the created post data

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"), postData.toString()
        );

        // Log the type of API request being sent
        Log.d(TAG, "Sending API request with POST data: " + postData.toString());

        apiService.getSearchResults(API_KEY, requestBody).enqueue(new Callback<JsonNode>() {
            @Override
            public void onResponse(Call<JsonNode> call, Response<JsonNode> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Log the response body
                    Log.d(TAG, "Response received: " + response.body().toString());

                    List<YouTubeResponse.ContentItem> searchResults = extractSearchResults(response.body());

                    // Log the search results extracted
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
                + "\"params\": \"EgIQAQ%3D%3D\""
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
                                    // Extracting data
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

                                    // Log the extracted information
                                    Log.d(TAG, "Video ID: " + videoId);
                                    Log.d(TAG, "Title: " + title);
                                    Log.d(TAG, "Author: " + author);
                                    Log.d(TAG, "Thumbnail URL: " + thumbnailUrl);

                                    // Add to the results list
                                    searchResults.add(new YouTubeResponse.ContentItem(videoId, title, thumbnailUrl));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing search results JSON: " + e.getMessage(), e);
        }

        // Log the total number of search results found
        Log.d(TAG, "Total search results: " + searchResults.size());

        return searchResults;
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
        List<JsonNode> tileRenderers = new ArrayList<>();

        try {
            // Log the entire JSON response to inspect its structure
            Log.d(TAG, "Full JSON Response: " + response.toString());

            JsonNode horizontalListRenderer = response.at("/contents/singleColumnWatchNextResults/pivot/sectionListRenderer/contents/0/shelfRenderer/content/horizontalListRenderer");
            Log.d(TAG, "HorizontalListRenderer Node: " + horizontalListRenderer.toString());

            JsonNode items = horizontalListRenderer.path("items");

            if (items.isArray()) {
                int totalVideos = items.size();
                Log.d(TAG, "Total videos found in 'items': " + totalVideos);

                for (JsonNode item : items) {
                    Log.d(TAG, "Item Node: " + item.toString());

                    JsonNode videoNode = item.path("tileRenderer");
                    Log.d(TAG, "TileRenderer Node: " + videoNode.toString());

                    if (!videoNode.isNull() && videoNode.isObject()) {
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

                        relatedVideos.add(new YouTubeResponse.ContentItem(videoId, title, thumbnailUrl));
                    } else {
                        Log.w(TAG, "TileRenderer is null or not an object in item: " + item.toString());
                    }
                }

                Log.d(TAG, "Total extracted related videos: " + relatedVideos.size());

            } else {
                Log.w(TAG, "'items' is not an array in horizontalListRenderer: " + horizontalListRenderer.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing related videos JSON: " + e.getMessage(), e);
        }

        Log.d(TAG, "Collected TileRenderers: " + tileRenderers.toString());

        return relatedVideos;
    }

}