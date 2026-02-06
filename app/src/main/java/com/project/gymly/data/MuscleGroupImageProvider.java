package com.project.gymly.data;

import com.project.gymly.R;

public class MuscleGroupImageProvider {

    public static int getIconResource(String muscleGroup) {
        if (muscleGroup == null) return R.drawable.ic_gymly_logo;

        String key = muscleGroup.toLowerCase().trim();

        if (key.contains("chest")) return android.R.drawable.ic_menu_edit; // Placeholder for Chest
        if (key.contains("back")) return android.R.drawable.ic_menu_sort_by_size; // Placeholder for Back
        if (key.contains("leg")) return android.R.drawable.ic_menu_directions; // Placeholder for Legs
        if (key.contains("shoulder")) return android.R.drawable.ic_menu_slideshow; // Placeholder for Shoulders
        if (key.contains("arm") || key.contains("bicep") || key.contains("tricep")) return android.R.drawable.ic_menu_call; // Placeholder for Arms
        if (key.contains("core") || key.contains("abs")) return android.R.drawable.ic_menu_crop; // Placeholder for Core
        if (key.contains("cardio")) return android.R.drawable.ic_menu_compass; // Placeholder for Cardio

        return R.drawable.ic_gymly_logo;
    }
}
