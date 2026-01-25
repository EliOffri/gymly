package com.project.gymly;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.gymly.models.Exercise;

public class ExerciseDetailFragment extends Fragment {

    private TextView exerciseNameTextView;
    private ImageView exerciseImageView;
    private TextView exerciseDescriptionTextView;
    private TextView muscleGroupTextView;
    private TextView difficultyTextView;
    private TextView durationTextView;
    private LinearLayout instructionsContainer;
    private TextView exerciseDetailsTextView;

    public ExerciseDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercise_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        exerciseNameTextView = view.findViewById(R.id.exercise_name);
        exerciseImageView = view.findViewById(R.id.exercise_image);
        exerciseDescriptionTextView = view.findViewById(R.id.exercise_description);
        muscleGroupTextView = view.findViewById(R.id.muscle_group_value);
        difficultyTextView = view.findViewById(R.id.difficulty_value);
        durationTextView = view.findViewById(R.id.duration_value);
        instructionsContainer = view.findViewById(R.id.instructions_container);
        exerciseDetailsTextView = view.findViewById(R.id.exercise_details);

        if (getArguments() != null) {
            String exerciseName = getArguments().getString("exerciseName");
            if (exerciseName != null) {
                loadExerciseDetails(exerciseName);
            }
        }
    }

    private void loadExerciseDetails(String exerciseName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("exerciseLibrary").whereEqualTo("name", exerciseName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Exercise exercise = queryDocumentSnapshots.getDocuments().get(0).toObject(Exercise.class);
                        if (exercise != null) {
                            // Set exercise details
                            exerciseNameTextView.setText(exercise.getName());
                            exerciseDescriptionTextView.setText(exercise.getDescription());
                            muscleGroupTextView.setText(exercise.getMuscleGroup());
                            durationTextView.setText(exercise.getDuration() + " seconds");

                            // Set difficulty level
                            String difficultyString;
                            switch (exercise.getDifficulty()) {
                                case 1:
                                    difficultyString = "Beginner";
                                    break;
                                case 2:
                                    difficultyString = "Intermediate";
                                    break;
                                case 3:
                                    difficultyString = "Advanced";
                                    break;
                                default:
                                    difficultyString = "N/A";
                            }
                            difficultyTextView.setText(difficultyString);

                            // Load image with Glide
                            if (!TextUtils.isEmpty(exercise.getImageUrl())) {
                                Glide.with(this)
                                        .load(exercise.getImageUrl())
                                        .into(exerciseImageView);
                            }

                            // Show instructions only if available
                            if (!TextUtils.isEmpty(exercise.getInstructions())) {
                                instructionsContainer.setVisibility(View.VISIBLE);
                                exerciseDetailsTextView.setText(exercise.getInstructions());
                            } else {
                                instructionsContainer.setVisibility(View.GONE);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("GymlyError", "Failed to load exercise details", e);
                });
    }
}
