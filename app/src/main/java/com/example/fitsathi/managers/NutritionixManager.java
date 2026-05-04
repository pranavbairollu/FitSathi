package com.example.fitsathi.managers;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.fitsathi.models.FoodItem;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class NutritionixManager {

    private static final String API_URL = "https://trackapi.nutritionix.com/v2/natural/nutrients";
    private static final String API_ID = com.example.fitsathi.BuildConfig.NUTRITIONIX_APP_ID;
    private static final String API_KEY = com.example.fitsathi.BuildConfig.NUTRITIONIX_APP_KEY;

    public interface NutritionixCallback {
        void onSuccess(FoodItem foodItem);
        void onError(String message);
    }

    public static void fetchNutrition(Context context, String query, NutritionixCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(context);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("query", query);
        } catch (Exception e) {
            callback.onError("Failed to build request.");
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, API_URL, requestBody,
                response -> {
                    try {
                        JSONArray foods = response.getJSONArray("foods");
                        if (foods.length() > 0) {
                            JSONObject foodJson = foods.getJSONObject(0);
                            FoodItem item = new FoodItem(
                                    foodJson.optString("food_name", "Unknown"),
                                    foodJson.optDouble("nf_calories", 0),
                                    foodJson.optDouble("nf_total_carbohydrate", 0),
                                    foodJson.optDouble("nf_protein", 0),
                                    foodJson.optDouble("nf_total_fat", 0),
                                    foodJson.optDouble("nf_dietary_fiber", 0),
                                    foodJson.optDouble("nf_sugars", 0),
                                    foodJson.optDouble("serving_weight_grams", 0),
                                    "", // Meal type is set later
                                    "Nutritionix"
                            );
                            callback.onSuccess(item);
                        } else {
                            callback.onError("No food found for '" + query + "'");
                        }
                    } catch (Exception e) {
                        callback.onError("Failed to parse nutrition data.");
                    }
                },
                error -> callback.onError("Network error. Please try again.")
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("x-app-id", API_ID);
                headers.put("x-app-key", API_KEY);
                return headers;
            }
        };

        queue.add(jsonObjectRequest);
    }
}
