package com.project.gymly.data.firestore;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
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
    private static final String PREF_KEY_SEED_DONE = "seed_done";

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

        // 1) Seed demo users (must exist before we create their plans)
        seedUsers(db);

        // 2) User IDs used in the weekly plans and logs
        String[] userIds = new String[] {
                "eliUser",
                "ofirUser"
        };

        // 3) Global data (shared by all users)
        seedExerciseLibrary(db);
        seedMotivationMessages(db);

        // 3a) Global plan options (fitness levels, goals, equipment)
        seedPlanOptions(db);

        // 4) User-specific data: weekly plans + sample workout history
        for (String uid : userIds) {
            seedWeeklyPlanForUser(db, uid);
            seedCompletedWorkoutsForUser(db, uid);
        }

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

    /**
     * Seed the "motivationMessages" collection with static messages.
     */
    private static void seedMotivationMessages(FirebaseFirestore db) {
        CollectionReference colRef = db.collection("motivationMessages");
        WriteBatch batch = db.batch();

        Map<String, Object> msg1 = new HashMap<>();
        msg1.put("text", "Every workout counts, even a short one.");
        msg1.put("type", "daily");
        batch.set(colRef.document("msg1"), msg1);

        Map<String, Object> msg2 = new HashMap<>();
        msg2.put("text", "Great job! Drink some water and stretch your muscles.");
        msg2.put("type", "postWorkout");
        batch.set(colRef.document("msg2"), msg2);

        Map<String, Object> msg3 = new HashMap<>();
        msg3.put("text", "Small steps each week add up to big results.");
        msg3.put("type", "daily");
        batch.set(colRef.document("msg3"), msg3);

        Map<String, Object> msg4 = new HashMap<>();
        msg4.put("text", "You showed up today – that’s the hardest part.");
        msg4.put("type", "postWorkout");
        batch.set(colRef.document("msg4"), msg4);

        batch.commit()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "motivationMessages seeded"))
                .addOnFailureListener(e -> Log.e(TAG, "Error seeding motivationMessages", e));
    }

    /**
     * Seed global plan options: fitness levels, goals and equipment.
     * Stored under collection "planOptions", document "default".
     */
    private static void seedPlanOptions(FirebaseFirestore db) {
        Map<String, Object> data = new HashMap<>();
        data.put("fitnessLevels", Arrays.asList(
                "Beginner",
                "Intermediate",
                "Advanced"
        ));

        data.put("goals", Arrays.asList(
                "Weight loss",
                "Strength",
                "Flexibility",
                "Tone",
                "General fitness"
        ));

        data.put("equipment", Arrays.asList(
                "Dumbbells",
                "Bench",
                "Kettlebell",
                "Mat",
                "Treadmill",
                "Resistance band"
        ));

        db.collection("planOptions")
                .document("default")
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "planOptions/default seeded"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error seeding planOptions/default", e));
    }

    /**
     * Seed the "weeklyPlans/{uid}" document for a specific user,
     * customized based on their profile.
     */
    private static void seedWeeklyPlanForUser(FirebaseFirestore db, String uid) {
        Map<String, Object> data = new HashMap<>();

        if ("ofirUser".equals(uid)) {
            // Beginner, weight loss + tone, mat/band/treadmill
            data.put("sun", Arrays.asList(
                    "treadmill_walk",
                    "jumping_jacks",
                    "mountain_climbers"
            ));
            data.put("mon", Arrays.asList());
            data.put("tue", Arrays.asList());
            data.put("wed", Arrays.asList(
                    "squats_bodyweight",
                    "glute_bridge",
                    "plank_basic",
                    "dead_bug_core"
            ));
            data.put("thu", Arrays.asList());
            data.put("fri", Arrays.asList());
            data.put("sat", Arrays.asList());

        } else if ("eliUser".equals(uid)) {
            // Intermediate, strength + flexibility, dumbbells/bench/kettlebell
            data.put("sun", Arrays.asList(
                    "jumping_jacks",
                    "bicep_curl_dumbbells",
                    "shoulder_press_dumbbells"
            ));
            data.put("mon", Arrays.asList());
            data.put("tue", Arrays.asList(
                    "squats_bodyweight",
                    "lunges_forward",
                    "glute_bridge"
            ));
            data.put("wed", Arrays.asList());
            data.put("thu", Arrays.asList(
                    "dead_bug_core",
                    "plank_basic",
                    "tricep_dips_chair"
            ));
            data.put("fri", Arrays.asList());
            data.put("sat", Arrays.asList());

        } else {
            // Fallback plan
            data.put("sun", Arrays.asList("jumping_jacks", "squats_bodyweight", "plank_basic"));
            data.put("mon", Arrays.asList());
            data.put("tue", Arrays.asList("jumping_jacks", "lunges_forward", "dead_bug_core"));
            data.put("wed", Arrays.asList());
            data.put("thu", Arrays.asList("bicep_curl_dumbbells", "shoulder_press_dumbbells", "tricep_dips_chair"));
            data.put("fri", Arrays.asList());
            data.put("sat", Arrays.asList("glute_bridge", "plank_basic", "dead_bug_core"));
        }

        data.put("planGeneratedAt", FieldValue.serverTimestamp());

        db.collection("weeklyPlans")
                .document(uid)
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "weeklyPlans seeded for " + uid))
                .addOnFailureListener(e -> Log.e(TAG, "Error seeding weeklyPlans for " + uid, e));
    }

    /**
     * Seed sample "completedWorkouts/{uid}/logs" history.
     */
    private static void seedCompletedWorkoutsForUser(FirebaseFirestore db, String uid) {
        CollectionReference logsCol = db.collection("completedWorkouts")
                .document(uid)
                .collection("logs");

        Map<String, Object> log1 = new HashMap<>();
        log1.put("date", FieldValue.serverTimestamp());
        log1.put("duration", 25);
        log1.put("calories", 180);
        log1.put("exercisesCompleted", 3);
        log1.put("avgDifficulty", 2);
        log1.put("notes", "Felt good, next time increase duration to 30 min");

        Map<String, Object> log2 = new HashMap<>();
        log2.put("date", FieldValue.serverTimestamp());
        log2.put("duration", 30);
        log2.put("calories", 220);
        log2.put("exercisesCompleted", 3);
        log2.put("avgDifficulty", 3);
        log2.put("notes", "Hard but manageable");

        logsCol.document("log1").set(log1);
        logsCol.document("log2").set(log2);
    }
}
