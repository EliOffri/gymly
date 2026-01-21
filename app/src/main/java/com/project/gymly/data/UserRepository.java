package com.project.gymly.data;

import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class UserRepository {
    private static final String TAG = "UserRepository_Debug";
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;
    private static UserRepository instance;

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) instance = new UserRepository();
        return instance;
    }

    public void registerUser(String email, String password, final Map<String, Object> userData, final AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            saveUserToFirestore(firebaseUser.getUid(), userData, task, callback);
                        }
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }

    private void saveUserToFirestore(String userId, Map<String, Object> userData, Task<AuthResult> authTask, AuthCallback callback) {
        db.collection("users").document(userId).set(userData)
                .addOnSuccessListener(aVoid -> createInitialWorkoutPlan(userId, authTask, callback))
                .addOnFailureListener(callback::onError);
    }

    private void createInitialWorkoutPlan(String userId, Task<AuthResult> authTask, AuthCallback callback) {
        Map<String, Object> plan = new HashMap<>();
        plan.put("title", "12-Week Transformation");
        plan.put("purpose", "General Fitness");
        plan.put("durationWeeks", 12);
        plan.put("isActive", true);
        plan.put("startDate", Timestamp.now());
        plan.put("totalSessions", 36);
        plan.put("completedSessions", 0);

        // Define default phases so "Journey Overview" isn't empty
        Map<String, Object> p1 = new HashMap<>();
        p1.put("title", "Foundations");
        p1.put("weeks", "1-4");
        p1.put("objective", "Building consistency and form.");
        p1.put("composition", "12 Reps / 60s Rest");
        p1.put("status", "IN PROGRESS");

        Map<String, Object> p2 = new HashMap<>();
        p2.put("title", "Strength Base");
        p2.put("weeks", "5-8");
        p2.put("objective", "Increasing resistance safely.");
        p2.put("composition", "8 Reps / 90s Rest");
        p2.put("status", "LOCKED");

        plan.put("phases", Arrays.asList(p1, p2));

        // Save to the correct path that PlanFragment expects
        db.collection("users").document(userId).collection("plans")
                .document("active_plan").set(plan)
                .addOnSuccessListener(aVoid -> callback.onSuccess(authTask))
                .addOnFailureListener(callback::onError);
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

    public FirebaseUser getCurrentUser() { return mAuth.getCurrentUser(); }
    public boolean isUserLoggedIn() { return mAuth.getCurrentUser() != null; }
    public void signOut() { mAuth.signOut(); }

    public interface AuthCallback { void onSuccess(Task<AuthResult> task); void onError(Exception e); }
    public interface UserCallback { void onSuccess(DocumentSnapshot document); void onError(Exception e); }
    public interface UpdateCallback { void onSuccess(); void onError(Exception e); }
}
