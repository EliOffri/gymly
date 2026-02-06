package com.project.gymly;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.gymly.adapters.WorkoutStepAdapter;
import com.project.gymly.data.UserRepository;
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
    private static final String ARG_IS_COMPLETED = "workout_is_completed";
    
    private static final String ARG_PLAN_ID = "workout_plan_id";
    private static final String ARG_WEEK = "workout_week";
    private static final String ARG_DAY_KEY = "workout_day_key";

    private String workoutName;
    private int duration;
    private boolean isCompleted;
    private String planId;
    private int week;
    private String dayKey;
    private ArrayList<Bundle> stepBundles;

    private TextView tvTitle, tvSummary;
    private RecyclerView rvExercises;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private MaterialButton btnComplete;
    private WorkoutStepAdapter adapter;
    private UserRepository userRepository;
    
    private final List<Map<String, Object>> stepsList = new ArrayList<>();
    private final List<Exercise> exerciseDetailsList = new ArrayList<>();
    private FirebaseFirestore db;

    public static WorkoutFragment newInstance(String name, int duration, ArrayList<Bundle> steps, boolean isCompleted, String planId, int week, String dayKey) {
        WorkoutFragment fragment = new WorkoutFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putInt(ARG_DURATION, duration);
        args.putBoolean(ARG_IS_COMPLETED, isCompleted);
        args.putString(ARG_PLAN_ID, planId);
        args.putInt(ARG_WEEK, week);
        args.putString(ARG_DAY_KEY, dayKey);
        args.putParcelableArrayList(ARG_STEPS_BUNDLES, steps);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        userRepository = UserRepository.getInstance();
        
        if (getArguments() != null) {
            workoutName = getArguments().getString(ARG_NAME);
            duration = getArguments().getInt(ARG_DURATION);
            isCompleted = getArguments().getBoolean(ARG_IS_COMPLETED);
            planId = getArguments().getString(ARG_PLAN_ID);
            week = getArguments().getInt(ARG_WEEK);
            dayKey = getArguments().getString(ARG_DAY_KEY);
            stepBundles = getArguments().getParcelableArrayList(ARG_STEPS_BUNDLES);
            
            if (stepBundles != null) {
                stepsList.clear();
                exerciseDetailsList.clear();
                for (Bundle b : stepBundles) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("exerciseId", b.getString("exerciseId"));
                    map.put("sets", b.getLong("sets", 3));
                    map.put("reps", b.getLong("reps", 12));
                    stepsList.add(map);
                    exerciseDetailsList.add(null);
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
        btnComplete = view.findViewById(R.id.btn_complete_workout);

        if (tvTitle != null) tvTitle.setText(workoutName != null ? workoutName : "Workout");
        if (tvSummary != null) tvSummary.setText("Strength • " + duration + "m");

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }
        
        setupCompletionUI();
        setupRecyclerView();
        fetchExerciseDetails();
    }

    private void setupCompletionUI() {
        if (btnComplete == null) return;

        if (isCompleted) {
            btnComplete.setText("Workout Completed");
            btnComplete.setIconResource(android.R.drawable.checkbox_on_background);
            btnComplete.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#334155"))); 
            btnComplete.setTextColor(Color.parseColor("#94A3B8")); 
            btnComplete.setClickable(false); 
        } else {
            btnComplete.setText("Complete Workout");
            btnComplete.setIconResource(android.R.drawable.checkbox_off_background);
            btnComplete.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#34D399")));
            btnComplete.setTextColor(Color.parseColor("#0F172A"));
            btnComplete.setClickable(true);
            
            btnComplete.setOnClickListener(v -> markAsCompleted());
        }
    }

    private void markAsCompleted() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null || planId == null) {
            Toast.makeText(getContext(), "Error: Plan ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Update UI locally first
        isCompleted = true;
        setupCompletionUI();

        // 2. Set Fragment Result to notify TodayFragment
        Bundle result = new Bundle();
        result.putBoolean("is_completed", true);
        result.putString("day_key", dayKey);
        getParentFragmentManager().setFragmentResult("workout_update", result);

        // 3. Perform Network Update
        userRepository.completeWorkout(userId, planId, week, dayKey, true, new UserRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Great job!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                isCompleted = false; // Revert
                setupCompletionUI();
            }
        });
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
            String id = (String) stepsList.get(index).get("exerciseId");
            
            if (id == null || id.isEmpty()) {
                checkLoadComplete();
                continue;
            }

            db.collection("exercises").document(id).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Exercise ex = doc.toObject(Exercise.class);
                    if (ex != null && isAdded() && index < exerciseDetailsList.size()) {
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
