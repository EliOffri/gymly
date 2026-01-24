package com.project.gymly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.gymly.data.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanFragment extends Fragment {

    private TextInputLayout tilPlanGoal, tilExercisesToAvoid, tilWeight, tilHeight;
    private TextInputEditText etPlanGoal, etExercisesToAvoid, etWeight, etHeight;
    private Spinner spinnerLevel;
    private ChipGroup chipGroupGoals, chipGroupEquipment;
    private RadioGroup rgLength;
    private ProgressBar progressBar;
    private MaterialButton btnSavePlan;

    private final Map<String, Slider> daySliders = new HashMap<>();

    private FirebaseFirestore firestore;
    private UserRepository userRepository;

    public PlanFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = FirebaseFirestore.getInstance();
        userRepository = UserRepository.getInstance();

        initViews(view);
        setupLevelSpinner();
        setupScheduleSliders(view);

        btnSavePlan.setOnClickListener(v -> savePlan());
    }

    private void initViews(View view) {
        tilPlanGoal = view.findViewById(R.id.til_plan_goal);
        tilExercisesToAvoid = view.findViewById(R.id.til_exercises_to_avoid);
        tilWeight = view.findViewById(R.id.til_weight);
        tilHeight = view.findViewById(R.id.til_height);

        etPlanGoal = view.findViewById(R.id.et_plan_goal);
        etExercisesToAvoid = view.findViewById(R.id.et_exercises_to_avoid);
        etWeight = view.findViewById(R.id.et_weight);
        etHeight = view.findViewById(R.id.et_height);

        spinnerLevel = view.findViewById(R.id.spinner_level);
        chipGroupGoals = view.findViewById(R.id.chip_group_goals);
        chipGroupEquipment = view.findViewById(R.id.chip_group_equipment);
        rgLength = view.findViewById(R.id.rg_length);

        progressBar = view.findViewById(R.id.progress_bar_plan);
        btnSavePlan = view.findViewById(R.id.btn_save_plan);
    }

    private void setupLevelSpinner() {
        String[] levels = new String[]{
                "Select level",
                "Beginner",
                "Intermediate",
                "Advanced"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                levels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLevel.setAdapter(adapter);
    }

    private void setupScheduleSliders(View root) {
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        String[] keys = {"sun", "mon", "tue", "wed", "thu", "fri", "sat"};
        int[] layoutIds = {
                R.id.layout_sun_plan,
                R.id.layout_mon_plan,
                R.id.layout_tue_plan,
                R.id.layout_wed_plan,
                R.id.layout_thu_plan,
                R.id.layout_fri_plan,
                R.id.layout_sat_plan
        };

        for (int i = 0; i < days.length; i++) {
            View dayLayout = root.findViewById(layoutIds[i]);
            TextView tvName = dayLayout.findViewById(R.id.tv_day_name);
            TextView tvValue = dayLayout.findViewById(R.id.tv_day_value);
            Slider slider = dayLayout.findViewById(R.id.slider_day);

            tvName.setText(days[i]);
            slider.addOnChangeListener((s, value, fromUser) ->
                    tvValue.setText((int) value + " min"));

            daySliders.put(keys[i], slider);
        }
    }

    private void savePlan() {
        tilPlanGoal.setError(null);
        tilWeight.setError(null);
        tilHeight.setError(null);

        String level = spinnerLevel.getSelectedItem().toString();
        String planGoal = getTextOrEmpty(etPlanGoal);
        String exercisesToAvoid = getTextOrEmpty(etExercisesToAvoid);
        String weightStr = getTextOrEmpty(etWeight);
        String heightStr = getTextOrEmpty(etHeight);

        boolean isValid = true;

        if (spinnerLevel.getSelectedItemPosition() == 0) {
            Toast.makeText(getContext(), "Please select fitness level", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (planGoal.isEmpty()) {
            tilPlanGoal.setError("Plan goal is required");
            isValid = false;
        }

        double weight = 0;
        double height = 0;

        if (!weightStr.isEmpty()) {
            try {
                weight = Double.parseDouble(weightStr);
            } catch (NumberFormatException e) {
                tilWeight.setError("Invalid weight");
                isValid = false;
            }
        }

        if (!heightStr.isEmpty()) {
            try {
                height = Double.parseDouble(heightStr);
            } catch (NumberFormatException e) {
                tilHeight.setError("Invalid height");
                isValid = false;
            }
        }

        if (!isValid) {
            return;
        }

        if (userRepository.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        int selectedLengthId = rgLength.getCheckedRadioButtonId();
        int lengthMonths = (selectedLengthId == R.id.rb_length_2_months) ? 2 : 1;

        Map<String, Object> plan = new HashMap<>();
        plan.put("ownerId", userRepository.getCurrentUser().getUid());
        plan.put("level", level);
        plan.put("goals", getSelectedChips(chipGroupGoals));
        plan.put("equipment", getSelectedChips(chipGroupEquipment));
        plan.put("schedule", getScheduleData());
        plan.put("planGoal", planGoal);
        plan.put("exercisesToAvoid", exercisesToAvoid);
        plan.put("weight", weight);
        plan.put("height", height);
        plan.put("lengthMonths", lengthMonths);
        plan.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("plans")
                .add(plan)
                .addOnSuccessListener(ref -> {
                    setLoading(false);
                    Toast.makeText(getContext(), "Plan saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(
                            getContext(),
                            "Failed to save plan: " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSavePlan.setEnabled(!loading);
    }

    private String getTextOrEmpty(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private List<String> getSelectedChips(ChipGroup group) {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < group.getChildCount(); i++) {
            Chip chip = (Chip) group.getChildAt(i);
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
