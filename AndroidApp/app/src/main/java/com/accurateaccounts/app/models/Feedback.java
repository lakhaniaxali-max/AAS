package com.accurateaccounts.app.models;

import com.google.gson.annotations.SerializedName;

public class Feedback {
    @SerializedName("feedback_id")
    private int feedbackId;

    @SerializedName("message")
    private String message;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("company_name")
    private String companyName;

    @SerializedName("contact_person")
    private String contactPerson;

    public int getFeedbackId() { return feedbackId; }
    public String getMessage() { return message; }
    public String getCreatedAt() { return createdAt; }
    public String getCompanyName() { return companyName; }
    public String getContactPerson() { return contactPerson; }
}

