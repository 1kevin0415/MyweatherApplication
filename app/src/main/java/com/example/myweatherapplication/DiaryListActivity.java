package com.example.myweatherapplication;

import android.app.DatePickerDialog; // 【修复】添加这个 import
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
    private Button buttonSelectWeek;

    private String currentFilterDate = null;
    private int currentFilterYear = -1;
    private int currentFilterMonth = -1;
    private String currentFilterWeekStartDate = null;
    private String currentFilterWeekEndDate = null;

    private long currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_list);

        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        currentUserId = prefs.getLong(LoginActivity.KEY_USER_ID, -1);

        if (currentUserId == -1) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        dbHelper = new DiaryDbHelper(this);
        diaryEntriesList = new ArrayList<>();

        textViewDiaryListTitle = findViewById(R.id.textViewDiaryListTitle);
        recyclerViewDiaryEntries = findViewById(R.id.recyclerViewDiaryEntries);
        fabAddDiary = findViewById(R.id.fabAddDiary);
        buttonSelectDate = findViewById(R.id.buttonSelectDate);
        textViewSelectedDate = findViewById(R.id.textViewSelectedDate);
        buttonShowAllDiaries = findViewById(R.id.buttonShowAllDiaries);
        buttonSelectMonth = findViewById(R.id.buttonSelectMonth);
        buttonSelectWeek = findViewById(R.id.buttonSelectWeek);

        recyclerViewDiaryEntries.setLayoutManager(new LinearLayoutManager(this));
        diaryListAdapter = new DiaryListAdapter(this, diaryEntriesList, this);
        recyclerViewDiaryEntries.setAdapter(diaryListAdapter);

        fabAddDiary.setOnClickListener(view -> {
            Intent intent = new Intent(DiaryListActivity.this, AddDiaryActivity.class);
            startActivity(intent);
        });

        buttonSelectDate.setOnClickListener(v -> showDatePickerDialogForDay());
        buttonSelectMonth.setOnClickListener(v -> showMonthYearPickerDialog());
        buttonSelectWeek.setOnClickListener(v -> showDatePickerDialogForWeek());

        buttonShowAllDiaries.setOnClickListener(v -> {
            clearAllFilters();
            loadDiaryEntries();
            textViewSelectedDate.setText("当前显示: 所有日记");
            buttonShowAllDiaries.setVisibility(View.GONE);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUserId != -1) {
            loadDiaryEntries();
        }
    }

    private void loadDiaryEntries() {
        List<DiaryEntry> entries;

        if (currentFilterDate != null) {
            entries = dbHelper.getDiariesForDate(currentFilterDate, currentUserId);
            textViewSelectedDate.setText("筛选日期: " + currentFilterDate);
            buttonShowAllDiaries.setVisibility(View.VISIBLE);
        } else if (currentFilterYear != -1 && currentFilterMonth != -1) {
            entries = dbHelper.getDiariesForMonth(currentFilterYear, currentFilterMonth, currentUserId);
            textViewSelectedDate.setText(String.format(Locale.getDefault(), "筛选月份: %04d-%02d", currentFilterYear, currentFilterMonth));
            buttonShowAllDiaries.setVisibility(View.VISIBLE);
        } else if (currentFilterWeekStartDate != null && currentFilterWeekEndDate != null) {
            entries = dbHelper.getDiariesForWeek(currentFilterWeekStartDate, currentFilterWeekEndDate, currentUserId);
            textViewSelectedDate.setText("筛选周: " + currentFilterWeekStartDate + " 至 " + currentFilterWeekEndDate);
            buttonShowAllDiaries.setVisibility(View.VISIBLE);
        } else {
            entries = dbHelper.getAllDiaryEntries(currentUserId);
            textViewSelectedDate.setText("当前显示: 所有日记");
            buttonShowAllDiaries.setVisibility(View.GONE);
        }

        if (entries != null) {
            Log.d(TAG, "Found " + entries.size() + " entries for user " + currentUserId);
            diaryListAdapter.setDiaryEntries(entries);

            if (entries.isEmpty()) {
                textViewDiaryListTitle.setText("我的日记本 (空)");
            } else {
                textViewDiaryListTitle.setText("我的日记本 (" + entries.size() + "条)");
            }
        } else {
            Log.e(TAG, "Failed to load diary entries for user " + currentUserId);
            Toast.makeText(this, "加载日记失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePickerDialogForDay() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                    clearAllFilters();
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDayOfMonth);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    currentFilterDate = dateFormat.format(selectedCalendar.getTime());
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
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(currentMonth);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setPositiveButton("确定", (dialog, which) -> {
            clearAllFilters();
            currentFilterYear = yearPicker.getValue();
            currentFilterMonth = monthPicker.getValue();
            loadDiaryEntries();
        });
        builder.setNegativeButton("取消", null);
        builder.create().show();
    }

    private void showDatePickerDialogForWeek() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                    clearAllFilters();
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(selectedYear, selectedMonth, selectedDayOfMonth);
                    selectedCal.setFirstDayOfWeek(Calendar.MONDAY);
                    Calendar firstDayOfWeekCal = (Calendar) selectedCal.clone();
                    firstDayOfWeekCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    Calendar lastDayOfWeekCal = (Calendar) firstDayOfWeekCal.clone();
                    lastDayOfWeekCal.add(Calendar.DAY_OF_YEAR, 6);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    currentFilterWeekStartDate = dateFormat.format(firstDayOfWeekCal.getTime());
                    currentFilterWeekEndDate = dateFormat.format(lastDayOfWeekCal.getTime());
                    loadDiaryEntries();
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.setTitle("选择任意一天以确定周");
        datePickerDialog.show();
    }

    private void clearAllFilters(){
        currentFilterDate = null;
        currentFilterYear = -1;
        currentFilterMonth = -1;
        currentFilterWeekStartDate = null;
        currentFilterWeekEndDate = null;
    }

    @Override
    public void onItemClick(DiaryEntry diaryEntry) {
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