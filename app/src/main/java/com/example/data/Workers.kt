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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

fun scheduleAllWorkers(context: Context) {
    // 1. Schedule Smart Reminder Worker
    // Note: PeriodicWorkRequest has a minimum interval of 15 minutes.
    // We use a cascading OneTimeWorkRequest for "every minute" precision.
    val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInitialDelay(30, TimeUnit.SECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        "SmartReminderWorker",
        ExistingWorkPolicy.REPLACE,
        reminderRequest
    )

    // 2. Schedule Weekly Review Worker (Sunday 9 PM)
    scheduleWeeklyReview(context)

    // 3. Schedule Budget Alert Worker (Daily 9 AM)
    scheduleBudgetAlert(context)

    // 4. Periodic Sync Worker (Every 1 hour)
    val syncRequest = PeriodicWorkRequestBuilder<GoogleSheetSyncWorker>(1, TimeUnit.HOURS)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "PeriodicSyncWorker",
        ExistingPeriodicWorkPolicy.KEEP,
        syncRequest
    )
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

// Helper to send a simple notifications
fun sendAndroidNotification(context: Context, id: Int, channelId: String, channelName: String, title: String, text: String, actions: List<NotificationCompat.Action> = emptyList()) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val prefs = SecurePrefHelper(context)
    val customTuneUri = prefs.getNotificationTune()
    
    // Dynamic channel ID based on sound to force updates
    val finalChannelId = if (customTuneUri.isNotBlank()) {
        "${channelId}_${customTuneUri.hashCode()}"
    } else {
        channelId
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(finalChannelId, channelName, importance).apply {
            description = "RK Assistant alerts"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            
            if (customTuneUri.isNotBlank()) {
                try {
                    val soundUri = Uri.parse(customTuneUri)
                    val audioAttributes = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setSound(soundUri, audioAttributes)
                } catch (e: Exception) {}
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

// WORKER 1: Smart Reminder checking (cascades self every 15-30 seconds for better precision)
class ReminderWorker(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(context)
        val activeReminders = db.reminderDao().getAllRemindersList() // Check all reminders
        val now = System.currentTimeMillis()

        activeReminders.forEach { reminder ->
            if (!reminder.isAcknowledged && !reminder.isDeleted && now >= reminder.dueDateTime) {
                // Send rich notification
                val completeIntent = Intent(context, SmartReminderReceiver::class.java).apply {
                    action = "COMPLETE_REMINDER"
                    putExtra("REMINDER_ID", reminder.id)
                }
                val pendingComplete = PendingIntent.getBroadcast(
                    context,
                    reminder.id,
                    completeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val completeAction = NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_save,
                    "Acknowledge",
                    pendingComplete
                ).build()

                sendAndroidNotification(
                    context = context,
                    id = reminder.id,
                    channelId = "reminders_channel",
                    channelName = "Reminders",
                    title = "🔔 Reminder: " + reminder.title,
                    text = "It's time for: ${reminder.title}",
                    actions = listOf(completeAction)
                )

                // Mark as acknowledged so it doesn't fire again immediately
                db.reminderDao().updateReminder(reminder.copy(isAcknowledged = true))
            }
        }

        // Reschedule work execution in 30 seconds for higher precision
        val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "SmartReminderWorker",
            ExistingWorkPolicy.REPLACE,
            reminderRequest
        )

        return Result.success()
    }
}

// WORKER 2: Weekly Review (Sunday PM)
class WeeklyReviewWorker(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(context)

        // Gather statistics from flows
        val allTasks = db.taskDao().getAllTasks().first()
        val allHabits = db.habitDao().getAllHabits().first()
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

// BROADCAST RECEIVER for complete reminder action & boot reschedule
class SmartReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "COMPLETE_REMINDER") {
            val reminderId = intent.getIntExtra("REMINDER_ID", -1)
            if (reminderId != -1) {
                // Cancel notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(reminderId)

                // Update in DB inside thread/coroutine
                val db = AppDatabase.getDatabase(context)
                CoroutineScope(Dispatchers.IO).launch {
                    val reminder = db.reminderDao().getReminderById(reminderId)
                    if (reminder != null) {
                        db.reminderDao().updateReminder(reminder.copy(isAcknowledged = true))
                    }
                    val smartReminder = db.smartReminderDao().getById(reminderId)
                    if (smartReminder != null) {
                        db.smartReminderDao().updateSmartReminder(smartReminder.copy(isAcknowledged = true))
                    }
                }
                Toast.makeText(context, "Smart Reminder completed and stopped.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
