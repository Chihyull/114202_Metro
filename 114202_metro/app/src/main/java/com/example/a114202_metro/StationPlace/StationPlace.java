package com.example.a114202_metro.StationPlace;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup;
import android.widget.RadioButton;
import androidx.appcompat.widget.AppCompatButton;

import java.io.BufferedOutputStream;
import java.io.OutputStream;

public class StationPlace extends AppCompatActivity {

    private TextView tvTitle;
    private RecyclerView rvExitCodes;   // 出口清單（橫向）
    private RecyclerView rvPlaces;      // 該出口的地點清單（直向）

    private ExitAdapter exitAdapter;
    private PlaceAdapter placeAdapter;

    private String stationCode, stationName;

    // key = ExitCode, value = List<PlaceModel>
    private final Map<String, List<PlaceModel>> exitPlaces = new LinkedHashMap<>();

    // ------- 「選擇收藏夾」Dialog -------
    private Dialog chooseFolderDialog;
    private FolderChoiceAdapter folderChoiceAdapter;
    private RecyclerView rvFolderChoicesInDialog; // for refresh after load
    private View rowNewFolderInDialog;            // for show/hide when only one

    // 目前要收藏的地點（從 btnFavorite 點到的那一筆）
    private PlaceModel pendingFavoritePlace = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_place);

        // 1) 取參數
        stationCode = getIntent().getStringExtra("station_code");
        stationName = getIntent().getStringExtra("station_name");

        // 2) 綁定 View
        tvTitle     = findViewById(R.id.stationTitle);
        rvExitCodes = findViewById(R.id.rv_exit_codes);
        rvPlaces    = findViewById(R.id.rv_station);

        // 3) 標題
        if (!TextUtils.isEmpty(stationName)) tvTitle.setText(stationName);

        // 4) 出口 RecyclerView：橫向
        rvExitCodes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        exitAdapter = new ExitAdapter();
        rvExitCodes.setAdapter(exitAdapter);

        // 出口點擊切換地點
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

        // 在 item attach 時為每個 btnFavorite 綁點擊（不改你的 PlaceAdapter）
        rvPlaces.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override public void onChildViewAttachedToWindow(View view) {
                View fav = view.findViewById(R.id.btnFavorite);
                if (fav != null && fav.getTag(R.id.btnFavorite) == null) {
                    fav.setOnClickListener(v -> {
                        int pos = rvPlaces.getChildAdapterPosition(view);
                        PlaceModel pm = placeAdapter.getItem(pos);
                        pendingFavoritePlace = pm; // 記住這一筆，Save 時取用
                        showChooseFolderDialog(/*preselectUlNo*/ -1);
                    });
                    fav.setTag(R.id.btnFavorite, true); // 防重複註冊
                }
            }
            @Override public void onChildViewDetachedFromWindow(View view) { /* no-op */ }
        });

        // 6) 取出口 + Places
        if (!TextUtils.isEmpty(stationCode)) {
            new FetchExitsTask(stationCode).execute();
        } else {
            Toast.makeText(this, "缺少站點代碼，無法載入出口", Toast.LENGTH_SHORT).show();
        }
    }

    /* 取目前登入者 Gmail（沿用 SharedPreferences 寫法；請換成你的登入流程） */
    private String getCurrentUserGmail() {
        return getSharedPreferences("auth", MODE_PRIVATE)
                .getString("gmail", "11336010@ntub.edu.tw");
    }

    /* 嘗試把 PlaceModel.placeId 視為 SPNo（數字） */
    private int getSpNoFromPlace(PlaceModel pm) {
        if (pm == null || TextUtils.isEmpty(pm.placeId)) return 0;
        try { return Integer.parseInt(pm.placeId.trim()); }
        catch (NumberFormatException ignore) { return 0; }
    }


    /* ---------------------- 「選擇收藏夾」Dialog（打 API 載資料） ---------------------- */
    private void showChooseFolderDialog(int preselectUlNo) {
        if (chooseFolderDialog == null) {
            chooseFolderDialog = new Dialog(this);
            chooseFolderDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            chooseFolderDialog.setContentView(R.layout.item_dialog_likes);

            // 透明背景 + 滿版寬，避免跑版
            if (chooseFolderDialog.getWindow() != null) {
                chooseFolderDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                chooseFolderDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            // 綁定 Dialog 內元件
            rvFolderChoicesInDialog = chooseFolderDialog.findViewById(R.id.rv_folder_choices);
            AppCompatButton btnCancel= chooseFolderDialog.findViewById(R.id.btn_cancel_pick);
            AppCompatButton btnSave  = chooseFolderDialog.findViewById(R.id.btn_confirm_pick);
            rowNewFolderInDialog     = chooseFolderDialog.findViewById(R.id.row_new_folder);
            View btnAddFolder        = chooseFolderDialog.findViewById(R.id.btn_add_folder);

            rvFolderChoicesInDialog.setLayoutManager(new LinearLayoutManager(this));
            rvFolderChoicesInDialog.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
            folderChoiceAdapter = new FolderChoiceAdapter();
            rvFolderChoicesInDialog.setAdapter(folderChoiceAdapter);

            // 新增收藏夾（先 Toast；之後你可打 createUserLike.php）
            View.OnClickListener createNew = v -> {
                Toast.makeText(this, "Open create-folder dialog …", Toast.LENGTH_SHORT).show();
                // TODO: 串你的新建收藏夾流程，成功後再呼叫 fetch，並 preselect 新增那筆
            };
            btnAddFolder.setOnClickListener(createNew);
            rowNewFolderInDialog.setOnClickListener(createNew);

            btnCancel.setOnClickListener(v -> chooseFolderDialog.dismiss());
            btnSave.setOnClickListener(v -> {
                FolderChoice picked = folderChoiceAdapter.getSelected();
                if (picked == null) {
                    Toast.makeText(this, "Please pick a folder.", Toast.LENGTH_SHORT).show();
                    return;
                }
                int spNo = getSpNoFromPlace(pendingFavoritePlace);
                if (spNo <= 0) {
                    Toast.makeText(this, "Missing SPNo: 請把 PlaceModel.placeId 改為 SPNo 或後端一起傳 SPNo", Toast.LENGTH_LONG).show();
                    return;
                }
                // ✅ 寫入收藏（IsFavorite=1）
                new SetFavoriteTask(getCurrentUserGmail(), picked.ulNo, spNo).execute();
                chooseFolderDialog.dismiss();
            });

            // onShow 再保險一次設置寬度
            chooseFolderDialog.setOnShowListener(d -> {
                if (chooseFolderDialog.getWindow() != null) {
                    chooseFolderDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
            });
        }

        // 顯示 Dialog（先 show 再去載資料，避免閃爍）
        chooseFolderDialog.show();

        // 呼叫 API 載入資料夾清單
        new FetchFoldersTask(getCurrentUserGmail(), 100, 0, preselectUlNo).execute();
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

                    String exitCode = o.optString("ExitCode", "");
                    if (TextUtils.isEmpty(exitCode)) continue;

                    if (TextUtils.isEmpty(stationName)) {
                        String backendStationName = o.optString("StationName", "");
                        if (!TextUtils.isEmpty(backendStationName)) {
                            stationName = backendStationName;
                        }
                    }

                    List<PlaceModel> places = new ArrayList<>();
                    // FetchExitsTask#doInBackground 內解析 Places 的迴圈，請改成這樣
                    JSONArray placesArr = o.optJSONArray("Places");
                    if (placesArr != null) {
                        for (int j = 0; j < placesArr.length(); j++) {
                            JSONObject p = placesArr.optJSONObject(j);
                            if (p == null) continue;

                            // ★ 關鍵：一定要從後端拿到 SPNo
                            int spNo = p.optInt("SPNo", 0);   // 如果後端 key 不是這個大小寫，請改成正確的 key

                            String placeName = p.optString("PlaceName", "");
                            String photoUrl  = p.optString("photo", null);

                            // 不改 PlaceModel 結構的情況下，把 SPNo 當字串放進 placeId
                            places.add(new PlaceModel(String.valueOf(spNo), placeName, photoUrl));
                        }
                    }

                    exitPlaces.put(exitCode, places);
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

    /* ---------------------- 取「我的最愛資料夾」清單（POST: gmail, limit, offset） ---------------------- */
    private class FetchFoldersTask extends AsyncTask<Void, Void, List<FolderChoice>> {
        private final String gmail;
        private final int limit, offset;
        private final int preselectUlNo;

        private boolean error = false;
        private String  message = "";

        FetchFoldersTask(String gmail, int limit, int offset, int preselectUlNo) {
            this.gmail = gmail;
            this.limit = limit;
            this.offset = offset;
            this.preselectUlNo = preselectUlNo;
        }

        @Override
        protected List<FolderChoice> doInBackground(Void... voids) {
            List<FolderChoice> list = new ArrayList<>();
            HttpURLConnection conn = null;
            try {
                JSONObject o = Api.postForm(Constants.URL_GET_USERLIKE,
                        "gmail", gmail,
                        "limit", String.valueOf(limit),
                        "offset", String.valueOf(offset));
                error = o.optBoolean("error", true);
                message = o.optString("message", "");
                if (!error) {
                    int count = o.optInt("count", 0);
                    if (count > 0) {
                        JSONArray arr = o.optJSONArray("items");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject it = arr.optJSONObject(i);
                                if (it == null) continue;
                                int ulNo = it.optInt("ULNo", 0);
                                String name = it.optString("FileName", "");
                                if (ulNo > 0 && !TextUtils.isEmpty(name)) {
                                    list.add(new FolderChoice(ulNo, name));
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                error = true;
                message = "Network error";
                Log.e("StationPlace", "fetch folders error", e);
            }
            return list;
        }

        @Override
        protected void onPostExecute(List<FolderChoice> folders) {
            if (folderChoiceAdapter == null || chooseFolderDialog == null) return;

            if (error) {
                Toast.makeText(StationPlace.this, TextUtils.isEmpty(message) ? "Fetch failed." : message, Toast.LENGTH_SHORT).show();
                folderChoiceAdapter.submit(new ArrayList<>());
                if (rowNewFolderInDialog != null) rowNewFolderInDialog.setVisibility(View.VISIBLE);
                return;
            }

            folderChoiceAdapter.submit(folders);
            if (rowNewFolderInDialog != null) {
                rowNewFolderInDialog.setVisibility(folders.size() <= 1 ? View.VISIBLE : View.GONE);
            }
            if (preselectUlNo > 0) {
                folderChoiceAdapter.preselect(preselectUlNo);
            }
        }
    }

    /* ---------------------- Favorite API 任務（set / unset） ---------------------- */

    private class SetFavoriteTask extends AsyncTask<Void, Void, JSONObject> {
        private final String gmail;
        private final int ulNo, spNo;

        SetFavoriteTask(String gmail, int ulNo, int spNo) {
            this.gmail = gmail; this.ulNo = ulNo; this.spNo = spNo;
        }

        @Override protected JSONObject doInBackground(Void... voids) {
            try {
                return Api.postForm(Constants.URL_UPDATE_HEART,
                        "gmail", gmail,
                        "ulNo", String.valueOf(ulNo),
                        "spNo", String.valueOf(spNo));
            } catch (Exception e) {
                Log.e("StationPlace", "setFavorite error", e);
                return null;
            }
        }

        @Override protected void onPostExecute(JSONObject o) {
            if (o == null) { Toast.makeText(StationPlace.this, "Network error", Toast.LENGTH_SHORT).show(); return; }
            boolean error = o.optBoolean("error", true);
            String msg     = o.optString("message", error ? "Set favorite failed" : "Favorite set");
            Toast.makeText(StationPlace.this, msg, Toast.LENGTH_SHORT).show();
            // 如需同步心型圖示狀態，可在這裡更新 UI（需要知道哪一列的狀態；目前略）
        }
    }

    // 先備好，之後要做「取消收藏」可直接呼叫
    private class UnsetFavoriteTask extends AsyncTask<Void, Void, JSONObject> {
        private final String gmail;
        private final int ulNo, spNo;

        UnsetFavoriteTask(String gmail, int ulNo, int spNo) {
            this.gmail = gmail; this.ulNo = ulNo; this.spNo = spNo;
        }

        @Override protected JSONObject doInBackground(Void... voids) {
            try {
                return Api.postForm(Constants.URL_UNSET_HEART,
                        "gmail", gmail,
                        "ulNo", String.valueOf(ulNo),
                        "spNo", String.valueOf(spNo));
            } catch (Exception e) {
                Log.e("StationPlace", "unsetFavorite error", e);
                return null;
            }
        }

        @Override protected void onPostExecute(JSONObject o) {
            if (o == null) { Toast.makeText(StationPlace.this, "Network error", Toast.LENGTH_SHORT).show(); return; }
            boolean error = o.optBoolean("error", true);
            String msg     = o.optString("message", error ? "Unset favorite failed" : "Favorite removed");
            Toast.makeText(StationPlace.this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    /* ---------------------- Folder Choice（小型 Adapter） ---------------------- */
    private static class FolderChoice {
        final int ulNo;
        final String name;
        FolderChoice(int ulNo, String name) { this.ulNo = ulNo; this.name = name; }
    }

    private static class FolderChoiceAdapter extends RecyclerView.Adapter<FolderChoiceAdapter.VH> {
        private final List<FolderChoice> data = new ArrayList<>();
        private int selectedPos = RecyclerView.NO_POSITION;

        void submit(List<FolderChoice> list) {
            data.clear();
            if (list != null) data.addAll(list);
            // 預設選第一個（或改為 NO_POSITION）
            selectedPos = data.isEmpty() ? RecyclerView.NO_POSITION : 0;
            notifyDataSetChanged();
        }

        FolderChoice getSelected() {
            if (selectedPos < 0 || selectedPos >= data.size()) return null;
            return data.get(selectedPos);
        }

        void preselect(int ulNo) {
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i).ulNo == ulNo) {
                    int old = selectedPos;
                    selectedPos = i;
                    if (old != RecyclerView.NO_POSITION) notifyItemChanged(old);
                    notifyItemChanged(i);
                    break;
                }
            }
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder_choices, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            FolderChoice fc = data.get(position);
            h.name.setText(fc.name);
            h.radio.setChecked(position == selectedPos);

            View.OnClickListener pick = v -> {
                int old = selectedPos;
                selectedPos = h.getAdapterPosition();
                if (old != RecyclerView.NO_POSITION) notifyItemChanged(old);
                notifyItemChanged(selectedPos);
            };
            h.itemView.setOnClickListener(pick);
            h.radio.setOnClickListener(pick);
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView name; RadioButton radio;
            VH(@NonNull View itemView) {
                super(itemView);
                name  = itemView.findViewById(R.id.tv_folder_name);
                radio = itemView.findViewById(R.id.radio_pick);
            }
        }
    }

    /* ---------------------- 小工具：HTTP POST 表單 ---------------------- */
    private static class Api {
        static JSONObject postForm(String urlStr, String... kvPairs) throws Exception {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            try {
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                StringBuilder body = new StringBuilder();
                for (int i = 0; i + 1 < kvPairs.length; i += 2) {
                    if (i > 0) body.append('&');
                    body.append(URLEncoder.encode(kvPairs[i], "UTF-8"))
                            .append('=')
                            .append(URLEncoder.encode(kvPairs[i + 1], "UTF-8"));
                }

                OutputStream os = new BufferedOutputStream(conn.getOutputStream());
                os.write(body.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line; while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                return new JSONObject(sb.toString());
            } finally {
                conn.disconnect();
            }
        }
    }
}





