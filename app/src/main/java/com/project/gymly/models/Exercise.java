package com.project.gymly.models;

import java.util.List;

public class Exercise {
    private String name;
    private String description;
    private int difficulty; // Changed from String to int
    private String muscleGroup; // Matches Seeder field name
    private List<String> equipmentRequired; // Matches Seeder field name
    private int duration;

    public Exercise() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    public String getMuscleGroup() { return muscleGroup; }
    public void setMuscleGroup(String muscleGroup) { this.muscleGroup = muscleGroup; }

    public List<String> getEquipmentRequired() { return equipmentRequired; }
    public void setEquipmentRequired(List<String> equipmentRequired) { this.equipmentRequired = equipmentRequired; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
}