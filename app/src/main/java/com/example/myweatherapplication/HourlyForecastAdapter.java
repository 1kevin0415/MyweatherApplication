package com.example.myweatherapplication;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HourlyForecastAdapter extends RecyclerView.Adapter<HourlyForecastAdapter.ViewHolder> {

    private final List<HourlyForecastItem> hourlyItems;
    private final Context context;
    private static final String TAG = "HourlyAdapter"; // 用于日志记录

    public HourlyForecastAdapter(Context context, List<HourlyForecastItem> hourlyItems) {
        this.context = context;
        this.hourlyItems = hourlyItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hourly_forecast, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HourlyForecastItem currentItem = hourlyItems.get(position);

        holder.timeTextView.setText(currentItem.getFxTime()); // fxTime 应该已经被格式化为 HH:mm
        holder.tempTextView.setText(currentItem.getTemp());   // temp 应该已经被格式化为 XX°

        // 动态设置天气图标
        String iconCode = currentItem.getIcon();
        String iconResourceName = "weather_icon_" + iconCode;
        int iconResId = context.getResources().getIdentifier(iconResourceName, "drawable", context.getPackageName());

        if (iconResId != 0) {
            holder.iconImageView.setImageResource(iconResId);
        } else {
            Log.w(TAG, "找不到图标，代码: " + iconCode + " (尝试的资源名: " + iconResourceName + ")");
            holder.iconImageView.setImageResource(R.mipmap.ic_launcher); // 使用App启动图标作为备用
        }
    }

    @Override
    public int getItemCount() {
        if (hourlyItems == null) {
            return 0;
        }
        return hourlyItems.size();
    }

    /**
     * ViewHolder内部类，持有 item_hourly_forecast.xml 布局中的视图引用
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView timeTextView;
        public final ImageView iconImageView;
        public final TextView tempTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            timeTextView = itemView.findViewById(R.id.item_hourly_time);
            iconImageView = itemView.findViewById(R.id.item_hourly_icon);
            tempTextView = itemView.findViewById(R.id.item_hourly_temp);
        }
    }
}