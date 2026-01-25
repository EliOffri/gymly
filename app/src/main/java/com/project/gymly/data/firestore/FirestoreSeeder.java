package com.project.gymly.data.firestore;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.project.gymly.data.ExerciseImageProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FirestoreSeeder {

    private static final String TAG = "FirestoreSeeder";
    private static final String PREFS_NAME = "app_prefs";
    private static final String PREF_KEY_SEED_DONE = "seed_done_v4";

    private FirestoreSeeder() { }

    public static void seedIfNeeded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean alreadySeeded = prefs.getBoolean(PREF_KEY_SEED_DONE, false);
        if (alreadySeeded) {
            Log.d(TAG, "Already seeded, skipping");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        seedExerciseLibrary(db);
        // Other seeding methods can be called here

        prefs.edit().putBoolean(PREF_KEY_SEED_DONE, true).apply();
        Log.d(TAG, "Seeding finished");
    }

    private static void seedExerciseLibrary(FirebaseFirestore db) {
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
        CollectionReference colRef = db.collection("exerciseLibrary");

        for (Map<String, Object> ex : exercises) {
            String id = (String) ex.get("id");
            ex.put("imageUrl", ExerciseImageProvider.getImageUrl(id));
            Map<String, Object> data = new HashMap<>(ex);
            data.remove("id");
            batch.set(colRef.document(id), data);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "exerciseLibrary seeded"))
                .addOnFailureListener(e -> Log.e(TAG, "Error seeding exerciseLibrary", e));
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
