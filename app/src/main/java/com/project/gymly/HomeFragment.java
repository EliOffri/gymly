package com.project.gymly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements WeekAdapter.OnDayClickListener {

    private RecyclerView recyclerWeek;
    private TextView tvWorkoutDetails;
    private Button btnCreatePlan;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private WeekAdapter weekAdapter;
    private List<Day> days = new ArrayList<>();
    private DocumentSnapshot plan;
    private int selectedDayPosition = -1;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerWeek = view.findViewById(R.id.recycler_week);
        tvWorkoutDetails = view.findViewById(R.id.tv_workout_details);
        btnCreatePlan = view.findViewById(R.id.btn_create_plan);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupWeekView();
        checkPlanExistence();
    }

    private void setupWeekView() {
        recyclerWeek.setLayoutManager(new GridLayoutManager(getContext(), 7));
        populateWeekDays();
        weekAdapter = new WeekAdapter(days, this);
        recyclerWeek.setAdapter(weekAdapter);
    }

    private void populateWeekDays() {
        days.clear();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        SimpleDateFormat dayNameFormat = new SimpleDateFormat("EEE", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            String dayName = dayNameFormat.format(calendar.getTime());
            int dayNumber = calendar.get(Calendar.DAY_OF_MONTH);
            boolean isToday = isToday(calendar);
            days.add(new Day(dayName, dayNumber, isToday));
            if (isToday) {
                selectedDayPosition = i;
            }
            calendar.add(Calendar.DAY_OF_WEEK, 1);
        }
    }

    private boolean isToday(Calendar calendar) {
        Calendar today = Calendar.getInstance();
        return today.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR) &&
                today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR);
    }

    private void checkPlanExistence() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("plans").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                plan = task.getResult();
                btnCreatePlan.setVisibility(View.GONE);
                if (selectedDayPosition != -1) {
                    onDayClick(selectedDayPosition);
                }
            } else {
                plan = null;
                btnCreatePlan.setVisibility(View.VISIBLE);
                tvWorkoutDetails.setText("Recovery time");
            }
        });
    }

    private void fetchWorkoutForDay(int dayOfWeek) {
        if (plan == null) {
            tvWorkoutDetails.setText("Recovery time");
            return;
        }

        String dayKey = getDayKey(dayOfWeek);
        String workout = plan.getString(dayKey);

        if (workout != null && !workout.isEmpty()) {
            tvWorkoutDetails.setText(workout);
        } else {
            tvWorkoutDetails.setText("Recovery time");
        }
    }

    private String getDayKey(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return "sunday";
            case Calendar.MONDAY:
                return "monday";
            case Calendar.TUESDAY:
                return "tuesday";
            case Calendar.WEDNESDAY:
                return "wednesday";
            case Calendar.THURSDAY:
                return "thursday";
            case Calendar.FRIDAY:
                return "friday";
            case Calendar.SATURDAY:
                return "saturday";
            default:
                return "";
        }
    }

    @Override
    public void onDayClick(int position) {
        this.selectedDayPosition = position;
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.add(Calendar.DAY_OF_WEEK, position);
        fetchWorkoutForDay(calendar.get(Calendar.DAY_OF_WEEK));
    }
}
