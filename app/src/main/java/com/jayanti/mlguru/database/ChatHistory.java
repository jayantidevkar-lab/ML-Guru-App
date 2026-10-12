package com.jayanti.mlguru.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a saved conversation session in ML Guru.
 */
@Entity(tableName = "chat_history")
public class ChatHistory {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "time")
    public long time;

    @ColumnInfo(name = "chatJson")
    public String chatJson;

    public ChatHistory() {
    }

    @Ignore
    public ChatHistory(String title, long time, String chatJson) {
        this.title = title;
        this.time = time;
        this.chatJson = chatJson;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getChatJson() {
        return chatJson;
    }

    public void setChatJson(String chatJson) {
        this.chatJson = chatJson;
    }
}
