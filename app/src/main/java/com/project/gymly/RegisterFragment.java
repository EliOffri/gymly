// app/src/main/java/com/project/gymly/RegisterFragment.java
package com.project.gymly;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FieldValue;
import com.project.gymly.data.UserRepository;

import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private static final String TAG = "RegisterFragment";

    // Text fields
    private TextInputLayout tilName, tilEmail, tilPassword;
    private TextInputEditText etName, etEmail, etPassword;

    // Submit + loading
    private ProgressBar progressBar;
    private MaterialButton btnSubmit;

    private UserRepository userRepository;

    public RegisterFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userRepository = UserRepository.getInstance();

        initViews(view);

        btnSubmit.setOnClickListener(v -> submitRegistration());
    }

    private void initViews(View view) {
        tilName = view.findViewById(R.id.til_name);
        etName = view.findViewById(R.id.et_name);

        tilEmail = view.findViewById(R.id.til_email);
        etEmail = view.findViewById(R.id.et_email);

        tilPassword = view.findViewById(R.id.til_password);
        etPassword = view.findViewById(R.id.et_password);

        progressBar = view.findViewById(R.id.progress_bar);
        btnSubmit = view.findViewById(R.id.btn_register_submit);
    }

    private void submitRegistration() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);

        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        boolean isValid = true;

        if (name.isEmpty()) {
            if (tilName != null) tilName.setError("Please enter your name");
            isValid = false;
        }

        if (email.isEmpty()) {
            if (tilEmail != null) tilEmail.setError("Please enter your email");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (tilEmail != null) tilEmail.setError("Please enter a valid email address");
            isValid = false;
        }

        if (password.isEmpty()) {
            if (tilPassword != null) tilPassword.setError("Please enter a password");
            isValid = false;
        } else if (password.length() < 6) {
            if (tilPassword != null) tilPassword.setError("Password should be at least 6 characters");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        setLoading(true);

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("createdAt", FieldValue.serverTimestamp());

        userRepository.registerUser(email, password, userData, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(Task<AuthResult> task) {
                setLoading(false);
                if (isAdded()) {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).onAuthSuccess();
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                handleRegistrationError(e);
            }
        });
    }

    private void handleRegistrationError(Exception e) {
        if (!isAdded()) return;
        if (e instanceof FirebaseNetworkException) {
            Toast.makeText(getContext(), "Network error. Please check your internet connection.", Toast.LENGTH_LONG).show();
        } else if (e instanceof FirebaseAuthUserCollisionException) {
            if (tilEmail != null) tilEmail.setError("This email is already registered.");
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            if (tilPassword != null) tilPassword.setError("The password is too weak.");
        } else {
            Toast.makeText(getContext(), "Registration failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
        Log.e(TAG, "Registration error", e);
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (btnSubmit != null) btnSubmit.setEnabled(!isLoading);
        if (etName != null) etName.setEnabled(!isLoading);
        if (etEmail != null) etEmail.setEnabled(!isLoading);
        if (etPassword != null) etPassword.setEnabled(!isLoading);
    }
}
