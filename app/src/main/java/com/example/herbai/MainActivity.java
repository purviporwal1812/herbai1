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
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load saved preference and apply the theme mode
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isNightMode = sharedPreferences.getBoolean(NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_main);  // Only call this once

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

    private void uploadImage() {
        if (imageUri != null || imageView.getDrawable() != null) {
            // Dummy JSON data
            ArrayList<String> probablePlants = new ArrayList<>(Arrays.asList(
                    "Tulsi",
                    "Neem",
                    "Aloe Vera"
            ));
            String topPlantUses = "Aloe Vera is used for skin treatment, digestion improvement, and healing wounds.";

            // Start ResultActivity and pass data
            Intent intent = new Intent(MainActivity.this, ResultActivity.class);
            intent.putStringArrayListExtra("probablePlants", probablePlants);
            intent.putExtra("topPlantUses", topPlantUses);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Please select or capture an image first!", Toast.LENGTH_SHORT).show();
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
