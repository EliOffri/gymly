package com.project.gymly.data.firestore;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.project.gymly.data.ExerciseImageProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FirestoreSeeder {

    private static final String TAG = "FirestoreSeeder";
    private static final String PREFS_NAME = "app_prefs";
    private static final String PREF_KEY_SEED_DONE = "seed_done_v10"; 

    private FirestoreSeeder() { }

    public static void seedIfNeeded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean alreadySeeded = prefs.getBoolean(PREF_KEY_SEED_DONE, false);
        if (alreadySeeded) {
            Log.d(TAG, "Already seeded (v10), skipping");
            return;
        }

        Log.d(TAG, "Starting seeding process v10...");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        seedExercises(db);
        seedWorkoutsForUser(db, "elioffri@gmail.com");

        prefs.edit().putBoolean(PREF_KEY_SEED_DONE, true).apply();
    }

    private static void seedExercises(FirebaseFirestore db) {
        List<Map<String, Object>> exercises = Arrays.asList(
                createExercise("pushups_basic", "Push-ups", "Standard floor push-ups", 30, "Chest", 2, Arrays.asList()),
                createExercise("squats_bodyweight", "Bodyweight Squats", "Squats using only body weight", 30, "Legs", 1, Arrays.asList()),
                createExercise("lunges_forward", "Forward Lunges", "Alternating forward lunges", 30, "Legs", 2, Arrays.asList()),
                createExercise("plank_basic", "Plank", "Hold plank position on elbows", 30, "Core", 1, Arrays.asList("mat")),
                createExercise("glute_bridge", "Glute Bridge", "Hip raises on the floor", 30, "Glutes", 1, Arrays.asList("mat")),
                createExercise("jumping_jacks", "Jumping Jacks", "Light cardio warmup", 30, "FullBody", 1, Arrays.asList()),
                createExercise("mountain_climbers", "Mountain Climbers", "Core and cardio movement", 30, "Core", 2, Arrays.asList()),
                createExercise("bicep_curl_dumbbells", "Biceps Curls", "Dumbbell curls standing", 30, "Arms", 2, Arrays.asList("dumbbells")),
                createExercise("shoulder_press_dumbbells", "Shoulder Press", "Overhead dumbbell press", 30, "Shoulders", 2, Arrays.asList("dumbbells")),
                createExercise("tricep_dips_chair", "Triceps Dips", "Triceps dips on a stable chair/bench", 30, "Arms", 2, Arrays.asList("bench")),
                createExercise("dead_bug_core", "Dead Bug", "Core stability floor exercise", 30, "Core", 1, Arrays.asList("mat")),
                createExercise("treadmill_walk", "Treadmill Walk", "Easy walk on a treadmill", 20, "FullBody", 1, Arrays.asList("treadmill"))
        );

        WriteBatch batch = db.batch();
        CollectionReference colRef = db.collection("exercises");

        for (Map<String, Object> ex : exercises) {
            String id = (String) ex.get("id");
            ex.put("imageUrl", ExerciseImageProvider.getImageUrl(id));
            Map<String, Object> data = new HashMap<>(ex);
            data.remove("id");
            batch.set(colRef.document(id), data);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Exercises collection seeded successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error seeding exercises", e));
    }

    private static void seedWorkoutsForUser(FirebaseFirestore db, String email) {
        Log.d(TAG, "Checking for user: " + email);
        db.collection("users").whereEqualTo("email", email).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                String userId = queryDocumentSnapshots.getDocuments().get(0).getId();
                Log.d(TAG, "Found user " + email + " with ID: " + userId + ". Seeding plan...");
                
                Map<String, Object> plan = new HashMap<>();
                plan.put("title", "12-Week Transformation");
                plan.put("isActive", true);
                plan.put("durationWeeks", 12);
                plan.put("startDate", FieldValue.serverTimestamp());
                
                Map<String, Object> schedule = new HashMap<>();
                for (int w = 1; w <= 12; w++) {
                    Map<String, Map<String, Object>> weekDays = new HashMap<>();
                    
                    weekDays.put("mon", createWorkout("Push Day", 45, Arrays.asList(
                            createWorkoutStep("pushups_basic", 4, 12),
                            createWorkoutStep("shoulder_press_dumbbells", 3, 10),
                            createWorkoutStep("tricep_dips_chair", 3, 15)
                    )));
                    
                    weekDays.put("wed", createWorkout("Pull & Core", 40, Arrays.asList(
                            createWorkoutStep("bicep_curl_dumbbells", 3, 12),
                            createWorkoutStep("plank_basic", 3, 45),
                            createWorkoutStep("mountain_climbers", 3, 20)
                    )));
                    
                    weekDays.put("fri", createWorkout("Leg Day", 50, Arrays.asList(
                            createWorkoutStep("squats_bodyweight", 4, 20),
                            createWorkoutStep("lunges_forward", 3, 12),
                            createWorkoutStep("glute_bridge", 3, 15)
                    )));
                    
                    schedule.put(String.valueOf(w), weekDays);
                }
                plan.put("schedule", schedule);
                
                db.collection("users").document(userId).collection("plans").add(plan)
                    .addOnSuccessListener(docRef -> Log.d(TAG, "Plan seeded successfully for " + email))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to seed plan for " + email, e));
            } else {
                Log.e(TAG, "User " + email + " NOT FOUND in 'users' collection. Make sure you register first!");
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error searching for user", e));
    }

    private static Map<String, Object> createWorkout(String name, int duration, List<Map<String, Object>> steps) {
        Map<String, Object> workout = new HashMap<>();
        workout.put("name", name);
        workout.put("duration", duration);
        workout.put("exercises", steps);
        return workout;
    }

    private static Map<String, Object> createWorkoutStep(String exerciseId, int sets, int reps) {
        Map<String, Object> step = new HashMap<>();
        step.put("exerciseId", exerciseId);
        step.put("sets", sets);
        step.put("reps", reps);
        return step;
    }

    private static Map<String, Object> createExercise(String id, String name, String description, int duration, String muscleGroup, int difficulty, List<String> equipmentRequired) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("description", description);
        map.put("duration", duration);
        map.put("muscleGroup", muscleGroup);
        map.put("difficulty", difficulty);
        map.put("equipmentRequired", equipmentRequired);
        return map;
    }
}
