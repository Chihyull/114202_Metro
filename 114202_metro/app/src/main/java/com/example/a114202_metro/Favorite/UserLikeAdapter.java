package com.example.a114202_metro.Favorite;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a114202_metro.R;

import java.util.ArrayList;
import java.util.List;

public class UserLikeAdapter extends RecyclerView.Adapter<UserLikeAdapter.VH> {

    public interface Listener {
        void onClickEdit(int position, UserLikeItem item, VH holder);
        void onClickDeleteBar(int position, UserLikeItem item, VH holder);
    }

    private final List<UserLikeItem> items = new ArrayList<>();
    private final Listener listener;

    public UserLikeAdapter(Listener listener) {
        this.listener = listener;
    }

    public static class VH extends RecyclerView.ViewHolder {
         View foreground;
        View deleteBar;
        TextView name;
        ImageButton btnEdit;

        public VH(@NonNull View itemView) {
            super(itemView);
            foreground = itemView.findViewById(R.id.foreground_container);
            deleteBar  = itemView.findViewById(R.id.btnDeleteBar);
            name       = itemView.findViewById(R.id.text_folder_name);
            btnEdit    = itemView.findViewById(R.id.btnEdit);
        }

        public void closeReveal() {
            if (foreground != null) foreground.animate().translationX(0f).setDuration(150).start();
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder_likes, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        UserLikeItem item = items.get(position);
        h.name.setText(item.fileName);

        // ✅ 重置狀態，避免重用殘留
        h.foreground.setTranslationX(0f);
        h.deleteBar.setVisibility(View.INVISIBLE);

        h.btnEdit.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            listener.onClickEdit(pos, items.get(pos), h);
        });

        h.deleteBar.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            listener.onClickDeleteBar(pos, items.get(pos), h);
        });
    }


    @Override public int getItemCount() { return items.size(); }

    // 公用方法
    public void setData(List<UserLikeItem> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    public void addFirst(UserLikeItem item) {
        items.add(0, item);
        notifyItemInserted(0);
    }

    public void updateName(int position, String newName) {
        items.get(position).fileName = newName;
        notifyItemChanged(position);
    }

    public void removeAt(int position) {
        items.remove(position);
        notifyItemRemoved(position);
    }

    public boolean isEmpty() { return items.isEmpty(); }
}
