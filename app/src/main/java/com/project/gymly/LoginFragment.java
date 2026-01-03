package com.project.gymly;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginFragment extends Fragment {

    private TextInputEditText etEmail;
    private Button btnLogin;
    private FirebaseFirestore db;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        db = FirebaseFirestore.getInstance();
        etEmail = view.findViewById(R.id.et_login_email);
        btnLogin = view.findViewById(R.id.btn_perform_login);

        btnLogin.setOnClickListener(v -> performLogin());
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(getContext(), "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Query Firestore to see if user exists by email
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // User found
                        String userId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        String userName = queryDocumentSnapshots.getDocuments().get(0).getString("name");
                        
                        Toast.makeText(getContext(), "Welcome back, " + userName + "!", Toast.LENGTH_SHORT).show();
                        
                        // Save login state
                        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                        prefs.edit().putString("current_user_id", userId).apply();
                        
                        // Navigate to UserWorkoutsFragment
                         getParentFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new UserWorkoutsFragment())
                                .commit();
                    } else {
                        Toast.makeText(getContext(), "User not found. Please Sign Up.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error logging in: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}