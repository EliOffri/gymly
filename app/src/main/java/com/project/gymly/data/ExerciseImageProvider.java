package com.project.gymly.data;

import java.util.HashMap;
import java.util.Map;

public class ExerciseImageProvider {

    private static final Map<String, String> exerciseImageUrls = new HashMap<>();

    static {
        exerciseImageUrls.put("pushups_basic", "https://media.istockphoto.com/id/578104104/vector/step-to-instruction-in-push-up.jpg?s=612x612&w=0&k=20&c=AYSyhYJB-98AZL2Euig4fygTjdxliyE8TWHGfXNO6go=");
        exerciseImageUrls.put("squats_bodyweight", "https://media.istockphoto.com/id/1135331615/vector/exercise-guide-by-woman-doing-air-squat-in-2-steps-in-side-view-for-strengthens-entire-lower.jpg?s=612x612&w=0&k=20&c=IkcJfYojhqUNv-q1uxD4d7oQGUB3gAczs4qeZbd00xo=");
        exerciseImageUrls.put("lunges_forward", "https://media.istockphoto.com/id/1224321252/vector/woman-doing-exercise-with-dumbbell-reverse-lunge-in-2-step-illustration-about-fitness-with.jpg?s=612x612&w=0&k=20&c=yGSc_8mDV6q3VPjeOOgqqMY_fBplNNIW8c_kqadBf_I=");
        exerciseImageUrls.put("plank_basic", "https://media.istockphoto.com/id/1204463032/vector/woman-doing-plank-exercise-on-blue-mat-with-stopclock-symbol-over-her-head.jpg?s=612x612&w=0&k=20&c=gmOmI8TkKt9deB1Mh3pAAA8DWBPVc7QBIdMh9r0NAQ8=");
        exerciseImageUrls.put("glute_bridge", "https://media.istockphoto.com/id/1840175315/vector/senior-man-doing-glute-bridge-exercise.jpg?s=612x612&w=0&k=20&c=DAZpx1KAIATdr_4OvNtZJojNwav2PtWhoylgZELyNjU=");
        exerciseImageUrls.put("jumping_jacks", "https://media.istockphoto.com/id/1309811714/vector/jumping-jacks-female-home-workout-exercise-guidance-colorful-vector-illustration.jpg?s=612x612&w=0&k=20&c=aAkNNo0D3AzHpYB8b4nECRgaNYQY_ME1VvpIe6PQl-0=");
        exerciseImageUrls.put("mountain_climbers", "https://media.istockphoto.com/id/957699448/vector/step-of-doing-the-mountain-climber-exercise-by-healthy-woman.jpg?s=612x612&w=0&k=20&c=VNul5Y5u4xaNBFhfXl2MxeWXek3jUwEAC4_axjLgQos=");
        exerciseImageUrls.put("bicep_curl_dumbbells", "https://media.istockphoto.com/id/1224321184/vector/sport-women-doing-fitness-with-the-dumbbell-curl-in-left-and-right-arm-build-muscle-and.jpg?s=612x612&w=0&k=20&c=2UIH0wZkOxajs5Jefxp1_0Fql51MmYnvyRKqEl3AN-8=");
        exerciseImageUrls.put("shoulder_press_dumbbells", "https://static.vecteezy.com/system/resources/previews/027/208/888/non_2x/man-doing-open-fit-shoulder-press-exercise-flat-illustration-isolated-on-white-background-workout-character-set-vector.jpg");
        exerciseImageUrls.put("tricep_dips_chair", "https://media.istockphoto.com/id/1248545807/vector/woman-doing-hip-dip-fitness-with-bench-in-2-steps-for-exercise-guide-fit-triceps-brach-ii.jpg?s=612x612&w=0&k=20&c=B4nWDXuV_WS9nShVG7YJAwrdJ2j7WvbVwyAp0AowmCQ=");
        exerciseImageUrls.put("dead_bug_core", "https://media.istockphoto.com/id/1400384067/video/dead-bug-exercise-female-workout-on-mat.jpg?s=640x640&k=20&c=u1BGAdtqzj2Ia_WNZW0E1e3_I_-9mZXtg9ICrPuejxQ=");
        exerciseImageUrls.put("treadmill_walk", "https://t4.ftcdn.net/jpg/01/70/07/47/360_F_170074762_PpziEJruY5JmGKAvkHCpbpLJm8auWlil.jpg");
    }

    public static String getImageUrl(String exerciseId) {
        return exerciseImageUrls.getOrDefault(exerciseId, ""); // Return empty string if not found
    }
}
