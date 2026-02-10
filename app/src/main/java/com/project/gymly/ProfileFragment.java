package com.project.gymly;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.project.gymly.data.UserRepository;

import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileStats_Debug";
    private TextView tvName, tvEmail, tvLevel, tvNotLoggedIn, tvInitial, tvWorkoutCount;
    private LinearLayout llProfileDetails;
    private ProgressBar progressBar;
    private MaterialButton btnEditProfile, btnLogout;
    private UserRepository userRepository;
    private FirebaseFirestore db;
    private ListenerRegistration statsListener;

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

        userRepository = UserRepository.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews(view);

        btnEditProfile.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.profile_container, new EditProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Create Plan button removed from here. It is now only accessible from TodayFragment if no plan exists.

        btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logout();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser currentUser = userRepository.getCurrentUser();
        if (currentUser != null) {
            fetchUserData(currentUser.getUid());
            startStatsListener(currentUser.getUid()); 
        } else {
            showNotLoggedInState();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (statsListener != null) {
            statsListener.remove();
            statsListener = null;
        }
    }

    private void initViews(View view) {
        tvName = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        tvLevel = view.findViewById(R.id.tv_profile_level);
        tvInitial = view.findViewById(R.id.tv_profile_initial);
        tvWorkoutCount = view.findViewById(R.id.tv_workout_count); 
        
        tvNotLoggedIn = view.findViewById(R.id.tv_not_logged_in);
        llProfileDetails = view.findViewById(R.id.ll_profile_details);
        progressBar = view.findViewById(R.id.progressBarProfile);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void fetchUserData(String uid) {
        if (tvName.getText().toString().equals("User Name")) {
            progressBar.setVisibility(View.VISIBLE);
            llProfileDetails.setVisibility(View.GONE);
        }

        userRepository.getUser(uid, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (isAdded()) {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        populateUserData(documentSnapshot);
                        llProfileDetails.setVisibility(View.VISIBLE);
                    } else {
                        showNotLoggedInState();
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    private void startStatsListener(String uid) {
        if (statsListener != null) return; 

        statsListener = db.collection("users").document(uid).collection("plans")
            .whereEqualTo("isActive", true)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    Log.e(TAG, "Listen failed.", e);
                    return;
                }

                if (snapshots != null && !snapshots.isEmpty()) {
                    List<DocumentSnapshot> docs = snapshots.getDocuments();
                    
                    docs.sort((d1, d2) -> {
                        Timestamp t1 = d1.getTimestamp("startDate");
                        Timestamp t2 = d2.getTimestamp("startDate");
                        if (t1 == null || t2 == null) return 0;
                        return t2.compareTo(t1);
                    });

                    DocumentSnapshot planDoc = docs.get(0);
                    int completedCount = countCompleted(planDoc);
                    if (tvWorkoutCount != null) {
                        tvWorkoutCount.setText(String.valueOf(completedCount));
                    }
                } else {
                    if (tvWorkoutCount != null) tvWorkoutCount.setText("0");
                }
            });
    }

    private int countCompleted(DocumentSnapshot doc) {
        int count = 0;
        Object scheduleObj = doc.get("schedule");
        
        if (scheduleObj instanceof Map) {
            Map<String, Object> schedule = (Map<String, Object>) scheduleObj;
            
            for (Map.Entry<String, Object> weekEntry : schedule.entrySet()) {
                Object weekVal = weekEntry.getValue();
                
                if (weekVal instanceof Map) {
                    Map<String, Object> weekMap = (Map<String, Object>) weekVal;
                    
                    for (Map.Entry<String, Object> dayEntry : weekMap.entrySet()) {
                        Object dayVal = dayEntry.getValue();
                        
                        if (dayVal instanceof Map) {
                            Map<String, Object> workout = (Map<String, Object>) dayVal;
                            if (Boolean.TRUE.equals(workout.get("isCompleted"))) {
                                count++;
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    private void populateUserData(DocumentSnapshot doc) {
        String name = doc.getString("name");
        String email = doc.getString("email");
        String level = doc.getString("level");

        tvName.setText(name != null ? name : "User");
        tvEmail.setText(email != null ? email : "");
        tvLevel.setText(level != null ? level : "Beginner");
        
        if (name != null && !name.isEmpty()) {
            tvInitial.setText(name.substring(0, 1).toUpperCase());
        }
    }

    private void showNotLoggedInState() {
        llProfileDetails.setVisibility(View.GONE);
        tvNotLoggedIn.setVisibility(View.VISIBLE);
    }
}
