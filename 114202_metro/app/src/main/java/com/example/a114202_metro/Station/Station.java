package com.example.a114202_metro.Station;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
    private EditText editText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_station);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editText = findViewById(R.id.filter_station);
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

        // 搜尋功能
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                stationAdapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        // 點擊線路代碼，呼叫抓取該線站點
        lineAdapter = new LineAdapter(lineList, lineCode -> {
            new GetStationsTask(lineCode).execute();
        });
        rvLineCodes.setAdapter(lineAdapter);

        // 預設載入全部站點並初始化線路列表
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

                    // 如果 selectedLineCode 是 null，代表是載入全部，初始化 lineList
                    if (selectedLineCode == null) {
                        lineList.clear();
                        lineCodeSet.clear();

                        for (StationModel s : stationList) {
                            if (!lineCodeSet.contains(s.getLineCode())) {
                                lineCodeSet.add(s.getLineCode());
                                lineList.add(new LineModel(s.getLineCode()));
                            }
                        }
                        lineAdapter.notifyDataSetChanged();
                    }

                    // 設定標題為第一筆資料的 line 字段 (如果有資料)
                    if (!stationList.isEmpty()) {
                        stationTitle.setText(stationList.get(0).getLine());
                    } else {
                        stationTitle.setText("無資料");
                    }

                    // 更新 Adapter 內資料並刷新列表
                    stationAdapter.setFilteredList(new ArrayList<>(stationList));  // 建議複製一份給 Adapter
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


