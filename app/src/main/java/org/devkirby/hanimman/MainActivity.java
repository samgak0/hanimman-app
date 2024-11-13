package org.devkirby.hanimman;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessaging;

import org.devkirby.hanimman.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
        Log.d("MainActivity", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                return;
            }
            String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            String token = task.getResult();
            Log.d("MainActivity", "androidId = " + androidId);
            Log.d("MainActivity", "token = " + token);
//            OkHttpClient client = new OkHttpClient();
//
//            // FormBody 객체 생성 (key-value 쌍 추가)
//            RequestBody formBody = new FormBody.Builder()
//                    .add("username", "john_doe")
//                    .add("email", "john@example.com")
//                    .build();
//
//            Request request = new Request.Builder()
//                    .url("https://example.com/api/login")
//                    .post(formBody)
//                    .build();
//
//            client.newCall(request).enqueue(new Callback() {
//                @Override
//                public void onFailure(Call call, IOException e) {
//                    e.printStackTrace();
//                }
//
//                @Override
//                public void onResponse(Call call, Response response) throws IOException {
//                    if (response.isSuccessful()) {
//                        System.out.println(response.body().string());
//                    } else {
//                        System.out.println("Request failed: " + response.code());
//                    }
//                }
//            });

            Log.d("MainActivity", token);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용된 경우
                // 알림을 보낼 준비를 합니다.
            } else {
                // 권한이 거부된 경우
                // 사용자에게 알림 권한이 필요하다는 메시지를 표시할 수 있습니다.
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}


