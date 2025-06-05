package com.example.myweatherapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    // 声明布局中的所有视图
    private TextView detailDate, detailTempRange, detailCondition;
    private TextView detailHumidity, detailWind, detailPressure, detailSunriseSunset, detailUv;
    private ImageView detailIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // 初始化所有视图
        findViews();

        // 从启动此Activity的Intent中获取传递过来的数据
        DailyForecastItem forecastItem = (DailyForecastItem) getIntent().getSerializableExtra("EXTRA_FORECAST_ITEM");

        // 检查数据是否成功传递
        if (forecastItem != null) {
            // 如果数据不为空，则用它来填充视图
            populateViews(forecastItem);
        } else {
            // 如果数据为空，显示错误提示并关闭此页面
            Toast.makeText(this, "无法加载天气详情", Toast.LENGTH_SHORT).show();
            Log.e("DetailActivity", "未能接收到天气预报对象");
            finish(); // 关闭当前Activity
        }
    }

    /**
     * 一个辅助方法，用于通过ID找到所有视图控件
     */
    private void findViews() {
        detailDate = findViewById(R.id.detail_date);
        detailTempRange = findViewById(R.id.detail_temp_range);
        detailCondition = findViewById(R.id.detail_condition);
        detailHumidity = findViewById(R.id.detail_humidity);
        detailWind = findViewById(R.id.detail_wind);
        detailPressure = findViewById(R.id.detail_pressure);
        detailSunriseSunset = findViewById(R.id.detail_sunrise_sunset);
        detailUv = findViewById(R.id.detail_uv);
        detailIcon = findViewById(R.id.detail_icon);
    }

    /**
     * 一个辅助方法，用于将DailyForecastItem对象的数据填充到视图中
     * @param item 包含详细天气数据的对象
     */
    private void populateViews(DailyForecastItem item) {
        // 设置所有TextView的文本
        detailDate.setText(item.getFxDate());
        detailCondition.setText(item.getTextDay());

        String tempRangeText = item.getTempMax() + "° / " + item.getTempMin() + "°";
        detailTempRange.setText(tempRangeText);

        String humidityText = item.getHumidity() + " %";
        detailHumidity.setText(humidityText);

        String windText = item.getWindDirDay() + " " + item.getWindScaleDay() + "级";
        detailWind.setText(windText);

        String pressureText = item.getPressure() + " hPa";
        detailPressure.setText(pressureText);

        String sunriseSunsetText = item.getSunrise() + " / " + item.getSunset();
        detailSunriseSunset.setText(sunriseSunsetText);

        detailUv.setText(item.getUvIndex());

        // 动态设置天气图标
        String iconCode = item.getIconDay();
        String iconResourceName = "weather_icon_" + iconCode;
        int iconResId = this.getResources().getIdentifier(iconResourceName, "drawable", this.getPackageName());

        if (iconResId != 0) {
            detailIcon.setImageResource(iconResId);
        } else {
            Log.w("DetailActivity", "找不到图标，代码: " + iconCode + " (尝试的资源名: " + iconResourceName + ")");
            detailIcon.setImageResource(R.mipmap.ic_launcher); // 设置备用图标
        }
    }
}