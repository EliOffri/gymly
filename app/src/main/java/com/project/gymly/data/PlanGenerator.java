package com.project.gymly.data;

import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PlanGenerator {

    public static Map<String, Object> generate(String title, String goal, String level, int weeks, List<String> days) {
        Map<String, Object> plan = new HashMap<>();
        plan.put("title", title);
        plan.put("isActive", true);
        plan.put("durationWeeks", weeks);
        plan.put("startDate", FieldValue.serverTimestamp());
        plan.put("goal", goal);
        plan.put("level", level);

        Map<String, Object> schedule = new HashMap<>();
        
        // Define workout templates
        Map<String, List<Map<String, Object>>> templates = getTemplates(goal, level);

        for (int w = 1; w <= weeks; w++) {
            Map<String, Object> weekData = new HashMap<>();
            
            // Distribute templates across selected days
            int templateIndex = 0;
            String[] templateKeys = templates.keySet().toArray(new String[0]);
            
            for (String day : days) {
                // Cycle through templates (e.g. Push -> Pull -> Legs -> Push...)
                String type = templateKeys[templateIndex % templateKeys.length];
                List<Map<String, Object>> exercises = templates.get(type);
                
                Map<String, Object> workout = new HashMap<>();
                workout.put("name", type + " Workout");
                workout.put("duration", 45 + (level.equals("Advanced") ? 15 : 0));
                workout.put("exercises", exercises);
                workout.put("isCompleted", false);
                
                weekData.put(day, workout);
                templateIndex++;
            }
            schedule.put(String.valueOf(w), weekData);
        }
        
        plan.put("schedule", schedule);
        return plan;
    }

    private static Map<String, List<Map<String, Object>>> getTemplates(String goal, String level) {
        Map<String, List<Map<String, Object>>> map = new HashMap<>();
        
        // Adjust volume based on level
        int sets = level.equals("Beginner") ? 3 : (level.equals("Intermediate") ? 4 : 5);
        int reps = goal.equals("Strength") ? 5 : 12;

        // Build generic templates using our known IDs from the Seeder
        // PUSH
        List<Map<String, Object>> push = new ArrayList<>();
        push.add(createStep("push_ups", sets, reps));
        push.add(createStep("bench_press_barbell", sets, reps));
        push.add(createStep("overhead_press_barbell", sets, reps));
        push.add(createStep("tricep_dips", 3, 15));
        
        // PULL
        List<Map<String, Object>> pull = new ArrayList<>();
        pull.add(createStep("pull_ups", sets, reps));
        pull.add(createStep("deadlifts", 3, 5)); // Heavy compound usually lower reps
        pull.add(createStep("seated_cable_rows", sets, reps));
        pull.add(createStep("bicep_curls_barbell", 3, 15));

        // LEGS
        List<Map<String, Object>> legs = new ArrayList<>();
        legs.add(createStep("squats_barbell", sets, reps));
        legs.add(createStep("lunges_walking", 3, 20));
        legs.add(createStep("leg_press", sets, 15));
        legs.add(createStep("plank", 3, 60)); // Core finisher

        // FULL BODY (Fallback or mixed)
        List<Map<String, Object>> full = new ArrayList<>();
        full.add(createStep("squats_barbell", 3, 10));
        full.add(createStep("push_ups", 3, 15));
        full.add(createStep("mountain_climbers", 3, 30));
        full.add(createStep("burpees", 3, 10));

        map.put("Upper Body", push);
        map.put("Lower Body", legs);
        map.put("Full Body", full);
        
        // Return diverse mapping based on goal could be improved here
        // For now, return a rotation of Push, Pull, Legs to be mapped to days
        Map<String, List<Map<String, Object>>> rotation = new HashMap<>();
        rotation.put("Push Focus", push);
        rotation.put("Pull Focus", pull);
        rotation.put("Legs & Core", legs);
        
        return rotation;
    }

    private static Map<String, Object> createStep(String id, int sets, int reps) {
        Map<String, Object> step = new HashMap<>();
        step.put("exerciseId", id);
        step.put("sets", sets);
        step.put("reps", reps);
        return step;
    }
}
