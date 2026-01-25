package com.project.gymly;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.project.gymly.data.UserRepository;
import com.project.gymly.data.firestore.FirestoreSeeder;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private UserRepository userRepository;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userRepository = UserRepository.getInstance();
        FirestoreSeeder.seedIfNeeded(this);

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        if (savedInstanceState == null) {
            if (userRepository.isUserLoggedIn()) {
                showBottomNav(true);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new TodayFragment())
                        .commit();
            } else {
                showBottomNav(false);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new LoginFragment())
                        .commit();
            }
        }
    }

    public void showBottomNav(boolean show) {
        if (bottomNav != null) {
            bottomNav.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public void setSelectedNavItem(int itemId) {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(itemId);
        }
    }

    public void navigateToExerciseDetail(String exerciseName) {
        ExerciseDetailFragment fragment = ExerciseDetailFragment.newInstance(exerciseName);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void navigateToWorkoutDetail(String name, int duration, ArrayList<String> exerciseIds) {
        WorkoutFragment fragment = WorkoutFragment.newInstance(name, duration, exerciseIds);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener = item -> {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            selectedFragment = new TodayFragment();
        } else if (itemId == R.id.nav_plan) {
            selectedFragment = new PlanFragment();
        } else if (itemId == R.id.nav_library) {
            selectedFragment = new LibraryFragment();
        } else if (itemId == R.id.nav_profile) {
            selectedFragment = new ProfileFragment();
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
        }

        return true;
    };
}
