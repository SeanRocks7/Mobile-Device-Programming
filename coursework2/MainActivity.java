package com.example.coursework2;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;
import android.view.View;
import android.Manifest;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MainActivity extends AppCompatActivity {
    private MapView mapView;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private EditText latitudeInput;
    private EditText longitudeInput;
    private EditText messageInput;
    private Button reminderButton;
    private ReminderViewModel reminderViewModel;
    private ListView listViewReminders;
    private ArrayAdapter<String> remindersAdapter;
    private List<LatLng> points = new ArrayList<>();
    private GoogleMap googleMap;
    private int geofenceIdCounter = 0;
    private GeofencingClient geofencingClient;
    PendingIntent geofencePendingIntent;

    private List<Reminder> geofenceList = new ArrayList<>();

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(locationUpdateReceiver, new IntentFilter("LOCATION_UPDATE"));
        IntentFilter filter = new IntentFilter("com.example.coursework2.REMINDER_NOTIFIED");
        registerReceiver(reminderNotifiedReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(locationUpdateReceiver);
        unregisterReceiver(reminderNotifiedReceiver);
    }

    private BroadcastReceiver reminderNotifiedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.coursework2.REMINDER_NOTIFIED".equals(intent.getAction())) {
                // Remove the reminder from reminderViewModel once the user has been notified
                String reminderId = intent.getStringExtra("reminderId");
                reminderViewModel.removeReminderById(reminderId);
            }
        }
    };

    private BroadcastReceiver locationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        // Broadcast location updates to the service
        if ("LOCATION_UPDATE".equals(intent.getAction())) {
            double latitude = intent.getDoubleExtra("latitude", 0);
            double longitude = intent.getDoubleExtra("longitude", 0);
            updateMapPath(latitude, longitude);
        }
        }
    };

    public class GeofenceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            if (geofencingEvent.hasError()) {
                int errorCode = geofencingEvent.getErrorCode();
                Log.e(TAG, "GeofencingEvent Error: " + errorCode);
                return;
            }

            // Get the transition type
            int geofenceTransition = geofencingEvent.getGeofenceTransition();

            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                // Geofence entry event
                Log.d(TAG, "Entering geofence area");
            } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                // Geofence exit event
                Log.d(TAG, "Exiting geofence area");
            }
        }
    }

    private GeofencingRequest getGeofencingRequest(Geofence geofence) {
        return new GeofencingRequest.Builder()
                .addGeofence(geofence)
                .build();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Creates or retrieves a PendingIntent for geofence transitions.
        // This PendingIntent is used to trigger a broadcast when a geofence transition occurs.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags);
        return geofencePendingIntent;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        geofencingClient = LocationServices.getGeofencingClient(this);

        // Asynchronously gets the GoogleMap instance associated with the mapView.
        mapView.getMapAsync(new OnMapReadyCallback() {
            // This callback method is triggered when the map is ready to be used.
            // It provides a non-null instance of GoogleMap.
            @Override
            public void onMapReady(GoogleMap googleMap) {
                MainActivity.this.googleMap = googleMap;
            }
        });

        // Initialize UI components
        latitudeInput = findViewById(R.id.editTextLatitude);
        longitudeInput = findViewById(R.id.editTextLongtitude);
        messageInput = findViewById(R.id.editTextMessage);
        reminderButton = findViewById(R.id.reminderButton);
        listViewReminders = findViewById(R.id.listView);

        // Initialize ViewModel
        reminderViewModel = new ViewModelProvider(this).get(ReminderViewModel.class);
        // Set up a LiveData observer
        reminderViewModel.getReminders().observe(this, this::updateUIWithReminders);
        // When clicked add a reminder
        reminderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addReminder();
            }
        });

    }
    private String getNextGeofenceId() {
        return "Geofence_" + geofenceIdCounter++;
    }
    private void addReminder() {
        double latitude = Double.parseDouble(latitudeInput.getText().toString());
        double longitude = Double.parseDouble(longitudeInput.getText().toString());
        String message = messageInput.getText().toString();
        int radius = 250;
        String id = getNextGeofenceId();
        Reminder newReminder = new Reminder(latitude, longitude, message, radius, id);
        reminderViewModel.addReminder(newReminder);
        // Add the geofence for this reminder
        Geofence geofence = new Geofence.Builder()
                .setRequestId(newReminder.getId())
                .setCircularRegion(
                        newReminder.getLatitude(),
                        newReminder.getLongitude(),
                        newReminder.getRadius()
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        // Check permissions before adding geofence
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            geofencingClient.addGeofences(getGeofencingRequest(geofence), getGeofencePendingIntent())
                    .addOnSuccessListener(this, aVoid -> {
                        // Geofence added successfully
                        Log.d(TAG, "Geofences added successfully");

                    })
                    .addOnFailureListener(this, e -> {
                        // Failed to add geofences
                        Log.e(TAG, "Failed to add geofences: " + e.getMessage());

                    });
        }
        geofenceList.add(newReminder);
        // Draw a circle on the map
        drawAllGeofences();
    }

    private void drawAllGeofences() {
        if (googleMap != null) {
            // Clear the map to redraw
            googleMap.clear();
            // Draw each geofence circle
            for (Reminder reminder : geofenceList) {
                LatLng circleLatLng = new LatLng(reminder.getLatitude(), reminder.getLongitude());
                googleMap.addCircle(new CircleOptions()
                        .center(circleLatLng)
                        .radius(reminder.getRadius())
                        .strokeColor(Color.argb(50, 70, 70, 70))
                        .fillColor(Color.argb(50, 150, 150, 150)));
            }
            // Draw the path, if any
            drawPath();
        }
    }
    private void updateUIWithReminders(List<Reminder> reminders) {
        List<String> reminderMessages = new ArrayList<>();
        for (Reminder reminder : reminders) {
            reminderMessages.add(reminder.getMessage());
        }

        // Initialize the adapter if it's null
        if (remindersAdapter == null) {
            remindersAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, reminderMessages);
            listViewReminders = findViewById(R.id.listView);
            listViewReminders.setAdapter(remindersAdapter);
        } else {
            remindersAdapter.clear();
            remindersAdapter.addAll(reminderMessages);
            remindersAdapter.notifyDataSetChanged();
        }
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            return false;
        }
        return true;
    }

    public void onClickStart(View view) {
        if (checkLocationPermission()) {
            Intent serviceIntent = new Intent(this, TrackerService.class);
            serviceIntent.setAction("ACTION_START_LOCATION_SERVICE");
            startService(serviceIntent);
        }
    }

    public void onClickStop(View view) {
        Intent serviceIntent = new Intent(this, TrackerService.class);
        serviceIntent.setAction("ACTION_STOP_LOCATION_SERVICE");
        // Removes all geofencing when stop tracking
        removeAllGeofences();
        stopService(serviceIntent);
    }

    public void onClickData(View view) {
        Intent activity = new Intent(this, DataAnalysisActivity.class);
        startActivity(activity);
    }

    private void updateMapPath(double latitude, double longitude) {
        points.add(new LatLng(latitude, longitude));

        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        for (LatLng point : points) {
            options.add(point);
        }
        // Clear old path
        googleMap.clear();
        // Draw new path
        googleMap.addPolyline(options);
        // Redraw geofences
        drawAllGeofences();

    }
    private void drawPath() {
        // Draws a blue line on the mapView
        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        for (LatLng point : points) {
            options.add(point);
        }
        // Draw the path
        googleMap.addPolyline(options);
    }

    private void removeAllGeofences() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            geofencingClient.removeGeofences(getGeofencePendingIntent())
                    .addOnSuccessListener(this, aVoid -> Log.d(TAG, "Successfully removed all geofences"))
                    .addOnFailureListener(this, e -> Log.e(TAG, "Failed to remove all geofences: " + e.getMessage()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }


}