package com.jayanti.mlguru.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Model representing a single chat message, persisted in Room DB.
 */
@Entity(tableName = "chat_messages")
public class ChatMessage {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public static final int SENDER_USER = 1;
    public static final int SENDER_BOT = 2;

    private int sender;
    private String message;
    private long timestamp;
    private String imageUri;

    @Ignore
    private boolean isTyping;

    public ChatMessage() {
    }

    @Ignore
    public ChatMessage(int sender, String message, long timestamp) {
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
        this.imageUri = null;
        this.isTyping = false;
    }

    @Ignore
    public ChatMessage(int sender, String message, long timestamp, String imageUri) {
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
        this.imageUri = imageUri;
        this.isTyping = false;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getSender() {
        return sender;
    }

    public void setSender(int sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public boolean isTyping() {
        return isTyping;
    }

    public void setTyping(boolean typing) {
        isTyping = typing;
    }
}
