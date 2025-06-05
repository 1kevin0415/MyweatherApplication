package com.example.myweatherapplication;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
// 移除了 ArrayList 和 List 的导入，因为我们不再直接在这里管理歌曲列表的原始数据

public class MusicPlayerActivity extends AppCompatActivity {

    private Button buttonPlayPause;
    private Button buttonStop;
    private Button buttonPrevious;
    private Button buttonNext;
    private TextView textViewSongTitle; // 用于显示歌曲标题和艺术家

    private static final String TAG_MUSIC_PLAYER = "MusicPlayerActivity";

    private MusicService musicService;
    private boolean isBound = false;
    private boolean currentIsPlaying = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            musicService.setMusicServiceCallback(serviceCallback); // 设置回调
            Log.d(TAG_MUSIC_PLAYER, "MusicService已连接");
            Toast.makeText(MusicPlayerActivity.this, "音乐服务已连接", Toast.LENGTH_SHORT).show();

            // 连接成功后，立即更新UI状态
            if (musicService != null) {
                currentIsPlaying = musicService.isPlaying();
                updatePlayPauseButtonText();
                SongInfo currentSong = musicService.getCurrentSongInfo();
                if (textViewSongTitle != null && currentSong != null) {
                    textViewSongTitle.setText(currentSong.getDisplayTitleWithArtist());
                } else if (textViewSongTitle != null) {
                    textViewSongTitle.setText("准备播放...");
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
            if (musicService != null) { // 检查musicService是否为null
                musicService.setMusicServiceCallback(null);
                musicService = null;
            }
            Log.d(TAG_MUSIC_PLAYER, "MusicService已断开连接");
            Toast.makeText(MusicPlayerActivity.this, "音乐服务已断开", Toast.LENGTH_SHORT).show();
            updatePlayPauseButtonText();
            if (textViewSongTitle != null) {
                textViewSongTitle.setText("服务未连接");
            }
        }
    };

    // 实现Service的回调接口
    private MusicService.MusicServiceCallback serviceCallback = new MusicService.MusicServiceCallback() {
        @Override
        public void onPlaybackStateChanged(boolean isPlaying, SongInfo currentSong) {
            currentIsPlaying = isPlaying;
            updatePlayPauseButtonText();
            if (textViewSongTitle != null && currentSong != null) {
                textViewSongTitle.setText(currentSong.getDisplayTitleWithArtist());
            } else if (textViewSongTitle != null && !isPlaying && currentSong == null) { // 停止时
                textViewSongTitle.setText("播放已停止");
            }
        }

        @Override
        public void onSongChanged(SongInfo newSong, int duration) {
            if (textViewSongTitle != null && newSong != null) {
                textViewSongTitle.setText(newSong.getDisplayTitleWithArtist());
            }
            // 如果有SeekBar，在这里更新总时长: seekBarMusic.setMax(duration);
            Log.d(TAG_MUSIC_PLAYER, "歌曲已切换: " + (newSong != null ? newSong.getDisplayTitleWithArtist() : "未知歌曲") + ", 时长: " + duration);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        textViewSongTitle = findViewById(R.id.textViewSongTitle);
        buttonPlayPause = findViewById(R.id.buttonPlayPause);
        buttonStop = findViewById(R.id.buttonStop);
        buttonPrevious = findViewById(R.id.buttonPrevious);
        buttonNext = findViewById(R.id.buttonNext);

        buttonPlayPause.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                if (musicService.isPlaying()) {
                    musicService.pauseMusic();
                } else {
                    musicService.playMusic();
                }
            } else {
                Toast.makeText(this, "音乐服务未连接，请稍后...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MusicService.class);
                startService(intent);
                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            }
        });

        buttonStop.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                musicService.stopMusic();
                // UI更新将通过回调（onPlaybackStateChanged，currentSong为null）处理
            }
        });

        buttonPrevious.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                musicService.playPreviousSong();
            }
        });

        buttonNext.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                musicService.playNextSong();
            }
        });

        updatePlayPauseButtonText(); // 初始按钮状态
        if (textViewSongTitle != null) {
            textViewSongTitle.setText("准备播放..."); // 初始文本
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MusicService.class);
        startService(intent); // 确保服务即使Activity不在前台也能运行
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG_MUSIC_PLAYER, "尝试启动并绑定MusicService");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            if (musicService != null) {
                musicService.setMusicServiceCallback(null); // 移除回调
            }
            unbindService(serviceConnection);
            isBound = false;
            // 不将 musicService 设为 null，除非确定在 onStart 不会再次绑定
            // 或者在 onServiceDisconnected 中处理
            Log.d(TAG_MUSIC_PLAYER, "已解绑MusicService");
        }
    }

    private void updatePlayPauseButtonText() {
        if (currentIsPlaying) {
            buttonPlayPause.setText("暂停");
        } else {
            buttonPlayPause.setText("播放");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 如果在onStop中没有将musicService设为null，这里通常也不需要额外处理
        // 如果希望Activity销毁时服务也停止（如果只有这个Activity用它），
        // 可以在这里调用stopService(new Intent(this, MusicService.class));
        // 但通常服务的生命周期由其自身或startService/stopService以及stopSelf()管理
        Log.d(TAG_MUSIC_PLAYER, "MusicPlayerActivity onDestroy");
    }
}