package com.project.gymly;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.project.gymly.adapters.ExerciseAdapter;
import com.project.gymly.models.Exercise;

import java.util.ArrayList;
import java.util.List;

public class WorkoutFragment extends Fragment {

    private static final String ARG_NAME = "workout_name";
    private static final String ARG_DURATION = "workout_duration";
    private static final String ARG_EXERCISES = "exercise_ids";

    private String workoutName;
    private int duration;
    private ArrayList<String> exerciseIds;

    private TextView tvTitle, tvSummary;
    private RecyclerView rvExercises;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private ExerciseAdapter adapter;
    private List<Exercise> exerciseList = new ArrayList<>();
    private FirebaseFirestore db;

    public static WorkoutFragment newInstance(String name, int duration, ArrayList<String> exerciseIds) {
        WorkoutFragment fragment = new WorkoutFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putInt(ARG_DURATION, duration);
        args.putStringArrayList(ARG_EXERCISES, exerciseIds);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            workoutName = getArguments().getString(ARG_NAME);
            duration = getArguments().getInt(ARG_DURATION);
            exerciseIds = getArguments().getStringArrayList(ARG_EXERCISES);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_workout_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTitle = view.findViewById(R.id.tv_workout_title);
        tvSummary = view.findViewById(R.id.tv_workout_summary);
        rvExercises = view.findViewById(R.id.rv_workout_exercises);
        progressBar = view.findViewById(R.id.progressBar);
        btnBack = view.findViewById(R.id.btn_back);

        tvTitle.setText(workoutName);
        tvSummary.setText(duration + " min • " + (exerciseIds != null ? exerciseIds.size() : 0) + " Exercises");

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        setupRecyclerView();
        fetchExercises();
    }

    private void setupRecyclerView() {
        adapter = new ExerciseAdapter(exerciseList, exercise -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToExerciseDetail(exercise.getName());
            }
        });
        rvExercises.setLayoutManager(new LinearLayoutManager(getContext()));
        rvExercises.setAdapter(adapter);
    }

    private void fetchExercises() {
        if (exerciseIds == null || exerciseIds.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        exerciseList.clear();
        
        final int total = exerciseIds.size();
        for (String id : exerciseIds) {
            db.collection("exercises").document(id).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Exercise exercise = documentSnapshot.toObject(Exercise.class);
                    if (exercise != null) {
                        exerciseList.add(exercise);
                        adapter.notifyItemInserted(exerciseList.size() - 1);
                    }
                }
                if (exerciseList.size() >= total) {
                    progressBar.setVisibility(View.GONE);
                }
            }).addOnFailureListener(e -> {
                Log.e("WorkoutFragment", "Error fetching exercise", e);
                if (exerciseList.size() >= total) {
                    progressBar.setVisibility(View.GONE);
                }
            });
        }
    }
}
