package com.accurateaccounts.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ClientDocumentsResponse {
    @SerializedName("client_id")
    private int clientId;

    @SerializedName("important_documents")
    private List<DocRecord> importantDocuments;

    @SerializedName("other_documents")
    private List<DocRecord> otherDocuments;

    public int getClientId() { return clientId; }
    public List<DocRecord> getImportantDocuments() { return importantDocuments; }
    public List<DocRecord> getOtherDocuments() { return otherDocuments; }

    public static class DocRecord {
        @SerializedName("doc_id")
        private int docId;

        @SerializedName("doc_type")
        private String docType;

        @SerializedName("doc_name")
        private String docName;

        @SerializedName("created_at")
        private String createdAt;

        @SerializedName("has_file")
        private boolean hasFile;

        @SerializedName("mime_type")
        private String mimeType;

        public int getDocId() { return docId; }
        public String getDocType() { return docType; }
        public String getDocName() { return docName != null ? docName : docType; }
        public String getCreatedAt() { return createdAt; }
        public boolean hasFile() { return hasFile; }
        public String getMimeType() { return mimeType; }
    }
}

