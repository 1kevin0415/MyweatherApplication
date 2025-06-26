package com.example.myweatherapplication;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MusicPlayerActivity extends AppCompatActivity {

    // --- 修改：将 Button 改为 ImageButton，并移除 buttonStop ---
    private ImageButton buttonPlayPause, buttonPrevious, buttonNext;
    private TextView textViewSongTitle, textViewCurrentTime, textViewTotalTime;
    private SeekBar seekBarMusic;
    private RecyclerView recyclerViewMusicList;

    private static final String TAG_MUSIC_PLAYER = "MusicPlayerActivity";

    private MusicService musicService;
    private boolean isBound = false;
    private MusicListAdapter musicListAdapter;
    private ArrayList<SongInfo> songList = new ArrayList<>();
    private Handler handler = new Handler();

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            musicService.setMusicServiceCallback(serviceCallback);
            Log.d(TAG_MUSIC_PLAYER, "MusicService已连接");

            updateUIFromService();
            runOnUiThread(updateSeekBarRunnable);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
            handler.removeCallbacks(updateSeekBarRunnable);
            musicService = null;
            Log.d(TAG_MUSIC_PLAYER, "MusicService已断开连接");
        }
    };

    private MusicService.MusicServiceCallback serviceCallback = new MusicService.MusicServiceCallback() {
        @Override
        public void onPlaybackStateChanged(boolean isPlaying, SongInfo currentSong) {
            // --- 修改：调用新的图标更新方法 ---
            updatePlayPauseIcon(isPlaying);
            updateSongInfoUI(currentSong);
        }

        @Override
        public void onSongChanged(SongInfo newSong, int duration) {
            updateSongInfoUI(newSong);
            seekBarMusic.setMax(duration);
            textViewTotalTime.setText(formatDuration(duration));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        initViews();
        setupListeners();
        setupRecyclerView();

        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initViews() {
        textViewSongTitle = findViewById(R.id.textViewSongTitle);
        buttonPlayPause = findViewById(R.id.buttonPlayPause);
        buttonPrevious = findViewById(R.id.buttonPrevious);
        buttonNext = findViewById(R.id.buttonNext);
        seekBarMusic = findViewById(R.id.seekBarMusic);
        textViewCurrentTime = findViewById(R.id.textViewCurrentTime);
        textViewTotalTime = findViewById(R.id.textViewTotalTime);
        recyclerViewMusicList = findViewById(R.id.recyclerViewMusicList);
    }

    private void setupListeners() {
        buttonPlayPause.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                if (musicService.isPlaying()) {
                    musicService.pauseMusic();
                } else {
                    musicService.playMusic();
                }
            }
        });

        // --- 修改：移除 buttonStop 的监听器 ---

        buttonPrevious.setOnClickListener(v -> {
            if (isBound && musicService != null) musicService.playPreviousSong();
        });

        buttonNext.setOnClickListener(v -> {
            if (isBound && musicService != null) musicService.playNextSong();
        });

        seekBarMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound && musicService != null) {
                    musicService.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupRecyclerView() {
        recyclerViewMusicList.setLayoutManager(new LinearLayoutManager(this));
        musicListAdapter = new MusicListAdapter(songList, (song, position) -> {
            if (isBound && musicService != null) {
                musicService.playSongAtIndex(position);
            }
        });
        recyclerViewMusicList.setAdapter(musicListAdapter);
    }

    private void updateUIFromService() {
        if (!isBound || musicService == null) return;

        // --- 修改：调用新的图标更新方法 ---
        updatePlayPauseIcon(musicService.isPlaying());
        SongInfo currentSong = musicService.getCurrentSongInfo();
        updateSongInfoUI(currentSong);
        seekBarMusic.setMax(musicService.getDuration());
        seekBarMusic.setProgress(musicService.getCurrentPosition());
        textViewTotalTime.setText(formatDuration(musicService.getDuration()));

        ArrayList<SongInfo> serviceSongList = musicService.getSongInfoList();
        if(serviceSongList != null) {
            this.songList.clear();
            this.songList.addAll(serviceSongList);
            musicListAdapter.notifyDataSetChanged();
        }
    }

    // --- 新方法：替换掉旧的 updatePlayPauseButtonText ---
    private void updatePlayPauseIcon(boolean isPlaying) {
        if (isPlaying) {
            buttonPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            buttonPlayPause.setImageResource(R.drawable.ic_play_arrow);
        }
    }

    private void updateSongInfoUI(SongInfo song) {
        if (song != null) {
            textViewSongTitle.setText(song.getDisplayTitleWithArtist());
        } else {
            textViewSongTitle.setText("播放已停止");
        }
    }

    private String formatDuration(long durationMs) {
        long seconds = (durationMs / 1000) % 60;
        long minutes = (durationMs / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private final Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBound && musicService != null && musicService.isPlaying()) {
                int currentPosition = musicService.getCurrentPosition();
                seekBarMusic.setProgress(currentPosition);
                textViewCurrentTime.setText(formatDuration(currentPosition));
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    // --- RecyclerView Adapter and ViewHolder (这部分代码无需修改) ---
    public interface OnSongClickListener {
        void onSongClick(SongInfo song, int position);
    }

    public static class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.SongViewHolder> {
        private final List<SongInfo> songs;
        private final OnSongClickListener listener;

        public MusicListAdapter(List<SongInfo> songs, OnSongClickListener listener) {
            this.songs = songs;
            this.listener = listener;
        }

        @NonNull
        @Override
        public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
            return new SongViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
            SongInfo song = songs.get(position);
            holder.bind(song, listener);
        }

        @Override
        public int getItemCount() {
            return songs.size();
        }

        static class SongViewHolder extends RecyclerView.ViewHolder {
            private TextView titleTextView;
            private TextView artistTextView;

            public SongViewHolder(@NonNull View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.textViewItemSongTitle);
                artistTextView = itemView.findViewById(R.id.textViewItemSongArtist);
            }

            public void bind(final SongInfo song, final OnSongClickListener listener) {
                titleTextView.setText(song.getTitle());
                artistTextView.setText(song.getArtist());
                itemView.setOnClickListener(v -> listener.onSongClick(song, getAdapterPosition()));
            }
        }
    }
}