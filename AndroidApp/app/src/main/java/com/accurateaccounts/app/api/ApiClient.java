package com.accurateaccounts.app.api;

import android.content.Context;

import com.accurateaccounts.app.AccurateAccountsApp;
import com.accurateaccounts.app.utils.Constants;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;
    private static String currentBaseUrl = null;

    public static synchronized ApiService getService() {
        Context context = AccurateAccountsApp.getInstance();
        String baseUrl = Constants.getBaseUrl(context);

        if (retrofit == null || currentBaseUrl == null || !currentBaseUrl.equals(baseUrl)) {
            currentBaseUrl = baseUrl;
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    public static synchronized void resetClient() {
        retrofit = null;
        currentBaseUrl = null;
    }
}
