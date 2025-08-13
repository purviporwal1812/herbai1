package com.example.herbai;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {
    private MaterialSwitch themeSwitch;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "theme_prefs";
    private static final String NIGHT_MODE = "night_mode";
    private static final String SERVER_URL = "server_url";

    private EditText serverUrlEditText;
    private Button saveButton, testConnectionButton;
    private TextView connectionStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load saved theme preference
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isNightMode = sharedPreferences.getBoolean(NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(isNightMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_settings);

        initializeViews();
        setupThemeSwitch();
        loadSettings();
        setupClickListeners();
    }

    private void initializeViews() {
        themeSwitch = findViewById(R.id.themeSwitch);
        serverUrlEditText = findViewById(R.id.serverUrlEditText);
        saveButton = findViewById(R.id.saveButton);
        testConnectionButton = findViewById(R.id.testConnectionButton);
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView);
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

    private void loadSettings() {
        String savedUrl = sharedPreferences.getString(SERVER_URL, "http://your-server-url:10000");
        serverUrlEditText.setText(savedUrl);
    }

    private void setupClickListeners() {
        saveButton.setOnClickListener(v -> saveSettings());
        testConnectionButton.setOnClickListener(v -> testConnection());
    }

    private void saveSettings() {
        String serverUrl = serverUrlEditText.getText().toString().trim();

        if (serverUrl.isEmpty()) {
            Toast.makeText(this, "Please enter a server URL", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate URL format
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            serverUrl = "http://" + serverUrl;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SERVER_URL, serverUrl);
        editor.apply();

        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
        connectionStatusTextView.setText("Settings saved. Test connection to verify.");
    }

    private void testConnection() {
        String serverUrl = serverUrlEditText.getText().toString().trim();

        if (serverUrl.isEmpty()) {
            Toast.makeText(this, "Please enter a server URL first", Toast.LENGTH_SHORT).show();
            return;
        }

        connectionStatusTextView.setText("Testing connection...");

        // Here you would implement actual connection testing
        // For now, we'll simulate it
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simulate network delay

                runOnUiThread(() -> {
                    // Simulate random success/failure for demo
                    boolean success = Math.random() > 0.3;

                    if (success) {
                        connectionStatusTextView.setText("✅ Connection successful!");
                        connectionStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    } else {
                        connectionStatusTextView.setText("❌ Connection failed. Check URL and network.");
                        connectionStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    }
                });

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}