package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra("REMINDER_ID", -1)
        val title = intent.getStringExtra("TITLE") ?: "Reminder"

        if (reminderId != -1) {
            // 1. Show notification immediately (Works OFFLINE)
            sendAndroidNotification(
                context = context,
                id = reminderId,
                channelId = "rk_assistant_alarms",
                channelName = "RK Alarms",
                title = "⏰ RK Reminder",
                text = title
            )

            // 2. Trigger a message in the AI Console (Works OFFLINE)
            val db = AppDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                // Mark reminder as done
                val all = db.reminderDao().getAllRemindersList()
                val match = all.find { it.id == reminderId }
                if (match != null) {
                    db.reminderDao().updateReminder(match.copy(isAcknowledged = true))
                }

                // Insert RK's automated response into the chat
                db.chatMessageDao().insertMessage(ChatMessage(
                    chatSessionId = "default",
                    sender = "Rk",
                    text = "⏰ Reminder Triggered: \"$title\". I hope you completed this task! ✅"
                ))
            }
        }
    }
}
