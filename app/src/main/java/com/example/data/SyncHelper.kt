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
}
