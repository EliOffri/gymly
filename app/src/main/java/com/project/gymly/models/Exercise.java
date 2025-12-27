package com.project.gymly.models;

import java.util.List;
import java.util.Map;

public class Exercise {
    private String name;
    private String description;
    private String difficulty;
    private List<String> muscles;
    private List<String> equipment;

    public Exercise() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public List<String> getMuscles() { return muscles; }
    public void setMuscles(List<String> muscles) { this.muscles = muscles; }
    public List<String> getEquipment() { return equipment; }
    public void setEquipment(List<String> equipment) { this.equipment = equipment; }
}