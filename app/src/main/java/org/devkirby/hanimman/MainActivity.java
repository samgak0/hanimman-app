package org.devkirby.hanimman;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
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

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import org.devkirby.hanimman.databinding.ActivityMainBinding;

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
    private final Gson gson = new Gson();

    @SuppressLint({"HardwareIds", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());

        WebView webView = binding.webView;

        setContentView(binding.getRoot());

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        requestNotificationPermission();
        logDeviceId();
        fetchAndSendToken();
        EdgeToEdge.enable(this);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        webView.setOnLongClickListener(v -> true);
        webView.setLongClickable(false);
        webView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect rect = new Rect();
            webView.getWindowVisibleDisplayFrame(rect);
            int screenHeight = webView.getRootView().getHeight();
            int keypadHeight = screenHeight - rect.bottom;

            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) webView.getLayoutParams();

            layoutParams.bottomMargin = Math.max(keypadHeight, 0);

            webView.setLayoutParams(layoutParams);
        });
        
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            for (String key : intent.getExtras().keySet()) {
                String value = intent.getStringExtra(key);
                Log.d("MainActivity", "Key: " + key + ", Value: " + value);
            }
            String senderId = intent.getExtras().getString("senderId");
            if (senderId != null) {
                Log.d("MainActivity", senderId);
                webView.loadUrl("http://192.168.101.34:3000/chats/" + senderId);
            } else {
                Log.e("MainActivity", "senderId is null");
            }
        } else {
            webView.loadUrl("http://192.168.101.34:3000");
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @SuppressLint("HardwareIds")
    private void logDeviceId() {
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d(TAG, "Android ID: " + androidId);
    }

    private void fetchAndSendToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                return;
            }

            String androidId = getAndroidId();
            String token = task.getResult();

            Log.d(TAG, "androidId = " + androidId);
            Log.d(TAG, "token = " + token);

            sendTokenToServer(androidId, token);
        });
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
                Map<String, String> requestMap = Map.of("androidId", androidId, "newToken", token);
                String json = gson.toJson(requestMap);

                RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url("http://192.168.101.34/api/users/token")
                        .put(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    handleServerResponse(response);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending token to server", e);
            }
        });
    }

    private void handleServerResponse(Response response) throws Exception {
        if (response.isSuccessful() && response.body() != null) {
            Log.d(TAG, "Response: " + response.body().string());
        } else {
            Log.d(TAG, "Request failed: " + response.code());
        }
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
}
