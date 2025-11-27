package com.aniketjain.weatherapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aniketjain.weatherapp.R;

import java.util.List;

public class CityHistoryAdapter extends RecyclerView.Adapter<CityHistoryAdapter.HistoryViewHolder> {
    private final Context context;
    private final List<String> cities;
    private final OnCityClickListener listener;

    public interface OnCityClickListener {
        void onCityClick(String cityName);
    }

    public CityHistoryAdapter(Context context, List<String> cities, OnCityClickListener listener) {
        this.context = context;
        this.cities = cities;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_city_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        String cityName = cities.get(position);
        holder.cityNameTv.setText(cityName);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCityClick(cityName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cities.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView cityNameTv;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cityNameTv = itemView.findViewById(R.id.city_name_tv);
        }
    }
}