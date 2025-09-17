package com.example.a114202_metro.Station;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.example.a114202_metro.Constants;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class FetchShortestPathTask extends AsyncTask<Void, Void, Integer> {
    private static final String TAG = "FetchShortestPathTask";

    private final WeakReference<PathStation> activityRef;
    private final String start;
    private final String end;

    private String errorMessage = null;
    private String lastRawBody = null; // for debugging non-JSON responses

    public FetchShortestPathTask(PathStation activity, String start, String end) {
        this.activityRef = new WeakReference<>(activity);
        this.start = start;
        this.end = end;
    }

    @Override
    protected Integer doInBackground(Void... voids) {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(Constants.URL_GET_SHORTEST_PATH);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            // POST form body (keys must match PHP: $_REQUEST['start'], $_REQUEST['end'])
            String post = "start=" + URLEncoder.encode(start, "UTF-8")
                    + "&end="   + URLEncoder.encode(end,   "UTF-8");
            Log.d(TAG, "POST " + Constants.URL_GET_SHORTEST_PATH + " body=" + post);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(post.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                // Read error body (if any) for easier debugging
                try (BufferedReader er = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder esb = new StringBuilder();
                    String eline;
                    while (er != null && (eline = er.readLine()) != null) esb.append(eline);
                    errorMessage = "HTTP " + code + (esb.length() > 0 ? (": " + esb) : "");
                } catch (Exception ignore) {}
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            lastRawBody = sb.toString();

            // Guard: ensure server returned JSON, not HTML (e.g., <br/> warnings)
            String trimmed = lastRawBody.trim();
            if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
                errorMessage = "Non-JSON response: " + trimmed.substring(0, Math.min(trimmed.length(), 200));
                return null;
            }

            JSONObject json = new JSONObject(trimmed);
            if (!json.optBoolean("ok", false)) {
                errorMessage = json.optString("error", "Server error");
                return null;
            }

            if (!json.optBoolean("found", false)) {
                return -1; // No path found
            }

            // Leniently parse total_time
            if (json.has("total_time") && !json.isNull("total_time")) {
                Object val = json.get("total_time");
                if (val instanceof Number) {
                    return ((Number) val).intValue();
                } else {
                    String s = String.valueOf(val).trim();
                    if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
                    try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
                }
            }
            return -1;

        } catch (Exception e) {
            errorMessage = e.getMessage();
            return null;
        } finally {
            try { if (reader != null) reader.close(); } catch (Exception ignore) {}
            if (conn != null) conn.disconnect();
        }
    }

    @Override
    protected void onPostExecute(Integer totalTime) {
        PathStation activity = activityRef.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        if (totalTime == null) {
            activity.setTravelTimeText("Travel Time: — minutes");
            Toast.makeText(activity, "Failed: " + (errorMessage != null ? errorMessage : "Unknown error"),
                    Toast.LENGTH_SHORT).show();
            if (lastRawBody != null) {
                Log.e(TAG, "Raw response: " + lastRawBody);
            }
            return;
        }

        if (totalTime == -1) {
            activity.setTravelTimeText("Travel Time: N/A");
            Toast.makeText(activity, "No path found", Toast.LENGTH_SHORT).show();
            return;
        }

        activity.setTravelTimeText("Travel Time: " + totalTime + " minutes");
    }
}
