package com.project.gymly;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.gymly.data.UserRepository;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlanFragment extends Fragment {

    private MaterialCardView cardActivePlan, cardNoPlan;
    private TextView tvPlanTitle, tvPlanSubtitle, tvPlanDuration, tvPlanProgress, tvPlanSchedule;
    private ProgressBar pbPlanProgress, loadingPlan;
    private MaterialButton btnDeletePlan;

    private FirebaseFirestore db;
    private UserRepository userRepository;
    private String currentPlanId;
    private String userId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Listen for plan updates (e.g. creation from CreatePlanFragment)
        getParentFragmentManager().setFragmentResultListener("plan_updated", this, (requestKey, result) -> {
            fetchActivePlan();
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        db = FirebaseFirestore.getInstance();
        userRepository = UserRepository.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        cardActivePlan = view.findViewById(R.id.card_active_plan);
        cardNoPlan = view.findViewById(R.id.card_no_plan);
        tvPlanTitle = view.findViewById(R.id.tv_plan_title);
        tvPlanSubtitle = view.findViewById(R.id.tv_plan_subtitle);
        tvPlanDuration = view.findViewById(R.id.tv_plan_duration);
        tvPlanProgress = view.findViewById(R.id.tv_plan_progress);
        pbPlanProgress = view.findViewById(R.id.pb_plan_progress);
        tvPlanSchedule = view.findViewById(R.id.tv_plan_schedule);
        btnDeletePlan = view.findViewById(R.id.btn_delete_plan);
        loadingPlan = view.findViewById(R.id.loading_plan);

        // Initially hide content
        cardActivePlan.setVisibility(View.GONE);
        cardNoPlan.setVisibility(View.GONE);
        btnDeletePlan.setVisibility(View.GONE);
        loadingPlan.setVisibility(View.VISIBLE);

        btnDeletePlan.setOnClickListener(v -> deleteActivePlan());
        cardNoPlan.setOnClickListener(v -> navigateToCreatePlan());

        fetchActivePlan();
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchActivePlan();
    }

    private void fetchActivePlan() {
        if (userId == null) {
            showEmptyState();
            return;
        }

        loadingPlan.setVisibility(View.VISIBLE);
        
        db.collection("users").document(userId).collection("plans")
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (isAdded()) {
                        loadingPlan.setVisibility(View.GONE);
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                            currentPlanId = doc.getId();
                            displayPlan(doc);
                        } else {
                            showEmptyState();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        loadingPlan.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Error fetching plan", Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    }
                });
    }

    private void displayPlan(DocumentSnapshot doc) {
        cardActivePlan.setVisibility(View.VISIBLE);
        cardNoPlan.setVisibility(View.GONE);
        btnDeletePlan.setVisibility(View.VISIBLE);

        String name = doc.getString("name");
        String goal = doc.getString("goal");
        String level = doc.getString("level");
        Long duration = doc.getLong("durationWeeks");
        Timestamp startDate = doc.getTimestamp("startDate");
        List<String> days = (List<String>) doc.get("days");

        tvPlanTitle.setText(name != null ? name : "My Plan");
        
        String subtitle = "";
        if (goal != null) subtitle += goal;
        if (level != null) subtitle += (subtitle.isEmpty() ? "" : " • ") + level;
        tvPlanSubtitle.setText(subtitle);

        int totalWeeks = (duration != null) ? duration.intValue() : 4;
        tvPlanDuration.setText(totalWeeks + " Weeks");

        int currentWeek = 1;
        if (startDate != null) {
            long diffInMs = new Date().getTime() - startDate.toDate().getTime();
            currentWeek = (int) (diffInMs / (7L * 24 * 60 * 60 * 1000)) + 1;
            if (currentWeek < 1) currentWeek = 1;
            if (currentWeek > totalWeeks) currentWeek = totalWeeks;
        }
        tvPlanProgress.setText("Week " + currentWeek);
        
        int progress = (int) (((float) currentWeek / totalWeeks) * 100);
        pbPlanProgress.setProgress(progress);

        if (days != null && !days.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String day : days) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(capitalize(day));
            }
            tvPlanSchedule.setText(sb.toString());
        } else {
            tvPlanSchedule.setText("Flexible");
        }
    }

    private void showEmptyState() {
        if (!isAdded()) return;
        loadingPlan.setVisibility(View.GONE);
        cardActivePlan.setVisibility(View.GONE);
        btnDeletePlan.setVisibility(View.GONE);
        
        // Show "Create Plan" card instead of simple text
        cardNoPlan.setVisibility(View.VISIBLE);
    }

    private void deleteActivePlan() {
        if (currentPlanId == null || userId == null) return;
        
        loadingPlan.setVisibility(View.VISIBLE);
        cardActivePlan.setVisibility(View.GONE);
        btnDeletePlan.setVisibility(View.GONE);
        cardNoPlan.setVisibility(View.GONE);

        userRepository.deletePlan(userId, currentPlanId, new UserRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Plan ended successfully", Toast.LENGTH_SHORT).show();
                    currentPlanId = null;
                    showEmptyState();
                    
                    // Notify other fragments (like TodayFragment)
                    getParentFragmentManager().setFragmentResult("plan_updated", new Bundle());
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    loadingPlan.setVisibility(View.GONE);
                    // Restore view state on error
                    cardActivePlan.setVisibility(View.VISIBLE);
                    btnDeletePlan.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), "Failed to delete plan", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void navigateToCreatePlan() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.plan_container, new CreatePlanFragment())
                .addToBackStack(null)
                .commit();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
