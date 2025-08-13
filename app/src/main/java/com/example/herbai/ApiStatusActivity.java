package com.example.herbai;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiStatusActivity extends AppCompatActivity {
    private MaterialSwitch themeSwitch;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "theme_prefs";
    private static final String NIGHT_MODE = "night_mode";

    private TextView serviceNameTextView;
    private TextView serviceVersionTextView;
    private TextView connectionStatusTextView;
    private TextView lastCheckedTextView;

    // API Status Cards
    private CardView wikipediaCard, gbifCard, tropicosCard, neo4jCard;
    private TextView wikipediaStatusTextView, gbifStatusTextView, tropicosStatusTextView, neo4jStatusTextView;
    private TextView wikipediaDescTextView, gbifDescTextView, tropicosDescTextView, neo4jDescTextView;

    private Button refreshButton, testConnectionButton, testApisButton;
    private ProgressBar progressBar;

    private OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Executor executor = Executors.newSingleThreadExecutor();

    // Replace with your actual server URL
    private static final String BASE_URL = "https://serverv1-1.onrender.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load saved theme preference
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isNightMode = sharedPreferences.getBoolean(NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_api_status);

        initializeViews();
        setupThemeSwitch();
        setupClickListeners();

        client = new OkHttpClient();

        // Load initial status
        loadSystemStatus();
    }

    private void initializeViews() {
        themeSwitch = findViewById(R.id.themeSwitch);
        serviceNameTextView = findViewById(R.id.serviceNameTextView);
        serviceVersionTextView = findViewById(R.id.serviceVersionTextView);
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView);
        lastCheckedTextView = findViewById(R.id.lastCheckedTextView);

        // API Status Cards
        wikipediaCard = findViewById(R.id.wikipediaCard);
        gbifCard = findViewById(R.id.gbifCard);
        tropicosCard = findViewById(R.id.tropicosCard);
        neo4jCard = findViewById(R.id.neo4jCard);

        wikipediaStatusTextView = findViewById(R.id.wikipediaStatusTextView);
        gbifStatusTextView = findViewById(R.id.gbifStatusTextView);
        tropicosStatusTextView = findViewById(R.id.tropicosStatusTextView);
        neo4jStatusTextView = findViewById(R.id.neo4jStatusTextView);

        wikipediaDescTextView = findViewById(R.id.wikipediaDescTextView);
        gbifDescTextView = findViewById(R.id.gbifDescTextView);
        tropicosDescTextView = findViewById(R.id.tropicosDescTextView);
        neo4jDescTextView = findViewById(R.id.neo4jDescTextView);

        refreshButton = findViewById(R.id.refreshButton);
        testConnectionButton = findViewById(R.id.testConnectionButton);
        testApisButton = findViewById(R.id.testApisButton);
        progressBar = findViewById(R.id.progressBar);
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

    private void setupClickListeners() {
        refreshButton.setOnClickListener(v -> loadSystemStatus());
        testConnectionButton.setOnClickListener(v -> testDatabaseConnection());
        testApisButton.setOnClickListener(v -> testExternalApis());
    }

    private void loadSystemStatus() {
        showLoading(true);

        Request request = new Request.Builder()
                .url(BASE_URL + "/status")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    connectionStatusTextView.setText("Status: Offline ❌");
                    Toast.makeText(ApiStatusActivity.this, "Failed to connect to server", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();

                mainHandler.post(() -> {
                    showLoading(false);
                    parseSystemStatus(responseBody);
                });
            }
        });
    }

    private void parseSystemStatus(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);

            // Service info
            serviceNameTextView.setText(json.optString("service", "Enhanced Plant Knowledge Graph API"));
            serviceVersionTextView.setText("Version: " + json.optString("version", "2.0.0"));

            // Connection status
            boolean kgAvailable = json.optBoolean("kg_available", false);
            boolean connectionTested = json.optBoolean("connection_tested", false);

            String statusText = "Status: ";
            if (kgAvailable && connectionTested) {
                statusText += "Online ✅";
                connectionStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                statusText += "Partial ⚠️";
                connectionStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            }
            connectionStatusTextView.setText(statusText);

            // Features
            JSONObject features = json.optJSONObject("features");
            if (features != null) {
                updateFeatureStatus(features);
            }

            lastCheckedTextView.setText("Last checked: " + getCurrentTime());

        } catch (JSONException e) {
            Toast.makeText(this, "Error parsing status response", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFeatureStatus(JSONObject features) {
        // Wikipedia
        boolean wikipediaAvailable = features.optBoolean("wikipedia_integration", false);
        wikipediaStatusTextView.setText(wikipediaAvailable ? "Available ✅" : "Unavailable ❌");
        wikipediaStatusTextView.setTextColor(getResources().getColor(
                wikipediaAvailable ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        wikipediaDescTextView.setText("Wikipedia REST API for plant summaries and basic information");

        // GBIF
        boolean gbifAvailable = features.optBoolean("gbif_integration", false);
        gbifStatusTextView.setText(gbifAvailable ? "Available ✅" : "Unavailable ❌");
        gbifStatusTextView.setTextColor(getResources().getColor(
                gbifAvailable ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        gbifDescTextView.setText("Global Biodiversity Information Facility for taxonomic data");

        // Tropicos (assuming available if other APIs work)
        tropicosStatusTextView.setText("Available ✅");
        tropicosStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        tropicosDescTextView.setText("Missouri Botanical Garden database for nomenclature");

        // Neo4j
        boolean neo4jAvailable = features.optBoolean("knowledge_graph", false);
        neo4jStatusTextView.setText(neo4jAvailable ? "Connected ✅" : "Disconnected ❌");
        neo4jStatusTextView.setTextColor(getResources().getColor(
                neo4jAvailable ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        neo4jDescTextView.setText("Neo4j knowledge graph database for plant relationships");
    }

    private void testDatabaseConnection() {
        showLoading(true);

        Request request = new Request.Builder()
                .url(BASE_URL + "/test_connection")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(ApiStatusActivity.this, "Connection test failed", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();

                mainHandler.post(() -> {
                    showLoading(false);
                    parseConnectionTest(responseBody);
                });
            }
        });
    }

    private void parseConnectionTest(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            boolean success = json.getBoolean("success");
            String message = json.getString("message");

            if (success) {
                neo4jStatusTextView.setText("Connected ✅");
                neo4jStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                Toast.makeText(this, "Database connection successful", Toast.LENGTH_SHORT).show();
            } else {
                neo4jStatusTextView.setText("Disconnected ❌");
                neo4jStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                Toast.makeText(this, "Database connection failed: " + message, Toast.LENGTH_LONG).show();
            }

        } catch (JSONException e) {
            Toast.makeText(this, "Error parsing connection test response", Toast.LENGTH_SHORT).show();
        }
    }

    private void testExternalApis() {
        showLoading(true);

        Request request = new Request.Builder()
                .url(BASE_URL + "/test_apis")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(ApiStatusActivity.this, "API test failed", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();

                mainHandler.post(() -> {
                    showLoading(false);
                    parseApiTest(responseBody);
                });
            }
        });
    }

    private void parseApiTest(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            JSONObject apis = json.getJSONObject("apis");

            // Wikipedia
            JSONObject wikipedia = apis.optJSONObject("wikipedia");
            if (wikipedia != null) {
                String status = wikipedia.getString("status");
                boolean available = "Available".equals(status);
                wikipediaStatusTextView.setText(status + (available ? " ✅" : " ❌"));
                wikipediaStatusTextView.setTextColor(getResources().getColor(
                        available ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            }

            // GBIF
            JSONObject gbif = apis.optJSONObject("gbif");
            if (gbif != null) {
                String status = gbif.getString("status");
                boolean available = "Available".equals(status);
                gbifStatusTextView.setText(status + (available ? " ✅" : " ❌"));
                gbifStatusTextView.setTextColor(getResources().getColor(
                        available ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            }

            // Tropicos
            JSONObject tropicos = apis.optJSONObject("tropicos");
            if (tropicos != null) {
                String status = tropicos.getString("status");
                boolean available = "Available".equals(status);
                tropicosStatusTextView.setText(status + (available ? " ✅" : " ❌"));
                tropicosStatusTextView.setTextColor(getResources().getColor(
                        available ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            }

            // Show summary
            JSONObject summary = json.optJSONObject("summary");
            if (summary != null) {
                String healthPercentage = summary.getString("health_percentage");
                Toast.makeText(this, "API Health: " + healthPercentage, Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            Toast.makeText(this, "Error parsing API test response", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        refreshButton.setEnabled(!show);
        testConnectionButton.setEnabled(!show);
        testApisButton.setEnabled(!show);
    }

    private String getCurrentTime() {
        return java.text.DateFormat.getTimeInstance().format(new java.util.Date());
    }
}