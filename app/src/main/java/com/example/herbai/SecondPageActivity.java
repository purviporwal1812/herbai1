package com.example.herbai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.GridView;
import androidx.appcompat.app.AppCompatActivity;

public class SecondPageActivity extends AppCompatActivity {
    private GridView gridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second_page);

        gridView = findViewById(R.id.gridView);

        // Example ImageAdapter, modify as per your needs
        gridView.setAdapter(new ImageAdapter(this));

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(SecondPageActivity.this, ImageDetailActivity.class);
            intent.putExtra("image_position", position);
            startActivity(intent);
        });
    }
}
