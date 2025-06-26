package com.example.myweatherapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements CitySearchAdapter.OnCityItemClickListener {

    // 视图变量
    private TextView textViewCityName, textViewTemperature, textViewWeatherCondition;
    private ImageView imageViewWeatherIcon, imageViewChangeCity;
    private RecyclerView recyclerViewDailyForecast, recyclerViewHourlyForecast;
    private FloatingActionButton buttonOpenMusicPlayer, buttonOpenAddDiary;
    private View main;

    // 适配器
    private DailyForecastAdapter dailyForecastAdapter;
    private HourlyForecastAdapter hourlyForecastAdapter;
    private CitySearchAdapter citySearchAdapter;

    // 数据列表
    private List<DailyForecastItem> dailyForecastsList;
    private List<HourlyForecastItem> hourlyForecastsList;
    private List<SearchedCity> searchedCityList;

    // 其他
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private String currentCityId = "101010100"; // 默认北京
    private String lastKnownIconCode = ""; // 【新】用于记住最后一次的天气代码
    private final String apiKey = "b4ad6b0783f54ad3add4a9c1e18f4a49";
    private final String jwtKeyId = "C95D39V2WU";
    private final String jwtProjectId = "3MTGVUE995";
    private static final String YOUR_ED25519_PRIVATE_KEY_PEM = "-----BEGIN PRIVATE KEY-----\n" +
            "MC4CAQAwBQYDK2VwBCIEICgW715RyseiVHjRNWsuxG3rG53KVH9Gqoh+pIiH92LI\n" +
            "-----END PRIVATE KEY-----\n";
    private static final String TAG_NOW = "MainActivityWeather";
    private static final String TAG_DAILY = "DailyForecast";
    private static final String TAG_HOURLY = "HourlyForecast";
    private static final String TAG_LOCATION = "Location";
    private static final String TAG_MUSIC = "MusicPermission";
    private static final String TAG_DIARY = "DiaryFeature";
    private static final String TAG_CITY_SEARCH = "CitySearch";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int READ_MEDIA_AUDIO_PERMISSION_REQUEST_CODE = 1002;
    private AlertDialog searchCityDialogInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // 【重要】应用主题的调用必须在setContentView之前
        SettingsActivity.applyUserThemePreference(this);

        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        NotificationHelper.createNotificationChannel(this);
        initViews();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkAndRequestLocationPermissions();
    }

    // 【新】添加 onResume 方法以在返回此界面时刷新背景
    @Override
    protected void onResume() {
        super.onResume();
        // 当从设置页返回时，这个方法会被调用
        // 我们根据最后一次获取到的天气代码，重新检查并设置背景
        if (main != null) { // 确保视图已初始化
            updateWeatherBackground(lastKnownIconCode);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        main = findViewById(R.id.main);
        textViewCityName = findViewById(R.id.textViewCityName);
        imageViewWeatherIcon = findViewById(R.id.imageViewWeatherIcon);
        textViewTemperature = findViewById(R.id.textViewTemperature);
        textViewWeatherCondition = findViewById(R.id.textViewWeatherCondition);
        imageViewChangeCity = findViewById(R.id.imageViewChangeCity);

        dailyForecastsList = new ArrayList<>();
        recyclerViewDailyForecast = findViewById(R.id.recyclerViewDailyForecast);
        recyclerViewDailyForecast.setLayoutManager(new LinearLayoutManager(this));
        dailyForecastAdapter = new DailyForecastAdapter(this, dailyForecastsList);
        recyclerViewDailyForecast.setAdapter(dailyForecastAdapter);

        hourlyForecastsList = new ArrayList<>();
        recyclerViewHourlyForecast = findViewById(R.id.recyclerViewHourlyForecast);
        recyclerViewHourlyForecast.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        hourlyForecastAdapter = new HourlyForecastAdapter(this, hourlyForecastsList);
        recyclerViewHourlyForecast.setAdapter(hourlyForecastAdapter);

        imageViewChangeCity.setOnClickListener(v -> showSearchCityDialog());

        buttonOpenMusicPlayer = findViewById(R.id.buttonOpenMusicPlayer);
        buttonOpenMusicPlayer.setOnClickListener(v -> checkAndRequestMediaPermissions());

        buttonOpenAddDiary = findViewById(R.id.buttonOpenAddDiary);
        buttonOpenAddDiary.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DiaryListActivity.class);
            startActivity(intent);
        });
    }

    private void showSearchCityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_search_city, null);
        builder.setView(dialogView);

        EditText editTextSearchCityName = dialogView.findViewById(R.id.editTextSearchCityName);
        Button buttonPerformSearch = dialogView.findViewById(R.id.buttonPerformCitySearch);
        RecyclerView recyclerViewResults = dialogView.findViewById(R.id.recyclerViewCitySearchResults);

        recyclerViewResults.setLayoutManager(new LinearLayoutManager(this));
        if (searchedCityList == null) {
            searchedCityList = new ArrayList<>();
        } else {
            searchedCityList.clear();
        }
        citySearchAdapter = new CitySearchAdapter(this, searchedCityList, this);
        recyclerViewResults.setAdapter(citySearchAdapter);

        searchCityDialogInstance = builder.create();
        searchCityDialogInstance.show();

        buttonPerformSearch.setOnClickListener(v -> {
            String keyword = editTextSearchCityName.getText().toString().trim();
            if (TextUtils.isEmpty(keyword)) {
                Toast.makeText(MainActivity.this, "请输入城市名称", Toast.LENGTH_SHORT).show();
                return;
            }
            fetchCitiesByKeyword(keyword);
        });
    }

    private void fetchCitiesByKeyword(String keyword) {
        String searchApiUrl = "https://pr6r6wtxvc.re.qweatherapi.com/geo/v2/city/lookup?location=" + Uri.encode(keyword) + "&range=cn&number=10";
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, searchApiUrl,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if ("200".equals(jsonObject.getString("code"))) {
                            JSONArray locationArray = jsonObject.getJSONArray("location");
                            searchedCityList.clear();
                            if (locationArray.length() > 0) {
                                for (int i = 0; i < locationArray.length(); i++) {
                                    JSONObject cityObject = locationArray.getJSONObject(i);
                                    searchedCityList.add(new SearchedCity(
                                            cityObject.getString("id"),
                                            cityObject.getString("name"),
                                            cityObject.optString("adm1", ""),
                                            cityObject.optString("adm2", ""),
                                            cityObject.optString("country", "")
                                    ));
                                }
                                citySearchAdapter.updateCityList(searchedCityList);
                            } else {
                                Toast.makeText(MainActivity.this, "未找到匹配的城市", Toast.LENGTH_SHORT).show();
                                citySearchAdapter.updateCityList(new ArrayList<>());
                            }
                        } else {
                            handleApiError(jsonObject, jsonObject.getString("code"), TAG_CITY_SEARCH, "城市搜索API错误");
                        }
                    } catch (JSONException e) {
                        handleJsonException(e, TAG_CITY_SEARCH, "解析城市数据失败");
                    }
                },
                error -> {
                    handleVolleyError(error, "网络请求城市搜索失败");
                    if (citySearchAdapter != null) {
                        citySearchAdapter.updateCityList(new ArrayList<>());
                    }
                }) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String parsed;
                try {
                    parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    parsed = new String(response.data);
                }
                return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
            }
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String jwtToken = generateJwtToken();
                if (jwtToken != null) {
                    headers.put("Authorization", "Bearer " + jwtToken);
                }
                return headers;
            }
        };
        queue.add(stringRequest);
    }

    @Override
    public void onCityClick(SearchedCity city) {
        if (searchCityDialogInstance != null && searchCityDialogInstance.isShowing()) {
            searchCityDialogInstance.dismiss();
        }
        updateWeatherForCity(city.getId(), city.getName());
    }

    private void checkAndRequestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getDeviceLocation();
        }
    }

    private void checkAndRequestMediaPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        String audioPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, audioPermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(audioPermission);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    READ_MEDIA_AUDIO_PERMISSION_REQUEST_CODE);
        } else {
            proceedToMusicPlayerFeatures();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getDeviceLocation();
            } else {
                Toast.makeText(this, "定位权限被拒绝，将加载默认城市天气。", Toast.LENGTH_LONG).show();
                updateWeatherForCity(currentCityId, "北京");
            }
        } else if (requestCode == READ_MEDIA_AUDIO_PERMISSION_REQUEST_CODE) {
            String audioPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                    Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;
            boolean audioPermissionGranted = false;
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(audioPermission) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    audioPermissionGranted = true;
                    break;
                }
            }
            if (audioPermissionGranted) {
                proceedToMusicPlayerFeatures();
            } else {
                Toast.makeText(this, "读取音频权限被拒绝，无法播放本地音乐。", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void proceedToMusicPlayerFeatures() {
        Intent intent = new Intent(MainActivity.this, MusicPlayerActivity.class);
        startActivity(intent);
    }

    private void getDeviceLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdates(1)
                .build();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    fetchCityInfoFromCoords(location);
                } else {
                    Toast.makeText(MainActivity.this, "无法获取当前位置，将加载默认城市天气。", Toast.LENGTH_LONG).show();
                    updateWeatherForCity(currentCityId, "北京");
                }
                stopLocationUpdates();
            }
        };
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                .addOnFailureListener(this, e -> {
                    Toast.makeText(this, "请求位置更新失败。", Toast.LENGTH_LONG).show();
                    updateWeatherForCity(currentCityId, "北京");
                });
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private String generateJwtToken() {
        try {
            String privateKeyPemContent = YOUR_ED25519_PRIVATE_KEY_PEM
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] privateKeyBytes = Base64.decode(privateKeyPemContent, Base64.DEFAULT);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            PrivateKey privateKey = new EdDSAPrivateKey(keySpec);
            String headerJson = String.format("{\"alg\": \"EdDSA\", \"kid\": \"%s\"}", jwtKeyId);
            long currentTimeSeconds = System.currentTimeMillis() / 1000;
            long iat = currentTimeSeconds - 30;
            long exp = iat + 900;
            String payloadJson = String.format("{\"sub\": \"%s\", \"iat\": %d, \"exp\": %d}", jwtProjectId, iat, exp);
            String headerEncoded = Base64.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8), Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            String payloadEncoded = Base64.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8), Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            String dataToSign = headerEncoded + "." + payloadEncoded;
            EdDSAParameterSpec edDsaSpec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
            Signature signer = new EdDSAEngine(MessageDigest.getInstance(edDsaSpec.getHashAlgorithm()));
            signer.initSign(privateKey);
            signer.update(dataToSign.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signer.sign();
            String signatureEncoded = Base64.encodeToString(signatureBytes, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            return dataToSign + "." + signatureEncoded;
        } catch (Exception e) {
            Log.e(TAG_LOCATION, "生成JWT失败", e);
            return null;
        }
    }

    private void fetchWeatherData() {
        String weatherApiUrl = "https://pr6r6wtxvc.re.qweatherapi.com/v7/weather/now?location=" + currentCityId + "&key=" + apiKey + "&unit=m";
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, weatherApiUrl,
                this::parseCurrentWeatherJsonAndUpdateUi,
                error -> {
                    handleVolleyError(error, "获取当天天气数据失败");
                    textViewWeatherCondition.setText("当天天气加载失败");
                }) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String parsed;
                try {
                    parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    parsed = new String(response.data);
                }
                return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
            }
        };
        queue.add(stringRequest);
    }

    private void fetchDailyForecastData() {
        String dailyWeatherApiUrl = "https://pr6r6wtxvc.re.qweatherapi.com/v7/weather/10d?location=" + currentCityId + "&key=" + apiKey + "&unit=m";
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, dailyWeatherApiUrl,
                this::parseDailyJsonResponse,
                error -> handleVolleyError(error, "获取未来天气数据失败")) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String parsed;
                try {
                    parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    parsed = new String(response.data);
                }
                return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
            }
        };
        queue.add(stringRequest);
    }

    private void fetchHourlyForecastData() {
        if (currentCityId == null || currentCityId.isEmpty()) return;
        String hourlyApiUrl = "https://pr6r6wtxvc.re.qweatherapi.com/v7/weather/24h?location=" + currentCityId;
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, hourlyApiUrl,
                this::parseHourlyJsonResponse,
                error -> handleVolleyError(error, "获取逐小时预报失败")) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String parsed;
                try {
                    parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    parsed = new String(response.data);
                }
                return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
            }
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String jwtToken = generateJwtToken();
                if (jwtToken != null) {
                    headers.put("Authorization", "Bearer " + jwtToken);
                }
                return headers;
            }
        };
        queue.add(stringRequest);
    }

    private void fetchCityInfoFromCoords(Location location) {
        String lon = String.format(Locale.US, "%.2f", location.getLongitude());
        String lat = String.format(Locale.US, "%.2f", location.getLatitude());
        String locationString = lon + "," + lat;
        final String geoApiUrl = "https://pr6r6wtxvc.re.qweatherapi.com/geo/v2/city/lookup?location=" + locationString;
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, geoApiUrl,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if ("200".equals(jsonObject.getString("code"))) {
                            JSONArray locationArray = jsonObject.getJSONArray("location");
                            if (locationArray.length() > 0) {
                                JSONObject cityObject = locationArray.getJSONObject(0);
                                String cityName = cityObject.getString("name");
                                String cityId = cityObject.getString("id");
                                updateWeatherForCity(cityId, cityName);
                            } else {
                                updateWeatherForCity(currentCityId, "北京");
                            }
                        } else {
                            updateWeatherForCity(currentCityId, "北京");
                        }
                    } catch (JSONException e) {
                        updateWeatherForCity(currentCityId, "北京");
                    }
                },
                error -> updateWeatherForCity(currentCityId, "北京")) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String parsed;
                try {
                    parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    parsed = new String(response.data);
                }
                return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
            }
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String jwtToken = generateJwtToken();
                if (jwtToken != null) {
                    headers.put("Authorization", "Bearer " + jwtToken);
                }
                return headers;
            }
        };
        queue.add(stringRequest);
    }

    private void parseCurrentWeatherJsonAndUpdateUi(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            if ("200".equals(jsonObject.getString("code"))) {
                JSONObject nowObject = jsonObject.getJSONObject("now");
                final String temp = nowObject.getString("temp");
                final String text = nowObject.getString("text");
                final String iconCode = nowObject.getString("icon");

                this.lastKnownIconCode = iconCode; // 记住天气代码

                textViewTemperature.setText(String.format("%s°C", temp));
                textViewWeatherCondition.setText(text);

                String iconResourceName = "weather_icon_" + iconCode;
                int iconResId = getResources().getIdentifier(iconResourceName, "drawable", getPackageName());
                imageViewWeatherIcon.setImageResource(iconResId != 0 ? iconResId : R.mipmap.ic_launcher);

                updateWeatherBackground(iconCode);
            } else {
                handleApiError(jsonObject, jsonObject.getString("code"), TAG_NOW, "获取当天天气数据失败");
            }
        } catch (JSONException e) {
            handleJsonException(e, TAG_NOW, "解析当天天气数据失败");
        }
    }

    private void updateWeatherBackground(String iconCode) {
        // 1. 首先检查当前系统是否处于深色模式
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;

        int backgroundResId;

        if (isDarkMode) {
            // 2. 如果是深色模式，总是使用夜晚晴天的背景，保证视觉统一
            backgroundResId = R.drawable.bg_clear_night;
        } else {
            // 3. 如果是浅色模式，才根据天气代码来显示不同背景
            if (iconCode == null || iconCode.isEmpty()) {
                backgroundResId = R.drawable.bg_default;
            } else {
                switch (iconCode) {
                    case "100": // 白天-晴
                        backgroundResId = R.drawable.bg_sunny_day;
                        break;
                    case "150": // 夜晚-晴
                        backgroundResId = R.drawable.bg_clear_night;
                        break;
                    case "101": case "151": case "152": case "153":
                        backgroundResId = R.drawable.bg_cloudy;
                        break;
                    case "104": case "154":
                        backgroundResId = R.drawable.bg_overcast;
                        break;
                    case "300": case "301": case "302": case "303": case "304": case "305":
                    case "306": case "307": case "308": case "309": case "310": case "311":
                    case "312": case "313": case "314": case "315": case "316": case "317":
                    case "318": case "350": case "351": case "399":
                    case "400": case "401": case "402": case "403": case "404": case "405":
                    case "406": case "407": case "408": case "409": case "410": case "456":
                    case "457": case "499":
                        backgroundResId = R.drawable.bg_rainy;
                        break;
                    default:
                        backgroundResId = R.drawable.bg_default;
                        break;
                }
            }
        }
        main.setBackgroundResource(backgroundResId);
    }

    private void updateWeatherForCity(String cityId, String cityNameFromParam) {
        this.currentCityId = cityId;
        textViewCityName.setText(cityNameFromParam);
        textViewTemperature.setText("--°C");
        textViewWeatherCondition.setText("天气加载中...");
        if (dailyForecastsList != null) dailyForecastsList.clear();
        if (hourlyForecastsList != null) hourlyForecastsList.clear();
        runOnUiThread(() -> {
            if (dailyForecastAdapter != null) dailyForecastAdapter.notifyDataSetChanged();
            if (hourlyForecastAdapter != null) hourlyForecastAdapter.notifyDataSetChanged();
        });
        fetchWeatherData();
        fetchDailyForecastData();
        fetchHourlyForecastData();
    }

    private void parseDailyJsonResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            if ("200".equals(jsonObject.getString("code"))) {
                JSONArray dailyArray = jsonObject.getJSONArray("daily");
                dailyForecastsList.clear();
                for (int i = 0; i < dailyArray.length(); i++) {
                    JSONObject dayObject = dailyArray.getJSONObject(i);
                    dailyForecastsList.add(new DailyForecastItem(
                            dayObject.getString("fxDate"),
                            dayObject.getString("tempMax"),
                            dayObject.getString("tempMin"),
                            dayObject.getString("iconDay"),
                            dayObject.getString("textDay"),
                            dayObject.getString("humidity"),
                            dayObject.getString("windDirDay"),
                            dayObject.getString("windScaleDay"),
                            dayObject.getString("pressure"),
                            dayObject.getString("sunrise"),
                            dayObject.getString("sunset"),
                            dayObject.getString("uvIndex")
                    ));
                }
                runOnUiThread(() -> dailyForecastAdapter.notifyDataSetChanged());
            } else {
                handleApiError(jsonObject, jsonObject.getString("code"), TAG_DAILY, "获取未来天气数据失败");
            }
        } catch (JSONException e) {
            handleJsonException(e, TAG_DAILY, "解析未来天气数据失败");
        }
    }

    private void parseHourlyJsonResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            if ("200".equals(jsonObject.getString("code"))) {
                JSONArray hourlyArray = jsonObject.getJSONArray("hourly");
                hourlyForecastsList.clear();
                for (int i = 0; i < hourlyArray.length(); i++) {
                    JSONObject hourObject = hourlyArray.getJSONObject(i);
                    hourlyForecastsList.add(new HourlyForecastItem(
                            hourObject.getString("fxTime"),
                            hourObject.getString("temp"),
                            hourObject.getString("icon"),
                            hourObject.getString("text")
                    ));
                }
                runOnUiThread(() -> hourlyForecastAdapter.notifyDataSetChanged());
            } else {
                handleApiError(jsonObject, jsonObject.getString("code"), TAG_HOURLY, "获取逐小时预报数据失败");
            }
        } catch (JSONException e) {
            handleJsonException(e, TAG_HOURLY, "解析逐小时预报数据失败");
        }
    }

    private void handleVolleyError(VolleyError error, String toastMessagePrefix) {
        Log.e("VolleyError", toastMessagePrefix, error);
        Toast.makeText(MainActivity.this, toastMessagePrefix, Toast.LENGTH_SHORT).show();
    }

    private void handleApiError(JSONObject jsonObject, String code, String logTag, String toastMessagePrefix) {
        String apiMessage = jsonObject.optString("message", "No message");
        Log.e(logTag, "API Error Code: " + code + ", Message: " + apiMessage);
        Toast.makeText(MainActivity.this, toastMessagePrefix + " (Code: " + code + ")", Toast.LENGTH_LONG).show();
    }

    private void handleJsonException(JSONException e, String logTag, String toastMessage) {
        Log.e(logTag, "JSON parsing error", e);
        Toast.makeText(MainActivity.this, toastMessage, Toast.LENGTH_LONG).show();
    }
}
