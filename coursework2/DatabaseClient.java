package com.example.coursework2;

import android.content.Context;

import androidx.room.Room;

public class DatabaseClient {
    private static DatabaseClient instance;
    private DatabaseApp databaseApp;

    private DatabaseClient(Context context) {
        // Create the database app
        databaseApp = Room.databaseBuilder(context, DatabaseApp.class, "Database").build();
    }

    public static synchronized DatabaseClient getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseClient(context);
        }
        return instance;
    }

    public DatabaseApp getAppDatabase() {
        return databaseApp;
    }
}

