package com.project.gymly;

import android.os.Bundle;
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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.project.gymly.data.UserRepository;

public class LoginFragment extends Fragment {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private UserRepository userRepository;

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

        userRepository = UserRepository.getInstance();

        tilEmail = view.findViewById(R.id.til_login_email);
        etEmail = view.findViewById(R.id.et_login_email);
        tilPassword = view.findViewById(R.id.til_login_password);
        etPassword = view.findViewById(R.id.et_login_password);
        btnLogin = view.findViewById(R.id.btn_perform_login);
        progressBar = view.findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> performLogin());
    }

    private void performLogin() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            tilEmail.setError("Please enter your email");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            return;
        }

        if (password.isEmpty()) {
            tilPassword.setError("Please enter your password");
            return;
        }

        setLoading(true);

        userRepository.loginUser(email, password, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(Task<AuthResult> task) {
                setLoading(false);
                Toast.makeText(getContext(), "Welcome back!", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new UserWorkoutsFragment())
                        .commit();
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                if (e instanceof FirebaseAuthInvalidUserException) {
                    tilEmail.setError("User with this email does not exist.");
                } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    tilPassword.setError("Incorrect password.");
                } else {
                    Toast.makeText(getContext(), "Authentication failed.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
        }
    }
}