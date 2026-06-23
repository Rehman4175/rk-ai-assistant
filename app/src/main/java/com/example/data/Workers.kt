package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

fun scheduleAllWorkers(context: Context) {
    // 1. Schedule Smart Reminder Worker as Periodic fallback (Every 1 hour)
    val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.HOURS)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "SmartReminderWorker",
        ExistingPeriodicWorkPolicy.KEEP,
        reminderRequest
    )

    // 2. Schedule Weekly Review Worker (Sunday 9 PM)
    scheduleWeeklyReview(context)

    // 3. Schedule Budget Alert Worker (Daily 9 AM)
    scheduleBudgetAlert(context)

    // 4. Schedule Daily Briefing Worker (Daily 8 AM)
    scheduleDailyBriefing(context)

    // 5. Schedule Recurring Reminder Checker (Daily 1 AM)
    scheduleRecurringCheck(context)

    // 6. Periodic Sync Worker (Every 1 hour)
    val syncRequest = PeriodicWorkRequestBuilder<GoogleSheetSyncWorker>(1, TimeUnit.HOURS)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "PeriodicSyncWorker",
        ExistingPeriodicWorkPolicy.KEEP,
        syncRequest
    )
}

fun scheduleRecurringCheck(context: Context) {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
    
    val delay = target.timeInMillis - now.timeInMillis
    val request = OneTimeWorkRequestBuilder<DailyRecurringWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork("DailyRecurringWorker", ExistingWorkPolicy.REPLACE, request)
}

fun scheduleWeeklyReview(context: Context) {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        set(Calendar.HOUR_OF_DAY, 21)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (target.before(now)) {
        target.add(Calendar.WEEK_OF_YEAR, 1)
    }
    val delayMs = target.timeInMillis - now.timeInMillis
    val request = OneTimeWorkRequestBuilder<WeeklyReviewWorker>()
        .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        "WeeklyReviewWorker",
        ExistingWorkPolicy.REPLACE,
        request
    )
}

fun scheduleBudgetAlert(context: Context) {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (target.before(now)) {
        target.add(Calendar.DAY_OF_YEAR, 1)
    }
    val delayMs = target.timeInMillis - now.timeInMillis
    val request = OneTimeWorkRequestBuilder<BudgetAlertWorker>()
        .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        "BudgetAlertWorker",
        ExistingWorkPolicy.REPLACE,
        request
    )
}

fun scheduleDailyBriefing(context: Context) {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 8)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (target.before(now)) {
        target.add(Calendar.DAY_OF_YEAR, 1)
    }
    val delayMs = target.timeInMillis - now.timeInMillis
    val request = OneTimeWorkRequestBuilder<DailyBriefingWorker>()
        .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        "DailyBriefingWorker",
        ExistingWorkPolicy.REPLACE,
        request
    )
}

// Helper to send a simple notifications
@android.annotation.SuppressLint("MissingPermission")
fun sendAndroidNotification(context: Context, id: Int, channelId: String, channelName: String, title: String, text: String, actions: List<NotificationCompat.Action> = emptyList()) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                "android.permission.POST_NOTIFICATIONS"
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
    }

    val prefs = SecurePrefHelper(context)
    val customTuneUri = prefs.getNotificationTune()
    
    // Dynamic channel ID based on sound to force updates
    val finalChannelId = if (customTuneUri.isNotBlank()) {
        val soundHash = try { 
            java.security.MessageDigest.getInstance("MD5")
                .digest(customTuneUri.toByteArray())
                .joinToString("") { "%02x".format(it) }
                .take(8)
        } catch (e: Exception) { 
            customTuneUri.hashCode().toString() 
        }
        "${channelId}_$soundHash"
    } else {
        channelId
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(finalChannelId, channelName, importance).apply {
            description = "RK Assistant alerts"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500)
            setBypassDnd(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            
            if (customTuneUri.isNotBlank()) {
                try {
                    val soundUri = Uri.parse(customTuneUri)
                    val audioAttributes = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setSound(soundUri, audioAttributes)
                } catch (_: Exception) {}
            }
        }
        notificationManager.createNotificationChannel(channel)
    }

    val launcherIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val contentPI = PendingIntent.getActivity(
        context, 
        id, 
        launcherIntent, 
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, finalChannelId)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle(title)
        .setContentText(text)
        .setAutoCancel(true)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setContentIntent(contentPI)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOngoing(false)
        .setFullScreenIntent(contentPI, true)
    
    if (customTuneUri.isNotBlank()) {
        builder.setSound(Uri.parse(customTuneUri))
    } else {
        builder.setDefaults(NotificationCompat.DEFAULT_ALL)
    }

    actions.forEach { builder.addAction(it) }

    val notification = builder.build()
    notificationManager.notify(id, notification)
}

// WORKER: Google Sheet Sync Worker
class GoogleSheetSyncWorker(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val prefs = SecurePrefHelper(context)
        val scriptUrl = prefs.getGoogleScriptUrl()
        if (scriptUrl.isBlank()) return Result.failure()

        val db = AppDatabase.getDatabase(context)
        val success = SyncHelper.performSync(db, scriptUrl)
        
        return if (success) Result.success() else Result.retry()
    }
}

// WORKER 1: Smart Reminder checking (Periodic fallback)
class ReminderWorker(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(context)
        val now = System.currentTimeMillis()

        // 1. Handle regular Reminders
        val activeReminders = db.reminderDao().getAllRemindersList()
        activeReminders.forEach { reminder ->
            if (!reminder.isAcknowledged && !reminder.isDeleted && now >= reminder.dueDateTime) {
                sendReminderNotification(context, reminder.id, reminder.title, "Reminder")
                db.reminderDao().updateReminder(reminder.copy(isAcknowledged = true))
            }
        }

        // 2. Handle SmartReminders
        val smartReminders = db.smartReminderDao().getActiveList()
        smartReminders.forEach { srem ->
            if (now >= srem.dueDateTime) {
                sendReminderNotification(context, srem.id + 50000, srem.title, "Smart Reminder")
                
                if (srem.currentRepeat < srem.maxRepeats) {
                    val nextTime = now + (srem.repeatIntervalMinutes * 60 * 1000L)
                    db.smartReminderDao().updateSmartReminder(srem.copy(
                        dueDateTime = nextTime,
                        currentRepeat = srem.currentRepeat + 1
                    ))
                } else {
                    db.smartReminderDao().updateSmartReminder(srem.copy(isAcknowledged = true))
                }
            }
        }

        return Result.success()
    }

    private fun sendReminderNotification(context: Context, id: Int, title: String, type: String) {
        val completeIntent = Intent(context, SmartReminderReceiver::class.java).apply {
            action = "com.aistudio.rkaiassistant.COMPLETE_REMINDER"
            putExtra("REMINDER_ID", id)
            putExtra("TYPE", type)
        }
        val pendingComplete = PendingIntent.getBroadcast(
            context, id, completeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val completeAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_save, "Acknowledge", pendingComplete
        ).build()

        val snoozeIntent = Intent(context, SmartReminderReceiver::class.java).apply {
            action = "com.aistudio.rkaiassistant.SNOOZE_REMINDER"
            putExtra("REMINDER_ID", id)
            putExtra("TYPE", type)
        }
        val pendingSnooze = PendingIntent.getBroadcast(
            context, id + 10000, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozeAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_lock_idle_alarm, "Snooze 10m", pendingSnooze
        ).build()

        sendAndroidNotification(
            context = context,
            id = id,
            channelId = "reminders_channel",
            channelName = "Reminders",
            title = "🔔 $type: $title",
            text = "It's time for: $title",
            actions = listOf(completeAction, snoozeAction)
        )
    }
}

// WORKER 2: Weekly Review (Sunday PM)
class WeeklyReviewWorker(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(context)

        // Gather statistics from flows
        val allTasks = db.taskDao().getAllTasks().first()
        val allExpenses = db.expenseDao().getAllExpenses().first()
        val allDiary = db.diaryEntryDao().getAllDiaryEntries().first()

        val completedCount = allTasks.count { it.isCompleted }
        val pendingCount = allTasks.count { !it.isCompleted }
        
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 3600 * 1000L)
        val weeklyExpenses = allExpenses.filter { it.timestamp >= oneWeekAgo && !it.isIncome }.sumOf { it.amount }
        val diaryCountThisWeek = allDiary.count { it.timestamp >= oneWeekAgo }

        val textReport = "Tasks done: $completedCount | Pending: $pendingCount | Expenses: Rs $weeklyExpenses | Diary logs: $diaryCountThisWeek"

        sendAndroidNotification(
            context = context,
            id = 9991,
            channelId = "weekly_review_channel",
            channelName = "Weekly Reviews",
            title = "📊 Your Weekly Review Report",
            text = textReport
        )

        // Reschedule for next Sunday
        scheduleWeeklyReview(context)
        return Result.success()
    }
}

// WORKER 3: Budget and Bills alerts (Daily 9 AM)
class BudgetAlertWorker(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(context)
        val prefs = SecurePrefHelper(context)

        // Expense budget exceed warning
        val allExpenses = db.expenseDao().getAllExpenses().first()
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 3600 * 1000L)
        val weeklyExpenseSum = allExpenses.filter { it.timestamp >= oneWeekAgo && !it.isIncome }.sumOf { it.amount }

        if (weeklyExpenseSum > prefs.getExpenseBudget()) {
            sendAndroidNotification(
                context = context,
                id = 9992,
                channelId = "budget_channel",
                channelName = "Budget Warnings",
                title = "⚠️ Weekly Budget Limit Exceeded",
                text = "Spent Rs $weeklyExpenseSum this week, exceeding weekly budget allowance!"
            )
        }

        // Bill alerts within 2 days
        val allBills = db.billDao().getAllBills().first()
        val calendar = Calendar.getInstance()
        val currentDayString = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        allBills.forEach { bill ->
            val paidMonths = bill.paidMonthsCommaSeparated.split(",").filter { it.isNotBlank() }
            val isPaidThisMonth = paidMonths.contains(currentDayString)
            if (!isPaidThisMonth) {
                val daysRemaining = bill.dueDayOfMonth - dayOfMonth
                if (daysRemaining in 0..2) {
                    sendAndroidNotification(
                        context = context,
                        id = 8880 + bill.id,
                        channelId = "bills_channel",
                        channelName = "Bill Warnings",
                        title = "⏳ Urgent Bill Due soon: " + bill.name,
                        text = "Pay Rs ${bill.amount} in $daysRemaining days (due date of month: ${bill.dueDayOfMonth})"
                    )
                }
            }
        }

        // Reschedule for next day 9 AM
        scheduleBudgetAlert(context)
        return Result.success()
    }
}

// WORKER 4: Daily Briefing (Daily 8 AM)
class DailyBriefingWorker(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(context)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        
        // 1. Get stats
        val pendingTasks = db.taskDao().getAllTasks().first().filter { !it.isCompleted }
        val allBills = db.billDao().getAllBills().first()
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val currentMonth = today.take(7)
        
        val dueBills = allBills.filter { bill ->
            val paidMonths = bill.paidMonthsCommaSeparated.split(",").filter { it.isNotBlank() }
            !paidMonths.contains(currentMonth) && (bill.dueDayOfMonth - dayOfMonth in 0..2)
        }
        
        val waterIntake = db.waterLogDao().getWaterSumByDay(today).first() ?: 0
        val waterGoal = SecurePrefHelper(context).getWaterGoal()

        // 2. Prepare context for Gemini
        val briefingContext = """
            Tasks pending: ${pendingTasks.size} (${pendingTasks.take(3).joinToString { it.title }})
            Bills due soon: ${dueBills.size}
            Water today: ${waterIntake}ml / ${waterGoal}ml
            Date: $today
        """.trimIndent()

        // 3. Get AI Briefing
        val prompt = "Generate a short, energetic 'Jarvis-style' morning briefing in Hinglish based on this data: $briefingContext. Keep it under 40 words and mention the number of tasks."
        val response = if (GeminiService.isApiKeyConfigured()) {
            GeminiService.chat(prompt, "You are RK, a smart personal assistant.") ?: "Good morning Boss! Aapke ${pendingTasks.size} tasks pending hain, check kar lijiye."
        } else {
            "Good morning Boss! Aaj ${pendingTasks.size} tasks pending hain. Paani piyo aur kaam shuru karo!"
        }

        // 4. Notify
        sendAndroidNotification(
            context = context,
            id = 9993,
            channelId = "daily_briefing_channel",
            channelName = "Daily Briefings",
            title = "🌅 Good Morning Boss!",
            text = response
        )

        scheduleDailyBriefing(context)
        return Result.success()
    }
}

// WORKER 5: Daily Recurring Reminder Checker
class DailyRecurringWorker(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(context)
        val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        
        // Find recurring reminders that should fire today
        val recurring = db.recurringReminderDao().getAll().first()
        recurring.forEach { rem ->
            // Logic: If 'time' field contains day of month like "Day 3" or similar
            // For now, simple match (this can be expanded with a specific field)
            if (rem.isActive && rem.time.contains("Day $dayOfMonth")) {
                sendAndroidNotification(
                    context, 
                    rem.id + 20000, 
                    "recurring_channel", 
                    "Bill Reminders", 
                    "📅 Recurring: ${rem.title}", 
                    "Today is day $dayOfMonth. Don't forget: ${rem.title}"
                )
            }
        }
        
        scheduleRecurringCheck(context)
        return Result.success()
    }
}

// BROADCAST RECEIVER for complete reminder action & boot reschedule
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
