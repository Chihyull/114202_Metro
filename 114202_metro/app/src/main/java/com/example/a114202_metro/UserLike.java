package com.example.a114202_metro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.a114202_metro.Itinerary.Itinerary;

public class UserLike extends AppCompatActivity {

    private CardView dialogCard;
    private LinearLayout layoutFolderItem;
    private Button btnCreate, btnCreateLike;
    private EditText editTextFolderName;
    private TextView textFolderName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_like);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 綁定元件
        dialogCard = findViewById(R.id.dialog_card);
        layoutFolderItem = findViewById(R.id.layout_folder_item);
        btnCreate = findViewById(R.id.btn_create);
        btnCreateLike = findViewById(R.id.btn_create_like);
        editTextFolderName = findViewById(R.id.editText_folder_name);
        textFolderName = findViewById(R.id.text_folder_name);

        // 點擊 "+" 顯示 dialog
        btnCreate.setOnClickListener(v -> {
            dialogCard.setVisibility(View.VISIBLE);
        });

        // 點擊 "Create" 關閉 dialog，顯示資料夾
        btnCreateLike.setOnClickListener(v -> {
            String folderName = editTextFolderName.getText().toString().trim();

            if (!folderName.isEmpty()) {
                textFolderName.setText(folderName); // 設定資料夾名稱
                dialogCard.setVisibility(View.GONE); // 隱藏對話框
                layoutFolderItem.setVisibility(View.VISIBLE); // 顯示資料夾項目

                // 若有其他內容（like圖示、提示文字）應該也隱藏
                findViewById(R.id.like).setVisibility(View.GONE);
                findViewById(R.id.content2).setVisibility(View.GONE);
                findViewById(R.id.content3).setVisibility(View.GONE);
            } else {
                Toast.makeText(this, "Folder name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

    }



}