package com.example.myweatherapplication;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ViewEditDiaryActivity extends AppCompatActivity {

    private static final String TAG = "ViewEditDiaryActivity";
    public static final String EXTRA_DIARY_ID = "com.example.myweatherapplication.DIARY_ID";

    private EditText editTextDiaryTitle, editTextDiaryContent;
    private DatePicker datePickerDiary;
    private Button buttonUpdateDiary, buttonDeleteDiary;

    private SwitchCompat switchIsSchedule;
    private LinearLayout layoutScheduleTime;
    private Button buttonSelectScheduleTime;
    private TextView textViewSelectedScheduleTime;
    private int scheduleHour = -1;
    private int scheduleMinute = -1;

    private DiaryDbHelper dbHelper;
    private long currentDiaryId = -1;
    private long currentUserId = -1;
    private DiaryEntry currentDiaryEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_edit_diary);

        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        currentUserId = prefs.getLong(LoginActivity.KEY_USER_ID, -1);

        if (currentUserId == -1) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DiaryDbHelper(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("日记/日程详情");
        }

        editTextDiaryTitle = findViewById(R.id.editTextDiaryTitle);
        editTextDiaryContent = findViewById(R.id.editTextDiaryContent);
        datePickerDiary = findViewById(R.id.datePickerDiary);
        buttonUpdateDiary = findViewById(R.id.buttonUpdateDiary);
        buttonDeleteDiary = findViewById(R.id.buttonDeleteDiary);
        switchIsSchedule = findViewById(R.id.switchIsSchedule);
        layoutScheduleTime = findViewById(R.id.layoutScheduleTime);
        buttonSelectScheduleTime = findViewById(R.id.buttonSelectScheduleTime);
        textViewSelectedScheduleTime = findViewById(R.id.textViewSelectedScheduleTime);

        currentDiaryId = getIntent().getLongExtra(EXTRA_DIARY_ID, -1);

        if (currentDiaryId != -1) {
            loadDiaryEntry();
        } else {
            Toast.makeText(this, "无法加载日记，ID无效", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        switchIsSchedule.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutScheduleTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                scheduleHour = -1;
                scheduleMinute = -1;
                updateSelectedScheduleTimeText();
            }
        });

        buttonSelectScheduleTime.setOnClickListener(v -> showTimePickerDialog());
        buttonUpdateDiary.setOnClickListener(v -> updateDiary());
        buttonDeleteDiary.setOnClickListener(v -> confirmDeleteDiary());
    }

    private void loadDiaryEntry() {
        currentDiaryEntry = dbHelper.getDiaryEntryById(currentDiaryId, currentUserId);
        if (currentDiaryEntry != null) {
            editTextDiaryTitle.setText(currentDiaryEntry.getTitle());
            editTextDiaryContent.setText(currentDiaryEntry.getContent());
            try {
                Date entryDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(currentDiaryEntry.getDate());
                if (entryDate != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(entryDate);
                    datePickerDiary.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date", e);
            }

            switchIsSchedule.setChecked(currentDiaryEntry.isSchedule());
            layoutScheduleTime.setVisibility(currentDiaryEntry.isSchedule() ? View.VISIBLE : View.GONE);
            if (currentDiaryEntry.isSchedule() && currentDiaryEntry.getScheduleTimeMillis() > 0) {
                Calendar scheduleCal = Calendar.getInstance();
                scheduleCal.setTimeInMillis(currentDiaryEntry.getScheduleTimeMillis());
                scheduleHour = scheduleCal.get(Calendar.HOUR_OF_DAY);
                scheduleMinute = scheduleCal.get(Calendar.MINUTE);
            } else {
                scheduleHour = -1;
                scheduleMinute = -1;
            }
            updateSelectedScheduleTimeText();
        } else {
            Toast.makeText(this, "找不到日记或无权访问", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    scheduleHour = hourOfDay;
                    scheduleMinute = minute;
                    updateSelectedScheduleTimeText();
                }, scheduleHour != -1 ? scheduleHour : calendar.get(Calendar.HOUR_OF_DAY), scheduleMinute != -1 ? scheduleMinute : calendar.get(Calendar.MINUTE), true);
        timePickerDialog.show();
    }

    private void updateSelectedScheduleTimeText() {
        if (scheduleHour != -1 && scheduleMinute != -1) {
            textViewSelectedScheduleTime.setText(String.format(Locale.getDefault(), "%02d:%02d", scheduleHour, scheduleMinute));
        } else {
            textViewSelectedScheduleTime.setText("未设置");
        }
    }

    private void updateDiary() {
        if (currentDiaryEntry == null) return;
        String title = editTextDiaryTitle.getText().toString().trim();
        String content = editTextDiaryContent.getText().toString().trim();
        int day = datePickerDiary.getDayOfMonth();
        int month = datePickerDiary.getMonth();
        int year = datePickerDiary.getYear();
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        String entryDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        boolean isScheduleNew = switchIsSchedule.isChecked();
        long scheduleTimeMillisNew = 0L;

        if (isScheduleNew) {
            if (scheduleHour != -1 && scheduleMinute != -1) {
                calendar.set(Calendar.HOUR_OF_DAY, scheduleHour);
                calendar.set(Calendar.MINUTE, scheduleMinute);
                scheduleTimeMillisNew = calendar.getTimeInMillis();
                if (scheduleTimeMillisNew <= System.currentTimeMillis()) {
                    Toast.makeText(this, "提醒时间必须晚于当前时间", Toast.LENGTH_LONG).show();
                    return;
                }
            } else {
                Toast.makeText(this, "请为日程选择一个提醒时间", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "标题和内容至少填写一项", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentDiaryEntry.isSchedule() && currentDiaryEntry.getScheduleTimeMillis() > 0) {
            AddDiaryActivity.cancelAlarm(this, currentDiaryEntry.getId());
        }

        currentDiaryEntry.setTitle(title);
        currentDiaryEntry.setContent(content);
        currentDiaryEntry.setDate(entryDate);
        currentDiaryEntry.setSchedule(isScheduleNew);
        currentDiaryEntry.setScheduleTimeMillis(scheduleTimeMillisNew);

        int rowsAffected = dbHelper.updateDiaryEntry(currentDiaryEntry, currentUserId);
        if (rowsAffected > 0) {
            Toast.makeText(this, "日记已更新", Toast.LENGTH_SHORT).show();
            if (isScheduleNew && scheduleTimeMillisNew > 0) {
                AddDiaryActivity.scheduleAlarm(this, currentDiaryEntry.getId(), scheduleTimeMillisNew, title, content);
            }
            finish();
        } else {
            Toast.makeText(this, "更新失败或未做修改", Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDeleteDiary() {
        if (currentDiaryEntry == null) return;
        new AlertDialog.Builder(this)
                .setTitle("删除日记")
                .setMessage("你确定要删除这条日记吗？此操作无法撤销。")
                .setPositiveButton("删除", (dialog, which) -> deleteDiary())
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteDiary() {
        if (currentDiaryEntry.isSchedule() && currentDiaryEntry.getScheduleTimeMillis() > 0) {
            AddDiaryActivity.cancelAlarm(this, currentDiaryEntry.getId());
        }

        if (dbHelper.deleteDiaryEntry(currentDiaryId, currentUserId)) {
            Toast.makeText(this, "日记已删除", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}