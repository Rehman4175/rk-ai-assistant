package com.aistudio.rkaiassistant.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.SecureRandom

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
                val prefs = SecurePrefHelper(context)
                var passphrase = prefs.getDbPassphrase()
                
                if (passphrase == null) {
                    passphrase = ByteArray(32)
                    SecureRandom().nextBytes(passphrase)
                    prefs.saveDbPassphrase(passphrase)
                }

                try {
                    val factory = SupportOpenHelperFactory(passphrase)
                    
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "rk_assistant_database_v3"
                    )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    android.util.Log.e("RKAI", "SQLCipher failed, falling back to unencrypted database!", e)
                    // Fallback to unencrypted database so the app at least opens and works
                    val fallbackInstance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "rk_assistant_database_unencrypted"
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                    INSTANCE = fallbackInstance
                    fallbackInstance
                } catch (e: Throwable) {
                    // Catch LinkageErrors etc.
                    android.util.Log.e("RKAI", "Critical failure in database init!", e)
                    // We try one last time with no encryption if it's a native loading issue
                    val finalFallback = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "rk_assistant_database_unencrypted_final"
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                    INSTANCE = finalFallback
                    finalFallback
                }
            }
        }
    }
}
