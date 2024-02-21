package com.example.coursework2;



import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class ReminderViewModel extends ViewModel {
    private MutableLiveData<List<Reminder>> reminders;

    public ReminderViewModel() {
        reminders = new MutableLiveData<>();
        // Initialize with empty list
        reminders.setValue(new ArrayList<>());
    }

    public MutableLiveData<List<Reminder>> getReminders() {
        return reminders;
    }

    public void addReminder(Reminder reminder) {
        // Update the database
        ReminderDatabase.getInstance().addReminder(reminder);
        // Update LiveData from database
        reminders.setValue(ReminderDatabase.getInstance().getReminders());
    }

    public void removeReminder(Reminder reminder) {
        // Update the database
        ReminderDatabase.getInstance().removeReminder(reminder);
        // Update LiveData from database
        reminders.setValue(ReminderDatabase.getInstance().getReminders());
    }

    public void removeReminderById(String reminderId) {
        List<Reminder> currentReminders = reminders.getValue();
        if (currentReminders != null) {
            currentReminders.removeIf(reminder -> reminder.getId().equals(reminderId));
            // Update LiveData
            reminders.setValue(currentReminders);
            // Update database
            ReminderDatabase.getInstance().removeReminderById(reminderId);
        }
    }
}