package com.accurateaccounts.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ClientDocumentData {
    @SerializedName("client_id")
    private int clientId;

    @SerializedName("company_name")
    private String companyName;

    @SerializedName("contact_person")
    private String contactPerson;

    @SerializedName("email")
    private String email;

    @SerializedName("gstin")
    private String gstin;

    @SerializedName("pan_number")
    private String panNumber;

    @SerializedName("importantDocuments")
    private List<DocItem> importantDocuments;

    @SerializedName("folders")
    private List<FolderItem> folders;

    public int getClientId() { return clientId; }
    public String getCompanyName() { return companyName; }
    public String getContactPerson() { return contactPerson; }
    public String getEmail() { return email; }
    public String getGstin() { return gstin; }
    public String getPanNumber() { return panNumber; }
    public List<DocItem> getImportantDocuments() { return importantDocuments; }
    public List<FolderItem> getFolders() { return folders; }

    public static class DocItem {
        @SerializedName("doc_id")
        private int docId;

        @SerializedName("doc_name")
        private String docName;

        @SerializedName("doc_type")
        private String docType;

        @SerializedName("created_at")
        private String createdAt;

        public int getDocId() { return docId; }
        public String getDocName() { return docName != null ? docName : docType; }
        public String getDocType() { return docType; }
        public String getCreatedAt() { return createdAt; }
    }

    public static class FolderItem {
        @SerializedName("folder_id")
        private int folderId;

        @SerializedName("folder_name")
        private String folderName;

        @SerializedName("importantDocuments")
        private List<DocItem> importantDocuments;

        @SerializedName("otherDocuments")
        private List<DocItem> otherDocuments;

        public int getFolderId() { return folderId; }
        public String getFolderName() { return folderName; }
        public List<DocItem> getImportantDocuments() { return importantDocuments; }
        public List<DocItem> getOtherDocuments() { return otherDocuments; }
    }
}

