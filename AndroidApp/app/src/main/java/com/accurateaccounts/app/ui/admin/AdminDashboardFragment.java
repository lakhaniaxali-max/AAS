package com.accurateaccounts.app.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.api.ApiClient;
import com.accurateaccounts.app.models.Announcement;
import com.accurateaccounts.app.models.CountResponse;
import com.accurateaccounts.app.models.Task;
import com.accurateaccounts.app.ui.common.FeedbackDialogHelper;
import com.accurateaccounts.app.ui.main.MainActivity;
import com.accurateaccounts.app.ui.tasks.TaskAdapter;
import com.accurateaccounts.app.ui.tasks.TasksFragment;
import com.accurateaccounts.app.utils.SessionManager;
import com.accurateaccounts.app.utils.SmoothNestedScrollView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardFragment extends Fragment {

    private SessionManager sessionManager;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvWelcome, tvFeedbackCount, tvFeedbackChange, tvGuestCount, tvGuestChange;
    private TextView tvNoAnnouncements, tvNoAdminTasks;
    private ProgressBar progressBar;
    private RecyclerView rvAnnouncements, rvAdminTasks;
    private AnnouncementAdapter announcementAdapter;
    private TaskAdapter taskAdapter;
    private final List<Announcement> announcementList = new ArrayList<>();
    private final List<Task> adminTaskList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        sessionManager = new SessionManager(requireContext());

        tvWelcome = view.findViewById(R.id.tvAdminWelcome);
        tvFeedbackCount = view.findViewById(R.id.tvFeedbackCount);
        tvFeedbackChange = view.findViewById(R.id.tvFeedbackChange);
        tvGuestCount = view.findViewById(R.id.tvGuestCount);
        tvGuestChange = view.findViewById(R.id.tvGuestChange);
        tvNoAnnouncements = view.findViewById(R.id.tvNoAnnouncements);
        tvNoAdminTasks = view.findViewById(R.id.tvNoAdminTasks);
        progressBar = view.findViewById(R.id.pbAdminLoading);
        swipeRefresh = view.findViewById(R.id.swipeRefreshAdmin);

        rvAnnouncements = view.findViewById(R.id.rvAnnouncements);
        rvAdminTasks = view.findViewById(R.id.rvAdminTasks);
        SmoothNestedScrollView nestedScroll = view.findViewById(R.id.nestedScrollAdmin);

        if (swipeRefresh != null && nestedScroll != null) {
            swipeRefresh.setOnChildScrollUpCallback((parent, child) -> nestedScroll.canScrollVertically(-1));
        }

        MaterialCardView cardFeedback = view.findViewById(R.id.cardFeedback);
        MaterialCardView cardGuest = view.findViewById(R.id.cardGuestMessages);
        TextView tvViewAllTasks = view.findViewById(R.id.tvViewAllTasks);

        tvWelcome.setText("Welcome back, " + sessionManager.getUsername());

        // Setup Announcements RecyclerView
        rvAnnouncements.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAnnouncements.setNestedScrollingEnabled(false);
        announcementAdapter = new AnnouncementAdapter(announcementList);
        rvAnnouncements.setAdapter(announcementAdapter);

        // Setup Tasks RecyclerView
        rvAdminTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAdminTasks.setNestedScrollingEnabled(false);
        taskAdapter = new TaskAdapter(adminTaskList);
        rvAdminTasks.setAdapter(taskAdapter);

        // Floating Action Button for Announcement (Bottom Right, Admin only)
        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabAddAnnouncement =
                view.findViewById(R.id.fabAddAnnouncement);
        if (fabAddAnnouncement != null) {
            fabAddAnnouncement.setOnClickListener(v -> showCreateAnnouncementDialog());
        }

        // Click listeners
        tvViewAllTasks.setOnClickListener(v -> navigateToTasks());

        cardFeedback.setOnClickListener(v -> FeedbackDialogHelper.showClientFeedbacksPopup(getContext()));
        cardGuest.setOnClickListener(v -> FeedbackDialogHelper.showGuestInquiriesPopup(getContext()));

        swipeRefresh.setOnRefreshListener(this::loadDashboardData);

        // Initial Load
        loadDashboardData();

        return view;
    }

    private void navigateToTasks() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(new TasksFragment(), true);
        }
    }

    private void loadDashboardData() {
        progressBar.setVisibility(View.VISIBLE);

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

        // 5. Fetch Announcements
        ApiClient.getService().getAnnouncements().enqueue(new Callback<List<Announcement>>() {
            @Override
            public void onResponse(Call<List<Announcement>> call, Response<List<Announcement>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    announcementList.clear();
                    announcementList.addAll(response.body());
                    announcementAdapter.notifyDataSetChanged();
                    tvNoAnnouncements.setVisibility(announcementList.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    tvNoAnnouncements.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Announcement>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                tvNoAnnouncements.setVisibility(View.VISIBLE);
            }
        });

        // 6. Fetch Tasks Preview
        ApiClient.getService().getAllTasks().enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adminTaskList.clear();
                    List<Task> all = response.body();
                    int userId = sessionManager.getUserId();
                    taskAdapter.setCurrentUserId(userId);
                    int unseen = 0;
                    for (Task t : all) {
                        boolean isNew = com.accurateaccounts.app.utils.TaskPreferenceManager.isTaskNew(getContext(), userId, t.getTaskId());
                        t.setNew(isNew);
                        if (isNew) unseen++;
                    }
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).updateTaskBadge(unseen);
                    }
                    // show top 3 urgent tasks
                    for (int i = 0; i < Math.min(3, all.size()); i++) {
                        adminTaskList.add(all.get(i));
                    }
                    taskAdapter.notifyDataSetChanged();
                    tvNoAdminTasks.setVisibility(adminTaskList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {}
        });
    }

    private void showCreateAnnouncementDialog() {
        if (getContext() == null) return;

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_announcement, null);
        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.65f);
        }

        com.google.android.material.textfield.TextInputEditText etTitle = dialogView.findViewById(R.id.etAnnouncementTitle);
        com.google.android.material.textfield.TextInputEditText etMessage = dialogView.findViewById(R.id.etAnnouncementMessage);
        com.google.android.material.textfield.TextInputEditText etDate = dialogView.findViewById(R.id.etAnnouncementDate);
        com.google.android.material.textfield.TextInputEditText etAuthor = dialogView.findViewById(R.id.etAnnouncementAuthor);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelAnnouncement);
        com.google.android.material.button.MaterialButton btnPost = dialogView.findViewById(R.id.btnPostAnnouncement);

        // Pre-populate date (YYYY-MM-DD)
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        etDate.setText(sdf.format(new java.util.Date()));

        // Pre-populate author
        String name = sessionManager.getFirstName();
        if (name != null && !name.trim().isEmpty()) {
            String lName = sessionManager.getLastName();
            if (lName != null && !lName.trim().isEmpty()) {
                name += " " + lName;
            }
        } else {
            name = "System Administrator";
        }
        etAuthor.setText(name + " (@" + sessionManager.getUsername() + ")");

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnPost.setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String message = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
            String date = etDate.getText() != null ? etDate.getText().toString().trim() : "";
            String author = etAuthor.getText() != null ? etAuthor.getText().toString().trim() : "";

            if (title.isEmpty()) {
                etTitle.setError("Title is required");
                etTitle.requestFocus();
                return;
            }

            if (message.isEmpty()) {
                etMessage.setError("Message details are required");
                etMessage.requestFocus();
                return;
            }

            if (date.isEmpty()) {
                date = sdf.format(new java.util.Date());
            }

            if (author.isEmpty()) {
                author = "System Admin (@" + sessionManager.getUsername() + ")";
            }

            btnPost.setEnabled(false);
            btnPost.setText("Posting...");

            int authId = sessionManager.getAuthId();
            com.accurateaccounts.app.models.CreateAnnouncementRequest req =
                    new com.accurateaccounts.app.models.CreateAnnouncementRequest(authId, title, message, author, date);

            ApiClient.getService().createAnnouncement(req).enqueue(new Callback<okhttp3.ResponseBody>() {
                @Override
                public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "📢 Announcement posted successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadDashboardData();
                    } else {
                        btnPost.setEnabled(true);
                        btnPost.setText("Post Announcement");
                        Toast.makeText(getContext(), "Failed to post announcement. Status: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                    btnPost.setEnabled(true);
                    btnPost.setText("Post Announcement");
                    Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }
}
