package com.example.coursework2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.Manifest;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.room.Room;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrackerService extends Service {

    private static final long MIN_TIME_BW_UPDATES = 1000 * 5; // 5 seconds
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    private final int NOTIFICATION_ID = 123;
    private ExecutorService executorService;
    private Set<String> notifiedReminderIds = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newSingleThreadExecutor();
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        MyLocationListener locationListener = new MyLocationListener();
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5, 5 , locationListener);
        } catch(SecurityException e) {
            Log.d("comp3018", e.toString());
        }

        startForeground(NOTIFICATION_ID, getNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("ACTION_START_LOCATION_SERVICE".equals(action)) {
                startLocationUpdates();
            } else if ("ACTION_STOP_LOCATION_SERVICE".equals(action)) {
                stopLocationUpdates();
            }
        }
        return START_STICKY;
    }

    private void checkReminders(Location location) {
        List<Reminder> reminders = ReminderDatabase.getInstance().getReminders();

        // Handle the reminders
        for (Reminder reminder : reminders) {
            // Check if the reminder's ID is in the set of notified IDs
            if (!notifiedReminderIds.contains(reminder.getId())) {
                Location targetLocation = new Location("");
                targetLocation.setLatitude(reminder.getLatitude());
                targetLocation.setLongitude(reminder.getLongitude());

                float distance = location.distanceTo(targetLocation);
                if (distance < reminder.getRadius()) {
                    sendNotification(reminder.getMessage(), reminder.getId());
                    // Add the reminder's ID to the Set of notified IDs
                    notifiedReminderIds.add(reminder.getId());
                }
            }
        }

    }

    public class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            Log.d("comp3018", location.getLatitude() + " " + location.getLongitude());
            EntityLocation entityLocation = new EntityLocation();
            entityLocation.latitude = location.getLatitude();
            entityLocation.longitude = location.getLongitude();
            entityLocation.timestamp = System.currentTimeMillis();
            // Insert into database on a background thread
            if (executorService != null && !executorService.isShutdown()) {
                executorService.execute(() -> {
                    DatabaseApp db = DatabaseClient.getInstance(getApplicationContext()).getAppDatabase();
                    db.daoLocation().insertLocation(entityLocation);
                });
            }
            // Check if a reminder needs to be sent to the user everytime the location changes
            checkReminders(location);
            // To start observing broadcast location
            sendLocationBroadcast(location);

        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // information about the signal, i.e. number of satellites
            Log.d("comp3018", "onStatusChanged: " + provider + " " + status);
        }
        @Override
        public void onProviderEnabled(String provider) {
            // the user enabled (for example) the GPS
            Log.d("comp3018", "onProviderEnabled: " + provider);
            if (LocationManager.GPS_PROVIDER.equals(provider)) {
                // GPS has been enabled
                sendGpsStateBroadcast(true);
            }
        }
        @Override
        public void onProviderDisabled(String provider) {
            // the user disabled (for example) the GPS
            Log.d("comp3018", "onProviderDisabled: " + provider);
            if (LocationManager.GPS_PROVIDER.equals(provider)) {
                // GPS has been disabled
                sendGpsStateBroadcast(false);
            }
        }
    }
    private void sendLocationBroadcast(Location location) {
        Intent intent = new Intent("LOCATION_UPDATE");
        intent.putExtra("latitude", location.getLatitude());
        intent.putExtra("longitude", location.getLongitude());
        sendBroadcast(intent);
    }
    private void sendGpsStateBroadcast(boolean isGpsEnabled) {
        Intent intent = new Intent("GPS_STATE_CHANGED");
        intent.putExtra("isGpsEnabled", isGpsEnabled);
        sendBroadcast(intent);
    }
    private void sendNotification(String message, String reminderId) {
        // Create and send a notification with the reminder message
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "REMINDER OF LOCATION",
                    "Reminder Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(TrackerService.this, "REMINDER OF LOCATION")
                .setContentTitle("Reminder")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .build();
        notificationManager.notify(new Random().nextInt(), notification);
        // Once user has been reminded, send broadcast to MainActivity
        // to remove the reminder from the LiveData list (from listView)
        Intent intent = new Intent("com.example.coursework2.REMINDER_NOTIFIED");
        intent.putExtra("reminderId", reminderId);
        sendBroadcast(intent);

    }
    private Notification getNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "channel_01", "Tracker Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel_01")
                .setContentTitle("Tracking Movement")
                .setContentText("Tracking your movement")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        return builder.build();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        MyLocationListener locationListener = new MyLocationListener();

        // Assuming permission has already been granted
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                locationListener);
    }


    private void stopLocationUpdates() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        MyLocationListener locationListener = new MyLocationListener();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission check
            return;
        }
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Shutdown service and stop tracking for location
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        stopLocationUpdates();
    }
}
