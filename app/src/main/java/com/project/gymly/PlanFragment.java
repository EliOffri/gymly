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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
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
        if (user != null) {
            userId = user.getUid();
        } else {
            return;
        }

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
                        DocumentSnapshot planDoc = queryDocumentSnapshots.getDocuments().get(0);
                        updatePlanUI(planDoc);
                    } else {
                        Log.d(TAG, "No active plan found");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching plan", e));
    }

    private void updatePlanUI(DocumentSnapshot doc) {
        String title = doc.getString("title");
        Timestamp startDateTs = doc.getTimestamp("startDate");
        Long durationWeeks = doc.getLong("durationWeeks");
        Long totalSessions = doc.getLong("totalSessions");
        Long completedSessions = doc.getLong("completedSessions");
        List<Map<String, Object>> phases = (List<Map<String, Object>>) doc.get("phases");

        if (totalSessions != null && completedSessions != null && totalSessions > 0) {
            int progress = (int) ((completedSessions * 100) / totalSessions);
            planProgressBar.setProgress(progress);
            tvProgressPercent.setText(progress + "%");
        }

        if (startDateTs != null && durationWeeks != null) {
            Date startDate = startDateTs.toDate();
            Date today = new Date();
            long diffInMillis = today.getTime() - startDate.getTime();
            long daysPassed = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
            int currentWeek = (int) (daysPassed / 7) + 1;
            
            tvWeekLabel.setText("Week " + currentWeek + " of " + durationWeeks);

            long totalDays = durationWeeks * 7;
            long daysRemaining = totalDays - daysPassed;
            tvCountdown.setText(Math.max(0, daysRemaining) + " Days Remaining");
        }

        if (phases != null) {
            phasesContainer.removeAllViews();
            for (int i = 0; i < phases.size(); i++) {
                Map<String, Object> phase = phases.get(i);
                addPhaseCard(phase);
                if ("IN PROGRESS".equals(phase.get("status"))) {
                    tvPhaseLabel.setText("Phase " + (i + 1) + ": " + phase.get("title"));
                }
            }
        }
    }

    private void addPhaseCard(Map<String, Object> phase) {
        View phaseCard = getLayoutInflater().inflate(R.layout.item_phase_card, phasesContainer, false);
        
        TextView title = phaseCard.findViewById(R.id.tv_phase_title);
        TextView weeks = phaseCard.findViewById(R.id.tv_phase_weeks);
        TextView objective = phaseCard.findViewById(R.id.tv_phase_objective);
        TextView composition = phaseCard.findViewById(R.id.tv_phase_composition);
        TextView status = phaseCard.findViewById(R.id.tv_phase_status);

        title.setText((String) phase.get("title"));
        weeks.setText((String) phase.get("weeks"));
        objective.setText((String) phase.get("objective"));
        composition.setText((String) phase.get("composition"));
        
        String statusText = (String) phase.get("status");
        status.setText(statusText);

        if ("LOCKED".equals(statusText)) {
            phaseCard.setAlpha(0.5f);
        } else if ("COMPLETED".equals(statusText)) {
            status.setBackgroundColor(Color.parseColor("#4CAF50"));
            status.setTextColor(Color.WHITE);
        }

        phasesContainer.addView(phaseCard);
    }

    private void fetchHeatmapData() {
        db.collection("completedWorkouts").document(userId)
                .collection("logs")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<Integer> completedDays = new HashSet<>();
                    Calendar cal = Calendar.getInstance();
                    Date today = new Date();
                    
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Timestamp ts = doc.getTimestamp("date");
                        if (ts != null) {
                            Date date = ts.toDate();
                            long diff = today.getTime() - date.getTime();
                            int daysAgo = (int) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
                            if (daysAgo >= 0 && daysAgo < 56) {
                                completedDays.add(55 - daysAgo);
                            }
                        }
                    }
                    populateHeatmap(completedDays);
                });
    }

    private void populateHeatmap(Set<Integer> completedDays) {
        heatmapGrid.removeAllViews();
        int cellSize = (int) (12 * getResources().getDisplayMetrics().density);
        int margin = (int) (4 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < 56; i++) {
            View cell = new View(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cellSize;
            params.height = cellSize;
            params.setMargins(margin, margin, margin, margin);
            cell.setLayoutParams(params);

            if (completedDays.contains(i)) {
                cell.setBackgroundColor(Color.parseColor("#6366F1"));
            } else {
                cell.setBackgroundColor(Color.parseColor("#E2E8F0"));
            }
            heatmapGrid.addView(cell);
        }
    }
}
