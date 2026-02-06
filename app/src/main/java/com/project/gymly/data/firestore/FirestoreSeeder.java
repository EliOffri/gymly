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
    private static final String PREF_KEY_SEED_DONE = "seed_done_v14"; 

    private FirestoreSeeder() { }

    public static void seedIfNeeded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean alreadySeeded = prefs.getBoolean(PREF_KEY_SEED_DONE, false);
        if (alreadySeeded) {
            Log.d(TAG, "Already seeded (v14), skipping");
            return;
        }

        Log.d(TAG, "Starting seeding process v14 (Fixed IDs)...");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        seedRichLibrary(db);
        seedWorkoutsForUser(db, "elioffri@gmail.com");

        prefs.edit().putBoolean(PREF_KEY_SEED_DONE, true).apply();
    }

    private static void seedRichLibrary(FirebaseFirestore db) {
        List<Map<String, Object>> exercises = new ArrayList<>();

        // Helper to make creation cleaner and ensure IDs match
        // Chest
        exercises.add(createRichExercise("Push-ups", "A classic bodyweight movement for chest development.", 
                "1. Start in plank position.\n2. Lower chest to floor.\n3. Push back up explosively.", 
                "Chest", 1, "mat", "IODxDxX7oi4")); 
        exercises.add(createRichExercise("Bench Press (Barbell)", "The king of upper body compound lifts.", 
                "1. Lie on bench, feet flat.\n2. Grip bar slightly wider than shoulders.\n3. Lower to mid-chest.\n4. Press up.", 
                "Chest", 2, "barbell, bench", "rT7DgCr-3pg"));
        exercises.add(createRichExercise("Incline Dumbbell Press", "Targets the upper chest fibers.", 
                "1. Set bench to 45 degrees.\n2. Press dumbbells overhead.\n3. Lower with control.", 
                "Chest", 2, "dumbbells, bench", "8iPEnn-ltC8"));

        // Back
        exercises.add(createRichExercise("Pull-ups", "Builds a wide back and strong lats.", 
                "1. Grip bar wide.\n2. Pull chin over bar.\n3. Lower all the way down.", 
                "Back", 3, "pull-up bar", "eGo4IYlbE5g"));
        exercises.add(createRichExercise("Deadlifts", "Total body strength builder focusing on the posterior chain.", 
                "1. Feet hip-width apart.\n2. Grip bar, hips down, chest up.\n3. Drive through heels to stand.", 
                "Back", 3, "barbell", "op9kVnSso6Q"));
        exercises.add(createRichExercise("Seated Cable Rows", "Adds thickness to the middle back.", 
                "1. Sit tall, knees bent.\n2. Pull handle to stomach.\n3. Squeeze shoulder blades.", 
                "Back", 2, "cable machine", "GZbfZ033f74")); // Added video ID
        exercises.add(createRichExercise("Bent Over Rows (Barbell)", "Compound back builder.", 
                "1. Hinge at hips.\n2. Pull bar to sternum.\n3. Lower slowly.", 
                "Back", 2, "barbell", "9efgcAjQe7E"));

        // Legs
        exercises.add(createRichExercise("Squats (Barbell)", "The ultimate lower body mass builder.", 
                "1. Bar on traps.\n2. Hips back and down below parallel.\n3. Drive up.", 
                "Legs", 2, "barbell", "ultWZbGWL5c"));
        exercises.add(createRichExercise("Lunges (Walking)", "Improves balance and unilateral leg strength.", 
                "1. Step forward, lower back knee.\n2. Push off front heel to next step.", 
                "Legs", 2, "dumbbells", "L8fvyb5yoT8"));
        exercises.add(createRichExercise("Leg Press", "Heavy leg loading without spinal compression.", 
                "1. Feet shoulder-width on platform.\n2. Lower weight until knees depend.\n3. Push back up.", 
                "Legs", 1, "leg press machine", "IZxyjW7MPJQ"));
        exercises.add(createRichExercise("Bulgarian Split Squats", "Advanced single-leg movement for glutes and quads.", 
                "1. Rear foot on bench.\n2. Lower hips straight down.\n3. Drive up through front heel.", 
                "Legs", 3, "bench, dumbbells", "2C-uNgKwPLE"));

        // Shoulders
        exercises.add(createRichExercise("Overhead Press (Barbell)", "Builds massive shoulders and core stability.", 
                "1. Bar at collarbone.\n2. Press straight up, head through.\n3. Lower with control.", 
                "Shoulders", 2, "barbell", "2yjwXTZQDDI"));

        // Arms
        exercises.add(createRichExercise("Bicep Curls (Barbell)", "Standard mass builder for biceps.", 
                "1. Underhand grip.\n2. Curl bar to chest.\n3. Lower fully.", 
                "Arms", 1, "barbell", "kwG2ipFRgfo"));
        exercises.add(createRichExercise("Tricep Dips", "Compound movement for triceps mass.", 
                "1. Support weight on bars.\n2. Lower until elbows at 90 degrees.\n3. Press up.", 
                "Arms", 2, "dip station", "2z8JmcrW-As"));

        // Core
        exercises.add(createRichExercise("Plank", "Foundational isometric core stability.", 
                "1. Elbows under shoulders.\n2. Body in straight line.\n3. Hold and breathe.", 
                "Core", 1, "mat", "ASdvN_XEl_c"));
        exercises.add(createRichExercise("Mountain Climbers", "Dynamic core and cardio movement.", 
                "1. Plank position.\n2. Drive knees to chest alternately.\n3. Keep hips down.", 
                "Core", 2, "none", "nmwgirgXLYM"));

        WriteBatch batch = db.batch();
        CollectionReference colRef = db.collection("exercises");

        for (Map<String, Object> ex : exercises) {
            String name = (String) ex.get("name");
            String id = generateId(name);
            ex.put("id", id);
            ex.put("imageUrl", ExerciseImageProvider.getImageUrl(id));
            
            // Just in case videoId wasn't passed to createRichExercise (fallback)
            if (!ex.containsKey("videoUrl")) {
                 ex.put("videoUrl", "https://www.youtube.com/results?search_query=" + name.replace(" ", "+"));
            }
            
            Map<String, Object> data = new HashMap<>(ex);
            data.remove("id"); 
            batch.set(colRef.document(id), data);
            Log.d(TAG, "Seeding Exercise: " + name + " -> ID: " + id);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Rich Library seeded (v14)"))
                .addOnFailureListener(e -> Log.e(TAG, "Error seeding library", e));
    }

    private static Map<String, Object> createRichExercise(String name, String desc, String instr, String group, int diff, String equip, String videoId) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("description", desc);
        map.put("instructions", instr);
        map.put("muscleGroup", group);
        map.put("difficulty", diff);
        map.put("duration", 60); 
        map.put("videoUrl", videoId);
        
        List<String> equipment = new ArrayList<>();
        if (!equip.equals("none")) {
            equipment = Arrays.asList(equip.split(", "));
        }
        map.put("equipmentRequired", equipment);
        
        return map;
    }

    private static String generateId(String name) {
        // e.g. "Bench Press (Barbell)" -> "bench_press_barbell"
        // e.g. "Push-ups" -> "push_ups"
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", ""); // Trim underscores
    }

    private static void seedWorkoutsForUser(FirebaseFirestore db, String email) {
        db.collection("users").whereEqualTo("email", email).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                String userId = queryDocumentSnapshots.getDocuments().get(0).getId();
                
                Map<String, Object> plan = new HashMap<>();
                plan.put("title", "12-Week Transformation");
                plan.put("isActive", true);
                plan.put("durationWeeks", 12);
                plan.put("startDate", FieldValue.serverTimestamp());
                
                Map<String, Object> schedule = new HashMap<>();
                for (int w = 1; w <= 12; w++) {
                    Map<String, Map<String, Object>> weekDays = new HashMap<>();
                    
                    // IMPORTANT: Manually matched these IDs to the generateId output
                    // "Bench Press (Barbell)" -> "bench_press_barbell"
                    // "Overhead Press (Barbell)" -> "overhead_press_barbell"
                    // "Incline Dumbbell Press" -> "incline_dumbbell_press"
                    // "Tricep Dips" -> "tricep_dips"
                    
                    weekDays.put("mon", createWorkout("Push Power", 50, Arrays.asList(
                            createWorkoutStep("bench_press_barbell", 4, 8),
                            createWorkoutStep("overhead_press_barbell", 3, 10),
                            createWorkoutStep("incline_dumbbell_press", 3, 12),
                            createWorkoutStep("tricep_dips", 3, 15)
                    )));
                    
                    weekDays.put("wed", createWorkout("Pull Strength", 45, Arrays.asList(
                            createWorkoutStep("deadlifts", 3, 5),
                            createWorkoutStep("pull_ups", 3, 8),
                            createWorkoutStep("seated_cable_rows", 3, 12),
                            createWorkoutStep("bicep_curls_barbell", 3, 12)
                    )));
                    
                    weekDays.put("fri", createWorkout("Leg Destruction", 60, Arrays.asList(
                            createWorkoutStep("squats_barbell", 4, 10),
                            createWorkoutStep("leg_press", 3, 12),
                            createWorkoutStep("bulgarian_split_squats", 3, 10),
                            createWorkoutStep("lunges_walking", 3, 20)
                    )));
                    
                    schedule.put(String.valueOf(w), weekDays);
                }
                plan.put("schedule", schedule);
                
                db.collection("users").document(userId).collection("plans").add(plan)
                    .addOnSuccessListener(docRef -> Log.d(TAG, "Plan seeded for " + email));
            }
        });
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
}
