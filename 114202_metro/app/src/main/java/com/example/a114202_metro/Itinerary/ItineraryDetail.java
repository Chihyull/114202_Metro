package com.example.a114202_metro.Itinerary;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a114202_metro.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ItineraryDetail extends AppCompatActivity {

    public static final String EXTRA_ITS_NO   = "extra_its_no";
    public static final String EXTRA_TITLE    = "extra_title";
    public static final String EXTRA_START    = "extra_start";
    public static final String EXTRA_END      = "extra_end";
    public static final String EXTRA_DEST     = "extra_dest";

    private int itsNo;
    private String title, startDate, endDate, dest;

    private ChipGroup chipGroupDays;
    private TextView tvDate,tvWeekday;
    private RecyclerView rvDayPlans;
    private ShapeableImageView btnAddDay, btnRemoveDay;

    private final SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    private final SimpleDateFormat weekFmt = new SimpleDateFormat("EEEE", Locale.ENGLISH);
    private final List<String> dateList = new ArrayList<>(); // 每個 Day 對應的日期字串 yyyy-MM-dd
    private DayPlanAdapter dayAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itinerary_detail);

        // 時區統一
        TimeZone tz = TimeZone.getTimeZone("Asia/Taipei");
        ymd.setTimeZone(tz);
        weekFmt.setTimeZone(tz);

        // 取 Intent 參數
        Intent it = getIntent();
        itsNo     = it.getIntExtra(EXTRA_ITS_NO, 0);
        title     = it.getStringExtra(EXTRA_TITLE);
        startDate = it.getStringExtra(EXTRA_START);
        endDate   = it.getStringExtra(EXTRA_END);
        dest      = it.getStringExtra(EXTRA_DEST);

        // 綁定 View
        chipGroupDays = findViewById(R.id.chipGroupDays);
        tvDate        = findViewById(R.id.tvDate);
        tvWeekday     = findViewById(R.id.tvWeekday);
        rvDayPlans    = findViewById(R.id.rvDayPlans);
        btnAddDay     = findViewById(R.id.btnAddDay);
        btnRemoveDay  = findViewById(R.id.btnRemoveDay);

        // RecyclerView 先放空佈局 Adapter（之後再串實際資料）
        rvDayPlans.setLayoutManager(new LinearLayoutManager(this));
        dayAdapter = new DayPlanAdapter();
        rvDayPlans.setAdapter(dayAdapter);

        // 依日期區間建立 Day 1..N
        buildDaysFromRange(startDate, endDate);

        // Day + / -（目前僅操作 UI；真正新增/刪除天數的後端後續再接）
        btnAddDay.setOnClickListener(v -> addOneDay());
        btnRemoveDay.setOnClickListener(v -> removeLastDay());
    }

    private void buildDaysFromRange(String start, String end) {
        dateList.clear();
        chipGroupDays.removeAllViews();

        List<String> days = enumerateDatesInclusive(start, end);
        dateList.addAll(days);

        for (int i = 0; i < dateList.size(); i++) {
            Chip chip = createDayChip("Day " + (i + 1), i);
            chipGroupDays.addView(chip);
            if (i == 0) chip.setChecked(true);
        }

        // 預設選 Day 1
        if (!dateList.isEmpty()) updateHeaderForDay(0);
    }

    private Chip createDayChip(String text, int index) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setOnClickListener(v -> updateHeaderForDay(index));
        return chip;
    }

    private void updateHeaderForDay(int index) {
        if (index < 0 || index >= dateList.size()) return;

        try {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Taipei"), Locale.TAIWAN);
            c.setTime(ymd.parse(dateList.get(index)));

            tvDate.setText(dateList.get(index));

            String week = weekFmt.format(c.getTime()).toUpperCase(Locale.ENGLISH); // 星期幾
            tvWeekday.setText(week);
        } catch (ParseException ignore) {
            tvWeekday.setText(dateList.get(index));
        }

        // 目前資料尚未串接 → 顯示「空資料」樣式
        dayAdapter.showPlaceholderForDay(index);
    }

    private void addOneDay() {
        if (dateList.isEmpty()) return;
        String last = dateList.get(dateList.size() - 1);

        try {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Taipei"), Locale.TAIWAN);
            c.setTime(ymd.parse(last));
            c.add(Calendar.DATE, 1);
            String newDate = ymd.format(c.getTime());
            dateList.add(newDate);

            int idx = dateList.size() - 1;
            Chip chip = createDayChip("Day " + (idx + 1), idx);
            chipGroupDays.addView(chip);
            chip.setChecked(true);
            updateHeaderForDay(idx);
        } catch (ParseException ignore) { }
    }

    private void removeLastDay() {
        int count = dateList.size();
        if (count <= 1) return; // 至少保留 Day 1

        dateList.remove(count - 1);
        chipGroupDays.removeViewAt(count - 1);

        // 回到最後一個 day
        int idx = dateList.size() - 1;
        if (idx >= 0) {
            Chip lastChip = (Chip) chipGroupDays.getChildAt(idx);
            if (lastChip != null) lastChip.setChecked(true);
            updateHeaderForDay(idx);
        }
    }

    /** 將起訖日期（含端點）展開成 yyyy-MM-dd 列表 */
    private List<String> enumerateDatesInclusive(String start, String end) {
        List<String> out = new ArrayList<>();
        try {
            Calendar s = Calendar.getInstance(TimeZone.getTimeZone("Asia/Taipei"), Locale.TAIWAN);
            Calendar e = Calendar.getInstance(TimeZone.getTimeZone("Asia/Taipei"), Locale.TAIWAN);
            s.setTime(ymd.parse(start));
            e.setTime(ymd.parse(end));

            while (!s.after(e)) {
                out.add(ymd.format(s.getTime()));
                s.add(Calendar.DATE, 1);
            }
        } catch (ParseException ignore) { }
        if (out.isEmpty() && start != null) out.add(start); // fallback
        return out;
    }

    /** 先放一個最簡單的 Placeholder Adapter（之後再換成真的 DayPlanAdapter） */
    private static class DayPlanAdapter extends RecyclerView.Adapter<DayPlanViewHolder> {
        private int placeholderCount = 1; // 顯示 1 個空白卡片

        @Override
        public DayPlanViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // 使用 Android 內建簡單版面先頂著（之後換成自訂 item_day_plan.xml）
            View v = getSimpleEmptyCard(parent);
            return new DayPlanViewHolder(v);
        }

        @Override
        public void onBindViewHolder(DayPlanViewHolder holder, int position) {
            // 空白版面，不綁資料
        }

        @Override
        public int getItemCount() { return placeholderCount; }

        public void showPlaceholderForDay(int dayIndex) {
            placeholderCount = 1;
            notifyDataSetChanged();
        }

        private View getSimpleEmptyCard(ViewGroup parent) {
            // 用一個簡單的 TextView 當佔位；你之後可換成自訂 layout
            TextView tv = new TextView(parent.getContext());
            int pad = (int) (16 * parent.getContext().getResources().getDisplayMetrics().density);
            tv.setPadding(pad, pad, pad, pad);
            tv.setText("（這天尚未加入地點，之後會顯示行程項目）");
            return tv;
        }
    }

    private static class DayPlanViewHolder extends RecyclerView.ViewHolder {
        public DayPlanViewHolder(View itemView) { super(itemView); }
    }
}
