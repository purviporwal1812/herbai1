package com.example.herbai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private SwitchMaterial themeSwitch;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "theme_prefs";
    private static final String NIGHT_MODE = "night_mode";
    private ImageView imageView;
    private Uri imageUri;
    private Uri cameraImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private LoadingDialog loadingDialog;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Executor executor = Executors.newSingleThreadExecutor();

    // Menu components
    private FloatingActionButton menuFab;
    private LinearLayout menuLayout;
    private boolean isMenuVisible = false;

    // Network client
    private OkHttpClient client;

    // Replace with your actual server URL
    private static final String BASE_URL = "https://serverv1-1.onrender.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load saved preference and apply the theme mode
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isNightMode = sharedPreferences.getBoolean(NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_main);

        // Initialize network client
        client = new OkHttpClient();

        // Initialize loading dialog
        loadingDialog = new LoadingDialog(this);

        initializeViews();
        setupPermissionLauncher();
        setupImageLaunchers();
        setupThemeSwitch();
        setupButtons();
        setupMenu();
    }

    private void initializeViews() {
        themeSwitch = findViewById(R.id.themeSwitch);
        imageView = findViewById(R.id.imageView);
        menuFab = findViewById(R.id.menu_fab);
        menuLayout = findViewById(R.id.menu_layout);
    }

    private void setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(this, "Camera permission granted!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Camera permission denied!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupImageLaunchers() {
        // Image Picker from Gallery
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Gallery result: " + result.getResultCode());
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        if (imageUri != null) {
                            displayImage(imageUri);
                            Log.d(TAG, "Image selected from gallery: " + imageUri.toString());
                        }
                    }
                });

        // Capture Image using Camera
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Camera result: " + result.getResultCode());
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (cameraImageUri != null) {
                            imageUri = cameraImageUri; // Set imageUri to the captured image
                            displayImage(cameraImageUri);
                            Log.d(TAG, "Image captured: " + cameraImageUri.toString());
                        }
                    }
                });
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
        Button selectButton = findViewById(R.id.selectButton);
        Button uploadButton = findViewById(R.id.uploadButton);
        Button cameraButton = findViewById(R.id.cameraButton);

        selectButton.setOnClickListener(v -> selectImage());
        cameraButton.setOnClickListener(v -> captureImage());
        uploadButton.setOnClickListener(v -> uploadImage());
    }

    private void setupMenu() {
        menuFab.setOnClickListener(v -> toggleMenu());

        // Menu item buttons
        Button plantSearchBtn = findViewById(R.id.menu_plant_search);
        Button plantSearchBtnK = findViewById(R.id.menu_search_by_disease);

        Button systemStatusBtn = findViewById(R.id.menu_system_status);
        Button generateDataBtn = findViewById(R.id.menu_generate_data);
        Button settingsBtn = findViewById(R.id.menu_settings);


        plantSearchBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, PlantSearchActivity.class));
            hideMenu();
        });
        plantSearchBtnK.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, PlantSearchActivityK.class));
            hideMenu();
        });
        systemStatusBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ApiStatusActivity.class));
            hideMenu();
        });

        generateDataBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, GenerateDataActivity.class));
            hideMenu();
        });

        settingsBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            hideMenu();
        });

        // Hide menu when clicking outside
        ConstraintLayout mainLayout = findViewById(R.id.main_layout);
        mainLayout.setOnClickListener(v -> {
            if (isMenuVisible) {
                hideMenu();
            }
        });
    }

    private void toggleMenu() {
        if (isMenuVisible) {
            hideMenu();
        } else {
            showMenu();
        }
    }

    private void showMenu() {
        menuLayout.setVisibility(View.VISIBLE);
        menuLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .start();

        menuFab.animate()
                .rotation(45f)
                .setDuration(200)
                .start();

        isMenuVisible = true;
    }

    private void hideMenu() {
        menuLayout.animate()
                .alpha(0f)
                .translationY(50f)
                .setDuration(200)
                .withEndAction(() -> menuLayout.setVisibility(View.GONE))
                .start();

        menuFab.animate()
                .rotation(0f)
                .setDuration(200)
                .start();

        isMenuVisible = false;
    }

    private void selectImage() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
            Log.d(TAG, "Gallery intent launched");
        } catch (Exception e) {
            Log.e(TAG, "Error launching gallery: " + e.getMessage());
            Toast.makeText(this, "Error opening gallery: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void captureImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }

        try {
            // Create the image file
            File imageFile = new File(getExternalFilesDir("Pictures"), "captured_image_" + System.currentTimeMillis() + ".jpg");

            // Create directories if they don't exist
            File parentDir = imageFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Create the file
            if (!imageFile.exists()) {
                imageFile.createNewFile();
            }

            cameraImageUri = FileProvider.getUriForFile(this, "com.example.herbai.fileprovider", imageFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);

            // Grant URI permission
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                cameraLauncher.launch(intent);
                Log.d(TAG, "Camera intent launched, URI: " + cameraImageUri.toString());
            } else {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error capturing image: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error capturing image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void displayImage(Uri uri) {
        try {
            if (uri == null) {
                Log.e(TAG, "URI is null");
                return;
            }

            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= 29) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source);
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }

            imageView.setImageBitmap(bitmap);
            Log.d(TAG, "Image displayed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error displaying image: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error displaying image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImage() {
        if (imageUri == null && imageView.getDrawable() == null) {
            Toast.makeText(this, "Please select or capture an image first!", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingDialog.show();
        loadingDialog.setLoadingText("Identifying plant...");

        executor.execute(() -> {
            try {
                File imageFile = createTempFileFromUri(imageUri);

                if (imageFile == null || !imageFile.exists()) {
                    mainHandler.post(() -> {
                        loadingDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Error preparing image file", Toast.LENGTH_SHORT).show();
                        showDummyResults();
                    });
                    return;
                }

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", imageFile.getName(),
                                RequestBody.create(imageFile, MediaType.parse("image/*")))
                        .build();

                Request request = new Request.Builder()
                        .url(BASE_URL + "predict")
                        .post(requestBody)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Upload failed: " + e.getMessage());
                        mainHandler.post(() -> {
                            loadingDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            showDummyResults();
                        });
                    }


                    // Replace the onResponse method in your MainActivity with this fixed version:
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Server response: " + responseBody);

                        mainHandler.post(() -> {
                            loadingDialog.dismiss();

                            try {
                                JSONObject jsonResponse = new JSONObject(responseBody);

                                if (jsonResponse.has("error")) {
                                    String errorMessage = jsonResponse.getString("error");
                                    Log.e(TAG, "Prediction error: " + errorMessage);
                                    Toast.makeText(MainActivity.this, "Prediction error: " + errorMessage, Toast.LENGTH_SHORT).show();
                                    showDummyResults();
                                    return;
                                }

                                // Check if this is a successful plant identification response
                                if (jsonResponse.has("species") && jsonResponse.has("confidence")) {
                                    handlePlantIdentificationResponse(jsonResponse);
                                } else if (jsonResponse.has("results") && jsonResponse.has("success")) {
                                    // **FIXED: Handle knowledge graph results properly**
                                    handleKnowledgeGraphResponse(jsonResponse);
                                } else {
                                    Log.w(TAG, "Unexpected response format: " + responseBody);
                                    showDummyResults();
                                }

                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing response: " + e.getMessage());
                                Toast.makeText(MainActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                                showDummyResults();
                            }
                        });
                    }

                    /**
                     * NEW METHOD: Handle plant identification from ML model
                     */
                    private void handlePlantIdentificationResponse(JSONObject jsonResponse) {
                        try {
                            String topSpecies = jsonResponse.getString("species");
                            double confidence = jsonResponse.getDouble("confidence");

                            Log.d(TAG, "Identified species: " + topSpecies + " with confidence: " + confidence);

                            // Get additional plant information if available
                            String family = jsonResponse.optString("family", "Unknown family");
                            String scientificName = jsonResponse.optString("scientific_name", topSpecies);
                            String commonNames = jsonResponse.optString("common_names", topSpecies);
                            String uses = jsonResponse.optString("medicinal_properties", "Information not available");
                            String habitat = jsonResponse.optString("habitat", "Various environments");

                            // Extract database information from JSON response
                            String databaseUses = jsonResponse.optString("uses", "");
                            String databaseMedicinalProperties = jsonResponse.optString("medicinal_properties", "");
                            String databaseChemicalComponents = jsonResponse.optString("chemical_components", "");

                            // Extract image URLs from MongoDB response
                            ArrayList<String> dbImageUrls = new ArrayList<>();
                            if (jsonResponse.has("db_image_urls")) {
                                JSONArray imageUrlArray = jsonResponse.getJSONArray("db_image_urls");
                                for (int i = 0; i < imageUrlArray.length(); i++) {
                                    dbImageUrls.add(imageUrlArray.getString(i));
                                }
                                Log.d(TAG, "Found " + dbImageUrls.size() + " database images for " + topSpecies);
                            }

                            // Extract images from db_matches
                            if (jsonResponse.has("db_matches")) {
                                JSONArray dbMatches = jsonResponse.getJSONArray("db_matches");
                                for (int i = 0; i < dbMatches.length(); i++) {
                                    JSONObject match = dbMatches.getJSONObject(i);
                                    if (match.has("image_urls")) {
                                        JSONArray matchImages = match.getJSONArray("image_urls");
                                        for (int j = 0; j < matchImages.length(); j++) {
                                            String imageUrl = matchImages.getString(j);
                                            if (!dbImageUrls.contains(imageUrl)) {
                                                dbImageUrls.add(imageUrl);
                                            }
                                        }
                                    }
                                }
                                Log.d(TAG, "Total images after db_matches: " + dbImageUrls.size());
                            }

                            // Get top predictions for probable plants
                            ArrayList<String> probablePlants = new ArrayList<>();
                            JSONArray topPredictions = jsonResponse.optJSONArray("top_predictions");

                            if (topPredictions != null && topPredictions.length() > 0) {
                                for (int i = 0; i < Math.min(5, topPredictions.length()); i++) {
                                    JSONObject prediction = topPredictions.getJSONObject(i);
                                    String species = prediction.getString("species");
                                    double predConfidence = prediction.getDouble("confidence");
                                    probablePlants.add(species + " (" + String.format("%.1f", predConfidence * 100) + "%)");
                                }
                            } else {
                                probablePlants.add(topSpecies + " (" + String.format("%.1f", confidence * 100) + "%)");
                            }

                            // Start ResultActivity with comprehensive plant data
                            Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                            intent.putStringArrayListExtra("probablePlants", probablePlants);
                            intent.putStringArrayListExtra("dbImageUrls", dbImageUrls);
                            intent.putExtra("topPlantUses", uses);
                            intent.putExtra("confidence", confidence);
                            intent.putExtra("plantName", commonNames);
                            intent.putExtra("scientificName", scientificName);
                            intent.putExtra("family", family);
                            intent.putExtra("habitat", habitat);
                            intent.putExtra("isRealIdentification", true);
                            intent.putExtra("isFromSearchRoute", true);
                            intent.putExtra("hasDbImages", !dbImageUrls.isEmpty());
                            intent.putExtra("databaseUses", databaseUses);
                            intent.putExtra("databaseMedicinalProperties", databaseMedicinalProperties);
                            intent.putExtra("databaseChemicalComponents", databaseChemicalComponents);

                            startActivity(intent);

                        } catch (Exception e) {
                            Log.e(TAG, "Error handling plant identification response: " + e.getMessage());
                            showDummyResults();
                        }
                    }

                    /**
                     * NEW METHOD: Handle knowledge graph results with priority to knowledge graph data
                     */
                    private void handleKnowledgeGraphResponse(JSONObject jsonResponse) {
                        try {
                            JSONArray results = jsonResponse.getJSONArray("results");
                            if (results.length() == 0) {
                                Log.w(TAG, "No results in knowledge graph response");
                                showDummyResults();
                                return;
                            }

                            Log.d(TAG, "Processing knowledge graph response with " + results.length() + " results");

                            // **PRIORITY 1: Find knowledge graph entries (auto_generated = null or false)**
                            JSONObject knowledgeGraphResult = findKnowledgeGraphEntry(results);

                            if (knowledgeGraphResult != null) {
                                Log.d(TAG, "Found knowledge graph entry - using it directly");
                                processKnowledgeGraphResult(knowledgeGraphResult, results);
                            } else {
                                // **PRIORITY 2: Find any result with meaningful uses**
                                JSONObject bestResult = findResultWithMeaningfulUses(results);
                                if (bestResult != null) {
                                    Log.d(TAG, "Found result with meaningful uses");
                                    processKnowledgeGraphResult(bestResult, results);
                                } else {
                                    Log.w(TAG, "No meaningful results found - using fallback");
                                    showDummyResults();
                                }
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Error handling knowledge graph response: " + e.getMessage());
                            showDummyResults();
                        }
                    }

                    /**
                     * NEW METHOD: Find authentic knowledge graph entries (not auto-generated)
                     */
                    private JSONObject findKnowledgeGraphEntry(JSONArray results) {
                        try {
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject result = results.getJSONObject(i);

                                // Check if this is a knowledge graph entry (not auto-generated)
                                boolean autoGenerated = result.optBoolean("auto_generated", false);
                                String dataSource = result.optString("data_source", "");

                                // Prioritize non-auto-generated entries from knowledge graph
                                if (!autoGenerated || dataSource.isEmpty()) {
                                    String uses = result.optString("uses", "").trim();

                                    // Must have meaningful uses to be considered
                                    if (!uses.isEmpty() && !isGenericUses(uses)) {
                                        Log.d(TAG, "Found knowledge graph entry: " + result.optString("plant_name", "Unknown"));
                                        Log.d(TAG, "Uses preview: " + uses.substring(0, Math.min(uses.length(), 100)));
                                        return result;
                                    }
                                }
                            }

                            Log.d(TAG, "No authentic knowledge graph entries found");
                            return null;

                        } catch (Exception e) {
                            Log.e(TAG, "Error finding knowledge graph entry: " + e.getMessage());
                            return null;
                        }
                    }

                    /**
                     * NEW METHOD: Find any result with meaningful uses (fallback)
                     */
                    private JSONObject findResultWithMeaningfulUses(JSONArray results) {
                        try {
                            JSONObject bestResult = null;
                            int bestUsesLength = 0;

                            for (int i = 0; i < results.length(); i++) {
                                JSONObject result = results.getJSONObject(i);
                                String uses = result.optString("uses", "").trim();

                                if (!uses.isEmpty() && !isGenericUses(uses) && uses.length() > bestUsesLength) {
                                    bestResult = result;
                                    bestUsesLength = uses.length();
                                    Log.d(TAG, "Better uses found (" + uses.length() + " chars): " +
                                            result.optString("plant_name", "Unknown"));
                                }
                            }

                            return bestResult;

                        } catch (Exception e) {
                            Log.e(TAG, "Error finding result with meaningful uses: " + e.getMessage());
                            return null;
                        }
                    }

                    /**
                     * NEW METHOD: Check if uses text is generic/meaningless
                     */
                    private boolean isGenericUses(String uses) {
                        if (uses == null || uses.length() < 15) {
                            return true;
                        }

                        String lowerUses = uses.toLowerCase();

                        // Filter out generic phrases
                        String[] genericPhrases = {
                                "traditional and ornamental uses",
                                "uses to be researched",
                                "information not available",
                                "under research",
                                "not available",
                                "a rose is either",
                                "they form"
                        };

                        for (String phrase : genericPhrases) {
                            if (lowerUses.contains(phrase)) {
                                return true;
                            }
                        }

                        return false;
                    }

                    /**
                     * NEW METHOD: Process the selected knowledge graph result
                     */
                    private void processKnowledgeGraphResult(JSONObject selectedResult, JSONArray allResults) {
                        try {
                            // Extract information from the selected result
                            String plantName = selectedResult.optString("plant_name", "Unknown Plant");
                            String scientificName = selectedResult.optString("scientific_name", "Unknown");
                            String family = selectedResult.optString("family", "Unknown family");
                            String habitat = selectedResult.optString("habitat", "Various environments");
                            String databaseUses = selectedResult.optString("uses", "");
                            String databaseMedicinalProperties = selectedResult.optString("medicinal_properties", "");
                            String databaseChemicalComponents = selectedResult.optString("chemical_components", "");
                            boolean autoGenerated = selectedResult.optBoolean("auto_generated", false);

                            Log.d(TAG, "Processing selected result:");
                            Log.d(TAG, "Plant: " + plantName);
                            Log.d(TAG, "Uses length: " + databaseUses.length());
                            Log.d(TAG, "Auto-generated: " + autoGenerated);

                            // Extract image URLs from the selected result
                            ArrayList<String> dbImageUrls = new ArrayList<>();
                            if (selectedResult.has("image_urls")) {
                                JSONArray imageUrlArray = selectedResult.getJSONArray("image_urls");
                                for (int i = 0; i < imageUrlArray.length(); i++) {
                                    dbImageUrls.add(imageUrlArray.getString(i));
                                }
                            }

                            // Build probable plants list from all results
                            ArrayList<String> probablePlants = new ArrayList<>();
                            for (int i = 0; i < Math.min(5, allResults.length()); i++) {
                                JSONObject result = allResults.getJSONObject(i);
                                String name = result.optString("plant_name", "Unknown");
                                boolean isAutoGen = result.optBoolean("auto_generated", false);

                                if (i == 0 || !isAutoGen) {
                                    probablePlants.add(name + (isAutoGen ? " (API)" : " (Database)"));
                                }
                            }

                            // **KEY FIX: Pass the database information directly to ResultActivity**
                            Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                            intent.putStringArrayListExtra("probablePlants", probablePlants);
                            intent.putStringArrayListExtra("dbImageUrls", dbImageUrls);
                            intent.putExtra("confidence", autoGenerated ? 0.75 : 0.85); // Lower confidence for API data
                            intent.putExtra("plantName", plantName);
                            intent.putExtra("scientificName", scientificName);
                            intent.putExtra("family", family);
                            intent.putExtra("habitat", habitat);
                            intent.putExtra("isRealIdentification", true);
                            intent.putExtra("hasDbImages", !dbImageUrls.isEmpty());

                            // **CRITICAL: Pass the database uses directly - don't let ResultActivity fetch again**
                            intent.putExtra("databaseUses", databaseUses);
                            intent.putExtra("databaseMedicinalProperties", databaseMedicinalProperties);
                            intent.putExtra("databaseChemicalComponents", databaseChemicalComponents);
                            intent.putExtra("topPlantUses", databaseUses); // This ensures it's displayed immediately

                            // **NEW: Add flag to prevent API fetching**
                            intent.putExtra("hasKnowledgeGraphData", true);
                            intent.putExtra("dataSource", autoGenerated ? "API" : "Knowledge Graph");

                            Log.d(TAG, "Starting ResultActivity with knowledge graph data");
                            startActivity(intent);

                        } catch (Exception e) {
                            Log.e(TAG, "Error processing knowledge graph result: " + e.getMessage());
                            showDummyResults();
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error preparing image: " + e.getMessage());
                mainHandler.post(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Error preparing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showDummyResults();
                });
            }
        });
    }

    private File createTempFileFromUri(Uri uri) {
        try {
            if (uri == null) return null;

            File tempFile = new File(getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");

            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {

                if (inputStream == null) return null;

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                Log.d(TAG, "Temp file created: " + tempFile.getAbsolutePath());
                return tempFile;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating temp file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void showDummyResults() {
        ArrayList<String> probablePlants = new ArrayList<>(Arrays.asList(
                "Tulsi (Holy Basil)", "Neem", "Aloe Vera"
        ));
        String topPlantUses = "Plant identification using local processing. For accurate results, ensure server connection is available.";

        Intent intent = new Intent(MainActivity.this, ResultActivity.class);
        intent.putStringArrayListExtra("probablePlants", probablePlants);
        intent.putExtra("topPlantUses", topPlantUses);
        intent.putExtra("confidence", 0.0);
        intent.putExtra("plantName", "Sample Plant");
        intent.putExtra("scientificName", "Plantus sampleus");
        intent.putExtra("family", "Sample Family");
        intent.putExtra("habitat", "Various environments");
        intent.putExtra("isRealIdentification", false);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (isMenuVisible) {
            hideMenu();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}