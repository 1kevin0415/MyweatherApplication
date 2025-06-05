package com.example.myweatherapplication; // 确保包名正确

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DiaryDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "DiaryDbHelper";
    private static final String DATABASE_NAME = "diary.db";
    private static final int DATABASE_VERSION = 2; // 假设版本号因为添加日程字段已是2

    public static final String TABLE_DIARY_ENTRIES = "diary_entries";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_DATE = "entry_date";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_IS_SCHEDULE = "is_schedule";
    public static final String COLUMN_SCHEDULE_TIME = "schedule_time";

    private static final String SQL_CREATE_DIARY_ENTRIES_TABLE =
            "CREATE TABLE " + TABLE_DIARY_ENTRIES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_TITLE + " TEXT," +
                    COLUMN_CONTENT + " TEXT," +
                    COLUMN_DATE + " TEXT," +
                    COLUMN_CREATED_AT + " INTEGER," +
                    COLUMN_IS_SCHEDULE + " INTEGER DEFAULT 0," +
                    COLUMN_SCHEDULE_TIME + " INTEGER DEFAULT 0);";

    public DiaryDbHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating database table: " + TABLE_DIARY_ENTRIES);
        db.execSQL(SQL_CREATE_DIARY_ENTRIES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion +
                ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DIARY_ENTRIES);
        onCreate(db);
    }

    public long addDiaryEntry(DiaryEntry entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, entry.getTitle());
        values.put(COLUMN_CONTENT, entry.getContent());
        values.put(COLUMN_DATE, entry.getDate());
        values.put(COLUMN_CREATED_AT, entry.getCreatedAt());
        values.put(COLUMN_IS_SCHEDULE, entry.isSchedule() ? 1 : 0);
        values.put(COLUMN_SCHEDULE_TIME, entry.getScheduleTimeMillis());
        long newRowId = db.insert(TABLE_DIARY_ENTRIES, null, values);
        Log.d(TAG, "New diary/schedule entry added with ID: " + newRowId);
        return newRowId;
    }

    private DiaryEntry cursorToDiaryEntry(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
        String content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT));
        String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));
        long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
        boolean isSchedule = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SCHEDULE)) == 1;
        long scheduleTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_TIME));
        return new DiaryEntry(id, title, content, date, createdAt, isSchedule, scheduleTime);
    }

    public List<DiaryEntry> getAllDiaryEntries() {
        List<DiaryEntry> diaryList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    TABLE_DIARY_ENTRIES, null, null, null, null, null,
                    COLUMN_CREATED_AT + " DESC"
            );
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    diaryList.add(cursorToDiaryEntry(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get all diary entries", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        Log.d(TAG, "Fetched " + diaryList.size() + " total diary entries.");
        return diaryList;
    }

    public DiaryEntry getDiaryEntryById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        DiaryEntry entry = null;
        try {
            cursor = db.query(
                    TABLE_DIARY_ENTRIES, null, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(id)}, null, null, null
            );
            if (cursor != null && cursor.moveToFirst()) {
                entry = cursorToDiaryEntry(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get diary entry by ID " + id, e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return entry;
    }

    public List<DiaryEntry> getDiariesForDate(String dateString) {
        List<DiaryEntry> diaryList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String selection = COLUMN_DATE + " = ?";
        String[] selectionArgs = {dateString};
        try {
            cursor = db.query(
                    TABLE_DIARY_ENTRIES, null, selection, selectionArgs,
                    null, null, COLUMN_CREATED_AT + " DESC"
            );
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    diaryList.add(cursorToDiaryEntry(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting entries for date: " + dateString, e);
        } finally {
            if (cursor != null) cursor.close();
        }
        Log.d(TAG, "Fetched " + diaryList.size() + " entries for date: " + dateString);
        return diaryList;
    }

    public List<DiaryEntry> getDiariesForMonth(int year, int month) {
        List<DiaryEntry> diaryList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String monthString = String.format(Locale.US, "%04d-%02d-%%", year, month);
        String selection = COLUMN_DATE + " LIKE ?";
        String[] selectionArgs = {monthString};
        Log.d(TAG, "Querying for month: " + monthString);
        try {
            cursor = db.query(
                    TABLE_DIARY_ENTRIES, null, selection, selectionArgs,
                    null, null, COLUMN_CREATED_AT + " DESC"
            );
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    diaryList.add(cursorToDiaryEntry(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting entries for month: " + year + "-" + month, e);
        } finally {
            if (cursor != null) cursor.close();
        }
        Log.d(TAG, "Fetched " + diaryList.size() + " entries for month: " + year + "-" + month);
        return diaryList;
    }

    public int updateDiaryEntry(DiaryEntry entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, entry.getTitle());
        values.put(COLUMN_CONTENT, entry.getContent());
        values.put(COLUMN_DATE, entry.getDate());
        values.put(COLUMN_IS_SCHEDULE, entry.isSchedule() ? 1 : 0);
        values.put(COLUMN_SCHEDULE_TIME, entry.getScheduleTimeMillis());
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(entry.getId())};
        int count = db.update(TABLE_DIARY_ENTRIES, values, selection, selectionArgs);
        Log.d(TAG, "Updated diary entry with ID: " + entry.getId() + ", rows affected: " + count);
        return count;
    }

    public boolean deleteDiaryEntry(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(id)};
        int deletedRows = db.delete(TABLE_DIARY_ENTRIES, selection, selectionArgs);
        Log.d(TAG, "Deleted diary entry with ID: " + id + ", rows affected: " + deletedRows);
        return deletedRows > 0;
    }

    /**
     * 【新增】根据指定周的开始和结束日期获取日记条目，按创建时间降序排列
     * @param startDateOfWeek 周的开始日期，格式 "yyyy-MM-dd"
     * @param endDateOfWeek 周的结束日期，格式 "yyyy-MM-dd"
     * @return 包含指定周所有DiaryEntry对象的列表
     */
    public List<DiaryEntry> getDiariesForWeek(String startDateOfWeek, String endDateOfWeek) {
        List<DiaryEntry> diaryList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        // 查询条件：COLUMN_DATE 大于等于周一开始日期 且 小于等于周日结束日期
        String selection = COLUMN_DATE + " >= ? AND " + COLUMN_DATE + " <= ?";
        String[] selectionArgs = { startDateOfWeek, endDateOfWeek };

        Log.d(TAG, "Querying for week: " + startDateOfWeek + " to " + endDateOfWeek);

        try {
            cursor = db.query(
                    TABLE_DIARY_ENTRIES,   // 表名
                    null,             // 要返回的列 (null 代表所有列)
                    selection,        // WHERE 子句的列
                    selectionArgs,    // WHERE 子句的值
                    null,             // GROUP BY 子句
                    null,             // HAVING 子句
                    COLUMN_CREATED_AT + " DESC"  // ORDER BY 子句 (按创建时间降序)
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    diaryList.add(cursorToDiaryEntry(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get diary entries for week: " + startDateOfWeek + " - " + endDateOfWeek, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.d(TAG, "Fetched " + diaryList.size() + " diary entries for week: " + startDateOfWeek + " - " + endDateOfWeek);
        return diaryList;
    }
}