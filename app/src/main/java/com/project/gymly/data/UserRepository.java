package com.project.gymly.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class UserRepository {

    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;
    private static UserRepository instance;

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    public void registerUser(String email, String password, final Map<String, Object> user, final AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        String userId = firebaseUser.getUid();
                        db.collection("users").document(userId)
                                .set(user)
                                .addOnSuccessListener(aVoid -> callback.onSuccess(task))
                                .addOnFailureListener(callback::onError);
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }

    public void loginUser(String email, String password, final AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(task);
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }

    public void getUser(String userId, final UserCallback callback) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(callback::onError);
    }

    public void updateUserProfile(String userId, Map<String, Object> updates, final UpdateCallback callback) {
        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    public void signOut() {
        mAuth.signOut();
    }

    public interface AuthCallback {
        void onSuccess(Task<AuthResult> task);
        void onError(Exception e);
    }

    public interface UserCallback {
        void onSuccess(DocumentSnapshot document);
        void onError(Exception e);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onError(Exception e);
    }
}