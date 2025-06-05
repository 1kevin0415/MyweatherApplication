package com.example.myweatherapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.Bundle;
public class DiaryAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "DiaryAlarmReceiver";

    // 定义Intent中携带数据的Key (与设置闹钟时放入的Key一致)
    public static final String EXTRA_NOTIFICATION_ID = "com.example.myweatherapplication.EXTRA_NOTIFICATION_ID";
    public static final String EXTRA_DIARY_ID = "com.example.myweatherapplication.EXTRA_DIARY_ID";
    public static final String EXTRA_DIARY_TITLE = "com.example.myweatherapplication.EXTRA_DIARY_TITLE";
    public static final String EXTRA_DIARY_CONTENT = "com.example.myweatherapplication.EXTRA_DIARY_CONTENT";


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "闹钟触发！准备显示通知。");

        // 从Intent中提取数据
        // 使用getExtras()更安全，以防某个extra不存在
        Bundle extras = intent.getExtras();
        if (extras == null) {
            Log.e(TAG, "接收到的Intent没有附加数据！无法显示通知。");
            return;
        }

        // 使用与放入时相同的默认值来获取数据，以防某个键不存在
        int notificationId = extras.getInt(EXTRA_NOTIFICATION_ID, (int) System.currentTimeMillis()); // 如果没有提供，用当前时间作为ID
        long diaryId = extras.getLong(EXTRA_DIARY_ID, -1);
        String title = extras.getString(EXTRA_DIARY_TITLE, "日程提醒");
        String content = extras.getString(EXTRA_DIARY_CONTENT, "你有一个日程需要查看。");

        if (diaryId == -1) {
            Log.e(TAG, "未提供有效的日记ID！");
            // 也可以选择仍然显示一个通用通知
        }

        Log.d(TAG, "通知信息 - ID: " + notificationId + ", 日记ID: " + diaryId + ", 标题: " + title);

        // 使用 NotificationHelper 显示通知
        NotificationHelper.showDiaryNotification(context, notificationId, title, content, diaryId);
    }
}