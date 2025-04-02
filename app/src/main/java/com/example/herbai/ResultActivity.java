package com.example.herbai;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;

public class ResultActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        RecyclerView plantRecyclerView = findViewById(R.id.plantListView); // Use RecyclerView
        plantRecyclerView.setLayoutManager(new LinearLayoutManager(this)); // Set layout manager

        // Dummy JSON object with probable plant names and uses
        ArrayList<String> probablePlants = new ArrayList<>(Arrays.asList(
                "Tulsi",
                "Neem",
                "Aloe Vera"
        ));

        String topPlantUses = "Aloe Vera is used for skin treatment, digestion improvement, and healing wounds.";

        // Set custom adapter
        PlantAdapter adapter = new PlantAdapter(this, probablePlants);
        plantRecyclerView.setAdapter(adapter);
    }
}
