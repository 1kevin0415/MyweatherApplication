package com.example.myweatherapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword;
    private Button buttonLogin;
    private TextView textViewGoToRegister;
    private DiaryDbHelper dbHelper;

    public static final String PREFS_NAME = "UserPrefs";
    public static final String KEY_USER_ID = "LoggedInUserId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 【核心修改】在设置布局之前，先检查是否需要自动登录
        if (checkAutoLogin()) {
            // 如果 checkAutoLogin 返回 true，说明已经跳转，直接结束当前 Activity 的创建
            return;
        }

        // 如果没有自动登录，才加载并显示登录界面
        setContentView(R.layout.activity_login);

        dbHelper = new DiaryDbHelper(this);

        editTextUsername = findViewById(R.id.editTextLoginUsername);
        editTextPassword = findViewById(R.id.editTextLoginPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewGoToRegister = findViewById(R.id.textViewGoToRegister);

        buttonLogin.setOnClickListener(v -> loginUser());

        textViewGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    /**
     * 【新增方法】检查并执行自动登录
     * @return 如果执行了自动登录，返回 true；否则返回 false。
     */
    private boolean checkAutoLogin() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long loggedInUserId = prefs.getLong(KEY_USER_ID, -1);

        // 如果保存的用户ID有效 (-1 是我们设定的无效值)
        if (loggedInUserId != -1) {
            // 直接跳转到主界面
            Toast.makeText(this, "自动登录成功", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // 结束登录页，防止用户按返回键回来
            return true; // 表示已经处理了跳转
        }

        return false; // 表示没有有效的登录信息，需要用户手动登录
    }

    private void loginUser() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        long userId = dbHelper.checkUser(username, password);

        if (userId != -1) {
            // 登录成功，保存用户ID以备下次自动登录
            saveUserId(userId);

            Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();

            // 跳转到主界面
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserId(long userId) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_USER_ID, userId);
        editor.apply();
    }
}