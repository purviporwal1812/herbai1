package com.example.herbai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import java.io.FileNotFoundException;
import android.os.Environment;
import android.net.Uri;
import android.os.Build;
import java.util.concurrent.TimeUnit;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
    private File cameraImageFile;
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
        // Initialize network client with extended timeouts for ML prediction
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)      // Time to establish connection
                .readTimeout(60, TimeUnit.SECONDS)         // Time to read response (important for ML predictions)
                .writeTimeout(30, TimeUnit.SECONDS)        // Time to upload image
                .retryOnConnectionFailure(true)            // Retry on connection failures
                .build();


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

// In setupImageLaunchers() method
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Camera result code: " + result.getResultCode());
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (cameraImageUri != null && cameraImageFile != null) {
                            // Wait a moment for the file to be fully written
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                // Verify the file exists and has content
                                if (cameraImageFile.exists() && cameraImageFile.length() > 0) {
                                    imageUri = cameraImageUri;
                                    displayImage(cameraImageUri);
                                    Log.d(TAG, "Image captured successfully: " + cameraImageUri.toString());
                                    Log.d(TAG, "Camera file size: " + cameraImageFile.length() + " bytes");
                                } else {
                                    Log.e(TAG, "Camera image file is empty or doesn't exist");
                                    Log.e(TAG, "File exists: " + cameraImageFile.exists());
                                    Log.e(TAG, "File size: " + cameraImageFile.length());
                                    Toast.makeText(this, "Error: Camera image not saved properly", Toast.LENGTH_SHORT).show();
                                }
                            }, 500); // Wait 500ms for file to be written
                        } else {
                            Log.e(TAG, "cameraImageUri or cameraImageFile is null");
                            Toast.makeText(this, "Error: Image URI is null", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "Camera cancelled or failed, result code: " + result.getResultCode());
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
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "_";
                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

                File imageFile = File.createTempFile(
                        imageFileName,
                        ".jpg",
                        storageDir
                );

                // Store the file reference
                cameraImageFile = imageFile;  // ← ADD THIS LINE

                cameraImageUri = FileProvider.getUriForFile(
                        this,
                        "com.example.herbai.fileprovider",
                        imageFile
                );

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                cameraLauncher.launch(intent);
                Log.d(TAG, "Camera intent launched, URI: " + cameraImageUri.toString());
                Log.d(TAG, "Camera file path: " + imageFile.getAbsolutePath());  // ← ADD THIS LINE

            } catch (IOException e) {
                Log.e(TAG, "Error creating image file: " + e.getMessage());
                e.printStackTrace();
                Toast.makeText(this, "Error creating image file: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                File imageFile;

                // Check if this is a camera image
                if (cameraImageFile != null && cameraImageFile.exists() &&
                        imageUri != null && imageUri.equals(cameraImageUri)) {

                    // VALIDATE CAMERA FILE
                    if (!cameraImageFile.exists()) {
                        Log.e(TAG, "Camera file does not exist: " + cameraImageFile.getAbsolutePath());
                        mainHandler.post(() -> {
                            loadingDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Error: Camera image file not found", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    if (cameraImageFile.length() == 0) {
                        Log.e(TAG, "Camera file is empty: " + cameraImageFile.getAbsolutePath());
                        mainHandler.post(() -> {
                            loadingDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Error: Camera image is empty", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    if (!cameraImageFile.canRead()) {
                        Log.e(TAG, "Cannot read camera file: " + cameraImageFile.getAbsolutePath());
                        mainHandler.post(() -> {
                            loadingDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Error: Cannot read camera image", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    imageFile = cameraImageFile;
                    Log.d(TAG, "Using original camera file: " + imageFile.getAbsolutePath());
                    Log.d(TAG, "Camera file size: " + imageFile.length() + " bytes");
                    Log.d(TAG, "Camera file readable: " + imageFile.canRead());
                } else {
                    // For gallery images, create temp file as before
                    imageFile = createTempFileFromUri(imageUri);
                }

                if (imageFile == null || !imageFile.exists()) {
                    Log.e(TAG, "Final image file is null or doesn't exist");
                    mainHandler.post(() -> {
                        loadingDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Error preparing image file", Toast.LENGTH_SHORT).show();
                        showDummyResults();
                    });
                    return;
                }

                if (imageFile.length() == 0) {
                    Log.e(TAG, "Final image file is empty");
                    mainHandler.post(() -> {
                        loadingDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Error: Image file is empty", Toast.LENGTH_SHORT).show();
                        showDummyResults();
                    });
                    return;
                }

                Log.d(TAG, "=== UPLOADING IMAGE ===");
                Log.d(TAG, "File path: " + imageFile.getAbsolutePath());
                Log.d(TAG, "File size: " + imageFile.length() + " bytes");
                Log.d(TAG, "File readable: " + imageFile.canRead());
                Log.d(TAG, "File name: " + imageFile.getName());

                // Step 1: Call /predict endpoint for plant identification
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", imageFile.getName(),
                                RequestBody.create(imageFile, MediaType.parse("image/*")))
                        .build();

                Request request = new Request.Builder()
                        .url(BASE_URL + "predict")
                        .post(requestBody)
                        .build();

                Log.d(TAG, "Sending request to: " + BASE_URL + "predict");

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Prediction failed: " + e.getMessage());
                        Log.e(TAG, "Exception class: " + e.getClass().getName());
                        e.printStackTrace();
                        mainHandler.post(() -> {
                            loadingDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Prediction failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            showDummyResults();
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseBody = response.body().string();
                        Log.d(TAG, "=== PREDICTION RESPONSE ===");
                        Log.d(TAG, "Response code: " + response.code());
                        Log.d(TAG, responseBody);

                        // ... rest of your response handling code

                        Log.d(TAG, "=== PREDICTION RESPONSE ===");
                        Log.d(TAG, responseBody);

                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);

                            // Check for error
                            if (jsonResponse.has("error")) {
                                String errorMessage = jsonResponse.getString("error");
                                Log.e(TAG, "Prediction error: " + errorMessage);
                                mainHandler.post(() -> {
                                    loadingDialog.dismiss();
                                    Toast.makeText(MainActivity.this, "Prediction error: " + errorMessage, Toast.LENGTH_SHORT).show();
                                    showDummyResults();
                                });
                                return;
                            }

                            // Extract plant name from prediction response
                            String identifiedPlantName = "";
                            double confidence = 0.0;
                            ArrayList<String> topPredictions = new ArrayList<>();

                            if (jsonResponse.has("species")) {
                                identifiedPlantName = jsonResponse.getString("species");
                                confidence = jsonResponse.optDouble("confidence", 0.0);

                                // Extract top predictions for backup
                                if (jsonResponse.has("top_predictions")) {
                                    JSONArray predictions = jsonResponse.getJSONArray("top_predictions");
                                    for (int i = 0; i < Math.min(3, predictions.length()); i++) {
                                        JSONObject pred = predictions.getJSONObject(i);
                                        String species = pred.optString("species", "");
                                        double predConf = pred.optDouble("confidence", 0.0);
                                        topPredictions.add(species + " (" + String.format("%.1f", predConf * 100) + "%)");
                                    }
                                }
                            } else {
                                Log.e(TAG, "No species found in prediction response");
                                mainHandler.post(() -> {
                                    loadingDialog.dismiss();
                                    Toast.makeText(MainActivity.this, "No plant identified", Toast.LENGTH_SHORT).show();
                                    showDummyResults();
                                });
                                return;
                            }

                            Log.d(TAG, "Identified plant: " + identifiedPlantName + " (confidence: " + confidence + ")");

                            // Step 2: Call /smart_search for detailed information
                            mainHandler.post(() -> loadingDialog.setLoadingText("Fetching plant details..."));

                            fetchDetailedPlantInfo(identifiedPlantName, confidence, topPredictions);

                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing prediction response: " + e.getMessage());
                            mainHandler.post(() -> {
                                loadingDialog.dismiss();
                                Toast.makeText(MainActivity.this, "Error parsing prediction response", Toast.LENGTH_SHORT).show();
                                showDummyResults();
                            });
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






    // Replace the fetchDetailedPlantInfo method in MainActivity with this fixed version
    private void fetchDetailedPlantInfo(String plantName, double confidence, ArrayList<String> fallbackPredictions) {
        try {
            // Use %20 encoding instead of + encoding for spaces
            String encodedPlantName = plantName.replace(" ", "%20");
            String smartSearchUrl = BASE_URL + "smart_search/" + encodedPlantName;

            Log.d(TAG, "Original plant name: " + plantName);
            Log.d(TAG, "Encoded plant name: " + encodedPlantName);
            Log.d(TAG, "Fetching details from: " + smartSearchUrl);

            Request request = new Request.Builder()
                    .url(smartSearchUrl)
                    .get()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Smart search failed: " + e.getMessage());
                    mainHandler.post(() -> {
                        loadingDialog.dismiss();
                        // Show results with limited info from prediction
                        showLimitedResults(plantName, confidence, fallbackPredictions);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "=== SMART SEARCH RESPONSE ===");
                    Log.d(TAG, "Response code: " + response.code());
                    Log.d(TAG, "Response body: " + responseBody);

                    mainHandler.post(() -> {
                        loadingDialog.dismiss();
                        processSmartSearchResponse(responseBody, plantName, confidence, fallbackPredictions);
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in smart search request: " + e.getMessage());
            mainHandler.post(() -> {
                loadingDialog.dismiss();
                showLimitedResults(plantName, confidence, fallbackPredictions);
            });
        }
    }
    // Process the smart_search response and launch ResultActivity
    private void processSmartSearchResponse(String responseBody, String fallbackPlantName, double confidence, ArrayList<String> fallbackPredictions) {
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);

            // Check for success
            if (!jsonResponse.has("success") || !jsonResponse.getBoolean("success")) {
                Log.w(TAG, "Smart search was not successful");
                showLimitedResults(fallbackPlantName, confidence, fallbackPredictions);
                return;
            }

            // Extract results array
            if (!jsonResponse.has("results")) {
                Log.w(TAG, "No results in smart search response");
                showLimitedResults(fallbackPlantName, confidence, fallbackPredictions);
                return;
            }

            JSONArray results = jsonResponse.getJSONArray("results");
            if (results.length() == 0) {
                Log.w(TAG, "Empty results array");
                showLimitedResults(fallbackPlantName, confidence, fallbackPredictions);
                return;
            }

            // Extract data from the first (and should be only) result
            JSONObject plantData = results.getJSONObject(0);

            Log.d(TAG, "=== EXTRACTING PLANT DATA ===");

            // Extract all available fields with fallbacks
            String plantName = extractPlantField(plantData, "plant_name", fallbackPlantName);
            String scientificName = extractPlantField(plantData, "scientific_name", plantName);
            String family = extractPlantField(plantData, "family", "");
            String habitat = extractPlantField(plantData, "habitat", "");
            String uses = extractPlantField(plantData, "uses", "");
            String medicinalProperties = extractPlantField(plantData, "medicinal_properties", "");
            String chemicalComponents = extractPlantField(plantData, "chemical_components", "");

            // Create probable plants list (include original predictions as fallback)
            ArrayList<String> probablePlants = new ArrayList<>();
            probablePlants.add(plantName);

            // Add fallback predictions if we have them
            if (fallbackPredictions != null && !fallbackPredictions.isEmpty()) {
                for (String pred : fallbackPredictions) {
                    if (!probablePlants.contains(pred)) {
                        probablePlants.add(pred);
                    }
                }
            }

            // Log extracted values
            Log.d(TAG, "Final plant name: '" + plantName + "'");
            Log.d(TAG, "Final scientific name: '" + scientificName + "'");
            Log.d(TAG, "Final family: '" + family + "'");
            Log.d(TAG, "Final habitat: '" + habitat + "'");
            Log.d(TAG, "Final uses: '" + uses + "'");

            // Create and start ResultActivity
            Intent intent = new Intent(MainActivity.this, ResultActivity.class);
            intent.putExtra("plantName", plantName);
            intent.putExtra("scientificName", scientificName);
            intent.putExtra("family", family);
            intent.putExtra("habitat", habitat);
            intent.putExtra("uses", uses);
            intent.putExtra("confidence", confidence);
            intent.putExtra("isRealIdentification", true);
            intent.putExtra("isFromSearchRoute", false);
            intent.putExtra("hasDbImages", false);

            // Database information
            intent.putExtra("databaseUses", uses);
            intent.putExtra("databaseMedicinalProperties", medicinalProperties);
            intent.putExtra("databaseChemicalComponents", chemicalComponents);

            intent.putStringArrayListExtra("dbImageUrls", new ArrayList<String>());
            intent.putStringArrayListExtra("probablePlants", probablePlants);

            startActivity(intent);

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing smart search response: " + e.getMessage());
            e.printStackTrace();
            showLimitedResults(fallbackPlantName, confidence, fallbackPredictions);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error processing smart search: " + e.getMessage());
            e.printStackTrace();
            showLimitedResults(fallbackPlantName, confidence, fallbackPredictions);
        }
    }

    // Helper method to extract plant field with validation
    private String extractPlantField(JSONObject plantData, String fieldName, String fallback) {
        String value = plantData.optString(fieldName, "").trim();

        // Check if value is meaningful
        if (value.isEmpty() ||
                value.equals("null") ||
                value.equals("NULL") ||
                value.equals("None") ||
                value.equals("Unknown")) {

            Log.d(TAG, "Field '" + fieldName + "' is empty/null, using fallback: '" + fallback + "'");
            return fallback;
        }

        Log.d(TAG, "Field '" + fieldName + "': '" + value + "'");
        return value;
    }

    // Show results with limited information when smart search fails
    private void showLimitedResults(String plantName, double confidence, ArrayList<String> probablePlants) {
        Log.d(TAG, "Showing limited results for: " + plantName);

        Intent intent = new Intent(MainActivity.this, ResultActivity.class);
        intent.putExtra("plantName", plantName);
        intent.putExtra("scientificName", plantName);
        intent.putExtra("family", "");
        intent.putExtra("habitat", "");
        intent.putExtra("uses", "Detailed information is being fetched. Please use 'Search More' for additional details.");
        intent.putExtra("confidence", confidence);
        intent.putExtra("isRealIdentification", true);
        intent.putExtra("isFromSearchRoute", false);
        intent.putExtra("hasDbImages", false);

        // Empty database information
        intent.putExtra("databaseUses", "");
        intent.putExtra("databaseMedicinalProperties", "");
        intent.putExtra("databaseChemicalComponents", "");

        intent.putStringArrayListExtra("dbImageUrls", new ArrayList<String>());
        intent.putStringArrayListExtra("probablePlants", probablePlants != null ? probablePlants : new ArrayList<String>());

        startActivity(intent);
    }



    private File createTempFileFromUri(Uri uri) {
        try {
            if (uri == null) {
                Log.e(TAG, "URI is null");
                return null;
            }

            Log.d(TAG, "Creating temp file from URI: " + uri.toString());
            Log.d(TAG, "URI scheme: " + uri.getScheme());

            // CHECK IF THIS IS CAMERA IMAGE - RETURN ORIGINAL FILE
            if (cameraImageFile != null && cameraImageFile.exists() &&
                    uri.equals(cameraImageUri)) {
                Log.d(TAG, "Using original camera file: " + cameraImageFile.getAbsolutePath());
                Log.d(TAG, "Camera file size: " + cameraImageFile.length());
                Log.d(TAG, "Camera file can read: " + cameraImageFile.canRead());

                // IMPORTANT: Return the camera file directly, don't copy it!
                return cameraImageFile;
            }

            // For gallery images or other content URIs, create a temp file
            File tempFile = new File(getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");

            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {

                if (inputStream == null) {
                    Log.e(TAG, "InputStream is null for URI: " + uri);
                    return null;
                }

                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }

                Log.d(TAG, "Temp file created: " + tempFile.getAbsolutePath());
                Log.d(TAG, "Total bytes copied: " + totalBytesRead);

                return tempFile;
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found for URI: " + uri);
            e.printStackTrace();
            return null;
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