package com.example.a114202_metro.Station;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a114202_metro.R;

import java.util.List;

public class LineAdapter extends RecyclerView.Adapter<LineAdapter.LineViewHolder> {

    private List<LineModel> lineList;
    private OnLineClickListener listener;

    public interface OnLineClickListener {
        void onLineClick(String lineCode);
    }

    public LineAdapter(List<LineModel> lineList, OnLineClickListener listener) {
        this.lineList = lineList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_filter_station, parent, false);
        return new LineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LineViewHolder holder, int position) {
        String code = lineList.get(position).getLineCode();
        holder.tvLineCode.setText(code);
        holder.itemView.setOnClickListener(v -> listener.onLineClick(code));
    }

    @Override
    public int getItemCount() {
        return lineList.size();
    }

    static class LineViewHolder extends RecyclerView.ViewHolder {
        TextView tvLineCode;

        public LineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLineCode = itemView.findViewById(R.id.tv_line_code);
        }
    }
}
