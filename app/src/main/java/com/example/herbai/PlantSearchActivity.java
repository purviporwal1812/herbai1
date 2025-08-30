package com.example.herbai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlantSearchActivity extends AppCompatActivity {
    private static final String TAG = "PlantSearchActivity";
    private static final String BASE_API_URL = "https://serverv1-1.onrender.com";

    private SwitchMaterial themeSwitch;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "theme_prefs";
    private static final String NIGHT_MODE = "night_mode";

    private EditText searchEditText;
    private LinearLayout searchResultsLayout;
    private ProgressBar progressBar;
    private TextView statusTextView;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load saved preference and apply the theme mode
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isNightMode = sharedPreferences.getBoolean(NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_plant_search);

        // Initialize ExecutorService
        executorService = Executors.newFixedThreadPool(2);

        initializeViews();
        setupThemeSwitch();
        setupSearchFunctionality();

        // Handle pre-filled search query from ResultActivity
        Intent intent = getIntent();
        String searchQuery = intent.getStringExtra("search_query");
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            searchEditText.setText(searchQuery);
            performSearch(searchQuery);
        }
    }

    private void initializeViews() {
        themeSwitch = findViewById(R.id.themeSwitch);
        searchEditText = findViewById(R.id.searchEditText);
        searchResultsLayout = findViewById(R.id.searchResultsLayout);
        progressBar = findViewById(R.id.progressBar);
        statusTextView = findViewById(R.id.statusTextView);

        // Initial status
        statusTextView.setText("Enter a plant name to search...");
    }

    private void setupThemeSwitch() {
        boolean isNightMode = sharedPreferences.getBoolean(NIGHT_MODE, false);
        themeSwitch.setChecked(isNightMode);

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(NIGHT_MODE, isChecked);
            editor.apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            recreate();
        });
    }

    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.length() >= 3) {
                    // Perform search with delay to avoid too many requests
                    searchEditText.removeCallbacks(searchRunnable);
                    searchRunnable = () -> performSearch(query);
                    searchEditText.postDelayed(searchRunnable, 1000); // 1 second delay
                } else if (query.isEmpty()) {
                    clearResults();
                    statusTextView.setText("Enter a plant name to search...");
                }
            }
        });
    }

    private Runnable searchRunnable;

    private void performSearch(String query) {
        Log.d(TAG, "Performing search for: " + query);

        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        statusTextView.setText("Searching for \"" + query + "\"...");
        clearResults();

        executorService.execute(() -> {
            try {
                String result = queryBackendAPI(query);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (result != null) {
                        parseAndDisplayResults(result, query);
                    } else {
                        statusTextView.setText("No results found for \"" + query + "\". Try a different search term.");
                        Toast.makeText(this, "Search failed. Check your internet connection.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Search error: " + e.getMessage());
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    statusTextView.setText("Search failed. Please try again.");
                    Toast.makeText(this, "Search error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // Replace the queryBackendAPI method in PlantSearchActivity with this fixed version
    private String queryBackendAPI(String plantName) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            // Use %20 encoding instead of + encoding for spaces to match browser behavior
            String encodedPlantName = plantName.trim().replace(" ", "%20");
            String urlString = BASE_API_URL + "/smart_search/" + encodedPlantName;

            Log.d(TAG, "Original plant name: " + plantName);
            Log.d(TAG, "Encoded plant name: " + encodedPlantName);
            Log.d(TAG, "Querying backend API: " + urlString);

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(20000); // 20 seconds for search
            connection.setReadTimeout(20000); // 20 seconds for search
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "HerbAI-Android/1.0");

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Backend API response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                String jsonResponse = response.toString();
                Log.d(TAG, "Backend API response: " + jsonResponse);

                return jsonResponse;
            } else {
                Log.w(TAG, "Backend API returned error code: " + responseCode);

                // Also log the error response body if available
                try {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    Log.w(TAG, "Error response body: " + errorResponse.toString());
                    errorReader.close();
                } catch (Exception e) {
                    Log.e(TAG, "Could not read error response: " + e.getMessage());
                }

                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error querying backend API: " + e.getMessage());
            e.printStackTrace();
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

    // Also update the showPlantDetails method to pass the correct data structure to ResultActivity
    private void showPlantDetails(JSONObject plantData) {
        try {
            Intent intent = new Intent(PlantSearchActivity.this, PlantDetailActivity.class);

            // Extract and pass plant information
            String plantName = plantData.optString("plant_name", "Unknown Plant");
            String scientificName = plantData.optString("scientific_name", "");
            String family = plantData.optString("family", "");
            String uses = plantData.optString("uses", "");
            String medicinalProperties = plantData.optString("medicinal_properties", "");
            String chemicalComponents = plantData.optString("chemical_components", "");
            String habitat = plantData.optString("habitat", "");

            Log.d(TAG, "=== PASSING DATA TO RESULT ACTIVITY ===");
            Log.d(TAG, "Plant name: " + plantName);
            Log.d(TAG, "Scientific name: " + scientificName);
            Log.d(TAG, "Family: " + family);
            Log.d(TAG, "Uses: " + uses);
            Log.d(TAG, "Medicinal properties: " + medicinalProperties);
            Log.d(TAG, "Chemical components: " + chemicalComponents);
            Log.d(TAG, "Habitat: " + habitat);

            // Pass data to ResultActivity using the same structure as MainActivity
            intent.putExtra("plantName", plantName);
            intent.putExtra("scientificName", scientificName);
            intent.putExtra("family", family);
            intent.putExtra("habitat", habitat);
            intent.putExtra("uses", uses); // Main uses field
            intent.putExtra("confidence", 0.95); // High confidence for database results
            intent.putExtra("isRealIdentification", false); // This is from database search
            intent.putExtra("isFromSearchRoute", true); // This is from search route

            // Database information fields (same as MainActivity structure)
            intent.putExtra("databaseUses", uses);
            intent.putExtra("databaseMedicinalProperties", medicinalProperties);
            intent.putExtra("databaseChemicalComponents", chemicalComponents);

            // Empty arrays for images and probable plants
            intent.putStringArrayListExtra("dbImageUrls", new ArrayList<String>());
            intent.putStringArrayListExtra("probablePlants", new ArrayList<String>());
            intent.putExtra("hasDbImages", false);

            startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, "Error showing plant details: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error opening plant details", Toast.LENGTH_SHORT).show();
        }
    }
    private void parseAndDisplayResults(String jsonResponse, String originalQuery) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            boolean success = jsonObject.optBoolean("success", false);

            if (!success) {
                statusTextView.setText("No results found for \"" + originalQuery + "\"");
                return;
            }

            JSONArray results = jsonObject.optJSONArray("results");
            if (results == null || results.length() == 0) {
                statusTextView.setText("No plants found matching \"" + originalQuery + "\"");
                return;
            }

            // Update status
            boolean wasGenerated = jsonObject.optBoolean("was_generated", false);
            int resultsCount = results.length();

            String statusMessage = "Found " + resultsCount + " result" + (resultsCount > 1 ? "s" : "") +
                    " for \"" + originalQuery + "\"";
            if (wasGenerated) {
                statusMessage += " (generated from web sources)";
            }
            statusTextView.setText(statusMessage);

            // Display results
            for (int i = 0; i < results.length(); i++) {
                JSONObject plantData = results.getJSONObject(i);
                addPlantResultCard(plantData, i + 1);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing search results: " + e.getMessage());
            statusTextView.setText("Error processing search results");
        }
    }

    private void addPlantResultCard(JSONObject plantData, int position) {
        try {
            // Extract plant information
            String plantName = plantData.optString("plant_name", "Unknown Plant");
            String scientificName = plantData.optString("scientific_name", "");
            String family = plantData.optString("family", "");
            String uses = plantData.optString("uses", "");
            String medicinalProperties = plantData.optString("medicinal_properties", "");
            String habitat = plantData.optString("habitat", "");
            boolean autoGenerated = plantData.optBoolean("auto_generated", false);
            String dataSource = plantData.optString("data_source", "");

            // Create card view
            CardView cardView = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 16);
            cardView.setLayoutParams(cardParams);
            cardView.setCardElevation(6);
            cardView.setRadius(12);
            cardView.setContentPadding(16, 16, 16, 16);
            cardView.setClickable(true);
            cardView.setFocusable(true);

            // Create content layout
            LinearLayout contentLayout = new LinearLayout(this);
            contentLayout.setOrientation(LinearLayout.VERTICAL);
            contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            // Plant name with position
            TextView nameTextView = new TextView(this);
            String displayName = position + ". " + plantName;
            if (autoGenerated) {
                displayName += " 🌐";
            }
            nameTextView.setText(displayName);
            nameTextView.setTextSize(18);
            nameTextView.setTextColor(getResources().getColor(android.R.color.black));
            nameTextView.setPadding(0, 0, 0, 8);
            contentLayout.addView(nameTextView);

            // Scientific name
            if (!scientificName.isEmpty() && !scientificName.equals("Unknown")) {
                TextView scientificTextView = new TextView(this);
                scientificTextView.setText("Scientific Name: " + scientificName);
                scientificTextView.setTextSize(14);
                scientificTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                scientificTextView.setPadding(0, 0, 0, 4);
                contentLayout.addView(scientificTextView);
            }

            // Family
            if (!family.isEmpty() && !family.equals("Unknown Family")) {
                TextView familyTextView = new TextView(this);
                familyTextView.setText("Family: " + family);
                familyTextView.setTextSize(14);
                familyTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                familyTextView.setPadding(0, 0, 0, 4);
                contentLayout.addView(familyTextView);
            }

            // Uses preview
            if (!uses.isEmpty() && !uses.equals("Uses to be researched")
                    && !uses.equals("Traditional and ornamental uses")) {
                TextView usesTextView = new TextView(this);
                String usesPreview = uses.length() > 150 ? uses.substring(0, 150) + "..." : uses;
                usesTextView.setText("Uses: " + usesPreview);
                usesTextView.setTextSize(13);
                usesTextView.setTextColor(getResources().getColor(android.R.color.black));
                usesTextView.setPadding(0, 8, 0, 0);
                contentLayout.addView(usesTextView);
            }

            // Data source indicator
            if (autoGenerated && !dataSource.isEmpty()) {
                TextView sourceTextView = new TextView(this);
                sourceTextView.setText("Source: " + dataSource);
                sourceTextView.setTextSize(11);
                sourceTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                sourceTextView.setPadding(0, 8, 0, 0);
                contentLayout.addView(sourceTextView);
            }

            cardView.addView(contentLayout);

            // Set click listener to show detailed results
            cardView.setOnClickListener(v -> {
                showPlantDetails(plantData);
            });

            // Add card to results layout
            searchResultsLayout.addView(cardView);

        } catch (Exception e) {
            Log.e(TAG, "Error creating result card: " + e.getMessage());
        }
    }

    private void clearResults() {
        searchResultsLayout.removeAllViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown executor service
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        // Remove any pending search callbacks
        if (searchEditText != null && searchRunnable != null) {
            searchEditText.removeCallbacks(searchRunnable);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}