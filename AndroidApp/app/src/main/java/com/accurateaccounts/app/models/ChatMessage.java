package com.accurateaccounts.app.models;

import com.google.gson.annotations.SerializedName;

public class ChatMessage {
    @SerializedName("message_id")
    private int messageId;

    @SerializedName("sender_id")
    private int senderId;

    @SerializedName("receiver_id")
    private Integer receiverId;

    @SerializedName("group_id")
    private Integer groupId;

    @SerializedName("message")
    private String message;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    public ChatMessage(int senderId, Integer receiverId, Integer groupId, String message) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.groupId = groupId;
        this.message = message;
    }

    public int getMessageId() { return messageId; }
    public int getSenderId() { return senderId; }
    public Integer getReceiverId() { return receiverId; }
    public Integer getGroupId() { return groupId; }
    public String getMessage() { return message; }
    public String getCreatedAt() { return createdAt; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
}

