package com.example.myweatherapplication;

import android.app.TimePickerDialog;
import android.content.Context; // 【新增】
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri; // 【新增】
import android.os.Build; // 【新增】
import android.os.Bundle;
import android.provider.Settings; // 【新增】
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout; // 【新增】
import android.widget.TextView;
import android.widget.TimePicker; // 【新增】
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat; // 【新增】

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
// 【新增】导入AlarmManager和PendingIntent，虽然我们是通过AddDiaryActivity调用，但保持清晰
import android.app.AlarmManager;
import android.app.PendingIntent;


public class ViewEditDiaryActivity extends AppCompatActivity {

    private static final String TAG = "ViewEditDiaryActivity";
    public static final String EXTRA_DIARY_ID = "com.example.myweatherapplication.DIARY_ID";

    private EditText editTextDiaryTitle, editTextDiaryContent;
    private DatePicker datePickerDiary;
    private Button buttonUpdateDiary, buttonDeleteDiary;
    private TextView textViewViewEditTitle;

    // 【新增】日程相关的UI控件和变量
    private SwitchCompat switchIsSchedule;
    private LinearLayout layoutScheduleTime;
    private Button buttonSelectScheduleTime;
    private TextView textViewSelectedScheduleTime;
    private int scheduleHour = -1;
    private int scheduleMinute = -1;

    private DiaryDbHelper dbHelper;
    private long currentDiaryId = -1;
    private DiaryEntry currentDiaryEntry; // 用于存储从数据库加载的原始日程信息

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_edit_diary); // 确保这个布局与AddDiaryActivity的类似

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
        textViewViewEditTitle = findViewById(R.id.textViewViewEditTitle);

        // 【新增】找到日程相关的UI控件
        switchIsSchedule = findViewById(R.id.switchIsSchedule); // 确保XML中有此ID
        layoutScheduleTime = findViewById(R.id.layoutScheduleTime); // 确保XML中有此ID
        buttonSelectScheduleTime = findViewById(R.id.buttonSelectScheduleTime); // 确保XML中有此ID
        textViewSelectedScheduleTime = findViewById(R.id.textViewSelectedScheduleTime); // 确保XML中有此ID

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_DIARY_ID)) {
            currentDiaryId = intent.getLongExtra(EXTRA_DIARY_ID, -1);
        }

        if (currentDiaryId != -1) {
            loadDiaryEntry(); // 加载现有数据
        } else {
            Log.e(TAG, "No diary ID passed. This activity is for viewing/editing existing entries.");
            Toast.makeText(this, "无法加载日记，ID无效", Toast.LENGTH_LONG).show();
            finish(); // 如果没有有效ID，直接关闭
            return;
        }

        // 【新增】为Switch设置监听，控制提醒时间布局的可见性
        switchIsSchedule.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutScheduleTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                scheduleHour = -1;
                scheduleMinute = -1;
                updateSelectedScheduleTimeText();
            }
        });

        // 【新增】为“选择提醒时间”按钮设置点击事件
        buttonSelectScheduleTime.setOnClickListener(v -> showTimePickerDialog());

        buttonUpdateDiary.setOnClickListener(v -> updateDiary());
        buttonDeleteDiary.setOnClickListener(v -> confirmDeleteDiary());
    }

    private void loadDiaryEntry() {
        currentDiaryEntry = dbHelper.getDiaryEntryById(currentDiaryId);
        if (currentDiaryEntry != null) {
            editTextDiaryTitle.setText(currentDiaryEntry.getTitle());
            editTextDiaryContent.setText(currentDiaryEntry.getContent());

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            try {
                Date entryDate = dateFormat.parse(currentDiaryEntry.getDate());
                if (entryDate != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(entryDate);
                    datePickerDiary.updateDate(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                    );
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date from DB: " + currentDiaryEntry.getDate(), e);
            }

            // 【新增】加载日程信息
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
            Toast.makeText(this, "找不到ID为 " + currentDiaryId + " 的日记条目", Toast.LENGTH_LONG).show();
            finish();
        }
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


    private void updateDiary() {
        if (currentDiaryEntry == null) {
            Toast.makeText(this, "没有可更新的日记数据", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = editTextDiaryTitle.getText().toString().trim();
        String content = editTextDiaryContent.getText().toString().trim();

        int day = datePickerDiary.getDayOfMonth();
        int month = datePickerDiary.getMonth();
        int year = datePickerDiary.getYear();
        Calendar calendar = Calendar.getInstance(); // 用来构建 scheduleTimeMillis
        calendar.set(year, month, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String entryDate = dateFormat.format(calendar.getTime()); // 这是日记的日期

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

        // 【修改】先取消旧的闹钟（如果之前是日程并且设置了时间）
        if (currentDiaryEntry.isSchedule() && currentDiaryEntry.getScheduleTimeMillis() > 0) {
            AddDiaryActivity.cancelAlarm(this, currentDiaryEntry.getId());
            Log.d(TAG, "已取消旧闹钟 for ID: " + currentDiaryEntry.getId());
        }

        // 更新DiaryEntry对象
        currentDiaryEntry.setTitle(title);
        currentDiaryEntry.setContent(content);
        currentDiaryEntry.setDate(entryDate);
        currentDiaryEntry.setSchedule(isScheduleNew);
        currentDiaryEntry.setScheduleTimeMillis(scheduleTimeMillisNew);
        // createdAt 通常不更新，若要更新“最后修改时间”，需另加字段和逻辑

        int rowsAffected = dbHelper.updateDiaryEntry(currentDiaryEntry);
        if (rowsAffected > 0) {
            Toast.makeText(this, "日记已更新", Toast.LENGTH_SHORT).show();
            // 如果更新后是日程并且设置了时间，则设置新的闹钟
            if (isScheduleNew && scheduleTimeMillisNew > 0) {
                AddDiaryActivity.scheduleAlarm(this, currentDiaryEntry.getId(), scheduleTimeMillisNew, title, content);
                Log.d(TAG, "已为更新后的日程设置新闹钟 for ID: " + currentDiaryEntry.getId());
            }
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "更新日记失败或未做修改", Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDeleteDiary() {
        if (currentDiaryId == -1 || currentDiaryEntry == null) {
            Toast.makeText(this, "没有可删除的日记", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("删除日记")
                .setMessage("你确定要删除这条日记 “" + currentDiaryEntry.getTitle() + "” 吗？此操作无法撤销。")
                .setPositiveButton("删除", (dialog, which) -> deleteDiary())
                .setNegativeButton("取消", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteDiary() {
        // 【修改】在删除数据库条目前，先取消可能的闹钟
        if (currentDiaryEntry != null && currentDiaryEntry.isSchedule() && currentDiaryEntry.getScheduleTimeMillis() > 0) {
            AddDiaryActivity.cancelAlarm(this, currentDiaryEntry.getId());
            Log.d(TAG, "已取消被删除日程的闹钟 for ID: " + currentDiaryEntry.getId());
        }

        boolean success = dbHelper.deleteDiaryEntry(currentDiaryId);
        if (success) {
            Toast.makeText(this, "日记已删除", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "删除日记失败", Toast.LENGTH_SHORT).show();
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