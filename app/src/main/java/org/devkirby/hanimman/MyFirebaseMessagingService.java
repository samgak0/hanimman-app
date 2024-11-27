package org.devkirby.hanimman;

import android.app.NotificationManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

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
        } else {
            Log.d("FirebaseMessaging", "App is in background");
        }
        Map<String, String> data = remoteMessage.getData();
        Gson gson = new Gson();
        String jsonString = gson.toJson(remoteMessage.getData());
        Log.d("MyFirebaseMessagingService", "remoteMessage.getData() = " + jsonString);
        showNotification(data.get("title"),
                data.get("content"));
    }

    private void showNotification(String title, String message) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelId = MyApplication.CHANNEL_ID;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        manager.notify(0, builder.build());
    }

    private void sendRegistrationToServer(String token) {
        Log.d("MyFirebaseMessagingService", "token = " + token);
    }
}
