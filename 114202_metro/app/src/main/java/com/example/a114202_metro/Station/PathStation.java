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
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.a114202_metro.Constants;
import com.example.a114202_metro.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathStation extends AppCompatActivity {

    private TextView tvStart, tvEnd;
    private TextView tvShortestPath;
    private TextView tvFullPrice, tvElderPrice, tvTaipeiChildPrice, tvTotalDistance;

    private RequestQueue queue;

    // Intent 帶入（只顯示，不被覆蓋）
    private String startNameIntent, endNameIntent, startCodeIntent, endCodeIntent;

    // 站碼→站名 快取
    private final Map<String, String> codeToName = new HashMap<>();
    private boolean stationMapReady = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_station);

        // 綁定 UI
        tvStart            = findViewById(R.id.tv_start_station);
        tvEnd              = findViewById(R.id.tv_end_station);
        tvShortestPath     = findViewById(R.id.tv_shortest_path);
        tvFullPrice        = findViewById(R.id.tv_full_price);
        tvElderPrice       = findViewById(R.id.tv_elder_price);
        tvTaipeiChildPrice = findViewById(R.id.tv_taipei_child_price);
        tvTotalDistance    = findViewById(R.id.tv_total_distance);

        queue = Volley.newRequestQueue(this);

        // 取 Route 傳來的資料
        startNameIntent = getIntent().getStringExtra("start_station");
        endNameIntent   = getIntent().getStringExtra("end_station");
        startCodeIntent = getIntent().getStringExtra("start_code");
        endCodeIntent   = getIntent().getStringExtra("end_code");

        // 檢查起迄站碼
        if (TextUtils.isEmpty(startCodeIntent) || TextUtils.isEmpty(endCodeIntent)) {
            Toast.makeText(this, "缺少起點或終點站碼", Toast.LENGTH_SHORT).show();
            tvStart.setText("Start Station");
            tvEnd.setText("End Station");
            resetResultViews();
            return;
        }

        // 顯示站名（若無則顯示站碼），後續不再覆蓋
        tvStart.setText(!TextUtils.isEmpty(startNameIntent) ? startNameIntent : startCodeIntent);
        tvEnd.setText(!TextUtils.isEmpty(endNameIntent) ? endNameIntent : endCodeIntent);

        // 初始顯示
        tvShortestPath.setText("查詢中…");
        tvFullPrice.setText("—");
        tvElderPrice.setText("—");
        tvTaipeiChildPrice.setText("—");
        tvTotalDistance.setText("—");

        // 先確保有站點對照表，再查最短路徑
        ensureStationMap(() -> fetchShortestPath(startCodeIntent, endCodeIntent));
    }

    /** 確保已抓到站點清單（StationCode / NameE），抓過就不再抓 */
    private void ensureStationMap(Runnable onReady) {
        if (stationMapReady) {
            onReady.run();
            return;
        }

        // 假設你已在 Constants 定義 getStations 的 URL（可回傳 JSONArray）
        // 允許回傳格式：
        //  1) [{"StationCode":"BR01","NameE":"Wenhu"}, ...]
        //  2) [{"station_code":"BR01","name_en":"Wenhu"}, ...]
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                Constants.URL_GET_STATIONS,
                null,
                arr -> {
                    try {
                        codeToName.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.optJSONObject(i);
                            if (o == null) continue;

                            String code =
                                    o.optString("StationCode",
                                            o.optString("station_code",
                                                    o.optString("code", ""))).trim();
                            String nameE =
                                    o.optString("NameE",
                                            o.optString("name_en",
                                                    o.optString("nameE", ""))).trim();

                            if (!code.isEmpty() && !nameE.isEmpty()) {
                                codeToName.put(code, nameE);
                            }
                        }
                        stationMapReady = true;
                    } catch (Exception ignore) {
                        stationMapReady = !codeToName.isEmpty();
                    }
                    onReady.run();
                },
                error -> {
                    // 抓不到也不中斷，後面會用代碼退回
                    stationMapReady = false;
                    onReady.run();
                }
        );

        req.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                1,
                1.0f
        ));
        queue.add(req);
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
                        if (!found) {
                            tvShortestPath.setText("找不到路徑");
                            fillFareAndDistance(response); // 仍可顯示票價/距離（若有）
                            return;
                        }

                        // 顯示路徑：把每個站碼轉成站名
                        JSONArray pathArr = response.optJSONArray("path");
                        if (pathArr != null && pathArr.length() > 0) {
                            List<String> nodes = new ArrayList<>();
                            for (int i = 0; i < pathArr.length(); i++) {
                                String code = String.valueOf(pathArr.opt(i));
                                nodes.add(resolveName(code));
                            }
                            tvShortestPath.setText(joinWithArrow(nodes));
                        } else {
                            tvShortestPath.setText("（路徑資料空白）");
                        }

                        // 顯示票價與距離
                        fillFareAndDistance(response);

                    } catch (Exception e) {
                        handleError("解析失敗：" + e.getMessage());
                    }
                },
                error -> handleError("網路或伺服器錯誤：" + error.getMessage())
        );

        req.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                1,
                1.0f
        ));
        queue.add(req);
    }

    /** 站碼 → 站名（找不到就回代碼） */
    private String resolveName(String stationCode) {
        String name = codeToName.get(stationCode);
        return (name != null && !name.isEmpty()) ? name : stationCode;
    }

    /** 將路徑以 " → " 連接（不依賴 String.join） */
    private String joinWithArrow(List<String> nodes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) sb.append(" \u2192 "); // →
            sb.append(nodes.get(i));
        }
        return sb.toString();
    }

    private void fillFareAndDistance(JSONObject response) {
        // 票價整數
        int full = response.optInt("full_price", -1);
        int elder = response.optInt("elder_price", -1);
        int tpeChild = response.optInt("taipei_child_price", -1);

        tvFullPrice.setText(full >= 0 ? "NT$" + full : "—");
        tvElderPrice.setText(elder >= 0 ? "NT$" + elder : "—");
        tvTaipeiChildPrice.setText(tpeChild >= 0 ? "NT$" + tpeChild : "—");

        // 距離浮點數（km）
        double dist = response.optDouble("distance", Double.NaN);
        if (!Double.isNaN(dist)) {
            DecimalFormat df = new DecimalFormat("#.##");
            tvTotalDistance.setText(df.format(dist) + " km");
        } else {
            tvTotalDistance.setText("—");
        }
    }

    private void handleError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        tvShortestPath.setText("—");
        tvFullPrice.setText("—");
        tvElderPrice.setText("—");
        tvTaipeiChildPrice.setText("—");
        tvTotalDistance.setText("—");
    }

    private void resetResultViews() {
        tvShortestPath.setText("—");
        tvFullPrice.setText("—");
        tvElderPrice.setText("—");
        tvTaipeiChildPrice.setText("—");
        tvTotalDistance.setText("—");
    }
}


