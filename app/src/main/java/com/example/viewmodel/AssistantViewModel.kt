package com.example.viewmodel

import java.io.InputStream
import java.io.OutputStream
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen {
    Dashboard, Chat, Tasks, Reminders, Habits, Water, Expenses, Bills, Calendar, Diary, Notes, Search, Settings, SmartReminders, VoiceNotes, WeeklyReview, Goals, RecurringReminders, RemindLinks, PrivateSpace
}

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = DatabaseRepository(db)
    val prefs = SecurePrefHelper(application)

    // Current Screen
    val currentScreen = MutableStateFlow(AppScreen.Dashboard)

    // Security Lock State
    val isLocked = MutableStateFlow(prefs.isPinEnabled())
    val pinError = MutableStateFlow<String?>(null)

    // Private Space Lock State
    val isPrivateSpaceLocked = MutableStateFlow(prefs.hasPrivateSpacePassword())
    val privateSpaceError = MutableStateFlow<String?>(null)
    val hasPrivateSpacePassword = MutableStateFlow(prefs.hasPrivateSpacePassword())

    // State Flows for DB Data
    val tasks: StateFlow<List<Task>> = repository.allTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reminders: StateFlow<List<Reminder>> = repository.allReminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val habits: StateFlow<List<Habit>> = repository.allHabits.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses: StateFlow<List<Expense>> = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bills: StateFlow<List<Bill>> = repository.allBills.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val events: StateFlow<List<CalendarEvent>> = repository.allEvents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val diaryEntries: StateFlow<List<DiaryEntry>> = repository.allDiaryEntries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notes: StateFlow<List<QuickNote>> = repository.allNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val memories: StateFlow<List<PersonalMemory>> = repository.allMemories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val smartReminders: StateFlow<List<SmartReminder>> = repository.activeSmartReminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val voiceNotes: StateFlow<List<VoiceNote>> = repository.allVoiceNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val goals: StateFlow<List<Goal>> = repository.allGoals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recurringReminders: StateFlow<List<RecurringReminder>> = repository.allRecurringReminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val remindLinks: StateFlow<List<RemindLink>> = repository.allRemindLinks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val privateSpaceItems: StateFlow<List<PrivateSpaceItem>> = repository.allPrivateSpaceItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Water tracker states
    private val currentDay = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val waterLogs: StateFlow<List<WaterLog>> = repository.getWaterLogs(currentDay).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val todayWaterSum: StateFlow<Int> = repository.getWaterSum(currentDay).map { it ?: 0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun getTodayDateString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun getWaterSumForDay(day: String): Flow<Int> = repository.getWaterSum(day).map { it ?: 0 }

    // Chat session state
    val currentChatSessionId = MutableStateFlow("default")
    val chatMessages: StateFlow<List<ChatMessage>> = currentChatSessionId.flatMapLatest { sessionId ->
        repository.getSessionMessages(sessionId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Universal Search
    val searchQuery = MutableStateFlow("")
    val searchResults = searchQuery.debounce(300).flatMapLatest { query ->
        if (query.isBlank()) {
            flowOf(emptyMap<String, List<Any>>())
        } else {
            flow {
                val map = mutableMapOf<String, List<Any>>()
                map["Tasks"] = repository.searchTasks(query)
                map["Expenses"] = repository.searchExpenses(query)
                map["Diary"] = repository.searchDiary(query)
                map["Notes"] = repository.searchNotes(query)
                map["Memories"] = repository.searchMemories(query)
                emit(map)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // AI state
    val aiIsGenerating = MutableStateFlow(false)
    val textToSpeechEnabled = MutableStateFlow(true)
    val isOnline = MutableStateFlow(GeminiService.isApiKeyConfigured()) 

    // OCR State
    val ocrText = MutableStateFlow<String?>(null)
    val ocrLoading = MutableStateFlow(false)

    // Backup text
    val backupDataJson = MutableStateFlow("")

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    // Google Sheets Sync State
    val isSyncing = MutableStateFlow(false)
    val lastSyncStatus = MutableStateFlow<String?>(null)

    // TTS
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Set language to Indian English/Hindi hybrid if possible, or just US with local flavor
                val result = tts?.setLanguage(Locale("hi", "IN"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.US
                }
            }
        }
        // Check if API is configured
        isOnline.value = GeminiService.isApiKeyConfigured()

        // --- Background Auto-Sync Job ---
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                kotlinx.coroutines.delay(900000) // Every 15 minutes as fallback
                if (isNetworkAvailable()) {
                    val url = prefs.getGoogleScriptUrl()
                    if (url.isNotBlank()) {
                        syncToGoogleSheets(url)
                    }
                }
            }
        }
    }

    private fun triggerImmediateSync() {
        val url = prefs.getGoogleScriptUrl()
        if (url.isNotBlank() && isNetworkAvailable()) {
            syncToGoogleSheets(url)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
    }

    fun speak(text: String) {
        if (textToSpeechEnabled.value) {
            // Pre-process text to make it sound more natural for Hinglish (Muslim friendly)
            val processedText = text
                .replace("Assalamualaikum", "Assalam-o-alaikum")
                .replace("Alhamdulillah", "Al-ham-du-lillah")
                .replace("InshAllah", "In-sha-Allah")
                .replace("MashAllah", "Ma-sha-Allah")
                .replace("SubhanAllah", "Sub-han-Allah")
            
            tts?.speak(processedText, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // --- Security ---
    fun setOrUpdatePin(pin: String) {
        if (pin.length == 4) {
            prefs.savePin(pin)
            isLocked.value = true
        }
    }

    fun removePin() {
        prefs.clearPin()
        isLocked.value = false
    }

    fun unlockApp(pin: String): Boolean {
        return if (prefs.verifyPin(pin)) {
            isLocked.value = false
            pinError.value = null
            speak("Welcome back, authorized.")
            true
        } else {
            pinError.value = "Incorrect PIN. Try again."
            false
        }
    }

    // --- Private Space Security ---
    fun setPrivateSpacePassword(password: String) {
        if (password.length >= 4) {
            prefs.savePrivateSpacePassword(password)
            isPrivateSpaceLocked.value = true
            hasPrivateSpacePassword.value = true
        }
    }

    fun unlockPrivateSpace(password: String): Boolean {
        return if (prefs.verifyPrivateSpacePassword(password)) {
            isPrivateSpaceLocked.value = false
            privateSpaceError.value = null
            true
        } else {
            privateSpaceError.value = "Incorrect Password."
            false
        }
    }

    fun lockPrivateSpace() {
        isPrivateSpaceLocked.value = true
    }

    // --- Task Actions ---
    fun addTask(title: String, priority: String, label: String, dueDate: String, notes: String, isRepeating: Boolean, repeatInterval: String) {
        viewModelScope.launch {
            repository.insertTask(Task(
                title = title,
                priority = priority,
                label = label,
                dueDate = dueDate,
                notes = notes,
                isRepeating = isRepeating,
                repeatInterval = repeatInterval,
                createdDate = getTodayDateString()
            ))
            triggerImmediateSync()
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
            triggerImmediateSync()
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val isCompleting = !task.isCompleted
            repository.updateTask(task.copy(
                isCompleted = isCompleting,
                doneDate = if (isCompleting) getTodayDateString() else ""
            ))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    // --- Reminder Actions ---
    fun addReminder(title: String, dueDateTime: Long, recurrence: String, chatId: String = "", remarks: String = "") {
        viewModelScope.launch {
            repository.insertReminder(Reminder(title = title, dueDateTime = dueDateTime, recurrence = recurrence, chatId = chatId, remarks = remarks))
            triggerImmediateSync()
        }
    }

    fun parseAndAddSmartReminder(text: String) {
        viewModelScope.launch {
            val nowMs = System.currentTimeMillis()
            var calculatedTime = nowMs + 3600000
            var cleanTitle = text
            val lowText = text.lowercase()

            when {
                lowText.contains("tomorrow") -> {
                    cleanTitle = text.replace("tomorrow", "", ignoreCase = true).trim()
                    calculatedTime = nowMs + 86400000
                }
                lowText.contains("hours") || lowText.contains("hour") -> {
                    val match = Regex("(\\d+)\\s*(hours|hour)").find(lowText)
                    if (match != null) {
                        val count = match.groupValues[1].toInt()
                        calculatedTime = nowMs + (count * 3600 * 1000L)
                        cleanTitle = text.replace(match.value, "", ignoreCase = true).trim()
                    }
                }
                lowText.contains("minutes") || lowText.contains("mins") || lowText.contains("min") -> {
                    val match = Regex("(\\d+)\\s*(minutes|mins|min)").find(lowText)
                    if (match != null) {
                        val count = match.groupValues[1].toInt()
                        calculatedTime = nowMs + (count * 60 * 1000L)
                        cleanTitle = text.replace(match.value, "", ignoreCase = true).trim()
                    }
                }
            }
            repository.insertReminder(Reminder(title = cleanTitle.ifBlank { text }, dueDateTime = calculatedTime))
            speak("Reminder set: $cleanTitle")
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch { repository.deleteReminder(reminder) }
    }

    fun acknowledgeReminder(reminder: Reminder) {
        viewModelScope.launch { repository.updateReminder(reminder.copy(isAcknowledged = true)) }
    }

    // --- Habit Actions ---
    fun addHabit(name: String, type: String, emoji: String = "✅", targetPerDay: String = "") {
        viewModelScope.launch { repository.insertHabit(Habit(name = name, type = type, emoji = emoji, targetPerDay = targetPerDay)) }
    }

    fun logHabitToday(habit: Habit) {
        viewModelScope.launch {
            val today = getTodayDateString()
            val existing = habit.loggedDaysCommaSeparated.split(",").filter { it.isNotBlank() }
            if (!existing.contains(today)) {
                val updatedDays = (existing + today).joinToString(",")
                val newStreak = habit.streakCount + 1
                repository.updateHabit(habit.copy(
                    loggedDaysCommaSeparated = updatedDays,
                    streakCount = newStreak,
                    lastLoggedTimestamp = System.currentTimeMillis()
                ))
            }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch { repository.deleteHabit(habit) }
    }

    // --- Water ---
    fun logWater(ml: Int) {
        viewModelScope.launch {
            repository.insertWaterLog(WaterLog(mlAmount = ml, dayString = getTodayDateString()))
        }
    }

    fun deleteWaterLog(id: Int) {
        viewModelScope.launch { repository.deleteWaterLogById(id) }
    }

    // --- Expense ---
    fun addExpense(amount: Double, title: String, isIncome: Boolean, category: String, dateString: String) {
        viewModelScope.launch {
            repository.insertExpense(Expense(amount = amount, title = title, isIncome = isIncome, category = category, dateString = dateString))
            triggerImmediateSync()
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    // --- Bills ---
    fun addBill(name: String, amount: Double, category: String, dueDay: Int, paymentMethod: String = "", notes: String = "") {
        viewModelScope.launch {
            repository.insertBill(Bill(
                name = name, amount = amount, category = category,
                dueDayOfMonth = dueDay, paymentMethod = paymentMethod, notes = notes
            ))
        }
    }

    fun payBillThisMonth(bill: Bill) {
        viewModelScope.launch {
            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
            val existing = bill.paidMonthsCommaSeparated.split(",").filter { it.isNotBlank() }
            if (!existing.contains(currentMonth)) {
                val updated = (existing + currentMonth).joinToString(",")
                repository.updateBill(bill.copy(paidMonthsCommaSeparated = updated))
            }
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch { repository.deleteBill(bill) }
    }

    // --- Calendar Events ---
    fun addCalendarEvent(title: String, dateString: String, timeString: String, location: String, notes: String, type: String = "Meeting", remindDayBefore: Boolean = true) {
        viewModelScope.launch {
            repository.insertEvent(CalendarEvent(
                title = title, dateString = dateString, timeString = timeString,
                location = location, notes = notes, type = type, remindDayBefore = remindDayBefore
            ))
        }
    }

    fun deleteCalendarEvent(event: CalendarEvent) {
        viewModelScope.launch { repository.deleteEvent(event) }
    }

    // --- Diary ---
    fun addOrUpdateDiaryEntry(dateString: String, text: String, mood: String, photoPath: String) {
        viewModelScope.launch {
            val existing = diaryEntries.value.firstOrNull { it.dateString == dateString }
            if (existing != null) {
                repository.updateDiaryEntry(existing.copy(text = text, mood = mood, photoPath = photoPath))
            } else {
                repository.insertDiaryEntry(DiaryEntry(dateString = dateString, text = text, mood = mood, photoPath = photoPath))
            }
            triggerImmediateSync()
        }
    }

    fun deleteDiaryEntry(entry: DiaryEntry) {
        viewModelScope.launch { repository.deleteDiaryEntry(entry) }
    }

    // --- Notes ---
    fun addNote(title: String, content: String, tag: String) {
        viewModelScope.launch { repository.insertNote(QuickNote(title = title, content = content, tag = tag)) }
    }

    fun toggleNotePin(note: QuickNote) {
        viewModelScope.launch { repository.updateNote(note.copy(isPinned = !note.isPinned)) }
    }

    fun toggleNoteFavorite(note: QuickNote) {
        viewModelScope.launch { repository.updateNote(note.copy(isFavorite = !note.isFavorite)) }
    }

    fun deleteNote(note: QuickNote) {
        viewModelScope.launch { repository.deleteNote(note) }
    }

    // --- Memories ---
    fun addMemory(content: String, category: String) {
        viewModelScope.launch { repository.insertMemory(PersonalMemory(content = content, category = category)) }
    }

    fun deleteMemory(memory: PersonalMemory) {
        viewModelScope.launch { repository.deleteMemory(memory) }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.deleteChatSession(currentChatSessionId.value)
        }
    }

    // ============================================================
    // MAIN AI ASSISTANT — Chat + Smart Command Parsing
    // ============================================================
    fun sendAssistancePrompt(promptStr: String) {
        val userPrompt = promptStr.trim()
        if (userPrompt.isEmpty()) return

        viewModelScope.launch {
            val session = currentChatSessionId.value
            repository.insertChatMessage(ChatMessage(chatSessionId = session, sender = "User", text = userPrompt))
            aiIsGenerating.value = true

            // Try local command first (works offline)
            val localResult = processLocalCommand(userPrompt)
            if (localResult != null) {
                repository.insertChatMessage(ChatMessage(chatSessionId = session, sender = "Rk", text = localResult))
                aiIsGenerating.value = false
                // REMOVED speak from console/chat screen as per request
                // speak(localResult) 
                return@launch
            }

            // If online, try AI-powered parsing for complex requests
            if (GeminiService.isApiKeyConfigured() && isNetworkAvailable()) {
                val smartResult = trySmartAiParsing(userPrompt)
                if (smartResult != null) {
                    repository.insertChatMessage(ChatMessage(chatSessionId = session, sender = "Rk", text = smartResult))
                    aiIsGenerating.value = false
                    return@launch
                }
            }

            // Full conversational AI response
            val allMemoryFacts = memories.value.joinToString("\n") { "- ${it.category}: ${it.content}" }
            val todayTasks = tasks.value.filter { !it.isCompleted }.take(5).joinToString(", ") { it.title }
            val systemContext = """
                You are RK, a smart personal AI assistant app. Be friendly, helpful and concise.
                Today's date: ${getTodayDateString()}
                
                User's active tasks: $todayTasks
                Personal memories: $allMemoryFacts
                
                You can help with: tasks, reminders, expenses, notes, habits, water tracking, calendar, diary.
                For adding items, tell user to use the form in each screen, or type commands like:
                "add task [title]", "remind 30m [title]", "water 250", "expense 200 [item]", "note [text]", "remember [fact]"
                
                Keep responses short and actionable.
            """.trimIndent()

            val aiResponse = (if (isNetworkAvailable()) {
                GeminiService.chat(
                    prompt = userPrompt,
                    systemInstruction = systemContext,
                    chatHistory = chatMessages.value.dropLast(1) // exclude current message
                )
            } else null) ?: "Assalamualaikum! Main RK hoon. Abhi internet offline hai, par main local commands samajh sakta hoon. Batao kya help chahiye?\nTasks, reminders, kharcha, diary, calendar, bills?"

            repository.insertChatMessage(ChatMessage(chatSessionId = session, sender = "Rk", text = aiResponse))
            aiIsGenerating.value = false
            // speak(aiResponse) // REMOVED speak from console/chat screen as per request
        }
    }

    /**
     * Local command processing — works OFFLINE
     * Returns response string if command matched, null otherwise
     */
    /**
     * Comprehensive Local Command Processing — Works OFFLINE
     * Ported from RK Bot Python v18.6 Command Parser
     * RULE: FIRST WORD ALWAYS WINS
     */
    private suspend fun processLocalCommand(command: String): String? {
        val c = command.lowercase().trim()
        val words = c.split("\\s+".toRegex())
        if (words.isEmpty()) return null
        val first = words[0]

        // --- 0. GREETINGS & HELP ---
        val greetings = listOf("hello", "hi", "hey", "assalamualaikum", "salam", "hola", "namaste")
        if (greetings.any { first == it } && c.length < 25) {
            return "Assalamualaikum! Main RK hoon - aapka personal AI assistant. Batao kya help chahiye?\nTasks, reminders, kharcha, diary, calendar, bills?"
        }
        if (first == "help" || first == "commands" || c == "kya kar sakte ho") {
            return "Main aapki in sab mein help kar sakta hoon:\n" +
                    "• Tasks: \"task [title]\", \"done [id]\", \"tasks\"\n" +
                    "• Reminders: \"reminder remind me [time] [text]\", \"reminders\"\n" +
                    "• Expenses: \"expense [amount] [desc]\", \"expenses\"\n" +
                    "• Water: \"water [ml]\", \"water\"\n" +
                    "• Diary: \"diary [text]\", \"diary\"\n" +
                    "• Habits: \"habit [name]\", \"hdone [id]\", \"habits\"\n" +
                    "• Sheet: \"check sheet\" or \"sync now\""
        }

        // --- SYNC CHECK ---
        if (first == "check" && words.getOrNull(1) == "sheet") {
            triggerImmediateSync()
            return "🔄 Syncing initiated. Check status in settings."
        }
        if (first == "sync" && words.getOrNull(1) == "now") {
            triggerImmediateSync()
            return "🔄 Syncing data to Google Sheets now..."
        }

        // --- 1. REMINDER COMMANDS (STRICT FIRST WORD) ---
        if (first == "reminder" || first == "remind") {
            // "reminders" or "remind show"
            if (c == "reminders" || words.size == 1 || words.getOrNull(1) in listOf("show", "dikhao", "list", "dekho")) {
                val active = reminders.value.filter { !it.isAcknowledged }
                if (active.isEmpty()) return "⏰ Koi active reminder nahi hai."
                val list = active.take(8).joinToString("\n") { 
                    val time = SimpleDateFormat("hh:mm a", Locale.US).format(Date(it.dueDateTime))
                    "• [${it.id}] $time: ${it.title}" 
                }
                return "⏰ Active Reminders:\n$list"
            }

            // Handle "reminder remind me 2 min baar paani pina hai"
            var textToParse = c
            if (c.startsWith("reminder remind me")) {
                textToParse = c.substringAfter("reminder remind me").trim()
            } else if (c.startsWith("remind me")) {
                textToParse = c.substringAfter("remind me").trim()
            } else if (c.startsWith("reminder")) {
                textToParse = c.substringAfter("reminder").trim()
            } else if (c.startsWith("remind")) {
                textToParse = c.substringAfter("remind").trim()
            }

            val nowMs = System.currentTimeMillis()
            var delayMs: Long = 5 * 60000L // Default 5 mins
            var cleanText = textToParse

            // Improved time parsing for "2 min" or "1 hour"
            val timeRegex = Regex("(\\d+)\\s*(min|mins|minutes|hour|hours|h|m)")
            val match = timeRegex.find(textToParse)
            if (match != null) {
                val value = match.groupValues[1].toLong()
                val unit = match.groupValues[2]
                delayMs = if (unit.startsWith("h")) value * 3600000L else value * 60000L
                cleanText = textToParse.replace(match.value, "").replace("baar", "").replace("baad", "").trim()
            }

            if (cleanText.isNotBlank()) {
                addReminder(cleanText.replaceFirstChar { it.uppercase() }, nowMs + delayMs, "None")
                val timeLabel = if (delayMs >= 3600000) "${delayMs / 3600000}h" else "${delayMs / 60000}min"
                return "⏰ Reminder set in $timeLabel: \"$cleanText\". InshAllah yaad dilaunga!"
            }
        }

        // --- 2. TASK COMMANDS (PRIORITY: task/tasks/todo/kaam/kam/work) ---
        val taskTriggers = listOf("task", "tasks", "todo", "kaam", "kam", "work")
        if (taskTriggers.any { first == it }) {
            // "tasks" or "task list"
            if (c == "tasks" || words.size == 1 || words.getOrNull(1) in listOf("show", "dikhao", "list", "dekho")) {
                val pending = tasks.value.filter { !it.isCompleted }
                if (pending.isEmpty()) return "📋 Alhamdulillah! Koi pending task nahi hai. Naya add karne ke liye: \"task [title]\""
                val list = pending.take(10).joinToString("\n") { "• [${it.id}] ${it.title} (${it.priority})" }
                return "📋 Pending Tasks:\n$list\n\n_Done karne ke liye: \"done [id]\"_"
            }
            
            // ADD: "task [title]"
            val title = words.drop(1).joinToString(" ").trim()
            if (title.isNotBlank()) {
                addTask(title.replaceFirstChar { it.uppercase() }, "Medium", "General", "", "", false, "None")
                return "✅ Task add ho gaya: \"${title.replaceFirstChar { it.uppercase() }}\". InshAllah ho jayega! 💪"
            }
        }

        // --- 3. COMPLETE / DONE COMMANDS ---
        val doneTriggers = listOf("done", "complete", "finished")
        if (doneTriggers.any { first == it }) {
            val id = words.getOrNull(1)?.toIntOrNull()
            if (id != null) {
                val task = tasks.value.find { it.id == id }
                if (task != null) {
                    if (!task.isCompleted) {
                        toggleTaskCompletion(task)
                        return "✅ MashAllah! Task #$id complete ho gaya: \"${task.title}\""
                    } else return "✅ Task #$id pehle se hi complete hai!"
                }
            }
            // Match by title hint
            val hint = words.drop(1).joinToString(" ").trim()
            if (hint.length >= 3) {
                val task = tasks.value.find { it.title.contains(hint, ignoreCase = true) && !it.isCompleted }
                if (task != null) {
                    toggleTaskCompletion(task)
                    return "✅ MashAllah! Task complete: \"${task.title}\""
                }
            }
        }

        // --- 4. EXPENSE COMMANDS ---
        val expTriggers = listOf("kharcha", "spent", "paisa", "paise", "rs", "rupees", "expense", "paid", "add")
        if (expTriggers.any { first == it }) {
            // "expenses"
            if (c == "expenses" || (first == "expense" && words.size == 1)) {
                val total = expenses.value.filter { it.dateString == getTodayDateString() && !it.isIncome }.sumOf { it.amount }.toInt()
                return "💰 Aaj ka total kharcha: Rs $total"
            }

            val amount = words.getOrNull(1)?.toDoubleOrNull() ?: words.getOrNull(2)?.toDoubleOrNull()
            if (amount != null) {
                val desc = words.filter { it != amount.toInt().toString() && it != amount.toString() && it != first }.joinToString(" ").trim()
                val finalDesc = if (desc.isBlank()) "Expense" else desc.replaceFirstChar { it.uppercase() }
                addExpense(amount, finalDesc, false, detectCategory(desc), getTodayDateString())
                val newTotal = expenses.value.filter { it.dateString == getTodayDateString() && !it.isIncome }.sumOf { it.amount }.toInt() + amount.toInt()
                return "💸 Kharcha add ho gaya: Rs ${amount.toInt()} for \"$finalDesc\". Aaj total: Rs $newTotal"
            }
        }

        // --- 5. WATER COMMANDS ---
        val waterTriggers = listOf("water", "paani", "pani", "drink", "piya")
        if (waterTriggers.any { first == it }) {
            if (c == "water" || words.size == 1) {
                return "💧 Water Intake: ${todayWaterSum.value}/${prefs.getWaterGoal()}ml. " + 
                       (if (todayWaterSum.value >= prefs.getWaterGoal()) "Goal complete! MashAllah!" else "Pani piyo!")
            }
            val ml = words.find { it.toIntOrNull() != null }?.toInt() ?: 250
            logWater(ml)
            return "💧 MashAllah! ${ml}ml paani piya. Aaj ka total: ${todayWaterSum.value + ml}ml"
        }

        // --- 6. DIARY COMMANDS ---
        val diaryTriggers = listOf("diary", "dairy", "likho", "likh", "save", "record")
        if (diaryTriggers.any { first == it }) {
            if (c == "diary" || words.size == 1) {
                val entries = diaryEntries.value.filter { it.dateString == getTodayDateString() }
                if (entries.isEmpty()) return "📖 Aaj ki koi entry nahi hai."
                return "📖 Aaj ki diary:\n" + entries.joinToString("\n") { "• ${it.text}" }
            }
            val text = words.drop(1).joinToString(" ").trim()
            if (text.isNotBlank()) {
                addOrUpdateDiaryEntry(getTodayDateString(), text.replaceFirstChar { it.uppercase() }, "Neutral", "")
                return "📖 Diary entry save ho gayi! Alhamdulillah."
            }
        }

        // --- 7. HABIT COMMANDS ---
        val habitTriggers = listOf("habit", "habits", "hdone", "gym", "exercise", "namaz")
        if (habitTriggers.any { first == it }) {
            if (first == "hdone") {
                val rest = words.drop(1).joinToString(" ").trim()
                val id = rest.toIntOrNull()
                val habit = if (id != null) habits.value.find { it.id == id } else habits.value.find { it.name.contains(rest, ignoreCase = true) }
                if (habit != null) {
                    logHabitToday(habit)
                    return "🔥 Habit \"${habit.name}\" done! Streak: ${habit.streakCount + 1}. MashAllah!"
                }
            }
            if (c == "habits" || words.size == 1) {
                val list = habits.value.joinToString("\n") { "• [${it.id}] ${it.emoji} ${it.name} (Streak: ${it.streakCount})" }
                return "🏃 Habits Status:\n${list.ifBlank { "Koi habit set nahi hai." }}"
            }
            val name = words.drop(1).joinToString(" ").trim()
            if (name.isNotBlank()) {
                addHabit(name, "Custom")
                return "🏃 Habit add ho gayi: \"$name\". InshAllah roz karoge!"
            }
        }

        // --- 8. NOTES ---
        if (first == "note" || first == "notes") {
            if (first == "notes" || words.size == 1) {
                val list = notes.value.take(10).joinToString("\n") { "• ${it.title}" }
                return "📝 Quick Notes:\n${list.ifBlank { "Koi note nahi hai." }}"
            }
            val text = words.drop(1).joinToString(" ").trim()
            if (text.isNotBlank()) {
                addNote(text.take(20), text, "General")
                return "📝 Note save ho gaya! /notes se dekho."
            }
        }

        // --- 9. PRIVATE SPACE (Locked Diary) ---
        if (first == "private" || first == "pvt") {
            val text = words.drop(1).joinToString(" ").trim()
            if (text.isNotBlank()) {
                addPrivateSpaceItem("Private Diary", text, "Diary")
                return "🔐 Locked: Item private space mein save ho gaya. Password se access karein."
            }
            return "🔐 Private Space: Locked content yahan save hota hai. Application tab se dekhein."
        }

        return null // AI fallback if no local pattern matched
    }

    /**
     * Try AI-powered natural language parsing for add-item commands
     * Returns action result or null if not an add-item type command
     */
    private suspend fun trySmartAiParsing(userInput: String): String? {
        val c = userInput.lowercase()

        // Detect intent type
        val parseType = when {
            (c.contains("add") || c.contains("create") || c.contains("new")) && c.contains("task") -> "task"
            c.contains("remind") || c.contains("reminder") -> "reminder"
            (c.contains("spent") || c.contains("paid") || c.contains("expense") || c.contains("bought")) && c.any { it.isDigit() } -> "expense"
            (c.contains("event") || c.contains("meeting") || c.contains("appointment") || c.contains("birthday")) -> "event"
            else -> return null
        }

        return try {
            val jsonStr = GeminiService.parseNaturalLanguage(userInput, parseType)
            val cleaned = jsonStr.replace("```json", "").replace("```", "").trim()
            val json = JSONObject(cleaned)

            when (parseType) {
                "task" -> {
                    val title = json.optString("title", userInput)
                    val priority = json.optString("priority", "Medium")
                    val dueDate = json.optString("dueDate", "")
                    val notes = json.optString("notes", "")
                    repository.insertTask(Task(title = title, priority = priority, dueDate = dueDate, notes = notes))
                    "✅ Task added:\n📌 \"$title\"\nPriority: $priority${if (dueDate.isNotBlank()) "\nDue: $dueDate" else ""}"
                }
                "reminder" -> {
                    val title = json.optString("title", userInput)
                    val delayMin = json.optInt("delayMinutes", 30)
                    val dueTime = System.currentTimeMillis() + (delayMin * 60 * 1000L)
                    repository.insertReminder(Reminder(title = title, dueDateTime = dueTime))
                    "⏰ Reminder set in ${delayMin}min: \"$title\""
                }
                "expense" -> {
                    val amount = json.optDouble("amount", 0.0)
                    val title = json.optString("title", "Expense")
                    val category = json.optString("category", "Other")
                    val isIncome = json.optBoolean("isIncome", false)
                    if (amount > 0) {
                        addExpense(amount, title, isIncome, category, getTodayDateString())
                        if (isIncome) "💰 Income recorded: ₹${amount.toInt()} ($title)"
                        else "💸 Expense recorded: ₹${amount.toInt()} for \"$title\" [$category]"
                    } else null
                }
                "event" -> {
                    val title = json.optString("title", userInput)
                    val date = json.optString("dateString", getTodayDateString())
                    val time = json.optString("timeString", "09:00")
                    val location = json.optString("location", "")
                    val type = json.optString("type", "Meeting")
                    repository.insertEvent(CalendarEvent(title = title, dateString = date, timeString = time, location = location, type = type))
                    "📅 Event added:\n\"$title\" on $date at $time${if (location.isNotBlank()) "\n📍 $location" else ""}"
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun detectCategory(text: String): String {
        val t = text.lowercase()
        return when {
            t.contains("fuel") || t.contains("petrol") || t.contains("diesel") -> "Fuel"
            t.contains("food") || t.contains("chai") || t.contains("lunch") || t.contains("dinner") || t.contains("breakfast") || t.contains("restaurant") -> "Food"
            t.contains("bill") || t.contains("recharge") || t.contains("electricity") || t.contains("internet") -> "Bill"
            t.contains("salary") || t.contains("income") -> "Salary"
            t.contains("rent") -> "Rent"
            t.contains("shopping") || t.contains("clothes") || t.contains("amazon") -> "Shopping"
            t.contains("doctor") || t.contains("medicine") || t.contains("hospital") || t.contains("health") -> "Health"
            else -> "Other"
        }
    }

    // --- OCR ---
    fun extractTextFromImage(base64Image: String) {
        viewModelScope.launch {
            ocrLoading.value = true
            val text = GeminiService.performOcr(base64Image)
            ocrText.value = text
            ocrLoading.value = false
            speak("Image text extraction completed.")
        }
    }

    // --- Backups ---
    fun generateBackup() {
        viewModelScope.launch(Dispatchers.Default) {
            val root = JSONObject()

            val taskArr = JSONArray()
            tasks.value.forEach { t ->
                val obj = JSONObject()
                obj.put("title", t.title); obj.put("isCompleted", t.isCompleted)
                obj.put("priority", t.priority); obj.put("label", t.label)
                obj.put("dueDate", t.dueDate); obj.put("notes", t.notes)
                obj.put("isRepeating", t.isRepeating); obj.put("repeatInterval", t.repeatInterval)
                taskArr.put(obj)
            }
            root.put("tasks", taskArr)

            val notesArr = JSONArray()
            notes.value.forEach { n ->
                val obj = JSONObject()
                obj.put("title", n.title); obj.put("content", n.content)
                obj.put("isPinned", n.isPinned); obj.put("isFavorite", n.isFavorite); obj.put("tag", n.tag)
                notesArr.put(obj)
            }
            root.put("notes", notesArr)

            val memoryArr = JSONArray()
            memories.value.forEach { m ->
                val obj = JSONObject()
                obj.put("content", m.content); obj.put("category", m.category)
                memoryArr.put(obj)
            }
            root.put("memories", memoryArr)

            backupDataJson.value = root.toString(2)
        }
    }

    fun restoreBackup(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            viewModelScope.launch {
                if (root.has("tasks")) {
                    val arr = root.getJSONArray("tasks")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertTask(Task(
                            title = obj.getString("title"),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            priority = obj.optString("priority", "Medium"),
                            label = obj.optString("label", "General"),
                            dueDate = obj.optString("dueDate", ""),
                            notes = obj.optString("notes", ""),
                            isRepeating = obj.optBoolean("isRepeating", false),
                            repeatInterval = obj.optString("repeatInterval", "None")
                        ))
                    }
                }
                if (root.has("notes")) {
                    val arr = root.getJSONArray("notes")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertNote(QuickNote(
                            title = obj.getString("title"), content = obj.getString("content"),
                            isPinned = obj.optBoolean("isPinned", false),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            tag = obj.optString("tag", "Notes")
                        ))
                    }
                }
                if (root.has("memories")) {
                    val arr = root.getJSONArray("memories")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertMemory(PersonalMemory(
                            content = obj.getString("content"),
                            category = obj.optString("category", "General")
                        ))
                    }
                }
            }
            true
        } catch (e: Exception) { false }
    }

    fun exportBackupJson(outputStream: OutputStream) {
        try {
            val root = JSONObject()
            val taskArr = JSONArray()
            tasks.value.forEach { t ->
                val obj = JSONObject()
                obj.put("title", t.title); obj.put("isCompleted", t.isCompleted)
                obj.put("priority", t.priority); obj.put("label", t.label)
                obj.put("dueDate", t.dueDate); obj.put("notes", t.notes)
                obj.put("isRepeating", t.isRepeating); obj.put("repeatInterval", t.repeatInterval)
                taskArr.put(obj)
            }
            root.put("tasks", taskArr)
            val notesArr = JSONArray()
            notes.value.forEach { n ->
                val obj = JSONObject()
                obj.put("title", n.title); obj.put("content", n.content)
                obj.put("isPinned", n.isPinned); obj.put("isFavorite", n.isFavorite); obj.put("tag", n.tag)
                notesArr.put(obj)
            }
            root.put("notes", notesArr)
            outputStream.write(root.toString(2).toByteArray(Charsets.UTF_8))
            outputStream.close()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun importBackupJson(inputStream: InputStream): Boolean {
        return try {
            restoreBackup(inputStream.bufferedReader().use { it.readText() })
        } catch (e: Exception) { false }
    }

    // --- Smart Reminders ---
    fun addSmartReminder(title: String, delayMinutes: Int, priority: String) {
        viewModelScope.launch {
            val dueTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
            val maxRepeats = when (priority.uppercase()) { "HIGH" -> 12; "MEDIUM" -> 8; else -> 4 }
            val repeatInterval = when (priority.uppercase()) { "HIGH" -> 5; "MEDIUM" -> 15; else -> 30 }
            repository.insertSmartReminder(SmartReminder(
                title = title, dueDateTime = dueTime, priority = priority.uppercase(),
                repeatIntervalMinutes = repeatInterval, maxRepeats = maxRepeats
            ))
            speak("Smart reminder set for $title")
        }
    }

    fun completeSmartReminder(reminder: SmartReminder) {
        viewModelScope.launch { repository.updateSmartReminder(reminder.copy(isAcknowledged = true)) }
    }

    fun deleteSmartReminder(reminder: SmartReminder) {
        viewModelScope.launch { repository.deleteSmartReminder(reminder) }
    }

    // --- Voice Notes ---
    fun addVoiceNote(filePath: String, duration: Long, category: String = "General") {
        viewModelScope.launch {
            repository.insertVoiceNote(VoiceNote(filePath = filePath, transcription = "Not Transcribed Yet", duration = duration, category = category))
        }
    }

    fun deleteVoiceNote(voiceNote: VoiceNote) {
        viewModelScope.launch {
            val file = java.io.File(voiceNote.filePath)
            if (file.exists()) file.delete()
            repository.deleteVoiceNote(voiceNote)
        }
    }

    fun transcribeVoiceNoteGemini(voiceNote: VoiceNote) {
        viewModelScope.launch {
            try {
                repository.updateVoiceNote(voiceNote.copy(transcription = "Transcribing with AI..."))
                val file = java.io.File(voiceNote.filePath)
                if (!file.exists()) {
                    repository.updateVoiceNote(voiceNote.copy(transcription = "Error: File not found.", status = "Failed"))
                    return@launch
                }
                val bytes = withContext(Dispatchers.IO) { file.readBytes() }
                val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val mime = if (voiceNote.filePath.endsWith(".3gp")) "audio/3gpp" else "audio/mp4"
                val text = GeminiService.transcribeAudio(base64Str, mime)
                repository.updateVoiceNote(voiceNote.copy(transcription = text, isTranscribed = true, status = "Success"))
                speak("Transcription complete.")
            } catch (e: Exception) {
                repository.updateVoiceNote(voiceNote.copy(transcription = "Error: ${e.localizedMessage}", status = "Error"))
            }
        }
    }

    // --- Goal Actions ---
    fun addGoal(title: String, deadline: String) {
        viewModelScope.launch {
            repository.insertGoal(Goal(title = title, deadline = deadline, createdAt = getTodayDateString()))
        }
    }

    fun updateGoalProgress(goal: Goal, progress: Int) {
        viewModelScope.launch {
            repository.updateGoal(goal.copy(progress = progress, isDone = progress >= 100))
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch { repository.deleteGoal(goal) }
    }

    // --- Recurring Reminder Actions ---
    fun addRecurringReminder(title: String, type: String, time: String) {
        viewModelScope.launch {
            repository.insertRecurringReminder(RecurringReminder(title = title, type = type, time = time))
        }
    }

    fun deleteRecurringReminder(reminder: RecurringReminder) {
        viewModelScope.launch { repository.deleteRecurringReminder(reminder) }
    }

    // --- Remind Link Actions ---
    fun addRemindLink(chatId: Long, text: String, link: String, dueDateTime: String, originalMsgId: Long? = null) {
        viewModelScope.launch {
            repository.insertRemindLink(RemindLink(
                chatId = chatId, text = text, link = link,
                dueDateTime = dueDateTime, originalMsgId = originalMsgId,
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            ))
        }
    }

    fun acknowledgeRemindLink(link: RemindLink) {
        viewModelScope.launch { repository.updateRemindLink(link.copy(isAcknowledged = true)) }
    }

    fun deleteRemindLink(link: RemindLink) {
        viewModelScope.launch { repository.deleteRemindLink(link) }
    }

    // --- Private Space Actions ---
    fun addPrivateSpaceItem(title: String, content: String, category: String) {
        viewModelScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            repository.insertPrivateSpaceItem(PrivateSpaceItem(
                title = title, content = content, category = category,
                createdAt = now, modifiedAt = now
            ))
        }
    }

    fun deletePrivateSpaceItem(item: PrivateSpaceItem) {
        viewModelScope.launch { repository.deletePrivateSpaceItem(item) }
    }



    // --- Google Sheets Sync ---
    fun syncToGoogleSheets(scriptUrl: String) {
        if (scriptUrl.isBlank()) {
            lastSyncStatus.value = "Error: Google Script URL is missing."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            isSyncing.value = true
            lastSyncStatus.value = "Syncing data to Google Sheets..."

            try {
                val root = JSONObject()
                
                // 1. Tasks
                val taskArr = JSONArray()
                tasks.value.forEach { t ->
                    val obj = JSONObject()
                    obj.put("ID", "${t.id}#"); obj.put("Title", t.title); obj.put("Priority", t.priority)
                    obj.put("Status", if (t.isCompleted) "Completed" else "Pending")
                    obj.put("Created", t.createdDate); obj.put("Done Date", t.doneDate)
                    taskArr.put(obj)
                }
                root.put("Tasks", taskArr)

                // 2. Reminders
                val remArr = JSONArray()
                reminders.value.forEach { r ->
                    val obj = JSONObject()
                    obj.put("ID", "${r.id}#"); obj.put("Due", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(r.dueDateTime)))
                    obj.put("Text", r.title); obj.put("Repeat", r.recurrence)
                    obj.put("Status", if (r.isAcknowledged) "Acknowledged" else "Active")
                    obj.put("Created Date", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(r.createdAt)))
                    obj.put("Chat ID", r.chatId); obj.put("Last Fired", if (r.lastFired > 0) SimpleDateFormat("HH:mm", Locale.US).format(Date(r.lastFired)) else "")
                    obj.put("Acknowledged", r.isAcknowledged); obj.put("Remarks", r.remarks)
                    remArr.put(obj)
                }
                root.put("Reminders", remArr)

                // 3. Expenses
                val expArr = JSONArray()
                expenses.value.forEach { e ->
                    val obj = JSONObject()
                    obj.put("ID", "${e.id}#"); obj.put("Date", e.dateString)
                    obj.put("Time", SimpleDateFormat("HH:mm", Locale.US).format(Date(e.timestamp)))
                    obj.put("Amount", e.amount)
                    obj.put("Description", e.title)
                    obj.put("Category", e.category)
                    expArr.put(obj)
                }
                root.put("Expenses", expArr)

                // 4. Habits
                val habitArr = JSONArray()
                habits.value.forEach { h ->
                    val obj = JSONObject()
                    obj.put("ID", "${h.id}#"); obj.put("Habit Name", h.name)
                    obj.put("Emoji", h.emoji); obj.put("Streak", h.streakCount)
                    obj.put("Best Streak", h.bestStreak)
                    obj.put("Created Date", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(h.lastLoggedTimestamp))) // Approximate
                    obj.put("Target (per day)", h.targetPerDay)
                    habitArr.put(obj)
                }
                root.put("Habits", habitArr)

                // 5. Diary
                val diaryArr = JSONArray()
                diaryEntries.value.forEach { d ->
                    val obj = JSONObject()
                    obj.put("ID", "${d.id}#"); obj.put("Date", d.dateString)
                    obj.put("Time", SimpleDateFormat("HH:mm", Locale.US).format(Date(d.timestamp)))
                    obj.put("Text", d.text); obj.put("Mood", d.mood)
                    diaryArr.put(obj)
                }
                root.put("Diary", diaryArr)

                // 6. Bills
                val billArr = JSONArray()
                bills.value.forEach { b ->
                    val obj = JSONObject()
                    obj.put("ID", "${b.id}#"); obj.put("Name", b.name); obj.put("Amount", b.amount)
                    obj.put("Due Day", b.dueDayOfMonth)
                    obj.put("Status", if (b.paidMonthsCommaSeparated.contains(SimpleDateFormat("yyyy-MM", Locale.US).format(Date()))) "Paid" else "Unpaid")
                    obj.put("Auto Pay", if (b.isAutoPay) "Yes" else "No")
                    obj.put("Payment Method", b.paymentMethod); obj.put("Notes", b.notes)
                    obj.put("Created", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(b.createdAt)))
                    billArr.put(obj)
                }
                root.put("Bills & Subscriptions", billArr)

                // 7. Events
                val eventArr = JSONArray()
                events.value.forEach { v ->
                    val obj = JSONObject()
                    obj.put("ID", "${v.id}#"); obj.put("Title", v.title); obj.put("Date", v.dateString)
                    obj.put("Time", v.timeString); obj.put("Location", v.location)
                    obj.put("Notes", v.notes); obj.put("Type", v.type)
                    obj.put("Created", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(v.createdAt)))
                    eventArr.put(obj)
                }
                root.put("Calendar Events", eventArr)

                // 8. Water
                val waterArr = JSONArray()
                waterLogs.value.forEach { w ->
                    val obj = JSONObject()
                    obj.put("ID", "${w.id}#"); obj.put("Date", w.dayString)
                    obj.put("Time", SimpleDateFormat("HH:mm", Locale.US).format(Date(w.timestamp)))
                    obj.put("ML Added", w.mlAmount)
                    waterArr.put(obj)
                }
                root.put("Water Intake", waterArr)

                // 9. Memories
                val memArr = JSONArray()
                memories.value.forEach { m ->
                    val obj = JSONObject()
                    obj.put("ID", "${m.id}#"); obj.put("Date", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(m.timestamp)))
                    obj.put("Time", SimpleDateFormat("HH:mm", Locale.US).format(Date(m.timestamp)))
                    obj.put("Fact", m.content)
                    memArr.put(obj)
                }
                root.put("Memory / Important Notes", memArr)

                // 10. Voice Notes
                val voiceArr = JSONArray()
                voiceNotes.value.forEach { v ->
                    val obj = JSONObject()
                    obj.put("ID", "${v.id}#"); obj.put("Date", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(v.timestamp)))
                    obj.put("Time", SimpleDateFormat("HH:mm", Locale.US).format(Date(v.timestamp)))
                    obj.put("Transcript", v.transcription); obj.put("Saved To", v.filePath)
                    obj.put("Duration", v.duration); obj.put("Status", v.status); obj.put("Category", v.category)
                    voiceArr.put(obj)
                }
                root.put("Voice Notes", voiceArr)

                // 11. Quick Notes
                val noteArr = JSONArray()
                notes.value.forEach { n ->
                    val obj = JSONObject()
                    obj.put("ID", "${n.id}#"); obj.put("Date", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(n.timestamp)))
                    obj.put("Time", SimpleDateFormat("HH:mm", Locale.US).format(Date(n.timestamp)))
                    obj.put("Text", n.content); obj.put("Status", if (n.isPinned) "Pinned" else "Normal")
                    noteArr.put(obj)
                }
                root.put("Quick Notes", noteArr)

                // 12. Goals
                val goalArr = JSONArray()
                goals.value.forEach { g ->
                    val obj = JSONObject()
                    obj.put("ID", "${g.id}#"); obj.put("Title", g.title)
                    obj.put("Progress", g.progress); obj.put("Status", if (g.isDone) "Done" else "Active")
                    obj.put("Deadline", g.deadline); obj.put("Created", g.createdAt)
                    goalArr.put(obj)
                }
                root.put("Goals", goalArr)

                val success = GoogleSheetsService.sync(root.toString(), scriptUrl)
                if (success) {
                    lastSyncStatus.value = "✅ Sync Successful! (${SimpleDateFormat("HH:mm", Locale.US).format(Date())})"
                } else {
                    lastSyncStatus.value = "❌ Sync Failed. check script doPost(e) & URL."
                }
            } catch (e: Exception) {
                lastSyncStatus.value = "❌ Error: ${e.localizedMessage}"
            } finally {
                isSyncing.value = false
            }
        }
    }
}
