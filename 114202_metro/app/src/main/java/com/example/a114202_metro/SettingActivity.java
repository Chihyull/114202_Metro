package com.example.a114202_metro;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.imageview.ShapeableImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SettingActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_IMAGE = 2000;
    private static final int PERMISSION_REQUEST_CODE = 3000;
    private static final String PREF_NAME = "UserPrefs";
    private static final String KEY_PROFILE_IMAGE = "user_profile_image";

    private GoogleSignInClient gsc;
    private ImageView btn_logout;
    private ShapeableImageView avatar;     // 改為ShapeableImageView
    private EditText editTitleName;
    private String userGmail;
    private String originalName = "";
    private boolean isUpdating = false;
    private SharedPreferences sharedPreferences;

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

        // 初始化SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // 初始化元件
        btn_logout = findViewById(R.id.btn_logout);
        avatar = findViewById(R.id.avatar);  // 現在這是一個ShapeableImageView
        editTitleName = findViewById(R.id.edit_title_name);

        editTitleName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {  // 當 EditText 失去焦點時
                String newName = editTitleName.getText().toString().trim();
                GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
                if (acct != null && !newName.isEmpty()) {
                    String gmail = acct.getEmail();
                }
            }
        });


        // 初始化 GoogleSignInClient
        gsc = GoogleSignIn.getClient(this,
                new GoogleSignInOptions
                        .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build());

        // 設置登出按鈕點擊事件
        btn_logout.setOnClickListener(v -> {
            // 登出時不清除保存的個人資料圖片，這樣用戶再次登入時仍能看到之前設置的頭像

            gsc.signOut().addOnCompleteListener(task -> {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("logout_success", true);
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        });

        // 顯示登入使用者的名稱與圖片
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);

        // 設置用戶名稱
        if (acct != null) {
            String name = acct.getDisplayName();
            editTitleName.setText(name);
        }

        // 先檢查是否有自定義圖片，無論用戶是否登入都嘗試載入自定義圖片
        String savedImageUri = sharedPreferences.getString(KEY_PROFILE_IMAGE, "");
        if (!TextUtils.isEmpty(savedImageUri)) {
            // 如果有保存的自定義圖片路徑，使用這個圖片
            Glide.with(this)
                    .load(Uri.parse(savedImageUri))
                    .placeholder(R.drawable.metro_photo)
                    .into(avatar);
        } else if (acct != null) {
            // 如果沒有自定義圖片，且用戶已登入，則使用Google帳號的圖片
            String photoUrl = (acct.getPhotoUrl() != null) ? acct.getPhotoUrl().toString() : "";
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.metro_photo) // 請用你現有的預設圖片資源
                    .into(avatar);
        } else {
            // 如果都沒有，就顯示預設圖片
            Glide.with(this)
                    .load(R.drawable.metro_photo)
                    .into(avatar);
        }

        // *** 新增: 點擊avatar開啟相簿選圖 ***
        avatar.setClickable(true);
        avatar.setFocusable(true);
        avatar.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else {
                openGallery();
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permission denied to read external storage", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                // 顯示選擇的圖片
                Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.metro_photo)
                        .into(avatar);

                // 保存圖片Uri到SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(KEY_PROFILE_IMAGE, imageUri.toString());
                editor.apply();

                Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 實現檔案持久化，在重啟app後仍能載入使用者自訂的圖片
    private void saveImageToInternalStorage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                // 創建檔案
                File directory = getFilesDir();
                File imageFile = new File(directory, "profile_image.jpg");

                // 將bitmap保存為檔案
                FileOutputStream fos = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.flush();
                fos.close();
                inputStream.close();

                // 保存檔案路徑到SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(KEY_PROFILE_IMAGE, imageFile.getAbsolutePath());
                editor.apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }
}


