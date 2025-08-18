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
    // Replace your displayResults() method in ResultActivity with this fixed version:
    private void displayResults() {
        Intent intent = getIntent();

        // Get plant information from intent
        String plantName = intent.getStringExtra("plantName");
        String scientificName = intent.getStringExtra("scientificName");
        String family = intent.getStringExtra("family");
        String habitat = intent.getStringExtra("habitat");
        String serverUses = intent.getStringExtra("topPlantUses");
        double confidence = intent.getDoubleExtra("confidence", 0.0);
        boolean isRealIdentification = intent.getBooleanExtra("isRealIdentification", false);
        boolean isFromSearchRoute = getIntent().getBooleanExtra("isFromSearchRoute", false);


        ArrayList<String> probablePlants = intent.getStringArrayListExtra("probablePlants");

        // **CRITICAL: Get database information from intent**
        String databaseUses = intent.getStringExtra("databaseUses");
        String databaseMedicinalProperties = intent.getStringExtra("databaseMedicinalProperties");
        String databaseChemicalComponents = intent.getStringExtra("databaseChemicalComponents");
        boolean hasKnowledgeGraphData = intent.getBooleanExtra("hasKnowledgeGraphData", false);
        String dataSource = intent.getStringExtra("dataSource");

        // Extract image data from intent
        dbImageUrls = intent.getStringArrayListExtra("dbImageUrls");
        hasDbImages = intent.getBooleanExtra("hasDbImages", false);

        Log.d(TAG, "=== DISPLAYING RESULTS ===");
        Log.d(TAG, "Plant: " + plantName);
        Log.d(TAG, "Has knowledge graph data: " + hasKnowledgeGraphData);
        Log.d(TAG, "Data source: " + dataSource);
        Log.d(TAG, "Database uses available: " + (databaseUses != null && !databaseUses.isEmpty()));
        Log.d(TAG, "Database uses length: " + (databaseUses != null ? databaseUses.length() : 0));

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

        // **CRITICAL FIX: Display database uses directly if available**
        if (hasKnowledgeGraphData) {
            Log.d(TAG, "Using knowledge graph data directly - NO API CALLS");
            displayDatabaseUsesDirectly(databaseUses, databaseMedicinalProperties,
                    databaseChemicalComponents, dataSource);
        } else {
            Log.d(TAG, "No knowledge graph data - falling back to API");
            displayDatabaseUses(databaseUses, databaseMedicinalProperties,
                    databaseChemicalComponents, serverUses);
        }

        // Display identification status
        displayIdentificationStatus(isRealIdentification, confidence,isFromSearchRoute);

        // Display probable plants
        displayProbablePlants(probablePlants);
    }

    /**
     * NEW METHOD: Display database uses directly without any API calls
     */
    private void displayDatabaseUsesDirectly(String databaseUses, String medicinalProperties,
                                             String chemicalComponents, String dataSource) {

        Log.d(TAG, "=== DISPLAYING DATABASE USES DIRECTLY ===");
        Log.d(TAG, "Database uses: " + (databaseUses != null ?
                databaseUses.substring(0, Math.min(100, databaseUses.length())) + "..." : "null"));

        StringBuilder usesText = new StringBuilder();
        boolean hasAnyMeaningfulInfo = false;

        // Add data source header
        if (dataSource != null && !dataSource.isEmpty()) {
            if (dataSource.equals("Knowledge Graph")) {
                usesText.append("📚 Knowledge Graph Information:\n\n");
            } else {
                usesText.append("🌐 Information from ").append(dataSource).append(":\n\n");
            }
        }

        // **PRIORITY: Display uses information first (this is the main content)**
        if (databaseUses != null && !databaseUses.trim().isEmpty() &&
                !databaseUses.equalsIgnoreCase("null") && !isGenericDescription(databaseUses)) {

            usesText.append("📋 Traditional Uses:\n");
            usesText.append(databaseUses.trim()).append("\n\n");
            hasAnyMeaningfulInfo = true;

            Log.d(TAG, "✓ Added database uses (" + databaseUses.length() + " chars)");
        } else {
            Log.d(TAG, "✗ Database uses not meaningful or empty");
        }

        // Add medicinal properties if available and meaningful
        if (medicinalProperties != null && !medicinalProperties.trim().isEmpty() &&
                !medicinalProperties.equalsIgnoreCase("null") &&
                !medicinalProperties.equals("Under research") &&
                !medicinalProperties.equals("Information not available")) {

            usesText.append("💊 Medicinal Properties:\n");
            usesText.append(medicinalProperties.trim()).append("\n\n");
            hasAnyMeaningfulInfo = true;

            Log.d(TAG, "✓ Added medicinal properties");
        }

        // Add chemical components if available and meaningful
        if (chemicalComponents != null && !chemicalComponents.trim().isEmpty() &&
                !chemicalComponents.equalsIgnoreCase("null") &&
                !chemicalComponents.equals("Various organic compounds") &&
                !chemicalComponents.equals("Components under study")) {

            usesText.append("🧪 Active Compounds:\n");
            usesText.append(chemicalComponents.trim()).append("\n\n");
            hasAnyMeaningfulInfo = true;

            Log.d(TAG, "✓ Added chemical components");
        }

        // If we have meaningful information, display it
        if (hasAnyMeaningfulInfo) {
            // Add safety disclaimer
            usesText.append("⚠️ Safety Notice:\n");
            usesText.append("Always consult with healthcare practitioners before using any plant medicinally. ");
            usesText.append("Proper identification is essential for safety.");

            Log.d(TAG, "✓ Displaying " + usesText.length() + " characters of database information");
            displayPlantUses(usesText.toString());
        } else {
            // Last resort fallback
            Log.w(TAG, "No meaningful database information found - using generic fallback");
            String fallbackText = "Plant Information:\n\n" +
                    "This plant has been identified in our database. " +
                    "Many plants have traditional medicinal and cultural uses. " +
                    "For detailed information about uses and properties, " +
                    "consult botanical references or traditional medicine practitioners.\n\n" +
                    "⚠️ Safety Notice:\n" +
                    "Always verify plant identification and consult healthcare practitioners " +
                    "before using any plant for medicinal purposes.";

            displayPlantUses(fallbackText);
        }
    }

    /**
     * UPDATED METHOD: Only use this if we don't have knowledge graph data
     */
    private void displayDatabaseUses(String databaseUses, String medicinalProperties,
                                     String chemicalComponents, String serverUses) {

        // **This method should only be called if hasKnowledgeGraphData is false**
        Log.d(TAG, "Using original displayDatabaseUses method as fallback");

        StringBuilder usesText = new StringBuilder();
        boolean hasAnyMeaningfulInfo = false;

        // Check if we have meaningful database information
        if (databaseUses != null && !databaseUses.trim().isEmpty() &&
                !databaseUses.equalsIgnoreCase("null") && !isGenericDescription(databaseUses)) {

            usesText.append("📋 Traditional Uses:\n").append(databaseUses.trim()).append("\n\n");
            hasAnyMeaningfulInfo = true;
            Log.d(TAG, "Found meaningful database uses");
        }

        if (medicinalProperties != null && !medicinalProperties.trim().isEmpty() &&
                !medicinalProperties.equalsIgnoreCase("null") &&
                !medicinalProperties.equals("Under research") &&
                !medicinalProperties.equals("Information not available")) {

            usesText.append("💊 Medicinal Properties:\n").append(medicinalProperties.trim()).append("\n\n");
            hasAnyMeaningfulInfo = true;
        }

        if (chemicalComponents != null && !chemicalComponents.trim().isEmpty() &&
                !chemicalComponents.equalsIgnoreCase("null") &&
                !chemicalComponents.equals("Various organic compounds") &&
                !chemicalComponents.equals("Components under study")) {

            usesText.append("🧪 Active Compounds:\n").append(chemicalComponents.trim()).append("\n\n");
            hasAnyMeaningfulInfo = true;
        }

        if (hasAnyMeaningfulInfo) {
            usesText.append("⚠️ Safety Notice:\nAlways consult with healthcare practitioners before using any plant medicinally. Proper identification is essential for safety.");
            Log.d(TAG, "Displaying meaningful database information directly");
            displayPlantUses(usesText.toString());
        } else {
            // Only fall back to API if no meaningful database info
            Log.d(TAG, "No meaningful database info - trying API as last resort");
            String plantName = getIntent().getStringExtra("plantName");
            String scientificName = getIntent().getStringExtra("scientificName");
            fetchEnhancedPlantUses(plantName, scientificName, serverUses);
        }
    }
    /**
     * NEW METHOD: Display uses directly from database without API calls
     */

    /**
     * Helper method to check if uses text is generic/meaningless
     */
    private boolean isGenericDescription(String text) {
        if (text == null || text.trim().isEmpty()) {
            return true;
        }

        String lowerText = text.toLowerCase().trim();

        // Common generic phrases to filter out
        String[] genericPhrases = {
                "a rose is either a woody perennial flowering plant",
                "they form",
                "there are over three hundred species",
                "uses to be researched",
                "traditional and ornamental uses",
                "information not available",
                "not available",
                "under research"
        };

        for (String phrase : genericPhrases) {
            if (lowerText.contains(phrase)) {
                Log.d(TAG, "Identified generic content: " + phrase);
                return true;
            }
        }

        // Check if it's too short to be meaningful
        if (text.length() < 25) {
            Log.d(TAG, "Content too short to be meaningful: " + text.length() + " chars");
            return true;
        }

        return false;
    }

    /**
     * SIMPLIFIED: Updated fetchEnhancedPlantUses - only as last resort
     */
    private void fetchEnhancedPlantUses(String plantName, String scientificName, String serverUses) {
        Log.d(TAG, "API fetch as last resort for: " + plantName);

        // Show loading state
        plantUsesTextView.setText("Loading plant information...");

        executorService.execute(() -> {
            try {
                String enhancedUses = getEnhancedPlantUsesFromBackend(plantName, scientificName, serverUses);

                runOnUiThread(() -> {
                    if (enhancedUses != null && !enhancedUses.isEmpty() && !isGenericDescription(enhancedUses)) {
                        displayPlantUses(enhancedUses);
                        Log.d(TAG, "Successfully got enhanced uses from API");
                    } else {
                        // Final fallback
                        String fallbackUses = getFallbackUses(serverUses);
                        displayPlantUses(fallbackUses);
                        Log.d(TAG, "Used final fallback uses");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "API fetch failed: " + e.getMessage());
                runOnUiThread(() -> {
                    String fallbackUses = getFallbackUses(serverUses);
                    displayPlantUses(fallbackUses);
                });
            }
        });
    }

    private String getEnhancedPlantUsesFromBackend(String plantName, String scientificName, String serverUses) {
        try {
            // Try with primary plant name first
            String result = queryBackendAPI(plantName != null ? plantName : "");

            if (result == null && scientificName != null && !scientificName.trim().isEmpty()) {
                // Fallback to scientific name
                Log.d(TAG, "Trying with scientific name: " + scientificName);
                result = queryBackendAPI(scientificName);
            }

            if (result != null) {
                return result;
            }

            return getFallbackUses(serverUses);

        } catch (Exception e) {
            Log.e(TAG, "Backend query failed: " + e.getMessage());
            return getFallbackUses(serverUses);
        }
    }

    private String queryBackendAPI(String plantName) {
        if (plantName == null || plantName.trim().isEmpty()) {
            return null;
        }

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            // Use smart_search endpoint for better results
            String encodedPlantName = URLEncoder.encode(plantName.trim(), "UTF-8");
            String urlString = BASE_API_URL + "/smart_search/" + encodedPlantName;

            Log.d(TAG, "Querying backend API: " + urlString);

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000); // 15 seconds
            connection.setReadTimeout(15000); // 15 seconds
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

                return parseBackendResponse(jsonResponse);
            } else {
                Log.w(TAG, "Backend API returned error code: " + responseCode);
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error querying backend API: " + e.getMessage());
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

    private String parseBackendResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            boolean success = jsonObject.optBoolean("success", false);

            if (!success) {
                Log.w(TAG, "Backend API returned success=false");
                return null;
            }

            // Parse the results array from the new backend format
            JSONArray results = jsonObject.optJSONArray("results");
            if (results == null || results.length() == 0) {
                Log.w(TAG, "No results array found in backend response");
                return null;
            }

            Log.d(TAG, "Found " + results.length() + " results from backend");

            // **EXTRACT IMAGES WHILE PARSING RESULTS**
            extractImagesFromBackendResponse(jsonObject);

            // Find the best result with uses information
            JSONObject bestPlantData = findBestResult(results);
            if (bestPlantData == null) {
                Log.w(TAG, "No suitable result found with plant uses information");
                return null;
            }

            Log.d(TAG, "Selected best result: " + bestPlantData.optString("plant_name", "Unknown"));

            StringBuilder enhancedUses = new StringBuilder();

            // Extract plant information from the best result
            String plantName = bestPlantData.optString("plant_name", "");
            String scientificName = bestPlantData.optString("scientific_name", "");
            String family = bestPlantData.optString("family", "");
            String uses = bestPlantData.optString("uses", "");
            String medicinalProperties = bestPlantData.optString("medicinal_properties", "");
            String chemicalComponents = bestPlantData.optString("chemical_components", "");
            String habitat = bestPlantData.optString("habitat", "");
            boolean autoGenerated = bestPlantData.optBoolean("auto_generated", false);
            String dataSource = bestPlantData.optString("data_source", "");

            // Build comprehensive information
            if (autoGenerated) {
                enhancedUses.append("🌐 Information from ").append(dataSource.isEmpty() ? "APIs" : dataSource).append(":\n\n");
            } else {
                enhancedUses.append("📚 Knowledge Graph Information:\n\n");
            }

            // Add scientific classification if available
            if (!scientificName.isEmpty() && !scientificName.equals("Unknown") && !scientificName.equalsIgnoreCase("null")) {
                enhancedUses.append("Scientific Name: ").append(scientificName);
                if (!family.isEmpty() && !family.equals("Unknown Family") && !family.equals("Unknown") && !family.equalsIgnoreCase("null")) {
                    enhancedUses.append(" (Family: ").append(family).append(")");
                }
                enhancedUses.append("\n\n");
            }

            // Add medicinal properties - prioritize this since it's often missing
            if (!medicinalProperties.isEmpty() && !medicinalProperties.equals("Information not available")
                    && !medicinalProperties.equals("Under research") && !medicinalProperties.equalsIgnoreCase("null")) {
                enhancedUses.append("Medicinal Properties: ").append(medicinalProperties).append("\n\n");
            }

            // Add uses information (this is the main content we're looking for)
            if (!uses.isEmpty() && !uses.equals("Uses to be researched")
                    && !uses.equals("Traditional and ornamental uses") && !uses.equalsIgnoreCase("null")) {
                enhancedUses.append("Traditional Uses: ").append(uses).append("\n\n");
            }

            // Add chemical components
            if (!chemicalComponents.isEmpty() && !chemicalComponents.equals("Components under study")
                    && !chemicalComponents.equals("Various organic compounds") && !chemicalComponents.equalsIgnoreCase("null")) {
                enhancedUses.append("Active Compounds: ").append(chemicalComponents).append("\n\n");
            }

            // Add habitat information
            if (!habitat.isEmpty() && !habitat.equals("Various environments") && !habitat.equalsIgnoreCase("null")) {
                enhancedUses.append("Habitat: ").append(habitat).append("\n\n");
            }

            // If we have substantial information, return it
            if (enhancedUses.length() > 100) {
                // Add safety disclaimer
                enhancedUses.append("⚠️ Safety Notice: Always consult with healthcare practitioners or qualified herbalists before using any plant for medicinal purposes. Proper plant identification is essential for safety.");

                Log.d(TAG, "Successfully parsed backend response with " + enhancedUses.length() + " characters");
                return enhancedUses.toString();
            }

            Log.w(TAG, "Backend response had insufficient information");
            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error parsing backend response: " + e.getMessage());
            return null;
        }
    }

    private void extractImagesFromBackendResponse(JSONObject jsonResponse) {
        try {
            ArrayList<String> collectedImages = new ArrayList<>();

            // Extract from results array - but ONLY the first/best result
            JSONArray results = jsonResponse.optJSONArray("results");
            if (results != null && results.length() > 0) {
                // Only process the FIRST result (the best match)
                JSONObject firstResult = results.getJSONObject(0);
                String firstResultName = firstResult.optString("plant_name", "Unknown");

                Log.d(TAG, "Extracting images only from first/best result: " + firstResultName);

                if (firstResult.has("image_urls")) {
                    JSONArray imageUrls = firstResult.getJSONArray("image_urls");
                    for (int j = 0; j < imageUrls.length(); j++) {
                        String imageUrl = imageUrls.getString(j);
                        if (isValidImageUrl(imageUrl) && !collectedImages.contains(imageUrl)) {
                            collectedImages.add(imageUrl);
                        }
                    }
                    Log.d(TAG, "Added " + imageUrls.length() + " images from first result");
                }
            }

            // Extract from direct db_image_urls if present (should be for main predicted plant)
            if (jsonResponse.has("db_image_urls")) {
                JSONArray dbImages = jsonResponse.getJSONArray("db_image_urls");
                for (int i = 0; i < dbImages.length(); i++) {
                    String imageUrl = dbImages.getString(i);
                    if (isValidImageUrl(imageUrl) && !collectedImages.contains(imageUrl)) {
                        collectedImages.add(imageUrl);
                    }
                }
                Log.d(TAG, "Added " + dbImages.length() + " direct db_image_urls");
            }

            // Extract from db_matches - but ONLY the FIRST match (highest confidence)
            if (jsonResponse.has("db_matches")) {
                JSONArray dbMatches = jsonResponse.getJSONArray("db_matches");

                // Only process the FIRST db_match
                if (dbMatches.length() > 0) {
                    JSONObject firstMatch = dbMatches.getJSONObject(0);
                    String firstMatchSpecies = firstMatch.optString("species", "Unknown");

                    Log.d(TAG, "Processing db_matches only for first match: " + firstMatchSpecies);

                    if (firstMatch.has("image_urls")) {
                        JSONArray matchImages = firstMatch.getJSONArray("image_urls");
                        for (int j = 0; j < matchImages.length(); j++) {
                            String imageUrl = matchImages.getString(j);
                            if (isValidImageUrl(imageUrl) && !collectedImages.contains(imageUrl)) {
                                collectedImages.add(imageUrl);
                            }
                        }
                        Log.d(TAG, "Added " + matchImages.length() + " images from first db_match");
                    }
                }
            }

            // Update the instance variables
            if (!collectedImages.isEmpty()) {
                if (dbImageUrls == null) {
                    dbImageUrls = new ArrayList<>();
                }
                dbImageUrls.clear();
                dbImageUrls.addAll(collectedImages);
                hasDbImages = true;

                Log.d(TAG,
                        "Final count: " + dbImageUrls.size() + " images for main predicted plant only");

                // Update UI on main thread
                runOnUiThread(() -> updateImageGalleryButton());
            } else {
                Log.d(TAG, "No images found for the main predicted plant");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error extracting images for main plant: " + e.getMessage());
        }
    }
    private boolean isValidImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String lowerUrl = url.toLowerCase();
        return (lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://")) &&
                (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg") ||
                        lowerUrl.contains(".png") || lowerUrl.contains(".gif") ||
                        lowerUrl.contains("cloudinary.com") || lowerUrl.contains("image"));
    }

    private void updateImageGalleryButton() {
        if (viewImagesButton != null) {
            if (hasDbImages && dbImageUrls != null && !dbImageUrls.isEmpty()) {
                viewImagesButton.setVisibility(View.VISIBLE);
                viewImagesButton.setText("View "+ " Images 📸");
                Log.d(TAG, "Updated view images button: " + dbImageUrls.size() + " images available");
            } else {
                viewImagesButton.setVisibility(View.GONE);
                Log.d(TAG, "No images available for gallery button");
            }
        }

        // Update plant name clickable indicator
        if (hasDbImages && dbImageUrls != null && !dbImageUrls.isEmpty()) {
            topPlantNameTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_camera_small, 0);
            topPlantNameTextView.setCompoundDrawablePadding(8);
        }
    }

    /**
     * Enhanced findBestResult method that prioritizes results with meaningful uses information
     * and filters out generic/empty responses
     */
    private JSONObject findBestResult(JSONArray results) {
        try {
            JSONObject bestResult = null;
            int bestScore = -1;

            Log.d(TAG, "Evaluating " + results.length() + " results to find the one with best uses info:");

            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);
                int score = calculateResultScore(result);

                String plantName = result.optString("plant_name", "Unknown");
                String uses = result.optString("uses", "");
                String shortUses = uses.length() > 50 ? uses.substring(0, 50) + "..." : uses;

                Log.d(TAG, "Result " + (i + 1) + ": " + plantName +
                        " | Score: " + score +
                        " | Uses: " + (uses.isEmpty() ? "[EMPTY]" : "[" + shortUses + "]"));

                if (score > bestScore) {
                    bestScore = score;
                    bestResult = result;
                    Log.d(TAG, "New best result found with score: " + bestScore);
                }
            }

            if (bestResult != null && bestScore > 0) {
                String selectedName = bestResult.optString("plant_name", "Unknown");
                Log.d(TAG, "Final selection: " + selectedName + " with score: " + bestScore);
                return bestResult;
            } else {
                Log.w(TAG, "No result had meaningful uses information, using fallback logic");

                // Try to find ANY result with non-empty uses as fallback
                for (int i = 0; i < results.length(); i++) {
                    JSONObject result = results.getJSONObject(i);
                    String uses = result.optString("uses", "").trim();

                    if (!uses.isEmpty() &&
                            !uses.equals("Uses to be researched") &&
                            !uses.equals("Traditional and ornamental uses") &&
                            !uses.toLowerCase().contains("not available")) {

                        Log.d(TAG, "Found fallback result with some uses: " + result.optString("plant_name", "Unknown"));
                        return result;
                    }
                }

                // Last resort - return first result
                Log.w(TAG, "No results with meaningful uses found, returning first result");
                return results.getJSONObject(0);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error finding best result: " + e.getMessage());
            try {
                return results.getJSONObject(0);
            } catch (Exception fallbackError) {
                Log.e(TAG, "Fallback also failed: " + fallbackError.getMessage());
                return null;
            }
        }
    }

    /**
     * Enhanced scoring system that heavily prioritizes meaningful uses information
     */
    private int calculateResultScore(JSONObject result) {
        int score = 0;

        try {
            // PRIORITY 1: Uses information (highest weight - this is what you want)
            String uses = result.optString("uses", "").trim();
            if (!uses.isEmpty() && !uses.equalsIgnoreCase("null")) {
                String lowerUses = uses.toLowerCase();

                // Heavily penalize generic/empty uses
                if (lowerUses.equals("uses to be researched") ||
                        lowerUses.equals("traditional and ornamental uses") ||
                        lowerUses.contains("not available") ||
                        lowerUses.contains("information not available") ||
                        lowerUses.contains("a rose is either") || // Filter out generic descriptions
                        lowerUses.contains("they form") ||
                        uses.length() < 20) { // Too short to be meaningful

                    score -= 50; // Heavy penalty for generic content
                    Log.d(TAG, "Penalized generic uses: " + uses.substring(0, Math.min(uses.length(), 30)));
                } else {
                    score += 200; // Very high priority for actual uses

                    // Extra bonus for detailed uses (this is what you want to display)
                    if (uses.length() > 300) {
                        score += 100; // Very detailed
                    } else if (uses.length() > 200) {
                        score += 80;
                    } else if (uses.length() > 100) {
                        score += 60;
                    } else if (uses.length() > 50) {
                        score += 40;
                    }

                    // Bonus for specific medicinal keywords
                    if (lowerUses.contains("medicinal") || lowerUses.contains("treatment") ||
                            lowerUses.contains("therapeutic") || lowerUses.contains("cure") ||
                            lowerUses.contains("astringent") || lowerUses.contains("digestive") ||
                            lowerUses.contains("antibacterial") || lowerUses.contains("antioxidant") ||
                            lowerUses.contains("traditional") || lowerUses.contains("remedy")) {
                        score += 50;
                    }

                    Log.d(TAG, "Found meaningful uses (" + uses.length() + " chars): " +
                            uses.substring(0, Math.min(uses.length(), 50)) + "...");
                }
            } else {
                score -= 100; // Heavy penalty for no uses information
            }

            // PRIORITY 2: Medicinal properties
            String medicinalProperties = result.optString("medicinal_properties", "").trim();
            if (!medicinalProperties.isEmpty() && !medicinalProperties.equalsIgnoreCase("null")) {
                String lowerProps = medicinalProperties.toLowerCase();
                if (!lowerProps.equals("information not available") &&
                        !lowerProps.equals("under research") &&
                        !lowerProps.equals("components under study") &&
                        medicinalProperties.length() > 15) {
                    score += 80;
                }
            }

            // PRIORITY 3: Chemical components (relevant for medicinal uses)
            String chemicalComponents = result.optString("chemical_components", "").trim();
            if (!chemicalComponents.isEmpty() && !chemicalComponents.equalsIgnoreCase("null")) {
                String lowerChemical = chemicalComponents.toLowerCase();
                if (!lowerChemical.equals("components under study") &&
                        !lowerChemical.equals("various organic compounds") &&
                        chemicalComponents.length() > 15) {
                    score += 60;
                }
            }

            // Lower priority items
            String scientificName = result.optString("scientific_name", "").trim();
            if (!scientificName.isEmpty() && !scientificName.equals("Unknown") &&
                    !scientificName.equalsIgnoreCase("null") && !scientificName.equals("There are")) {
                score += 30;
            }

            String family = result.optString("family", "").trim();
            if (!family.isEmpty() && !family.equals("Unknown") && !family.equalsIgnoreCase("null")) {
                score += 20;
            }

            // Prefer knowledge graph data over auto-generated
            boolean autoGenerated = result.optBoolean("auto_generated", false);
            if (!autoGenerated) {
                score += 25;
            }

            Log.d(TAG, "Final score: " + score + " for " + result.optString("plant_name", "Unknown"));

        } catch (Exception e) {
            Log.e(TAG, "Error calculating result score: " + e.getMessage());
            return -100; // Return very low score for problematic results
        }

        return score;
    }

    /**
     * Additional method to validate if a result has meaningful uses
     */
    private boolean hasmeaningfulUses(JSONObject result) {
        try {
            String uses = result.optString("uses", "").trim();

            if (uses.isEmpty() || uses.equalsIgnoreCase("null")) {
                return false;
            }

            String lowerUses = uses.toLowerCase();

            // Check for generic/meaningless content
            String[] genericPhrases = {
                    "uses to be researched",
                    "traditional and ornamental uses",
                    "information not available",
                    "not available",
                    "a rose is either",
                    "they form"
            };

            for (String phrase : genericPhrases) {
                if (lowerUses.contains(phrase)) {
                    return false;
                }
            }

            // Must have minimum length to be meaningful
            return uses.length() >= 30;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Method to filter results to only those with meaningful uses before scoring
     */
    private JSONObject findResultWithBestUses(JSONArray results) {
        try {
            // First pass: collect only results with meaningful uses
            ArrayList<JSONObject> meaningfulResults = new ArrayList<>();

            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);
                if (hasmeaningfulUses(result)) {
                    meaningfulResults.add(result);
                    Log.d(TAG, "Found meaningful uses in: " + result.optString("plant_name", "Unknown"));
                }
            }

            if (meaningfulResults.isEmpty()) {
                Log.w(TAG, "No results with meaningful uses found");
                return results.length() > 0 ? results.getJSONObject(0) : null;
            }

            // Second pass: find the best among meaningful results
            JSONObject bestResult = null;
            int bestScore = -1;

            for (JSONObject result : meaningfulResults) {
                int score = calculateResultScore(result);
                if (score > bestScore) {
                    bestScore = score;
                    bestResult = result;
                }
            }

            Log.d(TAG, "Selected result with meaningful uses: " +
                    (bestResult != null ? bestResult.optString("plant_name", "Unknown") : "None"));

            return bestResult;

        } catch (Exception e) {
            Log.e(TAG, "Error in findResultWithBestUses: " + e.getMessage());
            try {
                return results.getJSONObject(0);
            } catch (Exception fallbackError) {
                return null;
            }
        }
    }

    /**
     * Method to filter results to only those with meaningful uses before scoring
     */

    private String getFallbackUses(String serverUses) {
        StringBuilder fallbackUses = new StringBuilder();

        // Use server uses if available
        if (serverUses != null && !serverUses.trim().isEmpty()
                && !serverUses.contains("not available")
                && !serverUses.contains("local processing")) {
            fallbackUses.append("Server Analysis: ").append(serverUses.trim()).append("\n\n");
        }

        // Add general information
        if (fallbackUses.length() == 0) {
            fallbackUses.append("Plant Information: ");
        }

        fallbackUses.append("This plant may have medicinal and traditional uses. ")
                .append("Many plants have been used throughout history for various therapeutic purposes. ")
                .append("Different cultures and regions may have unique applications for this species.\n\n");

        return fallbackUses.toString();
    }

    private void displayPlantUses(String uses) {
        completeUsesText = uses;

        if (uses.length() <= MAX_USES_LENGTH) {
            // Short text - display completely
            plantUsesTextView.setText(uses);
            readMoreButton.setVisibility(View.GONE);
            isExpanded = true;
        } else {
            // Long text - display truncated with "Read More" option
            String shortText = uses.substring(0, MAX_USES_LENGTH).trim() + "...";
            plantUsesTextView.setText(shortText);

            // Add Read More button to the parent layout
            if (readMoreButton.getParent() == null) {
                // Find the parent layout of plantUsesTextView and add the button
                if (plantUsesTextView.getParent() instanceof LinearLayout) {
                    LinearLayout parent = (LinearLayout) plantUsesTextView.getParent();
                    parent.addView(readMoreButton, parent.indexOfChild(plantUsesTextView) + 1);
                }
            }

            readMoreButton.setVisibility(View.VISIBLE);
            readMoreButton.setText("Read More");
            isExpanded = false;
        }

        setupReadMoreButton();
    }

    private void setupReadMoreButton() {
        readMoreButton.setOnClickListener(v -> {
            if (isExpanded) {
                // Collapse - show truncated text
                String shortText = completeUsesText.substring(0, MAX_USES_LENGTH).trim() + "...";
                plantUsesTextView.setText(shortText);
                readMoreButton.setText("Read More");
                isExpanded = false;
            } else {
                // Expand - show complete text
                plantUsesTextView.setText(completeUsesText);
                readMoreButton.setText("Read Less");
                isExpanded = true;
            }
        });
    }

    private void displayIdentificationStatus(boolean isRealIdentification, double confidence, boolean isFromSearchRoute) {
        if (isFromSearchRoute) {
            identificationStatusText.setVisibility(View.GONE);
            return;
        }

        identificationStatusText.setVisibility(View.VISIBLE);

        if (isRealIdentification) {
            if (confidence > 0.8) {
                identificationStatusText.setText("✓ High confidence identification from AI model");
            } else if (confidence > 0.5) {
                identificationStatusText.setText("⚠ Medium confidence identification from AI model");
            } else {
                identificationStatusText.setText("⚠ Low confidence identification from AI model");
            }
        } else {
            identificationStatusText.setText("");  // or hide it
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
                viewImagesButton.setText("View " + " Images 📸");
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