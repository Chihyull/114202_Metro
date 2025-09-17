package com.example.a114202_metro.Station;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GestureDetectorCompat;
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

public class Route extends AppCompatActivity {

    private List<StationModel> stationList;
    private List<LineModel> lineList;
    private Set<String> lineCodeSet;
    private RecyclerView rvLineCodes, rvStation;
    private TextView stationTitle;
    private StationAdapter stationAdapter;
    private LineAdapter lineAdapter;

    // 新增：起訖輸入框與按鈕
    private EditText etStart, etEnd;
    private Button btnSearchRoute;

    // 追蹤目前正在輸入哪一個（true=起點；false=終點）
    private boolean isEditingStart = true;

    // 方案A：同時記住站碼（API 用）
    private String startCode = null, endCode = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_route);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 綁定原本元件
        stationTitle = findViewById(R.id.stationTitle);
        rvLineCodes = findViewById(R.id.rv_line_codes);
        rvStation = findViewById(R.id.rv_station);

        // 綁定新加入的元件
        etStart = findViewById(R.id.et_start);
        etEnd = findViewById(R.id.et_end);
        btnSearchRoute = findViewById(R.id.btn_search_route);

        rvLineCodes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvStation.setLayoutManager(new LinearLayoutManager(this));

        stationList = new ArrayList<>();
        lineList = new ArrayList<>();
        lineCodeSet = new HashSet<>();

        stationAdapter = new StationAdapter(stationList);
        rvStation.setAdapter(stationAdapter);

        // --- 搜尋與焦點邏輯 ---
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 使用目前焦點欄位的內容做篩選
                CharSequence query = isEditingStart ? etStart.getText() : etEnd.getText();
                stationAdapter.getFilter().filter(query);
                // 使用者正在輸入時，重設對應的站碼（避免殘留舊選取）
                if (isEditingStart) {
                    startCode = null;
                } else {
                    endCode = null;
                }
            }
            @Override public void afterTextChanged(Editable s) { }
        };
        etStart.addTextChangedListener(watcher);
        etEnd.addTextChangedListener(watcher);

        etStart.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                isEditingStart = true;
                stationAdapter.getFilter().filter(etStart.getText());
            }
        });
        etEnd.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                isEditingStart = false;
                stationAdapter.getFilter().filter(etEnd.getText());
            }
        });

        // 如果一開始沒有點任何輸入框，預設編輯起點
        etStart.requestFocus();

        // --- 點擊站點 → 自動帶入 & 記住站碼 ---
        attachStationClickToFill();

        // --- 點擊線路代碼，抓該線站點 ---
        lineAdapter = new LineAdapter(lineList, lineCode -> new GetStationsTask(lineCode).execute());
        rvLineCodes.setAdapter(lineAdapter);

        // --- 查詢按鈕：優先用站碼；若為手打名稱則嘗試回查站碼 ---
        btnSearchRoute.setOnClickListener(v -> {
            String startName = etStart.getText() != null ? etStart.getText().toString().trim() : "";
            String endName   = etEnd.getText()   != null ? etEnd.getText().toString().trim()   : "";
            if (startName.isEmpty() || endName.isEmpty()) {
                Toast.makeText(Route.this, "Please select start and end stations", Toast.LENGTH_SHORT).show();
                return;
            }
            if (startName.equalsIgnoreCase(endName)) {
                Toast.makeText(Route.this, "Start and end stations cannot be the same", Toast.LENGTH_SHORT).show();
                return;
            }

            // 使用者若是手打而非點選，嘗試用名稱回查站碼
            if (startCode == null) startCode = findCodeByName(startName);
            if (endCode   == null) endCode   = findCodeByName(endName);

            if (startCode == null || endCode == null) {
                Toast.makeText(Route.this, "Please pick stations from the list", Toast.LENGTH_SHORT).show();
                return;
            }

            // 開啟 PathStation Activity，傳遞名稱（顯示用）與站碼（API 用）
            Intent intent = new Intent(Route.this, PathStation.class);
            intent.putExtra("start_station", startName);
            intent.putExtra("end_station",   endName);
            intent.putExtra("start_code",    startCode);
            intent.putExtra("end_code",      endCode);
            startActivity(intent);

            hideKeyboard();
        });

        // 預設載入全部站點並初始化線路列表
        new GetStationsTask(null).execute();
    }

    private void attachStationClickToFill() {
        GestureDetectorCompat detector = new GestureDetectorCompat(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        View child = rvStation.findChildViewUnder(e.getX(), e.getY());
                        if (child != null) {
                            int pos = rvStation.getChildAdapterPosition(child);

                            // 讀取當前 cell 顯示的站名（item_station.xml 的 TextView）
                            String displayName = null;
                            TextView tv = child.findViewById(R.id.stationName);
                            if (tv != null) {
                                CharSequence t = tv.getText();
                                if (t != null && t.length() > 0) displayName = t.toString();
                            }

                            StationModel sel = null;

                            // 1) 優先用顯示名稱在 stationList 中找對應的 StationModel
                            if (displayName != null) {
                                for (StationModel s : stationList) {
                                    if (displayName.equalsIgnoreCase(s.getNameE())) {
                                        sel = s;
                                        break;
                                    }
                                }
                            }

                            // 2) 若找不到，退回用 pos（在 adapter 與 stationList 同步時可用）
                            if (sel == null && pos >= 0 && pos < stationList.size()) {
                                sel = stationList.get(pos);
                            }

                            if (sel != null) {
                                String pickedName = sel.getNameE();        // 顯示用
                                String pickedCode = sel.getStationCode();  // API 用（關鍵）

                                if (isEditingStart) {
                                    etStart.setText(pickedName);
                                    startCode = pickedCode;               // 記住起點站碼
                                    etEnd.requestFocus();
                                    isEditingStart = false;
                                } else {
                                    etEnd.setText(pickedName);
                                    endCode = pickedCode;                 // 記住終點站碼
                                    hideKeyboard();
                                }
                            }
                        }
                        return super.onSingleTapUp(e);
                    }
                });

        rvStation.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                detector.onTouchEvent(e);
                return false;
            }
        });
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    // 依名稱從 stationList 回查對應的 StationCode（手打時使用）
    private String findCodeByName(String name) {
        if (name == null || name.isEmpty()) return null;
        for (StationModel s : stationList) {
            if (name.equalsIgnoreCase(s.getNameE())) {
                return s.getStationCode();
            }
        }
        return null;
    }

    private class GetStationsTask extends AsyncTask<Void, Void, String> {
        private final String selectedLineCode;

        public GetStationsTask(String selectedLineCode) {
            this.selectedLineCode = selectedLineCode;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url;
                if (selectedLineCode != null && !selectedLineCode.isEmpty()) {
                    Uri built = Uri.parse(Constants.URL_GET_STATIONS)
                            .buildUpon()
                            .appendQueryParameter("lineCode", selectedLineCode)
                            .build();
                    url = new URL(built.toString());
                } else {
                    url = new URL(Constants.URL_GET_STATIONS);
                }

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

                    stationAdapter.setFilteredList(new ArrayList<>(stationList));
                    stationAdapter.notifyDataSetChanged();

                    if (isEditingStart) {
                        stationAdapter.getFilter().filter(etStart.getText());
                    } else {
                        stationAdapter.getFilter().filter(etEnd.getText());
                    }

                } catch (Exception e) {
                    Toast.makeText(Route.this, "解析錯誤", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(Route.this, "連線失敗", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
