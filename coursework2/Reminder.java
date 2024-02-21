package com.example.coursework2;

public class Reminder {
    public double latitude;
    public double longitude;
    public String message;
    public float radius;
    public String id;

    public Reminder(double latitude, double longitude, String message, float radius, String id) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.message = message;
        this.radius = radius;
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }
    public double getLongitude() {
        return longitude;
    }
    public String getMessage() {
        return message;
    }
    public float getRadius() {
        return radius;
    }

    public String getId() {
        return id;
    }






}