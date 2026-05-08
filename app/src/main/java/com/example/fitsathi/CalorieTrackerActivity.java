package com.example.fitsathi;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

public class CalorieTrackerActivity extends BaseActivity {

    private EditText foodInput;
    private Button trackButton;
    private PieChart pieChart;

    // Uses keys from local.properties (via BuildConfig)
    private static final String APP_ID = com.example.fitsathi.BuildConfig.NUTRITIONIX_APP_ID;
    private static final String API_KEY = com.example.fitsathi.BuildConfig.NUTRITIONIX_APP_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calorie_tracker);

        foodInput = findViewById(R.id.food_input);
        trackButton = findViewById(R.id.track_button);
        pieChart = findViewById(R.id.pie_chart);

        trackButton.setOnClickListener(view -> {
            String query = foodInput.getText().toString().trim();
            if (!query.isEmpty()) {
                fetchNutritionData(query);
            } else {
                Toast.makeText(this, "Enter a food item", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchNutritionData(String query) {
        new Thread(() -> {
            try {
                URL url = new URL("https://trackapi.nutritionix.com/v2/natural/nutrients");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("x-app-id", APP_ID);
                conn.setRequestProperty("x-app-key", API_KEY);
                conn.setRequestProperty("Content-Type", "application/json");

                conn.setDoOutput(true);
                String body = "{\"query\":\"" + query + "\"}";
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes());
                }

                Scanner scanner = new Scanner(conn.getInputStream());
                StringBuilder response = new StringBuilder();
                while (scanner.hasNext()) response.append(scanner.nextLine());
                scanner.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray foods = jsonResponse.getJSONArray("foods");

                if (foods.length() > 0) {
                    JSONObject food = foods.getJSONObject(0);
                    float calories = (float) food.getDouble("nf_calories");
                    float carbs = (float) food.getDouble("nf_total_carbohydrate");
                    float protein = (float) food.getDouble("nf_protein");
                    float fat = (float) food.getDouble("nf_total_fat");

                    runOnUiThread(() -> updatePieChart(calories, carbs, protein, fat));
                }

            } catch (Exception e) {
                Log.e("NutritionixAPI", "Error: ", e);
                runOnUiThread(() -> Toast.makeText(this, "Failed to fetch data", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void updatePieChart(float calories, float carbs, float protein, float fat) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(carbs, "Carbs"));
        entries.add(new PieEntry(protein, "Protein"));
        entries.add(new PieEntry(fat, "Fat"));

        PieDataSet dataSet = new PieDataSet(entries, "Macronutrients");
        dataSet.setColors(Color.YELLOW, Color.GREEN, Color.RED);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(16f);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);

        Description description = new Description();
        description.setText("Nutrient Breakdown");
        pieChart.setDescription(description);

        pieChart.invalidate(); // refresh
    }
}
