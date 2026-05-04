package com.example.fitsathi.managers;

import com.example.fitsathi.models.FoodItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FoodLogManager {

    private static final String FIREBASE_FOOD_LOG_KEY = "food_logs";

    public interface FoodListCallback {
        void onFoodListReceived(List<FoodItem> foodList);
    }

    private static DatabaseReference getFoodLogRef() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return FirebaseDatabase.getInstance().getReference(FIREBASE_FOOD_LOG_KEY).child(userId);
    }

    public interface LogOperationCallback {
        void onComplete(boolean success);
    }

    public static void addFoodItem(String date, FoodItem newItem, LogOperationCallback callback) {
        getFoodLogRef().child(date).push().setValue(newItem).addOnCompleteListener(task -> {
            if (callback != null) callback.onComplete(task.isSuccessful());
        });
    }

    public static void updateFoodItem(String date, FoodItem item, LogOperationCallback callback) {
        getFoodLogRef().child(date).child(item.getKey()).setValue(item).addOnCompleteListener(task -> {
            if (callback != null) callback.onComplete(task.isSuccessful());
        });
    }

    public static void removeFoodItem(String date, String key, LogOperationCallback callback) {
        getFoodLogRef().child(date).child(key).removeValue().addOnCompleteListener(task -> {
            if (callback != null) callback.onComplete(task.isSuccessful());
        });
    }

    public static void getFoodListForDate(String date, FoodListCallback callback) {
        getFoodLogRef().child(date).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<FoodItem> foodList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    FoodItem foodItem = snapshot.getValue(FoodItem.class);
                    if (foodItem != null) {
                        foodItem.setKey(snapshot.getKey());
                        foodList.add(foodItem);
                    }
                }
                callback.onFoodListReceived(foodList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onFoodListReceived(new ArrayList<>());
            }
        });
    }
}
