package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        Task::class,
        Reminder::class,
        Habit::class,
        WaterLog::class,
        Expense::class,
        Bill::class,
        CalendarEvent::class,
        DiaryEntry::class,
        QuickNote::class,
        PersonalMemory::class,
        ChatMessage::class,
        SmartReminder::class,
        VoiceNote::class,
        Goal::class,
        RecurringReminder::class,
        RemindLink::class,
        PrivateSpaceItem::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun habitDao(): HabitDao
    abstract fun waterLogDao(): WaterLogDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun billDao(): BillDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun diaryEntryDao(): DiaryEntryDao
    abstract fun quickNoteDao(): QuickNoteDao
    abstract fun personalMemoryDao(): PersonalMemoryDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun smartReminderDao(): SmartReminderDao
    abstract fun voiceNoteDao(): VoiceNoteDao
    abstract fun goalDao(): GoalDao
    abstract fun recurringReminderDao(): RecurringReminderDao
    abstract fun remindLinkDao(): RemindLinkDao
    abstract fun privateSpaceItemDao(): PrivateSpaceItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Passphrase for SQLCipher encryption
                val factory = SupportOpenHelperFactory("rk-secure-assistant-db-key-2025".toByteArray())
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rk_assistant_database_v2"
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
