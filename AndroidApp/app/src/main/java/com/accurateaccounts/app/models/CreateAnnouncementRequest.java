package com.accurateaccounts.app.models;

import com.google.gson.annotations.SerializedName;

public class CreateAnnouncementRequest {
    @SerializedName("auth_id")
    private int authId;

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("created_by")
    private String createdBy;

    @SerializedName("announcement_date")
    private String announcementDate;

    public CreateAnnouncementRequest(int authId, String title, String message, String createdBy, String announcementDate) {
        this.authId = authId;
        this.title = title;
        this.message = message;
        this.createdBy = createdBy;
        this.announcementDate = announcementDate;
    }

    public int getAuthId() { return authId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getCreatedBy() { return createdBy; }
    public String getAnnouncementDate() { return announcementDate; }
}
