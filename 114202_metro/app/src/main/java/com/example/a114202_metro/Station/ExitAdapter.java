package com.example.a114202_metro.Station;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.a114202_metro.R;
import java.util.ArrayList;
import java.util.List;

public class ExitAdapter extends RecyclerView.Adapter<ExitAdapter.VH> {

    private final List<ExitModel> data = new ArrayList<>();

    public void submit(List<ExitModel> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_filter_exits, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.exitCode.setText(data.get(position).getExitCode());
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView exitCode;
        VH(@NonNull View itemView) {
            super(itemView);
            exitCode = itemView.findViewById(R.id.tv_exits_code);
        }
    }
}
