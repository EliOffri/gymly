package com.project.gymly;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.gymly.adapters.ExerciseAdapter;
import com.project.gymly.models.Exercise;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TodayFragment extends Fragment {

    private LinearLayout calendarStrip;
    private TextView tvMainTitle;
    private TextView tvSubtitle;
    private TextView tvWeekHeader; // For "Week X/Y"
    private RecyclerView rvTodayWorkout;
    private View workoutSection;
    private ExerciseAdapter exerciseAdapter;
    private List<Exercise> todayExercises = new ArrayList<>();
    
    private FirebaseFirestore db;
    private String userId;
    
    // To track selected day
    private int selectedDayIndex = -1; 

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_today, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        calendarStrip = view.findViewById(R.id.calendar_strip);
        tvMainTitle = view.findViewById(R.id.tv_main_title);
        tvSubtitle = view.findViewById(R.id.tv_subtitle);
        tvWeekHeader = view.findViewById(R.id.tv_week_header); // This might be null if ID not in XML yet
        rvTodayWorkout = view.findViewById(R.id.rv_today_workout);
        workoutSection = view.findViewById(R.id.workout_section);

        setupRecyclerView();
        setupUser();
        setupCalendar();
    }

    private void setupRecyclerView() {
        exerciseAdapter = new ExerciseAdapter(todayExercises, exercise -> {
            Toast.makeText(getContext(), exercise.getName(), Toast.LENGTH_SHORT).show();
        });
        rvTodayWorkout.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTodayWorkout.setAdapter(exerciseAdapter);
    }

    private void setupUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String name = documentSnapshot.getString("name");
                    if (name != null && !name.isEmpty()) {
                        tvMainTitle.setText("Rest up, " + name);
                    } else {
                        tvMainTitle.setText("Rest up, Athlete");
                    }
                }
            });
        }
    }

    private void setupCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        
        Calendar today = Calendar.getInstance();
        int todayIndex = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7; 
        selectedDayIndex = todayIndex;

        for (int i = 0; i < calendarStrip.getChildCount(); i++) {
            final int index = i;
            View dayView = calendarStrip.getChildAt(i);
            TextView tvName = dayView.findViewById(R.id.tv_day_name);
            TextView tvNumber = dayView.findViewById(R.id.tv_day_number);
            View dot = dayView.findViewById(R.id.status_dot);

            Date date = cal.getTime();
            String dayKey = new SimpleDateFormat("EEE", Locale.ENGLISH).format(date).toLowerCase();
            
            tvName.setText(new SimpleDateFormat("EEE", Locale.getDefault()).format(date).toUpperCase());
            tvNumber.setText(new SimpleDateFormat("d", Locale.getDefault()).format(date));

            updateDaySelection(dayView, i == selectedDayIndex);
            fetchDayStatus(dayKey, dot);

            dayView.setOnClickListener(v -> {
                updateSelectionUI(index);
                fetchWorkoutForDay(dayKey);
            });

            if (i == selectedDayIndex) {
                fetchWorkoutForDay(dayKey);
            }

            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void updateSelectionUI(int newIndex) {
        if (selectedDayIndex != -1) {
            View oldView = calendarStrip.getChildAt(selectedDayIndex);
            updateDaySelection(oldView, false);
        }
        selectedDayIndex = newIndex;
        View newView = calendarStrip.getChildAt(selectedDayIndex);
        updateDaySelection(newView, true);
    }

    private void updateDaySelection(View view, boolean isSelected) {
        TextView tvNumber = view.findViewById(R.id.tv_day_number);
        if (isSelected) {
            tvNumber.setBackgroundResource(R.drawable.circle_white_bg);
            tvNumber.setTextColor(getResources().getColor(android.R.color.black, null));
        } else {
            tvNumber.setBackground(null);
            tvNumber.setTextColor(getResources().getColor(android.R.color.white, null));
        }
    }

    private void fetchDayStatus(String dayKey, View dot) {
        if (userId == null) return;
        
        db.collection("users").document(userId).collection("plans")
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot planDoc = queryDocumentSnapshots.getDocuments().get(0);
                        int currentWeek = calculateCurrentWeek(planDoc);
                        
                        Map<String, Object> schedule = (Map<String, Object>) planDoc.get("schedule");
                        if (schedule != null && schedule.containsKey(String.valueOf(currentWeek))) {
                            Map<String, Object> weekData = (Map<String, Object>) schedule.get(String.valueOf(currentWeek));
                            List<String> exerciseIds = (List<String>) weekData.get(dayKey);
                            if (exerciseIds != null && !exerciseIds.isEmpty()) {
                                dot.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981"))); 
                                dot.setVisibility(View.VISIBLE);
                            } else {
                                dot.setVisibility(View.INVISIBLE);
                            }
                        }
                    } else {
                        dot.setVisibility(View.INVISIBLE);
                    }
                });
    }

    private void fetchWorkoutForDay(String dayKey) {
        if (userId == null) return;
        
        todayExercises.clear();
        exerciseAdapter.notifyDataSetChanged();
        
        db.collection("users").document(userId).collection("plans")
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot planDoc = queryDocumentSnapshots.getDocuments().get(0);
                        int currentWeek = calculateCurrentWeek(planDoc);
                        long totalWeeks = planDoc.getLong("durationWeeks") != null ? planDoc.getLong("durationWeeks") : 0;

                        if (tvWeekHeader != null) {
                            tvWeekHeader.setText("Week " + currentWeek + "/" + totalWeeks);
                        }

                        Map<String, Object> schedule = (Map<String, Object>) planDoc.get("schedule");
                        if (schedule != null && schedule.containsKey(String.valueOf(currentWeek))) {
                            Map<String, Object> weekData = (Map<String, Object>) schedule.get(String.valueOf(currentWeek));
                            List<String> exerciseIds = (List<String>) weekData.get(dayKey);
                            
                            if (exerciseIds != null && !exerciseIds.isEmpty()) {
                                tvMainTitle.setText("Let's crush it!");
                                tvSubtitle.setText("Day " + dayKey.toUpperCase() + " • Week " + currentWeek);
                                workoutSection.setVisibility(View.VISIBLE);
                                
                                for (String exerciseId : exerciseIds) {
                                    db.collection("exercises").document(exerciseId).get().addOnSuccessListener(exerciseDoc -> {
                                        if (exerciseDoc.exists()) {
                                            Exercise exercise = exerciseDoc.toObject(Exercise.class);
                                            if (exercise != null) {
                                                todayExercises.add(exercise);
                                                exerciseAdapter.notifyItemInserted(todayExercises.size() - 1);
                                            }
                                        }
                                    });
                                }
                            } else {
                                showRestDayUI();
                            }
                        } else {
                            showRestDayUI();
                        }
                    } else {
                        showRestDayUI();
                    }
                });
    }

    private int calculateCurrentWeek(DocumentSnapshot planDoc) {
        Timestamp startDate = planDoc.getTimestamp("startDate");
        if (startDate == null) return 1;
        
        long diffInMs = System.currentTimeMillis() - startDate.toDate().getTime();
        int week = (int) (diffInMs / (7L * 24 * 60 * 60 * 1000)) + 1;
        
        Long duration = planDoc.getLong("durationWeeks");
        if (duration != null && week > duration) return duration.intValue();
        if (week < 1) return 1;
        
        return week;
    }

    private void showRestDayUI() {
        workoutSection.setVisibility(View.GONE);
        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            String name = userDoc.getString("name");
            tvMainTitle.setText("Rest up, " + (name != null ? name : "Athlete"));
            tvSubtitle.setText("Recovery fuels success, so enjoy the gift of a rest day!");
        });
    }
}
