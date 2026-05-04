package com.example.fitsathi.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitsathi.R;
import com.example.fitsathi.models.FoodItem;

import java.util.List;

public class FoodLogAdapter extends RecyclerView.Adapter<FoodLogAdapter.ViewHolder> {

    private final List<FoodItem> foodItems;

    public FoodLogAdapter(List<FoodItem> foodItems) {
        this.foodItems = foodItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodItem foodItem = foodItems.get(position);
        holder.foodNameTextView.setText(foodItem.getName());
        holder.calorieTextView.setText(String.format("%.0f kcal", foodItem.getCalories()));
        holder.servingInfoTextView.setText(String.format("1 serving (%.0f g)", foodItem.getGrams()));
    }

    @Override
    public int getItemCount() {
        return foodItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView foodNameTextView;
        TextView calorieTextView;
        TextView servingInfoTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            foodNameTextView = itemView.findViewById(R.id.food_name_text_view);
            calorieTextView = itemView.findViewById(R.id.calories_text_view);
            servingInfoTextView = itemView.findViewById(R.id.serving_info_text_view);
        }
    }
}
