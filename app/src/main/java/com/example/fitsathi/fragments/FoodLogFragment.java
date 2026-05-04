package com.example.fitsathi.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitsathi.R;
import com.example.fitsathi.adapters.FoodLogAdapter;
import com.example.fitsathi.managers.FoodLogManager;
import com.example.fitsathi.models.FoodItem;
import com.example.fitsathi.utils.DateUtils;

import java.util.Calendar;
import java.util.Date;

public class FoodLogFragment extends Fragment {

    private TextView selectedDateText;
    private RecyclerView recyclerView;
    private FoodLogAdapter foodLogAdapter;
    private TextView emptyView;
    private Calendar selectedDate = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_food_log, container, false);

        selectedDateText = view.findViewById(R.id.selected_date_text);
        recyclerView = view.findViewById(R.id.food_log_recycler_view);
        emptyView = view.findViewById(R.id.empty_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        view.findViewById(R.id.date_picker_container).setOnClickListener(v -> showDatePickerDialog());

        updateDateDisplay();
        updateFoodList();

        return view;
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    updateDateDisplay();
                    updateFoodList();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateDisplay() {
        selectedDateText.setText(DateUtils.formatDate(selectedDate.getTime()));
    }

    private void updateFoodList() {
        Date date = selectedDate.getTime();
        String formattedDate = DateUtils.formatDate(date);
        FoodLogManager.getFoodListForDate(formattedDate, foodItems -> {
            if (isAdded()) {
                if (foodItems.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                    foodLogAdapter = new FoodLogAdapter(foodItems);
                    recyclerView.setAdapter(foodLogAdapter);
                }
            }
        });
    }
}
