// app/src/main/java/com/project/gymly/data/firestore/FirestoreSeeder.java
package com.project.gymly.data.firestore;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FirestoreSeeder {

    private static final String TAG = "FirestoreSeeder";
    private static final String PREFS_NAME = "app_prefs";
    private static final String PREF_KEY_SEED_DONE = "seed_done_v1";

    // Prevent instantiation
    private FirestoreSeeder() { }

    /**
     * Public entry point – seeds Firestore only once per install.
     * Called from MainActivity.onCreate().
     */
    public static void seedIfNeeded(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        boolean alreadySeeded = prefs.getBoolean(PREF_KEY_SEED_DONE, false);
        if (alreadySeeded) {
            Log.d(TAG, "Already seeded, skipping");
            return;
        }

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

        // 4) User-specific data: weekly plans + sample workout history
        for (String uid : userIds) {
            seedWeeklyPlanForUser(db, uid);
            seedCompletedWorkoutsForUser(db, uid);
        }

        prefs.edit().putBoolean(PREF_KEY_SEED_DONE, true).apply();
        Log.d(TAG, "Seeding finished");
    }

    /**
     * Seed the "users" collection with two demo users: ofirUser and eliUser.
     */
    private static void seedUsers(FirebaseFirestore db) {
        CollectionReference usersCol = db.collection("users");

        // ----- Ofir user -----
        Map<String, Object> ofir = new HashMap<>();
        ofir.put("name", "Ofir");
        ofir.put("email", "Ofir@gmail.com");
        ofir.put("level", "beginner");
        ofir.put("goals", Arrays.asList("weight_loss", "tone", "general_fitness"));
        ofir.put("equipment", Arrays.asList("mat", "resistance_band", "treadmill"));

        Map<String, Object> ofirSchedule = new HashMap<>();
        ofirSchedule.put("sun", 45);
        ofirSchedule.put("mon", 0);
        ofirSchedule.put("tue", 0);
        ofirSchedule.put("wed", 45);
        ofirSchedule.put("thu", 0);
        ofirSchedule.put("fri", 0);
        ofirSchedule.put("sat", 0);
        ofir.put("schedule", ofirSchedule);

        ofir.put("createdAt", FieldValue.serverTimestamp());

        usersCol.document("ofirUser").set(ofir)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User ofirUser seeded"))
                .addOnFailureListener(e -> Log.e(TAG, "Error seeding user ofirUser", e));

        // ----- Eli user -----
        Map<String, Object> eli = new HashMap<>();
        eli.put("name", "Eli");
        eli.put("email", "eli@gmail.com");
        eli.put("level", "intermediate");
        eli.put("goals", Arrays.asList("strength", "flexibility"));
        eli.put("equipment", Arrays.asList("dumbbells", "bench", "kettlebell"));

        Map<String, Object> eliSchedule = new HashMap<>();
        eliSchedule.put("sun", 45);
        eliSchedule.put("mon", 0);
        eliSchedule.put("tue", 45);
        eliSchedule.put("wed", 0);
        eliSchedule.put("thu", 45);
        eliSchedule.put("fri", 0);
        eliSchedule.put("sat", 0);
        eli.put("schedule", eliSchedule);

        eli.put("createdAt", FieldValue.serverTimestamp());

        usersCol.document("eliUser").set(eli)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User eliUser seeded"))
                .addOnFailureListener(e -> Log.e(TAG, "Error seeding user eliUser", e));
    }

    /**
     * Seed the "exerciseLibrary" collection with predefined exercises.
     */
    private static void seedExerciseLibrary(FirebaseFirestore db) {
        List<Map<String, Object>> exercises = Arrays.asList(
                createExercise("pushups_basic", "Push-ups",
                        "Standard floor push-ups",
                        30, "Chest", 2,
                        Arrays.asList()),
                createExercise("squats_bodyweight", "Bodyweight Squats",
                        "Squats using only body weight",
                        30, "Legs", 1,
                        Arrays.asList()),
                createExercise("lunges_forward", "Forward Lunges",
                        "Alternating forward lunges",
                        30, "Legs", 2,
                        Arrays.asList()),
                createExercise("plank_basic", "Plank",
                        "Hold plank position on elbows",
                        30, "Core", 1,
                        Arrays.asList("mat")),
                createExercise("glute_bridge", "Glute Bridge",
                        "Hip raises on the floor",
                        30, "Glutes", 1,
                        Arrays.asList("mat")),
                createExercise("jumping_jacks", "Jumping Jacks",
                        "Light cardio warmup",
                        30, "FullBody", 1,
                        Arrays.asList()),
                createExercise("mountain_climbers", "Mountain Climbers",
                        "Core and cardio movement",
                        30, "Core", 2,
                        Arrays.asList()),
                createExercise("bicep_curl_dumbbells", "Biceps Curls",
                        "Dumbbell curls standing",
                        30, "Arms", 2,
                        Arrays.asList("dumbbells")),
                createExercise("shoulder_press_dumbbells", "Shoulder Press",
                        "Overhead dumbbell press",
                        30, "Shoulders", 2,
                        Arrays.asList("dumbbells")),
                createExercise("tricep_dips_chair", "Triceps Dips",
                        "Triceps dips on a stable chair/bench",
                        30, "Arms", 2,
                        Arrays.asList("bench")),
                createExercise("dead_bug_core", "Dead Bug",
                        "Core stability floor exercise",
                        30, "Core", 1,
                        Arrays.asList("mat")),
                createExercise("treadmill_walk", "Treadmill Walk",
                        "Easy walk on a treadmill",
                        20, "FullBody", 1,
                        Arrays.asList("treadmill"))
        );

        WriteBatch batch = db.batch();
        CollectionReference colRef = db.collection("exerciseLibrary");

        for (Map<String, Object> ex : exercises) {
            String id = (String) ex.get("id");
            Map<String, Object> data = new HashMap<>(ex);
            data.remove("id"); // Use "id" only as document ID, not as a field
            batch.set(colRef.document(id), data);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "exerciseLibrary seeded"))
                .addOnFailureListener(e -> Log.e(TAG, "Error seeding exerciseLibrary", e));
    }

    /**
     * Helper method to build a single exercise map.
     */
    private static Map<String, Object> createExercise(String id,
                                                      String name,
                                                      String description,
                                                      int duration,
                                                      String muscleGroup,
                                                      int difficulty,
                                                      List<String> equipmentRequired) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("description", description);
        map.put("duration", duration);
        map.put("muscleGroup", muscleGroup);
        map.put("difficulty", difficulty);
        map.put("equipmentRequired", equipmentRequired);
        map.put("imageUrl", "");
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
