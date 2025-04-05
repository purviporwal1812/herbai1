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

        // Receive the formatted list: ["Species (0.02)", ...]
        ArrayList<String> probablePlants =
                getIntent().getStringArrayListExtra("probablePlants");

        if (probablePlants == null || probablePlants.isEmpty()) {
            probablePlants = new ArrayList<>();
            probablePlants.add("No plants identified");
        }

        // (Optional) if you still want to show uses
        String topPlantUses = getIntent().getStringExtra("topPlantUses");
        if (topPlantUses != null) {
            plantUsesTextView.setText(topPlantUses);
        }

        // Use your existing adapter
        PlantAdapter adapter = new PlantAdapter(this, probablePlants);
        plantRecyclerView.setAdapter(adapter);
    }
}
