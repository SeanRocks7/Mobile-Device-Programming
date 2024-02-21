package com.example.coursework2;

import java.util.ArrayList;
import java.util.List;

public class ReminderDatabase {
    private List<Reminder> reminders = new ArrayList<>();

    private static ReminderDatabase instance;

    public List<Reminder> getReminders() {
        return reminders;
    }

    public void removeReminder(Reminder reminder) {
        reminders.remove(reminder);
    }

    public void addReminder(Reminder reminder) {
        reminders.add(reminder);
    }

    private ReminderDatabase() {}

    public static synchronized ReminderDatabase getInstance() {
        if (instance == null) {
            instance = new ReminderDatabase();
        }
        return instance;
    }

    public void removeReminderById(String reminderId) {
        for (int i = 0; i < reminders.size(); i++) {
            if (reminders.get(i).getId().equals(reminderId)) {
                reminders.remove(i);
                break;
            }
        }
    }
}
