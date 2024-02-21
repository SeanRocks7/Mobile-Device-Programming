package com.example.coursework2;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DAOLocation {
    @Insert
    void insertLocation(EntityLocation location);

    @Query("SELECT * FROM EntityLocation")
    LiveData<List<EntityLocation>> getAllLocations();

    @Query("DELETE FROM EntityLocation")
    void deleteAllLocations();

    @Update
    void updateLocation(EntityLocation location);
}
