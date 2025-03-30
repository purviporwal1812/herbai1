package com.example.herbai;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        ListView plantListView = findViewById(R.id.plantListView);
        TextView plantUsesTextView = findViewById(R.id.plantUsesTextView);

        String response = getIntent().getStringExtra("apiResponse");

        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray probablePlantsArray = jsonResponse.getJSONArray("probablePlants");
            String topPlantUses = jsonResponse.getString("topPlantUses");

            ArrayList<String> probablePlants = new ArrayList<>();
            for (int i = 0; i < probablePlantsArray.length(); i++) {
                probablePlants.add(probablePlantsArray.getString(i));
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, probablePlants);
            plantListView.setAdapter(adapter);

            plantUsesTextView.setText("Top Plant Uses:\n" + topPlantUses);
        } catch (Exception e) {
            e.printStackTrace();
            plantUsesTextView.setText("Error processing results");
        }
    }
}