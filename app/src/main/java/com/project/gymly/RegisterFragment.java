package com.project.gymly;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private static final String TAG = "RegisterFragment";

    private TextInputEditText etName, etEmail;
    private Spinner spinnerLevel;
    
    // Goals CheckBoxes
    private CheckBox cbGoalWeightLoss, cbGoalStrength, cbGoalFlexibility, cbGoalTone, cbGoalGeneralFitness;
    
    // Equipment CheckBoxes
    private CheckBox cbEqDumbbells, cbEqBench, cbEqKettlebell, cbEqMat, cbEqTreadmill, cbEqResistanceBand;
    
    // Schedule EditTexts
    private EditText etSun, etMon, etTue, etWed, etThu, etFri, etSat;

    private Button btnSubmit;
    private FirebaseFirestore db;

    public RegisterFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        initViews(view);
        setupSpinner();

        btnSubmit.setOnClickListener(v -> submitRegistration());
    }

    private void initViews(View view) {
        etName = view.findViewById(R.id.et_name);
        etEmail = view.findViewById(R.id.et_email);
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

        // Schedule
        etSun = view.findViewById(R.id.et_sun);
        etMon = view.findViewById(R.id.et_mon);
        etTue = view.findViewById(R.id.et_tue);
        etWed = view.findViewById(R.id.et_wed);
        etThu = view.findViewById(R.id.et_thu);
        etFri = view.findViewById(R.id.et_fri);
        etSat = view.findViewById(R.id.et_sat);

        btnSubmit = view.findViewById(R.id.btn_submit);
    }

    private void setupSpinner() {
        String[] levels = new String[]{"beginner", "intermediate", "advanced"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, levels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLevel.setAdapter(adapter);
    }

    private void submitRegistration() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String level = spinnerLevel.getSelectedItem().toString();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(getContext(), "Please enter name and email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if email already exists
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "A user with this email already exists.", Toast.LENGTH_SHORT).show();
                    } else {
                        // Email is unique, proceed with registration
                        registerUser(name, email, level);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error checking email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void registerUser(String name, String email, String level) {
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

        // Collect Schedule
        Map<String, Integer> schedule = new HashMap<>();
        schedule.put("sun", parseScheduleInput(etSun));
        schedule.put("mon", parseScheduleInput(etMon));
        schedule.put("tue", parseScheduleInput(etTue));
        schedule.put("wed", parseScheduleInput(etWed));
        schedule.put("thu", parseScheduleInput(etThu));
        schedule.put("fri", parseScheduleInput(etFri));
        schedule.put("sat", parseScheduleInput(etSat));

        // Create User Map
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("level", level);
        user.put("goals", goals);
        user.put("equipment", equipment);
        user.put("schedule", schedule);
        user.put("createdAt", FieldValue.serverTimestamp());

        // Add to Firestore
        db.collection("users")
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "User registered with ID: " + documentReference.getId(), Toast.LENGTH_SHORT).show();
                    // Optional: Navigate away or clear form
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error registering user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error adding document", e);
                });
    }

    private int parseScheduleInput(EditText editText) {
        String text = editText.getText().toString().trim();
        if (text.isEmpty()) return 0;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}