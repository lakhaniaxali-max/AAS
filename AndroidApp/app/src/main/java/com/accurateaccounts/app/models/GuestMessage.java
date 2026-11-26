package com.accurateaccounts.app.models;

import com.google.gson.annotations.SerializedName;

public class GuestMessage {
    @SerializedName("message_id")
    private int messageId;

    @SerializedName("guest_name")
    private String guestName;

    @SerializedName("guest_email")
    private String guestEmail;

    @SerializedName("message")
    private String message;

    @SerializedName("created_at")
    private String createdAt;

    public int getMessageId() { return messageId; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public String getMessage() { return message; }
    public String getCreatedAt() { return createdAt; }
}

