package com.example.myweatherapplication;

import android.util.Log;

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
        // API返回的时间格式是 "2025-06-25T19:00+08:00"
        // 我们只需要 "19:00" 这部分
        if (fxTime != null && fxTime.contains("T")) {
            try {
                // 找到 "T" 的位置
                int tIndex = fxTime.indexOf("T");
                // 从 "T" 的后一位开始，截取5个字符 (HH:mm)
                return fxTime.substring(tIndex + 1, tIndex + 6);
            } catch (Exception e) {
                // 如果出现任何异常，打印日志并返回原始字符串，防止崩溃
                Log.e("HourlyForecastItem", "解析时间字符串失败: " + fxTime, e);
                return fxTime;
            }
        }
        // 如果没有 "T"，按原样返回
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