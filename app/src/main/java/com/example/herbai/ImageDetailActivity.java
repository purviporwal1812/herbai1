package com.example.herbai;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class ImageDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detail);

        ImageView imageView = findViewById(R.id.imageView);

        // Get the selected image position from intent
        int position = getIntent().getIntExtra("image_position", 0);

        // Example image array, replace this with actual logic
        Integer[] imageIds = {R.drawable.sample1, R.drawable.sample2, R.drawable.sample3};

        // Set image based on position
        imageView.setImageResource(imageIds[position]);
    }
}
