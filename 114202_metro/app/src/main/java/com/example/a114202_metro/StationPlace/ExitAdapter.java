package com.example.a114202_metro.StationPlace;

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
    private int selectedPosition = RecyclerView.NO_POSITION;
    public interface OnExitClickListener { void onClick(String exitCode); }
    private OnExitClickListener listener;
    public void setOnExitClickListener(OnExitClickListener l) { this.listener = l; }

    private final List<ExitModel> data = new ArrayList<>();

    /** 維持你原本的方法簽名不變 */
    public void submit(List<ExitModel> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_filter_exits, parent, false); // 保留原本用 layout
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ExitModel m = data.get(position);
        String exitCode = m.getExitCode(); // 依照你原本的模型存取方式

        holder.exitCode.setText(exitCode);
        holder.exitCode.setSelected(position == selectedPosition);

        holder.exitCode.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // 更新舊的和新的 item 狀態
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);

            if (listener != null) listener.onClick(exitCode);
        });


        ViewGroup.LayoutParams lp = holder.exitCode.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            mlp.setMargins(dp(holder.exitCode, 12), dp(holder.exitCode, 8),
                    dp(holder.exitCode, 12), dp(holder.exitCode, 8));
            holder.exitCode.setLayoutParams(mlp);
        }

    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView exitCode;
        VH(@NonNull View itemView) {
            super(itemView);
            exitCode = itemView.findViewById(R.id.tv_exits_code); // 保留原本 id
        }
    }

    /** 小工具：dp 轉 px */
    private static int dp(View view, int dp) {
        float density = view.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}

