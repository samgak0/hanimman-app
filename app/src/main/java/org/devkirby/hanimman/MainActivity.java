package org.devkirby.hanimman;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import org.apache.commons.text.StringEscapeUtils;
import org.devkirby.hanimman.databinding.ActivityMainBinding;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;
    private static final String URL_FOR_AVD = "http://10.0.2.2:3000/";
    private static final String URL_FOR_LOCAL = "http://192.168.100.219:3000/";
    private static final String URL_SERVER_FOR_AVD = "http://10.0.2.2:8080/";
    private static final String URL_SERVER_FOR_LOCAL = "http://192.168.100.219:8080/";

    private final Gson gson = new Gson();
    private WebView webView;
    private String receiverId;

    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleBroadcastMessage(context, intent);
        }
    };

    @SuppressLint({"HardwareIds", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        webView = binding.webView;
        setContentView(binding.getRoot());

        LocalBroadcastManager.getInstance(this).registerReceiver(
                messageReceiver, new IntentFilter("MY_NOTIFICATION")
        );

        initializeUI(binding);
        requestNotificationPermission();
        logDeviceId();
        fetchAndSendToken();

        handleIntent(getIntent());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }

    private void initializeUI(ActivityMainBinding binding) {
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupWebView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setDefaultTextEncodingName("utf-8");

        webView.setOnLongClickListener(v -> true);
        webView.setLongClickable(false);
        webView.getViewTreeObserver().addOnGlobalLayoutListener(this::adjustWebViewForKeyboard);
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
    }

    private void adjustWebViewForKeyboard() {
        Rect rect = new Rect();
        webView.getWindowVisibleDisplayFrame(rect);
        int screenHeight = webView.getRootView().getHeight();
        int keypadHeight = screenHeight - rect.bottom;

        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) webView.getLayoutParams();
        layoutParams.bottomMargin = Math.max(keypadHeight, 0);
        webView.setLayoutParams(layoutParams);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getExtras() == null) {
            webView.loadUrl(getFrontBaseUrl());
            return;
        }
        Log.d("MainActivity", "handleIntent = " + intent);
        Log.d("MainActivity", "handleIntent receiverId = " + receiverId);
        receiverId = intent.getExtras().getString("receiverId");

        moveChatRoom();
    }

    private void moveChatRoom() {
        if (receiverId != null) {
            Log.d("MainActivity", "moveChatRoom receiverId = " + receiverId);
            webView.loadUrl(getFrontBaseUrl() + "chats/" + receiverId);
        } else {
            Log.e(TAG, "receiverId is null");
        }
    }

    private void handleBroadcastMessage(Context context, Intent intent) {
        Map<String, String> data = extractDataFromIntent(intent);
        Log.d("MainActivity", "data = " + data);
        Log.d("MainActivity", "handleBroadcastMessage receiverId = " + receiverId);

        if (receiverId == null) {
            webView.evaluateJavascript("window.NativeInterface.getReceiveId();", (result) -> MainActivity.this.receiverId = result);
        }

        if (receiverId != null && receiverId.equals(data.get("senderId"))) {
            if (data.get("content") != null) {
                String receiverName = StringEscapeUtils.escapeEcmaScript(data.get("receiverName"));
                String content = StringEscapeUtils.escapeEcmaScript(data.get("content"));
                String createdAt = StringEscapeUtils.escapeEcmaScript(data.get("createdAt"));
                webView.evaluateJavascript("window.NativeInterface.receiveMessage(\"" + content + "\",\"" + receiverName + "\"," + "\"" + createdAt + "\");", null);
            }
        } else {
            String receiverName = data.get("receiverName");
            String content = data.get("content");
            MyNotificationHelper.showNotification(context, receiverName, content, data);
        }
    }

    @NonNull
    private Map<String, String> extractDataFromIntent(@NonNull Intent intent) {
        Map<String, String> data = new HashMap<>();
        if (intent.getExtras() != null) {
            for (String key : intent.getExtras().keySet()) {
                Object value = intent.getExtras().get(key);
                if (value != null) {
                    data.put(key, value.toString());
                }
            }
        }
        return data;
    }

    private void fetchAndSendToken() {
        Log.d("MainActivity", "fetchAndSendToken");
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;

            String androidId = getAndroidId();
            String token = task.getResult();
            sendTokenToServer(androidId, token);
        });
    }

    private String getFrontBaseUrl() {
        return isRunningOnEmulator() ? URL_FOR_AVD : URL_FOR_LOCAL;
    }

    private String getServerBaseUrl() {
        return isRunningOnEmulator() ? URL_SERVER_FOR_AVD : URL_SERVER_FOR_LOCAL;
    }

    @SuppressLint("HardwareIds")
    private String getAndroidId() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private void sendTokenToServer(String androidId, String token) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String json = gson.toJson(Map.of("androidId", androidId, "newToken", token));
                RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

                String url = getServerBaseUrl() + "api/users/token";
                Log.d("MainActivity", "url = " + url);
                Log.d("MainActivity", "json = " + json);
                Request request = new Request.Builder().url(url).put(body).build();

                try (Response response = client.newCall(request).execute()) {
                    handleServerResponse(response);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending token to server", e);
            }
        });
    }

    private void handleServerResponse(@NonNull Response response) throws Exception {
        if (response.isSuccessful() && response.body() != null) {
            Log.d(TAG, "Response: " + response.body().string());
        } else {
            assert response.body() != null;
            Log.e(TAG, "Request failed: " + response.body().string() + " " + response.code());
        }
    }

    private boolean isRunningOnEmulator() {
        return Build.FINGERPRINT.contains("sdk_gphone");
    }

    private void logDeviceId() {
        String androidId = getAndroidId();
        Log.d(TAG, "Android ID: " + androidId);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");
            } else {
                Log.d(TAG, "Notification permission denied");
            }
        }
    }

    public class WebAppInterface {
        Context context;

        WebAppInterface(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void setReceiverId(String receiverId) {
            Log.d("MainActivity", "WebAppInterface receiverId = " + receiverId);
            MainActivity.this.receiverId = receiverId;
        }
    }
}
