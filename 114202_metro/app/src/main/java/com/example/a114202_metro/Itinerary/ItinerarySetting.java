package com.example.a114202_metro.Itinerary;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.a114202_metro.R;

import java.util.Calendar;

public class ItinerarySetting extends AppCompatActivity {

    EditText editTitleName, editStartDate;
    Button btn_confirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_itinerary_setting);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editTitleName = findViewById(R.id.edit_title_name);
        editStartDate = findViewById(R.id.edit_start_date);
        btn_confirm = findViewById(R.id.btn_confirm);

        // 防止日期欄位彈出鍵盤
        editStartDate.setInputType(InputType.TYPE_NULL);

        // 點擊彈出日期選擇器
        editStartDate.setOnClickListener(v -> showDatePickerDialog(editStartDate));

        // 按下確認按鈕，進入ItinerayActiviy
        btn_confirm.setOnClickListener(v -> {
            Intent intent = new Intent(ItinerarySetting.this, Itinerary.class);
            intent.putExtra("showCardView", true);
            startActivity(intent);
            finish(); // 結束ItinerarySetting頁面
        });

    }

    private void showDatePickerDialog(EditText targetEditText) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay;
                    targetEditText.setText(date);
                }, year, month, day);
        datePickerDialog.show();
    }
}
