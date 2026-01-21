package com.project.gymly;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FieldValue;
import com.project.gymly.data.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private static final String TAG = "RegisterFragment";

    private TextInputLayout tilName, tilEmail, tilPassword;
    private TextInputEditText etName, etEmail, etPassword;
    private Spinner spinnerLevel;
    private ChipGroup chipGroupGoals, chipGroupEquipment;
    private ProgressBar progressBar;
    private Button btnSubmit;

    private final Map<String, Slider> daySliders = new HashMap<>();
    private UserRepository userRepository;

    public RegisterFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userRepository = UserRepository.getInstance();

        initViews(view);
        setupSpinner();
        setupScheduleSliders(view);

        btnSubmit.setOnClickListener(v -> submitRegistration());
    }

    private void initViews(View view) {
        tilName = view.findViewById(R.id.til_name);
        etName = view.findViewById(R.id.et_name);
        tilEmail = view.findViewById(R.id.til_email);
        etEmail = view.findViewById(R.id.et_email);
        tilPassword = view.findViewById(R.id.til_password);
        etPassword = view.findViewById(R.id.et_password);
        spinnerLevel = view.findViewById(R.id.spinner_level);
        progressBar = view.findViewById(R.id.progress_bar);
        chipGroupGoals = view.findViewById(R.id.chip_group_goals);
        chipGroupEquipment = view.findViewById(R.id.chip_group_equipment);
        btnSubmit = view.findViewById(R.id.btn_register_submit);
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

    private void setupScheduleSliders(View view) {
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        String[] keys = {"sun", "mon", "tue", "wed", "thu", "fri", "sat"};
        int[] layoutIds = {
                R.id.layout_sun, R.id.layout_mon, R.id.layout_tue,
                R.id.layout_wed, R.id.layout_thu, R.id.layout_fri, R.id.layout_sat
        };

        for (int i = 0; i < days.length; i++) {
            View layout = view.findViewById(layoutIds[i]);
            TextView tvName = layout.findViewById(R.id.tv_day_name);
            TextView tvValue = layout.findViewById(R.id.tv_day_value);
            Slider slider = layout.findViewById(R.id.slider_day);

            tvName.setText(days[i]);
            slider.addOnChangeListener((s, value, fromUser) -> tvValue.setText((int) value + " min"));
            
            daySliders.put(keys[i], slider);
        }
    }

    private void submitRegistration() {
        // Reset errors
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String level = spinnerLevel.getSelectedItem().toString();

        boolean isValid = true;

        if (name.isEmpty()) {
            tilName.setError("Please enter your name");
            isValid = false;
        }

        if (email.isEmpty()) {
            tilEmail.setError("Please enter your email");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            isValid = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError("Please enter a password");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password should be at least 6 characters");
            isValid = false;
        }

        if (spinnerLevel.getSelectedItemPosition() == 0) {
            Toast.makeText(getContext(), "Please select your fitness level", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (!isValid) return;

        setLoading(true);

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("level", level);
        userData.put("goals", getSelectedChips(chipGroupGoals));
        userData.put("equipment", getSelectedChips(chipGroupEquipment));
        userData.put("schedule", getScheduleData());
        userData.put("createdAt", FieldValue.serverTimestamp());

        userRepository.registerUser(email, password, userData, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(Task<AuthResult> task) {
                setLoading(false);
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.showBottomNav(true);
                    mainActivity.setSelectedNavItem(R.id.nav_home);
                    
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new HomeFragment())
                            .commit();
                }
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                handleRegistrationError(e);
            }
        });
    }

    private void handleRegistrationError(Exception e) {
        if (e instanceof FirebaseNetworkException) {
            Toast.makeText(getContext(), "Network error. Please check your internet connection.", Toast.LENGTH_LONG).show();
        } else if (e instanceof FirebaseAuthUserCollisionException) {
            tilEmail.setError("This email is already registered.");
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            tilPassword.setError("The password is too weak.");
        } else {
            Toast.makeText(getContext(), "Registration failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
        Log.e(TAG, "Registration error", e);
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!isLoading);
        etName.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
    }

    private List<String> getSelectedChips(ChipGroup chipGroup) {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            if (chip.isChecked()) {
                selected.add(chip.getText().toString());
            }
        }
        return selected;
    }

    private Map<String, Integer> getScheduleData() {
        Map<String, Integer> schedule = new HashMap<>();
        for (Map.Entry<String, Slider> entry : daySliders.entrySet()) {
            schedule.put(entry.getKey(), (int) entry.getValue().getValue());
        }
        return schedule;
    }
}
