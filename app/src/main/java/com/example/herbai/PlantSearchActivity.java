package com.example.herbai;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PlantSearchActivity extends AppCompatActivity{
    private static final String TAG = "PlantSearchActivity";
    private MaterialSwitch themeSwitch;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "theme_prefs";
    private static final String NIGHT_MODE = "night_mode";

    private EditText searchEditText;
    private Button searchButton, smartSearchButton;
    private ProgressBar progressBar;
    private TextView statusTextView, resultsCountTextView;
    private RecyclerView resultsRecyclerView;
    private PlantAdapter plantAdapter;
    private List<PlantItem> plantList;

    private OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Executor executor = Executors.newSingleThreadExecutor();

    // Replace with your actual server URL
    private static final String BASE_URL = "https://serverv1-1.onrender.com/";
    private boolean isSmartSearchMode = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load saved theme preference
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isNightMode = sharedPreferences.getBoolean(NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_plant_search);

        initializeViews();
        setupThemeSwitch();
        setupRecyclerView();
        setupClickListeners();

        client = new OkHttpClient();
        plantList = new ArrayList<>();
    }

    private void initializeViews() {
        themeSwitch = findViewById(R.id.themeSwitch);
        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        smartSearchButton = findViewById(R.id.smartSearchButton);
        progressBar = findViewById(R.id.progressBar);
        statusTextView = findViewById(R.id.statusTextView);
        resultsCountTextView = findViewById(R.id.resultsCountTextView);
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView);
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

    private void setupRecyclerView() {
        plantList = new ArrayList<>(); // Initialize the list
        plantAdapter = new PlantAdapter(plantList);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        resultsRecyclerView.setAdapter(plantAdapter);

        Log.d(TAG, "RecyclerView setup completed");
    }

    private void setupClickListeners() {
        searchButton.setOnClickListener(v -> performSearch(false));
        smartSearchButton.setOnClickListener(v -> performSearch(true));
    }

    private void performSearch(boolean isSmartSearch) {
        isSmartSearchMode = isSmartSearch;
        String query = searchEditText.getText().toString().trim();
        Log.d(TAG, "Performing search for: " + query + ", Smart: " + isSmartSearch);

        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, "Please enter a plant name to search", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        statusTextView.setText(isSmartSearch ? "Performing smart search..." : "Searching...");
        statusTextView.setVisibility(View.VISIBLE);
        searchButton.setEnabled(false);
        smartSearchButton.setEnabled(false);

        // Clear previous results
        plantList.clear();
        plantAdapter.notifyDataSetChanged();
        resultsCountTextView.setVisibility(View.GONE);

        String endpoint = isSmartSearch ? "smart_search/" : "search/";
        String url = BASE_URL + endpoint + query;
        Log.d(TAG, "API URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network request failed", e);
                mainHandler.post(() -> {
                    hideLoadingState();
                    statusTextView.setText("Search failed: " + e.getMessage());
                    Toast.makeText(PlantSearchActivity.this, "Network error occurred", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "API Response: " + responseBody);

                mainHandler.post(() -> {
                    hideLoadingState();

                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        boolean success = jsonResponse.getBoolean("success");
                        Log.d(TAG, "API Success: " + success);

                        if (success) {
                            JSONArray results = jsonResponse.getJSONArray("results");
                            int resultsCount = jsonResponse.getInt("results_count");
                            String searchType = jsonResponse.optString("search_type", "search");
                            boolean wasGenerated = jsonResponse.optBoolean("was_generated", false);

                            Log.d(TAG, "Results count: " + resultsCount + ", Array length: " + results.length());

                            // Update status
                            String statusMessage = "Found " + resultsCount + " result(s)";
                            if (wasGenerated) {
                                statusMessage += " (Generated from APIs)";
                            }
                            statusTextView.setText(statusMessage);

                            // Show results count
                            resultsCountTextView.setText("Results: " + resultsCount);
                            resultsCountTextView.setVisibility(View.VISIBLE);

                            // Parse and display results
                            parseSearchResults(results);

                        } else {
                            String message = jsonResponse.optString("message", "Search failed");
                            Log.w(TAG, "API returned success=false: " + message);
                            statusTextView.setText(message);
                            Toast.makeText(PlantSearchActivity.this, message, Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error", e);
                        statusTextView.setText("Error parsing search results");
                        Toast.makeText(PlantSearchActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void parseSearchResults(JSONArray results) {
        Log.d(TAG, "Parsing " + results.length() + " results");
        plantList.clear();

        try {
            for (int i = 0; i < results.length(); i++) {
                JSONObject plantJson = results.getJSONObject(i);
                Log.d(TAG, "Parsing plant " + i + ": " + plantJson.toString());

                PlantItem plant = new PlantItem();
                plant.setPlantName(plantJson.optString("plant_name", "Unknown Plant"));
                plant.setScientificName(plantJson.optString("scientific_name", "Unknown"));
                plant.setFamily(plantJson.optString("family", "Unknown"));
                plant.setKingdom(plantJson.optString("kingdom", "Plantae"));
                plant.setGenus(plantJson.optString("genus", "Unknown"));
                plant.setSpecies(plantJson.optString("species", "Unknown"));
                plant.setMedicinalProperties(plantJson.optString("medicinal_properties", "Not available"));
                plant.setUses(plantJson.optString("uses", "Not available"));
                plant.setHabitat(plantJson.optString("habitat", "Various environments"));
                plant.setChemicalComponents(plantJson.optString("chemical_components", "Components under study"));

                // Force "Knowledge Graph" for existing (normal) search
                if (!searchEditText.getText().toString().trim().isEmpty() && !isSmartSearchMode) {
                    plant.setDataSource("Knowledge Graph");
                } else {
                    plant.setDataSource(plantJson.optString("data_source", "Knowledge Graph"));
                }

                plantList.add(plant);
                Log.d(TAG, "Added plant: " + plant.getDisplayTitle());
            }

            resultsRecyclerView.setVisibility(View.VISIBLE);
            if (plantAdapter != null) {
                plantAdapter.updatePlantList(plantList);
            }

            if (plantList.isEmpty()) {
                statusTextView.setText("No plants found matching your search");
                resultsRecyclerView.setVisibility(View.GONE);
            } else {
                resultsRecyclerView.scrollToPosition(0);
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing plant data", e);
            statusTextView.setText("Error parsing plant data");
            Toast.makeText(this, "Error parsing plant information", Toast.LENGTH_SHORT).show();
        }
    }


    private void hideLoadingState() {
        progressBar.setVisibility(View.GONE);
        searchButton.setEnabled(true);
        smartSearchButton.setEnabled(true);
    }
}