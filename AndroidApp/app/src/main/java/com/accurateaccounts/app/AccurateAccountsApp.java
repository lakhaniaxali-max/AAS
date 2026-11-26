package com.accurateaccounts.app;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class AccurateAccountsApp extends Application {

    private static AccurateAccountsApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // Force Light theme across the entire application on all mobile devices
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    public static AccurateAccountsApp getInstance() {
        return instance;
    }
}
