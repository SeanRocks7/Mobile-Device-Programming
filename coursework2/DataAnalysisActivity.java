package com.example.coursework2;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataAnalysisActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private DataAdapter adapter;
    private ExecutorService executorService;
    private Button annotationButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_analysis);

        executorService = Executors.newSingleThreadExecutor();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize data list and adapter
        adapter = new DataAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Get location data from database
        DatabaseApp db = DatabaseClient.getInstance(this).getAppDatabase();

        // Observe the LiveData
        db.daoLocation().getAllLocations().observe(this, locations -> {
            // Update the adapter with the new data
            adapter.setData(locations);
        });

        annotationButton = findViewById(R.id.button5);
        annotationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAnnotations();
            }
        });
    }

    private void saveAnnotations() {
        // This saves updated annotations for a list of EntityLocation objects to the database
        List<EntityLocation> locations = adapter.getDataList();
        for (EntityLocation location : locations) {
            executorService.execute(() -> {
                DatabaseApp db = DatabaseClient.getInstance(getApplicationContext()).getAppDatabase();
                db.daoLocation().updateLocation(location);
            });
        }
    }

    private void clearDataAndUpdateSource() {
        // Clear the data in the adapter
        adapter.clearData();

        // Update the data source to reflect the change
        executorService.execute(() -> {
            DatabaseApp db = DatabaseClient.getInstance(getApplicationContext()).getAppDatabase();
            db.daoLocation().deleteAllLocations();
        });
    }

    public void onClickBack(View view) {
        finish();
    }

    public void onClickClear(View view) {
        clearDataAndUpdateSource();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}