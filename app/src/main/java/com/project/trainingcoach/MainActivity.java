package com.project.trainingcoach;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends ComponentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Keep existing edge-to-edge behavior
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Seed Firestore data once (exercise library, motivation messages, plans, logs)
        seedFirestoreIfNeeded();
    }

    // Entry point for seeding Firestore, runs only once per install
    private void seedFirestoreIfNeeded() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean alreadySeeded = prefs.getBoolean("seed_done_v1", false);
        if (alreadySeeded) {
            Log.d("SEED", "Already seeded, skipping");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1) Seed demo users (must exist before we create their plans)
        seedUsers(db);

        // Use the user document IDs you already created in Firestore
        String[] userIds = new String[] {
                "eliUser",
                "ofirUser"
        };

        // Global data (shared for all users)
        seedExerciseLibrary(db);
        seedMotivationMessages(db);

        // User-specific data
        for (String uid : userIds) {
            seedWeeklyPlanForUser(db, uid);
            seedCompletedWorkoutsForUser(db, uid);
        }

        prefs.edit().putBoolean("seed_done_v1", true).apply();
        Log.d("SEED", "Seeding finished");
    }

    // Seed the users collection with two demo users: ofirUser and eliUser
    private void seedUsers(FirebaseFirestore db) {
        CollectionReference usersCol = db.collection("users");

        // ----- Ofir user -----
        Map<String, Object> ofir = new HashMap<>();
        ofir.put("name", "Ofir");
        ofir.put("email", "Ofir@gmail.com");
        ofir.put("level", "beginner");
        ofir.put("goals", Arrays.asList("weight_loss", "tone", "general_fitness"));
        ofir.put("equipment", Arrays.asList("mat", "resistance_band", "treadmill"));

        // Schedule in minutes per day (as you defined in Firestore)
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
                .addOnSuccessListener(aVoid -> Log.d("SEED", "User ofirUser seeded"))
                .addOnFailureListener(e -> Log.e("SEED", "Error seeding user ofirUser", e));

        // ----- Eli user -----
        Map<String, Object> eli = new HashMap<>();
        eli.put("name", "eli");
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
                .addOnSuccessListener(aVoid -> Log.d("SEED", "User eliUser seeded"))
                .addOnFailureListener(e -> Log.e("SEED", "Error seeding user eliUser", e));
    }

    // Create the exerciseLibrary collection with predefined exercises
    private void seedExerciseLibrary(FirebaseFirestore db) {
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

        // Use batch writes for better performance and atomic commit
        for (Map<String, Object> ex : exercises) {
            String id = (String) ex.get("id");
            Map<String, Object> data = new HashMap<>(ex);
            data.remove("id"); // Use "id" only as document ID, not as a field
            batch.set(colRef.document(id), data);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> Log.d("SEED", "exerciseLibrary seeded"))
                .addOnFailureListener(e -> Log.e("SEED", "Error seeding exerciseLibrary", e));
    }

    // Helper method to build a single exercise map
    private Map<String, Object> createExercise(String id,
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

    // Create motivationMessages collection with static motivation texts
    private void seedMotivationMessages(FirebaseFirestore db) {
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
                .addOnSuccessListener(aVoid -> Log.d("SEED", "motivationMessages seeded"))
                .addOnFailureListener(e -> Log.e("SEED", "Error seeding motivationMessages", e));
    }

    // Create weeklyPlans document for a specific user, customized by their profile
    private void seedWeeklyPlanForUser(FirebaseFirestore db, String uid) {
        Map<String, Object> data = new HashMap<>();

        if ("ofirUser".equals(uid)) {
            // Ofir:
            // level: beginner
            // goals: weight_loss, tone, general_fitness
            // equipment: mat, resistance_band, treadmill
            // schedule: sun = 45, wed = 45, others = 0

            // Sunday: more cardio and fat-burning focus
            data.put("sun", Arrays.asList(
                    "treadmill_walk",    // main cardio on treadmill
                    "jumping_jacks",     // light cardio warmup
                    "mountain_climbers"  // cardio + core
            ));

            // No workouts on Monday and Tuesday
            data.put("mon", Arrays.asList());
            data.put("tue", Arrays.asList());

            // Wednesday: strength + tone with simple bodyweight and mat
            data.put("wed", Arrays.asList(
                    "squats_bodyweight", // legs + tone
                    "glute_bridge",      // glutes on mat
                    "plank_basic",       // core
                    "dead_bug_core"      // core stability
            ));

            // No workouts on Thursday, Friday, Saturday
            data.put("thu", Arrays.asList());
            data.put("fri", Arrays.asList());
            data.put("sat", Arrays.asList());

        } else if ("eliUser".equals(uid)) {
            // Eli:
            // level: intermediate
            // goals: strength, flexibility
            // equipment: dumbbells, bench, kettlebell
            // schedule: sun = 45, tue = 45, thu = 45, others = 0

            // Sunday: upper-body strength focus
            data.put("sun", Arrays.asList(
                    "jumping_jacks",           // short warmup
                    "bicep_curl_dumbbells",    // arms strength
                    "shoulder_press_dumbbells" // shoulders strength
            ));

            // Monday: no workout
            data.put("mon", Arrays.asList());

            // Tuesday: lower body strength
            data.put("tue", Arrays.asList(
                    "squats_bodyweight",       // legs
                    "lunges_forward",          // legs + balance
                    "glute_bridge"             // posterior chain
            ));

            // Wednesday: no workout
            data.put("wed", Arrays.asList());

            // Thursday: core + triceps to support strength and stability
            data.put("thu", Arrays.asList(
                    "dead_bug_core",           // core stability
                    "plank_basic",             // core strength
                    "tricep_dips_chair"        // triceps using bench
            ));

            // Friday, Saturday: rest
            data.put("fri", Arrays.asList());
            data.put("sat", Arrays.asList());

        } else {
            // Fallback plan for any other user
            data.put("sun", Arrays.asList("jumping_jacks", "squats_bodyweight", "plank_basic"));
            data.put("mon", Arrays.asList());
            data.put("tue", Arrays.asList("jumping_jacks", "lunges_forward", "dead_bug_core"));
            data.put("wed", Arrays.asList());
            data.put("thu", Arrays.asList("bicep_curl_dumbbells", "shoulder_press_dumbbells", "tricep_dips_chair"));
            data.put("fri", Arrays.asList());
            data.put("sat", Arrays.asList("glute_bridge", "plank_basic", "dead_bug_core"));
        }

        // Timestamp for when the plan was generated
        data.put("planGeneratedAt", FieldValue.serverTimestamp());

        db.collection("weeklyPlans")
                .document(uid)
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d("SEED", "weeklyPlans seeded for " + uid))
                .addOnFailureListener(e -> Log.e("SEED", "Error seeding weeklyPlans for " + uid, e));
    }

    // Create completedWorkouts logs for a specific user (sample history)
    private void seedCompletedWorkoutsForUser(FirebaseFirestore db, String uid) {
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
