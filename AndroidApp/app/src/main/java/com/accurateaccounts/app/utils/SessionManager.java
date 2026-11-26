package com.accurateaccounts.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "AccurateAccountsSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_CLIENT_ID = "clientId";
    private static final String KEY_AUTH_ID = "authId";
    private static final String KEY_ROLE = "role";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FIRST_NAME = "firstName";
    private static final String KEY_LAST_NAME = "lastName";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_COMPANY_NAME = "companyName";
    private static final String KEY_CONTACT_PERSON = "contactPerson";
    private static final String KEY_GSTIN = "gstin";
    private static final String KEY_PAN_NUMBER = "panNumber";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        this.pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = pref.edit();
    }

    public void createLoginSession(int userId, int clientId, int authId, String username, String role, 
                                   String firstName, String lastName, String email, String phone, 
                                   String companyName, String contactPerson, String gstin, String panNumber) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId);
        editor.putInt(KEY_CLIENT_ID, clientId);
        editor.putInt(KEY_AUTH_ID, authId);
        editor.putString(KEY_USERNAME, username != null ? username : "");
        editor.putString(KEY_ROLE, role != null ? role : "");
        editor.putString(KEY_FIRST_NAME, firstName != null ? firstName : "");
        editor.putString(KEY_LAST_NAME, lastName != null ? lastName : "");
        editor.putString(KEY_EMAIL, email != null ? email : "");
        editor.putString(KEY_PHONE, phone != null ? phone : "");
        editor.putString(KEY_COMPANY_NAME, companyName != null ? companyName : "");
        editor.putString(KEY_CONTACT_PERSON, contactPerson != null ? contactPerson : "");
        editor.putString(KEY_GSTIN, gstin != null ? gstin : "");
        editor.putString(KEY_PAN_NUMBER, panNumber != null ? panNumber : "");
        editor.apply();
    }

    // Overload for backward compatibility
    public void createLoginSession(int userId, int clientId, int authId, String username, String role, String firstName, String lastName) {
        createLoginSession(userId, clientId, authId, username, role, firstName, lastName, "", "", "", "", "", "");
    }

    public void updateProfileDetails(String email, String phone, String companyName, String contactPerson, String gstin, String panNumber) {
        if (email != null) editor.putString(KEY_EMAIL, email);
        if (phone != null) editor.putString(KEY_PHONE, phone);
        if (companyName != null) editor.putString(KEY_COMPANY_NAME, companyName);
        if (contactPerson != null) editor.putString(KEY_CONTACT_PERSON, contactPerson);
        if (gstin != null) editor.putString(KEY_GSTIN, gstin);
        if (panNumber != null) editor.putString(KEY_PAN_NUMBER, panNumber);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public int getUserId() {
        return pref.getInt(KEY_USER_ID, -1);
    }

    public int getClientId() {
        return pref.getInt(KEY_CLIENT_ID, -1);
    }

    public int getAuthId() {
        return pref.getInt(KEY_AUTH_ID, -1);
    }

    public String getUsername() {
        return pref.getString(KEY_USERNAME, "");
    }

    public String getUserRole() {
        return pref.getString(KEY_ROLE, "");
    }

    public String getFirstName() {
        return pref.getString(KEY_FIRST_NAME, "");
    }

    public String getLastName() {
        return pref.getString(KEY_LAST_NAME, "");
    }

    public String getEmail() {
        return pref.getString(KEY_EMAIL, "");
    }

    public String getPhone() {
        return pref.getString(KEY_PHONE, "");
    }

    public String getCompanyName() {
        return pref.getString(KEY_COMPANY_NAME, "");
    }

    public String getContactPerson() {
        return pref.getString(KEY_CONTACT_PERSON, "");
    }

    public String getGstin() {
        return pref.getString(KEY_GSTIN, "");
    }

    public String getPanNumber() {
        return pref.getString(KEY_PAN_NUMBER, "");
    }

    public String getFullName() {
        return pref.getString(KEY_FIRST_NAME, "") + " " + pref.getString(KEY_LAST_NAME, "");
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}

