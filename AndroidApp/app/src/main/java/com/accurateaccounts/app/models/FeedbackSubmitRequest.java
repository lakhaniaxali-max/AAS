package com.accurateaccounts.app.models;

import com.google.gson.annotations.SerializedName;

public class FeedbackSubmitRequest {
    @SerializedName("client_id")
    private int clientId;

    @SerializedName("message")
    private String message;

    public FeedbackSubmitRequest(int clientId, String message) {
        this.clientId = clientId;
        this.message = message;
    }

    public int getClientId() { return clientId; }
    public String getMessage() { return message; }
}

