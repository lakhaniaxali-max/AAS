package com.accurateaccounts.app.models;

import com.google.gson.annotations.SerializedName;

public class Announcement {
    @SerializedName("announcement_id")
    private int announcementId;

    @SerializedName("auth_id")
    private int authId;

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("created_by")
    private String createdBy;

    @SerializedName("announcement_date")
    private String announcementDate;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    public int getAnnouncementId() { return announcementId; }
    public int getAuthId() { return authId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public String getAnnouncementDate() { return announcementDate; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
}
