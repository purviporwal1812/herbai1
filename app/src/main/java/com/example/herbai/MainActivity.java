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
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
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
import com.google.android.material.materialswitch.MaterialSwitch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MultipartBody;
import okhttp3.Response;
import okhttp3.MediaType;
import java.util.Locale;
import org.json.JSONObject;
import org.json.JSONArray;
 import java.util.concurrent.TimeUnit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private MaterialSwitch themeSwitch;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "theme_prefs";
    private static final String NIGHT_MODE = "night_mode";
    private ImageView imageView;
    private Uri imageUri;
    private Uri cameraImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private LoadingDialog loadingDialog;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Executor executor = Executors.newSingleThreadExecutor();

    // FloatingActionButton variables
    private FloatingActionButton mainFab, fab1, fab2, fab3;
    private ConstraintLayout mainLayout;
    private boolean isFabExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load saved preference and apply the theme mode
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isNightMode = sharedPreferences.getBoolean(NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_main);  // Only call this once

        // Initialize loading dialog
        loadingDialog = new LoadingDialog(this);

        themeSwitch = findViewById(R.id.themeSwitch);

        // Set the switch state according to saved preference
        themeSwitch.setChecked(isNightMode);

        // Listener for the theme toggle switch
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the night mode preference in SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(NIGHT_MODE, isChecked);
            editor.apply();

            // Apply the new theme based on the switch state
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            // Restart the activity to apply the theme change immediately
            recreate();  // This will restart the activity to apply the new theme
        });

        // Initialize image-related UI components
        imageView = findViewById(R.id.imageView);
        Button selectButton = findViewById(R.id.selectButton);
        Button uploadButton = findViewById(R.id.uploadButton);
        Button cameraButton = findViewById(R.id.cameraButton);

        // Request camera permission if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        }

        // Image Picker from Gallery
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        displayImage(imageUri);
                    }
                });

        // Capture Image using Camera
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        displayImage(cameraImageUri);
                    }
                });

        selectButton.setOnClickListener(v -> selectImage());
        cameraButton.setOnClickListener(v -> captureImage());
        uploadButton.setOnClickListener(v -> uploadImage());

        // Initialize FAB components
        initializeFAB();
    }

    private void initializeFAB() {
        // Initialize the FAB components
        mainLayout = findViewById(R.id.main_layout);
        mainFab = findViewById(R.id.main_fab);
        fab1 = findViewById(R.id.fab_1);
        fab2 = findViewById(R.id.fab_2);
        fab3 = findViewById(R.id.fab_3);

        // Set up click listeners for FABs
        mainFab.setOnClickListener(view -> {
            if (!isFabExpanded) {
                expandFab();
            } else {
                collapseFab();
            }
        });

        // Set up click listeners for child FABs
        fab1.setOnClickListener(view -> {
            Toast.makeText(MainActivity.this, "Navigating to First Page", Toast.LENGTH_SHORT).show();
            // Start FirstPage activity
            Intent intent = new Intent(MainActivity.this, FirstPageActivity.class);
            startActivity(intent);
            collapseFab();
        });

        fab2.setOnClickListener(view -> {
            Toast.makeText(MainActivity.this, "Navigating to Second Page", Toast.LENGTH_SHORT).show();
            // Start SecondPage activity
            Intent intent = new Intent(MainActivity.this, SecondPageActivity.class);
            startActivity(intent);
            collapseFab();
        });

        fab3.setOnClickListener(view -> {
            Toast.makeText(MainActivity.this, "Navigating to Third Page", Toast.LENGTH_SHORT).show();
            // Start ThirdPage activity
            Intent intent = new Intent(MainActivity.this, ThirdPageActivity.class);
            startActivity(intent);
            collapseFab();
        });

        // Add touch listener to the main layout to detect clicks outside the FABs
        mainLayout.setOnTouchListener((v, event) -> {
            if (isFabExpanded && event.getAction() == MotionEvent.ACTION_DOWN) {
                // Check if the touch is outside the main FAB
                float x = event.getX();
                float y = event.getY();
                int[] location = new int[2];
                mainFab.getLocationOnScreen(location);
                int fabX = location[0];
                int fabY = location[1];

                // If touch outside the main FAB area, collapse
                if (x < fabX || x > fabX + mainFab.getWidth() ||
                        y < fabY || y > fabY + mainFab.getHeight()) {
                    collapseFab();
                    return true;
                }
            }
            return false;
        });
    }

    private void expandFab() {
        // Set visibility and animations for child FABs
        fab1.setVisibility(View.VISIBLE);
        fab2.setVisibility(View.VISIBLE);
        fab3.setVisibility(View.VISIBLE);

        // Animate the child FABs to appear in a semicircle
        // Create animations for each child FAB
        Animation animation1 = createAnimation(fab1, -60f);  // Top-right
        Animation animation2 = createAnimation(fab2, -120f); // Top-left
        Animation animation3 = createAnimation(fab3, -90f);  // Directly above

        // Start animations
        fab1.startAnimation(animation1);
        fab2.startAnimation(animation2);
        fab3.startAnimation(animation3);

        isFabExpanded = true;
    }

    private void collapseFab() {
        // Create animations to hide FABs
        Animation hideAnimation1 = AnimationUtils.loadAnimation(this, R.anim.hide_fab);
        Animation hideAnimation2 = AnimationUtils.loadAnimation(this, R.anim.hide_fab);
        Animation hideAnimation3 = AnimationUtils.loadAnimation(this, R.anim.hide_fab);

        hideAnimation1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                fab1.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        hideAnimation2.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                fab2.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        hideAnimation3.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                fab3.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        fab1.startAnimation(hideAnimation1);
        fab2.startAnimation(hideAnimation2);
        fab3.startAnimation(hideAnimation3);

        isFabExpanded = false;
    }

    private Animation createAnimation(View view, float angle) {
        // Create custom animation for the specified angle
        AnimationSet animationSet = new AnimationSet(true);

        // Translate animation
        float endX = (float) (100 * Math.cos(Math.toRadians(angle)));
        float endY = (float) (100 * Math.sin(Math.toRadians(angle)));

        TranslateAnimation translateAnimation = new TranslateAnimation(0, endX, 0, endY);
        translateAnimation.setDuration(300);
        translateAnimation.setFillAfter(true);

        // Alpha animation
        AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1f);
        alphaAnimation.setDuration(300);
        alphaAnimation.setFillAfter(true);

        // Scale animation
        ScaleAnimation scaleAnimation = new ScaleAnimation(0f, 1f, 0f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(300);
        scaleAnimation.setFillAfter(true);

        animationSet.addAnimation(translateAnimation);
        animationSet.addAnimation(alphaAnimation);
        animationSet.addAnimation(scaleAnimation);

        return animationSet;
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void captureImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Camera permission required!", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
            return;
        }

        try {
            File imageFile = new File(getExternalFilesDir("Pictures"), "captured_image.jpg");
            if (!imageFile.exists()) {
                imageFile.createNewFile();
            }

            cameraImageUri = FileProvider.getUriForFile(this, "com.example.herbai.fileprovider", imageFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);

            if (intent.resolveActivity(getPackageManager()) != null) {
                cameraLauncher.launch(intent);
            } else {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error capturing image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void displayImage(Uri uri) {
        try {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= 29) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source);
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }
            imageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadImage1() {
        if (imageUri != null || imageView.getDrawable() != null) {
            // Show loading dialog
            loadingDialog.show();
            loadingDialog.setLoadingText("Identifying plant...");

            // Process image on background thread
            executor.execute(() -> {
                try {
                    // Open InputStream from the selected image URI
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    // Create a temporary file to upload
                    File tempFile = File.createTempFile("upload", ".jpg", getCacheDir());
                    FileOutputStream out = new FileOutputStream(tempFile);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    out.close();
                    inputStream.close();

                    // Build multipart request using OkHttp with timeout settings
                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .build();

                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", tempFile.getName(),
                                    RequestBody.create(tempFile, MediaType.parse("image/jpeg")))
                            .build();

                    Request request = new Request.Builder()
                            .url("https://serverv1-1.onrender.com/predict")
                            .post(requestBody)
                            .build();

                    // Execute the request and get the response
                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code " + response);

                    String responseBody = response.body().string();

                    // Parse the JSON response from the API
                    JSONObject jsonResponse = new JSONObject(responseBody);

                    // Parse the "top_predictions" array to get the probable plants
                    JSONArray topPredictionsJson = jsonResponse.getJSONArray("top_predictions");
                    ArrayList<String> probablePlants = new ArrayList<>();
                    for (int i = 0; i < topPredictionsJson.length(); i++) {
                        JSONObject prediction = topPredictionsJson.getJSONObject(i);
                        probablePlants.add(prediction.getString("species"));
                    }
                    // Get the top species from the main response
                    String predictedSpecies = jsonResponse.getString("species");

                    // Update UI on main thread
                    mainHandler.post(() -> {
                        loadingDialog.dismiss();
                        Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                        intent.putStringArrayListExtra("probablePlants", probablePlants);
                        intent.putExtra("predictedSpecies", predictedSpecies);
                        startActivity(intent);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    mainHandler.post(() -> {
                        loadingDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
        } else {
            Toast.makeText(this, "Please select or capture an image first!", Toast.LENGTH_SHORT).show();
        }
    }


    private void uploadImage() {
        if (imageUri != null || imageView.getDrawable() != null) {
            loadingDialog.show();
            loadingDialog.setLoadingText("Identifying plant...");

            executor.execute(() -> {
                try {
                    // Copy the picked/captured image into a temp file
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    File tempFile = File.createTempFile("upload", ".jpg", getCacheDir());
                    try (FileOutputStream out = new FileOutputStream(tempFile)) {
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = inputStream.read(buf)) != -1) {
                            out.write(buf, 0, len);
                        }
                    }
                    inputStream.close();

                    // Build OkHttp client & request
                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .build();

                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart(
                                    "file",
                                    tempFile.getName(),
                                    RequestBody.create(tempFile, MediaType.parse("image/jpeg"))
                            )
                            .build();

                    Request request = new Request.Builder()
                            .url("https://serverv1-1.onrender.com/predict")
                            .post(requestBody)
                            .build();

                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);

                    // Build a list of "SpeciesName (0.02)"
                    JSONArray preds = json.getJSONArray("top_predictions");
                    ArrayList<String> probablePlants = new ArrayList<>();
                    for (int i = 0; i < preds.length(); i++) {
                        JSONObject p = preds.getJSONObject(i);
                        String name = p.getString("species");
                        double conf = p.getDouble("confidence");
                        // two decimal places
                        String entry = String.format(Locale.getDefault(),
                                "%s (%.4f)",
                                name, conf*100);
                        probablePlants.add(entry);
                    }

                    // Back on UI thread: dismiss & launch
                    mainHandler.post(() -> {
                        loadingDialog.dismiss();
                        Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                        intent.putStringArrayListExtra("probablePlants", probablePlants);
                        startActivity(intent);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    mainHandler.post(() -> {
                        loadingDialog.dismiss();
                        Toast.makeText(MainActivity.this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }
            });
        } else {
            Toast.makeText(this,
                    "Please select or capture an image first!",
                    Toast.LENGTH_SHORT).show();
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