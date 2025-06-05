package com.example.myweatherapplication;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DailyForecastAdapter extends RecyclerView.Adapter<DailyForecastAdapter.ViewHolder> {

    private final List<DailyForecastItem> forecastItems;
    private final Context context;

    public DailyForecastAdapter(Context context, List<DailyForecastItem> forecastItems) {
        this.context = context;
        this.forecastItems = forecastItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_forecast, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyForecastItem currentItem = forecastItems.get(position);

        holder.dateTextView.setText(currentItem.getFxDate());
        holder.conditionTextView.setText(currentItem.getTextDay());
        String temperatureText = currentItem.getTempMax() + "° / " + currentItem.getTempMin() + "°";
        holder.temperatureTextView.setText(temperatureText);

        String iconCode = currentItem.getIconDay();
        String iconResourceName = "weather_icon_" + iconCode;
        int iconResId = context.getResources().getIdentifier(iconResourceName, "drawable", context.getPackageName());

        if (iconResId != 0) {
            holder.weatherIconImageView.setImageResource(iconResId);
        } else {
            Log.w("DailyForecastAdapter", "Icon not found for code: " + iconCode + " (tried " + iconResourceName + ")");
            holder.weatherIconImageView.setImageResource(R.mipmap.ic_launcher);
        }

        holder.itemView.setOnClickListener(v -> {
            // **【修正】** 使用正确的 Activity 名称
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("EXTRA_FORECAST_ITEM", currentItem);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return forecastItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView weatherIconImageView;
        public final TextView dateTextView;
        public final TextView conditionTextView;
        public final TextView temperatureTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            weatherIconImageView = itemView.findViewById(R.id.item_weather_icon);
            dateTextView = itemView.findViewById(R.id.item_date);
            conditionTextView = itemView.findViewById(R.id.item_condition);
            temperatureTextView = itemView.findViewById(R.id.item_temperature);
        }
    }
}