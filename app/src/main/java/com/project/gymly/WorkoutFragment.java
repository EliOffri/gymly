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

    private static final String TAG = "WorkoutFragment_Debug";
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
    
    private final List<Map<String, Object>> stepsList = new ArrayList<>();
    private final List<Exercise> exerciseDetailsList = new ArrayList<>();
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
        db = FirebaseFirestore.getInstance();
        
        if (getArguments() != null) {
            workoutName = getArguments().getString(ARG_NAME);
            duration = getArguments().getInt(ARG_DURATION);
            stepBundles = getArguments().getParcelableArrayList(ARG_STEPS_BUNDLES);
            
            Log.d(TAG, "onCreate: Received " + (stepBundles != null ? stepBundles.size() : 0) + " steps.");

            if (stepBundles != null) {
                stepsList.clear();
                exerciseDetailsList.clear();
                for (Bundle b : stepBundles) {
                    Map<String, Object> map = new HashMap<>();
                    String id = b.getString("exerciseId");
                    map.put("exerciseId", id);
                    map.put("sets", b.getLong("sets", 3));
                    map.put("reps", b.getLong("reps", 12));
                    stepsList.add(map);
                    exerciseDetailsList.add(null);
                    Log.d(TAG, "Queued step for ID: " + id);
                }
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

        tvTitle.setText(workoutName != null ? workoutName : "Workout");
        tvSummary.setText("Strength • " + duration + "m");

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        setupRecyclerView();
        fetchExerciseDetails();
    }

    private void setupRecyclerView() {
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
            Log.w(TAG, "fetchExerciseDetails: stepsList is EMPTY.");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        for (int i = 0; i < stepsList.size(); i++) {
            final int index = i;
            String id = (String) stepsList.get(index).get("exerciseId");
            
            if (id == null || id.isEmpty()) {
                Log.e(TAG, "Fetching: ID is null at index " + index);
                checkLoadComplete();
                continue;
            }

            Log.d(TAG, "Fetching details from DB for ID: " + id);
            db.collection("exercises").document(id).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Exercise ex = doc.toObject(Exercise.class);
                    if (ex != null && isAdded()) {
                        Log.d(TAG, "Success: Fetched " + ex.getName());
                        exerciseDetailsList.set(index, ex);
                        if (adapter != null) adapter.notifyItemChanged(index);
                    }
                } else {
                    Log.e(TAG, "Error: Document " + id + " DOES NOT EXIST in library.");
                }
                checkLoadComplete();
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error: Network failure for " + id, e);
                checkLoadComplete();
            });
        }
    }

    private void checkLoadComplete() {
        if (!isAdded()) return;
        int count = 0;
        for (Exercise e : exerciseDetailsList) if (e != null) count++;
        if (count >= stepsList.size()) {
            progressBar.setVisibility(View.GONE);
            Log.d(TAG, "Done: All details processed.");
        }
    }
}
