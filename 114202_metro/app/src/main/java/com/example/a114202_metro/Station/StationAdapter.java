package com.example.a114202_metro.Station;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a114202_metro.R;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.StationViewHolder> implements Filterable {

    private List<StationModel> stationList;
    private List<StationModel> stationListFull; // 原始完整資料（for filter）

    public StationAdapter(List<StationModel> stationList) {
        this.stationList = new ArrayList<>(stationList);
        this.stationListFull = new ArrayList<>(stationList);
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_station, parent, false);
        return new StationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {
        StationModel station = stationList.get(position);

        String fullStationCode = station.getStationCode(); // 例如 BR01 或 R01
        String lineCode = "";
        String stationCode = "";

        // 使用正規表達式分離英文字母與數字
        Pattern pattern = Pattern.compile("([A-Z]+)([0-9]+)");
        Matcher matcher = pattern.matcher(fullStationCode);
        if (matcher.matches()) {
            lineCode = matcher.group(1);     // 英文字母部分，例如 "BR" 或 "R"
            stationCode = matcher.group(2);  // 數字部分，例如 "01"
        } else {
            lineCode = fullStationCode;
            stationCode = "";
        }

        holder.lineCode.setText(lineCode);
        holder.stationCode.setText(stationCode);
        holder.stationName.setText(station.getNameE());

        // 根據 lineCode 設定六角形背景
        int bgResId = getBackgroundByLineCode(lineCode);
        holder.hexLayout.setBackgroundResource(bgResId);

        // ★ 點擊整列 → 直接開啟 StationPlaceActivity（不帶任何 extras）
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), StationPlace.class);
            intent.putExtra("station_code", station.getStationCode()); // 例如 BL12
            intent.putExtra("station_name", station.getNameE());       // 畫面標題用
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return stationList.size();
    }

    public void setFilteredList(List<StationModel> newList) {
        this.stationList = new ArrayList<>(newList);
        this.stationListFull = new ArrayList<>(newList); // 更新原始資料
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return stationFilter;
    }

    private final Filter stationFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<StationModel> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(stationListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (StationModel item : stationListFull) {
                    if (item.getNameE().toLowerCase().contains(filterPattern)
                            || item.getStationCode().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            stationList.clear();
            //noinspection unchecked
            stationList.addAll((List<StationModel>) results.values);
            notifyDataSetChanged();
        }
    };

    public static class StationViewHolder extends RecyclerView.ViewHolder {
        TextView stationCode, stationName, lineCode;
        LinearLayout hexLayout;  // 六角形外層容器

        public StationViewHolder(@NonNull View itemView) {
            super(itemView);
            stationCode = itemView.findViewById(R.id.stationCode);
            stationName = itemView.findViewById(R.id.stationName);
            lineCode = itemView.findViewById(R.id.lineCode);
            hexLayout = itemView.findViewById(R.id.hexLayout);
        }
    }

    // 依照 lineCode 返回不同背景 drawable id
    private int getBackgroundByLineCode(String lineCode) {
        switch (lineCode) {
            case "BR": return R.drawable.station_brown;
            case "BL": return R.drawable.station_blue;
            case "G":  return R.drawable.station_green;
            case "O":  return R.drawable.station_orange;
            case "R":  return R.drawable.station_red;
            case "Y":  return R.drawable.station_yellow;
            default:   return R.drawable.station_bg; // 預設背景
        }
    }
}
