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

    public int calories;      // calories burned
    public int durationSec;   // duration in seconds (for timers)

    // Default constructor needed for JSON loader
    public Exercise() {}

    // Convenience constructor (optional)
    public Exercise(String name, String setsReps, int imageRes, int duration,
                    String level, String location, List<String> goals) {
        this.name = name;
        this.setsReps = setsReps;
        this.imageRes = imageRes;
        this.duration = duration;
        this.level = level;
        this.location = location;
        this.goals = goals;

        // auto-calculate derived fields
        this.durationSec = duration * 60;
        this.calories = duration * 5;
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
    public String getName() { return name; }
    public String getSetsReps() { return setsReps; }
    public int getImageRes() { return imageRes; }
    public int getDuration() { return duration; }
    public String getLevel() { return level; }
    public String getLocation() { return location; }
    public List<String> getGoals() { return goals; }
    public int getCalories() { return calories; }
    public int getDurationSec() { return durationSec; }

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
    }
}
