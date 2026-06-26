package com.aistudio.rkaiassistant.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY isCompleted ASC, priority DESC, dueDate ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY timestamp DESC")
    fun getAllTasksTotal(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%'")
    suspend fun searchTasks(query: String): List<Task>
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isAcknowledged = 0 AND isDeleted = 0 ORDER BY dueDateTime ASC")
    fun getActiveReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isDeleted = 0 ORDER BY dueDateTime ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders ORDER BY createdAt DESC")
    fun getFullHistory(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersList(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isDeleted = 0")
    fun getAllHabits(): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)
}

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_logs WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<WaterLog>>

    @Query("SELECT * FROM water_logs WHERE dayString = :dayString AND isDeleted = 0")
    fun getLogsByDay(dayString: String): Flow<List<WaterLog>>

    @Query("SELECT SUM(mlAmount) FROM water_logs WHERE dayString = :dayString AND isDeleted = 0")
    fun getWaterSumByDay(dayString: String): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WaterLog)

    @Update
    suspend fun updateLog(log: WaterLog)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY dateString DESC, timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpensesTotal(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    suspend fun searchExpenses(query: String): List<Expense>
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills WHERE isDeleted = 0 ORDER BY dueDayOfMonth ASC")
    fun getAllBills(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE dueDayOfMonth = :day AND isDeleted = 0")
    suspend fun getBillsByDay(day: Int): List<Bill>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill)

    @Update
    suspend fun updateBill(bill: Bill)

    @Delete
    suspend fun deleteBill(bill: Bill)
}

@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events WHERE isDeleted = 0 ORDER BY dateString ASC, timeString ASC")
    fun getAllEvents(): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM calendar_events WHERE dateString = :dateString AND isDeleted = 0 ORDER BY timeString ASC")
    fun getEventsByDay(dateString: String): Flow<List<CalendarEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEvent)

    @Update
    suspend fun updateEvent(event: CalendarEvent)

    @Delete
    suspend fun deleteEvent(event: CalendarEvent)
}

@Dao
interface DiaryEntryDao {
    @Query("SELECT * FROM diary_entries WHERE isDeleted = 0 ORDER BY dateString DESC")
    fun getAllDiaryEntries(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE dateString = :dateString LIMIT 1")
    suspend fun getEntryByDate(dateString: String): DiaryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntry(entry: DiaryEntry)

    @Update
    suspend fun updateDiaryEntry(entry: DiaryEntry)

    @Delete
    suspend fun deleteDiaryEntry(entry: DiaryEntry)

    @Query("SELECT * FROM diary_entries WHERE text LIKE '%' || :query || '%'")
    suspend fun searchDiary(query: String): List<DiaryEntry>
}

@Dao
interface QuickNoteDao {
    @Query("SELECT * FROM quick_notes WHERE isDeleted = 0 ORDER BY isPinned DESC, timestamp DESC")
    fun getAllNotes(): Flow<List<QuickNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: QuickNote)

    @Update
    suspend fun updateNote(note: QuickNote)

    @Delete
    suspend fun deleteNote(note: QuickNote)

    @Query("SELECT * FROM quick_notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    suspend fun searchNotes(query: String): List<QuickNote>
}

@Dao
interface PersonalMemoryDao {
    @Query("SELECT * FROM memories WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<PersonalMemory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: PersonalMemory)

    @Update
    suspend fun updateMemory(memory: PersonalMemory)

    @Delete
    suspend fun deleteMemory(memory: PersonalMemory)

    @Query("SELECT * FROM memories WHERE content LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    suspend fun searchMemories(query: String): List<PersonalMemory>
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE chatSessionId = :chatId ORDER BY timestamp ASC")
    fun getSessionMessages(chatId: String): Flow<List<ChatMessage>>

    @Query("SELECT DISTINCT chatSessionId FROM chat_messages ORDER BY timestamp DESC")
    fun getChatSessions(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE chatSessionId = :chatId")
    suspend fun deleteSession(chatId: String)
}

@Dao
interface SmartReminderDao {
    @Query("SELECT * FROM smart_reminders WHERE isAcknowledged = 0 AND isDeleted = 0 ORDER BY dueDateTime ASC")
    fun getActive(): Flow<List<SmartReminder>>

    @Query("SELECT * FROM smart_reminders WHERE isAcknowledged = 0 AND isDeleted = 0 ORDER BY dueDateTime ASC")
    suspend fun getActiveList(): List<SmartReminder>

    @Query("SELECT * FROM smart_reminders")
    fun getAll(): Flow<List<SmartReminder>>

    @Query("SELECT * FROM smart_reminders WHERE id = :id")
    suspend fun getById(id: Int): SmartReminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmartReminder(smartReminder: SmartReminder): Long

    @Update
    suspend fun updateSmartReminder(smartReminder: SmartReminder)

    @Delete
    suspend fun deleteSmartReminder(smartReminder: SmartReminder)
}

@Dao
interface VoiceNoteDao {
    @Query("SELECT * FROM voice_notes WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllVoiceNotes(): Flow<List<VoiceNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceNote(voiceNote: VoiceNote): Long

    @Update
    suspend fun updateVoiceNote(voiceNote: VoiceNote)

    @Delete
    suspend fun deleteVoiceNote(voiceNote: VoiceNote)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE isDeleted = 0 ORDER BY isDone ASC, deadline ASC")
    fun getAllGoals(): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)
}

@Dao
interface RecurringReminderDao {
    @Query("SELECT * FROM recurring_reminders WHERE isActive = 1 AND isDeleted = 0")
    fun getActiveRecurringReminders(): Flow<List<RecurringReminder>>

    @Query("SELECT * FROM recurring_reminders WHERE isDeleted = 0")
    fun getAll(): Flow<List<RecurringReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringReminder: RecurringReminder)

    @Update
    suspend fun update(recurringReminder: RecurringReminder)

    @Delete
    suspend fun delete(recurringReminder: RecurringReminder)
}

@Dao
interface RemindLinkDao {
    @Query("SELECT * FROM remind_links WHERE isAcknowledged = 0 AND isDeleted = 0 ORDER BY dueDateTime ASC")
    fun getPendingLinks(): Flow<List<RemindLink>>

    @Query("SELECT * FROM remind_links WHERE isDeleted = 0")
    fun getAllLinks(): Flow<List<RemindLink>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: RemindLink)

    @Update
    suspend fun updateLink(link: RemindLink)

    @Delete
    suspend fun deleteLink(link: RemindLink)
}

@Dao
interface PrivateSpaceItemDao {
    @Query("SELECT * FROM private_space_items WHERE isDeleted = 0 ORDER BY modifiedAt DESC")
    fun getAllItems(): Flow<List<PrivateSpaceItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PrivateSpaceItem)

    @Update
    suspend fun updateItem(item: PrivateSpaceItem)

    @Delete
    suspend fun deleteItem(item: PrivateSpaceItem)

    @Query("SELECT * FROM private_space_items WHERE title LIKE '%' || :query || '%'")
    suspend fun searchItems(query: String): List<PrivateSpaceItem>
}
