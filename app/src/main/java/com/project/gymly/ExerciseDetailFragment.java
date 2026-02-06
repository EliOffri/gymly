package com.project.gymly;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.gymly.models.Exercise;

import java.util.List;

public class ExerciseDetailFragment extends Fragment {

    private static final String ARG_EXERCISE_NAME = "exercise_name";
    private String exerciseName; // Can be name or ID
    
    private TextView tvTitle, tvCategory, tvDescription;
    private ChipGroup cgEquipment;
    private ImageButton btnBack;
    private WebView webViewVideo;
    private ProgressBar videoLoading;
    private FirebaseFirestore db;

    public ExerciseDetailFragment() {
        // Required empty public constructor
    }

    public static ExerciseDetailFragment newInstance(String exerciseName) {
        ExerciseDetailFragment fragment = new ExerciseDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EXERCISE_NAME, exerciseName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            exerciseName = getArguments().getString(ARG_EXERCISE_NAME);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercise_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        tvTitle = view.findViewById(R.id.tv_exercise_detail_title);
        tvCategory = view.findViewById(R.id.tv_exercise_category);
        tvDescription = view.findViewById(R.id.tv_exercise_description);
        cgEquipment = view.findViewById(R.id.cg_equipment);
        btnBack = view.findViewById(R.id.btn_back);
        webViewVideo = view.findViewById(R.id.webview_video);
        videoLoading = view.findViewById(R.id.video_loading);

        setupWebView();

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        if (exerciseName != null) {
            fetchExerciseDetails(exerciseName);
        }
    }

    private void setupWebView() {
        WebSettings webSettings = webViewVideo.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        
        webViewVideo.setWebChromeClient(new WebChromeClient());
        webViewVideo.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (videoLoading != null) videoLoading.setVisibility(View.GONE);
            }
        });
    }

    private void loadVideo(String videoId) {
        if (videoId != null && !videoId.isEmpty() && !videoId.startsWith("http")) {
            // It's an ID, embed it
            String embedUrl = "https://www.youtube.com/embed/" + videoId + "?autoplay=0&rel=0";
            webViewVideo.loadUrl(embedUrl);
        } else {
            // It's a search URL or empty, try searching directly in embed or hide
            // Fallback to search query embed if needed, but for now we seeded IDs
            if (exerciseName != null) {
                String searchEmbed = "https://www.youtube.com/embed?listType=search&list=" + exerciseName + "+exercise";
                webViewVideo.loadUrl(searchEmbed);
            }
        }
    }

    private void fetchExerciseDetails(String query) {
        // First try by ID
        db.collection("exercises").document(query).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Exercise exercise = doc.toObject(Exercise.class);
                if (exercise != null) displayExercise(exercise);
            } else {
                // Fallback: search by name
                db.collection("exercises").whereEqualTo("name", query).limit(1).get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            Exercise exercise = querySnapshot.getDocuments().get(0).toObject(Exercise.class);
                            if (exercise != null) displayExercise(exercise);
                        } else {
                            Toast.makeText(getContext(), "Exercise not found", Toast.LENGTH_SHORT).show();
                        }
                    });
            }
        });
    }

    private void displayExercise(Exercise exercise) {
        tvTitle.setText(exercise.getName());
        
        String category = exercise.getMuscleGroup();
        int difficulty = exercise.getDifficulty();
        String difficultyStr = (difficulty == 1) ? "Beginner" : (difficulty == 2 ? "Intermediate" : "Advanced");
        tvCategory.setText(difficultyStr + " • " + category);

        StringBuilder content = new StringBuilder();
        if (exercise.getDescription() != null) {
            content.append(exercise.getDescription()).append("\n\n");
        }
        if (exercise.getInstructions() != null) {
            content.append("Instructions:\n").append(exercise.getInstructions());
        }
        tvDescription.setText(content.toString());

        cgEquipment.removeAllViews();
        List<String> equipment = exercise.getEquipmentRequired();
        if (equipment != null && !equipment.isEmpty()) {
            for (String item : equipment) {
                Chip chip = new Chip(getContext());
                chip.setText(item);
                chip.setClickable(false);
                chip.setCheckable(false);
                chip.setTextColor(getResources().getColor(android.R.color.white));
                chip.setChipBackgroundColorResource(android.R.color.holo_blue_dark); 
                cgEquipment.addView(chip);
            }
        } else {
            Chip chip = new Chip(getContext());
            chip.setText("No Equipment");
            cgEquipment.addView(chip);
        }
        
        // Load the video
        loadVideo(exercise.getVideoUrl());
    }
}
