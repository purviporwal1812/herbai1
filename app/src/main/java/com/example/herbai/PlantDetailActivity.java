package com.example.herbai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class PlantDetailActivity extends AppCompatActivity {
    private static final String TAG = "PlantDetailActivity";
    private static final int MAX_USES_LENGTH = 300; // Character limit before showing "Read More"

    private SwitchMaterial themeSwitch;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "theme_prefs";
    private static final String NIGHT_MODE = "night_mode";

    // UI Components
    private TextView plantNameTextView;
    private TextView scientificNameTextView;
    private TextView familyTextView;
    private TextView habitatTextView;
    private TextView genusTextView;
    private TextView dataSourceTextView;
    private TextView usesTextView;
    private TextView chemicalComponentsTextView;
    private Button readMoreButton;
    private Button viewImagesButton;
    private Button searchMoreButton;
    private Button backButton;
    private CardView chemicalCard;

    // Data storage
    private String completeUsesText = "";
    private boolean isExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load saved preference and apply the theme mode
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isNightMode = sharedPreferences.getBoolean(NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_plant_detail);

        initializeViews();
        setupThemeSwitch();
        setupButtons();
        displayPlantData();
    }

    private void initializeViews() {
        themeSwitch = findViewById(R.id.themeSwitch);
        plantNameTextView = findViewById(R.id.plantNameTextView);
        scientificNameTextView = findViewById(R.id.scientificNameTextView);
        familyTextView = findViewById(R.id.familyTextView);
        habitatTextView = findViewById(R.id.habitatTextView);
        genusTextView = findViewById(R.id.genusTextView);
        dataSourceTextView = findViewById(R.id.dataSourceTextView);
        usesTextView = findViewById(R.id.usesTextView);
        chemicalComponentsTextView = findViewById(R.id.chemicalComponentsTextView);
        readMoreButton = findViewById(R.id.readMoreButton);
        viewImagesButton = findViewById(R.id.viewImagesButton);
        searchMoreButton = findViewById(R.id.searchMoreButton);
        backButton = findViewById(R.id.backButton);
        chemicalCard = findViewById(R.id.chemicalCard);
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

    private void setupButtons() {
        backButton.setOnClickListener(v -> finish());

        viewImagesButton.setOnClickListener(v -> openImageGallery());

        searchMoreButton.setOnClickListener(v -> searchMoreInformation());

        readMoreButton.setOnClickListener(v -> {
            if (isExpanded) {
                // Collapse
                String truncatedText = completeUsesText.substring(0, Math.min(MAX_USES_LENGTH, completeUsesText.length())) + "...";
                usesTextView.setText(truncatedText);
                readMoreButton.setText("Read More");
                isExpanded = false;
            } else {
                // Expand
                usesTextView.setText(completeUsesText);
                readMoreButton.setText("Read Less");
                isExpanded = true;
            }
        });
    }

    private void displayPlantData() {
        Intent intent = getIntent();

        // Extract plant data from intent
        String plantName = intent.getStringExtra("plantName");
        String scientificName = intent.getStringExtra("scientificName");
        String family = intent.getStringExtra("family");
        String habitat = intent.getStringExtra("habitat");
        String genus = intent.getStringExtra("genus");
        String uses = intent.getStringExtra("uses");
        String medicinalProperties = intent.getStringExtra("medicinalProperties");
        String chemicalComponents = intent.getStringExtra("chemicalComponents");
        String dataSource = intent.getStringExtra("dataSource");
        boolean autoGenerated = intent.getBooleanExtra("autoGenerated", false);

        Log.d(TAG, "=== DISPLAYING PLANT DETAIL DATA ===");
        Log.d(TAG, "Plant name: " + plantName);
        Log.d(TAG, "Scientific name: " + scientificName);
        Log.d(TAG, "Family: " + family);
        Log.d(TAG, "Uses: " + uses);
        Log.d(TAG, "Medicinal properties: " + medicinalProperties);

        // Display plant name
        if (isValidField(plantName)) {
            plantNameTextView.setText(plantName);
        } else {
            plantNameTextView.setText("Unknown Plant");
        }

        // Display scientific name
        if (isValidField(scientificName) && !scientificName.equals(plantName)) {
            scientificNameTextView.setText(scientificName);
            scientificNameTextView.setVisibility(View.VISIBLE);
        } else {
            scientificNameTextView.setVisibility(View.GONE);
        }

        // Display family
        if (isValidField(family)) {
            familyTextView.setText("Family: " + family);
            familyTextView.setVisibility(View.VISIBLE);
        } else {
            familyTextView.setVisibility(View.GONE);
        }

        // Display habitat
        if (isValidField(habitat)) {
            habitatTextView.setText("Habitat: " + habitat);
            habitatTextView.setVisibility(View.VISIBLE);
        } else {
            habitatTextView.setVisibility(View.GONE);
        }

        // Display genus
        if (isValidField(genus)) {
            genusTextView.setText("Genus: " + genus);
            genusTextView.setVisibility(View.VISIBLE);
        } else {
            genusTextView.setVisibility(View.GONE);
        }

        // Display data source
        String sourceText = "Source: ";
        if (autoGenerated) {
            sourceText += (isValidField(dataSource) ? dataSource + " (Generated)" : "Generated from Web Sources");
        } else {
            sourceText += "Knowledge Database";
        }
        dataSourceTextView.setText(sourceText);

        // Build and display medicinal uses
        displayMedicinalInformation(uses, medicinalProperties, chemicalComponents);
    }

    private void displayMedicinalInformation(String uses, String medicinalProperties, String chemicalComponents) {
        StringBuilder usesBuilder = new StringBuilder();
        boolean hasContent = false;

        // Add main uses
        if (isValidField(uses)) {
            usesBuilder.append("Traditional Uses:\n").append(uses);
            hasContent = true;
        }

        // Add medicinal properties
        if (isValidField(medicinalProperties) && !medicinalProperties.equals(uses)) {
            if (hasContent) usesBuilder.append("\n\nMedicinal Properties:\n");
            else usesBuilder.append("Medicinal Properties:\n");
            usesBuilder.append(medicinalProperties);
            hasContent = true;
        }

        // Display chemical components separately if available
        if (isValidField(chemicalComponents)) {
            chemicalComponentsTextView.setText(chemicalComponents);
            chemicalCard.setVisibility(View.VISIBLE);
        } else {
            chemicalCard.setVisibility(View.GONE);
        }

        // Handle uses display
        if (hasContent) {
            completeUsesText = usesBuilder.toString();

            if (completeUsesText.length() > MAX_USES_LENGTH) {
                String truncatedText = completeUsesText.substring(0, MAX_USES_LENGTH) + "...";
                usesTextView.setText(truncatedText);
                readMoreButton.setVisibility(View.VISIBLE);
            } else {
                usesTextView.setText(completeUsesText);
                readMoreButton.setVisibility(View.GONE);
            }
        } else {
            usesTextView.setText("No detailed medicinal information available for this plant in our database.\n\nYou can use 'Search More Information' to explore additional resources online.");
            readMoreButton.setVisibility(View.GONE);
        }
    }

    private boolean isValidField(String field) {
        if (field == null || field.trim().isEmpty()) {
            return false;
        }

        String trimmed = field.trim().toLowerCase();
        return !trimmed.equals("null") &&
                !trimmed.equals("none") &&
                !trimmed.equals("unknown") &&
                !trimmed.equals("not available") &&
                !trimmed.equals("n/a") &&
                !trimmed.equals("no information") &&
                !trimmed.equals("no data") &&
                !trimmed.equals("unknown family") &&
                !trimmed.equals("unknown plant") &&
                !trimmed.equals("uses to be researched") &&
                !trimmed.equals("traditional and ornamental uses");
    }

    private void openImageGallery() {
        String plantName = getIntent().getStringExtra("plantName");
        String scientificName = getIntent().getStringExtra("scientificName");

        if (!isValidField(plantName)) {
            Toast.makeText(this, "Plant name not available for image search", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PlantImageGalleryActivity.class);
        intent.putExtra("plant_name", plantName);
        intent.putExtra("scientific_name", scientificName);
        startActivity(intent);
    }

    private void searchMoreInformation() {
        String plantName = getIntent().getStringExtra("plantName");

        if (!isValidField(plantName)) {
            Toast.makeText(this, "Plant name not available for search", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PlantSearchActivity.class);
        intent.putExtra("search_query", plantName);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}