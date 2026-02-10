package com.project.gymly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.project.gymly.data.UserRepository;

import java.util.HashMap;
import java.util.Map;

public class EditProfileFragment extends Fragment {

    private TextInputEditText etName;
    private MaterialAutoCompleteTextView etLevel;
    private Button btnSave;
    private ProgressBar progressBar;
    private UserRepository userRepository;
    private String userId;

    public EditProfileFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userRepository = UserRepository.getInstance();
        etName = view.findViewById(R.id.et_edit_name);
        etLevel = view.findViewById(R.id.et_edit_level);
        btnSave = view.findViewById(R.id.btn_save_profile);
        progressBar = view.findViewById(R.id.progressBar);

        setupLevelDropdown();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            loadUserData();
        }

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void setupLevelDropdown() {
        String[] levels = new String[]{"Beginner", "Intermediate", "Advanced"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, levels);
        etLevel.setAdapter(adapter);
    }

    private void loadUserData() {
        setLoading(true);
        userRepository.getUser(userId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(DocumentSnapshot document) {
                setLoading(false);
                if (document.exists()) {
                    String name = document.getString("name");
                    String level = document.getString("level");
                    if (name != null) etName.setText(name);
                    if (level != null) etLevel.setText(level, false); // false = no filtering
                }
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String level = etLevel.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name required");
            return;
        }
        
        if (level.isEmpty()) {
            etLevel.setError("Level required");
            return;
        }

        setLoading(true);
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("level", level);

        userRepository.updateUserProfile(userId, updates, new UserRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Toast.makeText(getContext(), "Failed to update", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }
}
