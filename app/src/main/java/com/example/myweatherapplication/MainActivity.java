package com.example.myweatherapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu; // 【确保导入】
import android.view.MenuItem; // 【确保导入】
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
import com.google.android.material.appbar.MaterialToolbar; // 【确保导入】


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

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;

public class MainActivity extends AppCompatActivity implements CitySearchAdapter.OnCityItemClickListener {

    private TextView textViewCityName;
    private ImageView imageViewWeatherIcon;
    private TextView textViewTemperature;
    private TextView textViewWeatherCondition;
    private TextView textViewMoreDetails;
    private RecyclerView recyclerViewDailyForecast;
    private DailyForecastAdapter dailyForecastAdapter;
    private ImageView imageViewChangeCity;
    private RecyclerView recyclerViewHourlyForecast;
    private HourlyForecastAdapter hourlyForecastAdapter;
    private Button buttonOpenMusicPlayer;
    private Button buttonOpenAddDiary;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private String currentCityId = "101010100";
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
    private List<DailyForecastItem> dailyForecastsList;
    private List<HourlyForecastItem> hourlyForecastsList;
    private CitySearchAdapter citySearchAdapter;
    private List<SearchedCity> searchedCityList;
    private AlertDialog searchCityDialogInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsActivity.applyUserThemePreference(this);
        setContentView(R.layout.activity_main);

        // 【设置 Toolbar】
        MaterialToolbar toolbar = findViewById(R.id.topAppBar); // 确保ID与activity_main.xml中一致
        setSupportActionBar(toolbar);
        // 设置Toolbar标题为空，以便只显示菜单图标（如果showAsAction允许）
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(""); // 或者可以设置 getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // 让根视图的顶部padding为0，因为AppBarLayout会处理状态栏高度
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return windowInsets;
        });

        NotificationHelper.createNotificationChannel(this);
        initViews();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkAndRequestLocationPermissions();
    }

    // 【确保这两个菜单方法存在且正确】
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        Log.d("MENU_DEBUG", "onCreateOptionsMenu in MainActivity CALLED");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Log.d(TAG_DIARY, "设置菜单项被点击，启动SettingsActivity...");
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void initViews() {
        textViewCityName = findViewById(R.id.textViewCityName);
        imageViewWeatherIcon = findViewById(R.id.imageViewWeatherIcon);
        textViewTemperature = findViewById(R.id.textViewTemperature);
        textViewWeatherCondition = findViewById(R.id.textViewWeatherCondition);
        textViewMoreDetails = findViewById(R.id.textViewMoreDetails);
        imageViewChangeCity = findViewById(R.id.imageViewChangeCity);

        dailyForecastsList = new ArrayList<>();
        recyclerViewDailyForecast = findViewById(R.id.recyclerViewDailyForecast);
        if (recyclerViewDailyForecast != null) {
            recyclerViewDailyForecast.setLayoutManager(new LinearLayoutManager(this));
            dailyForecastAdapter = new DailyForecastAdapter(this, dailyForecastsList);
            recyclerViewDailyForecast.setAdapter(dailyForecastAdapter);
        } else { Log.e(TAG_DAILY, "recyclerViewDailyForecast not found!"); }

        hourlyForecastsList = new ArrayList<>();
        recyclerViewHourlyForecast = findViewById(R.id.recyclerViewHourlyForecast);
        hourlyForecastAdapter = new HourlyForecastAdapter(this, hourlyForecastsList);
        if (recyclerViewHourlyForecast != null) {
            recyclerViewHourlyForecast.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            recyclerViewHourlyForecast.setAdapter(hourlyForecastAdapter);
        } else { Log.e(TAG_HOURLY, "recyclerViewHourlyForecast not found!"); }

        imageViewChangeCity.setOnClickListener(v -> showSearchCityDialog());

        buttonOpenMusicPlayer = findViewById(R.id.buttonOpenMusicPlayer);
        if (buttonOpenMusicPlayer != null) {
            buttonOpenMusicPlayer.setOnClickListener(v -> {
                Log.d(TAG_MUSIC, "播放音乐按钮被点击，检查音频和通知权限...");
                checkAndRequestMediaPermissions();
            });
        } else { Log.e("MainActivity", "buttonOpenMusicPlayer not found!");}

        buttonOpenAddDiary = findViewById(R.id.buttonOpenAddDiary);
        if (buttonOpenAddDiary != null) {
            buttonOpenAddDiary.setOnClickListener(v -> {
                Log.d(TAG_DIARY, "写日记按钮被点击，启动DiaryListActivity...");
                Intent intent = new Intent(MainActivity.this, DiaryListActivity.class);
                startActivity(intent);
            });
        } else { Log.e("MainActivity", "buttonOpenAddDiary not found!");}
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
        Log.d(TAG_CITY_SEARCH, "Requesting City Search API: " + searchApiUrl);
        Toast.makeText(this, "正在搜索: " + keyword, Toast.LENGTH_SHORT).show();
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, searchApiUrl,
                response -> {
                    Log.d(TAG_CITY_SEARCH, "City Search API Response: " + response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if ("200".equals(jsonObject.getString("code"))) {
                            JSONArray locationArray = jsonObject.getJSONArray("location");
                            searchedCityList.clear();
                            if (locationArray.length() > 0) {
                                for (int i = 0; i < locationArray.length(); i++) {
                                    JSONObject cityObject = locationArray.getJSONObject(i);
                                    String id = cityObject.getString("id");
                                    String name = cityObject.getString("name");
                                    String adm1 = cityObject.optString("adm1", "");
                                    String adm2 = cityObject.optString("adm2", "");
                                    String country = cityObject.optString("country", "");
                                    searchedCityList.add(new SearchedCity(id, name, adm1, adm2, country));
                                }
                                citySearchAdapter.updateCityList(searchedCityList);
                            } else {
                                Toast.makeText(MainActivity.this, "未找到匹配的城市", Toast.LENGTH_SHORT).show();
                                citySearchAdapter.updateCityList(new ArrayList<>());
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "城市搜索API错误: " + jsonObject.optString("code", "Unknown error"), Toast.LENGTH_LONG).show();
                            Log.e(TAG_CITY_SEARCH, "City Search API Error Code: " + jsonObject.optString("code", "Unknown"));
                            citySearchAdapter.updateCityList(new ArrayList<>());
                        }
                    } catch (JSONException e) {
                        Log.e(TAG_CITY_SEARCH, "解析城市搜索JSON失败", e);
                        Toast.makeText(MainActivity.this, "解析城市数据失败", Toast.LENGTH_LONG).show();
                        citySearchAdapter.updateCityList(new ArrayList<>());
                    }
                },
                error -> {
                    Log.e(TAG_CITY_SEARCH, "请求城市搜索API失败: ", error);
                    if (error.networkResponse != null) {
                        Log.e(TAG_CITY_SEARCH, "Status Code: " + error.networkResponse.statusCode);
                        try {
                            String body = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                            Log.e(TAG_CITY_SEARCH, "Error Body: " + body);
                        } catch (Exception e) { /* ignore */ }
                    }
                    Toast.makeText(MainActivity.this, "网络请求城市搜索失败", Toast.LENGTH_LONG).show();
                    citySearchAdapter.updateCityList(new ArrayList<>());
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String jwtToken = generateJwtToken();
                if (jwtToken != null) {
                    headers.put("Authorization", "Bearer " + jwtToken);
                } else {
                    Log.e(TAG_CITY_SEARCH, "JWT Token is null for City Search API.");
                }
                return headers;
            }
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                return MainActivity.parseCharsetNetworkResponse(response, TAG_CITY_SEARCH);
            }
        };
        queue.add(stringRequest);
    }

    @Override
    public void onCityClick(SearchedCity city) {
        Log.d(TAG_CITY_SEARCH, "Selected city: " + city.getName() + " (ID: " + city.getId() + ")");
        if (searchCityDialogInstance != null && searchCityDialogInstance.isShowing()) {
            searchCityDialogInstance.dismiss();
        }
        textViewCityName.setText(city.getName());
        updateWeatherForCity(city.getId(), city.getName());
    }

    private void checkAndRequestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG_LOCATION, "定位权限已被授予，获取位置。");
            getDeviceLocation();
        }
    }

    private void checkAndRequestMediaPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        String audioPermission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            audioPermission = Manifest.permission.READ_MEDIA_AUDIO;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
                Log.d(TAG_MUSIC, "准备请求通知权限 (for Music Player)");
            }
        } else {
            audioPermission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        if (ContextCompat.checkSelfPermission(this, audioPermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(audioPermission);
            Log.d(TAG_MUSIC, "准备请求音频权限: " + audioPermission);
        }
        if (!permissionsToRequest.isEmpty()) {
            Log.d(TAG_MUSIC, "请求以下权限: " + permissionsToRequest.toString());
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    READ_MEDIA_AUDIO_PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG_MUSIC, "音频和所需通知权限均已被授予 (for Music Player)。");
            proceedToMusicPlayerFeatures();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG_LOCATION, "用户已授予定位权限。");
                getDeviceLocation();
            } else {
                Log.d(TAG_LOCATION, "用户已拒绝定位权限。");
                Toast.makeText(this, "定位权限被拒绝，将加载默认城市天气。", Toast.LENGTH_LONG).show();
                updateWeatherForCity(currentCityId, "北京");
            }
        }
        else if (requestCode == READ_MEDIA_AUDIO_PERMISSION_REQUEST_CODE) {
            boolean audioPermissionGranted = false;
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults.length > i ? grantResults[i] : PackageManager.PERMISSION_DENIED;
                if (permission.equals(Manifest.permission.READ_MEDIA_AUDIO) || permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        audioPermissionGranted = true;
                        Log.d(TAG_MUSIC, "用户已授予音频读取权限。");
                    } else {
                        Log.d(TAG_MUSIC, "用户已拒绝音频读取权限。");
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && permission.equals(Manifest.permission.POST_NOTIFICATIONS)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG_MUSIC, "用户已授予通知权限 (for Music Player)。");
                    } else {
                        Log.d(TAG_MUSIC, "用户已拒绝通知权限 (for Music Player)。");
                        Toast.makeText(this, "通知权限被拒绝，音乐播放通知可能无法显示。", Toast.LENGTH_LONG).show();
                    }
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
        Log.d(TAG_MUSIC, "音频权限已获取，准备启动音乐播放器...");
        Toast.makeText(this, "音频权限已获取！正在启动播放器...", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, MusicPlayerActivity.class);
        startActivity(intent);
    }

    private void getDeviceLocation() {
        Log.d(TAG_LOCATION, "准备请求实时位置更新...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG_LOCATION, "在getDeviceLocation中权限检查再次未通过。");
            updateWeatherForCity(currentCityId, "北京");
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
                    Log.d(TAG_LOCATION, "成功获取到实时位置：Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                    fetchCityInfoFromCoords(location);
                    stopLocationUpdates();
                } else {
                    Log.w(TAG_LOCATION, "实时位置更新返回的location为null。");
                    Toast.makeText(MainActivity.this, "无法获取当前位置，将加载默认城市天气。", Toast.LENGTH_LONG).show();
                    updateWeatherForCity(currentCityId, "北京");
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                .addOnFailureListener(this, e -> {
                    Log.e(TAG_LOCATION, "请求实时位置更新失败: ", e);
                    Toast.makeText(this, "请求位置更新失败，将加载默认城市天气。", Toast.LENGTH_LONG).show();
                    updateWeatherForCity(currentCityId, "北京");
                });
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG_LOCATION, "已停止位置更新。");
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
            String jwt = dataToSign + "." + signatureEncoded;
            Log.d(TAG_LOCATION, "Generated JWT successfully (showing first 20 chars): " + (jwt.length() > 20 ? jwt.substring(0, 20) : jwt) + "...");
            return jwt;
        } catch (Exception e) {
            Log.e(TAG_LOCATION, "生成JWT失败", e);
            Toast.makeText(this, "生成认证令牌失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private static Response<String> parseCharsetNetworkResponse(NetworkResponse response, String logTag) {
        String parsed;
        parsed = new String(response.data, StandardCharsets.UTF_8);
        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
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
                                Log.d(TAG_LOCATION, "GeoAPI success (Coords): City=" + cityName + ", ID=" + cityId);
                                textViewCityName.setText(cityName);
                                updateWeatherForCity(cityId, cityName);
                            } else { Log.e(TAG_LOCATION, "GeoAPI (Coords)返回的location数组为空"); Toast.makeText(this,"无法识别城市信息(Coords)",Toast.LENGTH_LONG).show(); updateWeatherForCity(currentCityId,"北京");}
                        } else { Log.e(TAG_LOCATION, "GeoAPI (Coords)返回错误码: "+jsonObject.getString("code")+", Msg: "+jsonObject.optString("message")); Toast.makeText(this,"查询城市信息失败(Coords)",Toast.LENGTH_LONG).show(); updateWeatherForCity(currentCityId,"北京");}
                    } catch (JSONException e) { Log.e(TAG_LOCATION,"解析GeoAPI (Coords)失败",e); Toast.makeText(this,"解析城市数据失败(Coords)",Toast.LENGTH_LONG).show(); updateWeatherForCity(currentCityId,"北京");}
                },
                error -> {
                    Log.e(TAG_LOCATION, "请求GeoAPI (Coords)失败: ", error);
                    if (error.networkResponse != null) { Log.e(TAG_LOCATION, "GeoAPI (Coords) Error Status Code: " + error.networkResponse.statusCode); }
                    Toast.makeText(this, "网络请求城市信息失败(Coords)，加载默认城市。", Toast.LENGTH_LONG).show();
                    updateWeatherForCity(currentCityId, "北京");
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String jwtToken = generateJwtToken();
                if (jwtToken != null) {
                    headers.put("Authorization", "Bearer " + jwtToken);
                } else {
                    Log.e(TAG_LOCATION, "JWT Token is null for GeoAPI (Coords).");
                }
                return headers;
            }
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                return MainActivity.parseCharsetNetworkResponse(response, TAG_LOCATION + "_Coords");
            }
        };
        queue.add(stringRequest);
    }

    private void updateWeatherForCity(String cityId, String cityNameFromParam) {
        this.currentCityId = cityId;
        textViewCityName.setText(cityNameFromParam);
        textViewTemperature.setText("--°C");
        textViewWeatherCondition.setText("天气加载中...");
        if (dailyForecastsList != null) { dailyForecastsList.clear(); }
        if (hourlyForecastsList != null) { hourlyForecastsList.clear(); }
        runOnUiThread(() -> {
            if (dailyForecastAdapter != null) { dailyForecastAdapter.notifyDataSetChanged(); }
            if (hourlyForecastAdapter != null) { hourlyForecastAdapter.notifyDataSetChanged(); }
        });
        fetchWeatherData();
        fetchDailyForecastData();
        fetchHourlyForecastData();
    }

    private void fetchWeatherData() {
        String weatherApiUrl = "https://pr6r6wtxvc.re.qweatherapi.com/v7/weather/now?location=" + currentCityId + "&key=" + apiKey + "&unit=m";
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, weatherApiUrl,
                this::parseCurrentWeatherJsonAndUpdateUi,
                error -> {
                    Log.e(TAG_NOW, "Volley Error (Current Weather): " + error.toString());
                    handleVolleyError(error, "获取当天天气数据失败");
                    textViewWeatherCondition.setText("当天天气加载失败");
                }) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                return MainActivity.parseCharsetNetworkResponse(response, TAG_NOW);
            }
        };
        queue.add(stringRequest);
    }

    private void fetchDailyForecastData() {
        String dailyWeatherApiUrl = "https://pr6r6wtxvc.re.qweatherapi.com/v7/weather/3d?location=" + currentCityId + "&key=" + apiKey + "&unit=m";
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, dailyWeatherApiUrl,
                this::parseDailyJsonResponse,
                error -> {
                    Log.e(TAG_DAILY, "Volley Error (Daily Forecast): " + error.toString());
                    handleVolleyError(error, "获取未来天气数据失败");
                }) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                return MainActivity.parseCharsetNetworkResponse(response, TAG_DAILY);
            }
        };
        queue.add(stringRequest);
    }

    private void fetchHourlyForecastData() {
        if (currentCityId == null || currentCityId.isEmpty()) { Log.e(TAG_HOURLY, "currentCityId 为空，无法获取逐小时预报。"); return; }
        String hourlyApiUrl = "https://pr6r6wtxvc.re.qweatherapi.com/v7/weather/24h?location=" + currentCityId;
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, hourlyApiUrl,
                this::parseHourlyJsonResponse,
                error -> {
                    Log.e(TAG_HOURLY, "Volley Error (Hourly Forecast): ", error);
                    if (error.networkResponse != null) { Log.e(TAG_HOURLY, "Hourly API Error Status Code: " + error.networkResponse.statusCode); }
                    Toast.makeText(this, "获取逐小时预报失败", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String jwtToken = generateJwtToken();
                if (jwtToken != null) {
                    headers.put("Authorization", "Bearer " + jwtToken);
                } else { Log.e(TAG_HOURLY, "JWT Token is null for Hourly API."); }
                return headers;
            }
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                return MainActivity.parseCharsetNetworkResponse(response, TAG_HOURLY);
            }
        };
        queue.add(stringRequest);
    }

    private void parseCurrentWeatherJsonAndUpdateUi(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            String code = jsonObject.getString("code");
            if ("200".equals(code)) {
                JSONObject nowObject = jsonObject.getJSONObject("now");
                final String temp = nowObject.getString("temp");
                final String text = nowObject.getString("text");
                final String iconCode = nowObject.getString("icon");
                Log.d(TAG_NOW,"Current Weather data: temp="+temp+", text="+text+", iconCode="+iconCode);
                textViewTemperature.setText(temp+"°C");
                textViewWeatherCondition.setText(text);
                String iconResourceName="weather_icon_"+iconCode;
                int iconResId=getResources().getIdentifier(iconResourceName,"drawable",getPackageName());
                if(iconResId!=0){imageViewWeatherIcon.setImageResource(iconResId);}
                else{Log.w(TAG_NOW,"Weather icon not found for code: "+iconCode+" (tried: "+iconResourceName+")");imageViewWeatherIcon.setImageResource(R.mipmap.ic_launcher);}
                textViewMoreDetails.setText("温度: "+temp+"°C, 天气: "+text+", 图标代码: "+iconCode);
            } else {
                handleApiError(jsonObject,code,TAG_NOW,"获取当天天气数据失败");
            }
        } catch(JSONException e) {
            handleJsonException(e,TAG_NOW,"解析当天天气数据失败");
        }
    }
    private void parseDailyJsonResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            String code = jsonObject.getString("code");
            if ("200".equals(code)) {
                JSONArray dailyArray = jsonObject.getJSONArray("daily");
                dailyForecastsList.clear();
                for (int i = 0; i < dailyArray.length(); i++) {
                    JSONObject dayObject = dailyArray.getJSONObject(i);
                    String fxDate = dayObject.getString("fxDate");
                    String tempMax = dayObject.getString("tempMax");
                    String tempMin = dayObject.getString("tempMin");
                    String iconDay = dayObject.getString("iconDay");
                    String textDay = dayObject.getString("textDay");
                    String humidity = dayObject.getString("humidity");
                    String windDirDay = dayObject.getString("windDirDay");
                    String windScaleDay = dayObject.getString("windScaleDay");
                    String pressure = dayObject.getString("pressure");
                    String sunrise = dayObject.getString("sunrise");
                    String sunset = dayObject.getString("sunset");
                    String uvIndex = dayObject.getString("uvIndex");
                    DailyForecastItem item = new DailyForecastItem(fxDate,tempMax,tempMin,iconDay,textDay,humidity,windDirDay,windScaleDay,pressure,sunrise,sunset,uvIndex);
                    dailyForecastsList.add(item);
                }
                Log.d(TAG_DAILY,"Successfully parsed "+dailyForecastsList.size()+" full daily forecast items.");
                runOnUiThread(() -> { if (dailyForecastAdapter != null) { dailyForecastAdapter.notifyDataSetChanged(); }});
            } else {
                handleApiError(jsonObject,code,TAG_DAILY,"获取未来天气数据失败");
            }
        } catch(JSONException e) {
            handleJsonException(e,TAG_DAILY,"解析未来天气数据失败");
        }
    }
    private void parseHourlyJsonResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            String code = jsonObject.getString("code");
            if ("200".equals(code)) {
                JSONArray hourlyArray = jsonObject.getJSONArray("hourly");
                if (hourlyForecastsList == null) { hourlyForecastsList = new ArrayList<>(); }
                hourlyForecastsList.clear();
                for (int i = 0; i < hourlyArray.length(); i++) {
                    JSONObject hourObject = hourlyArray.getJSONObject(i);
                    String fxTime = hourObject.getString("fxTime");
                    String temp = hourObject.getString("temp");
                    String icon = hourObject.getString("icon");
                    String text = hourObject.getString("text");
                    HourlyForecastItem item = new HourlyForecastItem(fxTime,temp,icon,text);
                    hourlyForecastsList.add(item);
                }
                Log.d(TAG_HOURLY,"成功解析 "+hourlyForecastsList.size()+" 条逐小时预报。");
                runOnUiThread(() -> { if (hourlyForecastAdapter != null) { hourlyForecastAdapter.notifyDataSetChanged(); } else { Log.e(TAG_HOURLY, "hourlyForecastAdapter 在 parseHourlyJsonResponse 中为 null (UI可能未完全初始化)"); }});
            } else {
                Log.e(TAG_HOURLY,"逐小时预报API返回错误码: "+code+", Msg: "+jsonObject.optString("message"));
                Toast.makeText(this,"获取逐小时预报数据状态码错误: "+code,Toast.LENGTH_SHORT).show();
            }
        } catch(JSONException e) {
            Log.e(TAG_HOURLY,"解析逐小时预报JSON失败: ",e);
            Toast.makeText(this,"解析逐小时预报数据失败",Toast.LENGTH_SHORT).show();
        }
    }
    private void handleVolleyError(VolleyError error,String toastMessagePrefix){
        if(error.networkResponse!=null){Log.e(TAG_NOW,"Status Code: "+error.networkResponse.statusCode);try{String responseBody=new String(error.networkResponse.data,"utf-8");Log.e(TAG_NOW,"Error Response Body: "+responseBody);}catch(java.io.UnsupportedEncodingException e){Log.e(TAG_NOW,"UnsupportedEncodingException for error response: "+e.getMessage());}}
        Toast.makeText(MainActivity.this,toastMessagePrefix+": "+(error.getMessage() == null ? "Unknown Volley error" : error.getMessage()),Toast.LENGTH_LONG).show();
    }
    private void handleApiError(JSONObject jsonObject,String code,String logTag,String toastMessagePrefix){
        String apiMessage=jsonObject.optString("message","No error message provided by API");Log.e(logTag,"API Error Code: "+code+", Message: "+apiMessage);Toast.makeText(MainActivity.this,toastMessagePrefix+"，API状态码: "+code,Toast.LENGTH_LONG).show();
    }
    private void handleJsonException(JSONException e,String logTag,String toastMessage){
        Log.e(logTag,"JSON parsing error: "+e.getMessage());Toast.makeText(MainActivity.this,toastMessage,Toast.LENGTH_LONG).show();
    }
}