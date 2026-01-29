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
import com.project.gymly.adapters.WorkoutStepAdapter;
import com.project.gymly.models.Exercise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkoutFragment extends Fragment {

    private static final String TAG = "WorkoutFragment";
    private static final String ARG_NAME = "workout_name";
    private static final String ARG_DURATION = "workout_duration";
    private static final String ARG_STEPS_BUNDLES = "workout_steps_bundles";

    private String workoutName;
    private int duration;
    private ArrayList<Bundle> stepBundles;

    private TextView tvTitle, tvSummary;
    private RecyclerView rvExercises;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private WorkoutStepAdapter adapter;
    private final List<Exercise> exerciseDetailsList = new ArrayList<>();
    private final List<Map<String, Object>> stepsList = new ArrayList<>();
    private FirebaseFirestore db;

    public static WorkoutFragment newInstance(String name, int duration, ArrayList<Bundle> steps) {
        WorkoutFragment fragment = new WorkoutFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putInt(ARG_DURATION, duration);
        args.putParcelableArrayList(ARG_STEPS_BUNDLES, steps);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            workoutName = getArguments().getString(ARG_NAME);
            duration = getArguments().getInt(ARG_DURATION);
            stepBundles = getArguments().getParcelableArrayList(ARG_STEPS_BUNDLES);
        }
        db = FirebaseFirestore.getInstance();
        
        // Convert Bundles back to Maps for the adapter
        if (stepBundles != null) {
            for (Bundle b : stepBundles) {
                Map<String, Object> map = new HashMap<>();
                map.put("exerciseId", b.getString("exerciseId"));
                map.put("sets", b.getLong("sets"));
                map.put("reps", b.getLong("reps"));
                stepsList.add(map);
                exerciseDetailsList.add(null); // Pre-fill with nulls for loading state
            }
        }
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

        if (tvTitle != null) tvTitle.setText(workoutName != null ? workoutName : "Workout");
        if (tvSummary != null) tvSummary.setText("Strength • " + duration + "m");

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }

        setupRecyclerView();
        fetchExerciseDetails();
    }

    private void setupRecyclerView() {
        if (rvExercises == null) return;
        adapter = new WorkoutStepAdapter(stepsList, exerciseDetailsList, exercise -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToExerciseDetail(exercise.getName());
            }
        });
        rvExercises.setLayoutManager(new LinearLayoutManager(getContext()));
        rvExercises.setAdapter(adapter);
    }

    private void fetchExerciseDetails() {
        if (stepsList.isEmpty()) {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        for (int i = 0; i < stepsList.size(); i++) {
            final int index = i;
            String id = (String) stepsList.get(i).get("exerciseId");
            
            if (id == null) {
                checkLoadComplete();
                continue;
            }
            
            db.collection("exercises").document(id).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Exercise ex = doc.toObject(Exercise.class);
                    if (ex != null && index < exerciseDetailsList.size()) {
                        exerciseDetailsList.set(index, ex);
                        if (adapter != null) adapter.notifyItemChanged(index);
                    }
                }
                checkLoadComplete();
            }).addOnFailureListener(e -> checkLoadComplete());
        }
    }

    private void checkLoadComplete() {
        if (!isAdded()) return;

        int count = 0;
        for (Exercise e : exerciseDetailsList) if (e != null) count++;
        
        if (count >= stepsList.size()) {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
        }
    }
}
