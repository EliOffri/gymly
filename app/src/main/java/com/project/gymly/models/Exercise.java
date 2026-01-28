package com.project.gymly.models;

import com.google.firebase.firestore.DocumentId;

import java.util.List;

public class Exercise {
    @DocumentId
    private String id;
    private String name;
    private String description;
    private int difficulty;
    private String muscleGroup;
    private List<String> equipmentRequired;
    private int duration;
    private String instructions;
    private String imageUrl;

    public Exercise() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
