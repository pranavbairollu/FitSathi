package com.example.fitsathi;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.fitsathi.managers.ProgressStatsManager;
import com.example.fitsathi.models.WeeklyProgress;
import com.example.fitsathi.utils.ShareUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProgressShareActivity extends BaseActivity {

    private FrameLayout infographicContainer;
    private View infographicView;
    private View loadingOverlay;
    private WeeklyProgress weeklyProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_share);

        setupToolbar();
        infographicContainer = findViewById(R.id.infographic_container);
        loadingOverlay = findViewById(R.id.loading_overlay);

        loadProgressData();

        findViewById(R.id.btn_share_whatsapp).setOnClickListener(v -> shareToPlatform("whatsapp"));
        findViewById(R.id.btn_share_instagram).setOnClickListener(v -> shareToPlatform("instagram"));
        findViewById(R.id.btn_share_other).setOnClickListener(v -> shareToPlatform(null));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Weekly Progress");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadProgressData() {
        loadingOverlay.setVisibility(View.VISIBLE);
        ProgressStatsManager.getWeeklyProgress(this, progress -> {
            this.weeklyProgress = progress;
            setupInfographic();
            loadingOverlay.setVisibility(View.GONE);
        });
    }

    private void setupInfographic() {
        infographicView = LayoutInflater.from(this).inflate(R.layout.layout_progress_card_v1, infographicContainer, false);
        
        // Populate Data
        TextView tvName = infographicView.findViewById(R.id.tv_user_name);
        TextView tvDate = infographicView.findViewById(R.id.tv_date_range);
        ImageView ivAvatar = infographicView.findViewById(R.id.iv_user_avatar);
        
        TextView tvSteps = infographicView.findViewById(R.id.tv_stat_steps);
        TextView tvCalories = infographicView.findViewById(R.id.tv_stat_calories);
        TextView tvWater = infographicView.findViewById(R.id.tv_stat_water);
        TextView tvWorkouts = infographicView.findViewById(R.id.tv_stat_workouts);

        tvName.setText(weeklyProgress.getUserName() != null ? weeklyProgress.getUserName() : "FitSathi User");
        tvDate.setText(weeklyProgress.getStartDate() + " - " + weeklyProgress.getEndDate());
        
        if (weeklyProgress.getProfilePicUrl() != null) {
            Glide.with(this).load(weeklyProgress.getProfilePicUrl()).circleCrop().into(ivAvatar);
        }

        tvSteps.setText(String.format("%, d", weeklyProgress.getTotalSteps()));
        tvCalories.setText(String.format("%, d kcal", weeklyProgress.getAverageCalories()));
        tvWater.setText(weeklyProgress.getTotalWater() + " Glasses");
        tvWorkouts.setText(weeklyProgress.getTotalWorkouts() + " Days");

        setupChart(infographicView.findViewById(R.id.sharing_bar_chart));

        // Add to container and scale down for preview
        infographicContainer.removeAllViews();
        infographicContainer.addView(infographicView);
        
        // Scale to fit preview (1080x1920 -> 300dp x 533dp)
        // This is tricky with simple views, but FrameLayout handles it if we just let it be.
        // However, the card might look huge. Let's apply a scale if needed.
        infographicView.post(() -> {
            float scale = (float) infographicContainer.getWidth() / 1080f;
            infographicView.setScaleX(scale);
            infographicView.setScaleY(scale);
            infographicView.setPivotX(0);
            infographicView.setPivotY(0);
        });
    }

    private void setupChart(BarChart chart) {
        if (chart == null || weeklyProgress.getStepsHistory() == null) return;

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Integer> entry : weeklyProgress.getStepsHistory().entrySet()) {
            entries.add(new BarEntry(i, entry.getValue()));
            // Just take day of week if possible or last 2 digits
            labels.add(entry.getKey().substring(Math.max(0, entry.getKey().length() - 2)));
            i++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Steps");
        dataSet.setColor(0xFF3B82F6);
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        chart.setData(barData);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setTextColor(0xFF94A3B8);
        chart.getXAxis().setDrawGridLines(false);
        chart.getAxisLeft().setTextColor(0xFF94A3B8);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setGridColor(0xFF334155);
        
        chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return (index >= 0 && index < labels.size()) ? labels.get(index) : "";
            }
        });

        chart.invalidate();
    }

    private void shareToPlatform(String platform) {
        if (infographicView == null) return;

        loadingOverlay.setVisibility(View.VISIBLE);
        // Reset scale before capture to ensure high res
        float oldScaleX = infographicView.getScaleX();
        float oldScaleY = infographicView.getScaleY();
        infographicView.setScaleX(1f);
        infographicView.setScaleY(1f);

        infographicView.post(() -> {
            Bitmap bitmap = ShareUtils.getBitmapFromView(infographicView);
            
            // Restore scale for preview
            infographicView.setScaleX(oldScaleX);
            infographicView.setScaleY(oldScaleY);

            Uri uri = ShareUtils.saveBitmapToCache(this, bitmap);
            if (uri != null) {
                ShareUtils.shareImage(this, uri, platform);
            } else {
                Toast.makeText(this, "Failed to generate image", Toast.LENGTH_SHORT).show();
            }
            loadingOverlay.setVisibility(View.GONE);
        });
    }
}
