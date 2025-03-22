package com.example.a114202_metro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;

public class SettingActivity extends AppCompatActivity {

    private GoogleSignInClient gsc;
    private ImageView btn_logout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setting);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btn_logout = findViewById(R.id.btn_logout);

        // ✅ 初始化 GoogleSignInClient
        gsc = GoogleSignIn.getClient(this,
                new com.google.android.gms.auth.api.signin.GoogleSignInOptions
                        .Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build());

        // ✅ 設置登出按鈕點擊事件
        btn_logout.setOnClickListener(v -> {
            gsc.signOut().addOnCompleteListener(task -> {
                // ✅ 傳回登出訊息
                Intent resultIntent = new Intent();
                resultIntent.putExtra("logout_success", true);
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        });
    }
}
