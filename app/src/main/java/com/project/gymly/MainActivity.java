package com.project.gymly;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.project.gymly.data.UserRepository;
import com.project.gymly.data.firestore.FirestoreSeeder;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private UserRepository userRepository;
    private BottomNavigationView bottomNav;
    
    private FrameLayout todayCont, planCont, libraryCont, profileCont, authCont;
    private int currentContainerId = R.id.today_container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userRepository = UserRepository.getInstance();
        FirestoreSeeder.seedIfNeeded(this);

        initViews();

        if (savedInstanceState == null) {
            if (userRepository.isUserLoggedIn()) {
                setupMainFlow();
            } else {
                setupAuthFlow();
            }
        }
    }

    private void initViews() {
        bottomNav = findViewById(R.id.bottom_navigation);
        todayCont = findViewById(R.id.today_container);
        planCont = findViewById(R.id.plan_container);
        libraryCont = findViewById(R.id.library_container);
        profileCont = findViewById(R.id.profile_container);
        authCont = findViewById(R.id.auth_container);
        
        bottomNav.setOnNavigationItemSelectedListener(navListener);
    }

    private void setupMainFlow() {
        showBottomNav(true);
        authCont.setVisibility(View.GONE);
        todayCont.setVisibility(View.VISIBLE);
        planCont.setVisibility(View.GONE);
        libraryCont.setVisibility(View.GONE);
        profileCont.setVisibility(View.GONE);
        
        FragmentManager fm = getSupportFragmentManager();
        // Clear back stack to avoid ghost fragments
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        
        // Replace instead of Add to ensure fresh instances on re-login
        fm.beginTransaction()
            .replace(R.id.today_container, new TodayFragment())
            .replace(R.id.plan_container, new PlanFragment())
            .replace(R.id.library_container, new LibraryFragment())
            .replace(R.id.profile_container, new ProfileFragment())
            .commit();
    }

    private void setupAuthFlow() {
        showBottomNav(false);
        authCont.setVisibility(View.VISIBLE);
        todayCont.setVisibility(View.GONE);
        planCont.setVisibility(View.GONE);
        libraryCont.setVisibility(View.GONE);
        profileCont.setVisibility(View.GONE);
        
        // Clear main fragments on logout
        FragmentManager fm = getSupportFragmentManager();
        Fragment today = fm.findFragmentById(R.id.today_container);
        if (today != null) fm.beginTransaction().remove(today).commit();
        
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.auth_container, new LoginFragment()).commit();
    }

    public void showBottomNav(boolean show) {
        bottomNav.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void setSelectedNavItem(int itemId) {
        bottomNav.setSelectedItemId(itemId);
    }

    public void onAuthSuccess() {
        setupMainFlow();
        setSelectedNavItem(R.id.nav_home);
    }

    public void logout() {
        userRepository.signOut();
        setupAuthFlow();
    }

    public void navigateToExerciseDetail(String exerciseId) {
        ExerciseDetailFragment fragment = ExerciseDetailFragment.newInstance(exerciseId);
        getSupportFragmentManager().beginTransaction()
                .add(currentContainerId, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void navigateToWorkoutDetail(String name, int duration, ArrayList<Bundle> steps, boolean isCompleted, String planId, int week, String dayKey) {
        WorkoutFragment fragment = WorkoutFragment.newInstance(name, duration, steps, isCompleted, planId, week, dayKey);
        getSupportFragmentManager().beginTransaction()
                .add(currentContainerId, fragment)
                .addToBackStack(null)
                .commit();
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener = item -> {
        int itemId = item.getItemId();
        
        todayCont.setVisibility(View.GONE);
        planCont.setVisibility(View.GONE);
        libraryCont.setVisibility(View.GONE);
        profileCont.setVisibility(View.GONE);

        if (itemId == R.id.nav_home) {
            currentContainerId = R.id.today_container;
            todayCont.setVisibility(View.VISIBLE);
        } else if (itemId == R.id.nav_plan) {
            currentContainerId = R.id.plan_container;
            planCont.setVisibility(View.VISIBLE);
        } else if (itemId == R.id.nav_library) {
            currentContainerId = R.id.library_container;
            libraryCont.setVisibility(View.VISIBLE);
        } else if (itemId == R.id.nav_profile) {
            currentContainerId = R.id.profile_container;
            profileCont.setVisibility(View.VISIBLE);
        }
        return true;
    };

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
