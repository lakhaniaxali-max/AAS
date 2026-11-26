package com.accurateaccounts.app.ui.tasks;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
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
import com.accurateaccounts.app.models.CreateTaskRequest;
import com.accurateaccounts.app.models.Task;
import com.accurateaccounts.app.models.User;
import com.accurateaccounts.app.ui.main.MainActivity;
import com.accurateaccounts.app.utils.SessionManager;
import com.accurateaccounts.app.utils.TaskPreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TasksFragment extends Fragment {

    private RecyclerView rvTasks;
    private TaskAdapter taskAdapter;
    private final List<Task> taskList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private MaterialButton btnAssignTask;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        rvTasks = view.findViewById(R.id.rvTasks);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        btnAssignTask = view.findViewById(R.id.btnAssignTask);

        sessionManager = new SessionManager(requireContext());

        // Strict Role Check: Only Admin can assign tasks; Employees and Clients CANNOT assign tasks
        if ("admin".equalsIgnoreCase(sessionManager.getUserRole())) {
            btnAssignTask.setVisibility(View.VISIBLE);
            btnAssignTask.setOnClickListener(v -> showAssignTaskDialog());
        } else {
            btnAssignTask.setVisibility(View.GONE);
        }

        rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        taskAdapter = new TaskAdapter(taskList);
        rvTasks.setAdapter(taskAdapter);

        if (swipeRefresh != null && rvTasks != null) {
            swipeRefresh.setOnChildScrollUpCallback((parent, child) -> rvTasks.canScrollVertically(-1));
        }

        swipeRefresh.setOnRefreshListener(this::fetchTasks);

        fetchTasks();

        return view;
    }

    private void showAssignTaskDialog() {
        if (getContext() == null) return;

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_assign_task, null);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageButton btnBack = dialogView.findViewById(R.id.btnAssignTaskBack);
        TextInputLayout tilTaskName = dialogView.findViewById(R.id.tilTaskName);
        TextInputEditText etTaskName = dialogView.findViewById(R.id.etTaskName);
        Spinner spinnerAssignTo = dialogView.findViewById(R.id.spinnerAssignTo);
        RadioGroup rgPriority = dialogView.findViewById(R.id.rgTaskPriority);
        TextInputLayout tilDueDate = dialogView.findViewById(R.id.tilTaskDueDate);
        TextInputEditText etDueDate = dialogView.findViewById(R.id.etTaskDueDate);
        ProgressBar pbAssign = dialogView.findViewById(R.id.pbAssignTask);
        MaterialButton btnSubmit = dialogView.findViewById(R.id.btnSubmitAssignTask);

        btnBack.setOnClickListener(v -> dialog.dismiss());

        // Default due date = tomorrow
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 2);
        String defaultDate = String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        etDueDate.setText(defaultDate);

        etDueDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (dp, year, month, dayOfMonth) -> {
                String picked = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                etDueDate.setText(picked);
                tilDueDate.setError(null);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        etTaskName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { tilTaskName.setError(null); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Populate Employee/Staff spinner from live database
        List<User> assignableUsers = new ArrayList<>();
        List<String> userLabels = new ArrayList<>();
        userLabels.add("Loading staff...");
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, userLabels);
        spinnerAssignTo.setAdapter(spinnerAdapter);

        ApiClient.getService().getAllUsers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (!isAdded()) return;
                assignableUsers.clear();
                userLabels.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (User u : response.body()) {
                        if ("employee".equalsIgnoreCase(u.getRole()) || "admin".equalsIgnoreCase(u.getRole())) {
                            assignableUsers.add(u);
                            userLabels.add(u.getDisplayName() + " (@" + u.getUsername() + ")");
                        }
                    }
                }
                if (userLabels.isEmpty()) {
                    userLabels.add("No staff available");
                }
                spinnerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                if (!isAdded()) return;
                userLabels.clear();
                userLabels.add("Failed to load staff");
                spinnerAdapter.notifyDataSetChanged();
            }
        });

        btnSubmit.setOnClickListener(v -> {
            String taskName = etTaskName.getText() != null ? etTaskName.getText().toString().trim() : "";
            String dueDate = etDueDate.getText() != null ? etDueDate.getText().toString().trim() : "";

            boolean hasError = false;
            if (taskName.isEmpty()) {
                tilTaskName.setError("Task title is required");
                hasError = true;
            }
            if (dueDate.isEmpty()) {
                tilDueDate.setError("Due date is required");
                hasError = true;
            }
            if (hasError) return;

            Integer assignedToId = null;
            int selectedPos = spinnerAssignTo.getSelectedItemPosition();
            if (selectedPos >= 0 && selectedPos < assignableUsers.size()) {
                assignedToId = assignableUsers.get(selectedPos).getUserId();
            }

            String priority = "Medium";
            int checkedId = rgPriority.getCheckedRadioButtonId();
            if (checkedId == R.id.rbPriorityHigh) priority = "High";
            else if (checkedId == R.id.rbPriorityLow) priority = "Low";

            int assignedBy = sessionManager.getUserId() > 0 ? sessionManager.getUserId() : 1;

            pbAssign.setVisibility(View.VISIBLE);
            btnSubmit.setEnabled(false);

            CreateTaskRequest req = new CreateTaskRequest(taskName, assignedBy, assignedToId, dueDate, "Pending", priority);
            ApiClient.getService().createTask(req).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    pbAssign.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Task assigned successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        fetchTasks();
                    } else {
                        tilTaskName.setError("Failed to assign task. Please try again.");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    pbAssign.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    tilTaskName.setError("Network error. Please check your connection.");
                }
            });
        });

        dialog.show();
    }

    private void fetchTasks() {
        progressBar.setVisibility(View.VISIBLE);

        Call<List<Task>> call;
        if ("admin".equalsIgnoreCase(sessionManager.getUserRole())) {
            call = ApiClient.getService().getAllTasks();
        } else {
            call = ApiClient.getService().getUserTasks(sessionManager.getUserId());
        }

        call.enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    taskList.clear();
                    int userId = sessionManager.getUserId();
                    taskAdapter.setCurrentUserId(userId);
                    for (Task t : response.body()) {
                        boolean isNew = TaskPreferenceManager.isTaskNew(getContext(), userId, t.getTaskId());
                        t.setNew(isNew);
                        taskList.add(t);
                    }
                    taskAdapter.notifyDataSetChanged();

                    tvEmpty.setVisibility(taskList.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(getContext(), "Failed to load tasks", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        markAllCurrentTasksSeen();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        markAllCurrentTasksSeen();
    }

    private void markAllCurrentTasksSeen() {
        if (getContext() == null || sessionManager == null || taskList.isEmpty()) return;
        int userId = sessionManager.getUserId();
        List<Integer> ids = new ArrayList<>();
        for (Task t : taskList) {
            ids.add(t.getTaskId());
        }
        TaskPreferenceManager.markAllTasksAsSeen(getContext(), userId, ids);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).clearTaskBadge();
        }
    }
}
