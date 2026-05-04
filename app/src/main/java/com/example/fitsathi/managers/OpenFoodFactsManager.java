package com.example.fitsathi.managers;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;

public class OpenFoodFactsManager {

    private static final String API_URL = "https://world.openfoodfacts.org/api/v0/product/";

    public interface OpenFoodFactsCallback {
        void onSuccess(JSONObject product);
        void onError(String message);
    }

    public static void fetchProductByBarcode(Context context, String barcode, OpenFoodFactsCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = API_URL + barcode + ".json";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.has("product")) {
                            callback.onSuccess(response.getJSONObject("product"));
                        } else {
                            callback.onError("Product not found.");
                        }
                    } catch (Exception e) {
                        callback.onError("Failed to parse product data.");
                    }
                },
                error -> callback.onError("Network error or invalid barcode.")
        );

        queue.add(jsonObjectRequest);
    }
}
