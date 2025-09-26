package com.example.a114202_metro.StationPlace;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.a114202_metro.R;

import java.util.ArrayList;
import java.util.List;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.VH> {

    private final List<PlaceModel> data = new ArrayList<>();

    /** 一次替換整份列表資料 */
    public void submit(List<PlaceModel> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_station_place, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PlaceModel m = data.get(position);

        // 名稱
        h.placeName.setText(TextUtils.isEmpty(m.name) ? "(未命名地點)" : m.name);

        // 圖片
        if (!TextUtils.isEmpty(m.photoUrl)) {
            Glide.with(h.itemView.getContext())
                    .load(m.photoUrl)
                    .centerCrop()
                    .placeholder(R.drawable.metro_photo) // 你自己的占位圖
                    .into(h.placeImage);
        } else {
            h.placeImage.setImageResource(R.drawable.metro_photo);
        }

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    /** 預留：取得單筆資料 */
    public PlaceModel getItem(int position) {
        return (position >= 0 && position < data.size()) ? data.get(position) : null;
    }

    /** ViewHolder */
    static class VH extends RecyclerView.ViewHolder {
        ImageView placeImage;
        TextView placeName;
        ImageView btnFavorite, btnAdd; // 先保留，如果你要用

        VH(@NonNull View itemView) {
            super(itemView);
            placeImage = itemView.findViewById(R.id.placeImage);
            placeName  = itemView.findViewById(R.id.placeName);
            btnFavorite= itemView.findViewById(R.id.btnFavorite);
            btnAdd     = itemView.findViewById(R.id.btnAdd);
        }
    }
}
