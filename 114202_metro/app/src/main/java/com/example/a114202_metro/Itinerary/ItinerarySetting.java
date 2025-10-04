package com.example.a114202_metro.Itinerary;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.a114202_metro.Constants;
import com.example.a114202_metro.R;
import com.example.a114202_metro.Station.Station;
import com.google.android.material.datepicker.MaterialDatePicker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ItinerarySetting extends AppCompatActivity {

    EditText editTitleName, editStartDate, editDest;
    Button btn_confirm;

    // 送後端用
    private String startDateStr = null;
    private String endDateStr   = null;
    private String destStationCode = null;

    // 使用者 Gmail（請改成你 Google Sign-In 取得的值）
    private String currentUserGmail = "11336010@ntub.edu.tw"; // TODO: 用你的登入 Gmail 取代

    // 站點快取（從 getStations.php 抓）
    private final ArrayList<StationItem> stationList = new ArrayList<>();
    private final ArrayList<String> stationDisplay = new ArrayList<>();
    private boolean stationsLoaded = false;
    private boolean aiMode = false;

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

        boolean aiMode = getIntent().getBooleanExtra("EXTRA_AI_MODE", false);

        editTitleName = findViewById(R.id.edit_title_name);
        editStartDate = findViewById(R.id.edit_start_date);
        editDest      = findViewById(R.id.edit_dest);
        btn_confirm   = findViewById(R.id.btn_confirm);

        // 日期欄位：一次選起訖（Material Date Range）
        editStartDate.setInputType(InputType.TYPE_NULL);
        editStartDate.setFocusable(false);
        editStartDate.setOnClickListener(v -> showMaterialRangePicker());

        // 目的地：點一下開「可搜尋站點」對話框
        editDest.setInputType(InputType.TYPE_NULL);
        editDest.setFocusable(false);
        editDest.setOnClickListener(v -> {
            if (!stationsLoaded) {
                loadStationsAndShowSearchDialog();
            } else {
                showStationSearchDialog();
            }
        });

        // 新增行程
        btn_confirm.setOnClickListener(v -> {
            String title = editTitleName.getText().toString().trim();
            if (title.isEmpty()) { toast("請輸入行程名稱"); return; }
            if (startDateStr == null || endDateStr == null) { toast("請選擇起訖日期"); return; }
            if (destStationCode == null || destStationCode.isEmpty()) { toast("請選擇目的地站點"); return; }

            createItineraryOnServer(currentUserGmail, title, startDateStr, endDateStr, destStationCode);
        });

        TextView tvTitle = findViewById(R.id.title_itisetting);
        if (aiMode) {
            tvTitle.setText("AI Itinerary");
            btn_confirm.setText("Generate with AI");
        } else {
            tvTitle.setText(getString(R.string.title_itisetting));
            btn_confirm.setText(getString(R.string.btn_confirm));
        }
    }

    /** Material Date Range Picker：一次選起訖日期 */
    private void showMaterialRangePicker() {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("選擇行程日期");

        long today = MaterialDatePicker.todayInUtcMilliseconds();
        builder.setSelection(new androidx.core.util.Pair<>(today, today));

        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) return;
            Long startUtc = selection.first;
            Long endUtc   = (selection.second != null ? selection.second : selection.first);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.TAIWAN);
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Taipei"));
            startDateStr = sdf.format(new Date(startUtc));
            endDateStr   = sdf.format(new Date(endUtc));

            editStartDate.setText(startDateStr + " ~ " + endDateStr);
        });
        picker.show(getSupportFragmentManager(), "date_range_picker");
    }

    /** 取站點（一次）→ 顯示「搜尋」對話框 */
    private void loadStationsAndShowSearchDialog() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = Constants.URL_GET_STATIONS;

        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray arr = new JSONArray(response);
                        stationList.clear();
                        stationDisplay.clear();

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);
                            String code = o.optString("StationCode", "");
                            String name = o.optString("NameE", o.optString("Station", code));
                            String line = o.optString("Line", o.optString("LineCode", ""));

                            StationItem item = new StationItem(code, name, line);
                            stationList.add(item);
                            stationDisplay.add(formatDisplay(item));
                        }
                        stationsLoaded = true;
                        showStationSearchDialog();
                    } catch (Exception e) {
                        toast("站點載入失敗（解析錯誤）");
                    }
                },
                error -> toast("站點載入失敗：" + (error.getMessage() != null ? error.getMessage() : ""))
        );

        req.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        queue.add(req);
    }

    /** 顯示「可搜尋」的站點清單（上方搜尋欄、下方清單即時篩選） */
    private void showStationSearchDialog() {
        if (stationDisplay.isEmpty()) {
            toast("目前沒有可選站點");
            return;
        }

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText etSearch = new EditText(this);
        etSearch.setHint("搜尋站名 / 線名 / 代碼");
        container.addView(etSearch);

        ListView listView = new ListView(this);
        ArrayList<String> data = new ArrayList<>(stationDisplay);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, data);
        listView.setAdapter(adapter);
        container.addView(listView);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("選擇目的地站點")
                .setView(container)
                .setNegativeButton("取消", null)
                .create();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String display = (String) parent.getItemAtPosition(position);
            StationItem chosen = parseDisplayToStation(display);
            if (chosen != null) {
                destStationCode = chosen.code;   // 後端要的值
                editDest.setText(chosen.name);   // 顯示站名
                dialog.dismiss();
            } else {
                toast("解析站點失敗，請再試一次");
            }
        });

        dialog.show();
    }

    /** 從顯示字串反查 StationItem（解析最後的 (CODE)） */
    private StationItem parseDisplayToStation(String display) {
        try {
            int l = display.lastIndexOf('(');
            int r = display.lastIndexOf(')');
            if (l >= 0 && r > l) {
                String code = display.substring(l + 1, r);
                for (StationItem s : stationList) {
                    if (code.equals(s.code)) return s;
                }
            }
        } catch (Exception ignore) {}
        // 退而求其次：完全比對
        for (int i = 0; i < stationDisplay.size(); i++) {
            if (stationDisplay.get(i).equals(display)) return stationList.get(i);
        }
        return null;
    }

    /** 呼叫後端建立行程（沿用 Constants.URL_CREATE_ITINERARY） */
    private void createItineraryOnServer(String gmail, String title, String start, String end, String destCode) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest req = new StringRequest(Request.Method.POST, Constants.URL_CREATE_ITINERARY,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        boolean error = obj.optBoolean("error", true);
                        if (!error) {
                            // 成功 → 回到 Itinerary 顯示 CardView
                            Intent intent = new Intent(ItinerarySetting.this, Itinerary.class);
                            intent.putExtra("showCardView", true);
                            intent.putExtra("title", title);
                            intent.putExtra("start_date", start);
                            intent.putExtra("end_date", end);
                            intent.putExtra("dest", destCode);
                            startActivity(intent);
                            finish();
                        } else {
                            toast(obj.optString("message", "建立失敗"));
                        }
                    } catch (Exception e) {
                        toast("回應解析失敗");
                    }
                },
                error -> toast("網路或伺服器錯誤：" + (error.getMessage() != null ? error.getMessage() : ""))
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("gmail", gmail);
                p.put("title", title);
                p.put("start_date", start);
                p.put("end_date", end);
                p.put("dest", destCode);
                return p;
            }
        };

        req.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        queue.add(req);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /** 站點資料容器 */
    private static class StationItem {
        final String code;
        final String name;
        final String line;
        StationItem(String code, String name, String line) {
            this.code = code; this.name = name; this.line = line;
        }
    }

    private String formatDisplay(StationItem item) {
        if (item.line != null && !item.line.isEmpty()) {
            return item.name + " [" + item.line + "] (" + item.code + ")";
        }
        return item.name + " (" + item.code + ")";
    }
}


