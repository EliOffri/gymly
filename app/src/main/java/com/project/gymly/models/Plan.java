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

    public Plan() {}

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

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public int getDurationWeeks() { return durationWeeks; }
    public void setDurationWeeks(int durationWeeks) { this.durationWeeks = durationWeeks; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Timestamp getStartDate() { return startDate; }
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }

    public int getTotalSessions() { return totalSessions; }
    public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }

    public int getCompletedSessions() { return completedSessions; }
    public void setCompletedSessions(int completedSessions) { this.completedSessions = completedSessions; }

    public List<Map<String, Object>> getPhases() { return phases; }
    public void setPhases(List<Map<String, Object>> phases) { this.phases = phases; }
}
