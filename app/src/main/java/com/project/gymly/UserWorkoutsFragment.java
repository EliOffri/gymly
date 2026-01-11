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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.gymly.adapters.UserWorkoutsAdapter;
import com.project.gymly.models.WorkoutDay;

import java.util.ArrayList;
import java.util.List;

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

        db.collection("weeklyPlans").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        fullWorkoutList = parseWorkoutPlan(documentSnapshot);
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
        String[] dayKeys = {"sun", "mon", "tue", "wed", "thu", "fri", "sat"};
        String[] dayDisplayNames = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

        for (int i = 0; i < dayKeys.length; i++) {
            List<String> exercises = (List<String>) document.get(dayKeys[i]);
            if (exercises != null && !exercises.isEmpty()) {
                days.add(new WorkoutDay(dayDisplayNames[i], exercises));
            }
        }
        return days;
    }
}
