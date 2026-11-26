package com.accurateaccounts.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public class Constants {

    public static final String EMULATOR_HOST = "http://10.0.2.2:3000/";
    public static final String CLOUD_PUBLIC_HOST = "https://late-foxes-count.loca.lt/";
    public static final String LOCAL_WIFI_HOST = "http://10.105.4.200:3000/";
    public static final String DEFAULT_DEVICE_HOST = LOCAL_WIFI_HOST;

    private static final String PREF_NAME = "aas_server_prefs";
    private static final String KEY_SERVER_URL = "server_base_url";

    public static String BASE_URL = isEmulator() ? EMULATOR_HOST : DEFAULT_DEVICE_HOST;

    public static boolean isEmulator() {
        return (Build.BRAND != null && Build.BRAND.startsWith("generic") && Build.DEVICE != null && Build.DEVICE.startsWith("generic"))
                || (Build.FINGERPRINT != null && (Build.FINGERPRINT.startsWith("generic") || Build.FINGERPRINT.startsWith("unknown")))
                || (Build.HARDWARE != null && (Build.HARDWARE.contains("goldfish") || Build.HARDWARE.contains("ranchu")))
                || (Build.MODEL != null && (Build.MODEL.contains("google_sdk") || Build.MODEL.contains("Emulator") || Build.MODEL.contains("Android SDK built for x86")))
                || (Build.MANUFACTURER != null && Build.MANUFACTURER.contains("Genymotion"))
                || (Build.PRODUCT != null && (Build.PRODUCT.contains("sdk_google") || Build.PRODUCT.contains("google_sdk") || Build.PRODUCT.contains("sdk") || Build.PRODUCT.contains("sdk_x86") || Build.PRODUCT.contains("vbox86p") || Build.PRODUCT.contains("emulator") || Build.PRODUCT.contains("simulator")));
    }

    public static String getBaseUrl(Context context) {
        if (context == null) {
            return isEmulator() ? EMULATOR_HOST : DEFAULT_DEVICE_HOST;
        }
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String defaultHost = isEmulator() ? EMULATOR_HOST : DEFAULT_DEVICE_HOST;
        String url = sp.getString(KEY_SERVER_URL, defaultHost);
        BASE_URL = url;
        return url;
    }

    public static void setBaseUrl(Context context, String newUrl) {
        if (context == null || newUrl == null || newUrl.trim().isEmpty()) return;
        String formatted = newUrl.trim();
        if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
            formatted = "http://" + formatted;
        }
        if (!formatted.endsWith("/")) {
            formatted = formatted + "/";
        }
        BASE_URL = formatted;
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_SERVER_URL, formatted).apply();
    }
}
