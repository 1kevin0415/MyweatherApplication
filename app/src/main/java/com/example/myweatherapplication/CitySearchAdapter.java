package com.example.myweatherapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.ArrayList; // 【新增】导入 ArrayList

public class CitySearchAdapter extends RecyclerView.Adapter<CitySearchAdapter.ViewHolder> {

    private List<SearchedCity> cityList;
    private Context context;
    private OnCityItemClickListener listener;

    // 接口用于处理列表项的点击事件
    public interface OnCityItemClickListener {
        void onCityClick(SearchedCity city);
    }

    public CitySearchAdapter(Context context, List<SearchedCity> cityList, OnCityItemClickListener listener) {
        this.context = context;
        // 【修改】确保即使传入的cityList为null，成员变量也被初始化为一个空的ArrayList，防止后续空指针
        this.cityList = (cityList == null) ? new ArrayList<>() : cityList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_searched_city, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchedCity city = cityList.get(position);
        if (city != null) {
            holder.cityName.setText(city.getName());
            holder.cityAdmin.setText(city.getFormattedLocation()); // 使用我们定义的getFormattedLocation()

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCityClick(city);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return cityList == null ? 0 : cityList.size();
    }

    // 用于从Activity更新数据列表并刷新RecyclerView
    public void updateCityList(List<SearchedCity> newCityList) {
        if (newCityList == null) {
            this.cityList.clear();
        } else {
            this.cityList = newCityList;
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView cityName;
        TextView cityAdmin;

        ViewHolder(View itemView) {
            super(itemView);
            cityName = itemView.findViewById(R.id.textViewSearchedCityName);
            cityAdmin = itemView.findViewById(R.id.textViewSearchedCityAdmin);
        }
    }
}