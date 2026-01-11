package com.project.gymly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.project.gymly.data.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditProfileFragment extends Fragment {

    private AutoCompleteTextView levelAutoCompleteTextView;
    private ChipGroup chipGroupGoals, chipGroupEquipment;
    private Button btnSaveChanges;
    private UserRepository userRepository;
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

        userRepository = UserRepository.getInstance();

        initViews(view);
        setupLevelDropdown();

        FirebaseUser currentUser = userRepository.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            fetchCurrentData(userId);
        } else {
            Toast.makeText(getContext(), "User not identified", Toast.LENGTH_SHORT).show();
        }

        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void initViews(View view) {
        levelAutoCompleteTextView = view.findViewById(R.id.level_auto_complete_text_view);
        chipGroupGoals = view.findViewById(R.id.chip_group_goals);
        chipGroupEquipment = view.findViewById(R.id.chip_group_equipment);
        btnSaveChanges = view.findViewById(R.id.btn_save_changes);
    }

    private void setupLevelDropdown() {
        String[] levels = new String[]{"Select Level", "Beginner", "Intermediate", "Advanced"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, levels);
        levelAutoCompleteTextView.setAdapter(adapter);
    }

    private void fetchCurrentData(String uid) {
        userRepository.getUser(uid, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    populateFields(documentSnapshot);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateFields(DocumentSnapshot doc) {
        String level = doc.getString("level");
        if (level != null && !level.isEmpty()) {
            levelAutoCompleteTextView.setText(level, false);
        } else {
            levelAutoCompleteTextView.setText("Select Level", false);
        }


        List<String> goals = (List<String>) doc.get("goals");
        if (goals != null) {
            for (int i = 0; i < chipGroupGoals.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupGoals.getChildAt(i);
                if (goals.contains(chip.getText().toString().toLowerCase().replace(" ", "_"))) {
                    chip.setChecked(true);
                }
            }
        }

        List<String> equipment = (List<String>) doc.get("equipment");
        if (equipment != null) {
            for (int i = 0; i < chipGroupEquipment.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupEquipment.getChildAt(i);
                if (equipment.contains(chip.getText().toString().toLowerCase().replace(" ", "_"))) {
                    chip.setChecked(true);
                }
            }
        }
    }

    private void saveChanges() {
        if (userId == null) return;

        String level = levelAutoCompleteTextView.getText().toString();
        if ("Select Level".equals(level)) {
            Toast.makeText(getContext(), "Please select a valid fitness level.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> goals = new ArrayList<>();
        for (int i = 0; i < chipGroupGoals.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupGoals.getChildAt(i);
            if (chip.isChecked()) {
                goals.add(chip.getText().toString().toLowerCase().replace(" ", "_"));
            }
        }

        List<String> equipment = new ArrayList<>();
        for (int i = 0; i < chipGroupEquipment.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupEquipment.getChildAt(i);
            if (chip.isChecked()) {
                equipment.add(chip.getText().toString().toLowerCase().replace(" ", "_"));
            }
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("level", level);
        updates.put("goals", goals);
        updates.put("equipment", equipment);

        userRepository.updateUserProfile(userId, updates, new UserRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
