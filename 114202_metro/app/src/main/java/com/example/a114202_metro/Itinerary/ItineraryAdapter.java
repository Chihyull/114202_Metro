package com.example.a114202_metro.Itinerary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a114202_metro.R;

import java.util.List;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ViewHolder> {

    private final List<ItineraryItem> list;

    public ItineraryAdapter(List<ItineraryItem> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_itinerary, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItineraryItem item = list.get(position);
        holder.title.setText(item.title);
        holder.date.setText(item.startDate + " ~ " + item.endDate);
        holder.dest.setText(item.dest);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, dest;
        ViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.itineraryTitle);
            date  = v.findViewById(R.id.itineraryDate);
            dest  = v.findViewById(R.id.itineraryDest);
        }
    }
}
