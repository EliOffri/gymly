package com.project.gymly;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.gymly.adapters.ExerciseAdapter;
import com.project.gymly.models.Exercise;
import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment implements ExerciseAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private EditText etSearch;
    private ExerciseAdapter adapter;
    private List<Exercise> fullExerciseList = new ArrayList<>();
    private FirebaseFirestore db;

    public LibraryFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recycler_exercises);
        etSearch = view.findViewById(R.id.et_search);

        if (recyclerView == null) {
            Toast.makeText(getContext(), "Error: View not found!", Toast.LENGTH_SHORT).show();
            return view;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Initialize with empty list
        adapter = new ExerciseAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        setupSearch();
        fetchExercises();

        return view;
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterExercises(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterExercises(String query) {
        List<Exercise> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();

        for (Exercise ex : fullExerciseList) {
            boolean matchesName = ex.getName() != null && ex.getName().toLowerCase().contains(lowerCaseQuery);
            boolean matchesMuscle = ex.getMuscleGroup() != null && ex.getMuscleGroup().toLowerCase().contains(lowerCaseQuery);
            
            if (matchesName || matchesMuscle) {
                filteredList.add(ex);
            }
        }
        adapter.updateList(filteredList);
    }

    private void fetchExercises() {
        db.collection("exercises")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    fullExerciseList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Exercise exercise = document.toObject(Exercise.class);
                        fullExerciseList.add(exercise);
                    }
                    // Initial load shows all exercises
                    adapter.updateList(new ArrayList<>(fullExerciseList));
                })
                .addOnFailureListener(e -> {
                    Log.e("GymlyError", "Firestore failed", e);
                });
    }

    @Override
    public void onItemClick(Exercise exercise) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.navigateToExerciseDetail(exercise.getName());
        }
    }
}
