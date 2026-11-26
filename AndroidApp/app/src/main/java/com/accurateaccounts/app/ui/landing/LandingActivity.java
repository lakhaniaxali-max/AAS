package com.accurateaccounts.app.ui.landing;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.ui.auth.LoginActivity;
import com.accurateaccounts.app.ui.main.MainActivity;
import com.accurateaccounts.app.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

public class LandingActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        // If already logged in, redirect straight to their role-based dashboard
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_landing);

        MaterialButton btnSignIn = findViewById(R.id.btnNavSignIn);
        btnSignIn.setOnClickListener(v -> {
            startActivity(new Intent(LandingActivity.this, LoginActivity.class));
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_landing);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbarLanding);

        // Load Home fragment initially
        loadFragment(new HomeLandingFragment());
        if (toolbar != null) {
            toolbar.setNavigationIcon(null);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_landing_home) {
                loadFragment(new HomeLandingFragment());
                if (toolbar != null) {
                    toolbar.setNavigationIcon(null);
                }
                return true;
            } else if (id == R.id.nav_landing_about) {
                loadFragment(new AboutFragment());
                if (toolbar != null) {
                    toolbar.setNavigationIcon(null);
                }
                return true;
            } else if (id == R.id.nav_landing_contact) {
                loadFragment(new ContactFragment());
                if (toolbar != null) {
                    toolbar.setNavigationIcon(null);
                }
                return true;
            } else if (id == R.id.nav_landing_faq) {
                loadFragment(new FaqFragment());
                if (toolbar != null) {
                    toolbar.setNavigationIcon(null);
                }
                return true;
            }
            return false;
        });
    }

    @Override
    public void onBackPressed() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_landing);
        if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.nav_landing_home) {
            bottomNav.setSelectedItemId(R.id.nav_landing_home);
        } else {
            super.onBackPressed();
        }
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container_landing, fragment)
                .commit();
    }
}

