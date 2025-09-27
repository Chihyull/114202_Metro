package com.example.a114202_metro.Favorite;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.a114202_metro.Constants;
import com.example.a114202_metro.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserLike extends AppCompatActivity {

    // UI
    private RecyclerView rv;
    private ImageView ivEmpty;
    private TextView tvEmpty1, tvEmpty2;
    private View btnCreate;
    private CardView dialogCard;
    private EditText editFolderName;
    private View btnCreateLike;

    // Adapter
    private UserLikeAdapter adapter;

    // Volley
    private RequestQueue queue;

    // 對話框模式
    private enum DialogMode { CREATE, RENAME }
    private DialogMode currentMode = DialogMode.CREATE;
    private int editingPosition = -1;   // rename 時用
    private int editingULNo = -1;       // rename 時用

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_like);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        queue = Volley.newRequestQueue(this);

        // 找 UI
        rv = findViewById(R.id.rv_folders);
        ivEmpty = findViewById(R.id.like);
        tvEmpty1 = findViewById(R.id.content2);
        tvEmpty2 = findViewById(R.id.content3);
        btnCreate = findViewById(R.id.btn_create);

        dialogCard = findViewById(R.id.dialog_card);
        editFolderName = findViewById(R.id.editText_folder_name);
        btnCreateLike = findViewById(R.id.btn_create_like);

        // RecyclerView
        adapter = new UserLikeAdapter(new UserLikeAdapter.Listener() {
            @Override
            public void onClickEdit(int position, UserLikeItem item, UserLikeAdapter.VH holder) {
                openRenameDialog(position, item);
            }

            @Override
            public void onClickDeleteBar(int position, UserLikeItem item, UserLikeAdapter.VH holder) {
                confirmDelete(position, item, holder);
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // Swipe reveal（你的 callback）
        ItemTouchHelper helper = new ItemTouchHelper(new SwipteDeleteBar(this, adapter, 72));
        helper.attachToRecyclerView(rv);

        // 建立資料夾按鈕
        btnCreate.setOnClickListener(v -> openCreateDialog());

        // 對話框送出按鈕（Create / Rename 共用）
        btnCreateLike.setOnClickListener(v -> {
            String name = editFolderName.getText().toString().trim();
            if (name.isEmpty()) {
                editFolderName.setError("Name is required");
                return;
            }
            if (currentMode == DialogMode.CREATE) {
                apiCreateFolder(name);
            } else {
                apiRenameFolder(editingULNo, name, editingPosition);
            }
        });

        // 進場先抓清單
        apiFetchFolders(/*limit=*/100, /*offset=*/0);
    }

    // 取得目前登入者 Gmail（請改成你真正的登入流程）
    private String getCurrentUserGmail() {
        return getSharedPreferences("auth", MODE_PRIVATE)
                .getString("gmail", "11336010@ntub.edu.tw");
    }

    // —————————————— Dialogs ——————————————
    private void openCreateDialog() {
        currentMode = DialogMode.CREATE;
        editingPosition = -1;
        editingULNo = -1;
        editFolderName.setText("");
        dialogCard.setVisibility(View.VISIBLE);
        editFolderName.requestFocus();
    }

    private void openRenameDialog(int position, UserLikeItem item) {
        currentMode = DialogMode.RENAME;
        editingPosition = position;
        editingULNo = item.ULNo;
        editFolderName.setText(item.fileName);
        if (btnCreateLike instanceof TextView) {
            ((TextView) btnCreateLike).setText("Rename");
        }
        dialogCard.setVisibility(View.VISIBLE);
        editFolderName.requestFocus();
    }

    private void closeDialog() {
        dialogCard.setVisibility(View.GONE);
        if (btnCreateLike instanceof TextView) {
            ((TextView) btnCreateLike).setText("Create");
        }
        currentMode = DialogMode.CREATE;
        editingPosition = -1;
        editingULNo = -1;
    }

    // —————————————— Empty state ——————————————
    private void refreshEmptyState() {
        boolean empty = adapter.isEmpty();
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
        ivEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        tvEmpty1.setVisibility(empty ? View.VISIBLE : View.GONE);
        tvEmpty2.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    // —————————————— API：抓清單（getUserLike.php） ——————————————
    private void apiFetchFolders(int limit, int offset) {
        StringRequest req = new StringRequest(Request.Method.POST, Constants.URL_GET_USERLIKE,
                resp -> {
                    try {
                        JSONObject o = new JSONObject(resp);
                        boolean error = o.optBoolean("error", true);
                        if (error) {
                            toast(o.optString("message", "Fetch failed."));
                            adapter.setData(new ArrayList<>());
                            refreshEmptyState();
                            return;
                        }

                        int count = o.optInt("count", 0);
                        List<UserLikeItem> list = new ArrayList<>();
                        if (count > 0) {
                            JSONArray arr = o.optJSONArray("items");
                            if (arr != null) {
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject it = arr.optJSONObject(i);
                                    if (it == null) continue;
                                    int ulNo = it.optInt("ULNo", 0);
                                    String name = it.optString("FileName", "");
                                    if (ulNo > 0 && !name.isEmpty()) {
                                        list.add(new UserLikeItem(ulNo, name));
                                    }
                                }
                            }
                        }
                        adapter.setData(list);
                        refreshEmptyState(); // ✅ 清單長度為 0 才顯示 empty

                    } catch (JSONException e) {
                        toast("Unexpected response.");
                        adapter.setData(new ArrayList<>());
                        refreshEmptyState();
                    }
                },
                err -> {
                    toast("Network error (fetch).");
                    adapter.setData(new ArrayList<>());
                    refreshEmptyState();
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> p = new HashMap<>();
                p.put("gmail", getCurrentUserGmail()); // 後端用 gmail 換 user
                p.put("limit", String.valueOf(limit));
                p.put("offset", String.valueOf(offset));
                return p;
            }
        };
        queue.add(req);
    }

    // —————————————— API：Create（createUserLike.php） ——————————————
    private void apiCreateFolder(String fileName) {
        StringRequest req = new StringRequest(Request.Method.POST, Constants.URL_CREATE_USERLIKE,
                resp -> {
                    try {
                        JSONObject o = new JSONObject(resp);
                        boolean error = o.optBoolean("error", true);
                        String status = o.optString("status", "");
                        int ulNo = o.optInt("ULNo", 0);

                        if (!error && ("created".equalsIgnoreCase(status) || "exists".equalsIgnoreCase(status)) && ulNo > 0) {
                            // 加到最上方（或改成 apiFetchFolders(100,0) 皆可）
                            adapter.addFirst(new UserLikeItem(ulNo, fileName));
                            refreshEmptyState();
                            closeDialog();
                        } else {
                            toast("Create failed. " + o.optString("message", ""));
                        }
                    } catch (Exception e) {
                        toast("Unexpected response.");
                    }
                },
                err -> toast("Network error (create).")
        ) {
            @Override protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> p = new HashMap<>();
                p.put("gmail", getCurrentUserGmail()); // 小寫
                p.put("fileName", fileName);           // 小寫
                return p;
            }
        };
        queue.add(req);
    }

    // —————————————— API：Rename（updateUserLike.php） ——————————————
    // 後端需求：gmail, ulNo, fileName（小寫）
    private void apiRenameFolder(int ulNo, String newName, int position) {
        StringRequest req = new StringRequest(Request.Method.POST, Constants.URL_UPDATE_USERLIKE,
                resp -> {
                    try {
                        JSONObject o = new JSONObject(resp);
                        boolean error = o.optBoolean("error", true);
                        if (!error) {
                            adapter.updateName(position, newName);
                            closeDialog();
                        } else {
                            toast(o.optString("message", "Rename failed."));
                        }
                    } catch (JSONException e) {
                        toast("Unexpected response.");
                    }
                },
                err -> toast("Network error (rename).")
        ) {
            @Override protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> p = new HashMap<>();
                p.put("gmail", getCurrentUserGmail());     // ✅ 小寫
                p.put("ulNo", String.valueOf(ulNo));       // ✅ 小寫
                p.put("fileName", newName);                // ✅ 小寫
                return p;
            }
        };
        queue.add(req);
    }

    // —————————————— API：Delete（deleteUserLike.php） ——————————————
    // 後端需求：gmail + (ulNo 或 fileName)，我們用 ulNo
    private void apiDeleteFolder(int ulNo, int position, UserLikeAdapter.VH holder) {
        StringRequest req = new StringRequest(Request.Method.POST, Constants.URL_DELETE_USERLIKE,
                resp -> {
                    try {
                        JSONObject o = new JSONObject(resp);
                        boolean error = o.optBoolean("error", true);
                        if (!error) {
                            adapter.removeAt(position);
                            refreshEmptyState();
                        } else {
                            holder.closeReveal();
                            toast(o.optString("message", "Delete failed."));
                        }
                    } catch (JSONException e) {
                        holder.closeReveal();
                        toast("Unexpected response.");
                    }
                },
                err -> {
                    holder.closeReveal();
                    toast("Network error (delete).");
                }
        ) {
            @Override protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> p = new HashMap<>();
                p.put("gmail", getCurrentUserGmail());      // ✅ 小寫
                p.put("ulNo", String.valueOf(ulNo));        // ✅ 小寫
                // 若想用檔名刪除也可：p.put("fileName", name);
                return p;
            }
        };
        queue.add(req);
    }

    // —————————————— UI 封裝 ——————————————
    private void confirmDelete(int position, UserLikeItem item, UserLikeAdapter.VH holder) {
        new AlertDialog.Builder(this)
                .setTitle("Delete this folder?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> apiDeleteFolder(item.ULNo, position, holder))
                .setNegativeButton("Cancel", (d, w) -> holder.closeReveal())
                .setCancelable(false)
                .show();
    }

    private void toast(String s) {
        android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_SHORT).show();
    }
}




