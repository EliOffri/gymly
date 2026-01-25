package com.project.gymly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * @deprecated This fragment has been replaced by TodayFragment.
 */
@Deprecated
public class HomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Redirect to TodayFragment if this is ever accidentally opened
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new TodayFragment())
                .commit();
        return new View(getContext());
    }
}
