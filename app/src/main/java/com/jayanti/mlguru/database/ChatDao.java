package com.jayanti.mlguru.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.jayanti.mlguru.model.ChatMessage;

import java.util.List;

/**
 * Room Data Access Object (DAO) for chat messages and chat history.
 */
@Dao
public interface ChatDao {

    // Message Operations
    @Insert
    long insertMessage(ChatMessage message);

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    List<ChatMessage> getAllMessages();

    @Query("DELETE FROM chat_messages")
    void clearAllMessages();

    // Session History Operations
    @Insert
    long insertChat(ChatHistory chat);

    @Update
    void updateChat(ChatHistory chat);

    @Delete
    void deleteChat(ChatHistory chat);

    @Query("DELETE FROM chat_history WHERE id = :id")
    void deleteChatById(long id);

    @Query("SELECT * FROM chat_history ORDER BY time DESC")
    List<ChatHistory> getAllChats();

    @Query("SELECT * FROM chat_history WHERE id = :id LIMIT 1")
    ChatHistory getChatById(long id);

    @Query("DELETE FROM chat_history")
    void clearAllHistory();
}
