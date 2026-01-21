package com.project.gymly;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.gymly.models.Plan;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PlanFragment extends Fragment {

    private static final String TAG = "PlanFragment";
    private FirebaseFirestore db;
    private String userId;

    private TextView tvPhaseLabel, tvWeekLabel, tvCountdown, tvProgressPercent;
    private LinearProgressIndicator planProgressBar;
    private GridLayout heatmapGrid;
    private LinearLayout phasesContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_plan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        userId = user.getUid();

        initViews(view);
        fetchActivePlan();
        fetchHeatmapData();
    }

    private void initViews(View view) {
        tvPhaseLabel = view.findViewById(R.id.tv_phase_label);
        tvWeekLabel = view.findViewById(R.id.tv_week_label);
        tvCountdown = view.findViewById(R.id.tv_countdown);
        tvProgressPercent = view.findViewById(R.id.tv_progress_percent);
        planProgressBar = view.findViewById(R.id.plan_progress_bar);
        heatmapGrid = view.findViewById(R.id.heatmap_grid);
        phasesContainer = view.findViewById(R.id.phases_container);
    }

    private void fetchActivePlan() {
        db.collection("users").document(userId)
                .collection("plans")
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Plan activePlan = queryDocumentSnapshots.getDocuments().get(0).toObject(Plan.class);
                        if (activePlan != null) {
                            updatePlanUI(activePlan);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching plan", e));
    }

    private void updatePlanUI(Plan plan) {
        // 1. Progress Logic
        if (plan.getTotalSessions() > 0) {
            int progress = (plan.getCompletedSessions() * 100) / plan.getTotalSessions();
            planProgressBar.setProgress(progress);
            tvProgressPercent.setText(progress + "%");
        }

        // 2. Timeline Logic (Calculated from Start Date)
        if (plan.getStartDate() != null) {
            Date start = plan.getStartDate().toDate();
            long diff = new Date().getTime() - start.getTime();
            long daysPassed = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
            
            int currentWeek = (int) (daysPassed / 7) + 1;
            tvWeekLabel.setText("Week " + currentWeek + " of " + plan.getDurationWeeks());

            long daysRemaining = (plan.getDurationWeeks() * 7L) - daysPassed;
            tvCountdown.setText(Math.max(0, daysRemaining) + " Days Remaining");
        }

        // 3. Phases Logic
        if (plan.getPhases() != null) {
            phasesContainer.removeAllViews();
            for (int i = 0; i < plan.getPhases().size(); i++) {
                Map<String, Object> phaseData = plan.getPhases().get(i);
                addPhaseCard(phaseData, i + 1);
            }
        }
    }

    private void addPhaseCard(Map<String, Object> data, int phaseNumber) {
        View card = getLayoutInflater().inflate(R.layout.item_phase_card, phasesContainer, false);
        
        TextView title = card.findViewById(R.id.tv_phase_title);
        TextView weeks = card.findViewById(R.id.tv_phase_weeks);
        TextView status = card.findViewById(R.id.tv_phase_status);

        String phaseTitle = (String) data.get("title");
        String statusText = (String) data.get("status");

        title.setText("Phase " + phaseNumber + ": " + phaseTitle);
        weeks.setText((String) data.get("weeks"));
        status.setText(statusText);

        if ("IN PROGRESS".equals(statusText)) {
            tvPhaseLabel.setText("Currently in: " + phaseTitle);
        } else if ("LOCKED".equals(statusText)) {
            card.setAlpha(0.5f);
        }

        phasesContainer.addView(card);
    }

    private void fetchHeatmapData() {
        db.collection("completedWorkouts").document(userId).collection("logs")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<Integer> completedIndices = new HashSet<>();
                    Date today = new Date();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Timestamp ts = doc.getTimestamp("date");
                        if (ts != null) {
                            long diff = today.getTime() - ts.toDate().getTime();
                            int daysAgo = (int) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
                            if (daysAgo >= 0 && daysAgo < 56) completedIndices.add(55 - daysAgo);
                        }
                    }
                    populateHeatmap(completedIndices);
                });
    }

    private void populateHeatmap(Set<Integer> completedIndices) {
        heatmapGrid.removeAllViews();
        int size = (int) (12 * getResources().getDisplayMetrics().density);
        int m = (int) (4 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < 56; i++) {
            View cell = new View(getContext());
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = size; p.height = size;
            p.setMargins(m, m, m, m);
            cell.setLayoutParams(p);
            cell.setBackgroundColor(completedIndices.contains(i) ? 
                Color.parseColor("#6366F1") : Color.parseColor("#E2E8F0"));
            heatmapGrid.addView(cell);
        }
    }
}
