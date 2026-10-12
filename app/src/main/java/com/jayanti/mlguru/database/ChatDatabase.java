package com.jayanti.mlguru.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.jayanti.mlguru.model.ChatMessage;

/**
 * Room Database for ML Guru chat persistence.
 * Uses "ml_db" with allowMainThreadQueries() and fallbackToDestructiveMigration().
 */
@Database(entities = {ChatMessage.class, ChatHistory.class}, version = 1, exportSchema = false)
public abstract class ChatDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "ml_db";
    private static volatile ChatDatabase instance;

    public abstract ChatDao chatDao();

    public static synchronized ChatDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            ChatDatabase.class,
                            DATABASE_NAME)
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
