package com.example.myweatherapplication;

import java.io.Serializable;

public class DailyForecastItem implements Serializable {
    // 原有字段
    private String fxDate;
    private String tempMax;
    private String tempMin;
    private String iconDay;
    private String textDay;

    // 【新增】详细信息字段
    private String humidity;
    private String windDirDay;
    private String windScaleDay;
    private String pressure;
    private String sunrise;
    private String sunset;
    private String uvIndex;


    // 【修改】构造函数以包含所有新字段
    public DailyForecastItem(String fxDate, String tempMax, String tempMin, String iconDay, String textDay,
                             String humidity, String windDirDay, String windScaleDay, String pressure,
                             String sunrise, String sunset, String uvIndex) {
        this.fxDate = fxDate;
        this.tempMax = tempMax;
        this.tempMin = tempMin;
        this.iconDay = iconDay;
        this.textDay = textDay;
        this.humidity = humidity;
        this.windDirDay = windDirDay;
        this.windScaleDay = windScaleDay;
        this.pressure = pressure;
        this.sunrise = sunrise;
        this.sunset = sunset;
        this.uvIndex = uvIndex;
    }

    // --- 原有Getter方法保持不变 ---
    public String getFxDate() { return fxDate; }
    public String getTempMax() { return tempMax; }
    public String getTempMin() { return tempMin; }
    public String getIconDay() { return iconDay; }
    public String getTextDay() { return textDay; }

    // 【新增】新字段的Getter方法
    public String getHumidity() { return humidity; }
    public String getWindDirDay() { return windDirDay; }
    public String getWindScaleDay() { return windScaleDay; }
    public String getPressure() { return pressure; }
    public String getSunrise() { return sunrise; }
    public String getSunset() { return sunset; }
    public String getUvIndex() { return uvIndex; }
}