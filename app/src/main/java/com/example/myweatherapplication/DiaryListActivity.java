package com.example.myweatherapplication;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DiaryListActivity extends AppCompatActivity implements DiaryListAdapter.OnDiaryItemClickListener {

    private static final String TAG = "DiaryListActivity";
    private RecyclerView recyclerViewDiaryEntries;
    private DiaryListAdapter diaryListAdapter;
    private List<DiaryEntry> diaryEntriesList;
    private DiaryDbHelper dbHelper;
    private FloatingActionButton fabAddDiary;
    private TextView textViewDiaryListTitle;

    private Button buttonSelectDate;
    private TextView textViewSelectedDate;
    private Button buttonShowAllDiaries;
    private Button buttonSelectMonth;
    private Button buttonSelectWeek; // 【新增】星期选择按钮

    private String currentFilterDate = null;
    private int currentFilterYear = -1;
    private int currentFilterMonth = -1;
    private String currentFilterWeekStartDate = null; // 【新增】用于存储当前筛选周的开始日期
    private String currentFilterWeekEndDate = null;   // 【新增】用于存储当前筛选周的结束日期


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_list);

        dbHelper = new DiaryDbHelper(this);
        diaryEntriesList = new ArrayList<>();

        textViewDiaryListTitle = findViewById(R.id.textViewDiaryListTitle);
        recyclerViewDiaryEntries = findViewById(R.id.recyclerViewDiaryEntries);
        fabAddDiary = findViewById(R.id.fabAddDiary);
        buttonSelectDate = findViewById(R.id.buttonSelectDate);
        textViewSelectedDate = findViewById(R.id.textViewSelectedDate);
        buttonShowAllDiaries = findViewById(R.id.buttonShowAllDiaries);
        buttonSelectMonth = findViewById(R.id.buttonSelectMonth);
        buttonSelectWeek = findViewById(R.id.buttonSelectWeek); // 【新增】找到星期按钮

        recyclerViewDiaryEntries.setLayoutManager(new LinearLayoutManager(this));
        diaryListAdapter = new DiaryListAdapter(this, diaryEntriesList, this);
        recyclerViewDiaryEntries.setAdapter(diaryListAdapter);

        fabAddDiary.setOnClickListener(view -> {
            Intent intent = new Intent(DiaryListActivity.this, AddDiaryActivity.class);
            startActivity(intent);
        });

        buttonSelectDate.setOnClickListener(v -> showDatePickerDialogForDay());
        buttonSelectMonth.setOnClickListener(v -> showMonthYearPickerDialog());
        buttonSelectWeek.setOnClickListener(v -> showDatePickerDialogForWeek()); // 【新增】调用新的方法

        buttonShowAllDiaries.setOnClickListener(v -> {
            currentFilterDate = null;
            currentFilterYear = -1;
            currentFilterMonth = -1;
            currentFilterWeekStartDate = null; // 【新增】清除星期筛选
            currentFilterWeekEndDate = null;
            loadDiaryEntries();
            textViewSelectedDate.setText("当前显示: 所有日记");
            buttonShowAllDiaries.setVisibility(View.GONE);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDiaryEntries();
    }

    private void loadDiaryEntries() {
        Log.d(TAG, "Loading diary entries... FilterDate: " + currentFilterDate +
                ", FilterYear: " + currentFilterYear + ", FilterMonth: " + currentFilterMonth +
                ", FilterWeekStart: " + currentFilterWeekStartDate);
        List<DiaryEntry> entries;

        if (currentFilterDate != null) { // 按日筛选
            entries = dbHelper.getDiariesForDate(currentFilterDate);
            textViewSelectedDate.setText("筛选日期: " + currentFilterDate);
            buttonShowAllDiaries.setVisibility(View.VISIBLE);
        } else if (currentFilterYear != -1 && currentFilterMonth != -1) { // 按月份筛选
            entries = dbHelper.getDiariesForMonth(currentFilterYear, currentFilterMonth);
            textViewSelectedDate.setText(String.format(Locale.getDefault(), "筛选月份: %04d-%02d", currentFilterYear, currentFilterMonth));
            buttonShowAllDiaries.setVisibility(View.VISIBLE);
        } else if (currentFilterWeekStartDate != null && currentFilterWeekEndDate != null) { // 【新增】按星期筛选
            entries = dbHelper.getDiariesForWeek(currentFilterWeekStartDate, currentFilterWeekEndDate);
            textViewSelectedDate.setText("筛选周: " + currentFilterWeekStartDate + " 至 " + currentFilterWeekEndDate);
            buttonShowAllDiaries.setVisibility(View.VISIBLE);
        }
        else { // 显示全部
            entries = dbHelper.getAllDiaryEntries();
            textViewSelectedDate.setText("当前显示: 所有日记");
            buttonShowAllDiaries.setVisibility(View.GONE);
        }

        if (entries != null) {
            Log.d(TAG, "Found " + entries.size() + " entries.");
            if (diaryListAdapter != null) {
                diaryListAdapter.setDiaryEntries(entries);
            } else {
                diaryListAdapter = new DiaryListAdapter(this, entries, this);
                recyclerViewDiaryEntries.setAdapter(diaryListAdapter);
            }

            // 更新标题和Toast的逻辑
            if (entries.isEmpty()) {
                String filterType = "所有";
                if (currentFilterDate != null) filterType = currentFilterDate;
                else if (currentFilterYear != -1) filterType = String.format(Locale.getDefault(), "%04d-%02d", currentFilterYear, currentFilterMonth);
                else if (currentFilterWeekStartDate != null) filterType = currentFilterWeekStartDate + "周";

                if (currentFilterDate == null && currentFilterYear == -1 && currentFilterWeekStartDate == null) {
                    textViewDiaryListTitle.setText("我的日记本 (空)");
                    Toast.makeText(this, "还没有日记，点击右下角按钮添加吧！", Toast.LENGTH_SHORT).show();
                } else {
                    textViewDiaryListTitle.setText("我的日记本 (" + filterType + " 无日记)");
                    Toast.makeText(this, "选定时间范围没有日记", Toast.LENGTH_SHORT).show();
                }
            } else {
                textViewDiaryListTitle.setText("我的日记本 (" + entries.size() + "条)");
            }
        } else {
            Log.e(TAG, "Failed to load diary entries.");
            Toast.makeText(this, "加载日记失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePickerDialogForDay() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                    clearAllFilters(); // 清除其他筛选
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDayOfMonth);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    currentFilterDate = dateFormat.format(selectedCalendar.getTime());
                    Log.d(TAG, "Date selected for day filter: " + currentFilterDate);
                    loadDiaryEntries();
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showMonthYearPickerDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_month_year_picker, null);
        final NumberPicker yearPicker = dialogView.findViewById(R.id.picker_year);
        final NumberPicker monthPicker = dialogView.findViewById(R.id.picker_month);
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        yearPicker.setMinValue(currentYear - 10);
        yearPicker.setMaxValue(currentYear + 10);
        yearPicker.setValue(currentYear);
        yearPicker.setWrapSelectorWheel(false);
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(currentMonth);
        String[] monthNames = new String[12];
        for (int i = 0; i < 12; i++) {
            monthNames[i] = String.format(Locale.getDefault(), "%d月", i + 1);
        }
        monthPicker.setDisplayedValues(monthNames);
        monthPicker.setWrapSelectorWheel(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setPositiveButton("确定", (dialog, which) -> {
            clearAllFilters(); // 清除其他筛选
            currentFilterYear = yearPicker.getValue();
            currentFilterMonth = monthPicker.getValue();
            Log.d(TAG, "Month selected for filter: " + currentFilterYear + "-" + currentFilterMonth);
            loadDiaryEntries();
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    /**
     * 【新增】显示用于选择一周中某天的DatePickerDialog，然后计算该周的起止日期
     */
    private void showDatePickerDialogForWeek() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                    clearAllFilters(); // 清除其他筛选

                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(selectedYear, selectedMonth, selectedDayOfMonth);

                    // 计算所选日期所在周的周一
                    Calendar firstDayOfWeekCal = (Calendar) selectedCal.clone();
                    firstDayOfWeekCal.setFirstDayOfWeek(Calendar.MONDAY); // 设置一周从周一开始
                    firstDayOfWeekCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

                    // 计算所选日期所在周的周日
                    Calendar lastDayOfWeekCal = (Calendar) firstDayOfWeekCal.clone();
                    lastDayOfWeekCal.add(Calendar.DAY_OF_YEAR, 6);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    currentFilterWeekStartDate = dateFormat.format(firstDayOfWeekCal.getTime());
                    currentFilterWeekEndDate = dateFormat.format(lastDayOfWeekCal.getTime());

                    Log.d(TAG, "Week selected: StartDate=" + currentFilterWeekStartDate + ", EndDate=" + currentFilterWeekEndDate);
                    loadDiaryEntries();
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.setTitle("选择任意一天以确定周");
        datePickerDialog.show();
    }

    /**
     * 【新增】辅助方法，用于在设置新筛选前清除所有旧的筛选标记
     */
    private void clearAllFilters(){
        currentFilterDate = null;
        currentFilterYear = -1;
        currentFilterMonth = -1;
        currentFilterWeekStartDate = null;
        currentFilterWeekEndDate = null;
    }


    @Override
    public void onItemClick(DiaryEntry diaryEntry) {
        Log.d(TAG, "Clicked diary: " + diaryEntry.getTitle() + " (ID: " + diaryEntry.getId() + ")");
        Intent intent = new Intent(DiaryListActivity.this, ViewEditDiaryActivity.class);
        intent.putExtra(ViewEditDiaryActivity.EXTRA_DIARY_ID, diaryEntry.getId());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}