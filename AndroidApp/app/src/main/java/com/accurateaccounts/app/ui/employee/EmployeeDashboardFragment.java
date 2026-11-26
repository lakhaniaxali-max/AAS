package com.accurateaccounts.app.ui.employee;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.api.ApiClient;
import com.accurateaccounts.app.models.CountResponse;
import com.accurateaccounts.app.models.Task;
import com.accurateaccounts.app.ui.chat.ChatActivity;
import com.accurateaccounts.app.ui.common.FeedbackDialogHelper;
import com.accurateaccounts.app.ui.main.MainActivity;
import com.accurateaccounts.app.ui.tasks.TaskAdapter;
import com.accurateaccounts.app.ui.tasks.TasksFragment;
import com.accurateaccounts.app.utils.SessionManager;
import com.accurateaccounts.app.utils.SmoothNestedScrollView;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmployeeDashboardFragment extends Fragment {

    private SessionManager sessionManager;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvWelcome, tvTaskCount, tvNewTaskCount, tvNoTasks, tvNoAnnouncements;
    private TextView tvFeedbackCount, tvFeedbackChange, tvGuestCount, tvGuestChange;
    private ProgressBar progressBar;
    private RecyclerView rvTasks, rvAnnouncements;
    private TaskAdapter taskAdapter;
    private com.accurateaccounts.app.ui.admin.AnnouncementAdapter announcementAdapter;
    private final List<Task> taskList = new ArrayList<>();
    private final List<com.accurateaccounts.app.models.Announcement> announcementList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_employee_dashboard, container, false);

        sessionManager = new SessionManager(requireContext());

        tvWelcome = view.findViewById(R.id.tvEmployeeWelcome);
        tvTaskCount = view.findViewById(R.id.tvEmployeeTaskCount);
        tvNewTaskCount = view.findViewById(R.id.tvEmployeeNewTaskCount);
        tvNoTasks = view.findViewById(R.id.tvNoEmployeeTasks);
        tvFeedbackCount = view.findViewById(R.id.tvEmployeeFeedbackCount);
        tvFeedbackChange = view.findViewById(R.id.tvEmployeeFeedbackChange);
        tvGuestCount = view.findViewById(R.id.tvEmployeeGuestCount);
        tvGuestChange = view.findViewById(R.id.tvEmployeeGuestChange);
        progressBar = view.findViewById(R.id.pbEmployeeLoading);
        swipeRefresh = view.findViewById(R.id.swipeRefreshEmployee);
        rvTasks = view.findViewById(R.id.rvEmployeeTasks);
        rvAnnouncements = view.findViewById(R.id.rvEmployeeAnnouncements);
        tvNoAnnouncements = view.findViewById(R.id.tvNoEmployeeAnnouncements);
        SmoothNestedScrollView nestedScroll = view.findViewById(R.id.nestedScrollEmployee);

        if (swipeRefresh != null && nestedScroll != null) {
            swipeRefresh.setOnChildScrollUpCallback((parent, child) -> nestedScroll.canScrollVertically(-1));
        }

        MaterialCardView cardFeedback = view.findViewById(R.id.cardEmployeeFeedback);
        MaterialCardView cardGuest = view.findViewById(R.id.cardEmployeeGuestMessages);
        MaterialCardView cardTasks = view.findViewById(R.id.cardEmployeeTasks);
        MaterialCardView cardNewTasks = view.findViewById(R.id.cardEmployeeNewTasks);

        String name = sessionManager.getFirstName();
        if (name == null || name.isEmpty()) {
            name = sessionManager.getUsername();
        }
        tvWelcome.setText("Welcome back, " + name);

        // Setup Tasks
        rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTasks.setNestedScrollingEnabled(false);
        taskAdapter = new TaskAdapter(taskList);
        rvTasks.setAdapter(taskAdapter);

        // Setup Announcements
        rvAnnouncements.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAnnouncements.setNestedScrollingEnabled(false);
        announcementAdapter = new com.accurateaccounts.app.ui.admin.AnnouncementAdapter(announcementList);
        rvAnnouncements.setAdapter(announcementAdapter);

        // Click listeners for scrollable Client Feedback & Guest Inquiries popups
        cardFeedback.setOnClickListener(v -> FeedbackDialogHelper.showClientFeedbacksPopup(getContext()));
        cardGuest.setOnClickListener(v -> FeedbackDialogHelper.showGuestInquiriesPopup(getContext()));

        cardTasks.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new TasksFragment(), true);
            }
        });

        if (cardNewTasks != null) {
            cardNewTasks.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadFragment(new TasksFragment(), true);
                }
            });
        }

        view.findViewById(R.id.tvEmployeeViewAllTasks).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new TasksFragment(), true);
            }
        });

        swipeRefresh.setOnRefreshListener(this::loadEmployeeData);
        loadEmployeeData();

        return view;
    }

    private void loadEmployeeData() {
        progressBar.setVisibility(View.VISIBLE);
        int userId = sessionManager.getUserId();

        // 1. Fetch Feedback count
        ApiClient.getService().getFeedbackCount().enqueue(new Callback<CountResponse>() {
            @Override
            public void onResponse(Call<CountResponse> call, Response<CountResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvFeedbackCount.setText(String.valueOf(response.body().getCount()));
                }
            }
            @Override
            public void onFailure(Call<CountResponse> call, Throwable t) {}
        });

        // 2. Fetch Today's Feedback count
        ApiClient.getService().getYesterdayFeedbackCount().enqueue(new Callback<CountResponse>() {
            @Override
            public void onResponse(Call<CountResponse> call, Response<CountResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int todayCount = response.body().getCount();
                    tvFeedbackChange.setText(todayCount > 0 ? "+" + todayCount + " new today • Tap to view" : "Tap to view reviews");
                }
            }
            @Override
            public void onFailure(Call<CountResponse> call, Throwable t) {}
        });

        // 3. Fetch Guest Messages count
        ApiClient.getService().getGuestMessageCount().enqueue(new Callback<CountResponse>() {
            @Override
            public void onResponse(Call<CountResponse> call, Response<CountResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvGuestCount.setText(String.valueOf(response.body().getCount()));
                }
            }
            @Override
            public void onFailure(Call<CountResponse> call, Throwable t) {}
        });

        // 4. Fetch Today's Guest Messages count
        ApiClient.getService().getYesterdayGuestMessageCount().enqueue(new Callback<CountResponse>() {
            @Override
            public void onResponse(Call<CountResponse> call, Response<CountResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int todayCount = response.body().getCount();
                    tvGuestChange.setText(todayCount > 0 ? "+" + todayCount + " new today • Tap to view" : "Tap to view messages");
                }
            }
            @Override
            public void onFailure(Call<CountResponse> call, Throwable t) {}
        });

        // 5. Fetch Employee's Assigned Tasks
        Call<List<Task>> call = (userId > 0)
                ? ApiClient.getService().getUserTasks(userId)
                : ApiClient.getService().getAllTasks();

        call.enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                taskList.clear();
                int newTasksCount = 0;
                if (response.isSuccessful() && response.body() != null) {
                    for (Task t : response.body()) {
                        boolean isNew = com.accurateaccounts.app.utils.TaskPreferenceManager.isTaskNew(getContext(), userId, t.getTaskId());
                        t.setNew(isNew);
                        if (isNew) {
                            newTasksCount++;
                        }
                        taskList.add(t);
                    }
                }
                tvTaskCount.setText(String.valueOf(taskList.size()));
                if (tvNewTaskCount != null) {
                    tvNewTaskCount.setText(String.valueOf(newTasksCount));
                    if (newTasksCount > 0) {
                        tvNewTaskCount.setTextColor(android.graphics.Color.parseColor("#EF4444"));
                    } else {
                        tvNewTaskCount.setTextColor(android.graphics.Color.parseColor("#94A3B8"));
                    }
                }
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateTaskBadge(newTasksCount);
                }
                tvNoTasks.setVisibility(taskList.isEmpty() ? View.VISIBLE : View.GONE);
                taskAdapter.setCurrentUserId(userId);
                taskAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                tvNoTasks.setVisibility(taskList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        // 6. Fetch Announcements for Staff
        ApiClient.getService().getAnnouncements().enqueue(new Callback<List<com.accurateaccounts.app.models.Announcement>>() {
            @Override
            public void onResponse(Call<List<com.accurateaccounts.app.models.Announcement>> call, Response<List<com.accurateaccounts.app.models.Announcement>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    announcementList.clear();
                    announcementList.addAll(response.body());
                    announcementAdapter.notifyDataSetChanged();
                    if (tvNoAnnouncements != null) {
                        tvNoAnnouncements.setVisibility(announcementList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                } else if (tvNoAnnouncements != null) {
                    tvNoAnnouncements.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<com.accurateaccounts.app.models.Announcement>> call, Throwable t) {
                if (tvNoAnnouncements != null) {
                    tvNoAnnouncements.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
