package com.example.a114202_metro.Station;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.a114202_metro.R;
import java.util.List;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.StationViewHolder> {

    private List<StationModel> stationList;

    public StationAdapter(List<StationModel> stationList) {
        this.stationList = stationList;
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

        // 拆分 stationCode，假設格式為 BR01
        String fullStationCode = station.getStationCode(); // 例如：BR01
        String lineCode = fullStationCode.substring(0, 2);  // 取前兩個字元，為 BR
        String stationCode = fullStationCode.substring(2);  // 取剩下的字元，為 01

        // 顯示線代碼 (BR) 和站點代碼 (01)
        holder.lineCode.setText(lineCode); // 顯示線代碼
        holder.stationCode.setText(stationCode); // 顯示站點代碼
        holder.stationName.setText(station.getNameE()); // 顯示站點名稱
    }

    @Override
    public int getItemCount() {
        return stationList.size();
    }

    public static class StationViewHolder extends RecyclerView.ViewHolder {
        TextView stationCode, stationName, lineCode;  // 加入 lineCode 的 TextView

        public StationViewHolder(@NonNull View itemView) {
            super(itemView);
            stationCode = itemView.findViewById(R.id.stationCode);
            stationName = itemView.findViewById(R.id.stationName);
            lineCode = itemView.findViewById(R.id.lineCode); // 綁定 lineCode 的 TextView
        }
    }
}
