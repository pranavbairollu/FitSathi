
package com.example.fitsathi.managers;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class UserInfoManager {

    private static final String FIREBASE_USERS_KEY = "users";

    public interface UserInfoCallback {
        void onUserInfoReceived(UserInfo userInfo);
    }

    public static class UserInfo {
        private String name;
        private int age;
        private float height;
        private float weight;
        private String gender;
        private String activityLevel;
        private String profilePicUrl;

        public UserInfo() {}

        // Getters
        public String getName() { return name; }
        public int getAge() { return age; }
        public float getHeight() { return height; }
        public float getWeight() { return weight; }
        public String getGender() { return gender; }
        public String getActivityLevel() { return activityLevel; }
        public String getProfilePicUrl() { return profilePicUrl; }

        // Setters
        public void setName(String name) { this.name = name; }
        public void setAge(int age) { this.age = age; }
        public void setHeight(float height) { this.height = height; }
        public void setWeight(float weight) { this.weight = weight; }
        public void setGender(String gender) { this.gender = gender; }
        public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }
        public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }
    }

    private static DatabaseReference getUserRef() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return FirebaseDatabase.getInstance().getReference(FIREBASE_USERS_KEY).child(userId);
    }

    public static void saveUserInfo(UserInfo userInfo) {
        getUserRef().setValue(userInfo);
    }

    public static void getUserInfo(UserInfoCallback callback) {
        getUserRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserInfo userInfo = dataSnapshot.getValue(UserInfo.class);
                callback.onUserInfoReceived(userInfo);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onUserInfoReceived(null);
            }
        });
    }

    public static void setProfilePicUrl(String url) {
        getUserRef().child("profilePicUrl").setValue(url);
    }

    public static String getEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user != null) ? user.getEmail() : "No Email";
    }

    public static double calculateBMR(UserInfo userInfo) {
        if (userInfo.getWeight() == 0 || userInfo.getHeight() == 0 || userInfo.getAge() == 0) {
            return 2000;
        }
        if ("Male".equalsIgnoreCase(userInfo.getGender())) {
            return 88.362 + (13.397 * userInfo.getWeight()) + (4.799 * userInfo.getHeight()) - (5.677 * userInfo.getAge());
        } else {
            return 447.593 + (9.247 * userInfo.getWeight()) + (3.098 * userInfo.getHeight()) - (4.330 * userInfo.getAge());
        }
    }

    public static double calculateDailyCalorieGoal(UserInfo userInfo) {
        double bmr = calculateBMR(userInfo);
        if(userInfo.getActivityLevel() == null) return bmr * 1.2;
        switch (userInfo.getActivityLevel()) {
            case "Sedentary":
                return bmr * 1.2;
            case "Lightly Active":
                return bmr * 1.375;
            case "Moderately Active":
                return bmr * 1.55;
            case "Very Active":
                return bmr * 1.725;
            case "Extra Active":
                return bmr * 1.9;
            default:
                return bmr * 1.2;
        }
    }

    public static void getStreak(StreakCallback callback) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference foodLogRef = FirebaseDatabase.getInstance().getReference("food_logs").child(userId);

        foodLogRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    callback.onStreakReceived(0);
                    return;
                }

                Set<String> logDates = new HashSet<>();
                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    logDates.add(dateSnapshot.getKey());
                }

                // FIX: Standardize on UTC timezone and CORRECT date format to match Firebase
                TimeZone utc = TimeZone.getTimeZone("UTC");
                Calendar calendar = Calendar.getInstance(utc);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT); // Use ROOT locale for non-linguistic formatting
                sdf.setTimeZone(utc);

                // If today (in UTC) hasn't been logged, start checking from yesterday
                if (!logDates.contains(sdf.format(calendar.getTime()))) {
                    calendar.add(Calendar.DATE, -1);
                }

                int consecutiveDays = 0;
                // Count backwards day-by-day as long as a log exists for that date
                while (logDates.contains(sdf.format(calendar.getTime()))) {
                    consecutiveDays++;
                    calendar.add(Calendar.DATE, -1);
                }

                callback.onStreakReceived(consecutiveDays);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onStreakReceived(0);
            }
        });
    }

    public interface StreakCallback {
        void onStreakReceived(int streak);
    }
}
