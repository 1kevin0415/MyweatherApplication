package com.example.myweatherapplication;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri; // 确保导入
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings; // 确保导入
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddDiaryActivity extends AppCompatActivity {

    // 【修改】将TAG设为public static final，以便静态方法访问，或者在静态方法中直接使用类名字符串
    public static final String TAG = "AddDiaryActivity";

    private EditText editTextDiaryTitle, editTextDiaryContent;
    private DatePicker datePickerDiary;
    private Button buttonSaveDiary;
    private DiaryDbHelper dbHelper;

    private SwitchCompat switchIsSchedule;
    private LinearLayout layoutScheduleTime;
    private Button buttonSelectScheduleTime;
    private TextView textViewSelectedScheduleTime;

    private int scheduleHour = -1;
    private int scheduleMinute = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_diary);

        dbHelper = new DiaryDbHelper(this);

        editTextDiaryTitle = findViewById(R.id.editTextDiaryTitle);
        editTextDiaryContent = findViewById(R.id.editTextDiaryContent);
        datePickerDiary = findViewById(R.id.datePickerDiary);
        buttonSaveDiary = findViewById(R.id.buttonSaveDiary);

        switchIsSchedule = findViewById(R.id.switchIsSchedule);
        layoutScheduleTime = findViewById(R.id.layoutScheduleTime);
        buttonSelectScheduleTime = findViewById(R.id.buttonSelectScheduleTime);
        textViewSelectedScheduleTime = findViewById(R.id.textViewSelectedScheduleTime);

        updateSelectedScheduleTimeText();
        switchIsSchedule.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutScheduleTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                scheduleHour = -1;
                scheduleMinute = -1;
                updateSelectedScheduleTimeText();
            }
        });

        buttonSelectScheduleTime.setOnClickListener(v -> showTimePickerDialog());
        buttonSaveDiary.setOnClickListener(v -> saveDiaryEntry());
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);
        int initialHour = (scheduleHour != -1) ? scheduleHour : currentHour;
        int initialMinute = (scheduleMinute != -1) ? scheduleMinute : currentMinute;

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    scheduleHour = hourOfDay;
                    scheduleMinute = minute;
                    updateSelectedScheduleTimeText();
                }, initialHour, initialMinute, true);
        timePickerDialog.show();
    }

    private void updateSelectedScheduleTimeText() {
        if (scheduleHour != -1 && scheduleMinute != -1) {
            textViewSelectedScheduleTime.setText(String.format(Locale.getDefault(), "%02d:%02d", scheduleHour, scheduleMinute));
        } else {
            textViewSelectedScheduleTime.setText("未设置");
        }
    }

    private void saveDiaryEntry() {
        String title = editTextDiaryTitle.getText().toString().trim();
        String content = editTextDiaryContent.getText().toString().trim();

        int day = datePickerDiary.getDayOfMonth();
        int month = datePickerDiary.getMonth();
        int year = datePickerDiary.getYear();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String entryDate = dateFormat.format(calendar.getTime());
        long createdAt = System.currentTimeMillis();

        boolean isSchedule = switchIsSchedule.isChecked();
        long scheduleTimeMillis = 0L;

        if (isSchedule) {
            if (scheduleHour != -1 && scheduleMinute != -1) {
                calendar.set(Calendar.HOUR_OF_DAY, scheduleHour);
                calendar.set(Calendar.MINUTE, scheduleMinute);
                scheduleTimeMillis = calendar.getTimeInMillis();

                if (scheduleTimeMillis <= System.currentTimeMillis()) {
                    Toast.makeText(this, "提醒时间必须晚于当前时间", Toast.LENGTH_LONG).show();
                    return;
                }
                Log.d(TAG, "日程提醒时间设置为: " + new Date(scheduleTimeMillis).toString());
            } else {
                Toast.makeText(this, "请为日程选择一个提醒时间", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "标题和内容至少填写一项", Toast.LENGTH_SHORT).show();
            return;
        }

        DiaryEntry newEntry = new DiaryEntry(title, content, entryDate, createdAt, isSchedule, scheduleTimeMillis);
        long newEntryId = dbHelper.addDiaryEntry(newEntry);

        if (newEntryId != -1) {
            String message = isSchedule ? "日程已保存！" : "日记已保存！";
            Toast.makeText(this, message + " ID: " + newEntryId, Toast.LENGTH_SHORT).show();

            if (isSchedule && scheduleTimeMillis > 0) {
                Log.d(TAG, "日程已保存，ID: " + newEntryId + ", 提醒时间戳: " + scheduleTimeMillis + ". 正在设置闹钟...");
                scheduleAlarm(this, newEntryId, scheduleTimeMillis, title, content);
            }
            finish();
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 【修改为 public static】设置闹钟提醒
     */
    public static void scheduleAlarm(Context context, long diaryId, long triggerAtMillis, String title, String content) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "无法获取AlarmManager服务 from scheduleAlarm");
            Toast.makeText(context, "无法设置提醒，AlarmManager不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(context, DiaryAlarmReceiver.class);
        intent.putExtra(DiaryAlarmReceiver.EXTRA_NOTIFICATION_ID, (int) diaryId);
        intent.putExtra(DiaryAlarmReceiver.EXTRA_DIARY_ID, diaryId);
        intent.putExtra(DiaryAlarmReceiver.EXTRA_DIARY_TITLE, title);
        intent.putExtra(DiaryAlarmReceiver.EXTRA_DIARY_CONTENT, content.length() > 100 ? content.substring(0, 100) + "..." : content);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) diaryId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                Log.d(TAG, "精确闹钟已设置 (API 31+): " + title + " at " + new Date(triggerAtMillis).toString());
            } else {
                Log.w(TAG, "没有精确闹钟权限，无法设置精确提醒。");
                Toast.makeText(context, "需要精确闹钟权限。请在系统设置中为本应用开启“闹钟和提醒”权限。", Toast.LENGTH_LONG).show();
                try {
                    context.startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(Uri.parse("package:" + context.getPackageName()))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); // 从非Activity Context启动需要此Flag
                    Log.d(TAG, "已尝试跳转到精确闹钟设置页面。");
                } catch (Exception e) {
                    Log.e(TAG, "无法跳转到精确闹钟设置页面", e);
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                    Log.d(TAG, "已尝试设置非精确闹钟 (API 31+) 因缺少或无法跳转到精确闹钟权限设置。");
                }
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            Log.d(TAG, "精确闹钟已设置 (API < 31): " + title + " at " + new Date(triggerAtMillis).toString());
        }
        // 这个Toast可以考虑移到调用scheduleAlarm之后，或者由调用者决定是否显示
        // Toast.makeText(context, "提醒已设置在 " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(triggerAtMillis)), Toast.LENGTH_LONG).show();
    }

    /**
     * 【新增并修改为 public static】取消指定ID的闹钟提醒
     */
    public static void cancelAlarm(Context context, long diaryId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "无法获取AlarmManager服务，无法取消闹钟 for ID: " + diaryId);
            return;
        }

        Intent intent = new Intent(context, DiaryAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) diaryId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE); // 使用与设置时相同的Flag

        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel(); // 确保PendingIntent也被取消

        Log.d(TAG, "已尝试取消ID为 " + diaryId + " 的闹钟提醒。");
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}