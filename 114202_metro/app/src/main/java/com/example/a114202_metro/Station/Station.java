package com.example.a114202_metro.Station;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Station extends AppCompatActivity {

    private List<StationModel> stationList;
    private List<LineModel> lineList;
    private Set<String> lineCodeSet;
    private RecyclerView rvLineCodes, rvStation;
    private TextView stationTitle;
    private StationAdapter stationAdapter;
    private LineAdapter lineAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station);

        stationTitle = findViewById(R.id.stationTitle);
        rvLineCodes = findViewById(R.id.rv_line_codes);
        rvStation = findViewById(R.id.rv_station);

        rvLineCodes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvStation.setLayoutManager(new LinearLayoutManager(this));

        stationList = new ArrayList<>();
        lineList = new ArrayList<>();
        lineCodeSet = new HashSet<>();

        stationAdapter = new StationAdapter(stationList);
        rvStation.setAdapter(stationAdapter);

        lineAdapter = new LineAdapter(lineList, lineCode -> {
            // 點擊 lineCode 後，重新抓該線站點並顯示
            new GetStationsTask(lineCode).execute();
        });
        rvLineCodes.setAdapter(lineAdapter);

        // 一開始抓所有站點，並初始化 lineList
        new GetStationsTask(null).execute();

    }

    private class GetStationsTask extends AsyncTask<Void, Void, String> {
        private String selectedLineCode;

        public GetStationsTask(String selectedLineCode) {
            this.selectedLineCode = selectedLineCode;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                String urlStr = Constants.URL_GET_STATIONS;
                if (selectedLineCode != null) {
                    urlStr += "?lineCode=" + URLEncoder.encode(selectedLineCode, "UTF-8");
                }
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                return result.toString();

            } catch (Exception e) {
                Log.e("Station", "Error fetching stations", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String json) {
            if (json != null) {
                try {
                    JSONArray jsonArray = new JSONArray(json);
                    stationList.clear();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String stationCode = obj.getString("StationCode");
                        String nameE = obj.getString("NameE");
                        String line = obj.getString("Line");
                        String lineCode = obj.getString("LineCode");

                        stationList.add(new StationModel(stationCode, nameE, line, lineCode));
                    }

                    // 抽出唯一的 LineCode 及 Line 名稱
                    if (selectedLineCode == null) {
                        // 只在第一次（全部資料）時整理 lineList
                        lineList.clear();
                        lineCodeSet.clear();

                        for (StationModel s : stationList) {
                            if (!lineCodeSet.contains(s.getLineCode())) {
                                lineCodeSet.add(s.getLineCode());
                                lineList.add(new LineModel(s.getLineCode()));
                            }
                        }

                        lineAdapter.notifyDataSetChanged();

                        // 預設標題為第一筆站點的線名
                        if (!stationList.isEmpty()) {
                            stationTitle.setText(stationList.get(0).getLine());
                        }
                    } else {
                        // 選擇特定線時，標題改成該線名稱 (stationList 全是同一線)
                        if (!stationList.isEmpty()) {
                            stationTitle.setText(stationList.get(0).getLine());
                        }
                    }

                    stationAdapter.notifyDataSetChanged();

                } catch (Exception e) {
                    Toast.makeText(Station.this, "解析錯誤", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(Station.this, "連線失敗", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
