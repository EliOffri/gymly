package com.project.gymly;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.firestore.FieldValue;
import com.project.gymly.data.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private static final String TAG = "RegisterFragment";
    private static final String USER_REGISTERED_SUCCESSFULLY = "User registered successfully";

    private TextInputEditText etName, etEmail, etPassword;
    private Spinner spinnerLevel;
    private ChipGroup chipGroupGoals, chipGroupEquipment;

    // Schedule EditTexts
    private EditText etSun, etMon, etTue, etWed, etThu, etFri, etSat;

    private Button btnSubmit;
    private UserRepository userRepository;

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

        userRepository = UserRepository.getInstance();

        initViews(view);
        setupSpinner();

        btnSubmit.setOnClickListener(v -> submitRegistration());
    }

    private void initViews(View view) {
        etName = view.findViewById(R.id.et_name);
        etEmail = view.findViewById(R.id.et_email);
        etPassword = view.findViewById(R.id.et_password);
        spinnerLevel = view.findViewById(R.id.spinner_level);

        // Goals
        chipGroupGoals = view.findViewById(R.id.chip_group_goals);

        // Equipment
        chipGroupEquipment = view.findViewById(R.id.chip_group_equipment);

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
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.fitness_levels,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLevel.setAdapter(adapter);
    }

    private void submitRegistration() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String level = spinnerLevel.getSelectedItem().toString();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("Select Level".equals(level)) {
            Toast.makeText(getContext(), "Please select a fitness level.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create User Map
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("level", level);
        user.put("goals", getGoals());
        user.put("equipment", getEquipment());
        user.put("schedule", getSchedule());
        user.put("createdAt", FieldValue.serverTimestamp());

        userRepository.registerUser(email, password, user, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(Task<AuthResult> task) {
                Toast.makeText(getContext(), USER_REGISTERED_SUCCESSFULLY, Toast.LENGTH_SHORT).show();
                // Optional: Navigate away or clear form
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error registering user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error adding document", e);
            }
        });
    }

    private List<String> getGoals() {
        List<String> goals = new ArrayList<>();
        for (int i = 0; i < chipGroupGoals.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupGoals.getChildAt(i);
            if (chip.isChecked()) {
                goals.add(chip.getText().toString().toLowerCase().replace(" ", "_"));
            }
        }
        return goals;
    }

    private List<String> getEquipment() {
        List<String> equipment = new ArrayList<>();
        for (int i = 0; i < chipGroupEquipment.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupEquipment.getChildAt(i);
            if (chip.isChecked()) {
                equipment.add(chip.getText().toString().toLowerCase().replace(" ", "_"));
            }
        }
        return equipment;
    }

    private Map<String, Integer> getSchedule() {
        Map<String, Integer> schedule = new HashMap<>();
        schedule.put("sun", parseScheduleInput(etSun));
        schedule.put("mon", parseScheduleInput(etMon));
        schedule.put("tue", parseScheduleInput(etTue));
        schedule.put("wed", parseScheduleInput(etWed));
        schedule.put("thu", parseScheduleInput(etThu));
        schedule.put("fri", parseScheduleInput(etFri));
        schedule.put("sat", parseScheduleInput(etSat));
        return schedule;
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
