package com.example.myweatherapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {

    private RadioGroup radioGroupTheme;
    private RadioButton radioButtonLight;
    private RadioButton radioButtonDark;
    private RadioButton radioButtonSystemDefault;

    public static final String PREFS_NAME = "ThemePrefs";
    public static final String KEY_THEME = "SelectedTheme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyUserThemePreference(this); // 应用主题必须在setContentView之前
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("应用设置");
        }

        radioGroupTheme = findViewById(R.id.radioGroupTheme);
        radioButtonLight = findViewById(R.id.radioButtonLight);
        radioButtonDark = findViewById(R.id.radioButtonDark);
        radioButtonSystemDefault = findViewById(R.id.radioButtonSystemDefault);

        loadAndApplyThemeSelection();

        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (checkedId == R.id.radioButtonLight) {
                themeMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.radioButtonDark) {
                themeMode = AppCompatDelegate.MODE_NIGHT_YES;
            } else if (checkedId == R.id.radioButtonSystemDefault) {
                themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }
            saveThemePreference(themeMode);
            AppCompatDelegate.setDefaultNightMode(themeMode);
            recreate(); // 重新创建当前Activity以应用新主题
        });
    }

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
}