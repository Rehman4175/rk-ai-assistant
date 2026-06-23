package com.example.data

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmartReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id = intent.getIntExtra("REMINDER_ID", -1)
        val type = intent.getStringExtra("TYPE") ?: "Reminder"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == "com.aistudio.rkaiassistant.COMPLETE_REMINDER") {
                    if (id != -1) {
                        notificationManager.cancel(id)
                        val db = AppDatabase.getDatabase(context)
                        if (type == "Smart Reminder") {
                            val srem = db.smartReminderDao().getById(id - 50000)
                            if (srem != null) db.smartReminderDao().updateSmartReminder(srem.copy(isAcknowledged = true))
                        } else {
                            val reminder = db.reminderDao().getReminderById(id)
                            if (reminder != null) db.reminderDao().updateReminder(reminder.copy(isAcknowledged = true))
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Completed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else if (intent.action == "com.aistudio.rkaiassistant.SNOOZE_REMINDER") {
                    if (id != -1) {
                        notificationManager.cancel(id)
                        val db = AppDatabase.getDatabase(context)
                        val newTime = System.currentTimeMillis() + (10 * 60 * 1000L)
                        
                        if (type == "Smart Reminder") {
                            val srem = db.smartReminderDao().getById(id - 50000)
                            if (srem != null) db.smartReminderDao().updateSmartReminder(srem.copy(dueDateTime = newTime, isAcknowledged = false))
                        } else {
                            val reminder = db.reminderDao().getReminderById(id)
                            if (reminder != null) db.reminderDao().updateReminder(reminder.copy(dueDateTime = newTime, isAcknowledged = false))
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Snoozed for 10 minutes.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
