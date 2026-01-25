package com.project.gymly;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.gymly.adapters.CalendarAdapter;
import com.project.gymly.models.Plan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements CalendarAdapter.OnDateClickListener {

    private TextView tvMotivation, tvDateHeader, tvWorkoutStatus, tvWorkoutName, tvWorkoutDetails;
    private RecyclerView rvWeekCalendar;
    private MaterialButton btnCreatePlan, btnViewDetails;
    private MaterialCardView cardDailyWorkout;
    private View progressBar;

    private FirebaseFirestore db;
    private String userId;
    private Plan activePlan;
    private List<Date> weekDates = new ArrayList<>();
    private CalendarAdapter calendarAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        initViews(view);
        setupCalendar(); // Set up adapter first
        fetchMotivation();
        fetchActivePlan();
    }

    private void initViews(View view) {
        tvMotivation = view.findViewById(R.id.tv_motivation);
        tvDateHeader = view.findViewById(R.id.tv_date_header);
        tvWorkoutStatus = view.findViewById(R.id.tv_workout_status);
        tvWorkoutName = view.findViewById(R.id.tv_workout_name);
        tvWorkoutDetails = view.findViewById(R.id.tv_workout_details);
        rvWeekCalendar = view.findViewById(R.id.rv_week_calendar);
        btnCreatePlan = view.findViewById(R.id.btn_create_plan_home);
        btnViewDetails = view.findViewById(R.id.btn_start_workout);
        cardDailyWorkout = view.findViewById(R.id.card_daily_workout);
        progressBar = view.findViewById(R.id.progressBar);

        tvDateHeader.setText(new SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(new Date()).toUpperCase());
    }

    private void setupCalendar() {
        // Use GridLayoutManager with 7 columns to fit perfectly in one row
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 7);
        rvWeekCalendar.setLayoutManager(layoutManager);
        
        Calendar cal = Calendar.getInstance();
        // Reset to first day of the current week
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        
        weekDates.clear();
        for (int i = 0; i < 7; i++) {
            weekDates.add(cal.getTime());
            cal.add(Calendar.DAY_OF_WEEK, 1);
        }

        // Pass 'this' as the listener
        calendarAdapter = new CalendarAdapter(weekDates, new Date(), this);
        rvWeekCalendar.setAdapter(calendarAdapter);
        
        // Force refresh
        calendarAdapter.notifyDataSetChanged();
    }

    private void fetchMotivation() {
        db.collection("motivationMessages").limit(1).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                tvMotivation.setText("“" + queryDocumentSnapshots.getDocuments().get(0).getString("text") + "”");
            }
        });
    }

    private void fetchActivePlan() {
        if (userId == null) return;
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(userId).collection("plans")
                .whereEqualTo("isActive", true).limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    if (!queryDocumentSnapshots.isEmpty()) {
                        activePlan = queryDocumentSnapshots.getDocuments().get(0).toObject(Plan.class);
                        btnCreatePlan.setVisibility(View.GONE);
                        updateWorkoutForSelectedDate(new Date());
                    } else {
                        btnCreatePlan.setVisibility(View.VISIBLE);
                        showRecoveryState();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    showRecoveryState();
                });
    }

    private void updateWorkoutForSelectedDate(Date date) {
        tvDateHeader.setText(new SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(date).toUpperCase());

        if (activePlan == null) {
            showRecoveryState();
            return;
        }

        String dayKey = new SimpleDateFormat("EEE", Locale.ENGLISH).format(date).toLowerCase();
        fetchWorkoutFromWeeklyPlan(dayKey);
    }

    private void fetchWorkoutFromWeeklyPlan(String dayKey) {
        db.collection("weeklyPlans").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                List<String> exercises = (List<String>) doc.get(dayKey);
                if (exercises != null && !exercises.isEmpty()) {
                    showWorkoutState(dayKey, exercises);
                } else {
                    showRecoveryState();
                }
            } else {
                showRecoveryState();
            }
        });
    }

    private void showWorkoutState(String day, List<String> exercises) {
        tvWorkoutStatus.setText("TODAY'S GOAL");
        tvWorkoutStatus.setTextColor(Color.parseColor("#6366F1")); 
        tvWorkoutName.setText(day.toUpperCase() + " SESSION");
        tvWorkoutDetails.setText(exercises.size() + " Exercises planned");
        btnViewDetails.setVisibility(View.VISIBLE);
        
        btnViewDetails.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                // Navigate to Workout Details
            }
        });
    }

    private void showRecoveryState() {
        tvWorkoutStatus.setText("RECOVERY TIME");
        tvWorkoutStatus.setTextColor(Color.parseColor("#94A3B8"));
        tvWorkoutName.setText("Rest & Recharge");
        tvWorkoutDetails.setText("No exercises scheduled.");
        btnViewDetails.setVisibility(View.GONE);
    }

    @Override
    public void onDateClick(Date date) {
        updateWorkoutForSelectedDate(date);
    }
}
