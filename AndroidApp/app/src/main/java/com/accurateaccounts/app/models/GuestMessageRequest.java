package com.accurateaccounts.app.models;

import com.google.gson.annotations.SerializedName;

public class GuestMessageRequest {
    @SerializedName("guest_name")
    private String guestName;

    @SerializedName("guest_email")
    private String guestEmail;

    @SerializedName("message")
    private String message;

    public GuestMessageRequest(String guestName, String guestEmail, String message) {
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.message = message;
    }

    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public String getMessage() { return message; }
}

