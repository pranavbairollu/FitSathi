package com.example.fitsathi.models;

public class SquadMemberStat {
    private String uid;
    private String displayName;
    private int steps;
    private float calories;
    private int rank;

    public SquadMemberStat() {
        // Default constructor for Firebase
    }

    public SquadMemberStat(String uid, String displayName, int steps, float calories) {
        this.uid = uid;
        this.displayName = displayName;
        this.steps = steps;
        this.calories = calories;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public int getSteps() { return steps; }
    public void setSteps(int steps) { this.steps = steps; }

    public float getCalories() { return calories; }
    public void setCalories(float calories) { this.calories = calories; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
}
