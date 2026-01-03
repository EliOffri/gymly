package com.project.gymly.models;

import java.util.List;

public class WorkoutDay {
    private String dayName;
    private List<String> exercises;

    public WorkoutDay(String dayName, List<String> exercises) {
        this.dayName = dayName;
        this.exercises = exercises;
    }

    public String getDayName() { return dayName; }
    public List<String> getExercises() { return exercises; }
}