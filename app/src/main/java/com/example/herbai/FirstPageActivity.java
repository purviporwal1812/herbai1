package com.example.herbai;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class FirstPageActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_page);

        TextView aboutText = findViewById(R.id.aboutText);
        aboutText.setText("HerbAI: A Plant Classification App\n\nDeveloped by: Your Team\nFor more info, visit our website.");
    }
}
