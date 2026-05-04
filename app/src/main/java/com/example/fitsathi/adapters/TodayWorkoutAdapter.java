package com.example.fitsathi.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitsathi.R;
import com.example.fitsathi.WorkoutActivity;
import com.example.fitsathi.models.Exercise;

import java.util.List;

public class TodayWorkoutAdapter extends RecyclerView.Adapter<TodayWorkoutAdapter.ExerciseViewHolder> {

    private final Context context;
    private final List<Exercise> exerciseList;

    public TodayWorkoutAdapter(Context context, List<Exercise> exerciseList) {
        this.context = context;
        this.exerciseList = exerciseList;
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_today_workout, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        Exercise exercise = exerciseList.get(position);

        holder.tvName.setText(exercise.getName());
        holder.tvSets.setText(exercise.getSetsReps());
        holder.tvDuration.setText(exercise.getDuration() + " min");
        holder.imgExercise.setImageResource(exercise.getImageRes());

        holder.btnStart.setOnClickListener(v -> {
            SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            String userGoal = prefs.getString("user_goal", "Lose Weight");

            Intent intent = new Intent(context, WorkoutActivity.class);
            intent.putExtra("USER_GOAL", userGoal);  // pass user goal
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return exerciseList.size();
    }

    public static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        ImageView imgExercise;
        TextView tvName, tvSets, tvDuration;
        Button btnStart;

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            imgExercise = itemView.findViewById(R.id.img_exercise);
            tvName = itemView.findViewById(R.id.tv_exercise_name);
            tvSets = itemView.findViewById(R.id.tv_exercise_sets);
            tvDuration = itemView.findViewById(R.id.tv_exercise_duration);
            btnStart = itemView.findViewById(R.id.btn_start_exercise);
        }
    }
}
