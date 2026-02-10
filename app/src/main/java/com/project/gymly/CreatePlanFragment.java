package com.project.gymly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.project.gymly.data.PlanGenerator;
import com.project.gymly.data.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreatePlanFragment extends Fragment {

    private TextInputEditText etPlanName;
    private ChipGroup cgGoal, cgLevel, cgDays;
    private Slider sliderDuration;
    private Button btnCreate;
    private ProgressBar progressBar;
    private NestedScrollView nsvContent;
    private UserRepository userRepository;

    public CreatePlanFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_plan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userRepository = UserRepository.getInstance();

        nsvContent = view.findViewById(R.id.nsv_content);
        etPlanName = view.findViewById(R.id.et_plan_name);
        cgGoal = view.findViewById(R.id.cg_goal);
        cgLevel = view.findViewById(R.id.cg_level);
        cgDays = view.findViewById(R.id.cg_days);
        sliderDuration = view.findViewById(R.id.slider_duration);
        btnCreate = view.findViewById(R.id.btn_create_plan);
        progressBar = view.findViewById(R.id.progressBar);

        btnCreate.setOnClickListener(v -> generateAndSavePlan());
    }

    private void generateAndSavePlan() {
        String planName = etPlanName.getText().toString().trim();
        if (planName.isEmpty()) planName = "My Custom Plan";

        String goal = getSelectedChipText(cgGoal);
        String level = getSelectedChipText(cgLevel);
        List<String> selectedDays = getSelectedDays();
        int durationWeeks = (int) sliderDuration.getValue();

        if (goal == null) {
            Toast.makeText(getContext(), "Please select a goal", Toast.LENGTH_SHORT).show();
            return;
        }
        if (level == null) {
            Toast.makeText(getContext(), "Please select a level", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedDays.isEmpty()) {
            Toast.makeText(getContext(), "Please select at least one workout day", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        Map<String, Object> planData = PlanGenerator.generate(planName, goal, level, durationWeeks, selectedDays);
        String userId = FirebaseAuth.getInstance().getUid();

        if (userId != null) {
            userRepository.createCustomPlan(userId, planData, new UserRepository.UpdateCallback() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) return;
                    setLoading(false);
                    Toast.makeText(getContext(), "Plan created successfully!", Toast.LENGTH_SHORT).show();
                    
                    // Notify listeners that plan has been updated
                    getParentFragmentManager().setFragmentResult("plan_updated", new Bundle());
                    
                    if (getActivity() instanceof MainActivity) {
                        // Switch to Today tab cleanly
                        ((MainActivity) getActivity()).setSelectedNavItem(R.id.nav_home);
                    }
                    // Remove this fragment from backstack to reveal TodayFragment
                    getParentFragmentManager().popBackStack();
                }

                @Override
                public void onError(Exception e) {
                    if (!isAdded()) return;
                    setLoading(false);
                    Toast.makeText(getContext(), "Failed to create plan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String getSelectedChipText(ChipGroup chipGroup) {
        int id = chipGroup.getCheckedChipId();
        if (id != -1) {
            Chip chip = chipGroup.findViewById(id);
            return chip.getText().toString();
        }
        return null;
    }

    private List<String> getSelectedDays() {
        List<String> days = new ArrayList<>();
        if (isChecked(R.id.chip_mon)) days.add("mon");
        if (isChecked(R.id.chip_tue)) days.add("tue");
        if (isChecked(R.id.chip_wed)) days.add("wed");
        if (isChecked(R.id.chip_thu)) days.add("thu");
        if (isChecked(R.id.chip_fri)) days.add("fri");
        if (isChecked(R.id.chip_sat)) days.add("sat");
        if (isChecked(R.id.chip_sun)) days.add("sun");
        return days;
    }

    private boolean isChecked(int id) {
        Chip chip = cgDays.findViewById(id);
        return chip != null && chip.isChecked();
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (nsvContent != null) nsvContent.setVisibility(loading ? View.GONE : View.VISIBLE);
    }
}
