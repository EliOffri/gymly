package com.project.gymly;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
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

    private TextInputLayout tilName, tilEmail, tilPassword;
    private TextInputEditText etName, etEmail, etPassword;
    private ProgressBar progressBar;
    private Button btnSubmit;
    private UserRepository userRepository;

    public RegisterFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userRepository = UserRepository.getInstance();

        tilName = view.findViewById(R.id.til_name);
        etName = view.findViewById(R.id.et_name);
        tilEmail = view.findViewById(R.id.til_email);
        etEmail = view.findViewById(R.id.et_email);
        tilPassword = view.findViewById(R.id.til_password);
        etPassword = view.findViewById(R.id.et_password);
        progressBar = view.findViewById(R.id.progress_bar);
        btnSubmit = view.findViewById(R.id.btn_register_submit);

        btnSubmit.setOnClickListener(v -> submitRegistration());
    }

    private void submitRegistration() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean isValid = true;

        if (name.isEmpty()) {
            tilName.setError("Required");
            isValid = false;
        }
        if (email.isEmpty()) {
            tilEmail.setError("Required");
            isValid = false;
        }
        if (password.length() < 6) {
            tilPassword.setError("Min 6 chars");
            isValid = false;
        }

        if (!isValid) return;

        setLoading(true);

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("createdAt", FieldValue.serverTimestamp());

        userRepository.registerUser(email, password, userData, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(Task<AuthResult> task) {
                setLoading(false);
                if (isAdded() && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).onAuthSuccess();
                }
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                if (e instanceof FirebaseAuthUserCollisionException) {
                    tilEmail.setError("Email already in use");
                } else if (e instanceof FirebaseAuthWeakPasswordException) {
                    tilPassword.setError("Weak password");
                } else {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!isLoading);
    }
}
