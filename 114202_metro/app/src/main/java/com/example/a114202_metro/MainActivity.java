package com.example.a114202_metro;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private GoogleSignInOptions gso;
    private GoogleSignInClient gsc;
    private ImageView btn_login;
    private Button btn_logout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        setupGoogleSignIn();

        // 初始化視圖
        btn_login = findViewById(R.id.btn_login);
        btn_logout = findViewById(R.id.btn_logout);

        // 設置Google登入
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        gsc = GoogleSignIn.getClient(this, gso);

        // 檢查是否已經登入
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {
            String personEmail = acct.getEmail();
            btn_login.setVisibility(View.GONE);
            btn_logout.setVisibility(View.VISIBLE);
        }

        // 設置登入按鈕點擊事件
        btn_login.setOnClickListener(view -> signIn());

        // 設置登出按鈕點擊事件
        btn_logout.setOnClickListener(v -> {
            gsc.signOut().addOnCompleteListener(task -> {
                Toast.makeText(MainActivity.this, "logout success", Toast.LENGTH_SHORT).show();
                btn_login.setVisibility(View.VISIBLE);
                btn_logout.setVisibility(View.GONE);
            });
        });
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso;
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this, gso);
    }
    private void signIn() {
//        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
//        startActivity(intent);
        Intent signInIntent = gsc.getSignInIntent();
        startActivityForResult(signInIntent, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000) {
            handleGoogleSignInResult(data);
        }
    }

    private void handleGoogleSignInResult(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                // String personName = account.getDisplayName();
                String personEmail = account.getEmail();
                registerUser(personEmail);
                navigateToSecondActivity();
            }
        } catch (ApiException e) {
            Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerUser(String personEmail) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Constants.URL_REGISTER,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        Toast.makeText(getApplicationContext(), jsonObject.getString("message"), Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                //params.put("username", personName);
                params.put("gmail", personEmail);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    private void navigateToSecondActivity() {
        finish();
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        startActivity(intent);
    }
}