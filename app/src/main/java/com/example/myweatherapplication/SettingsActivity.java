package com.example.myweatherapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {

    // 主题切换相关
    private RadioGroup radioGroupTheme;
    private RadioButton radioButtonLight, radioButtonDark, radioButtonSystemDefault;
    public static final String PREFS_NAME = "ThemePrefs";
    public static final String KEY_THEME = "SelectedTheme";

    // 【新增】账户管理相关
    private TextView textViewCurrentUsername;
    private Button buttonChangePassword, buttonSwitchAccount;
    private DiaryDbHelper dbHelper;
    private long currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyUserThemePreference(this);
        setContentView(R.layout.activity_settings);

        dbHelper = new DiaryDbHelper(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("应用设置");
        }

        // 初始化主题控件
        radioGroupTheme = findViewById(R.id.radioGroupTheme);
        radioButtonLight = findViewById(R.id.radioButtonLight);
        radioButtonDark = findViewById(R.id.radioButtonDark);
        radioButtonSystemDefault = findViewById(R.id.radioButtonSystemDefault);

        // 【新增】初始化账户控件
        textViewCurrentUsername = findViewById(R.id.textViewCurrentUsername);
        buttonChangePassword = findViewById(R.id.buttonChangePassword);
        buttonSwitchAccount = findViewById(R.id.buttonSwitchAccount);

        // 加载并应用主题设置
        loadAndApplyThemeSelection();
        // 【新增】加载并显示用户信息
        loadAndDisplayUserInfo();

        // 设置主题切换监听
        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (checkedId == R.id.radioButtonLight) {
                themeMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.radioButtonDark) {
                themeMode = AppCompatDelegate.MODE_NIGHT_YES;
            }
            saveThemePreference(themeMode);
            AppCompatDelegate.setDefaultNightMode(themeMode);
        });

        // 【新增】设置账户管理按钮监听
        buttonChangePassword.setOnClickListener(v -> {
            // 我们将在下一步创建这个 ChangePasswordActivity
            Intent intent = new Intent(SettingsActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        buttonSwitchAccount.setOnClickListener(v -> {
            // 清除登录状态
            SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
            prefs.edit().remove(LoginActivity.KEY_USER_ID).apply();

            // 跳转回登录页并清空所有上层页面
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadAndDisplayUserInfo() {
        SharedPreferences userPrefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        currentUserId = userPrefs.getLong(LoginActivity.KEY_USER_ID, -1);
        if (currentUserId != -1) {
            String username = dbHelper.getUsernameById(currentUserId);
            if (username != null) {
                textViewCurrentUsername.setText("当前用户: " + username);
            } else {
                textViewCurrentUsername.setText("无法获取用户信息");
            }
        }
    }

    // --- 主题相关方法保持不变 ---
    private void loadAndApplyThemeSelection() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedThemeMode = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (savedThemeMode == AppCompatDelegate.MODE_NIGHT_NO) {
            radioButtonLight.setChecked(true);
        } else if (savedThemeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            radioButtonDark.setChecked(true);
        } else {
            radioButtonSystemDefault.setChecked(true);
        }
    }

    private void saveThemePreference(int themeMode) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_THEME, themeMode);
        editor.apply();
    }

    public static void applyUserThemePreference(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int themeMode = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
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