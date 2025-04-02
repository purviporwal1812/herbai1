package com.example.herbai;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ResultActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        RecyclerView plantRecyclerView = findViewById(R.id.plantListView);
        TextView plantUsesTextView = findViewById(R.id.plantUsesTextView);

        plantRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get data from intent
        ArrayList<String> probablePlants = getIntent().getStringArrayListExtra("probablePlants");
        String topPlantUses = getIntent().getStringExtra("topPlantUses");

        // If no data received, use default values
        if (probablePlants == null) {
            probablePlants = new ArrayList<>();
            probablePlants.add("No plants identified");
        }

        if (topPlantUses != null) {
            plantUsesTextView.setText(topPlantUses);
        }

        // Set custom adapter
        PlantAdapter adapter = new PlantAdapter(this, probablePlants);
        plantRecyclerView.setAdapter(adapter);
    }
}