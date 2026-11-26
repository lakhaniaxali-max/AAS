package com.accurateaccounts.app.models;

import com.google.gson.annotations.SerializedName;

public class CountResponse {
    @SerializedName("count")
    private int count;

    public int getCount() {
        return count;
    }
}

