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
import java.util.ArrayList;
import java.util.List;

public class Station extends AppCompatActivity {

    private TextView stationTitle;
    private RecyclerView rvStation;
    private StationAdapter stationAdapter;
    private List<StationModel> stationList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station);

        stationTitle = findViewById(R.id.stationTitle);

        rvStation = findViewById(R.id.rv_station);
        rvStation.setLayoutManager(new LinearLayoutManager(this));

        stationList = new ArrayList<>();
        stationAdapter = new StationAdapter(stationList);
        rvStation.setAdapter(stationAdapter);

        new GetStationsTask().execute();
    }

    private class GetStationsTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(Constants.URL_GET_STATIONS);
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

                    String lineName = "";

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String stationCode = obj.getString("StationCode");
                        String nameE = obj.getString("NameE");
                        String line = obj.getString("Line");

                        if (i == 0) {
                            lineName = line;
                        }

                        stationList.add(new StationModel(stationCode, nameE, line));
                    }

                    stationTitle.setText(lineName); // 設定標題
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
