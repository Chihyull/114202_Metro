package com.example.a114202_metro.Station;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.a114202_metro.R;

public class PathStation extends AppCompatActivity {

    private TextView tvStart, tvEnd, tvTime;
    private String startStation, endStation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // ⚠️ 如果你的 layout 檔名其實是 activity_path_station.xml，就改回 R.layout.activity_path_station
        setContentView(R.layout.activity_path_station);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        tvStart = findViewById(R.id.tv_start_station);
        tvEnd   = findViewById(R.id.tv_end_station);
        tvTime  = findViewById(R.id.tv_travel_time);

        // 取得 Route 傳來的起訖站
        startStation = getIntent().getStringExtra("start_station");
        endStation   = getIntent().getStringExtra("end_station");

        // 先把起訖直接顯示
        tvStart.setText(startStation != null ? startStation : "Start Station");
        tvEnd.setText(endStation != null ? endStation : "End Station");

        // 如果沒帶資料就不打 API
        if (startStation == null || endStation == null
                || startStation.trim().isEmpty() || endStation.trim().isEmpty()) {
            Toast.makeText(this, "Missing start or end station", Toast.LENGTH_SHORT).show();
            return;
        }

        // 呼叫最短路徑 API（把 this 傳進去）
        new FetchShortestPathTask(this, startStation.trim(), endStation.trim()).execute();
    }

    // 讓 FetchShortestPathTask 能更新這個頁面的時間 TextView
    public void setTravelTimeText(String text) {
        if (tvTime != null) tvTime.setText(text);
    }
}
