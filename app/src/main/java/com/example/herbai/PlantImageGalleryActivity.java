package com.example.herbai;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PlantImageGalleryActivity extends AppCompatActivity {
    private static final String TAG = "PlantImageGallery";
    private static final String BASE_URL = "https://serverv1-1.onrender.com";
    private static final int MAX_IMAGES = 2; // Limit to 2 images

    // UI Components
    private SwitchMaterial themeSwitch;
    private TextView plantNameTextView;
    private TextView scientificNameTextView;
    private TextView imageCountTextView;
    private RecyclerView imagesRecyclerView;
    private ProgressBar loadingProgressBar;
    private TextView noImagesTextView;
    private Button backButton;

    // Data
    private String plantName;
    private String scientificName;
    private List<String> imageUrls;
    private PlantImageAdapter imageAdapter;

    // Utils
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "theme_prefs";
    private static final String NIGHT_MODE = "night_mode";
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply theme
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isNightMode = sharedPreferences.getBoolean(NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_plant_image_gallery);

        // Initialize executor service
        executorService = Executors.newFixedThreadPool(2);

        initializeViews();
        setupThemeSwitch();
        getIntentData();
        setupRecyclerView();
        setupButtons();
        loadPlantImages();
    }

    private void initializeViews() {
        themeSwitch = findViewById(R.id.themeSwitch);
        plantNameTextView = findViewById(R.id.plantNameTextView);
        scientificNameTextView = findViewById(R.id.scientificNameTextView);
        imageCountTextView = findViewById(R.id.imageCountTextView);
        imagesRecyclerView = findViewById(R.id.imagesRecyclerView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        noImagesTextView = findViewById(R.id.noImagesTextView);
        backButton = findViewById(R.id.backButton);
    }

    private void setupThemeSwitch() {
        boolean isNightMode = sharedPreferences.getBoolean(NIGHT_MODE, false);
        themeSwitch.setChecked(isNightMode);

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(NIGHT_MODE, isChecked);
            editor.apply();

            AppCompatDelegate.setDefaultNightMode(isChecked ?
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            recreate();
        });
    }

    private void getIntentData() {
        plantName = getIntent().getStringExtra("plant_name");
        scientificName = getIntent().getStringExtra("scientific_name");

        // Check if we have preloaded images from the previous activity
        ArrayList<String> preloadedImages = getIntent().getStringArrayListExtra("preloaded_images");
        if (preloadedImages != null && !preloadedImages.isEmpty()) {
            Log.d(TAG, "Found " + preloadedImages.size() + " preloaded images");
            // Limit preloaded images to MAX_IMAGES
            imageUrls = new ArrayList<>();
            int limit = Math.min(preloadedImages.size(), MAX_IMAGES);
            for (int i = 0; i < limit; i++) {
                imageUrls.add(preloadedImages.get(i));
            }
            Log.d(TAG, "Limited to " + imageUrls.size() + " preloaded images");
        }

        // Set plant information
        plantNameTextView.setText(plantName != null ? plantName : "Unknown Plant");

        if (scientificName != null && !scientificName.equals("Unknown")) {
            scientificNameTextView.setText(scientificName);
            scientificNameTextView.setVisibility(View.VISIBLE);
        } else {
            scientificNameTextView.setVisibility(View.GONE);
        }

        Log.d(TAG, "Plant: " + plantName + ", Scientific: " + scientificName);
    }

    private void setupRecyclerView() {
        if (imageUrls == null) {
            imageUrls = new ArrayList<>();
        }

        imageAdapter = new PlantImageAdapter(this, imageUrls);

        // Use GridLayoutManager for 2 columns
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        imagesRecyclerView.setLayoutManager(gridLayoutManager);
        imagesRecyclerView.setAdapter(imageAdapter);
    }

    private void setupButtons() {
        backButton.setOnClickListener(v -> finish());
    }

    private void loadPlantImages() {
        showLoading(true);

        // If we already have preloaded images, show them first
        if (imageUrls != null && !imageUrls.isEmpty()) {
            Log.d(TAG, "Displaying " + imageUrls.size() + " preloaded images");
            showLoading(false);
            updateImageDisplay();
            return;
        }

        // Otherwise, fetch from backend
        executorService.execute(() -> {
            try {
                List<String> fetchedImages = fetchPlantImagesFromBackend();

                runOnUiThread(() -> {
                    showLoading(false);

                    if (fetchedImages != null && !fetchedImages.isEmpty()) {
                        imageUrls.clear();
                        imageUrls.addAll(fetchedImages);
                        updateImageDisplay();
                        Log.d(TAG, "Loaded " + imageUrls.size() + " images for " + plantName);
                    } else {
                        showNoImagesMessage();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading images: " + e.getMessage());
                runOnUiThread(() -> {
                    showLoading(false);
                    showNoImagesMessage();
                    Toast.makeText(this, "Failed to load images: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateImageDisplay() {
        imageAdapter.notifyDataSetChanged();
        imageCountTextView.setText("Showing " + imageUrls.size() + " images");
        imageCountTextView.setVisibility(View.VISIBLE);
        noImagesTextView.setVisibility(View.GONE);
    }

    private List<String> fetchPlantImagesFromBackend() {
        Set<String> uniqueImages = new HashSet<>();
        List<String> finalImages = new ArrayList<>();

        try {
            // Method 1: Try smart_search endpoint first - but only for the main identified plant
            List<String> smartSearchImages = querySmartSearchAPI(plantName);
            if (smartSearchImages != null && !smartSearchImages.isEmpty()) {
                uniqueImages.addAll(smartSearchImages);
                Log.d(TAG, "Found " + smartSearchImages.size() + " images from smart_search for main plant");
            }

            // Method 2: Try with scientific name if different - but still only for main plant
            if (scientificName != null && !scientificName.equals(plantName) && !scientificName.equals("Unknown")) {
                List<String> scientificImages = querySmartSearchAPI(scientificName);
                if (scientificImages != null && !scientificImages.isEmpty()) {
                    uniqueImages.addAll(scientificImages);
                    Log.d(TAG, "Found additional images using scientific name for main plant");
                }
            }

            // Method 3: Try prediction endpoint - filter for ONLY the first predicted plant
            List<String> predictImages = tryPredictEndpointForFirstPlantOnly();
            if (predictImages != null && !predictImages.isEmpty()) {
                uniqueImages.addAll(predictImages);
                Log.d(TAG, "Found additional images from predict endpoint for main plant only");
            }

            // Limit to MAX_IMAGES
            finalImages.addAll(uniqueImages);
            if (finalImages.size() > MAX_IMAGES) {
                finalImages = finalImages.subList(0, MAX_IMAGES);
                Log.d(TAG, "Limited to " + MAX_IMAGES + " images for display");
            }

            Log.d(TAG, "Total unique images collected for main plant: " + finalImages.size());
            return finalImages;

        } catch (Exception e) {
            Log.e(TAG, "Error fetching images from backend: " + e.getMessage());
            return null;
        }
    }

    private List<String> tryPredictEndpointForFirstPlantOnly() {
        List<String> imageUrls = new ArrayList<>();

        try {
            File dummyImageFile = createDummyImageFile();
            if (dummyImageFile == null) {
                Log.w(TAG, "Could not create dummy image file");
                return imageUrls;
            }

            Log.d(TAG, "Trying predict endpoint with dummy image to get images for main plant only");

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            RequestBody fileBody = RequestBody.create(dummyImageFile, MediaType.parse("image/jpeg"));
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "dummy.jpg", fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(BASE_URL + "/predict")
                    .post(requestBody)
                    .addHeader("User-Agent", "HerbAI-Android/1.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Predict endpoint response received");

                    imageUrls.addAll(parsePredictResponseForFirstPlantOnly(responseBody));

                    // Clean up dummy file
                    if (dummyImageFile.exists()) {
                        dummyImageFile.delete();
                    }
                } else {
                    Log.w(TAG, "Predict endpoint failed with code: " + response.code());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error trying predict endpoint: " + e.getMessage());
        }

        return imageUrls;
    }

    private List<String> parsePredictResponseForFirstPlantOnly(String jsonResponse) {
        List<String> imageUrls = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);

            // Get the main predicted species name
            String mainSpecies = jsonObject.optString("species", "");

            // Parse db_image_urls - these should be for the main predicted plant
            if (jsonObject.has("db_image_urls")) {
                JSONArray dbImageUrls = jsonObject.getJSONArray("db_image_urls");
                Log.d(TAG, "Found " + dbImageUrls.length() + " db_image_urls for main plant");

                // Limit to MAX_IMAGES
                int limit = Math.min(dbImageUrls.length(), MAX_IMAGES);
                for (int i = 0; i < limit; i++) {
                    String imageUrl = dbImageUrls.getString(i);
                    if (isValidImageUrl(imageUrl)) {
                        imageUrls.add(imageUrl);
                        Log.d(TAG, "Added db image " + (i+1) + "/" + MAX_IMAGES + ": " + imageUrl);
                    }
                }
            }

            // If we don't have enough images yet, check db_matches - but ONLY the first one
            if (imageUrls.size() < MAX_IMAGES && jsonObject.has("db_matches")) {
                JSONArray dbMatches = jsonObject.getJSONArray("db_matches");
                Log.d(TAG, "Found db_matches, processing only the first match");

                // Only process the FIRST match (highest confidence prediction)
                if (dbMatches.length() > 0) {
                    JSONObject firstMatch = dbMatches.getJSONObject(0);
                    if (firstMatch.has("image_urls")) {
                        JSONArray matchImages = firstMatch.getJSONArray("image_urls");

                        // Add images until we reach MAX_IMAGES
                        for (int j = 0; j < matchImages.length() && imageUrls.size() < MAX_IMAGES; j++) {
                            String imageUrl = matchImages.getString(j);
                            if (isValidImageUrl(imageUrl) && !imageUrls.contains(imageUrl)) {
                                imageUrls.add(imageUrl);
                                Log.d(TAG, "Added first match image " + imageUrls.size() + "/" + MAX_IMAGES + ": " + imageUrl);
                            }
                        }
                    }
                }
            }

            Log.d(TAG, "Total images collected for main plant only: " + imageUrls.size());

        } catch (Exception e) {
            Log.e(TAG, "Error parsing predict response for main plant: " + e.getMessage());
        }

        return imageUrls;
    }

    private List<String> querySmartSearchAPI(String queryName) {
        if (queryName == null || queryName.trim().isEmpty()) {
            return null;
        }

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            String encodedName = URLEncoder.encode(queryName.trim(), "UTF-8");
            String urlString = BASE_URL + "/smart_search/" + encodedName;

            Log.d(TAG, "Querying smart_search API: " + urlString);

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "HerbAI-Android/1.0");

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Smart search API response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                String jsonResponse = response.toString();
                Log.d(TAG, "Smart search API response length: " + jsonResponse.length());

                return parseSmartSearchResponse(jsonResponse);
            } else {
                Log.w(TAG, "Smart search API returned error code: " + responseCode);
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error querying smart search API: " + e.getMessage());
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing reader: " + e.getMessage());
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private List<String> parseSmartSearchResponse(String jsonResponse) {
        List<String> imageUrls = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            Log.d(TAG, "Parsing smart search response for main plant only...");

            // Check if search was successful
            boolean success = jsonObject.optBoolean("success", false);
            if (!success) {
                Log.w(TAG, "Smart search returned success=false");
                return imageUrls;
            }

            // Parse results array - but focus on the BEST match only
            JSONArray results = jsonObject.optJSONArray("results");
            if (results != null && results.length() > 0) {
                Log.d(TAG, "Found " + results.length() + " search results, using only the first/best match");

                // Only process the FIRST result (best match for our plant)
                JSONObject firstResult = results.getJSONObject(0);
                String resultPlantName = firstResult.optString("plant_name", "");
                Log.d(TAG, "Processing images only for: " + resultPlantName);

                // Look for image_urls in the first result only - limit to MAX_IMAGES
                if (firstResult.has("image_urls")) {
                    JSONArray resultImages = firstResult.getJSONArray("image_urls");
                    Log.d(TAG, "First result has " + resultImages.length() + " images");

                    int limit = Math.min(resultImages.length(), MAX_IMAGES);
                    for (int j = 0; j < limit; j++) {
                        String imageUrl = resultImages.getString(j);
                        if (isValidImageUrl(imageUrl)) {
                            imageUrls.add(imageUrl);
                            Log.d(TAG, "Added image " + (j+1) + "/" + MAX_IMAGES + ": " + imageUrl);
                        }
                    }
                }
            } else {
                Log.w(TAG, "No results array found in smart search response");
            }

            // Also check for direct db_image_urls field (should be for main plant) - limit to MAX_IMAGES
            if (imageUrls.size() < MAX_IMAGES && jsonObject.has("db_image_urls")) {
                JSONArray dbImageUrls = jsonObject.getJSONArray("db_image_urls");
                Log.d(TAG, "Found direct db_image_urls with " + dbImageUrls.length() + " images for main plant");

                for (int i = 0; i < dbImageUrls.length() && imageUrls.size() < MAX_IMAGES; i++) {
                    String imageUrl = dbImageUrls.getString(i);
                    if (isValidImageUrl(imageUrl) && !imageUrls.contains(imageUrl)) {
                        imageUrls.add(imageUrl);
                        Log.d(TAG, "Added db image " + imageUrls.size() + "/" + MAX_IMAGES + ": " + imageUrl);
                    }
                }
            }

            // Check for db_matches - but ONLY the first one and limit to MAX_IMAGES
            if (imageUrls.size() < MAX_IMAGES && jsonObject.has("db_matches")) {
                JSONArray dbMatches = jsonObject.getJSONArray("db_matches");
                Log.d(TAG, "Found db_matches, processing only the first match");

                // Only process the FIRST match
                if (dbMatches.length() > 0) {
                    JSONObject firstMatch = dbMatches.getJSONObject(0);
                    if (firstMatch.has("image_urls")) {
                        JSONArray matchImages = firstMatch.getJSONArray("image_urls");
                        for (int j = 0; j < matchImages.length() && imageUrls.size() < MAX_IMAGES; j++) {
                            String imageUrl = matchImages.getString(j);
                            if (isValidImageUrl(imageUrl) && !imageUrls.contains(imageUrl)) {
                                imageUrls.add(imageUrl);
                                Log.d(TAG, "Added first match image " + imageUrls.size() + "/" + MAX_IMAGES + ": " + imageUrl);
                            }
                        }
                    }
                }
            }

            Log.d(TAG, "Total images parsed for main plant only: " + imageUrls.size());

        } catch (Exception e) {
            Log.e(TAG, "Error parsing smart search response: " + e.getMessage());
        }

        return imageUrls;
    }

    private File createDummyImageFile() {
        try {
            // Create a minimal 1x1 pixel JPEG file
            File dummyFile = new File(getCacheDir(), "dummy_prediction.jpg");

            // Create a minimal JPEG header and data
            byte[] minimalJpeg = {
                    (byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
                    0x01, 0x01, 0x00, 0x48, 0x00, 0x48, 0x00, 0x00, (byte)0xFF, (byte)0xDB, 0x00, 0x43, 0x00,
                    0x08, 0x06, 0x06, 0x07, 0x06, 0x05, 0x08, 0x07, 0x07, 0x07, 0x09, 0x09, 0x08, 0x0A, 0x0C,
                    0x14, 0x0D, 0x0C, 0x0B, 0x0B, 0x0C, 0x19, 0x12, 0x13, 0x0F, 0x14, 0x1D, 0x1A, 0x1F, 0x1E,
                    0x1D, 0x1A, 0x1C, 0x1C, 0x20, 0x24, 0x2E, 0x27, 0x20, 0x22, 0x2C, 0x23, 0x1C, 0x1C, 0x28,
                    0x37, 0x29, 0x2C, 0x30, 0x31, 0x34, 0x34, 0x34, 0x1F, 0x27, 0x39, 0x3D, 0x38, 0x32, 0x3C,
                    0x2E, 0x33, 0x34, 0x32, (byte)0xFF, (byte)0xC0, 0x00, 0x11, 0x08, 0x00, 0x01, 0x00, 0x01,
                    0x01, 0x01, 0x11, 0x00, 0x02, 0x11, 0x01, 0x03, 0x11, 0x01, (byte)0xFF, (byte)0xC4, 0x00,
                    0x14, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x08, (byte)0xFF, (byte)0xC4, 0x00, 0x14, 0x10, 0x01, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xFF,
                    (byte)0xDA, 0x00, 0x0C, 0x03, 0x01, 0x00, 0x02, 0x11, 0x03, 0x11, 0x00, 0x3F, 0x00, 0x00,
                    (byte)0xFF, (byte)0xD9
            };

            try (FileOutputStream fos = new FileOutputStream(dummyFile)) {
                fos.write(minimalJpeg);
                fos.flush();
            }

            Log.d(TAG, "Created dummy image file: " + dummyFile.getAbsolutePath());
            return dummyFile;

        } catch (Exception e) {
            Log.e(TAG, "Error creating dummy image file: " + e.getMessage());
            return null;
        }
    }

    private List<String> parsePredictResponse(String jsonResponse) {
        List<String> imageUrls = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);

            // Parse db_image_urls from predict response - limit to MAX_IMAGES
            if (jsonObject.has("db_image_urls")) {
                JSONArray dbImageUrls = jsonObject.getJSONArray("db_image_urls");
                Log.d(TAG, "Found " + dbImageUrls.length() + " db_image_urls in predict response");

                int limit = Math.min(dbImageUrls.length(), MAX_IMAGES);
                for (int i = 0; i < limit; i++) {
                    String imageUrl = dbImageUrls.getString(i);
                    if (isValidImageUrl(imageUrl)) {
                        imageUrls.add(imageUrl);
                    }
                }
            }

            // Parse db_matches from predict response - but ONLY the first match and limit images
            if (imageUrls.size() < MAX_IMAGES && jsonObject.has("db_matches")) {
                JSONArray dbMatches = jsonObject.getJSONArray("db_matches");
                Log.d(TAG, "Found " + dbMatches.length() + " db_matches in predict response");

                // Only process the FIRST match (highest confidence)
                if (dbMatches.length() > 0) {
                    JSONObject firstMatch = dbMatches.getJSONObject(0);
                    if (firstMatch.has("image_urls")) {
                        JSONArray matchImages = firstMatch.getJSONArray("image_urls");
                        for (int j = 0; j < matchImages.length() && imageUrls.size() < MAX_IMAGES; j++) {
                            String imageUrl = matchImages.getString(j);
                            if (isValidImageUrl(imageUrl) && !imageUrls.contains(imageUrl)) {
                                imageUrls.add(imageUrl);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing predict response: " + e.getMessage());
        }

        return imageUrls;
    }

    private boolean isValidImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        // Check if it's a valid image URL
        String lowerUrl = url.toLowerCase();
        return (lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://")) &&
                (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg") ||
                        lowerUrl.contains(".png") || lowerUrl.contains(".gif") ||
                        lowerUrl.contains("cloudinary.com") || lowerUrl.contains("image"));
    }

    private void showLoading(boolean show) {
        if (show) {
            loadingProgressBar.setVisibility(View.VISIBLE);
            noImagesTextView.setVisibility(View.GONE);
            imageCountTextView.setVisibility(View.GONE);
        } else {
            loadingProgressBar.setVisibility(View.GONE);
        }
    }

    private void showNoImagesMessage() {
        noImagesTextView.setVisibility(View.VISIBLE);
        imageCountTextView.setVisibility(View.GONE);
        noImagesTextView.setText("No images found for " + plantName +
                "\n\nImages may not be available in the database for this plant species." +
                "\n\nTry refreshing or check if the plant name is correct.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}