package com.example.herbai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;

public class ResultActivity extends AppCompatActivity {
    private SwitchMaterial themeSwitch;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "theme_prefs";
    private static final String NIGHT_MODE = "night_mode";

    private TextView topPlantNameTextView;
    private TextView scientificNameTextView;
    private TextView familyTextView;
    private TextView confidenceTextView;
    private TextView plantUsesTextView;
    private TextView habitatTextView;
    private LinearLayout probablePlantsLayout;
    private CardView identificationStatusCard;
    private TextView identificationStatusText;
    private Button backButton;
    private Button searchMoreButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load saved preference and apply the theme mode
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isNightMode = sharedPreferences.getBoolean(NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_result);

        initializeViews();
        setupThemeSwitch();
        displayResults();
        setupButtons();
    }

    private void initializeViews() {
        themeSwitch = findViewById(R.id.themeSwitch);
        topPlantNameTextView = findViewById(R.id.topPlantNameTextView);
        scientificNameTextView = findViewById(R.id.scientificNameTextView);
        familyTextView = findViewById(R.id.familyTextView);
        confidenceTextView = findViewById(R.id.confidenceTextView);
        plantUsesTextView = findViewById(R.id.plantUsesTextView);
        habitatTextView = findViewById(R.id.habitatTextView);
        probablePlantsLayout = findViewById(R.id.probablePlantsLayout);
        identificationStatusCard = findViewById(R.id.identificationStatusCard);
        identificationStatusText = findViewById(R.id.identificationStatusText);
        backButton = findViewById(R.id.backButton);
        searchMoreButton = findViewById(R.id.searchMoreButton);
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

    private void displayResults() {
        Intent intent = getIntent();

        // Get plant information from intent
        String plantName = intent.getStringExtra("plantName");
        String scientificName = intent.getStringExtra("scientificName");
        String family = intent.getStringExtra("family");
        String habitat = intent.getStringExtra("habitat");
        String topPlantUses = intent.getStringExtra("topPlantUses");
        double confidence = intent.getDoubleExtra("confidence", 0.0);
        boolean isRealIdentification = intent.getBooleanExtra("isRealIdentification", false);
        ArrayList<String> probablePlants = intent.getStringArrayListExtra("probablePlants");

        // Display main plant information
        if (plantName != null && !plantName.trim().isEmpty()) {
            topPlantNameTextView.setText(plantName);
        } else {
            topPlantNameTextView.setText("Plant Identified");
        }

        if (scientificName != null && !scientificName.trim().isEmpty()) {
            scientificNameTextView.setText(scientificName);
            scientificNameTextView.setVisibility(View.VISIBLE);
        } else {
            scientificNameTextView.setVisibility(View.GONE);
        }

        if (family != null && !family.trim().isEmpty() && !family.equals("Unknown")) {
            familyTextView.setText("Family: " + family);
            familyTextView.setVisibility(View.VISIBLE);
        } else {
            familyTextView.setVisibility(View.GONE);
        }

        if (habitat != null && !habitat.trim().isEmpty() && !habitat.equals("Various environments")) {
            habitatTextView.setText("Habitat: " + habitat);
            habitatTextView.setVisibility(View.VISIBLE);
        } else {
            habitatTextView.setVisibility(View.GONE);
        }

        // Display confidence
        if (confidence > 0) {
            confidenceTextView.setText(String.format("Confidence: %.1f%%", confidence * 100));
            confidenceTextView.setVisibility(View.VISIBLE);
        } else {
            confidenceTextView.setVisibility(View.GONE);
        }

        // Display uses/medicinal properties
        if (topPlantUses != null && !topPlantUses.trim().isEmpty()) {
            plantUsesTextView.setText(topPlantUses);
        } else {
            plantUsesTextView.setText("Medicinal properties and uses information not available.");
        }

        // Display identification status
        if (isRealIdentification) {
            if (confidence > 0.8) {
                identificationStatusText.setText("✓ High confidence identification from AI model");
                identificationStatusCard.setCardBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            } else if (confidence > 0.5) {
                identificationStatusText.setText("⚠ Medium confidence identification from AI model");
                identificationStatusCard.setCardBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
            } else {
                identificationStatusText.setText("⚠ Low confidence identification from AI model");
                identificationStatusCard.setCardBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
            }
        } else {
            identificationStatusText.setText("ℹ Sample data - Connect to server for AI identification");
            identificationStatusCard.setCardBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        }

        // Display probable plants
        displayProbablePlants(probablePlants);
    }

    private void displayProbablePlants(ArrayList<String> probablePlants) {
        probablePlantsLayout.removeAllViews();

        if (probablePlants != null && !probablePlants.isEmpty()) {
            for (int i = 0; i < probablePlants.size(); i++) {
                String plant = probablePlants.get(i);

                // Create a card for each probable plant
                CardView cardView = new CardView(this);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                cardParams.setMargins(0, 0, 0, 16);
                cardView.setLayoutParams(cardParams);
                cardView.setCardElevation(4);
                cardView.setRadius(8);
                cardView.setContentPadding(16, 12, 16, 12);

                TextView plantTextView = new TextView(this);
                plantTextView.setText((i + 1) + ". " + plant);
                plantTextView.setTextSize(14);
                plantTextView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

                cardView.addView(plantTextView);
                probablePlantsLayout.addView(cardView);
            }
        } else {
            // Show a message if no probable plants are available
            TextView noDataTextView = new TextView(this);
            noDataTextView.setText("No alternative suggestions available");
            noDataTextView.setTextSize(14);
            noDataTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            probablePlantsLayout.addView(noDataTextView);
        }
    }

    private void setupButtons() {
        backButton.setOnClickListener(v -> {
            finish(); // Go back to MainActivity
        });

        searchMoreButton.setOnClickListener(v -> {
            // Go to plant search activity
            Intent intent = new Intent(ResultActivity.this, PlantSearchActivity.class);

            // Pass the identified plant name as search query if available
            String plantName = getIntent().getStringExtra("plantName");
            if (plantName != null && !plantName.trim().isEmpty()) {
                intent.putExtra("search_query", plantName);
            }

            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}