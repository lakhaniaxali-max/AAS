package com.accurateaccounts.app.models;

import com.google.gson.annotations.SerializedName;

public class Task {
    @SerializedName("task_id")
    private int taskId;

    @SerializedName("task_name")
    private String taskName;

    @SerializedName("status")
    private String status;

    @SerializedName("priority")
    private String priority;

    @SerializedName("due_date")
    private String dueDate;

    @SerializedName("assigned_by_first_name")
    private String assignedByName;

    @SerializedName("assigned_to_first_name")
    private String assignedToName;

    @SerializedName("assigned_to_last_name")
    private String assignedToLastName;

    @SerializedName("group_name")
    private String groupName;

    private transient boolean isNew = false;

    public boolean isNew() { return isNew; }
    public void setNew(boolean isNew) { this.isNew = isNew; }

    public int getTaskId() { return taskId; }
    public String getTaskName() { return taskName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public String getDueDate() { return dueDate; }
    public String getAssignedByName() { return assignedByName; }
    public String getAssignedToName() { return assignedToName; }
    public String getAssignedToLastName() { return assignedToLastName; }
    public String getGroupName() { return groupName; }

    public String getFullAssignedToName() {
        String first = assignedToName != null ? assignedToName.trim() : "";
        String last = assignedToLastName != null ? assignedToLastName.trim() : "";
        if (!first.isEmpty() && !last.isEmpty()) {
            return first + " " + last;
        } else if (!first.isEmpty()) {
            return first;
        } else if (!last.isEmpty()) {
            return last;
        }
        return "";
    }
}
