package com.example.herbai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResultActivity extends AppCompatActivity {
    private static final String TAG = "ResultActivity";
    private static final String BASE_API_URL = "https://serverv1-1.onrender.com";
    private SwitchMaterial themeSwitch;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "theme_prefs";
    private static final String NIGHT_MODE = "night_mode";
    private static final int MAX_USES_LENGTH = 200; // Character limit before showing "Read More"
    private Button viewImagesButton;
    private ArrayList<String> dbImageUrls;
    private boolean hasDbImages;
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
    private Button readMoreButton;

    // Store the complete uses text
    private String completeUsesText = "";
    private boolean isExpanded = false;

    // ExecutorService for background tasks
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load saved preference and apply the theme mode
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isNightMode = sharedPreferences.getBoolean(NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_result);

        // Initialize ExecutorService
        executorService = Executors.newFixedThreadPool(3);

        initializeViews();
        setupThemeSwitch();
        displayResults();
        setupButtons();
        setupImageGalleryFeature();
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
        viewImagesButton = findViewById(R.id.viewImagesButton);

        // Create Read More button programmatically
        readMoreButton = new Button(this);
        readMoreButton.setText("Read More");
        readMoreButton.setTextSize(12);
        readMoreButton.setBackgroundResource(android.R.drawable.btn_default);
        readMoreButton.setPadding(16, 8, 16, 8);
        readMoreButton.setVisibility(View.GONE);

        // Add the button to the layout
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 0);
        readMoreButton.setLayoutParams(params);
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

// Replace the displayResults() method in ResultActivity with this improved version

    private void displayResults() {
        Intent intent = getIntent();

        // Extract all data from intent
        String plantName = intent.getStringExtra("plantName");
        String scientificName = intent.getStringExtra("scientificName");
        String family = intent.getStringExtra("family");
        String habitat = intent.getStringExtra("habitat");
        String uses = intent.getStringExtra("uses");
        double confidence = intent.getDoubleExtra("confidence", 0.0);
        boolean isRealIdentification = intent.getBooleanExtra("isRealIdentification", false);
        boolean isFromSearchRoute = intent.getBooleanExtra("isFromSearchRoute", false);

        // Get additional database information
        String databaseUses = intent.getStringExtra("databaseUses");
        String databaseMedicinalProperties = intent.getStringExtra("databaseMedicinalProperties");
        String databaseChemicalComponents = intent.getStringExtra("databaseChemicalComponents");

        ArrayList<String> probablePlants = intent.getStringArrayListExtra("probablePlants");

        // Extract image data from intent
        dbImageUrls = intent.getStringArrayListExtra("dbImageUrls");
        hasDbImages = intent.getBooleanExtra("hasDbImages", false);

        // Debug logging
        Log.d(TAG, "=== DISPLAYING RESULTS ===");
        Log.d(TAG, "Plant name: '" + plantName + "'");
        Log.d(TAG, "Scientific name: '" + scientificName + "'");
        Log.d(TAG, "Family: '" + family + "'");
        Log.d(TAG, "Habitat: '" + habitat + "'");
        Log.d(TAG, "Uses: '" + uses + "'");
        Log.d(TAG, "Database uses: '" + databaseUses + "'");
        Log.d(TAG, "Database medicinal properties: '" + databaseMedicinalProperties + "'");

        // Display plant name
        if (plantName != null && !plantName.trim().isEmpty()) {
            topPlantNameTextView.setText(plantName);
        } else {
            topPlantNameTextView.setText("Plant Identified");
        }

        // Display scientific name (show only if different from plant name)
        if (scientificName != null && !scientificName.trim().isEmpty() &&
                !scientificName.equals(plantName) && !scientificName.equals("null")) {
            scientificNameTextView.setText("Scientific Name: " + scientificName);
            scientificNameTextView.setVisibility(View.VISIBLE);
        } else {
            scientificNameTextView.setVisibility(View.GONE);
        }

        // Display family
        if (isValidField(family)) {
            familyTextView.setText("Family: " + family);
            familyTextView.setVisibility(View.VISIBLE);
            Log.d(TAG, "Family displayed: " + family);
        } else {
            familyTextView.setVisibility(View.GONE);
            Log.d(TAG, "Family hidden - value was: '" + family + "'");
        }

        // Display habitat
        if (isValidField(habitat)) {
            habitatTextView.setText("Habitat: " + habitat);
            habitatTextView.setVisibility(View.VISIBLE);
            Log.d(TAG, "Habitat displayed: " + habitat);
        } else {
            habitatTextView.setVisibility(View.GONE);
            Log.d(TAG, "Habitat hidden - value was: '" + habitat + "'");
        }

        // Display medicinal properties and uses
        displayMedicinalUses(uses, databaseUses, databaseMedicinalProperties, databaseChemicalComponents);

        // Display confidence
        if (confidence > 0) {
            confidenceTextView.setText(String.format("Confidence: %.1f%%", confidence * 100));
            confidenceTextView.setVisibility(View.VISIBLE);
        } else {
            confidenceTextView.setVisibility(View.GONE);
        }

        // Display identification status
        displayIdentificationStatus(isRealIdentification, confidence, isFromSearchRoute);

        // Display probable plants
        displayProbablePlants(probablePlants);
    }

    // Replace the displayMedicinalUses method in ResultActivity with this fixed version
    private void displayMedicinalUses(String uses, String databaseUses, String databaseMedicinalProperties, String databaseChemicalComponents) {
        StringBuilder usesText = new StringBuilder();

        Log.d(TAG, "=== BUILDING MEDICINAL USES TEXT ===");
        Log.d(TAG, "Uses: '" + uses + "'");
        Log.d(TAG, "Database uses: '" + databaseUses + "'");
        Log.d(TAG, "Database medicinal properties: '" + databaseMedicinalProperties + "'");
        Log.d(TAG, "Database chemical components: '" + databaseChemicalComponents + "'");

        boolean hasContent = false;

        // Primary uses field (this should contain the main information from smart_search)
        if (isValidField(uses)) {
            usesText.append("Uses:\n").append(uses);
            hasContent = true;
            Log.d(TAG, "Added primary uses");
        }

        // Add database uses if different and valid
        if (isValidField(databaseUses) && !databaseUses.equals(uses)) {
            if (hasContent) usesText.append("\n\nAdditional Traditional Uses:\n");
            else usesText.append("Traditional Uses:\n");
            usesText.append(databaseUses);
            hasContent = true;
            Log.d(TAG, "Added database uses");
        }

        // Add medicinal properties if different and valid
        if (isValidField(databaseMedicinalProperties) &&
                !databaseMedicinalProperties.equals(uses) &&
                !databaseMedicinalProperties.equals(databaseUses)) {
            if (hasContent) usesText.append("\n\nMedicinal Properties:\n");
            else usesText.append("Medicinal Properties:\n");
            usesText.append(databaseMedicinalProperties);
            hasContent = true;
            Log.d(TAG, "Added medicinal properties");
        }

        // Add chemical components if valid
        if (isValidField(databaseChemicalComponents)) {
            if (hasContent) usesText.append("\n\nActive Compounds:\n");
            else usesText.append("Active Compounds:\n");
            usesText.append(databaseChemicalComponents);
            hasContent = true;
            Log.d(TAG, "Added chemical components");
        }

        String finalUsesText = usesText.toString();
        Log.d(TAG, "Final uses text length: " + finalUsesText.length());
        Log.d(TAG, "Final uses text: '" + finalUsesText + "'");

        if (hasContent) {
            completeUsesText = finalUsesText;

            // Check if text needs truncation
            if (completeUsesText.length() > MAX_USES_LENGTH) {
                String truncatedText = completeUsesText.substring(0, MAX_USES_LENGTH) + "...";
                plantUsesTextView.setText(truncatedText);
                setupReadMoreButton();
            } else {
                plantUsesTextView.setText(completeUsesText);
                if (readMoreButton != null) {
                    readMoreButton.setVisibility(View.GONE);
                }
            }
            plantUsesTextView.setVisibility(View.VISIBLE);
            Log.d(TAG, "Uses text displayed successfully");
        } else {
            // Show helpful message when no uses data is available
            String plantNameForSearch = getIntent().getStringExtra("plantName");
            String message = "No medicinal information available for this plant in our database.\n\n" +
                    "You can:\n" +
                    "• Use 'Search More' to explore additional resources\n" +
                    "• Try searching for the scientific name\n" +
                    "• Consult botanical references for this species";

            plantUsesTextView.setText(message);
            plantUsesTextView.setVisibility(View.VISIBLE);
            if (readMoreButton != null) {
                readMoreButton.setVisibility(View.GONE);
            }
            Log.d(TAG, "No uses information available - showing helpful message");
        }
    }

    // Also update the isValidField method to be more lenient
    private boolean isValidField(String field) {
        if (field == null || field.trim().isEmpty()) {
            return false;
        }

        String trimmed = field.trim().toLowerCase();
        // Remove overly restrictive checks that might filter out valid uses
        return !trimmed.equals("null") &&
                !trimmed.equals("none") &&
                !trimmed.equals("unknown") &&
                !trimmed.equals("not available") &&
                !trimmed.equals("n/a") &&
                !trimmed.equals("no information") &&
                !trimmed.equals("no data");
    }
    private static StringBuilder getUsesText(StringBuilder usesText) {
        return usesText;
    }


    private void setupReadMoreButton() {
        // Remove button if already added
        LinearLayout parent = (LinearLayout) plantUsesTextView.getParent();
        if (readMoreButton.getParent() != null) {
            ((LinearLayout) readMoreButton.getParent()).removeView(readMoreButton);
        }

        // Add button after plantUsesTextView
        parent.addView(readMoreButton, parent.indexOfChild(plantUsesTextView) + 1);
        readMoreButton.setVisibility(View.VISIBLE);

        readMoreButton.setOnClickListener(v -> {
            if (isExpanded) {
                // Collapse
                String truncatedText = completeUsesText.substring(0, MAX_USES_LENGTH) + "...";
                plantUsesTextView.setText(truncatedText);
                readMoreButton.setText("Read More");
                isExpanded = false;
            } else {
                // Expand
                plantUsesTextView.setText(completeUsesText);
                readMoreButton.setText("Read Less");
                isExpanded = true;
            }
        });
    }

    private void displayIdentificationStatus(boolean isRealIdentification, double confidence, boolean isFromSearchRoute) {
        if (isFromSearchRoute) {
            identificationStatusCard.setVisibility(View.GONE);
            return;
        }

        identificationStatusCard.setVisibility(View.VISIBLE);

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
            identificationStatusCard.setVisibility(View.GONE);
        }
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

    private void setupImageGalleryFeature() {
        // Get image data from intent
        dbImageUrls = getIntent().getStringArrayListExtra("dbImageUrls");
        hasDbImages = getIntent().getBooleanExtra("hasDbImages", false);

        // Setup view images button
        if (viewImagesButton != null) {
            if (hasDbImages && dbImageUrls != null && !dbImageUrls.isEmpty()) {
                viewImagesButton.setVisibility(View.VISIBLE);
                viewImagesButton.setText("View " + dbImageUrls.size() + " Images 📸");
                viewImagesButton.setOnClickListener(v -> openImageGallery());
            } else {
                viewImagesButton.setVisibility(View.GONE);
            }
        }

        // Also make the plant name clickable to view images
        topPlantNameTextView.setOnClickListener(v -> {
            if (hasDbImages) {
                openImageGallery();
            } else {
                Toast.makeText(this, "No images available for this plant", Toast.LENGTH_SHORT).show();
            }
        });

        // Add visual indicator if images are available
        if (hasDbImages && dbImageUrls != null && !dbImageUrls.isEmpty()) {
            topPlantNameTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_camera_small, 0);
            topPlantNameTextView.setCompoundDrawablePadding(8);
        }
    }

    private void openImageGallery() {
        String plantName = getIntent().getStringExtra("plantName");
        String scientificName = getIntent().getStringExtra("scientificName");

        if (plantName == null || plantName.trim().isEmpty()) {
            Toast.makeText(this, "Plant name not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PlantImageGalleryActivity.class);
        intent.putExtra("plant_name", plantName);
        intent.putExtra("scientific_name", scientificName);

        // Pass the image URLs if we already have them
        if (dbImageUrls != null && !dbImageUrls.isEmpty()) {
            intent.putStringArrayListExtra("preloaded_images", dbImageUrls);
        }

        startActivity(intent);
        Log.d(TAG, "Opened image gallery for: " + plantName);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown executor service
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}