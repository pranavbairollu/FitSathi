package com.example.fitsathi.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class Exercise implements Parcelable {
    public String name;
    public String setsReps;
    public int imageRes;
    public int duration; // in minutes
    public String level;
    public String location;
    public List<String> goals;
    public String description; // NEW: instructions
    public List<String> muscles; // NEW: target muscle groups
    public double intensity; // NEW: 1.0 to 2.0 multiplier for calories

    public int calories;      // calories burned
    public int durationSec;   // duration in seconds (for timers)
    public long completionTimestamp; // When the exercise was completed

    // Default constructor needed for JSON loader
    public Exercise() {}

    // Convenience constructor (optional)
    public Exercise(String name, String setsReps, int imageRes, int duration,
                    String level, String location, List<String> goals,
                    String description, List<String> muscles, double intensity) {
        this.name = name;
        this.setsReps = setsReps;
        this.imageRes = imageRes;
        this.duration = duration;
        this.level = level;
        this.location = location;
        this.goals = goals;
        this.description = description;
        this.muscles = muscles;
        this.intensity = intensity;

        // auto-calculate derived fields
        this.durationSec = duration * 60;
        this.calories = (int) (duration * 5 * intensity);
        this.completionTimestamp = 0;
    }

    protected Exercise(Parcel in) {
        name = in.readString();
        setsReps = in.readString();
        imageRes = in.readInt();
        duration = in.readInt();
        level = in.readString();
        location = in.readString();
        goals = in.createStringArrayList();
        calories = in.readInt();
        durationSec = in.readInt();
        completionTimestamp = in.readLong();
        description = in.readString();
        muscles = in.createStringArrayList();
        intensity = in.readDouble();
    }

    public static final Creator<Exercise> CREATOR = new Creator<Exercise>() {
        @Override
        public Exercise createFromParcel(Parcel in) {
            return new Exercise(in);
        }

        @Override
        public Exercise[] newArray(int size) {
            return new Exercise[size];
        }
    };

    // Optional getters
    public String getName() { return name != null ? name : "Unknown"; }
    public String getSetsReps() { return setsReps != null ? setsReps : "N/A"; }
    public int getImageRes() { return imageRes; }
    public int getDuration() { return duration; }
    public String getLevel() { return level != null ? level : "N/A"; }
    public String getLocation() { return location != null ? location : "Anywhere"; }
    public List<String> getGoals() { return goals; }
    public int getCalories() { return calories; }
    public int getDurationSec() { return durationSec; }
    public long getCompletionTimestamp() { return completionTimestamp; }
    public String getDescription() { return description != null ? description : "No instructions available."; }
    public List<String> getMuscles() { return muscles; }
    public double getIntensity() { return intensity; }

    // Determine duration category: Short / Medium / Long
    public String getDurationCategory() {
        if (duration <= 20) return "Short";
        else if (duration <= 40) return "Medium";
        else return "Long";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(setsReps);
        dest.writeInt(imageRes);
        dest.writeInt(duration);
        dest.writeString(level);
        dest.writeString(location);
        dest.writeStringList(goals);
        dest.writeInt(calories);
        dest.writeInt(durationSec);
        dest.writeLong(completionTimestamp);
        dest.writeString(description);
        dest.writeStringList(muscles);
        dest.writeDouble(intensity);
    }
}
