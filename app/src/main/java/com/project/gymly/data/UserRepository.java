package com.project.gymly.data;

import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {
    private static final String TAG = "Gymly_Firestore";
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;
    private static UserRepository instance;

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
        
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        this.db.setFirestoreSettings(settings);
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    public void testConnection(SimpleCallback callback) {
        db.collection("_connection_test_").document("test").get()
                .addOnSuccessListener(doc -> {
                    Log.d(TAG, "Firestore connection: SUCCESS");
                    callback.onResponse(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore connection: FAILED", e);
                    callback.onResponse(false);
                });
    }

    public void registerUser(String email, String password, final Map<String, Object> userData, final AuthCallback callback) {
        Log.d(TAG, "AUTH: Creating user " + email);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            saveUserToFirestore(userId, userData, task, callback);
                        }
                    } else {
                        Log.e(TAG, "AUTH FAILED", task.getException());
                        callback.onError(task.getException());
                    }
                });
    }

    private void saveUserToFirestore(String userId, Map<String, Object> userData, Task<AuthResult> authTask, AuthCallback callback) {
        Log.d(TAG, "FIRESTORE: Saving profile for " + userId);
        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "FIRESTORE: Profile saved.");
                    createInitialWorkoutPlan(userId, authTask, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "FIRESTORE FAILED", e);
                    callback.onError(e);
                });
    }

    private void createInitialWorkoutPlan(String userId, Task<AuthResult> authTask, AuthCallback callback) {
        // Create a basic 4-week active plan
        Map<String, Object> plan = new HashMap<>();
        plan.put("title", "Standard Kickstart");
        plan.put("isActive", true);
        plan.put("durationWeeks", 4);
        plan.put("startDate", FieldValue.serverTimestamp());
        
        Map<String, Object> schedule = new HashMap<>();
        for (int w = 1; w <= 4; w++) {
            Map<String, Map<String, Object>> weekDays = new HashMap<>();
            
            // Monday: Chest & Shoulders
            weekDays.put("mon", createWorkout("Chest & Shoulders", 30, Arrays.asList(
                    createWorkoutStep("pushups_basic", 3, 12),
                    createWorkoutStep("shoulder_press_dumbbells", 3, 10)
            )));
            
            // Wednesday: Legs
            weekDays.put("wed", createWorkout("Leg Foundations", 30, Arrays.asList(
                    createWorkoutStep("squats_bodyweight", 3, 15),
                    createWorkoutStep("lunges_forward", 3, 10)
            )));
            
            // Friday: Core & Cardio
            weekDays.put("fri", createWorkout("Core Burn", 20, Arrays.asList(
                    createWorkoutStep("plank_basic", 3, 30),
                    createWorkoutStep("mountain_climbers", 3, 15)
            )));
            
            schedule.put(String.valueOf(w), weekDays);
        }
        plan.put("schedule", schedule);

        db.collection("users").document(userId).collection("plans").add(plan)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Initial plan created in subcollection.");
                    callback.onSuccess(authTask);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create initial plan", e);
                    callback.onSuccess(authTask); // Continue anyway
                });
    }

    private Map<String, Object> createWorkout(String name, int duration, List<Map<String, Object>> steps) {
        Map<String, Object> workout = new HashMap<>();
        workout.put("name", name);
        workout.put("duration", duration);
        workout.put("exercises", steps);
        workout.put("isCompleted", false); // Default status
        return workout;
    }

    private Map<String, Object> createWorkoutStep(String exerciseId, int sets, int reps) {
        Map<String, Object> step = new HashMap<>();
        step.put("exerciseId", exerciseId);
        step.put("sets", sets);
        step.put("reps", reps);
        return step;
    }

    public void loginUser(String email, String password, final AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) callback.onSuccess(task);
                    else callback.onError(task.getException());
                });
    }

    public void getUser(String userId, final UserCallback callback) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(callback::onError);
    }

    public void updateUserProfile(String userId, Map<String, Object> updates, final UpdateCallback callback) {
        db.collection("users").document(userId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void completeWorkout(String userId, String planId, int week, String dayKey, boolean isCompleted, UpdateCallback callback) {
        if (planId == null || planId.isEmpty()) {
            callback.onError(new Exception("Invalid plan ID"));
            return;
        }

        // Direct update using known plan ID - No query required!
        String fieldPath = "schedule." + week + "." + dayKey + ".isCompleted";
        
        db.collection("users").document(userId).collection("plans").document(planId)
            .update(fieldPath, isCompleted)
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(callback::onError);
    }

    public FirebaseUser getCurrentUser() { return mAuth.getCurrentUser(); }
    public boolean isUserLoggedIn() { return mAuth.getCurrentUser() != null; }
    public void signOut() { mAuth.signOut(); }

    public interface AuthCallback { void onSuccess(Task<AuthResult> task); void onError(Exception e); }
    public interface UserCallback { void onSuccess(DocumentSnapshot document); void onError(Exception e); }
    public interface UpdateCallback { void onSuccess(); void onError(Exception e); }
    public interface SimpleCallback { void onResponse(boolean success); }
}
