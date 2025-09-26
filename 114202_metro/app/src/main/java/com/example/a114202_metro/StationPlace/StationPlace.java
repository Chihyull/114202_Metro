package com.example.a114202_metro.StationPlace;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.a114202_metro.Constants;
import com.example.a114202_metro.R;
import com.example.a114202_metro.StationPlace.ExitAdapter;
import com.example.a114202_metro.StationPlace.ExitModel;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StationPlace extends AppCompatActivity {

    private TextView tvTitle;
    private RecyclerView rvExitCodes;   // 出口清單（橫向）
    private RecyclerView rvPlaces;      // 該出口的地點清單（直向）

    private ExitAdapter exitAdapter;
    private PlaceAdapter placeAdapter;

    private String stationCode, stationName;

    // key = ExitCode, value = List<PlaceModel>
    private final Map<String, List<PlaceModel>> exitPlaces = new LinkedHashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_place);

        // 1) 取參數
        stationCode = getIntent().getStringExtra("station_code");
        stationName = getIntent().getStringExtra("station_name");

        // 2) 綁定 View
        tvTitle = findViewById(R.id.stationTitle);
        rvExitCodes = findViewById(R.id.rv_exit_codes);
        rvPlaces = findViewById(R.id.rv_station);

        // 3) 標題
        if (!TextUtils.isEmpty(stationName)) tvTitle.setText(stationName);

        // 4) 出口 RecyclerView：橫向
        rvExitCodes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        exitAdapter = new ExitAdapter();
        rvExitCodes.setAdapter(exitAdapter);

        // 出口點擊切換地點（直接吃後端已解析好的 PlaceModel）
        exitAdapter.setOnExitClickListener(exitCode -> {
            List<PlaceModel> list = exitPlaces.get(exitCode);
            if (list == null || list.isEmpty()) {
                placeAdapter.submit(Collections.emptyList());
                Toast.makeText(this, "此出口尚無地點資料", Toast.LENGTH_SHORT).show();
                return;
            }
            placeAdapter.submit(list);
        });

        // 5) 地點 RecyclerView：直向
        rvPlaces.setLayoutManager(new LinearLayoutManager(this));
        placeAdapter = new PlaceAdapter();
        rvPlaces.setAdapter(placeAdapter);

        // 6) 取出口 + Places
        if (!TextUtils.isEmpty(stationCode)) {
            new FetchExitsTask(stationCode).execute();
        } else {
            Toast.makeText(this, "缺少站點代碼，無法載入出口", Toast.LENGTH_SHORT).show();
        }
    }

    /* ---------------------- 取出口與 Places ---------------------- */
    private class FetchExitsTask extends AsyncTask<Void, Void, List<String>> {
        private final String sc;

        FetchExitsTask(String sc) { this.sc = sc; }

        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> exitOrder = new ArrayList<>();
            try {
                String urlStr = Constants.URL_GET_STATIONS_EXITS +
                        "?stationCode=" + URLEncoder.encode(sc, "UTF-8");
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject root = new JSONObject(sb.toString());
                if (!root.optBoolean("ok", false)) return exitOrder;

                JSONArray arr = root.optJSONArray("data");
                if (arr == null) return exitOrder;

                // 清空舊資料
                exitPlaces.clear();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);

                    // 出口代碼
                    String exitCode = o.optString("ExitCode", "");
                    if (TextUtils.isEmpty(exitCode)) continue;

                    // 站名（如果前面沒傳進來，就以後端為準）
                    if (TextUtils.isEmpty(stationName)) {
                        String backendStationName = o.optString("StationName", "");
                        if (!TextUtils.isEmpty(backendStationName)) {
                            stationName = backendStationName;
                        }
                    }

                    // 解析 Places：[{ PlaceName, photo }]
                    List<PlaceModel> places = new ArrayList<>();
                    JSONArray placesArr = o.optJSONArray("Places");
                    if (placesArr != null) {
                        for (int j = 0; j < placesArr.length(); j++) {
                            JSONObject p = placesArr.optJSONObject(j);
                            if (p == null) continue;

                            String placeName = p.optString("PlaceName", "");
                            String photoUrl  = p.optString("photo", null);

                            // 後端現在不再提供 placeId → 給空字串即可
                            places.add(new PlaceModel("", placeName, photoUrl));
                        }
                    }

                    exitPlaces.put(exitCode, places);

                    // 出口排序
                    if (!exitOrder.contains(exitCode)) exitOrder.add(exitCode);
                }

                return exitOrder;

            } catch (Exception e) {
                Log.e("StationPlace", "fetch exits error", e);
                return exitOrder;
            }
        }

        @Override
        protected void onPostExecute(List<String> exits) {
            if (!TextUtils.isEmpty(stationName)) tvTitle.setText(stationName);

            if (exits == null || exits.isEmpty()) {
                Toast.makeText(StationPlace.this, "沒有出口資料", Toast.LENGTH_SHORT).show();
                placeAdapter.submit(Collections.emptyList());
                exitAdapter.submit(Collections.emptyList());
                return;
            }

            // 給出口 RV
            List<ExitModel> models = new ArrayList<>();
            for (String e : exits) models.add(new ExitModel(e));
            exitAdapter.submit(models);

            // 預設載入第一個出口的地點
            List<PlaceModel> first = exitPlaces.get(exits.get(0));
            placeAdapter.submit(first == null ? Collections.emptyList() : first);
        }
    }
}

