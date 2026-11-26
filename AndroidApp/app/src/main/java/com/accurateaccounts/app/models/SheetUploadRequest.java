package com.accurateaccounts.app.models;

import com.google.gson.annotations.SerializedName;

public class SheetUploadRequest {
    @SerializedName("client_id")
    private int clientId;

    @SerializedName("doc_type")
    private String docType;

    @SerializedName("doc_name")
    private String docName;

    @SerializedName("file_data")
    private String fileData;

    @SerializedName("mime_type")
    private String mimeType;

    public SheetUploadRequest(int clientId, String docType, String docName, String fileData) {
        this.clientId = clientId;
        this.docType = docType;
        this.docName = docName;
        this.fileData = fileData;
        this.mimeType = "application/pdf";
    }

    public SheetUploadRequest(int clientId, String docType, String docName, String fileData, String mimeType) {
        this.clientId = clientId;
        this.docType = docType;
        this.docName = docName;
        this.fileData = fileData;
        this.mimeType = mimeType;
    }
}

