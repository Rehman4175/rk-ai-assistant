package com.aistudio.rkaiassistant.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.first

object SyncHelper {

    suspend fun generateFullSyncJson(db: AppDatabase): String {
        val root = JSONObject()
        
        // 1. Tasks
        val tasks = db.taskDao().getAllTasks().first()
        val taskArr = JSONArray()
        tasks.forEach { t ->
            val obj = JSONObject()
            obj.put("ID", "${t.id}#")
            obj.put("Title", t.title)
            obj.put("Priority", t.priority)
            obj.put("Status", if (t.isDeleted) "DELETED" else if (t.isCompleted) "Completed" else "Pending")
            obj.put("Created Date", t.createdDate)
            obj.put("Done Date", t.doneDate)
            obj.put("Label", t.label)
            obj.put("Due Date", t.dueDate)
            obj.put("Remarks", if (t.isDeleted) "Entry Deleted" else t.remarks)
            taskArr.put(obj)
        }
        root.put("Tasks", taskArr)

        // 2. Reminders
        val reminders = db.reminderDao().getAllReminders().first()
        val remArr = JSONArray()
        reminders.forEach { r ->
            val obj = JSONObject()
            obj.put("ID", "${r.id}#")
            obj.put("Trigger Time", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(r.dueDateTime)))
            obj.put("Reminder Text", r.title)
            obj.put("Recurrence", r.recurrence)
            obj.put("Status", if (r.isDeleted) "DELETED" else if (r.isAcknowledged) "Acknowledged" else "Active")
            obj.put("Created On", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(r.createdAt)))
            obj.put("Remarks", if (r.isDeleted) "Entry Deleted" else r.remarks)
            remArr.put(obj)
        }
        root.put("Reminders", remArr)

        // 3. Expenses
        val expenses = db.expenseDao().getAllExpenses().first()
        val expArr = JSONArray()
        expenses.forEach { e ->
            val obj = JSONObject()
            obj.put("ID", "${e.id}#")
            obj.put("Date", e.dateString)
            obj.put("Time", SimpleDateFormat("HH:mm", Locale.US).format(Date(e.timestamp)))
            obj.put("Amount", e.amount)
            obj.put("Description", e.title)
            obj.put("Category", e.category)
            obj.put("Type", if (e.isIncome) "Income" else "Expense")
            obj.put("Status", if (e.isDeleted) "DELETED" else "Valid")
            obj.put("Remarks", if (e.isDeleted) "Entry Deleted" else e.remarks)
            expArr.put(obj)
        }
        root.put("Expenses", expArr)

        // 4. Habits
        val habits = db.habitDao().getAllHabits().first()
        val habitArr = JSONArray()
        habits.forEach { h ->
            val obj = JSONObject()
            obj.put("ID", "${h.id}#")
            obj.put("Habit Name", h.name)
            obj.put("Emoji", h.emoji)
            obj.put("Streak", h.streakCount)
            obj.put("Best Streak", h.bestStreak)
            obj.put("Target", h.targetPerDay)
            obj.put("Status", if (h.isDeleted) "DELETED" else "Active")
            obj.put("Remarks", if (h.isDeleted) "Entry Deleted" else h.remarks)
            habitArr.put(obj)
        }
        root.put("Habits", habitArr)

        // 5. Diary
        val diaries = db.diaryEntryDao().getAllDiaryEntries().first()
        val diaryArr = JSONArray()
        diaries.forEach { d ->
            val obj = JSONObject()
            obj.put("ID", "${d.id}#")
            obj.put("Date", d.dateString)
            obj.put("Time", SimpleDateFormat("HH:mm", Locale.US).format(Date(d.timestamp)))
            obj.put("Entry Text", d.text)
            obj.put("Mood", d.mood)
            obj.put("Status", if (d.isDeleted) "DELETED" else "Logged")
            obj.put("Remarks", if (d.isDeleted) "Entry Deleted" else d.remarks)
            diaryArr.put(obj)
        }
        root.put("Diary", diaryArr)

        // 6. Bills
        val bills = db.billDao().getAllBills().first()
        val billArr = JSONArray()
        bills.forEach { b ->
            val obj = JSONObject()
            obj.put("ID", "${b.id}#")
            obj.put("Bill Name", b.name)
            obj.put("Amount", b.amount)
            obj.put("Due Day", b.dueDayOfMonth)
            obj.put("Category", b.category)
            obj.put("Auto Pay", if (b.isAutoPay) "Yes" else "No")
            obj.put("Status", if (b.isDeleted) "DELETED" else "Active")
            obj.put("Remarks", if (b.isDeleted) "Entry Deleted" else b.remarks)
            billArr.put(obj)
        }
        root.put("Bills", billArr)

        // 7. Events
        val events = db.calendarEventDao().getAllEvents().first()
        val eventArr = JSONArray()
        events.forEach { v ->
            val obj = JSONObject()
            obj.put("ID", "${v.id}#")
            obj.put("Title", v.title)
            obj.put("Date", v.dateString)
            obj.put("Time", v.timeString)
            obj.put("Location", v.location)
            obj.put("Type", v.type)
            obj.put("Status", if (v.isDeleted) "DELETED" else "Scheduled")
            obj.put("Remarks", if (v.isDeleted) "Entry Deleted" else v.remarks)
            eventArr.put(obj)
        }
        root.put("Events", eventArr)

        // 8. Water
        val waters = db.waterLogDao().getAllLogs().first()
        val waterArr = JSONArray()
        waters.forEach { w ->
            val obj = JSONObject()
            obj.put("ID", "${w.id}#")
            obj.put("Date", w.dayString)
            obj.put("Time", SimpleDateFormat("HH:mm", Locale.US).format(Date(w.timestamp)))
            obj.put("Quantity (ML)", w.mlAmount)
            obj.put("Status", if (w.isDeleted) "DELETED" else "Logged")
            obj.put("Remarks", if (w.isDeleted) "Entry Deleted" else w.remarks)
            waterArr.put(obj)
        }
        root.put("Water Intake", waterArr)

        // 9. Quick Notes
        val notes = db.quickNoteDao().getAllNotes().first()
        val noteArr = JSONArray()
        notes.forEach { n ->
            val obj = JSONObject()
            obj.put("ID", "${n.id}#")
            obj.put("Date", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(n.timestamp)))
            obj.put("Note Content", n.content)
            obj.put("Tag", n.tag)
            obj.put("Pinned", if (n.isPinned) "Yes" else "No")
            obj.put("Status", if (n.isDeleted) "DELETED" else "Saved")
            obj.put("Remarks", if (n.isDeleted) "Entry Deleted" else n.remarks)
            noteArr.put(obj)
        }
        root.put("Quick Notes", noteArr)

        // 10. Smart Reminders
        val smartReminders = db.smartReminderDao().getAll().first()
        val smartArr = JSONArray()
        smartReminders.forEach { s ->
            val obj = JSONObject()
            obj.put("ID", "${s.id}#")
            obj.put("Title", s.title)
            obj.put("Priority", s.priority)
            obj.put("Due", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(s.dueDateTime)))
            obj.put("Repeats", "${s.currentRepeat}/${s.maxRepeats}")
            obj.put("Status", if (s.isDeleted) "DELETED" else if (s.isAcknowledged) "Done" else "Active")
            smartArr.put(obj)
        }
        root.put("Smart Reminders", smartArr)

        // 11. Voice Notes
        val voiceNotes = db.voiceNoteDao().getAllVoiceNotes().first()
        val voiceArr = JSONArray()
        voiceNotes.forEach { v ->
            val obj = JSONObject()
            obj.put("ID", "${v.id}#")
            obj.put("Transcription", v.transcription)
            obj.put("Duration", "${v.duration / 1000}s")
            obj.put("Date", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(v.timestamp)))
            obj.put("Status", if (v.isDeleted) "DELETED" else v.status)
            voiceArr.put(obj)
        }
        root.put("Voice Notes", voiceArr)

        // 12. Goals
        val goals = db.goalDao().getAllGoals().first()
        val goalArr = JSONArray()
        goals.forEach { g ->
            val obj = JSONObject()
            obj.put("ID", "${g.id}#")
            obj.put("Title", g.title)
            obj.put("Progress", "${g.progress}%")
            obj.put("Deadline", g.deadline)
            obj.put("Status", if (g.isDeleted) "DELETED" else if (g.isDone) "Done" else "Active")
            goalArr.put(obj)
        }
        root.put("Goals", goalArr)

        return root.toString()
    }

    suspend fun performSync(db: AppDatabase, scriptUrl: String): Boolean {
        if (scriptUrl.isBlank()) return false
        return try {
            val payload = generateFullSyncJson(db)
            val success = GoogleSheetsService.sync(payload, scriptUrl)
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun generateDriveBackupJson(db: AppDatabase, context: android.content.Context): String {
        val root = JSONObject()
        val cryptoManager = CryptoManager()
        
        root.put("tasks", JSONArray(db.taskDao().getAllTasksTotal().first().map { t ->
            JSONObject().apply {
                put("id", t.id); put("title", t.title); put("isCompleted", t.isCompleted); put("priority", t.priority)
                put("label", t.label); put("dueDate", t.dueDate); put("notes", t.notes)
                put("isRepeating", t.isRepeating); put("repeatInterval", t.repeatInterval)
                put("createdDate", t.createdDate); put("doneDate", t.doneDate)
                put("isDeleted", t.isDeleted); put("remarks", t.remarks); put("timestamp", t.timestamp)
            }
        }))
        
        root.put("reminders", JSONArray(db.reminderDao().getFullHistory().first().map { r ->
            JSONObject().apply {
                put("id", r.id); put("title", r.title); put("dueDateTime", r.dueDateTime); put("recurrence", r.recurrence)
                put("isAcknowledged", r.isAcknowledged); put("createdAt", r.createdAt)
                put("chatId", r.chatId); put("lastFired", r.lastFired); put("remarks", r.remarks)
                put("isDeleted", r.isDeleted)
            }
        }))
        
        root.put("expenses", JSONArray(db.expenseDao().getAllExpensesTotal().first().map { e ->
            JSONObject().apply {
                put("id", e.id); put("amount", e.amount); put("title", e.title); put("isIncome", e.isIncome)
                put("category", e.category); put("dateString", e.dateString); put("timestamp", e.timestamp)
                put("isDeleted", e.isDeleted); put("remarks", e.remarks)
            }
        }))

        root.put("habits", JSONArray(db.habitDao().getAllHabits().first().map { h ->
            JSONObject().apply {
                put("name", h.name); put("type", h.type); put("emoji", h.emoji)
                put("loggedDaysCommaSeparated", h.loggedDaysCommaSeparated)
                put("streakCount", h.streakCount); put("bestStreak", h.bestStreak)
                put("lastLoggedTimestamp", h.lastLoggedTimestamp); put("targetPerDay", h.targetPerDay)
                put("isDeleted", h.isDeleted); put("remarks", h.remarks)
            }
        }))

        root.put("waterLogs", JSONArray(db.waterLogDao().getAllLogs().first().map { w ->
            JSONObject().apply {
                put("mlAmount", w.mlAmount); put("timestamp", w.timestamp)
                put("dayString", w.dayString); put("isDeleted", w.isDeleted); put("remarks", w.remarks)
            }
        }))

        root.put("bills", JSONArray(db.billDao().getAllBills().first().map { b ->
            JSONObject().apply {
                put("name", b.name); put("amount", b.amount); put("category", b.category)
                put("dueDayOfMonth", b.dueDayOfMonth); put("paidMonthsCommaSeparated", b.paidMonthsCommaSeparated)
                put("isAutoPay", b.isAutoPay); put("paymentMethod", b.paymentMethod); put("notes", b.notes)
                put("createdAt", b.createdAt); put("isDeleted", b.isDeleted); put("remarks", b.remarks)
            }
        }))

        root.put("events", JSONArray(db.calendarEventDao().getAllEvents().first().map { v ->
            JSONObject().apply {
                put("title", v.title); put("dateString", v.dateString); put("timeString", v.timeString)
                put("location", v.location); put("notes", v.notes); put("type", v.type)
                put("isAiGenerated", v.isAiGenerated); put("createdAt", v.createdAt)
                put("remindDayBefore", v.remindDayBefore); put("isDeleted", v.isDeleted); put("remarks", v.remarks)
            }
        }))

        root.put("diaryEntries", JSONArray(db.diaryEntryDao().getAllDiaryEntries().first().map { d ->
            JSONObject().apply {
                put("dateString", d.dateString); put("text", d.text); put("mood", d.mood)
                put("photoPath", d.photoPath); put("timestamp", d.timestamp); put("isDeleted", d.isDeleted)
                put("remarks", d.remarks)
            }
        }))

        root.put("notes", JSONArray(db.quickNoteDao().getAllNotes().first().map { n ->
            JSONObject().apply {
                put("title", n.title); put("content", n.content); put("isPinned", n.isPinned)
                put("isFavorite", n.isFavorite); put("timestamp", n.timestamp); put("tag", n.tag)
                put("isDeleted", n.isDeleted); put("remarks", n.remarks)
            }
        }))

        root.put("memories", JSONArray(db.personalMemoryDao().getAllMemories().first().map { m ->
            JSONObject().apply {
                put("content", m.content); put("category", m.category); put("timestamp", m.timestamp)
                put("isDeleted", m.isDeleted); put("remarks", m.remarks)
            }
        }))

        root.put("smartReminders", JSONArray(db.smartReminderDao().getAll().first().map { s ->
            JSONObject().apply {
                put("title", s.title); put("dueDateTime", s.dueDateTime); put("priority", s.priority)
                put("repeatIntervalMinutes", s.repeatIntervalMinutes); put("maxRepeats", s.maxRepeats)
                put("currentRepeat", s.currentRepeat); put("isAcknowledged", s.isAcknowledged)
                put("isDeleted", s.isDeleted); put("remarks", s.remarks)
            }
        }))

        root.put("voiceNotes", JSONArray(db.voiceNoteDao().getAllVoiceNotes().first().map { v ->
            JSONObject().apply {
                put("filePath", v.filePath); put("transcription", v.transcription); put("duration", v.duration)
                put("timestamp", v.timestamp); put("isTranscribed", v.isTranscribed); put("status", v.status)
                put("category", v.category); put("isDeleted", v.isDeleted); put("remarks", v.remarks)
            }
        }))

        root.put("goals", JSONArray(db.goalDao().getAllGoals().first().map { g ->
            JSONObject().apply {
                put("title", g.title); put("progress", g.progress); put("isDone", g.isDone)
                put("deadline", g.deadline); put("createdAt", g.createdAt); put("milestones", g.milestones)
                put("isDeleted", g.isDeleted); put("remarks", g.remarks); put("timestamp", g.timestamp)
            }
        }))

        root.put("recurringReminders", JSONArray(db.recurringReminderDao().getAll().first().map { r ->
            JSONObject().apply {
                put("title", r.title); put("type", r.type); put("time", r.time)
                put("isActive", r.isActive); put("createdAt", r.createdAt); put("isDeleted", r.isDeleted)
                put("remarks", r.remarks)
            }
        }))

        root.put("remindLinks", JSONArray(db.remindLinkDao().getAllLinks().first().map { l ->
            JSONObject().apply {
                put("chatId", l.chatId); put("text", l.text); put("link", l.link)
                put("dueDateTime", l.dueDateTime); put("originalMsgId", l.originalMsgId)
                put("isAcknowledged", l.isAcknowledged); put("createdAt", l.createdAt)
                put("isDeleted", l.isDeleted); put("remarks", l.remarks)
            }
        }))

        root.put("privateSpaceItems", JSONArray(db.privateSpaceItemDao().getAllItems().first().map { p ->
            JSONObject().apply {
                put("title", p.title); put("content", cryptoManager.decryptString(p.content)); put("category", p.category)
                put("isPinned", p.isPinned); put("photoPath", p.photoPath); put("createdAt", p.createdAt)
                put("modifiedAt", p.modifiedAt); put("isDeleted", p.isDeleted); put("remarks", p.remarks)
            }
        }))

        return root.toString(2)
    }
}
