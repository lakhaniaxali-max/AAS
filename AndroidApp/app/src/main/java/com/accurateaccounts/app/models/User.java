package com.accurateaccounts.app.models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("auth_id")
    private int authId;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("client_id")
    private int clientId;

    @SerializedName("username")
    private String username;

    @SerializedName("role")
    private String role;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("profile_pic")
    private String profilePic;

    @SerializedName("company_name")
    private String companyName;

    @SerializedName("contact_person")
    private String contactPerson;

    @SerializedName("gstin")
    private String gstin;

    @SerializedName("pan_number")
    private String panNumber;

    public int getAuthId() { return authId; }
    public int getUserId() { return userId; }
    public int getClientId() { return clientId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getProfilePic() { return profilePic; }
    public String getCompanyName() { return companyName; }
    public String getContactPerson() { return contactPerson; }
    public String getGstin() { return gstin; }
    public String getPanNumber() { return panNumber; }

    public String getDisplayName() {
        if (companyName != null && !companyName.trim().isEmpty()) {
            return companyName;
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            String full = firstName;
            if (lastName != null && !lastName.trim().isEmpty()) {
                full += " " + lastName;
            }
            return full;
        }
        if (contactPerson != null && !contactPerson.trim().isEmpty()) {
            return contactPerson;
        }
        return username != null ? username : "User";
    }
}
