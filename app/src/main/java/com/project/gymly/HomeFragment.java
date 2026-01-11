package com.project.gymly;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class HomeFragment extends Fragment {
private TextView tvMotivation;
private FirebaseFirestore db;

@Nullable
@Override
public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
return inflater.inflate(R.layout.fragment_home, container, false);
}

@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
super.onViewCreated(view, savedInstanceState);
tvMotivation = view.findViewById(R.id.tv_motivation);
db = FirebaseFirestore.getInstance();
fetchMotivation();
}

private void fetchMotivation() {
db.collection("motivationMessages").get().addOnCompleteListener(task -> {
if (task.isSuccessful() && !task.getResult().isEmpty()) {
for (QueryDocumentSnapshot document : task.getResult()) {
tvMotivation.setText(document.getString("text"));
break;
}
}
});
}
}