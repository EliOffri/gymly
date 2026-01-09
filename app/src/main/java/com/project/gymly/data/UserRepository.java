package com.project.gymly.data;

import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class UserRepository {
    private static final String TAG = "Gymly_Firestore";
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;
    private static UserRepository instance;

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
        
        // Ensure Firestore is configured for better reliability
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

    /**
     * Verifies if Firestore is reachable. Use this to debug "Network Errors".
     */
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
        Map<String, Object> initialPlan = new HashMap<>();
        initialPlan.put("sun", Arrays.asList("pushups_basic", "jumping_jacks"));
        initialPlan.put("mon", Arrays.asList());
        initialPlan.put("tue", Arrays.asList("squats_bodyweight", "lunges_forward"));
        initialPlan.put("wed", Arrays.asList());
        initialPlan.put("thu", Arrays.asList("plank_basic", "mountain_climbers"));
        initialPlan.put("fri", Arrays.asList());
        initialPlan.put("sat", Arrays.asList("glute_bridge", "dead_bug_core"));

        db.collection("weeklyPlans").document(userId).set(initialPlan)
                .addOnSuccessListener(aVoid -> callback.onSuccess(authTask))
                .addOnFailureListener(e -> callback.onSuccess(authTask)); 
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
    public interface SimpleCallback { void onResponse(boolean success); }
}
