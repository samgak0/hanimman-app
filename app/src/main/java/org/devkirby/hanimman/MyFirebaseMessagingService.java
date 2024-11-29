package org.devkirby.hanimman;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.Map;


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("MyFirebaseMessagingService", "onNewToken token = " + token);
        sendRegistrationToServer(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (MyApplication.isAppInForeground()) {
            Log.d("FirebaseMessaging", "App is in foreground");
            Intent intent = new Intent("MY_NOTIFICATION");

            Map<String, String> data = remoteMessage.getData();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }

            Log.d("FirebaseMessaging", "sendBroadcast");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } else {
            Log.d("FirebaseMessaging", "App is in background");
            Map<String, String> data = remoteMessage.getData();
            Gson gson = new Gson();
            String jsonString = gson.toJson(remoteMessage.getData());
            Log.d("MyFirebaseMessagingService", "remoteMessage.getData() = " + jsonString);

            String title = data.get("title");
            String content = data.get("content");

            MyNotificationHelper.showNotification(this, title, content, data);
        }
    }

    private void sendRegistrationToServer(String token) {
        Log.d("MyFirebaseMessagingService", "token = " + token);
    }
}