package com.example.a114202_metro.Itinerary;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.a114202_metro.Constants;
import com.example.a114202_metro.R;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialDatePicker.Builder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ViewHolder> {

    private final List<ItineraryItem> list;
    private final Context context;
    private final String gmail;

    // 站點快取
    private final ArrayList<StationItem> stationList = new ArrayList<>();
    private final ArrayList<String> stationDisplay = new ArrayList<>();
    private boolean stationsLoaded = false;

    public ItineraryAdapter(Context context, List<ItineraryItem> list, String gmail) {
        this.context = context;
        this.list = list;
        this.gmail = gmail;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_itinerary, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItineraryItem item = list.get(position);
        holder.title.setText(item.title);
        holder.date.setText(item.startDate + " ~ " + item.endDate);
        holder.dest.setText(item.dest);

        // 點整列 → 進行程詳情
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ItineraryDetail.class);
            intent.putExtra(ItineraryDetail.EXTRA_ITS_NO, item.itsNo);
            intent.putExtra(ItineraryDetail.EXTRA_TITLE, item.title);
            intent.putExtra(ItineraryDetail.EXTRA_START, item.startDate);
            intent.putExtra(ItineraryDetail.EXTRA_END, item.endDate);
            intent.putExtra(ItineraryDetail.EXTRA_DEST, item.dest);
            context.startActivity(intent);
        });

        holder.btnEdit.setOnClickListener(v -> showEditDialog(item, position));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, dest;
        ImageButton btnEdit;
        ViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.itineraryTitle);
            date  = v.findViewById(R.id.itineraryDate);
            dest  = v.findViewById(R.id.itineraryDest);
            btnEdit = v.findViewById(R.id.btnEditItinerary);
        }
    }

    /* ===== 編輯對話框（目的地改為可搜尋挑選） ===== */
    private void showEditDialog(ItineraryItem item, int position) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * context.getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        // 標題
        EditText etTitle = new EditText(context);
        etTitle.setHint("行程名稱");
        etTitle.setText(item.title);
        root.addView(etTitle);

        // 日期（不可輸入，點擊開 Range Picker）
        EditText etDate = new EditText(context);
        etDate.setHint("起訖日期（yyyy-MM-dd ~ yyyy-MM-dd）");
        etDate.setFocusable(false);
        etDate.setInputType(InputType.TYPE_NULL);
        etDate.setText(item.startDate + " ~ " + item.endDate);
        root.addView(etDate);

        // 目的地（不可輸入，點擊開站點選單；顯示站名或代碼）
        EditText etDest = new EditText(context);
        etDest.setHint("選擇目的地站點");
        etDest.setFocusable(false);
        etDest.setInputType(InputType.TYPE_NULL);
        // 顯示人看得懂的，暫時用代碼；選完會改成站名
        etDest.setText(item.dest);
        root.addView(etDest);

        // 暫存送後端用的 StationCode（預設用舊值）
        final String[] selectedDestCode = { item.dest };

        // 暫存新日期
        final String[] newStart = { item.startDate };
        final String[] newEnd   = { item.endDate };

        etDate.setOnClickListener(v -> showRangePicker((s, e) -> {
            newStart[0] = s; newEnd[0] = e;
            etDate.setText(s + " ~ " + e);
        }, item.startDate, item.endDate));

        // 點擊目的地 → 顯示可搜尋站點清單
        etDest.setOnClickListener(v -> {
            if (!stationsLoaded) {
                loadStationsIfNeeded(new Runnable() {
                    @Override public void run() { showStationPickerDialog(etDest, selectedDestCode); }
                }, new OnFail() {
                    @Override public void accept(String msg) {
                        Toast.makeText(context, "站點載入失敗：" + msg, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                showStationPickerDialog(etDest, selectedDestCode);
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("編輯行程")
                .setView(root)
                .setNegativeButton("取消", null)
                .setPositiveButton("更新", (d, which) -> {
                    String newTitle = etTitle.getText().toString().trim();

                    if (newTitle.isEmpty()) {
                        Toast.makeText(context, "請輸入行程名稱", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newStart[0] == null || newEnd[0] == null) {
                        Toast.makeText(context, "請選擇起訖日期", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (selectedDestCode[0] == null || selectedDestCode[0].isEmpty()) {
                        Toast.makeText(context, "請選擇目的地站點", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateItineraryOnServer(item.itsNo, newTitle, newStart[0], newEnd[0], selectedDestCode[0],
                            new Runnable() {
                                @Override public void run() {
                                    item.title = newTitle;
                                    item.startDate = newStart[0];
                                    item.endDate = newEnd[0];
                                    item.dest = selectedDestCode[0]; // 你若想顯示站名，可改成 lastSelectedName[0]
                                    notifyItemChanged(position);
                                    Toast.makeText(context, "更新成功", Toast.LENGTH_SHORT).show();
                                }
                            },
                            new OnFail() {
                                @Override public void accept(String msg) {
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                })
                .create();

        dialog.show();
    }

    /* ===== 站點搜尋對話框 ===== */
    private void showStationPickerDialog(EditText etDest, String[] selectedDestCodeRef) {
        // 對話框內容：上面搜尋、下面清單
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * context.getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText etSearch = new EditText(context);
        etSearch.setHint("搜尋站名 / 線名 / 代碼");
        container.addView(etSearch);

        ListView listView = new ListView(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_list_item_1, new ArrayList<>(stationDisplay));
        listView.setAdapter(adapter);
        container.addView(listView);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("選擇目的地站點")
                .setView(container)
                .setNegativeButton("取消", null)
                .create();

        // 即時篩選
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String display = (String) parent.getItemAtPosition(position);
            // 反查 StationCode
            StationItem chosen = findStationByDisplay(display);
            if (chosen != null) {
                selectedDestCodeRef[0] = chosen.code; // 送後端
                etDest.setText(chosen.name);          // 顯示站名
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private StationItem findStationByDisplay(String display) {
        for (int i = 0; i < stationDisplay.size(); i++) {
            if (stationDisplay.get(i).equals(display)) return stationList.get(i);
        }
        // 若經過篩選 adapter 的索引改變，以上方法可能對不上；可改更穩的方法：
        // 解析顯示字串最後的 "(CODE)" 取 CODE 再去 stationList 比對
        try {
            int l = display.lastIndexOf('(');
            int r = display.lastIndexOf(')');
            if (l >= 0 && r > l) {
                String code = display.substring(l + 1, r);
                for (StationItem s : stationList) if (code.equals(s.code)) return s;
            }
        } catch (Exception ignore) {}
        return null;
    }

    /* ===== 一次載入站點（快取） ===== */
    private void loadStationsIfNeeded(final Runnable onSuccess, final OnFail onFail) {
        if (stationsLoaded) { onSuccess.run(); return; }
        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest req = new StringRequest(Request.Method.GET, Constants.URL_GET_STATIONS,
                new Response.Listener<String>() {
                    @Override public void onResponse(String response) {
                        try {
                            JSONArray arr = new JSONArray(response);
                            stationList.clear();
                            stationDisplay.clear();
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.getJSONObject(i);
                                String code = o.optString("StationCode", "");
                                String name = o.optString("NameE", o.optString("Station", code));
                                String line = o.optString("Line", o.optString("LineCode", ""));
                                StationItem s = new StationItem(code, name, line);
                                stationList.add(s);
                                stationDisplay.add(formatDisplay(s));
                            }
                            stationsLoaded = true;
                            onSuccess.run();
                        } catch (Exception e) {
                            onFail.accept("解析錯誤");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override public void onErrorResponse(VolleyError error) {
                        onFail.accept("網路錯誤");
                    }
                });

        req.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        queue.add(req);
    }

    private String formatDisplay(StationItem s) {
        if (s.line != null && !s.line.isEmpty()) return s.name + " [" + s.line + "] (" + s.code + ")";
        return s.name + " (" + s.code + ")";
    }

    /* ===== Material Date Range Picker ===== */
    private interface OnDateRangeSelected { void onSelected(String start, String end); }
    private interface OnFail { void accept(String msg); }

    private void showRangePicker(OnDateRangeSelected cb, String defStart, String defEnd) {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("選擇行程日期");

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.TAIWAN);
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Taipei"));
            Date ds = sdf.parse(defStart);
            Date de = sdf.parse(defEnd);
            if (ds != null && de != null) {
                builder.setSelection(new androidx.core.util.Pair<>(ds.getTime(), de.getTime()));
            }
        } catch (Exception ignore) {}

        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) return;
            Long startUtc = selection.first;
            Long endUtc   = (selection.second != null ? selection.second : selection.first);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.TAIWAN);
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Taipei"));
            cb.onSelected(sdf.format(new Date(startUtc)), sdf.format(new Date(endUtc)));
        });

        if (context instanceof AppCompatActivity) {
            picker.show(((AppCompatActivity) context).getSupportFragmentManager(), "edit_date_range");
        } else {
            Toast.makeText(context, "無法顯示日期選擇器", Toast.LENGTH_SHORT).show();
        }
    }

    /* ===== 呼叫後端更新 ===== */
    private void updateItineraryOnServer(int itsNo, String title, String start, String end, String dest,
                                         final Runnable onSuccess, final OnFail onFail) {
        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest req = new StringRequest(Request.Method.POST, Constants.URL_UPDATE_ITINERARY,
                new Response.Listener<String>() {
                    @Override public void onResponse(String response) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            boolean error = obj.optBoolean("error", true);
                            if (!error) onSuccess.run();
                            else onFail.accept(obj.optString("message", "更新失敗"));
                        } catch (Exception e) {
                            onFail.accept("回應解析失敗");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override public void onErrorResponse(VolleyError error) {
                        onFail.accept("網路或伺服器錯誤");
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("gmail", gmail);
                p.put("its_no", String.valueOf(itsNo));
                p.put("title", title);
                p.put("start_date", start);
                p.put("end_date", end);
                p.put("dest", dest);
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

    /* ===== 站點資料模型 ===== */
    private static class StationItem {
        final String code, name, line;
        StationItem(String code, String name, String line) {
            this.code = code; this.name = name; this.line = line;
        }
    }
}


