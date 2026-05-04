package com.example.fitsathi.utils;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.AuthFailureError;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ApiHelper {
    private static ApiHelper instance;
    private final RequestQueue requestQueue;

    private ApiHelper(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static synchronized ApiHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ApiHelper(context);
        }
        return instance;
    }

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    public void get(String url, Map<String, String> headers, ApiCallback callback) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                callback::onSuccess,
                error -> callback.onError(error.toString())) {
            @Override
            public Map<String, String> getHeaders() {
                try {
                    return headers != null ? headers : super.getHeaders();
                } catch (AuthFailureError e) {
                    e.printStackTrace();
                    return new HashMap<>();
                }
            }
        };
        requestQueue.add(request);
    }

    public void post(String url, JSONObject body, Map<String, String> headers, ApiCallback callback) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                callback::onSuccess,
                error -> callback.onError(error.toString())) {
            @Override
            public Map<String, String> getHeaders() {
                try {
                    return headers != null ? headers : super.getHeaders();
                } catch (AuthFailureError e) {
                    e.printStackTrace();
                    return new HashMap<>();
                }
            }
        };
        requestQueue.add(request);
    }
}
