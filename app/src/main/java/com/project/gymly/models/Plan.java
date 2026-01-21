package com.project.gymly.models;

import com.google.firebase.Timestamp;
import java.util.List;
import java.util.Map;

public class Plan {
    private String title;
    private String purpose;
    private int durationWeeks;
    private boolean isActive;
    private Timestamp startDate;
    private int totalSessions;
    private int completedSessions;
    private List<Map<String, Object>> phases;

    public Plan() {} // Required for Firestore

    public Plan(String title, String purpose, int durationWeeks, boolean isActive, 
                Timestamp startDate, int totalSessions, int completedSessions, 
                List<Map<String, Object>> phases) {
        this.title = title;
        this.purpose = purpose;
        this.durationWeeks = durationWeeks;
        this.isActive = isActive;
        this.startDate = startDate;
        this.totalSessions = totalSessions;
        this.completedSessions = completedSessions;
        this.phases = phases;
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public String getPurpose() { return purpose; }
    public int getDurationWeeks() { return durationWeeks; }
    public boolean isActive() { return isActive; }
    public Timestamp getStartDate() { return startDate; }
    public int getTotalSessions() { return totalSessions; }
    public int getCompletedSessions() { return completedSessions; }
    public List<Map<String, Object>> getPhases() { return phases; }
}
