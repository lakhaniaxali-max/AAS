package com.accurateaccounts.app.ui.tasks;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.accurateaccounts.app.R;
import com.accurateaccounts.app.api.ApiClient;
import com.accurateaccounts.app.models.Task;
import com.accurateaccounts.app.ui.main.MainActivity;
import com.accurateaccounts.app.utils.TaskPreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskStatusChangeListener {
        void onStatusChanged(Task task, String newStatus, int position);
    }

    private final List<Task> tasks;
    private OnTaskStatusChangeListener statusChangeListener;
    private int currentUserId = -1;

    public void setCurrentUserId(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    public TaskAdapter(List<Task> tasks) {
        this.tasks = tasks;
    }

    public TaskAdapter(List<Task> tasks, OnTaskStatusChangeListener listener) {
        this.tasks = tasks;
        this.statusChangeListener = listener;
    }

    public void setOnTaskStatusChangeListener(OnTaskStatusChangeListener listener) {
        this.statusChangeListener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        Context context = holder.itemView.getContext();
        float density = context.getResources().getDisplayMetrics().density;

        holder.tvTaskName.setText(task.getTaskName());

        // Status badge styling (No emojis)
        String status = task.getStatus() != null ? task.getStatus().trim() : "Pending";
        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setCornerRadius(6 * density);

        if ("Completed".equalsIgnoreCase(status)) {
            statusBg.setColor(Color.parseColor("#DCFCE7"));
            holder.tvStatus.setTextColor(Color.parseColor("#15803D"));
            holder.tvStatus.setText("Completed");
        } else if ("In Progress".equalsIgnoreCase(status) || "In-Progress".equalsIgnoreCase(status)) {
            statusBg.setColor(Color.parseColor("#DBEAFE"));
            holder.tvStatus.setTextColor(Color.parseColor("#1D4ED8"));
            holder.tvStatus.setText("In Progress");
        } else {
            statusBg.setColor(Color.parseColor("#FEF3C7"));
            holder.tvStatus.setTextColor(Color.parseColor("#B45309"));
            holder.tvStatus.setText("Pending");
        }
        holder.tvStatus.setBackground(statusBg);

        // New Task Badge ("1" in red pill badge)
        if (task.isNew()) {
            holder.tvNewBadge.setVisibility(View.VISIBLE);
            holder.tvNewBadge.setText("1");
        } else {
            holder.tvNewBadge.setVisibility(View.GONE);
        }

        // Priority badge styling (No emojis)
        String priority = task.getPriority() != null ? task.getPriority().trim() : "Medium";
        GradientDrawable prioBg = new GradientDrawable();
        prioBg.setCornerRadius(6 * density);

        if ("High".equalsIgnoreCase(priority)) {
            prioBg.setColor(Color.parseColor("#FEE2E2"));
            holder.tvPriority.setTextColor(Color.parseColor("#DC2626"));
            holder.tvPriority.setText("High");
        } else if ("Low".equalsIgnoreCase(priority)) {
            prioBg.setColor(Color.parseColor("#F1F5F9"));
            holder.tvPriority.setTextColor(Color.parseColor("#475569"));
            holder.tvPriority.setText("Low");
        } else {
            prioBg.setColor(Color.parseColor("#FEF3C7"));
            holder.tvPriority.setTextColor(Color.parseColor("#D97706"));
            holder.tvPriority.setText("Medium");
        }
        holder.tvPriority.setBackground(prioBg);

        // Due Date: Clean format, no emojis
        String formattedDate = formatDueDate(task.getDueDate());
        holder.tvDueDate.setText("Due: " + formattedDate);

        // Assignee: Show full name (e.g. "Assigned to: System Admin", "Assigned to: Axali Lakhani")
        String assignedName = task.getFullAssignedToName();
        if (assignedName == null || assignedName.trim().isEmpty()) {
            assignedName = task.getAssignedToName();
        }

        if (assignedName != null && !assignedName.trim().isEmpty()) {
            GradientDrawable assigneeBg = new GradientDrawable();
            assigneeBg.setCornerRadius(6 * density);
            assigneeBg.setColor(Color.parseColor("#EFF6FF"));
            holder.tvAssignee.setBackground(assigneeBg);
            holder.tvAssignee.setText("Assigned to: " + assignedName.trim());
            holder.tvAssignee.setVisibility(View.VISIBLE);
        } else {
            holder.tvAssignee.setVisibility(View.GONE);
        }

        // Click on status badge or card to change status (Pending / In Progress / Completed)
        // If task is new, tapping the task marks it as seen and immediately clears the "1" badge
        View.OnClickListener statusClickListener = v -> {
            if (task.isNew()) {
                task.setNew(false);
                holder.tvNewBadge.setVisibility(View.GONE);
                if (currentUserId > 0) {
                    TaskPreferenceManager.markTaskAsSeen(context, currentUserId, task.getTaskId());
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).checkUnreadTasks();
                    }
                }
            }
            showStatusSelectionDialog(context, task, holder.getAdapterPosition());
        };

        holder.tvStatus.setOnClickListener(statusClickListener);
        holder.itemView.setOnClickListener(statusClickListener);
    }

    private void showStatusSelectionDialog(Context context, Task task, int position) {
        if (position == RecyclerView.NO_POSITION) return;

        BottomSheetDialog bottomSheet = new BottomSheetDialog(context, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_change_task_status, null);
        bottomSheet.setContentView(dialogView);

        if (bottomSheet.getWindow() != null) {
            View sheetView = bottomSheet.getWindow().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheetView != null) {
                sheetView.setBackgroundResource(android.R.color.transparent);
            }
        }

        TextView tvDialogTaskName = dialogView.findViewById(R.id.tvDialogTaskName);
        ImageButton btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);

        View cardPending = dialogView.findViewById(R.id.cardStatusPending);
        View cardInProgress = dialogView.findViewById(R.id.cardStatusInProgress);
        View cardCompleted = dialogView.findViewById(R.id.cardStatusCompleted);

        ImageView ivCheckPending = dialogView.findViewById(R.id.ivCheckPending);
        ImageView ivCheckInProgress = dialogView.findViewById(R.id.ivCheckInProgress);
        ImageView ivCheckCompleted = dialogView.findViewById(R.id.ivCheckCompleted);

        tvDialogTaskName.setText(task.getTaskName());
        btnDialogClose.setOnClickListener(v -> bottomSheet.dismiss());

        String currentStatus = task.getStatus() != null ? task.getStatus().trim() : "Pending";
        if ("Completed".equalsIgnoreCase(currentStatus)) {
            ivCheckCompleted.setVisibility(View.VISIBLE);
        } else if ("In Progress".equalsIgnoreCase(currentStatus) || "In-Progress".equalsIgnoreCase(currentStatus)) {
            ivCheckInProgress.setVisibility(View.VISIBLE);
        } else {
            ivCheckPending.setVisibility(View.VISIBLE);
        }

        cardPending.setOnClickListener(v -> {
            bottomSheet.dismiss();
            if (!"Pending".equalsIgnoreCase(currentStatus)) {
                updateTaskStatus(context, task, "Pending", position);
            }
        });

        cardInProgress.setOnClickListener(v -> {
            bottomSheet.dismiss();
            if (!"In Progress".equalsIgnoreCase(currentStatus) && !"In-Progress".equalsIgnoreCase(currentStatus)) {
                updateTaskStatus(context, task, "In Progress", position);
            }
        });

        cardCompleted.setOnClickListener(v -> {
            bottomSheet.dismiss();
            if (!"Completed".equalsIgnoreCase(currentStatus)) {
                updateTaskStatus(context, task, "Completed", position);
            }
        });

        bottomSheet.show();
    }

    private void updateTaskStatus(Context context, Task task, String newStatus, int position) {
        String oldStatus = task.getStatus();
        task.setStatus(newStatus);
        notifyItemChanged(position);

        Map<String, String> body = new HashMap<>();
        body.put("status", newStatus);

        ApiClient.getService().updateTaskStatus(task.getTaskId(), body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                    if (statusChangeListener != null) {
                        statusChangeListener.onStatusChanged(task, newStatus, position);
                    }
                } else {
                    task.setStatus(oldStatus);
                    notifyItemChanged(position);
                    Toast.makeText(context, "Failed to update status on server", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                task.setStatus(oldStatus);
                notifyItemChanged(position);
                Toast.makeText(context, "Network error updating status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static String formatDueDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return "No deadline";
        }
        try {
            String datePart = rawDate.trim();
            if (datePart.contains("T")) {
                datePart = datePart.substring(0, datePart.indexOf("T"));
            } else if (datePart.contains(" ")) {
                datePart = datePart.substring(0, datePart.indexOf(" "));
            }

            String[] parts = datePart.split("-");
            if (parts.length == 3) {
                int year = Integer.parseInt(parts[0].trim());
                int month = Integer.parseInt(parts[1].trim());
                int day = Integer.parseInt(parts[2].trim());
                String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                String monthName = (month >= 1 && month <= 12) ? months[month - 1] : String.valueOf(month);
                return day + " " + monthName + " " + year;
            }
        } catch (Exception ignored) {
        }
        return rawDate;
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskName, tvPriority, tvStatus, tvDueDate, tvAssignee, tvNewBadge;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvAssignee = itemView.findViewById(R.id.tvAssignee);
            tvNewBadge = itemView.findViewById(R.id.tvNewBadge);
        }
    }
}
