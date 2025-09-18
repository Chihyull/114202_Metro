package com.example.a114202_metro.Station;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.a114202_metro.Constants;
import com.example.a114202_metro.R;

public class PathStation extends AppCompatActivity {

    private TextView tvStart, tvEnd, tvTime;
    private RequestQueue queue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_station);

        tvStart = findViewById(R.id.tv_start_station);
        tvEnd   = findViewById(R.id.tv_end_station);
        tvTime  = findViewById(R.id.tv_travel_time);

        queue = Volley.newRequestQueue(this);

        // 從上一個畫面帶過來的起訖站（建議 key 用 "start" / "end"）
        // 取出 Route 傳來的資料 —— 名稱用來顯示，代碼用來打 API
        String startName = getIntent().getStringExtra("start_station");
        String endName   = getIntent().getStringExtra("end_station");
        String startCode = getIntent().getStringExtra("start_code");
        String endCode   = getIntent().getStringExtra("end_code");

        // 基本檢查
        if (TextUtils.isEmpty(startCode) || TextUtils.isEmpty(endCode)) {
            Toast.makeText(this, "缺少起點或終點站碼", Toast.LENGTH_SHORT).show();
            tvStart.setText("Start Station");
            tvEnd.setText("End Station");
            tvTime.setText("Travel Time: —");
            return;
        }

// 先顯示站名（若沒帶到就退回顯示站碼）
        tvStart.setText(!TextUtils.isEmpty(startName) ? startName : startCode);
        tvEnd.setText(!TextUtils.isEmpty(endName) ? endName : endCode);
        tvTime.setText("Travel Time: 查詢中…");

// 用「站碼」打後端
        fetchShortestPath(startCode, endCode);

    }

    private void fetchShortestPath(String start, String end) {
        Uri uri = Uri.parse(Constants.URL_GET_SHORTEST_PATH)
                .buildUpon()
                .appendQueryParameter("start", start)
                .appendQueryParameter("end", end)
                .build();

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                uri.toString(),
                null,
                response -> {
                    try {
                        boolean ok = response.optBoolean("ok", false);
                        if (!ok) {
                            String err = response.optString("error", "Unknown error");
                            handleError("後端回傳錯誤: " + err);
                            return;
                        }

                        boolean found = response.optBoolean("found", false);
                        String respStart = response.optString("start", "");
                        String respEnd   = response.optString("end", "");
                        // 若後端回傳的站名有可能較完整/標準化，優先顯示回傳值
                        if (!TextUtils.isEmpty(respStart)) tvStart.setText(respStart);
                        if (!TextUtils.isEmpty(respEnd))   tvEnd.setText(respEnd);

                        if (!found) {
                            tvTime.setText("Travel Time: 找不到路徑");
                            return;
                        }

                        int totalTime = response.optInt("total_time", -1);
                        if (totalTime >= 0) {
                            int minutes = Math.round(totalTime / 60f);
                            tvTime.setText("Travel Time: " + minutes + " 分鐘");
                        } else {
                            tvTime.setText("Travel Time: —");
                        }


                        // 如果你想顯示路徑節點，可自行取出 response.optJSONArray("path")

                    } catch (Exception e) {
                        handleError("解析失敗：" + e.getMessage());
                    }
                },
                error -> handleError("網路或伺服器錯誤：" + error.getMessage())
        );

        // 可視需求調整逾時與重試
        req.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                1,
                1.0f
        ));

        queue.add(req);
    }

    private void handleError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        tvTime.setText("Travel Time: —");
    }
}

