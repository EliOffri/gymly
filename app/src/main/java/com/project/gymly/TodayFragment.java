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
    private TextView tvMainTitle, tvSubtitle, tvWeekHeader, tvProfileInitial;
    private TextView tvWorkoutDateDuration, tvWorkoutTitleCard, tvWorkoutSubtitleCard;
    private View workoutSection, restDayContainer;
    
    private FirebaseFirestore db;
    private String userId;
    private String userName = "Athlete";
    
    private int selectedDayIndex = -1; 
    private Calendar currentCalendarRangeStart;

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
        tvProfileInitial = view.findViewById(R.id.tv_profile_initial);
        
        workoutSection = view.findViewById(R.id.workout_section);
        restDayContainer = view.findViewById(R.id.rest_day_container);
        
        tvWorkoutDateDuration = view.findViewById(R.id.tv_workout_date_duration);
        tvWorkoutTitleCard = view.findViewById(R.id.tv_workout_title_card);
        tvWorkoutSubtitleCard = view.findViewById(R.id.tv_workout_subtitle_card);

        setupUser();
        setupCalendar();
    }

    private void setupUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    userName = documentSnapshot.getString("name");
                    if (userName == null || userName.isEmpty()) userName = "Athlete";
                    
                    if (isAdded()) {
                        tvMainTitle.setText("Rest up, " + userName);
                        tvProfileInitial.setText(userName.substring(0, 1).toUpperCase());
                    }
                }
            });
        }
    }

    private void setupCalendar() {
        Calendar cal = Calendar.getInstance();
        // Move to Monday of current week
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        currentCalendarRangeStart = (Calendar) cal.clone();
        
        Calendar today = Calendar.getInstance();
        // Calculate index (0 for Mon, 6 for Sun)
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
                });
    }

    private void fetchWorkoutForDay(String dayKey) {
        if (userId == null) return;
        
        db.collection("users").document(userId).collection("plans")
                .whereEqualTo("isActive", true)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
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
                                    Map<String, Object> workout = (Map<String, Object>) workoutObj;
                                    String name = (String) workout.get("name");
                                    Long duration = (Long) workout.get("duration");
                                    List<String> exercises = (List<String>) workout.get("exercises");

                                    if (isAdded()) {
                                        displayWorkoutCard(dayKey, name, duration, exercises);
                                    }
                                    return;
                                }
                            }
                        }
                    }
                    showRestDayUI();
                })
                .addOnFailureListener(e -> showRestDayUI());
    }

    private void displayWorkoutCard(String dayKey, String name, Long duration, List<String> exercises) {
        workoutSection.setVisibility(View.VISIBLE);
        restDayContainer.setVisibility(View.GONE);
        
        // Calculate the date string for the card
        Calendar cardCal = (Calendar) currentCalendarRangeStart.clone();
        cardCal.add(Calendar.DAY_OF_YEAR, selectedDayIndex);
        String dateString = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(cardCal.getTime());
        
        tvWorkoutDateDuration.setText(dateString + " · " + (duration != null ? duration + "m" : "N/A"));
        tvWorkoutTitleCard.setText(name != null ? name : "Today's Workout");
        
        int exerciseCount = exercises != null ? exercises.size() : 0;
        tvWorkoutSubtitleCard.setText("Strength · " + exerciseCount + " exercises");
        
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
        if (!isAdded()) return;
        workoutSection.setVisibility(View.GONE);
        restDayContainer.setVisibility(View.VISIBLE);
        tvMainTitle.setText("Rest up, " + userName);
        tvSubtitle.setText("Recovery fuels success, so enjoy the gift of a rest day!");
    }
}
