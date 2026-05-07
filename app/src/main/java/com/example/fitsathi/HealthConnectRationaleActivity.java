package com.example.fitsathi;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity required by Health Connect to show why permissions are needed.
 */
public class HealthConnectRationaleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_rationale);

        TextView title = findViewById(R.id.rationale_title);
        TextView description = findViewById(R.id.rationale_description);
        Button okButton = findViewById(R.id.btn_ok);

        title.setText(R.string.health_sync_rationale_title);
        description.setText(R.string.health_sync_rationale_desc);

        okButton.setOnClickListener(v -> finish());
    }
}
