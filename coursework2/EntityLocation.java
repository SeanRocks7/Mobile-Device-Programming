package com.example.coursework2;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class EntityLocation {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public double latitude;
    public double longitude;
    public long timestamp;

    public String annotation;

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }
}
