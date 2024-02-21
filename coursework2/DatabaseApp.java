package com.example.coursework2;

import androidx.room.Database;
import androidx.room.RoomDatabase;
@Database(entities = {EntityLocation.class}, version = 1)
public abstract class DatabaseApp extends RoomDatabase {
    public abstract DAOLocation daoLocation();
}
