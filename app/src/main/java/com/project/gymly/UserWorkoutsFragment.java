package com.project.gymly;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.project.gymly.adapters.UserWorkoutsAdapter;
import com.project.gymly.models.WorkoutDay;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class UserWorkoutsFragment extends Fragment {

    private static final String TAG = "WorkoutsFragment_Debug";
    private RecyclerView rvUserWorkouts;
    private ProgressBar progressBar;
    private TextView tvEmptyPlan;
    private EditText etSearch;
    private FirebaseFirestore db;
    
    private List<WorkoutDay> fullWorkoutList = new ArrayList<>();
    private UserWorkoutsAdapter adapter;

    public UserWorkoutsFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_workouts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        rvUserWorkouts = view.findViewById(R.id.rv_user_workouts);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyPlan = view.findViewById(R.id.tv_empty_plan);
        etSearch = view.findViewById(R.id.et_search_workouts);

        rvUserWorkouts.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserWorkoutsAdapter(new ArrayList<>());
        rvUserWorkouts.setAdapter(adapter);

        setupSearch();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            fetchUserWorkouts(user.getUid());
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterWorkouts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterWorkouts(String query) {
        List<WorkoutDay> filteredList = new ArrayList<>();
        for (WorkoutDay day : fullWorkoutList) {
            boolean matchesDay = day.getDayName().toLowerCase().contains(query.toLowerCase());
            boolean matchesExercise = false;
            
            for (String exercise : day.getExercises()) {
                if (exercise.toLowerCase().contains(query.toLowerCase())) {
                    matchesExercise = true;
                    break;
                }
            }

            if (matchesDay || matchesExercise) {
                filteredList.add(day);
            }
        }

        adapter.updateData(filteredList);
        tvEmptyPlan.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void fetchUserWorkouts(String uid) {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(uid).collection("plans")
                .whereEqualTo("isActive", true)
                .orderBy("startDate", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot planDoc = queryDocumentSnapshots.getDocuments().get(0);
                        fullWorkoutList = parseWorkoutPlan(planDoc);
                        filterWorkouts(etSearch.getText().toString());
                    } else {
                        tvEmptyPlan.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private List<WorkoutDay> parseWorkoutPlan(DocumentSnapshot document) {
        List<WorkoutDay> days = new ArrayList<>();
        
        // Calculate current week
        Timestamp start = document.getTimestamp("startDate");
        int currentWeek = 1;
        if (start != null) {
            long diffInMs = System.currentTimeMillis() - start.toDate().getTime();
            currentWeek = (int) (diffInMs / (7L * 24 * 60 * 60 * 1000)) + 1;
        }

        Map<String, Object> schedule = (Map<String, Object>) document.get("schedule");
        if (schedule == null) return days;

        Map<String, Object> weekData = (Map<String, Object>) schedule.get(String.valueOf(currentWeek));
        if (weekData == null) return days;

        String[] dayKeys = {"sun", "mon", "tue", "wed", "thu", "fri", "sat"};
        String[] dayDisplayNames = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

        for (int i = 0; i < dayKeys.length; i++) {
            Object workoutObj = weekData.get(dayKeys[i]);
            if (workoutObj instanceof Map) {
                Map<String, Object> workout = (Map<String, Object>) workoutObj;
                List<Map<String, Object>> exercisesData = (List<Map<String, Object>>) workout.get("exercises");
                
                if (exercisesData != null) {
                    List<String> exerciseNames = new ArrayList<>();
                    for (Map<String, Object> ex : exercisesData) {
                        // For now we just show IDs or names if we had them. 
                        // The WorkoutDay model uses strings.
                        exerciseNames.add((String) ex.get("exerciseId"));
                    }
                    days.add(new WorkoutDay(dayDisplayNames[i], exerciseNames));
                }
            }
        }
        return days;
    }
}
