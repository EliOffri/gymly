package com.project.gymly.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.gymly.R;
import com.project.gymly.models.WorkoutDay;

import java.util.List;

public class UserWorkoutsAdapter extends RecyclerView.Adapter<UserWorkoutsAdapter.WorkoutViewHolder> {

    private final List<WorkoutDay> workoutDays;

    public UserWorkoutsAdapter(List<WorkoutDay> workoutDays) {
        this.workoutDays = workoutDays;
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout_day, parent, false);
        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        WorkoutDay workoutDay = workoutDays.get(position);
        holder.bind(workoutDay);
    }

    @Override
    public int getItemCount() {
        return workoutDays.size();
    }

    static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDayName;
        private final TextView tvExercisesList;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayName = itemView.findViewById(R.id.tv_day_name);
            tvExercisesList = itemView.findViewById(R.id.tv_exercises_list);
        }

        public void bind(WorkoutDay workoutDay) {
            tvDayName.setText(capitalize(workoutDay.getDayName()));
            List<String> exercises = workoutDay.getExercises();
            if (exercises == null || exercises.isEmpty()) {
                tvExercisesList.setText("Rest Day");
            } else {
                StringBuilder sb = new StringBuilder();
                for (String ex : exercises) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append("• ").append(ex.replace("_", " "));
                }
                tvExercisesList.setText(sb.toString());
            }
        }
        
        private String capitalize(String str) {
            if (str == null || str.isEmpty()) return str;
            return str.substring(0, 1).toUpperCase() + str.substring(1);
        }
    }
}