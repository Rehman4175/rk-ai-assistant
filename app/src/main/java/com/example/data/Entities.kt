package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val priority: String = "Medium", // Low, Medium, High
    val label: String = "General", // Work, Personal, Shopping, etc.
    val dueDate: String = "", // YYYY-MM-DD
    val notes: String = "",
    val isRepeating: Boolean = false,
    val repeatInterval: String = "None", // Daily, Weekly, Monthly
    val createdDate: String = "", // YYYY-MM-DD
    val doneDate: String = "" // YYYY-MM-DD
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dueDateTime: Long, // timestamp
    val recurrence: String = "None", // One-time, Daily, Weekly, Monthly
    val isAcknowledged: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val chatId: String = "",
    val lastFired: Long = 0L,
    val remarks: String = ""
)

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String = "Water", // Exercise, Water, Sleep, Prayer, Reading, Meditation, Custom
    val loggedDaysCommaSeparated: String = "", // comma-separated dates "2026-06-15,2026-06-16"
    val streakCount: Int = 0,
    val lastLoggedTimestamp: Long = 0L,
    val emoji: String = "✅",
    val bestStreak: Int = 0,
    val targetPerDay: String = ""
)

@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mlAmount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val dayString: String // YYYY-MM-DD
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val title: String,
    val isIncome: Boolean = false,
    val category: String = "Food", // Food, Fuel, Bill, Salary, Rent, Shopping, Health, Other
    val dateString: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val category: String = "Mobile", // Electricity, Rent, Internet, Mobile, custom
    val dueDayOfMonth: Int = 1,
    val paidMonthsCommaSeparated: String = "", // "2026-06,2026-07"
    val isAutoPay: Boolean = false,
    val paymentMethod: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dateString: String, // YYYY-MM-DD
    val timeString: String = "09:00", // HH:MM
    val location: String = "",
    val notes: String = "",
    val type: String = "Meeting", // Meeting, Birthday, Holiday, Custom
    val isAiGenerated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val remindDayBefore: Boolean = true
)

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String, // YYYY-MM-DD
    val text: String,
    val mood: String = "Neutral", // 😊 Happy, 😢 Sad, ⚡ Energetic, 🧘 Peaceful, 😐 Neutral
    val photoPath: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "quick_notes")
data class QuickNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String = "Notes"
)

@Entity(tableName = "memories")
data class PersonalMemory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val category: String = "General", // Work, Health, Finance, Personal, Study, Shopping, General
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatSessionId: String = "default",
    val sender: String, // "User" or "Rk" (AI)
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "smart_reminders")
data class SmartReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dueDateTime: Long,
    val priority: String, // HIGH, MEDIUM, LOW
    val repeatIntervalMinutes: Int,
    val maxRepeats: Int,
    val currentRepeat: Int = 0,
    val isAcknowledged: Boolean = false
)

@Entity(tableName = "voice_notes")
data class VoiceNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,
    val transcription: String,
    val duration: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val isTranscribed: Boolean = false,
    val status: String = "Success",
    val category: String = "General"
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val progress: Int = 0,
    val isDone: Boolean = false,
    val deadline: String = "", // YYYY-MM-DD
    val createdAt: String = "", // YYYY-MM-DD
    val milestones: String = ""
)

@Entity(tableName = "recurring_reminders")
data class RecurringReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String, // Daily, Weekly, Monthly
    val time: String, // HH:MM
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "remind_links")
data class RemindLink(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatId: Long,
    val text: String,
    val link: String,
    val dueDateTime: String, // YYYY-MM-DD HH:MM:SS
    val originalMsgId: Long? = null,
    val isAcknowledged: Boolean = false,
    val createdAt: String = "" // YYYY-MM-DD HH:MM:SS
)

@Entity(tableName = "private_space_items")
data class PrivateSpaceItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String, // Encrypted content
    val category: String = "note",
    val createdAt: String = "",
    val modifiedAt: String = ""
)
