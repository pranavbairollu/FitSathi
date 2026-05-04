package com.example.fitsathi.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fitsathi.R;
import com.example.fitsathi.managers.FoodLogManager;
import com.example.fitsathi.models.FoodItem;
import com.example.fitsathi.utils.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;

public class CalendarFragment extends Fragment {

    private TextView selectedDateText;
    private CalendarView calendarView;
    private ListView foodListView;
    private ArrayAdapter<String> adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        selectedDateText = view.findViewById(R.id.selected_date_text);
        calendarView = view.findViewById(R.id.calendar_view);
        foodListView = view.findViewById(R.id.food_list_view);

        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        foodListView.setAdapter(adapter);

        // Set initial date to today
        Calendar todayCal = Calendar.getInstance();
        String today = DateUtils.formatDate(todayCal.getTime());
        selectedDateText.setText(today);

        // Load food list for today
        loadFoodList(today);

        // Handle date changes
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);

            String date = DateUtils.formatDate(cal.getTime());
            selectedDateText.setText(date);

            loadFoodList(date);
        });

        return view;
    }

    private void loadFoodList(String date) {
        if (getContext() == null) return; // Avoid crashes if fragment is detached
        FoodLogManager.getFoodListForDate(date, foodItems -> {
            if(isAdded()) {
                adapter.clear();
                for (FoodItem item : foodItems) {
                    adapter.add(item.getName());
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}
