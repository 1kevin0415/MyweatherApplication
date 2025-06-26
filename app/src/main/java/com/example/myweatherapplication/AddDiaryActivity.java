package com.example.myweatherapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddDiaryActivity extends AppCompatActivity {

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
    private long currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_diary);

        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        currentUserId = prefs.getLong(LoginActivity.KEY_USER_ID, -1);

        if (currentUserId == -1) {
            Toast.makeText(this, "用户未登录，无法添加日记", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    scheduleHour = hourOfDay;
                    scheduleMinute = minute;
                    updateSelectedScheduleTimeText();
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
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

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "标题和内容至少填写一项", Toast.LENGTH_SHORT).show();
            return;
        }

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
            } else {
                Toast.makeText(this, "请为日程选择一个提醒时间", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        DiaryEntry newEntry = new DiaryEntry(title, content, entryDate, createdAt, isSchedule, scheduleTimeMillis);
        long newEntryId = dbHelper.addDiaryEntry(newEntry, currentUserId);

        if (newEntryId != -1) {
            Toast.makeText(this, isSchedule ? "日程已保存！" : "日记已保存！", Toast.LENGTH_SHORT).show();
            if (isSchedule && scheduleTimeMillis > 0) {
                scheduleAlarm(this, newEntryId, scheduleTimeMillis, title, content);
            }
            finish();
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    public static void scheduleAlarm(Context context, long diaryId, long triggerAtMillis, String title, String content) {
        // scheduleAlarm 逻辑不变
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "Cannot get AlarmManager service");
            return;
        }
        Intent intent = new Intent(context, DiaryAlarmReceiver.class);
        intent.putExtra(DiaryAlarmReceiver.EXTRA_NOTIFICATION_ID, (int) diaryId);
        intent.putExtra(DiaryAlarmReceiver.EXTRA_DIARY_ID, diaryId);
        intent.putExtra(DiaryAlarmReceiver.EXTRA_DIARY_TITLE, title);
        intent.putExtra(DiaryAlarmReceiver.EXTRA_DIARY_CONTENT, content.length() > 100 ? content.substring(0, 100) + "..." : content);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) diaryId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Inform user or request permission
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public static void cancelAlarm(Context context, long diaryId) {
        // cancelAlarm 逻辑不变
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        Intent intent = new Intent(context, DiaryAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) diaryId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}