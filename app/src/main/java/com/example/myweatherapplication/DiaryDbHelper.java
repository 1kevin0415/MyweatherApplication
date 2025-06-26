package com.example.myweatherapplication;

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
    // 【修改】数据库版本号必须增加，因为我们改变了表结构
    private static final int DATABASE_VERSION = 3;

    // --- 日记表定义 ---
    public static final String TABLE_DIARY_ENTRIES = "diary_entries";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_DATE = "entry_date";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_IS_SCHEDULE = "is_schedule";
    public static final String COLUMN_SCHEDULE_TIME = "schedule_time";
    // 【新增】日记表中用于关联用户ID的外键列
    public static final String COLUMN_DIARY_USER_ID_FK = "user_id";

    // --- 【新增】用户表定义 ---
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "_id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";

    // --- 【修改】日记表的创建语句，增加了 user_id 列 ---
    private static final String SQL_CREATE_DIARY_ENTRIES_TABLE =
            "CREATE TABLE " + TABLE_DIARY_ENTRIES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_TITLE + " TEXT," +
                    COLUMN_CONTENT + " TEXT," +
                    COLUMN_DATE + " TEXT," +
                    COLUMN_CREATED_AT + " INTEGER," +
                    COLUMN_IS_SCHEDULE + " INTEGER DEFAULT 0," +
                    COLUMN_SCHEDULE_TIME + " INTEGER DEFAULT 0," +
                    COLUMN_DIARY_USER_ID_FK + " INTEGER);";

    // --- 【新增】用户表的创建语句 ---
    private static final String SQL_CREATE_USERS_TABLE =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_USERNAME + " TEXT UNIQUE NOT NULL," +
                    COLUMN_PASSWORD + " TEXT NOT NULL);";

    public DiaryDbHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating database tables...");
        db.execSQL(SQL_CREATE_DIARY_ENTRIES_TABLE);
        db.execSQL(SQL_CREATE_USERS_TABLE); // 同时创建用户表
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion +
                ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DIARY_ENTRIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS); // 也删除用户表
        onCreate(db);
    }

    // --- 【新增】用户管理方法 ---

    /**
     * 注册新用户
     * @param username 用户名
     * @param password 密码 (为简化，此处为明文，实际项目中应加密)
     * @return 新用户的ID，如果用户名已存在或出错则返回-1
     */
    public long addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);
        // 使用 insertWithOnConflict 确保用户名是唯一的
        return db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    /**
     * 验证用户登录
     * @param username 用户名
     * @param password 密码
     * @return 匹配用户的ID，如果不存在或密码错误则返回-1
     */
    public long checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{COLUMN_USER_ID},
                    COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ?",
                    new String[]{username, password}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return -1; // 表示登录失败
    }


    // --- 【修改】所有日记方法，都增加了 userId 参数用于数据隔离 ---

    public long addDiaryEntry(DiaryEntry entry, long userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, entry.getTitle());
        values.put(COLUMN_CONTENT, entry.getContent());
        values.put(COLUMN_DATE, entry.getDate());
        values.put(COLUMN_CREATED_AT, entry.getCreatedAt());
        values.put(COLUMN_IS_SCHEDULE, entry.isSchedule() ? 1 : 0);
        values.put(COLUMN_SCHEDULE_TIME, entry.getScheduleTimeMillis());
        values.put(COLUMN_DIARY_USER_ID_FK, userId); // 关联用户ID
        long newRowId = db.insert(TABLE_DIARY_ENTRIES, null, values);
        Log.d(TAG, "New diary/schedule entry added for user " + userId + " with ID: " + newRowId);
        return newRowId;
    }

    public List<DiaryEntry> getAllDiaryEntries(long userId) {
        List<DiaryEntry> diaryList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    TABLE_DIARY_ENTRIES, null,
                    COLUMN_DIARY_USER_ID_FK + " = ?", new String[]{String.valueOf(userId)},
                    null, null, COLUMN_CREATED_AT + " DESC"
            );
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    diaryList.add(cursorToDiaryEntry(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while getting diary entries for user " + userId, e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return diaryList;
    }

    public DiaryEntry getDiaryEntryById(long entryId, long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        DiaryEntry entry = null;
        try {
            cursor = db.query(
                    TABLE_DIARY_ENTRIES, null,
                    COLUMN_ID + " = ? AND " + COLUMN_DIARY_USER_ID_FK + " = ?",
                    new String[]{String.valueOf(entryId), String.valueOf(userId)},
                    null, null, null
            );
            if (cursor != null && cursor.moveToFirst()) {
                entry = cursorToDiaryEntry(cursor);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return entry;
    }

    public List<DiaryEntry> getDiariesForDate(String dateString, long userId) {
        List<DiaryEntry> diaryList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String selection = COLUMN_DATE + " = ? AND " + COLUMN_DIARY_USER_ID_FK + " = ?";
        String[] selectionArgs = {dateString, String.valueOf(userId)};
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
        } finally {
            if (cursor != null) cursor.close();
        }
        return diaryList;
    }

    public List<DiaryEntry> getDiariesForMonth(int year, int month, long userId) {
        List<DiaryEntry> diaryList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String monthString = String.format(Locale.US, "%04d-%02d-%%", year, month);
        String selection = COLUMN_DATE + " LIKE ? AND " + COLUMN_DIARY_USER_ID_FK + " = ?";
        String[] selectionArgs = {monthString, String.valueOf(userId)};
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
        } finally {
            if (cursor != null) cursor.close();
        }
        return diaryList;
    }

    public List<DiaryEntry> getDiariesForWeek(String startDateOfWeek, String endDateOfWeek, long userId) {
        List<DiaryEntry> diaryList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String selection = COLUMN_DATE + " >= ? AND " + COLUMN_DATE + " <= ? AND " + COLUMN_DIARY_USER_ID_FK + " = ?";
        String[] selectionArgs = { startDateOfWeek, endDateOfWeek, String.valueOf(userId) };
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
        } finally {
            if (cursor != null) cursor.close();
        }
        return diaryList;
    }

    public int updateDiaryEntry(DiaryEntry entry, long userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, entry.getTitle());
        values.put(COLUMN_CONTENT, entry.getContent());
        values.put(COLUMN_DATE, entry.getDate());
        values.put(COLUMN_IS_SCHEDULE, entry.isSchedule() ? 1 : 0);
        values.put(COLUMN_SCHEDULE_TIME, entry.getScheduleTimeMillis());

        String selection = COLUMN_ID + " = ? AND " + COLUMN_DIARY_USER_ID_FK + " = ?";
        String[] selectionArgs = {String.valueOf(entry.getId()), String.valueOf(userId)};

        return db.update(TABLE_DIARY_ENTRIES, values, selection, selectionArgs);
    }

    public boolean deleteDiaryEntry(long entryId, long userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String selection = COLUMN_ID + " = ? AND " + COLUMN_DIARY_USER_ID_FK + " = ?";
        String[] selectionArgs = {String.valueOf(entryId), String.valueOf(userId)};
        int deletedRows = db.delete(TABLE_DIARY_ENTRIES, selection, selectionArgs);
        return deletedRows > 0;
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

    // 【新增】根据用户ID获取用户名
    public String getUsernameById(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{COLUMN_USERNAME},
                    COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return null; // 未找到用户
    }

    // 【新增】根据用户ID更新密码
    public int updatePassword(long userId, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PASSWORD, newPassword); // 实际项目中应加密

        return db.update(TABLE_USERS, values, COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
    }
}