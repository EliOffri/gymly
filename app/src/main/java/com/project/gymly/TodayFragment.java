package com.project.gymly;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.gymly.data.UserRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TodayFragment extends Fragment {

    private static final String TAG = "TodayFragment";

    private LinearLayout calendarStrip, llWeekSelector;
    private TextView tvMainTitle, tvSubtitle, tvWeekHeader;
    private TextView tvWorkoutDateDuration, tvWorkoutTitleCard, tvWorkoutSubtitleCard;
    private View workoutSection, restDayContainer;
    private ImageView btnCompleteWorkout; 
    
    private FirebaseFirestore db;
    private String userId;
    private String userName = "Athlete";
    private String currentPlanId; 
    private UserRepository userRepository;
    
    private int selectedDayIndex = -1; 
    private int currentPlanWeek = 1;   
    private int selectedWeek = 1;      
    private int totalPlanWeeks = 4;
    private Timestamp planStartDate;
    
    private Calendar calendarRangeStart;

    private String currentDayKey; 
    private boolean isCurrentWorkoutCompleted = false;
    private boolean hasActivePlan = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getParentFragmentManager().setFragmentResultListener("workout_update", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                boolean completed = result.getBoolean("is_completed");
                String key = result.getString("day_key");
                
                if (key != null && key.equals(currentDayKey)) {
                    isCurrentWorkoutCompleted = completed;
                    updateCompletionUI(completed);
                    updateDotLocally(completed);
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_today, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        userRepository = UserRepository.getInstance();
        
        calendarStrip = view.findViewById(R.id.calendar_strip);
        llWeekSelector = view.findViewById(R.id.ll_week_selector);
        tvMainTitle = view.findViewById(R.id.tv_main_title);
        tvSubtitle = view.findViewById(R.id.tv_subtitle);
        tvWeekHeader = view.findViewById(R.id.tv_week_header);
        
        workoutSection = view.findViewById(R.id.workout_section);
        restDayContainer = view.findViewById(R.id.rest_day_container);
        
        tvWorkoutDateDuration = view.findViewById(R.id.tv_workout_date_duration);
        tvWorkoutTitleCard = view.findViewById(R.id.tv_workout_title_card);
        tvWorkoutSubtitleCard = view.findViewById(R.id.tv_workout_subtitle_card);
        btnCompleteWorkout = view.findViewById(R.id.btn_complete_workout);

        llWeekSelector.setOnClickListener(this::showWeekSelectionMenu);

        setupUser();
        fetchActivePlanInfo(); 
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasActivePlan && currentPlanId != null && calendarRangeStart != null && selectedDayIndex != -1) {
            Calendar cal = (Calendar) calendarRangeStart.clone();
            cal.add(Calendar.DAY_OF_YEAR, selectedDayIndex);
            String dayKey = new SimpleDateFormat("EEE", Locale.ENGLISH).format(cal.getTime()).toLowerCase();
            fetchWorkoutForDay(dayKey);
            
            for (int i = 0; i < 7; i++) {
                Calendar c = (Calendar) calendarRangeStart.clone();
                c.add(Calendar.DAY_OF_YEAR, i);
                String dk = new SimpleDateFormat("EEE", Locale.ENGLISH).format(c.getTime()).toLowerCase();
                View dot = calendarStrip.getChildAt(i).findViewById(R.id.status_dot);
                fetchDayStatus(dk, dot);
            }
        }
    }

    private void setupUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists() && isAdded()) {
                    userName = documentSnapshot.getString("name");
                    if (userName == null || userName.isEmpty()) userName = "Athlete";
                    // Only update if resting, not creating plan
                    if (hasActivePlan) tvMainTitle.setText("Rest up, " + userName);
                }
            });
        }
    }

    private void fetchActivePlanInfo() {
        if (userId == null) return;
        
        db.collection("users").document(userId).collection("plans")
                .whereEqualTo("isActive", true)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        hasActivePlan = true;
                        List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                        docs.sort((d1, d2) -> {
                            Timestamp t1 = d1.getTimestamp("startDate");
                            Timestamp t2 = d2.getTimestamp("startDate");
                            if (t1 == null || t2 == null) return 0;
                            return t2.compareTo(t1);
                        });
                        
                        DocumentSnapshot planDoc = docs.get(0);
                        currentPlanId = planDoc.getId(); 
                        planStartDate = planDoc.getTimestamp("startDate");
                        Long duration = planDoc.getLong("durationWeeks");
                        totalPlanWeeks = (duration != null) ? duration.intValue() : 4;
                        
                        if (calendarRangeStart == null) {
                            currentPlanWeek = calculateWeekFromDate(planStartDate, new Date());
                            selectedWeek = currentPlanWeek;
                        }
                        
                        updateWeekHeaderUI();
                        setupCalendar();
                    } else {
                        hasActivePlan = false;
                        showCreatePlanUI();
                        // Even with no plan, setup generic calendar for visual
                        setupGenericCalendar();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching active plan", e);
                    hasActivePlan = false;
                    showCreatePlanUI();
                    setupGenericCalendar();
                });
    }

    private void showCreatePlanUI() {
        if (!isAdded()) return;
        
        llWeekSelector.setVisibility(View.INVISIBLE);
        workoutSection.setVisibility(View.GONE);
        restDayContainer.setVisibility(View.VISIBLE);
        
        tvMainTitle.setText("Welcome, " + userName);
        tvSubtitle.setText("You don't have an active plan yet. Tap to create one!");
        
        restDayContainer.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.today_container, new CreatePlanFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    // New method for calendar when no plan exists
    private void setupGenericCalendar() {
        Calendar cal = Calendar.getInstance();
        // Snap to this week's Monday
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendarRangeStart = (Calendar) cal.clone();
        
        // Select today
        Calendar today = Calendar.getInstance();
        // Simple day index logic (Mon=0...Sun=6)
        // Check if today is in this week range
        selectedDayIndex = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7;

        renderCalendarStrip();
    }

    private void setupCalendar() {
        if (!hasActivePlan) {
            setupGenericCalendar();
            return;
        }
        
        llWeekSelector.setVisibility(View.VISIBLE);
        
        Calendar cal = Calendar.getInstance();
        if (planStartDate != null) {
            cal.setTime(planStartDate.toDate());
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            cal.add(Calendar.WEEK_OF_YEAR, selectedWeek - 1);
        } else {
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        }
        
        calendarRangeStart = (Calendar) cal.clone();
        
        if (selectedDayIndex == -1) {
            if (selectedWeek == currentPlanWeek) {
                Calendar today = Calendar.getInstance();
                selectedDayIndex = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7; 
            } else {
                selectedDayIndex = 0;
            }
        }

        renderCalendarStrip();
    }

    private void renderCalendarStrip() {
        Calendar cal = (Calendar) calendarRangeStart.clone();

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

            updateDaySelectionUI(dayView, i == selectedDayIndex);
            
            if (hasActivePlan && currentPlanId != null) {
                fetchDayStatus(dayKey, dot);
            } else {
                dot.setVisibility(View.INVISIBLE);
            }

            dayView.setOnClickListener(v -> {
                updateSelectionUI(index);
                selectedDayIndex = index; 
                if (hasActivePlan) fetchWorkoutForDay(dayKey);
            });

            if (i == selectedDayIndex && hasActivePlan) {
                fetchWorkoutForDay(dayKey);
            }

            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void fetchWorkoutForDay(String dayKey) {
        if (userId == null || currentPlanId == null) {
            // No plan? Ensure we show Create Plan UI, not rest UI
            if (!hasActivePlan) {
                showCreatePlanUI();
            }
            return;
        }
        
        currentDayKey = dayKey;

        db.collection("users").document(userId).collection("plans").document(currentPlanId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Object scheduleObj = documentSnapshot.get("schedule");
                        if (scheduleObj instanceof Map) {
                            Map<String, Object> schedule = (Map<String, Object>) scheduleObj;
                            Object weekObj = schedule.get(String.valueOf(selectedWeek));
                            
                            if (weekObj instanceof Map) {
                                Map<String, Object> weekData = (Map<String, Object>) weekObj;
                                Object workoutObj = weekData.get(dayKey);
                                
                                if (workoutObj instanceof Map) {
                                    Map<String, Object> workout = (Map<String, Object>) workoutObj;
                                    String name = (String) workout.get("name");
                                    Object durationObj = workout.get("duration");
                                    long duration = (durationObj instanceof Number) ? ((Number) durationObj).longValue() : 0L;
                                    isCurrentWorkoutCompleted = Boolean.TRUE.equals(workout.get("isCompleted"));
                                    
                                    Object exercisesObj = workout.get("exercises");
                                    if (exercisesObj instanceof List) {
                                        ArrayList<Bundle> stepBundles = new ArrayList<>();
                                        for (Object item : (List<?>) exercisesObj) {
                                            Bundle b = new Bundle();
                                            if (item instanceof Map) {
                                                Map<String, Object> m = (Map<String, Object>) item;
                                                b.putString("exerciseId", (String) m.get("exerciseId"));
                                                b.putLong("sets", m.get("sets") instanceof Number ? ((Number) m.get("sets")).longValue() : 3L);
                                                b.putLong("reps", m.get("reps") instanceof Number ? ((Number) m.get("reps")).longValue() : 12L);
                                            } else if (item instanceof String) {
                                                b.putString("exerciseId", (String) item);
                                            }
                                            stepBundles.add(b);
                                        }
                                        
                                        if (isAdded()) {
                                            displayWorkoutCard(name, duration, stepBundles, isCurrentWorkoutCompleted, dayKey);
                                        }
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    showRestDayUI();
                })
                .addOnFailureListener(e -> showRestDayUI());
    }

    private void displayWorkoutCard(String name, long duration, ArrayList<Bundle> stepBundles, boolean isCompleted, String dayKey) {
        if (workoutSection == null || restDayContainer == null) return;
        
        workoutSection.setVisibility(View.VISIBLE);
        restDayContainer.setVisibility(View.GONE);
        restDayContainer.setOnClickListener(null); 
        
        Calendar cardCal = (Calendar) calendarRangeStart.clone();
        if (selectedDayIndex >= 0) {
            cardCal.add(Calendar.DAY_OF_YEAR, selectedDayIndex);
            String dateString = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(cardCal.getTime());
            tvWorkoutDateDuration.setText(dateString + " · " + (duration != 0 ? duration + "m" : "N/A"));
        }
        
        tvWorkoutTitleCard.setText(name != null ? name : "Workout");
        
        int exerciseCount = stepBundles.size();
        tvWorkoutSubtitleCard.setText("Strength · " + exerciseCount + " exercises");
        
        updateCompletionUI(isCompleted);

        btnCompleteWorkout.setOnClickListener(null);
        if (btnCompleteWorkout != null) {
            btnCompleteWorkout.setOnClickListener(v -> {
                boolean newState = !isCurrentWorkoutCompleted;
                
                updateCompletionUI(newState); 
                isCurrentWorkoutCompleted = newState;
                updateDotLocally(newState);

                userRepository.completeWorkout(userId, currentPlanId, selectedWeek, dayKey, newState, new UserRepository.UpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Workout status saved.");
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(), "Failed to save", Toast.LENGTH_SHORT).show();
                        isCurrentWorkoutCompleted = !newState;
                        updateCompletionUI(isCurrentWorkoutCompleted);
                        updateDotLocally(isCurrentWorkoutCompleted);
                    }
                });
            });
        }
        
        workoutSection.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToWorkoutDetail(
                        name, 
                        (int) duration, 
                        stepBundles,
                        isCurrentWorkoutCompleted, 
                        currentPlanId, 
                        selectedWeek,
                        dayKey
                );
            }
        });
    }

    private void updateDotLocally(boolean isCompleted) {
        if (selectedDayIndex >= 0 && selectedDayIndex < calendarStrip.getChildCount()) {
            View currentDot = calendarStrip.getChildAt(selectedDayIndex).findViewById(R.id.status_dot);
            if (currentDot != null) {
                currentDot.setBackgroundTintList(ColorStateList.valueOf(
                    Color.parseColor(isCompleted ? "#34D399" : "#F59E0B")
                ));
            }
        }
    }

    private void updateCompletionUI(boolean isCompleted) {
        if (btnCompleteWorkout != null) {
            if (isCompleted) {
                btnCompleteWorkout.setImageResource(android.R.drawable.checkbox_on_background);
                btnCompleteWorkout.setColorFilter(Color.parseColor("#34D399")); 
                tvWorkoutTitleCard.setPaintFlags(tvWorkoutTitleCard.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvWorkoutTitleCard.setAlpha(0.5f);
            } else {
                btnCompleteWorkout.setImageResource(android.R.drawable.checkbox_off_background);
                btnCompleteWorkout.setColorFilter(Color.parseColor("#334155")); 
                tvWorkoutTitleCard.setPaintFlags(tvWorkoutTitleCard.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvWorkoutTitleCard.setAlpha(1.0f);
            }
        }
    }

    private void fetchDayStatus(String dayKey, View dot) {
        if (userId == null || dot == null || currentPlanId == null) return;
        
        db.collection("users").document(userId).collection("plans").document(currentPlanId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Object scheduleObj = documentSnapshot.get("schedule");
                        if (scheduleObj instanceof Map) {
                            Map<String, Object> schedule = (Map<String, Object>) scheduleObj;
                            Object weekObj = schedule.get(String.valueOf(selectedWeek));
                            if (weekObj instanceof Map) {
                                Map<String, Object> weekData = (Map<String, Object>) weekObj;
                                Map<String, Object> workout = (Map<String, Object>) weekData.get(dayKey);
                                if (workout != null) {
                                    boolean completed = Boolean.TRUE.equals(workout.get("isCompleted"));
                                    if (completed) {
                                        dot.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#34D399"))); 
                                    } else {
                                        dot.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F59E0B"))); 
                                    }
                                    dot.setVisibility(View.VISIBLE);
                                    return;
                                }
                            }
                        }
                    }
                    dot.setVisibility(View.INVISIBLE);
                });
    }

    private void showWeekSelectionMenu(View v) {
        List<String> weeks = new ArrayList<>();
        for (int i = 1; i <= totalPlanWeeks; i++) {
            weeks.add("Week " + i);
        }
        ListPopupWindow listPopupWindow = new ListPopupWindow(getContext());
        listPopupWindow.setAnchorView(v);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, weeks);
        listPopupWindow.setAdapter(adapter);
        listPopupWindow.setWidth(400); 
        listPopupWindow.setHeight(ListPopupWindow.WRAP_CONTENT);
        listPopupWindow.setModal(true);
        listPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
            selectedWeek = position + 1;
            updateWeekHeaderUI();
            setupCalendar();
            listPopupWindow.dismiss();
        });
        listPopupWindow.show();
    }

    private void updateWeekHeaderUI() {
        if (tvWeekHeader != null) tvWeekHeader.setText("Week " + selectedWeek + "/" + totalPlanWeeks);
    }

    private void updateSelectionUI(int newIndex) {
        if (selectedDayIndex != -1 && selectedDayIndex < calendarStrip.getChildCount()) {
            updateDaySelectionUI(calendarStrip.getChildAt(selectedDayIndex), false);
        }
        selectedDayIndex = newIndex;
        if (selectedDayIndex != -1 && selectedDayIndex < calendarStrip.getChildCount()) {
            updateDaySelectionUI(calendarStrip.getChildAt(selectedDayIndex), true);
        }
    }

    private void updateDaySelectionUI(View view, boolean isSelected) {
        if (view == null) return;
        TextView tvNumber = view.findViewById(R.id.tv_day_number);
        if (tvNumber != null) {
            if (isSelected) {
                tvNumber.setBackgroundResource(R.drawable.circle_white_bg);
                tvNumber.setTextColor(getResources().getColor(android.R.color.black, null));
            } else {
                tvNumber.setBackground(null);
                tvNumber.setTextColor(getResources().getColor(android.R.color.white, null));
            }
        }
    }

    private int calculateWeekFromDate(Timestamp start, Date current) {
        if (start == null) return 1;
        long diffInMs = current.getTime() - start.toDate().getTime();
        int week = (int) (diffInMs / (7L * 24 * 60 * 60 * 1000)) + 1;
        if (week < 1) return 1;
        return week;
    }

    private void showRestDayUI() {
        if (!isAdded() || workoutSection == null || restDayContainer == null) return;
        workoutSection.setVisibility(View.GONE);
        restDayContainer.setVisibility(View.VISIBLE);
        
        if (hasActivePlan) {
            tvMainTitle.setText("Rest up, " + userName);
            tvSubtitle.setText("Recovery fuels success, so enjoy the gift of a rest day!");
            restDayContainer.setOnClickListener(null);
        } else {
            // This fallback shouldn't theoretically happen if handled by showCreatePlanUI
            // But just in case:
            showCreatePlanUI();
        }
    }
}
