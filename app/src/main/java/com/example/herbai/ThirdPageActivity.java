package com.example.herbai;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ThirdPageActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_third_page);

        setupToggle(findViewById(R.id.faqQuestion1), findViewById(R.id.faqAnswer1));
        setupToggle(findViewById(R.id.faqQuestion2), findViewById(R.id.faqAnswer2));
        setupToggle(findViewById(R.id.faqQuestion3), findViewById(R.id.faqAnswer3));
        setupToggle(findViewById(R.id.faqQuestion4), findViewById(R.id.faqAnswer4));
    }

    private void setupToggle(TextView question, TextView answer) {
        question.setOnClickListener(v -> {
            if (answer.getVisibility() == View.GONE) {
                answer.setVisibility(View.VISIBLE);
                question.setText(question.getText().toString().replace("⬇", "⬆"));
            } else {
                answer.setVisibility(View.GONE);
                question.setText(question.getText().toString().replace("⬆", "⬇"));
            }
        });
    }
}
