package com.project.gymly.models;

import com.google.firebase.Timestamp;

public class WorkoutLog {
    private String exerciseName;
    private int reps;
    private double weight;
    private Timestamp timestamp;

    public WorkoutLog() {}

    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }
    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}