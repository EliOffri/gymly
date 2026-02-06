package com.project.gymly.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.project.gymly.R;
import com.project.gymly.data.MuscleGroupImageProvider;
import com.project.gymly.models.Exercise;
import java.util.List;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ViewHolder> {

    private List<Exercise> exerciseList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Exercise exercise);
    }

    public ExerciseAdapter(List<Exercise> exerciseList, OnItemClickListener listener) {
        this.exerciseList = exerciseList;
        this.listener = listener;
    }

    public void updateList(List<Exercise> newList) {
        this.exerciseList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise exercise = exerciseList.get(position);

        holder.name.setText(exercise.getName() != null ? exercise.getName() : "Unknown");

        // Set Image based on Muscle Group
        if (exercise.getMuscleGroup() != null) {
            holder.muscles.setText(exercise.getMuscleGroup());
            int iconRes = MuscleGroupImageProvider.getIconResource(exercise.getMuscleGroup());
            holder.image.setImageResource(iconRes);
        } else {
            holder.muscles.setText("General");
            holder.image.setImageResource(R.drawable.ic_gymly_logo);
        }

        // Set Difficulty Badge Appearance
        int difficulty = exercise.getDifficulty();
        String diffText;
        int color;
        int bgTint;

        if (difficulty == 1) {
            diffText = "BEGINNER";
            color = Color.parseColor("#34D399"); // Emerald Green
            bgTint = Color.parseColor("#064E3B"); // Dark Green BG
        } else if (difficulty == 2) {
            diffText = "INTERMEDIATE";
            color = Color.parseColor("#FBBF24"); // Amber
            bgTint = Color.parseColor("#451A03"); // Dark Amber BG
        } else {
            diffText = "ADVANCED";
            color = Color.parseColor("#F87171"); // Red
            bgTint = Color.parseColor("#450A0A"); // Dark Red BG
        }

        holder.difficulty.setText(diffText);
        holder.difficulty.setTextColor(color);
        holder.difficultyCard.setCardBackgroundColor(bgTint);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(exercise);
            }
        });
    }

    @Override
    public int getItemCount() {
        return exerciseList != null ? exerciseList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, muscles, difficulty;
        ImageView image;
        MaterialCardView difficultyCard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_exercise_name);
            muscles = itemView.findViewById(R.id.text_exercise_muscles);
            difficulty = itemView.findViewById(R.id.text_exercise_difficulty);
            difficultyCard = itemView.findViewById(R.id.card_difficulty_badge);
            image = itemView.findViewById(R.id.iv_exercise_image);
        }
    }
}
