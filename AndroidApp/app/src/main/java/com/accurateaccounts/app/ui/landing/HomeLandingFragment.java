package com.accurateaccounts.app.ui.landing;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.ui.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;

public class HomeLandingFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_landing, container, false);

        MaterialButton btnSignIn = view.findViewById(R.id.btnHeroSignIn);
        btnSignIn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), LoginActivity.class));
        });

        return view;
    }
}

