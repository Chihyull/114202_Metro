package com.example.a114202_metro.Station;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
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
            lineCode = matcher.group(1);     // 取得英文字母部分，例如 "BR" 或 "R"
            stationCode = matcher.group(2);  // 取得數字部分，例如 "01"
        } else {
            // 若格式不符，可視需求處理，或顯示原始內容
            lineCode = fullStationCode;
            stationCode = "";
        }

        holder.lineCode.setText(lineCode);
        holder.stationCode.setText(stationCode);
        holder.stationName.setText(station.getNameE());
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
                    if (item.getNameE().toLowerCase().contains(filterPattern)) {
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
            stationList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    public static class StationViewHolder extends RecyclerView.ViewHolder {
        TextView stationCode, stationName, lineCode;

        public StationViewHolder(@NonNull View itemView) {
            super(itemView);
            stationCode = itemView.findViewById(R.id.stationCode);
            stationName = itemView.findViewById(R.id.stationName);
            lineCode = itemView.findViewById(R.id.lineCode);
        }
    }
}
