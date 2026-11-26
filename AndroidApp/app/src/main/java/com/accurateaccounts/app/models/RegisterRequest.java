package com.accurateaccounts.app.models;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("username")
    private String username;

    @SerializedName("password")
    private String password;

    @SerializedName("role")
    private String role; // "admin", "employee", "client"

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("company_name")
    private String companyName;

    @SerializedName("contact_person")
    private String contactPerson;

    @SerializedName("gstin")
    private String gstin;

    @SerializedName("pan_number")
    private String panNumber;

    public RegisterRequest(String username, String password, String role, String firstName, String lastName, String email, String phone) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }

    public RegisterRequest(String username, String password, String role, String companyName, String contactPerson, String email, String phone, String gstin, String panNumber) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.companyName = companyName;
        this.contactPerson = contactPerson;
        this.email = email;
        this.phone = phone;
        this.gstin = gstin;
        this.panNumber = panNumber;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getCompanyName() { return companyName; }
    public String getContactPerson() { return contactPerson; }
    public String getGstin() { return gstin; }
    public String getPanNumber() { return panNumber; }
}

