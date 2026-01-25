package com.project.gymly;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TodayFragment extends Fragment {

    private static final String TAG = "TodayFragment";

    private LinearLayout calendarStrip;
    private TextView tvMainTitle;
    private TextView tvSubtitle;
    private TextView tvWeekHeader;
    private View workoutSection;
    
    private FirebaseFirestore db;
    private String userId;
    private String userName = "Athlete";
    
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
        tvWeekHeader = view.findViewById(R.id.tv_week_header);
        workoutSection = view.findViewById(R.id.workout_section);

        setupUser();
        setupCalendar();
    }

    private void setupUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            Log.d(TAG, "User ID set: " + userId);
            db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    userName = documentSnapshot.getString("name");
                    if (userName == null || userName.isEmpty()) userName = "Athlete";
                    
                    if (isAdded()) {
                        // Set initial greeting
                        tvMainTitle.setText("Rest up, " + userName);
                    }
                }
            });
        } else {
            Log.e(TAG, "No user logged in!");
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
                Log.d(TAG, "Calendar day clicked: " + dayKey);
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
                        
                        Object scheduleObj = planDoc.get("schedule");
                        if (scheduleObj instanceof Map) {
                            Map<String, Object> schedule = (Map<String, Object>) scheduleObj;
                            Object weekObj = schedule.get(String.valueOf(currentWeek));
                            if (weekObj instanceof Map) {
                                Map<String, Object> weekData = (Map<String, Object>) weekObj;
                                if (weekData.get(dayKey) instanceof Map) {
                                    dot.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981"))); 
                                    dot.setVisibility(View.VISIBLE);
                                    return;
                                }
                            }
                        }
                    }
                    dot.setVisibility(View.INVISIBLE);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching day status", e);
                    dot.setVisibility(View.INVISIBLE);
                });
    }

    private void fetchWorkoutForDay(String dayKey) {
        if (userId == null) {
            Log.e(TAG, "userId is null in fetchWorkoutForDay");
            return;
        }
        
        Log.d(TAG, "Fetching workout for day: " + dayKey);
        db.collection("users").document(userId).collection("plans")
                .whereEqualTo("isActive", true)
                .limit(5) // Fetch a few to find the latest manually if needed, or just limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Sort manually by startDate descending to avoid index requirement
                        List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                        docs.sort((d1, d2) -> {
                            Timestamp t1 = d1.getTimestamp("startDate");
                            Timestamp t2 = d2.getTimestamp("startDate");
                            if (t1 == null || t2 == null) return 0;
                            return t2.compareTo(t1);
                        });
                        
                        DocumentSnapshot planDoc = docs.get(0);
                        int currentWeek = calculateCurrentWeek(planDoc);
                        long totalWeeks = planDoc.getLong("durationWeeks") != null ? planDoc.getLong("durationWeeks") : 0;

                        Log.d(TAG, "Plan found ID: " + planDoc.getId() + ". Week: " + currentWeek + "/" + totalWeeks);

                        if (tvWeekHeader != null) {
                            tvWeekHeader.setText("Week " + currentWeek + "/" + totalWeeks);
                        }

                        Object scheduleObj = planDoc.get("schedule");
                        if (scheduleObj instanceof Map) {
                            Map<String, Object> schedule = (Map<String, Object>) scheduleObj;
                            Object weekObj = schedule.get(String.valueOf(currentWeek));
                            
                            if (weekObj instanceof Map) {
                                Map<String, Object> weekData = (Map<String, Object>) weekObj;
                                Object workoutObj = weekData.get(dayKey);
                                
                                if (workoutObj instanceof Map) {
                                    Log.d(TAG, "Workout found for " + dayKey);
                                    Map<String, Object> workout = (Map<String, Object>) workoutObj;
                                    String name = (String) workout.get("name");
                                    Long duration = (Long) workout.get("duration");
                                    List<String> exercises = (List<String>) workout.get("exercises");

                                    if (isAdded()) {
                                        tvMainTitle.setText(name != null ? name : "Today's Workout");
                                        tvSubtitle.setText(duration != null ? duration + " min • Tap to view details" : "Tap to view details");
                                        workoutSection.setVisibility(View.VISIBLE);
                                        
                                        workoutSection.setOnClickListener(v -> {
                                            if (getActivity() instanceof MainActivity) {
                                                ((MainActivity) getActivity()).navigateToWorkoutDetail(
                                                        name, 
                                                        duration != null ? duration.intValue() : 0, 
                                                        exercises != null ? new ArrayList<>(exercises) : new ArrayList<>()
                                                );
                                            }
                                        });
                                    }
                                    return;
                                } else {
                                    Log.d(TAG, "No workout object for dayKey: " + dayKey);
                                }
                            } else {
                                Log.d(TAG, "No weekData for currentWeek: " + currentWeek + ". Keys: " + ((Map)schedule).keySet());
                            }
                        } else {
                            Log.d(TAG, "schedule is not a Map or is missing");
                        }
                    } else {
                        Log.d(TAG, "No active plan found for user: " + userId);
                    }
                    showRestDayUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching plan query", e);
                    showRestDayUI();
                });
    }

    private int calculateCurrentWeek(DocumentSnapshot planDoc) {
        Timestamp startDate = planDoc.getTimestamp("startDate");
        if (startDate == null) {
            Log.d(TAG, "startDate is null, defaulting to Week 1");
            return 1;
        }
        
        long diffInMs = System.currentTimeMillis() - startDate.toDate().getTime();
        Log.d(TAG, "Diff in MS: " + diffInMs);
        
        int week = (int) (diffInMs / (7L * 24 * 60 * 60 * 1000)) + 1;
        
        Long duration = planDoc.getLong("durationWeeks");
        if (duration != null && week > duration) return duration.intValue();
        if (week < 1) return 1;
        
        Log.d(TAG, "Calculated current week: " + week);
        return week;
    }

    private void showRestDayUI() {
        if (!isAdded()) return;
        Log.d(TAG, "Showing Rest Day UI");
        workoutSection.setVisibility(View.GONE);
        tvMainTitle.setText("Rest up, " + userName);
        tvSubtitle.setText("Recovery fuels success, so enjoy the gift of a rest day!");
    }
}
