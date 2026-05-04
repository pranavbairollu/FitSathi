package com.example.fitsathi.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class FoodItem implements Parcelable {
    private String key;
    private String name;
    private double calories;
    private double carbs;
    private double protein;
    private double fat;
    private double fibre;
    private double sugar;
    private double grams;   // serving size
    private String mealType;
    private String source;  // new: where it came from (search, scan, etc.)

    public FoodItem() {
        // Default constructor required for calls to DataSnapshot.getValue(FoodItem.class)
    }

    public FoodItem(String name, double calories, double carbs, double protein, double fat,
                    double fibre, double sugar, double grams, String mealType, String source) {
        this.name = name;
        this.calories = calories;
        this.carbs = carbs;
        this.protein = protein;
        this.fat = fat;
        this.fibre = fibre;
        this.sugar = sugar;
        this.grams = grams;
        this.mealType = mealType;
        this.source = source;
    }

    protected FoodItem(Parcel in) {
        key = in.readString();
        name = in.readString();
        calories = in.readDouble();
        carbs = in.readDouble();
        protein = in.readDouble();
        fat = in.readDouble();
        fibre = in.readDouble();
        sugar = in.readDouble();
        grams = in.readDouble();
        mealType = in.readString();
        source = in.readString();
    }

    public static final Creator<FoodItem> CREATOR = new Creator<FoodItem>() {
        @Override
        public FoodItem createFromParcel(Parcel in) {
            return new FoodItem(in);
        }

        @Override
        public FoodItem[] newArray(int size) {
            return new FoodItem[size];
        }
    };

    // Getters
    public String getKey() { return key; }
    public String getName() { return name; }
    public double getCalories() { return calories; }
    public double getCarbs() { return carbs; }
    public double getProtein() { return protein; }
    public double getFat() { return fat; }
    public double getFibre() { return fibre; }
    public double getSugar() { return sugar; }
    public double getGrams() { return grams; }
    public String getMealType() { return mealType; }
    public String getSource() { return source; }

    // Setters
    public void setKey(String key) { this.key = key; }
    public void setName(String name) { this.name = name; }
    public void setCalories(double calories) { this.calories = calories; }
    public void setCarbs(double carbs) { this.carbs = carbs; }
    public void setProtein(double protein) { this.protein = protein; }
    public void setFat(double fat) { this.fat = fat; }
    public void setFibre(double fibre) { this.fibre = fibre; }
    public void setSugar(double sugar) { this.sugar = sugar; }
    public void setGrams(double grams) { this.grams = grams; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    public void setSource(String source) { this.source = source; }


    // Serialize to JSON
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("name", name);
            obj.put("calories", calories);
            obj.put("carbs", carbs);
            obj.put("protein", protein);
            obj.put("fat", fat);
            obj.put("fibre", fibre);
            obj.put("sugar", sugar);
            obj.put("grams", grams);
            obj.put("mealType", mealType);
            obj.put("source", source);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    // Deserialize from JSON
    public static FoodItem fromJson(JSONObject obj) {
        if (obj == null) return null;
        try {
            return new FoodItem(
                    obj.optString("name", "Unknown"),
                    obj.optDouble("calories", 0),
                    obj.optDouble("carbs", 0),
                    obj.optDouble("protein", 0),
                    obj.optDouble("fat", 0),
                    obj.optDouble("fibre", 0),
                    obj.optDouble("sugar", 0),
                    obj.optDouble("grams", 0),
                    obj.optString("mealType", "Unknown"),
                    obj.optString("source", "Unknown")
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Return null on failure to prevent crashes
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FoodItem foodItem = (FoodItem) o;
        return Double.compare(foodItem.calories, calories) == 0 &&
                Double.compare(foodItem.carbs, carbs) == 0 &&
                Double.compare(foodItem.protein, protein) == 0 &&
                Double.compare(foodItem.fat, fat) == 0 &&
                Double.compare(foodItem.fibre, fibre) == 0 &&
                Double.compare(foodItem.sugar, sugar) == 0 &&
                Double.compare(foodItem.grams, grams) == 0 &&
                Objects.equals(key, foodItem.key) &&
                Objects.equals(name, foodItem.name) &&
                Objects.equals(mealType, foodItem.mealType) &&
                Objects.equals(source, foodItem.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, name, calories, carbs, protein, fat, fibre, sugar, grams, mealType, source);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(key);
        dest.writeString(name);
        dest.writeDouble(calories);
        dest.writeDouble(carbs);
        dest.writeDouble(protein);
        dest.writeDouble(fat);
        dest.writeDouble(fibre);
        dest.writeDouble(sugar);
        dest.writeDouble(grams);
        dest.writeString(mealType);
        dest.writeString(source);
    }
}
