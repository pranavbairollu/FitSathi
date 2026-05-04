package com.example.fitsathi.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitsathi.R;
import com.example.fitsathi.models.FoodItem;

public class FoodAdapter extends ListAdapter<FoodItem, FoodAdapter.FoodViewHolder> {

    private OnItemLongClickListener longClickListener;
    private OnItemClickListener clickListener;

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public FoodAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<FoodItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<FoodItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull FoodItem oldItem, @NonNull FoodItem newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull FoodItem oldItem, @NonNull FoodItem newItem) {
            return oldItem.equals(newItem);
        }
    };

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.food_item, parent, false);
        return new FoodViewHolder(view, clickListener, longClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        FoodItem item = getItem(position);
        holder.bind(item);
    }

    public FoodItem getFoodItemAt(int position) {
        return getItem(position);
    }

    static class FoodViewHolder extends RecyclerView.ViewHolder {
        TextView name, calories, carbs, protein, fat, fiber, sugar, grams;
        Context context;

        public FoodViewHolder(@NonNull View itemView, OnItemClickListener clickListener, OnItemLongClickListener longListener) {
            super(itemView);
            context = itemView.getContext();
            name = itemView.findViewById(R.id.food_name);
            calories = itemView.findViewById(R.id.food_calories);
            carbs = itemView.findViewById(R.id.food_carbs);
            protein = itemView.findViewById(R.id.food_protein);
            fat = itemView.findViewById(R.id.food_fat);
            fiber = itemView.findViewById(R.id.food_fiber);
            sugar = itemView.findViewById(R.id.food_sugar);
            grams = itemView.findViewById(R.id.food_grams);

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        clickListener.onItemClick(position);
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        longListener.onItemLongClick(position);
                        return true;
                    }
                }
                return false;
            });
        }

        void bind(FoodItem item) {
            name.setText(String.format("%s (%s)", item.getName().toUpperCase(), item.getMealType()));
            calories.setText(String.format("Calories: %.1f", item.getCalories()));
            carbs.setText(String.format("Carbs: %.1f g", item.getCarbs()));
            protein.setText(String.format("Protein: %.1f g", item.getProtein()));
            fat.setText(String.format("Fat: %.1f g", item.getFat()));
            fiber.setText(String.format("Fiber: %.1f g", item.getFibre()));
            sugar.setText(String.format("Sugar: %.1f g", item.getSugar()));
            grams.setText(String.format("Serving: %.1f g", item.getGrams()));

            applyThemeColors();
        }

        private void applyThemeColors() {
            int textColorPrimary = getThemeColor(context, R.attr.colorOnSurface);
            name.setTextColor(textColorPrimary);
            calories.setTextColor(textColorPrimary);
            carbs.setTextColor(textColorPrimary);
            protein.setTextColor(textColorPrimary);
            fat.setTextColor(textColorPrimary);
            fiber.setTextColor(textColorPrimary);
            sugar.setTextColor(textColorPrimary);
            grams.setTextColor(textColorPrimary);

            int surfaceColor = getThemeColor(context, R.attr.colorSurface);
            itemView.setBackgroundColor(surfaceColor);
        }

        private int getThemeColor(Context context, int attr) {
            TypedArray ta = context.getTheme().obtainStyledAttributes(new int[]{attr});
            int color = ta.getColor(0, 0);
            ta.recycle();
            return color;
        }
    }
}
