package com.example.fitsathi;

public class UserInfo {
    public String age, height, weight, gender, activity;

    public UserInfo() {
        // Default constructor required for calls to DataSnapshot.getValue(UserInfo.class)
    }

    public UserInfo(String age, String height, String weight, String gender, String activity) {
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.gender = gender;
        this.activity = activity;
    }
}
