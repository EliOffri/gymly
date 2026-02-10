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
import com.google.firebase.firestore.WriteBatch;

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
                .addOnSuccessListener(doc -> callback.onResponse(true))
                .addOnFailureListener(e -> callback.onResponse(false));
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
        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> callback.onSuccess(authTask))
                .addOnFailureListener(e -> callback.onError(e));
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
        String fieldPath = "schedule." + week + "." + dayKey + ".isCompleted";
        db.collection("users").document(userId).collection("plans").document(planId)
            .update(fieldPath, isCompleted)
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(callback::onError);
    }
    
    // Create new plan and deactivate old ones
    public void createCustomPlan(String userId, Map<String, Object> planData, final UpdateCallback callback) {
        // 1. Find currently active plans
        db.collection("users").document(userId).collection("plans")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    
                    // Deactivate old plans
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.update(doc.getReference(), "isActive", false);
                    }
                    
                    // Add new plan
                    batch.set(
                        db.collection("users").document(userId).collection("plans").document(),
                        planData
                    );
                    
                    // Commit atomically
                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    // Delete/End plan
    public void deletePlan(String userId, String planId, final UpdateCallback callback) {
        db.collection("users").document(userId).collection("plans").document(planId)
                .delete()
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
