package com.project.gymly.models;

import java.util.Map;

public class User {
    private String name;
    private int age;
    private String fitnessLevel;
    private String goal;
    private Map<String, String> weeklySchedule;

    public User() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getFitnessLevel() { return fitnessLevel; }
    public void setFitnessLevel(String fitnessLevel) { this.fitnessLevel = fitnessLevel; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public Map<String, String> getWeeklySchedule() { return weeklySchedule; }
    public void setWeeklySchedule(Map<String, String> weeklySchedule) { this.weeklySchedule = weeklySchedule; }
}