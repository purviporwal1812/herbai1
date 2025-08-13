package com.example.herbai;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GenerateDataActivity extends AppCompatActivity {
    private MaterialSwitch themeSwitch;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "theme_prefs";
    private static final String NIGHT_MODE = "night_mode";

    private EditText plantNameEditText;
    private SwitchMaterial saveToDatabaseSwitch;
    private Button generateButton;
    private ProgressBar progressBar;
    private TextView statusTextView;

    // Result display
    private CardView resultCard;
    private TextView resultPlantNameTextView;
    private TextView resultScientificNameTextView;
    private TextView resultFamilyTextView;
    private TextView resultMedicinalTextView;
    private TextView resultUsesTextView;
    private TextView resultHabitatTextView;
    private TextView generationTimeTextView;
    private TextView apisUsedTextView;
    private TextView databaseStatusTextView;

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

        setContentView(R.layout.activity_generate_data);

        initializeViews();
        setupThemeSwitch();
        setupClickListeners();

        client = new OkHttpClient();
    }

    private void initializeViews() {
        themeSwitch = findViewById(R.id.themeSwitch);
        plantNameEditText = findViewById(R.id.plantNameEditText);
        saveToDatabaseSwitch = findViewById(R.id.saveToDatabaseSwitch);
        generateButton = findViewById(R.id.generateButton);
        progressBar = findViewById(R.id.progressBar);
        statusTextView = findViewById(R.id.statusTextView);

        // Result display
        resultCard = findViewById(R.id.resultCard);
        resultPlantNameTextView = findViewById(R.id.resultPlantNameTextView);
        resultScientificNameTextView = findViewById(R.id.resultScientificNameTextView);
        resultFamilyTextView = findViewById(R.id.resultFamilyTextView);
        resultMedicinalTextView = findViewById(R.id.resultMedicinalTextView);
        resultUsesTextView = findViewById(R.id.resultUsesTextView);
        resultHabitatTextView = findViewById(R.id.resultHabitatTextView);
        generationTimeTextView = findViewById(R.id.generationTimeTextView);
        apisUsedTextView = findViewById(R.id.apisUsedTextView);
        databaseStatusTextView = findViewById(R.id.databaseStatusTextView);

        // Initially hide result card
        resultCard.setVisibility(View.GONE);

        // Set default values
        saveToDatabaseSwitch.setChecked(true);
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
        generateButton.setOnClickListener(v -> generatePlantData());
    }

    private void generatePlantData() {
        String plantName = plantNameEditText.getText().toString().trim();

        if (TextUtils.isEmpty(plantName)) {
            Toast.makeText(this, "Please enter a plant name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        showLoading(true);
        statusTextView.setText("Generating plant data from APIs...");
        statusTextView.setVisibility(View.VISIBLE);
        resultCard.setVisibility(View.GONE);

        // Create request body
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("plant_name", plantName);
            requestBody.put("save_to_database", saveToDatabaseSwitch.isChecked());
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(requestBody.toString(), JSON);

        Request request = new Request.Builder()
                .url(BASE_URL + "/generate_plant_data")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    statusTextView.setText("Generation failed: " + e.getMessage());
                    Toast.makeText(GenerateDataActivity.this, "Network error occurred", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();

                mainHandler.post(() -> {
                    showLoading(false);
                    parseGenerationResult(responseBody);
                });
            }
        });
    }

    private void parseGenerationResult(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            boolean success = json.getBoolean("success");

            if (success) {
                // Parse generated data
                JSONObject generatedData = json.getJSONObject("generated_data");
                String generationTime = json.getString("generation_time");

                // Display results
                displayGenerationResults(generatedData, json);
                statusTextView.setText("Plant data generated successfully!");

            } else {
                String error = json.optString("error", "Generation failed");
                statusTextView.setText("Error: " + error);
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            statusTextView.setText("Error parsing generation results");
            Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayGenerationResults(JSONObject plantData, JSONObject fullResponse) {
        try {
            // Plant information
            String plantName = plantData.optString("plant_name", "Unknown");
            String scientificName = plantData.optString("scientific_name", "Unknown");
            String family = plantData.optString("family", "Unknown Family");
            String medicinalProperties = plantData.optString("medicinal_properties", "Information not available");
            String uses = plantData.optString("uses", "Uses to be researched");
            String habitat = plantData.optString("habitat", "Various environments");

            // Display in UI
            resultPlantNameTextView.setText(plantName);
            resultScientificNameTextView.setText("Scientific Name: " + scientificName);
            resultFamilyTextView.setText("Family: " + family);
            resultMedicinalTextView.setText(medicinalProperties);
            resultUsesTextView.setText(uses);
            resultHabitatTextView.setText("Habitat: " + habitat);

            // Generation metadata
            String generationTime = fullResponse.optString("generation_time", "Unknown");
            generationTimeTextView.setText("Generation Time: " + generationTime);

            // APIs used
            JSONObject apisUsed = fullResponse.optJSONArray("apis_used") != null ?
                    new JSONObject() : new JSONObject();
            StringBuilder apisText = new StringBuilder("APIs Used: ");
            if (fullResponse.has("apis_used")) {
                try {
                    for (int i = 0; i < fullResponse.getJSONArray("apis_used").length(); i++) {
                        if (i > 0) apisText.append(", ");
                        apisText.append(fullResponse.getJSONArray("apis_used").getString(i));
                    }
                } catch (JSONException e) {
                    apisText.append("Unknown");
                }
            } else {
                apisText.append("Fallback Generator");
            }
            apisUsedTextView.setText(apisText.toString());

            // Database save status
            JSONObject databaseResult = fullResponse.optJSONObject("database_save");
            if (databaseResult != null) {
                boolean dbSuccess = databaseResult.optBoolean("success", false);
                String dbMessage = databaseResult.optString("message", "Unknown");
                String dbTime = databaseResult.optString("save_time", "");

                String dbStatusText = "Database: " + (dbSuccess ? "Saved ✅" : "Failed ❌");
                if (!dbTime.isEmpty()) {
                    dbStatusText += " (" + dbTime + ")";
                }
                databaseStatusTextView.setText(dbStatusText);
                databaseStatusTextView.setTextColor(getResources().getColor(
                        dbSuccess ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            } else {
                databaseStatusTextView.setText("Database: Not attempted");
                databaseStatusTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            }

            // Show result card
            resultCard.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            Toast.makeText(this, "Error displaying results", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        generateButton.setEnabled(!show);
        plantNameEditText.setEnabled(!show);
        saveToDatabaseSwitch.setEnabled(!show);
    }
}