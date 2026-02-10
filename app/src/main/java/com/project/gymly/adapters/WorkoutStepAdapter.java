package com.project.gymly.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.project.gymly.R;
import com.project.gymly.models.Exercise;

import java.util.List;
import java.util.Map;

public class WorkoutStepAdapter extends RecyclerView.Adapter<WorkoutStepAdapter.ViewHolder> {

    private List<Map<String, Object>> steps;
    private List<Exercise> fullExercises;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Exercise exercise);
    }

    public WorkoutStepAdapter(List<Map<String, Object>> steps, List<Exercise> fullExercises, OnItemClickListener listener) {
        this.steps = steps;
        this.fullExercises = fullExercises;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout_step, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position >= steps.size() || position >= fullExercises.size()) return;

        Map<String, Object> step = steps.get(position);
        Exercise exercise = fullExercises.get(position);

        holder.tvNumber.setText(String.valueOf(position + 1));
        
        Object setsObj = step.get("sets");
        Object repsObj = step.get("reps");
        
        long sets = 0;
        long reps = 0;
        
        if (setsObj instanceof Number) sets = ((Number) setsObj).longValue();
        if (repsObj instanceof Number) reps = ((Number) repsObj).longValue();
        
        holder.tvDetails.setText(sets + " sets of " + reps + " reps");

        if (exercise != null) {
            holder.tvName.setText(exercise.getName());
            holder.itemView.setAlpha(1.0f);
            holder.itemView.setClickable(true);
            
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(exercise);
            });
        } else {
            holder.tvName.setText("Loading exercise...");
            holder.itemView.setAlpha(0.5f);
            holder.itemView.setClickable(false);
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return steps != null ? steps.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumber, tvName, tvDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tv_step_number);
            tvName = itemView.findViewById(R.id.tv_exercise_name);
            tvDetails = itemView.findViewById(R.id.tv_exercise_details);
        }
    }
}
