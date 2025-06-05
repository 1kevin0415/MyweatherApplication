package com.example.myweatherapplication; // 确保包名正确

import java.io.Serializable;
import java.util.Locale;

public class SongInfo implements Serializable {
    private long id;          // 对于raw资源，我们可以用它的资源ID
    private String title;
    private String artist;
    private String album;         // 对于raw资源，这个可能为空或预设
    private String pathOrUri;     // 对于raw资源，这个可以为空或指向 "android.resource://"
    private long duration;        // 时长（对于raw资源，MediaPlayer可以获取）
    private int resourceId = 0; // 【新增】专门用于存储raw资源ID

    // 构造函数 - 主要用于 res/raw 资源
    public SongInfo(int resourceId, String title, String artist, String album) {
        this.id = resourceId; // 使用资源ID作为唯一标识
        this.resourceId = resourceId;
        this.title = title;
        this.artist = artist;
        this.album = (album == null || album.isEmpty()) ? "未知专辑" : album;
        this.pathOrUri = "android.resource://" + "com.example.myweatherapplication" + "/" + resourceId; // 构建一个象征性的URI
        this.duration = 0; // 初始时长设为0，待MediaPlayer加载后更新
    }

    // Getter 方法
    public long getId() {
        return id;
    }

    public String getTitle() {
        return title == null || title.isEmpty() ? "未知歌曲" : title;
    }

    public String getArtist() {
        return artist == null || artist.isEmpty() || artist.equalsIgnoreCase("<unknown>") ? "未知艺术家" : artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getPathOrUri() {
        return pathOrUri;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) { // 【新增】Setter，用于MediaPlayer准备好后更新时长
        this.duration = duration;
    }

    public int getResourceId() { // 【新增】Getter
        return resourceId;
    }

    public String getFormattedDuration() {
        if (duration <= 0) { // 修改判断条件
            return "00:00";
        }
        long seconds = (duration / 1000) % 60;
        long minutes = (duration / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    // 返回一个用于UI显示的字符串，例如 "歌曲名 - 歌手"
    public String getDisplayTitleWithArtist() {
        String artistDisplay = getArtist();
        if (artistDisplay.equals("未知艺术家")) {
            return getTitle();
        }
        return getTitle() + " - " + artistDisplay;
    }

    @Override
    public String toString() {
        return "SongInfo{" +
                "title='" + getTitle() + '\'' +
                ", artist='" + getArtist() + '\'' +
                ", resourceId=" + resourceId +
                '}';
    }
}