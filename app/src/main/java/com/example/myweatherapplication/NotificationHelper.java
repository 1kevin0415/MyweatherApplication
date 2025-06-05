package com.example.myweatherapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log;
public class NotificationHelper {

    private static final String CHANNEL_ID_DIARY = "diary_reminder_channel";
    private static final String CHANNEL_NAME_DIARY = "日记日程提醒";
    private static final String CHANNEL_DESC_DIARY = "用于日记和日程的提醒通知";

    /**
     * 创建通知渠道 (仅在API 26+ 需要)
     * 应在应用启动时或首次发送通知前调用
     * @param context Context
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID_DIARY,
                    CHANNEL_NAME_DIARY,
                    NotificationManager.IMPORTANCE_HIGH // 设置高重要性，确保提醒能弹出
            );
            channel.setDescription(CHANNEL_DESC_DIARY);
            // 可以进一步配置渠道，如灯光、震动等
            // channel.enableLights(true);
            // channel.setLightColor(Color.RED);
            // channel.enableVibration(true);
            // channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 显示日记/日程提醒通知
     * @param context Context
     * @param notificationId 通知的唯一ID (可以使用日程的数据库ID)
     * @param title 通知标题 (例如日程的标题)
     * @param content 通知内容 (例如日程的内容预览或提醒信息)
     * @param diaryEntryId 日记条目的ID，用于点击通知时跳转
     */
    public static void showDiaryNotification(Context context, int notificationId, String title, String content, long diaryEntryId) {
        // 创建通知渠道 (如果还没创建)
        createNotificationChannel(context); // 确保渠道存在

        // 创建点击通知时要执行的Intent (例如打开查看/编辑日记的Activity)
        Intent intent = new Intent(context, ViewEditDiaryActivity.class);
        intent.putExtra(ViewEditDiaryActivity.EXTRA_DIARY_ID, diaryEntryId); // 传递日记ID
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_DIARY)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // 你可以使用一个更合适的日记/提醒图标
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // 高优先级
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 在锁屏上显示内容
                .setContentIntent(pendingIntent) // 设置点击通知后的操作
                .setAutoCancel(true); // 点击后自动移除通知

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            // 在Android 13 (API 33)及以上版本，如果应用没有POST_NOTIFICATIONS权限，这里会抛出SecurityException
            // 你已经在AndroidManifest.xml中声明了权限，但用户可能禁用了它。
            // 实际应用中，应在尝试发送通知前检查权限。
            Log.e("NotificationHelper", "无法显示通知，请检查POST_NOTIFICATIONS权限", e);
        }
    }
}