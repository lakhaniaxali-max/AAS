package com.accurateaccounts.app.ui.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.ui.admin.AdminDashboardFragment;
import com.accurateaccounts.app.ui.admin.AdminDocumentsFragment;
import com.accurateaccounts.app.ui.admin.ManageUsersFragment;
import com.accurateaccounts.app.ui.auth.LoginActivity;
import com.accurateaccounts.app.ui.client.ClientDashboardFragment;
import com.accurateaccounts.app.ui.client.ClientDocumentsFragment;
import com.accurateaccounts.app.ui.employee.EmployeeDashboardFragment;
import com.accurateaccounts.app.ui.profile.ProfileFragment;
import com.accurateaccounts.app.ui.tasks.TasksFragment;
import com.accurateaccounts.app.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        String role = sessionManager.getUserRole();
        updateToolbarTitle(role);

        bottomNav = findViewById(R.id.bottom_navigation);

        // Role-based Bottom Navigation:
        // Admin: Dashboard | Tasks | Users | Documents | Profile
        // Employees: Dashboard | Tasks | Documents | Profile (Users hidden - only Admin can manage users)
        // Clients: Dashboard | Documents | Profile (Tasks & Users hidden)
        if ("admin".equalsIgnoreCase(role)) {
            if (bottomNav.getMenu().findItem(R.id.nav_tasks) != null) {
                bottomNav.getMenu().findItem(R.id.nav_tasks).setVisible(true);
            }
            if (bottomNav.getMenu().findItem(R.id.nav_users) != null) {
                bottomNav.getMenu().findItem(R.id.nav_users).setVisible(true);
            }
            if (bottomNav.getMenu().findItem(R.id.nav_documents) != null) {
                bottomNav.getMenu().findItem(R.id.nav_documents).setVisible(true);
            }
        } else if ("employee".equalsIgnoreCase(role)) {
            if (bottomNav.getMenu().findItem(R.id.nav_tasks) != null) {
                bottomNav.getMenu().findItem(R.id.nav_tasks).setVisible(true);
            }
            if (bottomNav.getMenu().findItem(R.id.nav_users) != null) {
                bottomNav.getMenu().findItem(R.id.nav_users).setVisible(false);
            }
            if (bottomNav.getMenu().findItem(R.id.nav_documents) != null) {
                bottomNav.getMenu().findItem(R.id.nav_documents).setVisible(true);
            }
        } else {
            // Client
            if (bottomNav.getMenu().findItem(R.id.nav_tasks) != null) {
                bottomNav.getMenu().findItem(R.id.nav_tasks).setVisible(false);
            }
            if (bottomNav.getMenu().findItem(R.id.nav_users) != null) {
                bottomNav.getMenu().findItem(R.id.nav_users).setVisible(false);
            }
            if (bottomNav.getMenu().findItem(R.id.nav_documents) != null) {
                bottomNav.getMenu().findItem(R.id.nav_documents).setVisible(true);
            }
        }

        // Listen for backstack changes to show/hide back arrow
        getSupportFragmentManager().addOnBackStackChangedListener(this::updateToolbarNavigationState);

        // Role-based initial fragment router
        loadRoleDashboard(role);

        // Check for any unread/new tasks for current user and show red dot badge
        checkUnreadTasks();

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // Clear any back stack when navigating via bottom bar
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }

            if (id == R.id.nav_dashboard) {
                loadRoleDashboard(role);
                return true;
            } else if (id == R.id.nav_tasks) {
                clearTaskBadge();
                loadFragment(new TasksFragment(), false);
                toolbar.setTitle("Tasks & Deadlines");
                toolbar.setSubtitle(null);
                toolbar.setNavigationIcon(null);
                return true;
            } else if (id == R.id.nav_users) {
                loadFragment(new ManageUsersFragment(), false);
                toolbar.setTitle("User & Role Management");
                toolbar.setSubtitle(null);
                toolbar.setNavigationIcon(null);
                return true;
            } else if (id == R.id.nav_documents) {
                if ("client".equalsIgnoreCase(role)) {
                    loadFragment(new ClientDocumentsFragment(), false);
                    toolbar.setTitle("Financial Documents");
                } else {
                    loadFragment(new AdminDocumentsFragment(), false);
                    toolbar.setTitle("Client Documents");
                }
                toolbar.setSubtitle(null);
                toolbar.setNavigationIcon(null);
                return true;
            } else if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment(), false);
                toolbar.setTitle("My Account Profile");
                toolbar.setSubtitle("@" + sessionManager.getUsername());
                toolbar.setNavigationIcon(null);
                return true;
            }
            return false;
        });
    }

    public void updateToolbarNavigationState() {
        int count = getSupportFragmentManager().getBackStackEntryCount();
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (count > 0) {
            // Show Back Arrow for sub-pages
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());

            if (current instanceof ManageUsersFragment) {
                toolbar.setTitle("User & Role Management");
                toolbar.setSubtitle(null);
            } else if (current instanceof AdminDocumentsFragment) {
                toolbar.setTitle("Client Documents");
                toolbar.setSubtitle(null);
            } else if (current instanceof TasksFragment) {
                toolbar.setTitle("Tasks & Deadlines");
                toolbar.setSubtitle("Assigned work");
            }
        } else {
            // Root pages (Dashboard / main tabs) have no back arrow
            toolbar.setNavigationIcon(null);
            if (current instanceof AdminDashboardFragment ||
                current instanceof EmployeeDashboardFragment ||
                current instanceof ClientDashboardFragment) {
                updateToolbarTitle(sessionManager.getUserRole());
            } else if (current instanceof ManageUsersFragment) {
                toolbar.setTitle("User & Role Management");
                toolbar.setSubtitle(null);
            } else if (current instanceof AdminDocumentsFragment || current instanceof ClientDocumentsFragment) {
                toolbar.setTitle("Client Documents");
                toolbar.setSubtitle(null);
            } else if (current instanceof TasksFragment) {
                toolbar.setTitle("Tasks & Deadlines");
                toolbar.setSubtitle(null);
            } else if (current instanceof ProfileFragment) {
                toolbar.setTitle("My Account Profile");
                toolbar.setSubtitle("@" + sessionManager.getUsername());
            }
        }
    }

    private void updateToolbarTitle(String role) {
        if ("admin".equalsIgnoreCase(role)) {
            toolbar.setTitle("AAS • Admin Dashboard");
        } else if ("employee".equalsIgnoreCase(role)) {
            toolbar.setTitle("AAS • Staff Portal");
        } else if ("client".equalsIgnoreCase(role)) {
            toolbar.setTitle("AAS • Client Portal");
        } else {
            toolbar.setTitle("Accurate Account Services");
        }
        toolbar.setSubtitle("@" + sessionManager.getUsername());
    }

    public void loadRoleDashboard(String role) {
        if ("admin".equalsIgnoreCase(role)) {
            loadFragment(new AdminDashboardFragment(), false);
        } else if ("employee".equalsIgnoreCase(role)) {
            loadFragment(new EmployeeDashboardFragment(), false);
        } else if ("client".equalsIgnoreCase(role)) {
            loadFragment(new ClientDashboardFragment(), false);
        } else {
            loadFragment(new TasksFragment(), false);
        }
        toolbar.setNavigationIcon(null);
        updateToolbarTitle(role);
    }

    public void loadFragment(Fragment fragment) {
        loadFragment(fragment, false);
    }

    public void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();

        // Update toolbar navigation icon after transaction completes
        toolbar.post(this::updateToolbarNavigationState);
    }

    @Override
    public void onBackPressed() {
        if (getOnBackPressedDispatcher().hasEnabledCallbacks()) {
            getOnBackPressedDispatcher().onBackPressed();
            return;
        }
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.nav_dashboard) {
                bottomNav.setSelectedItemId(R.id.nav_dashboard);
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUnreadTasks();
    }

    public void updateTaskBadge(int newTasksCount) {
        if (bottomNav == null) return;
        if (newTasksCount > 0) {
            com.google.android.material.badge.BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_tasks);
            badge.setBackgroundColor(android.graphics.Color.parseColor("#EF4444"));
            badge.setVisible(true);
            badge.clearNumber();
        } else {
            bottomNav.removeBadge(R.id.nav_tasks);
        }
    }

    public void clearTaskBadge() {
        if (bottomNav != null) {
            bottomNav.removeBadge(R.id.nav_tasks);
        }
    }

    public void checkUnreadTasks() {
        if (sessionManager == null) return;
        int userId = sessionManager.getUserId();
        String role = sessionManager.getUserRole();
        if (userId <= 0 || "client".equalsIgnoreCase(role)) {
            clearTaskBadge();
            return;
        }

        retrofit2.Call<java.util.List<com.accurateaccounts.app.models.Task>> call = "admin".equalsIgnoreCase(role)
                ? com.accurateaccounts.app.api.ApiClient.getService().getAllTasks()
                : com.accurateaccounts.app.api.ApiClient.getService().getUserTasks(userId);

        call.enqueue(new retrofit2.Callback<java.util.List<com.accurateaccounts.app.models.Task>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.accurateaccounts.app.models.Task>> call, retrofit2.Response<java.util.List<com.accurateaccounts.app.models.Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int unseen = 0;
                    for (com.accurateaccounts.app.models.Task t : response.body()) {
                        if (com.accurateaccounts.app.utils.TaskPreferenceManager.isTaskNew(MainActivity.this, userId, t.getTaskId())) {
                            unseen++;
                        }
                    }
                    updateTaskBadge(unseen);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.accurateaccounts.app.models.Task>> call, Throwable t) {}
        });
    }
}
