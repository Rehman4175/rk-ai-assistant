package com.aistudio.rkaiassistant.data

import kotlinx.coroutines.flow.Flow

class DatabaseRepository(private val db: AppDatabase) {

    // Tasks
    val allTasks: Flow<List<Task>> = db.taskDao().getAllTasks()
    suspend fun insertTask(task: Task) {
        db.taskDao().insertTask(task)
    }
    suspend fun updateTask(task: Task) {
        db.taskDao().updateTask(task)
    }
    suspend fun deleteTask(task: Task) {
        db.taskDao().deleteTask(task)
    }
    suspend fun searchTasks(query: String): List<Task> = db.taskDao().searchTasks(query)

    // Reminders
    val activeReminders: Flow<List<Reminder>> = db.reminderDao().getActiveReminders()
    val allReminders: Flow<List<Reminder>> = db.reminderDao().getAllReminders()
    val fullReminderHistory: Flow<List<Reminder>> = db.reminderDao().getFullHistory()
    suspend fun insertReminder(reminder: Reminder): Long {
        return db.reminderDao().insertReminder(reminder)
    }
    suspend fun updateReminder(reminder: Reminder) {
        db.reminderDao().updateReminder(reminder)
    }
    suspend fun deleteReminder(reminder: Reminder) {
        db.reminderDao().deleteReminder(reminder)
    }

    // Habits
    val allHabits: Flow<List<Habit>> = db.habitDao().getAllHabits()
    suspend fun insertHabit(habit: Habit) {
        db.habitDao().insertHabit(habit)
    }
    suspend fun updateHabit(habit: Habit) {
        db.habitDao().updateHabit(habit)
    }
    suspend fun deleteHabit(habit: Habit) {
        db.habitDao().deleteHabit(habit)
    }

    // Water
    fun getWaterLogs(day: String): Flow<List<WaterLog>> = db.waterLogDao().getLogsByDay(day)
    fun getWaterSum(day: String): Flow<Int?> = db.waterLogDao().getWaterSumByDay(day)
    suspend fun insertWaterLog(log: WaterLog) = db.waterLogDao().insertLog(log)
    suspend fun updateWaterLog(log: WaterLog) = db.waterLogDao().updateLog(log)
    suspend fun deleteWaterLogById(id: Int) = db.waterLogDao().deleteLogById(id)

    // Expenses
    val allExpenses: Flow<List<Expense>> = db.expenseDao().getAllExpenses()
    suspend fun insertExpense(expense: Expense) {
        db.expenseDao().insertExpense(expense)
    }
    suspend fun updateExpense(expense: Expense) {
        db.expenseDao().updateExpense(expense)
    }
    suspend fun deleteExpense(expense: Expense) {
        db.expenseDao().deleteExpense(expense)
    }
    suspend fun searchExpenses(query: String): List<Expense> = db.expenseDao().searchExpenses(query)

    // Bills
    val allBills: Flow<List<Bill>> = db.billDao().getAllBills()
    suspend fun insertBill(bill: Bill) = db.billDao().insertBill(bill)
    suspend fun updateBill(bill: Bill) = db.billDao().updateBill(bill)
    suspend fun deleteBill(bill: Bill) = db.billDao().deleteBill(bill)

    // Calendar Events
    val allEvents: Flow<List<CalendarEvent>> = db.calendarEventDao().getAllEvents()
    fun getEventsByDay(day: String): Flow<List<CalendarEvent>> = db.calendarEventDao().getEventsByDay(day)
    suspend fun insertEvent(event: CalendarEvent) = db.calendarEventDao().insertEvent(event)
    suspend fun updateEvent(event: CalendarEvent) = db.calendarEventDao().updateEvent(event)
    suspend fun deleteEvent(event: CalendarEvent) = db.calendarEventDao().deleteEvent(event)

    // Diary
    val allDiaryEntries: Flow<List<DiaryEntry>> = db.diaryEntryDao().getAllDiaryEntries()
    suspend fun getDiaryEntryByDate(day: String): DiaryEntry? = db.diaryEntryDao().getEntryByDate(day)
    suspend fun insertDiaryEntry(entry: DiaryEntry) = db.diaryEntryDao().insertDiaryEntry(entry)
    suspend fun updateDiaryEntry(entry: DiaryEntry) = db.diaryEntryDao().updateDiaryEntry(entry)
    suspend fun deleteDiaryEntry(entry: DiaryEntry) = db.diaryEntryDao().deleteDiaryEntry(entry)
    suspend fun searchDiary(query: String): List<DiaryEntry> = db.diaryEntryDao().searchDiary(query)

    // Quick Notes
    val allNotes: Flow<List<QuickNote>> = db.quickNoteDao().getAllNotes()
    suspend fun insertNote(note: QuickNote) {
        db.quickNoteDao().insertNote(note)
    }
    suspend fun updateNote(note: QuickNote) {
        db.quickNoteDao().updateNote(note)
    }
    suspend fun deleteNote(note: QuickNote) {
        db.quickNoteDao().deleteNote(note)
    }
    suspend fun searchNotes(query: String): List<QuickNote> = db.quickNoteDao().searchNotes(query)

    // Personal Memories
    val allMemories: Flow<List<PersonalMemory>> = db.personalMemoryDao().getAllMemories()
    suspend fun insertMemory(memory: PersonalMemory) = db.personalMemoryDao().insertMemory(memory)
    suspend fun updateMemory(memory: PersonalMemory) = db.personalMemoryDao().updateMemory(memory)
    suspend fun deleteMemory(memory: PersonalMemory) = db.personalMemoryDao().deleteMemory(memory)
    suspend fun searchMemories(query: String): List<PersonalMemory> = db.personalMemoryDao().searchMemories(query)

    // Chat History
    fun getSessionMessages(chatId: String): Flow<List<ChatMessage>> = db.chatMessageDao().getSessionMessages(chatId)
    val chatSessions: Flow<List<String>> = db.chatMessageDao().getChatSessions()
    suspend fun insertChatMessage(msg: ChatMessage) = db.chatMessageDao().insertMessage(msg)
    suspend fun deleteChatSession(chatId: String) = db.chatMessageDao().deleteSession(chatId)

    // Smart Reminders
    val activeSmartReminders: Flow<List<SmartReminder>> = db.smartReminderDao().getActive()
    suspend fun getActiveSmartRemindersList(): List<SmartReminder> = db.smartReminderDao().getActiveList()
    suspend fun insertSmartReminder(smartReminder: SmartReminder) = db.smartReminderDao().insertSmartReminder(smartReminder)
    suspend fun updateSmartReminder(smartReminder: SmartReminder) = db.smartReminderDao().updateSmartReminder(smartReminder)
    suspend fun deleteSmartReminder(smartReminder: SmartReminder) = db.smartReminderDao().deleteSmartReminder(smartReminder)

    // Voice Notes
    val allVoiceNotes: Flow<List<VoiceNote>> = db.voiceNoteDao().getAllVoiceNotes()
    suspend fun insertVoiceNote(voiceNote: VoiceNote) = db.voiceNoteDao().insertVoiceNote(voiceNote)
    suspend fun updateVoiceNote(voiceNote: VoiceNote) = db.voiceNoteDao().updateVoiceNote(voiceNote)
    suspend fun deleteVoiceNote(voiceNote: VoiceNote) = db.voiceNoteDao().deleteVoiceNote(voiceNote)

    // Goals
    val allGoals: Flow<List<Goal>> = db.goalDao().getAllGoals()
    suspend fun insertGoal(goal: Goal) = db.goalDao().insertGoal(goal)
    suspend fun updateGoal(goal: Goal) = db.goalDao().updateGoal(goal)
    suspend fun deleteGoal(goal: Goal) = db.goalDao().deleteGoal(goal)

    // Recurring Reminders
    val activeRecurringReminders: Flow<List<RecurringReminder>> = db.recurringReminderDao().getActiveRecurringReminders()
    val allRecurringReminders: Flow<List<RecurringReminder>> = db.recurringReminderDao().getAll()
    suspend fun insertRecurringReminder(reminder: RecurringReminder) = db.recurringReminderDao().insert(reminder)
    suspend fun updateRecurringReminder(reminder: RecurringReminder) = db.recurringReminderDao().update(reminder)
    suspend fun deleteRecurringReminder(reminder: RecurringReminder) = db.recurringReminderDao().delete(reminder)

    // Remind Links
    val pendingRemindLinks: Flow<List<RemindLink>> = db.remindLinkDao().getPendingLinks()
    val allRemindLinks: Flow<List<RemindLink>> = db.remindLinkDao().getAllLinks()
    suspend fun insertRemindLink(link: RemindLink) = db.remindLinkDao().insertLink(link)
    suspend fun updateRemindLink(link: RemindLink) = db.remindLinkDao().updateLink(link)
    suspend fun deleteRemindLink(link: RemindLink) = db.remindLinkDao().deleteLink(link)

    // Private Space
    val allPrivateSpaceItems: Flow<List<PrivateSpaceItem>> = db.privateSpaceItemDao().getAllItems()
    suspend fun insertPrivateSpaceItem(item: PrivateSpaceItem) = db.privateSpaceItemDao().insertItem(item)
    suspend fun updatePrivateSpaceItem(item: PrivateSpaceItem) = db.privateSpaceItemDao().updateItem(item)
    suspend fun deletePrivateSpaceItem(item: PrivateSpaceItem) = db.privateSpaceItemDao().deleteItem(item)
    suspend fun searchPrivateSpaceItems(query: String): List<PrivateSpaceItem> = db.privateSpaceItemDao().searchItems(query)
}
