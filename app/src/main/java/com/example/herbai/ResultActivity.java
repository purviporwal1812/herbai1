package com.example.herbai;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
public class ResultActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        ListView plantListView = findViewById(R.id.plantListView);
        TextView plantUsesTextView = findViewById(R.id.plantUsesTextView);

        // Dummy JSON object with probable plant names and uses
        ArrayList<String> probablePlants = new ArrayList<>(Arrays.asList(
                "Tulsi",
                "Neem",
                "Aloe Vera"
        ));
        String topPlantUses = "Aloe Vera is used for skin treatment, digestion improvement, and healing wounds.";

        // Display plant names in ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, probablePlants);
        plantListView.setAdapter(adapter);

        // Show uses of the top probable plant
        plantUsesTextView.setText("Top Plant Uses:\n" + topPlantUses);

    }
}
