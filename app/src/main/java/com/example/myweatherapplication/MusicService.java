package com.example.myweatherapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;

public class MusicService extends Service {

    private MediaPlayer mediaPlayer;
    private final IBinder musicBinder = new MusicBinder();
    private static final String TAG = "MusicService";

    private static final int NOTIFICATION_ID = 123;
    private static final String CHANNEL_ID = "MusicServiceChannel";
    private static final String CHANNEL_NAME = "音乐播放服务";

    // 【修改】使用 List<SongInfo> 来管理播放列表
    private ArrayList<SongInfo> songInfoList;
    private int currentSongIndex = 0;
    private boolean isPlaying = false;
    private boolean isPrepared = false;

    private MusicServiceCallback musicServiceCallback;

    // 【修改】回调接口，传递SongInfo对象或更详细的信息
    public interface MusicServiceCallback {
        void onPlaybackStateChanged(boolean isPlaying, SongInfo currentSong); // 传递整个SongInfo对象
        void onSongChanged(SongInfo newSong, int duration); // 传递整个SongInfo对象
    }

    public void setMusicServiceCallback(MusicServiceCallback callback) {
        this.musicServiceCallback = callback;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MusicService onCreate");
        createNotificationChannel();

        // 【修改】初始化 songInfoList，并填写真实的歌曲信息
        songInfoList = new ArrayList<>();
        // 确保 R.raw.test_song 对应 《青花瓷》
        songInfoList.add(new SongInfo(R.raw.test_song, "青花瓷", "周杰伦", "我很忙"));
        // 确保 R.raw.my_new_song2 对应 《麦恩莉》
        songInfoList.add(new SongInfo(R.raw.my_new_song2, "麦恩莉", "方大同", "JTW 西游记"));
        // 如果有更多歌曲，继续添加...

        // mediaPlayer 在 playSongAtIndex 中创建和管理
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("用于音乐播放的后台服务");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
                Log.d(TAG, "通知渠道已创建");
            } else {
                Log.e(TAG, "无法获取NotificationManager");
            }
        }
    }

    private void startForegroundAndShowNotification(String displayTitle, boolean isPlayingNow) { // 参数改为 displayTitle
        Intent notificationIntent = new Intent(this, MusicPlayerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String contentText = isPlayingNow ? "正在播放" : "已暂停";

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(displayTitle) // 【修改】使用包含艺术家信息的标题
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_music_notification)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_ID, notification);
        Log.d(TAG, "已启动前台服务并显示通知: " + contentText + " - " + displayTitle);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "MusicService onBind");
        return musicBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MusicService onStartCommand");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MusicService onDestroy");
        stopForeground(true);
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPrepared = false;
        isPlaying = false;
    }

    public void playSongAtIndex(int index) {
        if (songInfoList == null || songInfoList.isEmpty()) {
            Log.e(TAG, "歌曲列表为空，无法播放");
            Toast.makeText(this, "歌曲列表为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (index < 0 || index >= songInfoList.size()) {
            Log.e(TAG, "无效的歌曲索引: " + index + ", 列表大小: " + songInfoList.size());
            currentSongIndex = 0;
            if (songInfoList.isEmpty()) return;
            index = currentSongIndex;
        }

        currentSongIndex = index;
        isPrepared = false;
        SongInfo currentSong = songInfoList.get(currentSongIndex); // 【修改】获取SongInfo对象

        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
            } catch (IllegalStateException e) {
                Log.e(TAG, "重置MediaPlayer时出错: " + e.getMessage());
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }

        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }

        try {
            AssetFileDescriptor afd = getResources().openRawResourceFd(currentSong.getResourceId()); // 【修改】使用songInfo.getResourceId()
            if (afd == null) {
                Log.e(TAG, "无法打开歌曲资源: " + currentSong.getResourceId());
                Toast.makeText(this, "无法加载歌曲: " + currentSong.getTitle(), Toast.LENGTH_SHORT).show();
                return;
            }
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer prepared, starting playback for: " + currentSong.getDisplayTitleWithArtist());
                isPrepared = true;
                mp.start();
                isPlaying = true;
                currentSong.setDuration(mp.getDuration()); // 【新增】更新SongInfo中的时长
                startForegroundAndShowNotification(currentSong.getDisplayTitleWithArtist(), true); // 【修改】
                if (musicServiceCallback != null) {
                    musicServiceCallback.onPlaybackStateChanged(true, currentSong); // 【修改】
                    musicServiceCallback.onSongChanged(currentSong, mp.getDuration()); // 【修改】
                }
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Playback completed for: " + currentSong.getDisplayTitleWithArtist());
                playNextSong();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer 错误: what " + what + ", extra " + extra + " for " + currentSong.getDisplayTitleWithArtist());
                Toast.makeText(MusicService.this, "播放 '" + currentSong.getTitle() + "' 时出错", Toast.LENGTH_SHORT).show();
                isPlaying = false;
                isPrepared = false;
                if (musicServiceCallback != null) {
                    musicServiceCallback.onPlaybackStateChanged(false, currentSong); // 【修改】
                }
                return true;
            });

            mediaPlayer.prepareAsync();
            Log.d(TAG, "正在准备歌曲: " + currentSong.getDisplayTitleWithArtist());

        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "设置数据源或准备MediaPlayer时出错: " + e.getMessage());
            Toast.makeText(this, "播放错误", Toast.LENGTH_SHORT).show();
            isPlaying = false;
            isPrepared = false;
            if (musicServiceCallback != null && !songInfoList.isEmpty()) { // 确保列表不为空
                musicServiceCallback.onPlaybackStateChanged(false, songInfoList.get(currentSongIndex)); // 【修改】
            }
        }
    }

    public void playMusic() {
        if (songInfoList == null || songInfoList.isEmpty()){ // 【修改】检查songInfoList
            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isPrepared) {
            playSongAtIndex(currentSongIndex);
        } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            SongInfo currentSong = songInfoList.get(currentSongIndex); // 【修改】
            startForegroundAndShowNotification(currentSong.getDisplayTitleWithArtist(), true); // 【修改】
            if (musicServiceCallback != null) {
                musicServiceCallback.onPlaybackStateChanged(true, currentSong); // 【修改】
            }
            Log.d(TAG, "音乐继续播放 (Service): " + currentSong.getDisplayTitleWithArtist());
        }
    }

    public void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            if (songInfoList != null && !songInfoList.isEmpty()) { // 【修改】检查songInfoList
                SongInfo currentSong = songInfoList.get(currentSongIndex);
                startForegroundAndShowNotification(currentSong.getDisplayTitleWithArtist(), false); // 【修改】
                if (musicServiceCallback != null) {
                    musicServiceCallback.onPlaybackStateChanged(false, currentSong); // 【修改】
                }
            }
            Log.d(TAG, "音乐已暂停 (Service)");
        }
    }

    public void stopMusic() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            isPrepared = false;
            isPlaying = false;
            Log.d(TAG, "音乐已停止并释放 (Service)");
            stopForeground(true);
            stopSelf();
            if (musicServiceCallback != null) {
                // 当停止时，可以传递一个空的或表示无歌曲的SongInfo，或null
                musicServiceCallback.onPlaybackStateChanged(false, null);
            }
        }
    }

    public void playNextSong() {
        if (songInfoList != null && !songInfoList.isEmpty()) {
            currentSongIndex = (currentSongIndex + 1) % songInfoList.size();
            Log.d(TAG, "播放下一首, index: " + currentSongIndex);
            playSongAtIndex(currentSongIndex);
        }
    }

    public void playPreviousSong() {
        if (songInfoList != null && !songInfoList.isEmpty()) {
            currentSongIndex = (currentSongIndex - 1 + songInfoList.size()) % songInfoList.size();
            Log.d(TAG, "播放上一首, index: " + currentSongIndex);
            playSongAtIndex(currentSongIndex);
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && isPrepared) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) { Log.e(TAG, "获取当前位置时MediaPlayer状态错误", e); }
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null && isPrepared) {
            try {
                // 【修改】从SongInfo对象获取已设置的duration，如果还未设置，则从MediaPlayer获取
                SongInfo currentSong = songInfoList.get(currentSongIndex);
                if (currentSong.getDuration() > 0) {
                    return (int) currentSong.getDuration();
                }
                return mediaPlayer.getDuration();
            } catch (IllegalStateException e) { Log.e(TAG, "获取总时长时MediaPlayer状态错误", e); }
        }
        return 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null && isPrepared) {
            try {
                mediaPlayer.seekTo(position);
            } catch (IllegalStateException e) { Log.e(TAG, "SeekTo时MediaPlayer状态错误", e); }
        }
    }

    // 【修改】返回当前的SongInfo对象
    public SongInfo getCurrentSongInfo() {
        if (songInfoList != null && !songInfoList.isEmpty() && currentSongIndex >= 0 && currentSongIndex < songInfoList.size()) {
            return songInfoList.get(currentSongIndex);
        }
        return null; // 或者返回一个默认的 "未知歌曲" SongInfo 对象
    }
    public ArrayList<SongInfo> getSongInfoList() {
        return songInfoList;
    }
}
