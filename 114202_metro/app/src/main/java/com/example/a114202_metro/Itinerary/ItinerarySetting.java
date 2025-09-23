package com.example.a114202_metro.Itinerary;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a114202_metro.R;
import com.example.a114202_metro.Station.StationModel;
import com.example.a114202_metro.Constants; // 與 Station.java 相同的常數（內含 URL_GET_STATIONS）
import com.google.android.material.datepicker.MaterialDatePicker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
// 如果要依線別載入可再打開 URLEncoder
// import java.net.URLEncoder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ItinerarySetting extends AppCompatActivity {

    EditText editTitleName, editStartDate, editDest;
    Button btn_confirm;

    // 日期區間（毫秒）
    private long startDateMillis = -1L;
    private long endDateMillis   = -1L;

    // 目的地（站點）
    private String destinationDisplay = ""; // 例："BL12 Shandao Temple"
    private String destinationCode    = ""; // 例："BL12"

    // 站點資料（與 Station.java 同來源）
    private final ArrayList<StationModel> stationList = new ArrayList<>();
    private final ArrayList<String> stationDisplayList = new ArrayList<>(); // "Code NameE" 顯示用

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
        editDest      = findViewById(R.id.edit_dest);
        btn_confirm   = findViewById(R.id.btn_confirm);

        // ====== 日期區間欄位：只可點擊，不彈鍵盤 ======
        editStartDate.setInputType(InputType.TYPE_NULL);
        editStartDate.setKeyListener(null);
        editStartDate.setFocusable(false);
        editStartDate.setFocusableInTouchMode(false);
        editStartDate.setOnClickListener(v -> showDateRangePicker());

        // ====== 目的地欄位：只可點擊，不彈鍵盤；點擊後開站點彈窗 ======
        editDest.setInputType(InputType.TYPE_NULL);
        editDest.setKeyListener(null);
        editDest.setFocusable(false);
        editDest.setFocusableInTouchMode(false);
        editDest.setOnClickListener(v -> {
            if (stationDisplayList.isEmpty()) {
                // 尚未載入 → 先載一次，成功後再開彈窗
                new FetchStationsTask(this::showStationPickerDialog).execute();
            } else {
                showStationPickerDialog();
            }
        });

        // （可選）預先載一次，提升首次點擊體驗
        new FetchStationsTask(null).execute();

        // ====== 確認：帶資料到 Itinerary ======
        btn_confirm.setOnClickListener(v -> {
            Intent intent = new Intent(ItinerarySetting.this, Itinerary.class);
            intent.putExtra("showCardView", true);

            // 日期區間
            intent.putExtra("dateRangeText", editStartDate.getText() != null ? editStartDate.getText().toString() : "");
            intent.putExtra("dateStartMillis", startDateMillis);
            intent.putExtra("dateEndMillis", endDateMillis);

            // 目的地（站點）
            intent.putExtra("destinationDisplay", destinationDisplay);
            intent.putExtra("destinationCode", destinationCode);

            startActivity(intent);
            finish();
        });
    }

    // ───────────────── 日期區間選擇 ─────────────────
    private void showDateRangePicker() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("選擇行程日期區間");

        if (startDateMillis > 0 && endDateMillis > 0) {
            builder.setSelection(new Pair<>(startDateMillis, endDateMillis));
        }

        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                startDateMillis = selection.first != null ? selection.first : -1L;
                endDateMillis   = selection.second != null ? selection.second : -1L;
                if (startDateMillis > 0 && endDateMillis > 0) {
                    editStartDate.setText(formatRange(startDateMillis, endDateMillis));
                }
            }
        });

        picker.show(getSupportFragmentManager(), "date_range_picker");
    }

    private String formatRange(long startMillis, long endMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN);
        return sdf.format(new Date(startMillis)) + " – " + sdf.format(new Date(endMillis));
    }

    // ───────────────── 站點彈窗（動態建立：搜尋框 + RecyclerView） ─────────────────
    // ───────────────── 站點彈窗（動態建立：搜尋框 + RecyclerView） ─────────────────
    private void showStationPickerDialog() {
        // 外層
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        container.setPadding(pad, pad, pad, pad);

        // 搜尋輸入框
        EditText etSearch = new EditText(this);
        etSearch.setHint("搜尋站點（代碼或英文名）");
        etSearch.setSingleLine(true);
        etSearch.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        etSearch.setPadding(dp(12), dp(12), dp(12), dp(12));

        // RecyclerView
        RecyclerView rv = new RecyclerView(this);
        LinearLayout.LayoutParams rvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(420));
        rv.setLayoutParams(rvLp);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // 先建 dialog，等等在 onPick 裡 dismiss
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("選擇目的地站點")
                .setView(container)
                .setNegativeButton("取消", null)
                .create();

        // Adapter：顯示字串清單，點擊回填 + 關閉對話框
        SimpleStringAdapter adapter = new SimpleStringAdapter(new ArrayList<>(stationDisplayList),
                picked -> {
                    destinationDisplay = picked;                     // "BL12 Shandao Temple"
                    destinationCode = parseCodeFromDisplay(picked);  // "BL12"
                    editDest.setText(destinationDisplay);

                    // 關閉彈窗
                    dialog.dismiss();
                });
        rv.setAdapter(adapter);

        // 即時過濾
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        container.addView(etSearch);
        container.addView(rv);

        dialog.show();
    }

    // 由 "BL12 Shandao Temple" 取出 "BL12"
    private String parseCodeFromDisplay(String display) {
        if (display == null) return "";
        int idx = display.indexOf(' ');
        if (idx > 0) return display.substring(0, idx).trim();
        return display.trim();
    }

    private int dp(int value) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(value * d);
    }

    // ───────────────── 抓取站點清單（與 Station.java 同來源） ─────────────────
    private class FetchStationsTask extends AsyncTask<Void, Void, String> {
        private final Runnable onSuccessUi; // 抓到資料後要做的事（如開彈窗）

        public FetchStationsTask(Runnable onSuccessUi) {
            this.onSuccessUi = onSuccessUi;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                String urlStr = Constants.URL_GET_STATIONS;
                // 若要依線別載入，可加上 lineCode 參數
                // if (someLineCode != null) {
                //     urlStr += "?lineCode=" + URLEncoder.encode(someLineCode, "UTF-8");
                // }
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
                Log.e("ItinerarySetting", "Error fetching stations", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String json) {
            if (json != null) {
                try {
                    JSONArray jsonArray = new JSONArray(json);
                    stationList.clear();
                    stationDisplayList.clear();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String stationCode = obj.getString("StationCode");
                        String nameE       = obj.getString("NameE");
                        String line        = obj.getString("Line");
                        String lineCode    = obj.getString("LineCode");

                        StationModel m = new StationModel(stationCode, nameE, line, lineCode);
                        stationList.add(m);

                        // 顯示字串（與彈窗一致）
                        stationDisplayList.add(stationCode + " " + nameE);
                    }

                    if (onSuccessUi != null) onSuccessUi.run();

                } catch (Exception e) {
                    Toast.makeText(ItinerarySetting.this, "站點解析錯誤", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(ItinerarySetting.this, "站點載入失敗", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ───────────────── 對話框用的極簡字串 Adapter ─────────────────
    private static class SimpleStringAdapter extends RecyclerView.Adapter<SimpleStringAdapter.VH> {
        interface OnPick { void onPicked(String item); }

        private final ArrayList<String> all;     // 全量
        private final ArrayList<String> showing; // 目前顯示
        private final OnPick onPick;

        SimpleStringAdapter(ArrayList<String> seed, OnPick onPick) {
            this.all = new ArrayList<>(seed);
            this.showing = new ArrayList<>(seed);
            this.onPick = onPick;
        }

        void filter(String q) {
            String query = q == null ? "" : q.trim().toLowerCase();
            showing.clear();
            if (query.isEmpty()) {
                showing.addAll(all);
            } else {
                for (String s : all) {
                    if (s.toLowerCase().contains(query)) {
                        showing.add(s);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @Override public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setTextSize(16);
            tv.setPadding(dp(parent, 14), dp(parent, 12), dp(parent, 14), dp(parent, 12));
            tv.setClickable(true);
            tv.setBackgroundResource(android.R.drawable.list_selector_background);
            return new VH(tv);
        }

        @Override public void onBindViewHolder(VH h, int pos) {
            String txt = showing.get(pos);
            h.tv.setText(txt);
            h.tv.setOnClickListener(v -> onPick.onPicked(txt));
        }

        @Override public int getItemCount() { return showing.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tv;
            VH(TextView itemView) {
                super(itemView);
                this.tv = itemView;
            }
        }

        private static int dp(ViewGroup parent, int value) {
            float d = parent.getResources().getDisplayMetrics().density;
            return Math.round(value * d);
        }
    }
}
