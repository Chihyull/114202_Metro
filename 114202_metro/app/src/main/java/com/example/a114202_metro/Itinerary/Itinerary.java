package com.example.a114202_metro.Itinerary;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.a114202_metro.Constants;
import com.example.a114202_metro.MainActivity;
import com.example.a114202_metro.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Itinerary extends AppCompatActivity {

    Button btn_create, btn_ai;
    RecyclerView rv;
    ItineraryAdapter adapter;
    List<ItineraryItem> list = new ArrayList<>();

    private String currentUserGmail = "11336010@ntub.edu.tw"; // TODO: 改成登入 Gmail

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itinerary);

        btn_create = findViewById(R.id.btn_create);
        btn_ai = findViewById(R.id.btn_ai);
        rv = findViewById(R.id.rvItinerary);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItineraryAdapter(this, list, currentUserGmail);
        rv.setAdapter(adapter);

        btn_ai.setOnClickListener(v -> {
            Intent intent = new Intent(Itinerary.this, ItinerarySetting.class);
            intent.putExtra("EXTRA_AI_MODE", true); // ← 新增
            startActivity(intent);
        });

        btn_create.setOnClickListener(v -> {
            Intent intent = new Intent(Itinerary.this, ItinerarySetting.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchItineraries();
    }

    private void fetchItineraries() {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest req = new StringRequest(Request.Method.POST, Constants.URL_GET_ITINERARY,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (!obj.getBoolean("error")) {
                            JSONArray arr = obj.getJSONArray("items");
                            list.clear();
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.getJSONObject(i);
                                // ★ 取 ITSNo
                                int itsNo = o.optInt("ITSNo", 0);
                                list.add(new ItineraryItem(
                                        itsNo,
                                        o.optString("Title"),
                                        o.optString("StartDate"),
                                        o.optString("EndDate"),
                                        o.optString("Dest")
                                ));
                            }
                            adapter.notifyDataSetChanged();

                            if (list.isEmpty()) {
                                showEmptyState();
                            } else {
                                showListState();
                            }
                        } else {
                            showEmptyState();
                        }
                    } catch (Exception e) {
                        showEmptyState();
                    }
                },
                error -> showEmptyState()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("gmail", currentUserGmail);
                return p;
            }
        };

        queue.add(req);
    }

    private void showEmptyState() {
        rv.setVisibility(View.GONE);
        findViewById(R.id.bag).setVisibility(View.VISIBLE);
        findViewById(R.id.content2).setVisibility(View.VISIBLE);
        findViewById(R.id.content3).setVisibility(View.VISIBLE);
    }

    private void showListState() {
        rv.setVisibility(View.VISIBLE);
        findViewById(R.id.bag).setVisibility(View.GONE);
        findViewById(R.id.content2).setVisibility(View.GONE);
        findViewById(R.id.content3).setVisibility(View.GONE);
    }

}
