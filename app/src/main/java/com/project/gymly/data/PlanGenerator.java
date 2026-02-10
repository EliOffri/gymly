package com.project.gymly.data;

import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PlanGenerator {

    // Exercise Pools
    private static final List<String> CHEST = Arrays.asList("bench_press_barbell", "incline_dumbbell_press", "push_ups");
    private static final List<String> BACK = Arrays.asList("deadlifts", "pull_ups", "bent_over_rows_barbell", "seated_cable_rows");
    private static final List<String> LEGS = Arrays.asList("squats_barbell", "leg_press", "lunges_walking", "bulgarian_split_squats");
    private static final List<String> SHOULDERS = Arrays.asList("overhead_press_barbell"); 
    private static final List<String> ARMS = Arrays.asList("bicep_curls_barbell", "tricep_dips");
    private static final List<String> CORE = Arrays.asList("plank", "mountain_climbers");

    private static final Map<String, List<String>> WORKOUT_NAMES = new HashMap<>();
    static {
        WORKOUT_NAMES.put("Full Body", Arrays.asList(
            "Total Body Ignition", "Full Body Power", "Metabolic Matrix", "Iron Clad System",
            "Compound Crusher", "Total Strength", "Whole Body Awakening", "Dynamic Full Body",
            "Ultimate Body Burn", "Strength Foundation", "Athletic Conditioning", "Power & Performance"
        ));
        WORKOUT_NAMES.put("Upper Body", Arrays.asList(
            "Upper Body Blast", "Torso Terminator", "Chest & Back Attack", "Upper Body Prime",
            "The Gun Show", "Upper Hypertrophy", "Iron Upper", "Push & Pull Power",
            "Upper Body Sculpt", "Torso Torch"
        ));
        WORKOUT_NAMES.put("Lower Body", Arrays.asList(
            "Leg Day Destruction", "Wheels of Steel", "Lower Body Power", "Glute & Ham Hammer",
            "Quadzilla", "Leg Logic", "Lower Body Prime", "Foundation Legs",
            "Iron Legs", "Lower Limits"
        ));
        WORKOUT_NAMES.put("Push", Arrays.asList(
            "Push Power", "Chest & Tri Triumph", "Pressing Power", "Push Limits",
            "Push Perfection", "Pressing Strength", "Upper Push Focus", "Push Performance"
        ));
        WORKOUT_NAMES.put("Pull", Arrays.asList(
            "Pull Perfection", "Back & Bi Builder", "Deadlift Domination", "Row & Grow",
            "Pull Power", "Back Attack", "The Pull List", "Pull Performance"
        ));
        WORKOUT_NAMES.put("Legs", Arrays.asList(
            "Squat Rack Ritual", "Lower Limits", "Leg Pump", "Leg Day Logic",
            "Squat Therapy", "Lower Body Focus", "Leg Day"
        ));
    }

    public static Map<String, Object> generate(String title, String goal, String level, int weeks, List<String> days) {
        Map<String, Object> plan = new HashMap<>();
        plan.put("name", title);
        plan.put("isActive", true);
        plan.put("durationWeeks", weeks);
        plan.put("startDate", FieldValue.serverTimestamp());
        plan.put("goal", goal);
        plan.put("level", level);
        plan.put("days", days);

        Map<String, Object> schedule = new HashMap<>();
        
        // Determine Split Strategy based on frequency
        List<String> workoutTypes = getWorkoutSplit(days.size());

        for (int w = 1; w <= weeks; w++) {
            Map<String, Object> weekData = new HashMap<>();
            
            // Periodization: Vary sets/reps each week
            int cycleWeek = (w - 1) % 4; 
            
            int dayIndex = 0;
            for (String day : days) {
                String type = workoutTypes.get(dayIndex % workoutTypes.size());
                
                // Add variety: Shuffle accessories slightly or alternate focus
                List<Map<String, Object>> exercises = generateExercisesForType(type, goal, level, cycleWeek, w);
                
                // Generate a unique name
                String workoutName = getWorkoutName(type, w, dayIndex);
                
                Map<String, Object> workout = new HashMap<>();
                workout.put("name", workoutName);
                workout.put("duration", 60 + (level.equals("Advanced") ? 15 : 0));
                workout.put("exercises", exercises);
                workout.put("isCompleted", false);
                
                weekData.put(day, workout);
                dayIndex++;
            }
            schedule.put(String.valueOf(w), weekData);
        }
        
        plan.put("schedule", schedule);
        return plan;
    }

    private static String getWorkoutName(String type, int week, int dayIndex) {
        String baseType = type.replaceAll(" [ABC]$", "").replaceAll(" Remix", "").replaceAll(" Day", "").replaceAll(" & Core", "");
        
        if (baseType.contains("Full Body")) baseType = "Full Body";
        else if (baseType.contains("Upper")) baseType = "Upper Body";
        else if (baseType.contains("Lower")) baseType = "Lower Body";
        else if (baseType.contains("Push")) baseType = "Push";
        else if (baseType.contains("Pull")) baseType = "Pull";
        else if (baseType.contains("Legs")) baseType = "Legs";

        List<String> names = WORKOUT_NAMES.getOrDefault(baseType, Collections.singletonList(type));
        // Use a hash of week and dayIndex to pick a consistent but unique-looking name
        int index = (week * 7 + dayIndex) % names.size(); 
        return names.get(index);
    }

    private static List<String> getWorkoutSplit(int daysPerWeek) {
        if (daysPerWeek <= 3) {
            return Arrays.asList("Full Body A", "Full Body B", "Full Body C");
        } else if (daysPerWeek == 4) {
            return Arrays.asList("Upper Body A", "Lower Body A", "Upper Body B", "Lower Body B");
        } else {
            // 5-7 days: PPL or mixed
            return Arrays.asList("Push Day", "Pull Day", "Legs & Core", "Upper Body", "Lower Body", "Full Body Remix");
        }
    }

    private static List<Map<String, Object>> generateExercisesForType(String type, String goal, String level, int cycleWeek, int globalWeek) {
        List<Map<String, Object>> steps = new ArrayList<>();
        
        // Base sets/reps logic
        int sets = 3;
        int reps = 12;
        
        if (cycleWeek == 1) { sets = 4; reps = 10; } // Volume
        if (cycleWeek == 2) { sets = 5; reps = 8; }  // Strength
        if (cycleWeek == 3) { sets = 3; reps = 15; } // Endurance
        
        // Adjust for goal
        if (goal.equals("Strength")) { reps -= 2; sets += 1; }
        if (goal.equals("Fat Loss")) { reps += 2; sets = 3; }

        List<String> mainMoves = new ArrayList<>();
        List<String> accessories = new ArrayList<>();

        if (type.contains("Full Body")) {
            if (type.endsWith("A")) {
                mainMoves.add(LEGS.get(0)); // Squat
                mainMoves.add(CHEST.get(0)); // Bench
                mainMoves.add(BACK.get(2)); // Bent Row
            } else if (type.endsWith("B")) {
                mainMoves.add(BACK.get(1)); // Deadlift
                mainMoves.add(SHOULDERS.get(0)); // OHP
                mainMoves.add(LEGS.get(1)); // Leg Press
            } else {
                mainMoves.add(LEGS.get(2)); // Lunges
                mainMoves.add(CHEST.get(1)); // Incline
                mainMoves.add(BACK.get(1)); // Pull ups
            }
            accessories.addAll(ARMS);
            accessories.addAll(CORE);
            accessories.add(LEGS.get(3)); // Bulgarian
            accessories.add(CHEST.get(2)); // Pushups
            
        } else if (type.contains("Upper")) {
            mainMoves.add(CHEST.get(0));
            mainMoves.add(BACK.get(2));
            mainMoves.add(SHOULDERS.get(0));
            mainMoves.add(BACK.get(1)); // Pullups
            
            accessories.addAll(ARMS);
            accessories.add(CHEST.get(1));
            accessories.add(BACK.get(3));
            accessories.add(CHEST.get(2)); 
            
        } else if (type.contains("Lower")) {
            mainMoves.add(LEGS.get(0));
            mainMoves.add(BACK.get(0)); 
            mainMoves.add(LEGS.get(1));
            
            accessories.add(LEGS.get(2));
            accessories.add(LEGS.get(3));
            accessories.addAll(CORE);
            accessories.add(LEGS.get(2));
            
        } else if (type.contains("Push")) {
            mainMoves.add(CHEST.get(0));
            mainMoves.add(SHOULDERS.get(0));
            mainMoves.add(CHEST.get(1));
            
            accessories.add(ARMS.get(1)); 
            accessories.add(CHEST.get(2)); 
            accessories.add(LEGS.get(0)); 
            accessories.addAll(CORE);
            
        } else if (type.contains("Pull")) {
            mainMoves.add(BACK.get(0)); 
            mainMoves.add(BACK.get(1)); 
            mainMoves.add(BACK.get(2)); 
            
            accessories.add(BACK.get(3)); 
            accessories.add(ARMS.get(0)); 
            accessories.addAll(CORE);
            accessories.add(BACK.get(1)); 
            
        } else if (type.contains("Legs")) {
            mainMoves.addAll(LEGS);
            accessories.addAll(CORE);
            accessories.add(LEGS.get(2)); 
            accessories.add("plank"); 
        }

        Collections.shuffle(accessories, new Random(globalWeek));

        int count = 0;
        for (String id : mainMoves) {
            steps.add(createStep(id, sets, reps));
            count++;
        }
        
        for (String id : accessories) {
            if (count >= 10) break;
            steps.add(createStep(id, 3, 12));
            count++;
        }
        
        if (count < 8) {
            steps.add(createStep("plank", 3, 60));
            count++;
        }
        if (count < 8) {
            steps.add(createStep("mountain_climbers", 3, 30));
            count++;
        }

        return steps;
    }

    private static Map<String, Object> createStep(String id, int sets, int reps) {
        Map<String, Object> step = new HashMap<>();
        step.put("exerciseId", id);
        step.put("sets", sets);
        step.put("reps", reps);
        return step;
    }
}
