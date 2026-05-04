package com.example.fitsathi.models;

public class WeightLog {
    private long timestamp;
    private float weight;

    public WeightLog() {
        // Default constructor required for calls to DataSnapshot.getValue(WeightLog.class)
    }

    public WeightLog(long timestamp, float weight) {
        this.timestamp = timestamp;
        this.weight = weight;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
}
