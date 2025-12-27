package com.project.gymly;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class LibraryFragment extends Fragment {

    private RecyclerView recyclerView;
    private ExerciseAdapter adapter;
    private List<Exercise> exerciseList = new ArrayList<>();
    private FirebaseFirestore db;

    public LibraryFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recycler_exercises);

        if (recyclerView == null) {
            // This prevents the crash. If you see this Toast, your ID in XML is wrong.
            Toast.makeText(getContext(), "Error: View not found!", Toast.LENGTH_SHORT).show();
            return view;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ExerciseAdapter(exerciseList);
        recyclerView.setAdapter(adapter);

        fetchExercises();

        return view;
    }

    private void fetchExercises() {
        db.collection("exerciseLibrary")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    exerciseList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Exercise exercise = document.toObject(Exercise.class);
                        exerciseList.add(exercise);
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("GymlyError", "Firestore failed", e);
                });
    }
}