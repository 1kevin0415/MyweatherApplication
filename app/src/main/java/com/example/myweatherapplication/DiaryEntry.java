package com.example.myweatherapplication; // 确保包名正确

import java.io.Serializable;
import java.text.SimpleDateFormat; // 【新增】用于在toString中格式化时间
import java.util.Date;           // 【新增】用于在toString中格式化时间
import java.util.Locale;         // 【新增】用于在toString中格式化时间

public class DiaryEntry implements Serializable { // 实现Serializable方便以后可能在Activity间传递对象

    private long id;          // 日记的唯一ID，数据库主键
    private String title;     // 日记标题
    private String content;   // 日记内容
    private String date;      // 日记/日程对应的日期 "yyyy-MM-dd"
    private long createdAt;   // 创建/最后修改时间戳

    // 【新增】日程相关字段
    private boolean isSchedule;          // 是否为日程 (true表示是日程, false表示普通日记)
    private long scheduleTimeMillis;    // 日程的具体提醒时间戳 (如果isSchedule为true，则此字段有效)

    // 【修改】构造函数 - 用于从数据库读取时创建对象
    public DiaryEntry(long id, String title, String content, String date, long createdAt, boolean isSchedule, long scheduleTimeMillis) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
        this.createdAt = createdAt;
        this.isSchedule = isSchedule;
        this.scheduleTimeMillis = scheduleTimeMillis;
    }

    // 【修改】构造函数 - 用于创建新日记/日程时（ID由数据库自动生成）
    public DiaryEntry(String title, String content, String date, long createdAt, boolean isSchedule, long scheduleTimeMillis) {
        this.title = title;
        this.content = content;
        this.date = date;
        this.createdAt = createdAt;
        this.isSchedule = isSchedule;
        this.scheduleTimeMillis = scheduleTimeMillis;
    }

    // --- 原有 Getter 和 Setter 方法 ---
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        // 【小优化】防止返回null，如果为null则返回空字符串
        return title == null ? "" : title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        // 【小优化】防止返回null
        return content == null ? "" : content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    // --- 【新增】新字段的 Getter 和 Setter 方法 ---
    public boolean isSchedule() {
        return isSchedule;
    }

    public void setSchedule(boolean schedule) {
        isSchedule = schedule;
    }

    public long getScheduleTimeMillis() {
        return scheduleTimeMillis;
    }

    public void setScheduleTimeMillis(long scheduleTimeMillis) {
        this.scheduleTimeMillis = scheduleTimeMillis;
    }


    // 【修改】更新toString方便调试，加入日程信息
    @Override
    public String toString() {
        String scheduleInfo = "";
        if (isSchedule && scheduleTimeMillis > 0) {
            // 将时间戳格式化为可读的日期时间字符串
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            scheduleInfo = ", scheduleTime=" + sdf.format(new Date(scheduleTimeMillis));
        }
        return "DiaryEntry{" +
                "id=" + id +
                ", title='" + getTitle() + '\'' +
                ", date='" + date + '\'' +
                ", isSchedule=" + isSchedule +
                scheduleInfo +
                '}';
    }
}