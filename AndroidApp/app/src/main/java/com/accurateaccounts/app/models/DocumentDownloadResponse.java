package com.accurateaccounts.app.models;

import com.google.gson.annotations.SerializedName;

public class DocumentDownloadResponse {
    @SerializedName("doc_id")
    private int docId;

    @SerializedName("title")
    private String title;

    @SerializedName("mime_type")
    private String mimeType;

    @SerializedName("is_base64")
    private boolean isBase64;

    @SerializedName("content")
    private String content;

    @SerializedName("created_at")
    private String createdAt;

    public int getDocId() { return docId; }
    public String getTitle() { return title; }
    public String getMimeType() { return mimeType; }
    public boolean isBase64() { return isBase64; }
    public String getContent() { return content; }
    public String getCreatedAt() { return createdAt; }
}

