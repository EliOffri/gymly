package com.project.gymly;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvLevel, tvGoals, tvEquipment, tvNotLoggedIn;
    private LinearLayout llProfileDetails;
    private ProgressBar progressBar;
    private Button btnEditProfile;
    private FirebaseFirestore db;
    private String userId;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        initViews(view);

        // Check if user is logged in
        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        userId = prefs.getString("current_user_id", null);

        if (userId != null) {
            fetchUserData(userId);
        } else {
            showNotLoggedInState();
        }

        btnEditProfile.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new EditProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void initViews(View view) {
        tvName = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        tvLevel = view.findViewById(R.id.tv_profile_level);
        tvGoals = view.findViewById(R.id.tv_profile_goals);
        tvEquipment = view.findViewById(R.id.tv_profile_equipment);
        
        tvNotLoggedIn = view.findViewById(R.id.tv_not_logged_in);
        llProfileDetails = view.findViewById(R.id.ll_profile_details);
        progressBar = view.findViewById(R.id.progressBarProfile);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
    }

    private void fetchUserData(String uid) {
        progressBar.setVisibility(View.VISIBLE);
        llProfileDetails.setVisibility(View.GONE);
        tvNotLoggedIn.setVisibility(View.GONE);

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        populateUserData(documentSnapshot);
                        llProfileDetails.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(getContext(), "User details not found.", Toast.LENGTH_SHORT).show();
                        showNotLoggedInState();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error fetching profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showNotLoggedInState();
                });
    }

    private void populateUserData(DocumentSnapshot doc) {
        String name = doc.getString("name");
        String email = doc.getString("email");
        String level = doc.getString("level");
        
        // Handle Lists safely
        List<String> goalsList = (List<String>) doc.get("goals");
        List<String> equipmentList = (List<String>) doc.get("equipment");

        tvName.setText(name != null ? name : "N/A");
        tvEmail.setText(email != null ? email : "N/A");
        tvLevel.setText(level != null ? level : "N/A");

        if (goalsList != null && !goalsList.isEmpty()) {
            tvGoals.setText(TextUtils.join(", ", goalsList));
        } else {
            tvGoals.setText("None");
        }

        if (equipmentList != null && !equipmentList.isEmpty()) {
            tvEquipment.setText(TextUtils.join(", ", equipmentList));
        } else {
            tvEquipment.setText("None");
        }
    }

    private void showNotLoggedInState() {
        llProfileDetails.setVisibility(View.GONE);
        tvNotLoggedIn.setVisibility(View.VISIBLE);
        // Hide edit button if not logged in
        if (btnEditProfile != null) {
            btnEditProfile.setVisibility(View.GONE);
        }
    }
}