package com.project.gymly;

public class Day {
    private String dayName;
    private int dayNumber;
    private boolean isToday;

    public Day(String dayName, int dayNumber, boolean isToday) {
        this.dayName = dayName;
        this.dayNumber = dayNumber;
        this.isToday = isToday;
    }

    public String getDayName() {
        return dayName;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public boolean isToday() {
        return isToday;
    }
}
