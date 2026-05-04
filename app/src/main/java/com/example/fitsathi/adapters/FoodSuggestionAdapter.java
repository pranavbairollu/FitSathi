package com.example.fitsathi.adapters;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitsathi.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

// 1. EXTEND from ListAdapter for automatic diffing and animations.
public class FoodSuggestionAdapter extends ListAdapter<JSONObject, FoodSuggestionAdapter.SuggestionViewHolder> {

    public interface OnFoodSelectedListener {
        void onFoodSelected(JSONObject food);
    }

    private final OnFoodSelectedListener listener;
    private int selectedPosition = -1;

    // 2. CONSTRUCTOR is simplified. The list is managed by the adapter itself.
    public FoodSuggestionAdapter(@NonNull OnFoodSelectedListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    // 3. NEW: The DiffUtil.ItemCallback that powers ListAdapter.
    private static final DiffUtil.ItemCallback<JSONObject> DIFF_CALLBACK = new DiffUtil.ItemCallback<JSONObject>() {
        @Override
        public boolean areItemsTheSame(@NonNull JSONObject oldItem, @NonNull JSONObject newItem) {
            // Use a unique identifier if available, otherwise compare the string representation.
            return oldItem.optString("food_id").equals(newItem.optString("food_id"))
                    && oldItem.optString("food_name").equals(newItem.optString("food_name"));
        }

        @Override
        public boolean areContentsTheSame(@NonNull JSONObject oldItem, @NonNull JSONObject newItem) {
            // For JSONObjects, comparing string representations is a reliable way to check for content changes.
            return Objects.equals(oldItem.toString(), newItem.toString());
        }
    };


    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food_suggestion, parent, false);
        return new SuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        // 4. Use ListAdapter's built-in getItem() method.
        JSONObject food = getItem(position);
        holder.bind(food, position == selectedPosition);

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // Notify adapter about the changes to re-bind the views.
            if (previousPosition != -1) {
                notifyItemChanged(previousPosition);
            }
            notifyItemChanged(selectedPosition);

            listener.onFoodSelected(food);
        });
    }

    // New method to reset selection when the list is cleared or a new search is made.
    public void resetSelection() {
        selectedPosition = -1;
    }

    // ViewHolder is now responsible for its own view state.
    static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        TextView foodName;

        public SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            foodName = itemView.findViewById(R.id.suggestion_name);
        }

        void bind(JSONObject food, boolean isSelected) {
            String name = food.optString("food_name", "Unknown Food").toUpperCase();
            foodName.setText(name);

            // Set background color based on the 'isSelected' state.
            final int selectedColor = getThemeColor(itemView.getContext(), com.google.android.material.R.attr.colorPrimaryContainer);
            final int defaultColor = getThemeColor(itemView.getContext(), com.google.android.material.R.attr.colorSurface);

            itemView.setBackgroundColor(isSelected ? selectedColor : defaultColor);

            // Set text color for better contrast
            final int selectedTextColor = getThemeColor(itemView.getContext(), com.google.android.material.R.attr.colorOnPrimaryContainer);
            final int defaultTextColor = getThemeColor(itemView.getContext(), com.google.android.material.R.attr.colorOnSurface);

            foodName.setTextColor(isSelected ? selectedTextColor : defaultTextColor);
        }

        private int getThemeColor(Context context, @AttrRes int colorAttr) {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(colorAttr, typedValue, true);
            return ContextCompat.getColor(context, typedValue.resourceId);
        }
    }
}
