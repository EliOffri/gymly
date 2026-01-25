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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TodayFragment extends Fragment {

    private LinearLayout calendarStrip;
    private TextView tvMainTitle;
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

        setupUser();
        setupCalendar();
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

            // Set Initial Selection State
            updateDaySelection(dayView, i == selectedDayIndex);

            // Fetch status dot color
            fetchDayStatus(dayKey, dot);

            // Add Click Listener
            dayView.setOnClickListener(v -> {
                updateSelectionUI(index);
                fetchWorkoutForDay(dayKey);
            });

            // If it's today, fetch initial data
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
        db.collection("weeklyPlans").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                List<String> exercises = (List<String>) doc.get(dayKey);
                if (exercises != null && !exercises.isEmpty()) {
                    dot.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981"))); 
                    dot.setVisibility(View.VISIBLE);
                } else {
                    dot.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void fetchWorkoutForDay(String dayKey) {
        if (userId == null) return;
        db.collection("weeklyPlans").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                List<String> exercises = (List<String>) doc.get(dayKey);
                if (exercises != null && !exercises.isEmpty()) {
                    tvMainTitle.setText("Let's crush it!");
                } else {
                    db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
                        String name = userDoc.getString("name");
                        tvMainTitle.setText("Rest up, " + (name != null ? name : "Athlete"));
                    });
                }
            }
        });
    }
}
