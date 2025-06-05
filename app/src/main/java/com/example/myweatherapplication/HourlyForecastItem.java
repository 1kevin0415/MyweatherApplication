package com.example.myweatherapplication;

import java.io.Serializable;

public class HourlyForecastItem implements Serializable {
    private String fxTime;
    private String temp;
    private String icon;
    private String text;
    // 你可以根据需要从API文档中添加更多字段，比如风力、湿度等

    // 构造函数
    public HourlyForecastItem(String fxTime, String temp, String icon, String text) {
        this.fxTime = fxTime;
        this.temp = temp;
        this.icon = icon;
        this.text = text;
    }

    // Getter 方法
    public String getFxTime() {
        // 我们可能只需要显示小时，例如 "15:00" 而不是完整的 "2021-02-16T15:00+08:00"
        // 这里可以做一个简单的处理，只取时间部分
        if (fxTime != null && fxTime.contains("T") && fxTime.contains(":")) {
            try {
                return fxTime.substring(fxTime.indexOf("T") + 1, fxTime.lastIndexOf(":"));
            } catch (Exception e) {
                // 如果格式不符合预期，返回原始时间
                return fxTime;
            }
        }
        return fxTime;
    }

    public String getTemp() {
        return temp + "°"; // 直接加上单位
    }

    public String getIcon() {
        return icon;
    }

    public String getText() {
        return text;
    }
}