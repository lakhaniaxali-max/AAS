package com.accurateaccounts.app.api;

import android.content.Context;

import com.accurateaccounts.app.AccurateAccountsApp;
import com.accurateaccounts.app.utils.Constants;

import java.net.URISyntaxException;
import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketManager {
    private static Socket mSocket;
    private static String currentBaseUrl;

    public static synchronized Socket getSocket() {
        Context context = AccurateAccountsApp.getInstance();
        String baseUrl = Constants.getBaseUrl(context);

        if (mSocket == null || currentBaseUrl == null || !currentBaseUrl.equals(baseUrl)) {
            if (mSocket != null) {
                mSocket.disconnect();
                mSocket = null;
            }
            currentBaseUrl = baseUrl;
            try {
                IO.Options opts = new IO.Options();
                opts.forceNew = true;
                opts.reconnection = true;
                mSocket = IO.socket(baseUrl, opts);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return mSocket;
    }

    public static synchronized void disconnect() {
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket = null;
            currentBaseUrl = null;
        }
    }
}
