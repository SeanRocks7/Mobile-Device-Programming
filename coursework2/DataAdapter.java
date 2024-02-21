package com.example.coursework2;

import android.location.Location;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.DataViewHolder> {
    private List<EntityLocation> dataList;

    public DataAdapter(List<EntityLocation> dataList) {
        this.dataList = dataList;
    }
    public void setData(List<EntityLocation> newData) {
        this.dataList = newData;
        // Notify that data has changed
        notifyDataSetChanged();
    }

    @Override
    public DataViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_data, parent, false);
        return new DataViewHolder(view, dataList);    }

    public void clearData() {
        // Clear all the data in the recyclerView
        dataList.clear();
        // Notify the adapter that the data set has changed
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(DataViewHolder holder, int position) {
        EntityLocation currentData = dataList.get(position);
        EntityLocation previousData = position > 0 ? dataList.get(position - 1) : null;

        String formattedDate = formatDate(currentData.timestamp);
        String distanceStr = "";

        // This calculates the distance moved from current location
        if (previousData != null) {
            float[] results = new float[1];
            Location.distanceBetween(previousData.latitude, previousData.longitude, currentData.latitude, currentData.longitude, results);
            // Distance in meters
            float distance = results[0];
            distanceStr = ", Distance from last: " + distance + "m";
        }

        holder.textViewData.setText("Latitude: " + currentData.latitude + ", Longitude: " + currentData.longitude + ", Time: " + formattedDate + distanceStr);
        holder.editTextAnnotation.setText(currentData.getAnnotation());
    }

    private String formatDate(long timestamp) {
        // This is a DateFormatter object for displaying date in specified format
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        Date date = new Date(timestamp);

        // Format the date into the desired format
        return formatter.format(date);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class DataViewHolder extends RecyclerView.ViewHolder {
        TextView textViewData;
        EditText editTextAnnotation;
        List<EntityLocation> dataList;

        DataViewHolder(View itemView, List<EntityLocation> dataList) {
            super(itemView);
            this.dataList = dataList;
            textViewData = itemView.findViewById(R.id.textViewData);
            editTextAnnotation = itemView.findViewById(R.id.editTextAnnotation);

            // Listener that handles annotation edits
            editTextAnnotation.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    int position = getAdapterPosition();
                    // Ensures that the users doesn't access an invalid position in the list
                    if (position != RecyclerView.NO_POSITION && position < dataList.size()) {
                        EntityLocation entity = dataList.get(position);
                        entity.setAnnotation(s.toString());
                    }
                }
            });
        }
    }

    public List<EntityLocation> getDataList() {
        return dataList;
    }
}
