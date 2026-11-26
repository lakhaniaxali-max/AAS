package com.accurateaccounts.app.models;

import com.google.gson.annotations.SerializedName;

public class KycUploadRequest {
    @SerializedName("client_id")
    private int clientId;

    @SerializedName("doc_type")
    private String docType;

    @SerializedName("file_data")
    private String fileData;

    @SerializedName("mime_type")
    private String mimeType;

    public KycUploadRequest(int clientId, String docType, String fileData, String mimeType) {
        this.clientId = clientId;
        this.docType = docType;
        this.fileData = fileData;
        this.mimeType = mimeType;
    }
}

