package com.example.fitsathi.managers;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class StepCounterManager {

    private static final String TAG = "StepCounterManager";
    private static final String FIREBASE_STEP_COUNT_KEY = "step_counts";

    public interface StepCountCallback {
        void onStepCountReceived(int stepCount);
    }

    public interface StepHistoryCallback {
        void onStepHistoryReceived(Map<String, Integer> stepHistory);
    }

    private static DatabaseReference getStepCountRef() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return null;
        }
        String userId = user.getUid();
        return FirebaseDatabase.getInstance().getReference(FIREBASE_STEP_COUNT_KEY).child(userId);
    }

    private static String getTodayDateString() {
        return new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
    }

    public static void saveSteps(int steps) {
        DatabaseReference ref = getStepCountRef();
        if (ref != null) {
            ref.child(getTodayDateString()).setValue(steps);
        } else {
            Log.w(TAG, "Cannot save steps, user not logged in.");
        }
    }

    public static void getSteps(StepCountCallback callback) {
        DatabaseReference ref = getStepCountRef();
        if (ref == null) {
            callback.onStepCountReceived(0);
            return;
        }
        ref.child(getTodayDateString()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Integer steps = dataSnapshot.getValue(Integer.class);
                callback.onStepCountReceived(steps != null ? steps : 0);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to read step count from Firebase.", databaseError.toException());
                callback.onStepCountReceived(0);
            }
        });
    }

    public static void getLastNDaysSteps(int days, StepHistoryCallback callback) {
        DatabaseReference ref = getStepCountRef();
        if (ref == null) {
            callback.onStepHistoryReceived(getEmptyNDayStepMap(days));
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1));
        String startDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.getTime());
        String endDate = getTodayDateString();

        Query query = ref.orderByKey().startAt(startDate).endAt(endDate);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Integer> lastNDays = getEmptyNDayStepMap(days);
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String key = snapshot.getKey();
                    if (key != null && lastNDays.containsKey(key)) {
                        Integer steps = snapshot.getValue(Integer.class);
                        if (steps != null) {
                            lastNDays.put(key, steps);
                        }
                    }
                }
                callback.onStepHistoryReceived(lastNDays);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to read last " + days + " days steps from Firebase.", databaseError.toException());
                callback.onStepHistoryReceived(getEmptyNDayStepMap(days));
            }
        });
    }

    private static Map<String, Integer> getEmptyNDayStepMap(int days) {
        LinkedHashMap<String, Integer> lastNDays = new LinkedHashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        Calendar dayCalendar = Calendar.getInstance();
        dayCalendar.add(Calendar.DAY_OF_YEAR, -(days - 1));
        for (int i = 0; i < days; i++) {
            String dateKey = dateFormat.format(dayCalendar.getTime());
            lastNDays.put(dateKey, 0);
            dayCalendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return lastNDays;
    }
}
