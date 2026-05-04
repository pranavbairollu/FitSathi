package com.example.fitsathi.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.fitsathi.R;
import com.example.fitsathi.models.FoodItem;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class EditFoodDialogFragment extends DialogFragment {

    private static final String ARG_FOOD_ITEM = "food_item";

    private FoodItem foodItem;

    private OnFoodItemUpdatedListener listener;

    public interface OnFoodItemUpdatedListener {
        void onFoodItemUpdated(FoodItem foodItem);
    }

    public void setOnFoodItemUpdatedListener(OnFoodItemUpdatedListener listener) {
        this.listener = listener;
    }

    public static EditFoodDialogFragment newInstance(FoodItem foodItem) {
        EditFoodDialogFragment fragment = new EditFoodDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FOOD_ITEM, foodItem);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            foodItem = getArguments().getParcelable(ARG_FOOD_ITEM);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_serving, null);

        TextInputEditText inputServing = dialogView.findViewById(R.id.input_serving);
        TextView valueCalories = dialogView.findViewById(R.id.value_calories);
        TextView valueCarbs = dialogView.findViewById(R.id.value_carbs);
        TextView valueProtein = dialogView.findViewById(R.id.value_protein);
        TextView valueFat = dialogView.findViewById(R.id.value_fat);
        TextView valueFiber = dialogView.findViewById(R.id.value_fiber);
        TextView valueSugar = dialogView.findViewById(R.id.value_sugar);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView);

        final Dialog dialog = builder.create();

        if (foodItem != null) {
            final double currentGrams = foodItem.getGrams();
            final double caloriesPerGram = currentGrams > 0 ? foodItem.getCalories() / currentGrams : 0;
            final double carbsPerGram = currentGrams > 0 ? foodItem.getCarbs() / currentGrams : 0;
            final double proteinPerGram = currentGrams > 0 ? foodItem.getProtein() / currentGrams : 0;
            final double fatPerGram = currentGrams > 0 ? foodItem.getFat() / currentGrams : 0;
            final double fiberPerGram = currentGrams > 0 ? foodItem.getFibre() / currentGrams : 0;
            final double sugarPerGram = currentGrams > 0 ? foodItem.getSugar() / currentGrams : 0;

            inputServing.setText(String.valueOf(currentGrams));

            inputServing.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        double newServing = Double.parseDouble(s.toString());
                        valueCalories.setText(String.format("%.1f kcal", newServing * caloriesPerGram));
                        valueCarbs.setText(String.format("%.1f g", newServing * carbsPerGram));
                        valueProtein.setText(String.format("%.1f g", newServing * proteinPerGram));
                        valueFat.setText(String.format("%.1f g", newServing * fatPerGram));
                        valueFiber.setText(String.format("%.1f g", newServing * fiberPerGram));
                        valueSugar.setText(String.format("%.1f g", newServing * sugarPerGram));
                    } catch (NumberFormatException e) {
                        valueCalories.setText("0 kcal");
                        valueCarbs.setText("0 g");
                        valueProtein.setText("0 g");
                        valueFat.setText("0 g");
                        valueFiber.setText("0 g");
                        valueSugar.setText("0 g");
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            btnSave.setOnClickListener(v -> {
                try {
                    String inputText = inputServing.getText().toString();
                    if (inputText.isEmpty()) {
                        Toast.makeText(requireContext(), "Please enter serving weight", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double newServingSize = Double.parseDouble(inputText);
                    if (newServingSize <= 0) {
                        Toast.makeText(requireContext(), "Serving size must be greater than 0g", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    foodItem.setGrams(newServingSize);
                    foodItem.setCalories(newServingSize * caloriesPerGram);
                    foodItem.setCarbs(newServingSize * carbsPerGram);
                    foodItem.setProtein(newServingSize * proteinPerGram);
                    foodItem.setFat(newServingSize * fatPerGram);
                    foodItem.setFibre(newServingSize * fiberPerGram);
                    foodItem.setSugar(newServingSize * sugarPerGram);

                    if (listener != null) {
                        listener.onFoodItemUpdated(foodItem);
                    }
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Invalid serving size", Toast.LENGTH_SHORT).show();
                }
            });
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        return dialog;
    }
}
