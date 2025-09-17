package com.example.a114202_metro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.a114202_metro.Itinerary.Itinerary;
import com.example.a114202_metro.Station.Route;
import com.example.a114202_metro.Station.Station;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private GoogleSignInClient gsc;
    private GoogleSignInAccount acct;
    private ImageView btn_login, accountImg, itineraryImg, stationImg, pathImg, favoriteImg;
    private TextView guest;

    private final ActivityResultLauncher<Intent> settingLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getBooleanExtra("logout_success", false)) {
                        btn_login.setVisibility(View.VISIBLE);
                        guest.setText("Guest");
                        accountImg.setImageResource(R.drawable.account_photo); // 登出還原為預設頭像
                        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

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

        // 取得畫面中的元件
        btn_login = findViewById(R.id.btn_login);
        accountImg = findViewById(R.id.accountImg);
        pathImg = findViewById(R.id.pathImg);
        itineraryImg = findViewById(R.id.itineraryImg);
        stationImg = findViewById(R.id.stationImg);
        guest = findViewById(R.id.guest);
        favoriteImg = findViewById(R.id.favoriteImg);

        //開啟routeActivity
        pathImg.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Route.class);
            startActivity(intent);
        });

        //開啟UserLikeActivity
        favoriteImg.setOnClickListener(v -> {
            acct = GoogleSignIn.getLastSignedInAccount(this);
            if (acct != null) {
                Intent intent = new Intent(MainActivity.this, UserLike.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Please Login First", Toast.LENGTH_SHORT).show();
            }
        });

        //開啟ItineraryActivity
        itineraryImg.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Itinerary.class);
            startActivity(intent);
        });

        // 開啟ChatAIActivity


        // 開啟StationActivity
        stationImg.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Station.class);
            startActivity(intent);
        });

        // 已登入則隱藏登入按鈕，並顯示資訊
        acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {
            String personName = acct.getDisplayName();
            String personEmail = acct.getEmail();
            String personPhoto = (acct.getPhotoUrl() != null) ? acct.getPhotoUrl().toString() : "";

            guest.setText(personName);
            Glide.with(this)
                    .load(personPhoto)
                    .placeholder(R.drawable.account_photo)
                    .into(accountImg);

            btn_login.setVisibility(View.GONE);
        }

        // Google 登入按鈕
        btn_login.setOnClickListener(view -> signIn());

        // 開啟設定畫面
        accountImg.setOnClickListener(v -> {
            acct = GoogleSignIn.getLastSignedInAccount(this);
            if (acct != null) {
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                settingLauncher.launch(intent);
            } else {
                Toast.makeText(this, "Please Login First", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        gsc = GoogleSignIn.getClient(this, gso);
    }

    private void signIn() {
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
                String personName = account.getDisplayName();
                String personEmail = account.getEmail();
                String personPhoto = (account.getPhotoUrl() != null) ? account.getPhotoUrl().toString() : "";

                // 更新主畫面 UI
                guest.setText(personName);
                Glide.with(this)
                        .load(personPhoto)
                        .placeholder(R.drawable.account_photo)
                        .into(accountImg);

                // 呼叫後端 API
                registerUser(personEmail, personName, personPhoto);

                btn_login.setVisibility(View.GONE);
            }
        } catch (ApiException e) {
            Toast.makeText(getApplicationContext(), "Login Failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerUser(String gmail, String name, String imageUrl) {
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
                params.put("gmail", gmail);
                params.put("name", name);
                params.put("image", imageUrl);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }
}
