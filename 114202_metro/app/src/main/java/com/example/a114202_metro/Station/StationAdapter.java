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

        String fullStationCode = station.getStationCode(); // 例如 BR01
        String lineCode = fullStationCode.substring(0, 2);  // BR
        String stationCode = fullStationCode.substring(2);  // 01

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
