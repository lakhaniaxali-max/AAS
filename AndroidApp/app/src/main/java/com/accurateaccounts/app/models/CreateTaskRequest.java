package com.accurateaccounts.app.models;

import com.google.gson.annotations.SerializedName;

public class CreateTaskRequest {
    @SerializedName("task_name")
    private String taskName;

    @SerializedName("assigned_by")
    private int assignedBy;

    @SerializedName("assigned_to")
    private Integer assignedTo;

    @SerializedName("due_date")
    private String dueDate;

    @SerializedName("status")
    private String status;

    @SerializedName("priority")
    private String priority;

    public CreateTaskRequest(String taskName, int assignedBy, Integer assignedTo, String dueDate, String status, String priority) {
        this.taskName = taskName;
        this.assignedBy = assignedBy;
        this.assignedTo = assignedTo;
        this.dueDate = dueDate;
        this.status = status;
        this.priority = priority;
    }
}

