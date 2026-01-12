package com.project.gymly.data.firestore;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public final class FirestoreSeeder {
    private static final String PREFS_NAME = "app_prefs";
    private static final String PREF_KEY_SEED_DONE = "seed_done_v3";

    public static void seedIfNeeded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(PREF_KEY_SEED_DONE, false)) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String[] userIds = {"eliUser", "ofirUser"};

        for (String uid : userIds) {
            seedPersonalizedPlan(db, uid);
        }

        prefs.edit().putBoolean(PREF_KEY_SEED_DONE, true).apply();
    }

    private static void seedPersonalizedPlan(FirebaseFirestore db, String uid) {
        Map<String, Object> plan = new HashMap<>();
        plan.put("title", "8-Week Transformation");
        plan.put("purpose", "Muscle Hypertrophy");
        plan.put("durationWeeks", 8);
        plan.put("isActive", true);
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -20);
        plan.put("startDate", new Timestamp(cal.getTime()));

        plan.put("totalSessions", 32);
        plan.put("completedSessions", 14);

        // Nested Phases for this specific plan
        Map<String, Object> p1 = new HashMap<>();
        p1.put("title", "Foundations");
        p1.put("weeks", "1-4");
        p1.put("objective", "Building basic strength and motor patterns.");
        p1.put("composition", "12 Reps / 60s Rest");
        p1.put("status", "COMPLETED");

        Map<String, Object> p2 = new HashMap<>();
        p2.put("title", "Volume Peak");
        p2.put("weeks", "5-8");
        p2.put("objective", "Maximizing muscle cross-sectional area.");
        p2.put("composition", "8-10 Reps / 45s Rest");
        p2.put("status", "IN PROGRESS");

        plan.put("phases", Arrays.asList(p1, p2));

        db.collection("users").document(uid)
          .collection("plans").document("active_plan")
          .set(plan);
    }
}
