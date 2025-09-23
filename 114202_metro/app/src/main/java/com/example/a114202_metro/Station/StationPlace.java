package com.example.a114202_metro.Station;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.a114202_metro.Constants;
import com.example.a114202_metro.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class StationPlace extends AppCompatActivity {

    private TextView tvTitle;
    private RecyclerView rvExitCodes;
    private ExitAdapter exitAdapter;
    private String stationCode, stationName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_place);

        // 1) 取參數（由 StationAdapter putExtra 帶入）
        stationCode = getIntent().getStringExtra("station_code");
        stationName = getIntent().getStringExtra("station_name");

        // 2) 綁定 View
        tvTitle = findViewById(R.id.stationTitle);
        rvExitCodes = findViewById(R.id.rv_exit_codes);

        // 3) 設定標題為站名
        if (stationName != null && !stationName.isEmpty()) {
            tvTitle.setText(stationName);
        }

        // 4) 出口 RecyclerView 橫向
        rvExitCodes.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        exitAdapter = new ExitAdapter();
        rvExitCodes.setAdapter(exitAdapter);

        // 5) 取出口清單
        if (stationCode != null && !stationCode.isEmpty()) {
            new FetchExitsTask(stationCode).execute();
        } else {
            Toast.makeText(this, "缺少站點代碼，無法載入出口", Toast.LENGTH_SHORT).show();
        }
    }

    private class FetchExitsTask extends AsyncTask<Void, Void, List<ExitModel>> {
        private final String sc;

        FetchExitsTask(String sc) { this.sc = sc; }

        @Override
        protected List<ExitModel> doInBackground(Void... voids) {
            List<ExitModel> out = new ArrayList<>();
            try {
                String urlStr = Constants.URL_GET_STATIONS_EXITS +
                        "?stationCode=" + URLEncoder.encode(sc, "UTF-8");
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                // 後端回傳格式（之前給你的）：
                // { ok:true, data:[ {StationCode, NameE, ExitCode}, ... ] }
                JSONObject root = new JSONObject(sb.toString());
                if (!root.optBoolean("ok", false)) return out;

                JSONArray arr = root.optJSONArray("data");
                if (arr == null) return out;

                // 用 Set 去重並維持順序
                Set<String> exits = new LinkedHashSet<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    String exitCode = o.optString("ExitCode", "");
                    if (!exitCode.isEmpty()) exits.add(exitCode);
                }
                for (String e : exits) out.add(new ExitModel(e));
                return out;

            } catch (Exception e) {
                Log.e("StationPlace", "fetch exits error", e);
                return out;
            }
        }

        @Override
        protected void onPostExecute(List<ExitModel> exits) {
            if (exits == null || exits.isEmpty()) {
                Toast.makeText(StationPlace.this, "沒有出口資料", Toast.LENGTH_SHORT).show();
                return;
            }
            exitAdapter.submit(exits);
        }
    }
}
