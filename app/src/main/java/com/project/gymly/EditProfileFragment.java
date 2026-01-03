package com.project.gymly;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EditProfileFragment extends Fragment {

    private Spinner spinnerLevel;
    
    // Goals CheckBoxes
    private CheckBox cbGoalWeightLoss, cbGoalStrength, cbGoalFlexibility, cbGoalTone, cbGoalGeneralFitness;
    
    // Equipment CheckBoxes
    private CheckBox cbEqDumbbells, cbEqBench, cbEqKettlebell, cbEqMat, cbEqTreadmill, cbEqResistanceBand;

    private Button btnSaveChanges;
    private FirebaseFirestore db;
    private String userId;

    public EditProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        initViews(view);
        setupSpinner();

        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        userId = prefs.getString("current_user_id", null);

        if (userId != null) {
            fetchCurrentData(userId);
        } else {
            Toast.makeText(getContext(), "User not identified", Toast.LENGTH_SHORT).show();
        }

        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void initViews(View view) {
        spinnerLevel = view.findViewById(R.id.spinner_level);

        // Goals
        cbGoalWeightLoss = view.findViewById(R.id.cb_goal_weight_loss);
        cbGoalStrength = view.findViewById(R.id.cb_goal_strength);
        cbGoalFlexibility = view.findViewById(R.id.cb_goal_flexibility);
        cbGoalTone = view.findViewById(R.id.cb_goal_tone);
        cbGoalGeneralFitness = view.findViewById(R.id.cb_goal_general_fitness);

        // Equipment
        cbEqDumbbells = view.findViewById(R.id.cb_eq_dumbbells);
        cbEqBench = view.findViewById(R.id.cb_eq_bench);
        cbEqKettlebell = view.findViewById(R.id.cb_eq_kettlebell);
        cbEqMat = view.findViewById(R.id.cb_eq_mat);
        cbEqTreadmill = view.findViewById(R.id.cb_eq_treadmill);
        cbEqResistanceBand = view.findViewById(R.id.cb_eq_resistance_band);

        btnSaveChanges = view.findViewById(R.id.btn_save_changes);
    }

    private void setupSpinner() {
        String[] levels = new String[]{"beginner", "intermediate", "advanced"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, levels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLevel.setAdapter(adapter);
    }

    private void fetchCurrentData(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        populateFields(documentSnapshot);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error loading data", Toast.LENGTH_SHORT).show());
    }

    private void populateFields(DocumentSnapshot doc) {
        String level = doc.getString("level");
        if (level != null) {
            ArrayAdapter adapter = (ArrayAdapter) spinnerLevel.getAdapter();
            int position = adapter.getPosition(level);
            if (position >= 0) spinnerLevel.setSelection(position);
        }

        List<String> goals = (List<String>) doc.get("goals");
        if (goals != null) {
            if (goals.contains("weight_loss")) cbGoalWeightLoss.setChecked(true);
            if (goals.contains("strength")) cbGoalStrength.setChecked(true);
            if (goals.contains("flexibility")) cbGoalFlexibility.setChecked(true);
            if (goals.contains("tone")) cbGoalTone.setChecked(true);
            if (goals.contains("general_fitness")) cbGoalGeneralFitness.setChecked(true);
        }

        List<String> equipment = (List<String>) doc.get("equipment");
        if (equipment != null) {
            if (equipment.contains("dumbbells")) cbEqDumbbells.setChecked(true);
            if (equipment.contains("bench")) cbEqBench.setChecked(true);
            if (equipment.contains("kettlebell")) cbEqKettlebell.setChecked(true);
            if (equipment.contains("mat")) cbEqMat.setChecked(true);
            if (equipment.contains("treadmill")) cbEqTreadmill.setChecked(true);
            if (equipment.contains("resistance_band")) cbEqResistanceBand.setChecked(true);
        }
    }

    private void saveChanges() {
        if (userId == null) return;

        String level = spinnerLevel.getSelectedItem().toString();

        // Collect Goals
        List<String> goals = new ArrayList<>();
        if (cbGoalWeightLoss.isChecked()) goals.add("weight_loss");
        if (cbGoalStrength.isChecked()) goals.add("strength");
        if (cbGoalFlexibility.isChecked()) goals.add("flexibility");
        if (cbGoalTone.isChecked()) goals.add("tone");
        if (cbGoalGeneralFitness.isChecked()) goals.add("general_fitness");

        // Collect Equipment
        List<String> equipment = new ArrayList<>();
        if (cbEqDumbbells.isChecked()) equipment.add("dumbbells");
        if (cbEqBench.isChecked()) equipment.add("bench");
        if (cbEqKettlebell.isChecked()) equipment.add("kettlebell");
        if (cbEqMat.isChecked()) equipment.add("mat");
        if (cbEqTreadmill.isChecked()) equipment.add("treadmill");
        if (cbEqResistanceBand.isChecked()) equipment.add("resistance_band");

        db.collection("users").document(userId)
                .update("level", level, "goals", goals, "equipment", equipment)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    // Go back to ProfileFragment
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}