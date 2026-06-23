package com.aistudio.rkaiassistant.viewmodel

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
import android.net.NetworkRequest
import android.widget.Toast
import com.aistudio.rkaiassistant.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen {
    Dashboard, Chat, Tasks, Reminders, Habits, Water, Expenses, Bills, Calendar, Diary, Notes, Search, Settings, SmartReminders, VoiceNotes, WeeklyReview, Goals, RecurringReminders, RemindLinks, PrivateSpace, History, PrivateNoteEdit, GoldCalculator
}

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val db: AppDatabase by lazy { AppDatabase.getDatabase(application) }
    private val repository: DatabaseRepository by lazy { DatabaseRepository(db) }
    val prefs = SecurePrefHelper(application)
    private val cryptoManager = CryptoManager()

    // Gold Calculator State
    val goldWeight = MutableStateFlow("")
    val goldKarat = MutableStateFlow("22") // Default 22K
    val goldPricePerGram = MutableStateFlow("")
    val makingChargePerGram = MutableStateFlow("")
    val goldGstRate = MutableStateFlow("3.0") // Default 3% GST

    // Derived Total
    val goldTotalEstimate = combine(
        goldWeight, goldKarat, goldPricePerGram, makingChargePerGram, goldGstRate
    ) { weight, karat, price, making, gst ->
        calculateGold(weight, karat, price, making, gst)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoldResult())

    data class GoldResult(
        val goldValue: Double = 0.0,
        val totalMakingCharges: Double = 0.0,
        val gstAmount: Double = 0.0,
        val finalPrice: Double = 0.0
    )

    private fun calculateGold(weightStr: String, karatStr: String, priceStr: String, makingStr: String, gstStr: String): GoldResult {
        val w = weightStr.toDoubleOrNull() ?: 0.0
        val k = karatStr.toDoubleOrNull() ?: 22.0
        val p = priceStr.toDoubleOrNull() ?: 0.0
        val m = makingStr.toDoubleOrNull() ?: 0.0
        val g = gstStr.toDoubleOrNull() ?: 3.0

        val goldVal = w * (k / 24.0) * p
        val makingCharges = w * m
        val subTotal = goldVal + makingCharges
        val gstAmt = subTotal * (g / 100.0)
        val total = subTotal + gstAmt

        return GoldResult(goldVal, makingCharges, gstAmt, total)
    }

    // Current Screen
    val currentScreen = MutableStateFlow(AppScreen.Dashboard)
    private val screenStack = mutableListOf<AppScreen>()

    fun navigateTo(screen: AppScreen) {
        if (currentScreen.value != screen) {
            screenStack.add(currentScreen.value)
            currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (screenStack.isNotEmpty()) {
            currentScreen.value = screenStack.removeAt(screenStack.size - 1)
            return true
        }
        return false
    }

    val editingPrivateItem = MutableStateFlow<PrivateSpaceItem?>(null)


    // Security Lock State
    val isLocked = MutableStateFlow(prefs.isPinEnabled() && prefs.isAppLockActive())
    val pinError = MutableStateFlow<String?>(null)
    val isBiometricEnabled = MutableStateFlow(prefs.isBiometricEnabled())
    val isAppLockActive = MutableStateFlow(prefs.isAppLockActive())
    val isWelcomeSoundEnabled = MutableStateFlow(prefs.isWelcomeSoundEnabled())

    // Private Space Lock State
    val isPrivateSpaceLocked = MutableStateFlow(prefs.hasPrivateSpacePassword())
    val privateSpaceError = MutableStateFlow<String?>(null)
    val hasPrivateSpacePassword = MutableStateFlow(prefs.hasPrivateSpacePassword())

    // State Flows for DB Data - Using lazy initialization for repository
    val tasks: StateFlow<List<Task>> by lazy { repository.allTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) }
    val reminders: StateFlow<List<Reminder>> by lazy { repository.allReminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) }
    val habits: StateFlow<List<Habit>> by lazy { repository.allHabits.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) }
    val expenses: StateFlow<List<Expense>> by lazy { repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) }
    val bills: StateFlow<List<Bill>> by lazy { repository.allBills.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) }
    val events: StateFlow<List<CalendarEvent>> by lazy { repository.allEvents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) }
    val diaryEntries: StateFlow<List<DiaryEntry>> by lazy { repository.allDiaryEntries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) }
    val notes: StateFlow<List<QuickNote>> by lazy { repository.allNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) }
    val memories: StateFlow<List<PersonalMemory>> by lazy { repository.allMemories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) }
    val smartReminders: StateFlow<List<SmartReminder>> by lazy { repository.activeSmartReminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) }
    val voiceNotes: StateFlow<List<VoiceNote>> by lazy { repository.allVoiceNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) }
    val goals: StateFlow<List<Goal>> by lazy { repository.allGoals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) }
    val recurringReminders: StateFlow<List<RecurringReminder>> by lazy { repository.allRecurringReminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) }
    val remindLinks: StateFlow<List<RemindLink>> by lazy { repository.allRemindLinks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) }
    val privateSpaceItems: StateFlow<List<PrivateSpaceItem>> by lazy { 
        repository.allPrivateSpaceItems
            .map { items ->
                items.map { 
                    try {
                        it.copy(content = cryptoManager.decryptString(it.content))
                    } catch (e: Exception) {
                        it.copy(content = "[Decryption Error]")
                    }
                }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val allActivities: StateFlow<List<ActivityItem>> by lazy {
        combine(
            combine(tasks, reminders, habits, expenses, bills) { t, r, h, e, b ->
                val list = mutableListOf<ActivityItem>()
                val todayMonth = getTodayDateString().take(7)
                t.forEach { list.add(ActivityItem("${it.id}#", "TASK", it.title, it.createdDate, it.isDeleted, it.isCompleted, it.timestamp)) }
                r.forEach { list.add(ActivityItem("${it.id}#", "REMINDER", it.title, SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.dueDateTime)), it.isDeleted, it.isAcknowledged, it.createdAt)) }
                h.forEach { list.add(ActivityItem("${it.id}#", "HABIT", it.name, "", it.isDeleted, it.streakCount > 0, it.lastLoggedTimestamp)) }
                e.forEach { list.add(ActivityItem("${it.id}#", "EXPENSE", "${it.title} (₹${it.amount})", it.dateString, it.isDeleted, true, it.timestamp)) }
                b.forEach { list.add(ActivityItem("${it.id}#", "BILL", "${it.name} (₹${it.amount})", "", it.isDeleted, it.paidMonthsCommaSeparated.contains(todayMonth), it.createdAt)) }
                list
            },
            combine(events, diaryEntries, notes, memories, smartReminders) { ev, d, n, m, s ->
                val list = mutableListOf<ActivityItem>()
                ev.forEach { list.add(ActivityItem("${it.id}#", "EVENT", it.title, it.dateString, it.isDeleted, true, it.createdAt)) }
                d.forEach { list.add(ActivityItem("${it.id}#", "DIARY", it.text.take(30), it.dateString, it.isDeleted, true, it.timestamp)) }
                n.forEach { list.add(ActivityItem("${it.id}#", "NOTE", it.title, "", it.isDeleted, true, it.timestamp)) }
                m.forEach { list.add(ActivityItem("${it.id}#", "MEMORY", it.content.take(30), "", it.isDeleted, true, it.timestamp)) }
                s.forEach { list.add(ActivityItem("${it.id}#", "SMART_REM", it.title, "", it.isDeleted, it.isAcknowledged, it.dueDateTime)) }
                list
            },
            combine(voiceNotes, goals, recurringReminders, remindLinks, privateSpaceItems) { v, g, rr, rl, ps ->
                val list = mutableListOf<ActivityItem>()
                v.forEach { list.add(ActivityItem("${it.id}#", "VOICE", it.transcription.take(30), "", it.isDeleted, it.isTranscribed, it.timestamp)) }
                g.forEach { list.add(ActivityItem("${it.id}#", "GOAL", it.title, it.createdAt, it.isDeleted, it.isDone, it.timestamp)) }
                rr.forEach { list.add(ActivityItem("${it.id}#", "RECURRING", it.title, it.time, it.isDeleted, it.isActive, it.createdAt)) }
                rl.forEach { 
                    val ts = try { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(it.createdAt)?.time ?: 0L } catch (e: Exception) { 0L }
                    list.add(ActivityItem("${it.id}#", "LINK", it.text, it.dueDateTime, it.isDeleted, it.isAcknowledged, ts)) 
                }
                ps.forEach { 
                    val ts = try { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(it.modifiedAt)?.time ?: 0L } catch (e: Exception) { 0L }
                    list.add(ActivityItem("${it.id}#", "PRIVATE", it.title, it.modifiedAt, it.isDeleted, true, ts)) 
                }
                list
            }
        ) { l1, l2, l3 ->
            (l1 + l2 + l3).sortedByDescending { it.timestamp }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    data class ActivityItem(
        val id: String,
        val type: String,
        val title: String,
        val date: String,
        val isDeleted: Boolean,
        val isDone: Boolean,
        val timestamp: Long
    )

    // Water tracker states - Lazy initialization
    private val currentDay = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val waterLogs: StateFlow<List<WaterLog>> by lazy { repository.getWaterLogs(currentDay).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) }
    val todayWaterSum: StateFlow<Int> by lazy { repository.getWaterSum(currentDay).map { it ?: 0 }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0) }

    fun getTodayDateString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun getWaterSumForDay(day: String): Flow<Int> = repository.getWaterSum(day).map { it ?: 0 }

    // Chat session state - Lazy initialization
    val currentChatSessionId = MutableStateFlow("default")
    val chatMessages: StateFlow<List<ChatMessage>> by lazy { 
        currentChatSessionId.flatMapLatest { sessionId ->
            repository.getSessionMessages(sessionId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // Universal Search - Lazy initialization
    val searchQuery = MutableStateFlow("")
    val searchResults: StateFlow<Map<String, List<Any>>> by lazy { 
        searchQuery.debounce(300).flatMapLatest { query ->
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
    }

    // AI state
    val aiIsGenerating = MutableStateFlow(false)
    val textToSpeechEnabled = MutableStateFlow(true)
    val isSpeaking = MutableStateFlow(false)
    val isOnline = MutableStateFlow(GeminiService.isApiKeyConfigured())
    val isLocalAiAvailable = MutableStateFlow(false)
    val isImportingModel = MutableStateFlow(false)
    val modelDownloadProgress = MutableStateFlow<Float?>(null)

    // Jarvis Voice AI
    val isJarvisActive = MutableStateFlow(false)
    val jarvisStatus = MutableStateFlow("Jarvis Standby")
    private var speechRecognizer: SpeechRecognizer? = null

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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                LocalLLMService.initialize(application)
                withContext(Dispatchers.Main) {
                    isLocalAiAvailable.value = LocalLLMService.isModelAvailable(application)
                }
            } catch (e: Exception) {
                android.util.Log.e("RKAI", "Local AI Init Error", e)
            }
        }

        // Security: Key is initialized in AssistantApp.onCreate for safety.
        // We only check if it's available to update the UI state.
        val geminiKey = prefs.getGeminiApiKey()

        // Update online status
        isOnline.value = geminiKey.isNotBlank()

        // Use Google TTS engine specifically for best Hindi/Indian English support
        tts = TextToSpeech(application, { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale("hi", "IN")
                val result = tts?.setLanguage(locale)
                
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to Indian English which handles Hinglish much better than US accent
                    tts?.setLanguage(Locale("en", "IN"))
                }
                
                // Try to select a high-quality (Natural/Network) voice if available
                try {
                    val voices = tts?.voices
                    if (!voices.isNullOrEmpty()) {
                        val bestVoice = voices.find { v -> 
                            v.locale.language == "hi" && v.name.lowercase().contains("network")
                        } ?: voices.find { v -> 
                            v.locale.language == "hi" && v.name.lowercase().contains("enhanced")
                        } ?: voices.find { v -> 
                            v.locale.language == "hi"
                        } ?: voices.find { v ->
                            v.locale.language == "en" && v.locale.country == "IN"
                        }
                        
                        bestVoice?.let { tts?.voice = it }
                    }
                } catch (e: Exception) {}

                tts?.setPitch(1.0f)
                tts?.setSpeechRate(0.95f) // Slightly slower for better clarity
            }
        }, null) // Stability Fix: Default engine (null) instead of hardcoded package
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
        speechRecognizer?.destroy()
    }

    fun toggleTextToSpeech() {
        val newState = !textToSpeechEnabled.value
        textToSpeechEnabled.value = newState
        if (!newState) {
            tts?.stop()
        }
    }

    fun speak(text: String, onFinished: (() -> Unit)? = null) {
        if (textToSpeechEnabled.value) {
            isSpeaking.value = true
            // Enhanced phonetic processing for smoother Hindi/Hinglish delivery
            val processedText = text
                .replace("Assalamualaikum", "Assalam-o-alaikum")
                .replace("Alhamdulillah", "Al-ham-du-lillah")
                .replace("InshAllah", "In-sha-Allah")
                .replace("MashAllah", "Ma-sha-Allah")
                .replace("SubhanAllah", "Sub-han-Allah")
                .replace("RK", "R.K.")
                .replace("sir", "sir,") // adds a natural pause
                .replace("theek", "theek")
                .replace("kaise ho", "kaise ho?")
                .replace("kya ", "kya, ")
                .replace("hoon", "hoon.")
            
            val listener = object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    viewModelScope.launch(Dispatchers.Main) { isSpeaking.value = true }
                }
                override fun onDone(utteranceId: String?) {
                    viewModelScope.launch(Dispatchers.Main) { 
                        isSpeaking.value = false
                        onFinished?.invoke() 
                    }
                }
                override fun onError(utteranceId: String?) {
                    viewModelScope.launch(Dispatchers.Main) { isSpeaking.value = false }
                }
            }
            
            tts?.setOnUtteranceProgressListener(listener)
            
            val params = android.os.Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "id")
            tts?.speak(processedText, TextToSpeech.QUEUE_FLUSH, params, "id")
        } else {
            onFinished?.invoke()
        }
    }

    fun toggleJarvisMode() {
        if (isJarvisActive.value) {
            stopJarvisListening()
        } else {
            startJarvisListening()
        }
    }

    private fun startJarvisListening() {
        val context = getApplication<Application>()
        if (speechRecognizer == null) {
            speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(context)
        }
        
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "en-IN") // Support Indian English
        
        speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                jarvisStatus.value = "Listening..."
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                jarvisStatus.value = "Processing..."
            }
            override fun onError(error: Int) {
                if (!isJarvisActive.value) return // Don't restart if turned off

                speechRecognizer?.destroy()
                speechRecognizer = null

                if (error == android.speech.SpeechRecognizer.ERROR_NO_MATCH || error == android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    jarvisStatus.value = "Say something..."
                    startJarvisListening() // Keep listening if no speech detected
                } else {
                    jarvisStatus.value = "Error: $error"
                    isJarvisActive.value = false
                }
            }
            override fun onResults(results: android.os.Bundle?) {
                if (!isJarvisActive.value) return // Don't process if turned off

                val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    handleJarvisVoiceCommand(matches[0])
                } else {
                    startJarvisListening()
                }
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
        
        isJarvisActive.value = true
        speechRecognizer?.startListening(intent)
    }

    private fun stopJarvisListening() {
        isJarvisActive.value = false
        jarvisStatus.value = "Jarvis Standby"
        speechRecognizer?.cancel() // Immediate cancel instead of just stop
        tts?.stop()
    }

    private fun handleJarvisVoiceCommand(text: String) {
        val cleanedText = text.lowercase()
            .replace("rk", "")
            .replace("jarvis", "")
            .trim()
            
        viewModelScope.launch {
            val session = currentChatSessionId.value
            repository.insertChatMessage(ChatMessage(chatSessionId = session, sender = "User", text = text))
            
            jarvisStatus.value = "Thinking..."
            
            if (!LocalLLMService.isModelAvailable(getApplication()) && !isNetworkAvailable()) {
                speak("Sir, Offline model not found and internet is also down. I cannot process this.")
                jarvisStatus.value = "Offline & No Model"
                return@launch
            }
            
            val localResult = processLocalCommand(cleanedText)
            
            val response = if (localResult != null) {
                localResult
            } else {
                var aiRes: String? = null
                if (isOnline.value && isNetworkAvailable() && GeminiService.isApiKeyConfigured()) {
                    aiRes = GeminiService.chat(
                        text, 
                        "You are RK, a helpful personal assistant. Introduce yourself as 'Main RK, aapka personal voice assistant hoon'. Speak in natural, polite Hindi/Hinglish.",
                        onError = { errorMsg ->
                            viewModelScope.launch(Dispatchers.Main) {
                                Toast.makeText(getApplication(), errorMsg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                aiRes ?: LocalLLMService.generateResponse(text, getApplication())
            }
            
            repository.insertChatMessage(ChatMessage(chatSessionId = session, sender = "Rk", text = response))
            jarvisStatus.value = "Speaking..."
            
            speak(response) {
                if (isJarvisActive.value) {
                    startJarvisListening() // Listen again after speaking
                }
            }
        }
    }


    // --- Security ---
    fun toggleBiometricSetting(enabled: Boolean) {
        prefs.setBiometricEnabled(enabled)
        isBiometricEnabled.value = enabled
    }

    fun toggleAppLock(enabled: Boolean) {
        prefs.setAppLockActive(enabled)
        isAppLockActive.value = enabled
    }

    fun toggleWelcomeSound(enabled: Boolean) {
        prefs.setWelcomeSoundEnabled(enabled)
        isWelcomeSoundEnabled.value = enabled
    }

    fun unlockWithBiometric() {
        isLocked.value = false
        if (isWelcomeSoundEnabled.value) {
            speak("Welcome back RK")
        }
    }

    fun setOrUpdatePin(pin: String) {
        if (pin.length == 4) {
            prefs.savePin(pin)
            isLocked.value = true
            speak("Security PIN set successfully. Main aapka data secure rakhunga.")
        }
    }

    fun removePin() {
        prefs.clearPin()
        isLocked.value = false
        speak("Security PIN removed. Device open hai.")
    }

    fun unlockApp(pin: String): Boolean {
        return if (prefs.verifyPin(pin)) {
            isLocked.value = false
            pinError.value = null
            if (isWelcomeSoundEnabled.value) {
                speak("Welcome back RK")
            }
            true
        } else {
            pinError.value = "Incorrect PIN. Try again."
            speak("Ghalat PIN. Dubara koshish karein.")
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
            val id = repository.insertReminder(Reminder(title = title, dueDateTime = dueDateTime, recurrence = recurrence, chatId = chatId, remarks = remarks))
            AlarmHelper.scheduleExactAlarm(getApplication(), id.toInt(), dueDateTime, title)
            triggerImmediateSync()
        }
    }

    fun parseAndAddSmartReminder(text: String) {
        viewModelScope.launch {
            jarvisStatus.value = "Analyzing context..."
            
            // Try AI parsing first if online
            if (isNetworkAvailable() && isOnline.value) {
                val aiResult = trySmartAiParsing("remind me to $text")
                if (aiResult != null) {
                    speak("System update: $aiResult")
                    return@launch
                }
            }

            // Fallback to enhanced local parsing
            val nowMs = System.currentTimeMillis()
            var calculatedTime = nowMs + 3600000 // Default 1 hour
            var cleanTitle = text
            val lowText = text.lowercase()

            val timeUnits = mapOf(
                "minute" to 60000L, "minutes" to 60000L, "min" to 60000L, "mins" to 60000L,
                "hour" to 3600000L, "hours" to 3600000L, "h" to 3600000L,
                "day" to 86400000L, "days" to 86400000L
            )

            var foundTime = false
            for ((unit, ms) in timeUnits) {
                val regex = Regex("(\\d+)\\s*$unit")
                val match = regex.find(lowText)
                if (match != null) {
                    val count = match.groupValues[1].toLong()
                    calculatedTime = nowMs + (count * ms)
                    cleanTitle = text.replace(match.value, "", ignoreCase = true).trim()
                    foundTime = true
                    break
                }
            }
            
            if (!foundTime && lowText.contains("tomorrow")) {
                calculatedTime = nowMs + 86400000
                cleanTitle = text.replace("tomorrow", "", ignoreCase = true).trim()
            }

            val finalTitle = cleanTitle.ifBlank { text }.replaceFirstChar { it.uppercase() }
            val id = repository.insertReminder(Reminder(title = finalTitle, dueDateTime = calculatedTime))
            AlarmHelper.scheduleExactAlarm(getApplication(), id.toInt(), calculatedTime, finalTitle)
            
            val response = "Reminder set Boss: $finalTitle. I will notify you at ${SimpleDateFormat("hh:mm a", Locale.US).format(Date(calculatedTime))}."
            speak(response)
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

    private fun generateDataContext(): String {
        val today = getTodayDateString()
        val currentMonth = today.take(7)
        
        // Expenses
        val monthExpenses = expenses.value.filter { it.dateString.startsWith(currentMonth) && !it.isIncome }
        val totalSpend = monthExpenses.sumOf { it.amount }
        val topCategory = monthExpenses.groupBy { it.category }.maxByOrNull { it.value.sumOf { e -> e.amount } }?.key ?: "None"
        
        // Tasks
        val allTasks = tasks.value
        val completed = allTasks.count { it.isCompleted }
        val pending = allTasks.size - completed
        val completionRate = if (allTasks.isNotEmpty()) (completed * 100 / allTasks.size) else 0
        
        // Habits & Gamification (Derived)
        val habitData = habits.value.map { 
            val totalLogged = it.loggedDaysCommaSeparated.split(",").count { d -> d.isNotBlank() }
            val xp = (totalLogged * 10) + (it.streakCount * 50)
            val level = (xp / 500) + 1
            "${it.name} (Streak: ${it.streakCount}, Level: $level, XP: $xp)"
        }.joinToString(", ")
        
        // Bills
        val pendingBills = bills.value.count { !it.paidMonthsCommaSeparated.contains(currentMonth) }
        
        // Water
        val waterIntake = todayWaterSum.value
        val waterGoal = prefs.getWaterGoal()

        return """
            [USER DATA SUMMARY - $today]
            - Expenses this month: Rs $totalSpend (Top category: $topCategory)
            - Task Stats: $pending pending, $completed completed ($completionRate% completion rate)
            - Habit Progress: $habitData
            - Bills pending this month: $pendingBills
            - Water today: ${waterIntake}ml / ${waterGoal}ml
            - User Level: ${habits.value.sumOf { (it.loggedDaysCommaSeparated.split(",").count { d -> d.isNotBlank() } * 10) + (it.streakCount * 50) } / 1000 + 1}
        """.trimIndent()
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
                speak(localResult)
                return@launch
            }

            // If online, try AI-powered parsing for complex requests
            if (GeminiService.isApiKeyConfigured() && isNetworkAvailable()) {
                val smartResult = trySmartAiParsing(userPrompt)
                if (smartResult != null) {
                    repository.insertChatMessage(ChatMessage(chatSessionId = session, sender = "Rk", text = smartResult))
                    aiIsGenerating.value = false
                    speak(smartResult) 
                    return@launch
                }
            }

            // Full conversational AI response with Data Context
            val dataContext = generateDataContext()
            val allMemoryFacts = memories.value.joinToString("\n") { "- ${it.category}: ${it.content}" }
            val systemContext = """
                You are RK, a sophisticated and highly intelligent personal AI assistant, similar to Jarvis from Iron Man. 
                Your primary objective is to assist 'Boss' with efficiency, loyalty, and a touch of professional wit.
                
                PERSONALITY:
                - Address the user as 'Boss' or 'Sir'.
                - Your tone is professional, polite, and slightly futuristic.
                - Use phrases like "At your service, Boss", "Systems are optimal", "Processing your request", and "Alhamdulillah, task completed".
                - Speak in a mix of Hindi and English (Hinglish) that sounds natural and smart.
                
                DATA CONTEXT:
                You have real-time access to the user's life telemetry:
                $dataContext
                
                Today's date: ${getTodayDateString()}
                Personal memories: $allMemoryFacts
                
                CAPABILITIES:
                You manage: tasks, reminders, expenses, notes, habits, water tracking, calendar, diary, and gold calculations.
                
                When the user asks for a 'briefing' or 'summary', provide a concise overview of their day, pending tasks, and financial status.
            """.trimIndent()

            val aiResponse = if (isNetworkAvailable()) {
                GeminiService.chat(
                    prompt = userPrompt,
                    systemInstruction = systemContext,
                    chatHistory = chatMessages.value.dropLast(1),
                    onError = { errorMsg ->
                        viewModelScope.launch(Dispatchers.Main) {
                            Toast.makeText(getApplication(), errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) ?: (if (isLocalAiAvailable.value) LocalLLMService.generateResponse("System: $systemContext\nUser: $userPrompt\nRK:", getApplication()) else null)
            } else if (isLocalAiAvailable.value) {
                LocalLLMService.generateResponse("System: $systemContext\nUser: $userPrompt\nRK:", getApplication())
            } else {
                "Assalamualaikum! Main RK hoon. Abhi internet offline hai aur local model bhi nahi mila. Please model download karein ya internet on karein."
            }

            val finalResponse = aiResponse ?: "Maaf kijiyega Boss, main samajh nahi paya. Ek baar phir boliye?"
            repository.insertChatMessage(ChatMessage(chatSessionId = session, sender = "Rk", text = finalResponse))
            aiIsGenerating.value = false
            speak(finalResponse)

            // Extract memories in background
            if (aiResponse != null) {
                extractAndSaveMemories(userPrompt, finalResponse)
            }
        }
    }

    private fun extractAndSaveMemories(userText: String, aiResponse: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!GeminiService.isApiKeyConfigured() || !isNetworkAvailable()) return@launch
            
            val prompt = """
                Identify if there are any NEW personal facts about the user in this interaction.
                User: "$userText"
                AI: "$aiResponse"
                
                If a clear personal preference, date, or fact is mentioned, extract it.
                Return ONLY a JSON array of facts, e.g., ["User's daughter's name is Sara", "User is allergic to peanuts"].
                If no new facts, return exactly [].
            """.trimIndent()
            
            try {
                val result = GeminiService.chat(prompt, "You are a silent memory extractor. Output ONLY valid JSON array.")
                if (result != null) {
                    val startIndex = result.indexOf("[")
                    val endIndex = result.lastIndexOf("]")
                    if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                        val fullJson = result.substring(startIndex, endIndex + 1)
                        val array = JSONArray(fullJson)
                        for (i in 0 until array.length()) {
                            val fact = array.getString(i)
                            // Avoid duplicates (simple check)
                            if (memories.value.none { it.content.contains(fact, ignoreCase = true) }) {
                                repository.insertMemory(PersonalMemory(content = fact, category = "AI Memory"))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
            return "Assalamualaikum! Main RK, aapka personal voice assistant hoon. Bataiye Boss, main aapki kya madad kar sakta hoon?"
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
    private suspend fun getFullBackupJson(): String {
        val root = JSONObject()
        
        // Collect all data directly from repository (Flow.first() ensures a fresh DB read)
        root.put("tasks", JSONArray(repository.allTasks.first().map { t ->
            JSONObject().apply {
                put("title", t.title); put("isCompleted", t.isCompleted); put("priority", t.priority)
                put("label", t.label); put("dueDate", t.dueDate); put("notes", t.notes)
                put("isRepeating", t.isRepeating); put("repeatInterval", t.repeatInterval)
                put("createdDate", t.createdDate); put("doneDate", t.doneDate)
                put("isDeleted", t.isDeleted); put("remarks", t.remarks); put("timestamp", t.timestamp)
            }
        }))
        
        root.put("reminders", JSONArray(repository.allReminders.first().map { r ->
            JSONObject().apply {
                put("title", r.title); put("dueDateTime", r.dueDateTime); put("recurrence", r.recurrence)
                put("isAcknowledged", r.isAcknowledged); put("createdAt", r.createdAt)
                put("chatId", r.chatId); put("lastFired", r.lastFired); put("remarks", r.remarks)
                put("isDeleted", r.isDeleted)
            }
        }))
        
        root.put("expenses", JSONArray(repository.allExpenses.first().map { e ->
            JSONObject().apply {
                put("amount", e.amount); put("title", e.title); put("isIncome", e.isIncome)
                put("category", e.category); put("dateString", e.dateString); put("timestamp", e.timestamp)
                put("isDeleted", e.isDeleted); put("remarks", e.remarks)
            }
        }))

        root.put("habits", JSONArray(repository.allHabits.first().map { h ->
            JSONObject().apply {
                put("name", h.name); put("type", h.type); put("emoji", h.emoji)
                put("loggedDaysCommaSeparated", h.loggedDaysCommaSeparated)
                put("streakCount", h.streakCount); put("bestStreak", h.bestStreak)
                put("lastLoggedTimestamp", h.lastLoggedTimestamp); put("targetPerDay", h.targetPerDay)
                put("isDeleted", h.isDeleted); put("remarks", h.remarks)
            }
        }))

        // Use all water logs for full backup
        root.put("waterLogs", JSONArray(db.waterLogDao().getAllLogs().first().map { w ->
            JSONObject().apply {
                put("mlAmount", w.mlAmount); put("timestamp", w.timestamp)
                put("dayString", w.dayString); put("isDeleted", w.isDeleted); put("remarks", w.remarks)
            }
        }))

        root.put("bills", JSONArray(repository.allBills.first().map { b ->
            JSONObject().apply {
                put("name", b.name); put("amount", b.amount); put("category", b.category)
                put("dueDayOfMonth", b.dueDayOfMonth); put("paidMonthsCommaSeparated", b.paidMonthsCommaSeparated)
                put("isAutoPay", b.isAutoPay); put("paymentMethod", b.paymentMethod); put("notes", b.notes)
                put("createdAt", b.createdAt); put("isDeleted", b.isDeleted); put("remarks", b.remarks)
            }
        }))

        root.put("events", JSONArray(repository.allEvents.first().map { v ->
            JSONObject().apply {
                put("title", v.title); put("dateString", v.dateString); put("timeString", v.timeString)
                put("location", v.location); put("notes", v.notes); put("type", v.type)
                put("isAiGenerated", v.isAiGenerated); put("createdAt", v.createdAt)
                put("remindDayBefore", v.remindDayBefore); put("isDeleted", v.isDeleted); put("remarks", v.remarks)
            }
        }))

        root.put("diaryEntries", JSONArray(repository.allDiaryEntries.first().map { d ->
            JSONObject().apply {
                put("dateString", d.dateString); put("text", d.text); put("mood", d.mood)
                put("photoPath", d.photoPath); put("timestamp", d.timestamp); put("isDeleted", d.isDeleted)
                put("remarks", d.remarks)
            }
        }))

        root.put("notes", JSONArray(repository.allNotes.first().map { n ->
            JSONObject().apply {
                put("title", n.title); put("content", n.content); put("isPinned", n.isPinned)
                put("isFavorite", n.isFavorite); put("timestamp", n.timestamp); put("tag", n.tag)
                put("isDeleted", n.isDeleted); put("remarks", n.remarks)
            }
        }))

        root.put("memories", JSONArray(repository.allMemories.first().map { m ->
            JSONObject().apply {
                put("content", m.content); put("category", m.category); put("timestamp", m.timestamp)
                put("isDeleted", m.isDeleted); put("remarks", m.remarks)
            }
        }))

        // Use all smart reminders
        root.put("smartReminders", JSONArray(db.smartReminderDao().getAll().first().map { s ->
            JSONObject().apply {
                put("title", s.title); put("dueDateTime", s.dueDateTime); put("priority", s.priority)
                put("repeatIntervalMinutes", s.repeatIntervalMinutes); put("maxRepeats", s.maxRepeats)
                put("currentRepeat", s.currentRepeat); put("isAcknowledged", s.isAcknowledged)
                put("isDeleted", s.isDeleted); put("remarks", s.remarks)
            }
        }))

        root.put("voiceNotes", JSONArray(repository.allVoiceNotes.first().map { v ->
            JSONObject().apply {
                put("filePath", v.filePath); put("transcription", v.transcription); put("duration", v.duration)
                put("timestamp", v.timestamp); put("isTranscribed", v.isTranscribed); put("status", v.status)
                put("category", v.category); put("isDeleted", v.isDeleted); put("remarks", v.remarks)
            }
        }))

        root.put("goals", JSONArray(repository.allGoals.first().map { g ->
            JSONObject().apply {
                put("title", g.title); put("progress", g.progress); put("isDone", g.isDone)
                put("deadline", g.deadline); put("createdAt", g.createdAt); put("milestones", g.milestones)
                put("isDeleted", g.isDeleted); put("remarks", g.remarks); put("timestamp", g.timestamp)
            }
        }))

        root.put("recurringReminders", JSONArray(repository.allRecurringReminders.first().map { r ->
            JSONObject().apply {
                put("title", r.title); put("type", r.type); put("time", r.time)
                put("isActive", r.isActive); put("createdAt", r.createdAt); put("isDeleted", r.isDeleted)
                put("remarks", r.remarks)
            }
        }))

        root.put("remindLinks", JSONArray(repository.allRemindLinks.first().map { l ->
            JSONObject().apply {
                put("chatId", l.chatId); put("text", l.text); put("link", l.link)
                put("dueDateTime", l.dueDateTime); put("originalMsgId", l.originalMsgId)
                put("isAcknowledged", l.isAcknowledged); put("createdAt", l.createdAt)
                put("isDeleted", l.isDeleted); put("remarks", l.remarks)
            }
        }))

        root.put("privateSpaceItems", JSONArray(repository.allPrivateSpaceItems.first().map { p ->
            JSONObject().apply {
                put("title", p.title); put("content", p.content); put("category", p.category)
                put("isPinned", p.isPinned); put("photoPath", p.photoPath); put("createdAt", p.createdAt)
                put("modifiedAt", p.modifiedAt); put("isDeleted", p.isDeleted); put("remarks", p.remarks)
            }
        }))

        return root.toString(2)
    }

    fun generateBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            backupDataJson.value = getFullBackupJson()
        }
    }

    fun restoreBackup(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            
            // Stability Fix: Basic validation before restoring
            if (!root.has("tasks") && !root.has("expenses") && !root.has("notes")) {
                return false 
            }

            viewModelScope.launch {
                // Restore each category safely
                if (root.has("tasks")) {
                    val arr = root.getJSONArray("tasks")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertTask(Task(
                            title = obj.getString("title"), isCompleted = obj.optBoolean("isCompleted"),
                            priority = obj.optString("priority", "Medium"), label = obj.optString("label", "General"),
                            dueDate = obj.optString("dueDate"), notes = obj.optString("notes"),
                            isRepeating = obj.optBoolean("isRepeating"), repeatInterval = obj.optString("repeatInterval"),
                            createdDate = obj.optString("createdDate"), doneDate = obj.optString("doneDate"),
                            isDeleted = obj.optBoolean("isDeleted"), remarks = obj.optString("remarks"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        ))
                    }
                }
                
                if (root.has("reminders")) {
                    val arr = root.getJSONArray("reminders")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertReminder(Reminder(
                            title = obj.getString("title"), dueDateTime = obj.getLong("dueDateTime"),
                            recurrence = obj.optString("recurrence", "None"), isAcknowledged = obj.optBoolean("isAcknowledged"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()), chatId = obj.optString("chatId"),
                            lastFired = obj.optLong("lastFired"), remarks = obj.optString("remarks"), isDeleted = obj.optBoolean("isDeleted")
                        ))
                    }
                }

                if (root.has("expenses")) {
                    val arr = root.getJSONArray("expenses")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertExpense(Expense(
                            amount = obj.getDouble("amount"), title = obj.getString("title"),
                            isIncome = obj.optBoolean("isIncome"), category = obj.optString("category", "Food"),
                            dateString = obj.getString("dateString"), timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isDeleted = obj.optBoolean("isDeleted"), remarks = obj.optString("remarks")
                        ))
                    }
                }

                if (root.has("habits")) {
                    val arr = root.getJSONArray("habits")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertHabit(Habit(
                            name = obj.getString("name"), type = obj.optString("type", "Water"),
                            emoji = obj.optString("emoji", "✅"), loggedDaysCommaSeparated = obj.optString("loggedDaysCommaSeparated"),
                            streakCount = obj.optInt("streakCount"), bestStreak = obj.optInt("bestStreak"),
                            lastLoggedTimestamp = obj.optLong("lastLoggedTimestamp"), targetPerDay = obj.optString("targetPerDay"),
                            isDeleted = obj.optBoolean("isDeleted"), remarks = obj.optString("remarks")
                        ))
                    }
                }

                if (root.has("waterLogs")) {
                    val arr = root.getJSONArray("waterLogs")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertWaterLog(WaterLog(
                            mlAmount = obj.getInt("mlAmount"), timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            dayString = obj.getString("dayString"), isDeleted = obj.optBoolean("isDeleted"), remarks = obj.optString("remarks")
                        ))
                    }
                }

                if (root.has("bills")) {
                    val arr = root.getJSONArray("bills")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertBill(Bill(
                            name = obj.getString("name"), amount = obj.getDouble("amount"), category = obj.optString("category", "Mobile"),
                            dueDayOfMonth = obj.optInt("dueDayOfMonth", 1), paidMonthsCommaSeparated = obj.optString("paidMonthsCommaSeparated"),
                            isAutoPay = obj.optBoolean("isAutoPay"), paymentMethod = obj.optString("paymentMethod"), notes = obj.optString("notes"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()), isDeleted = obj.optBoolean("isDeleted"), remarks = obj.optString("remarks")
                        ))
                    }
                }

                if (root.has("events")) {
                    val arr = root.getJSONArray("events")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertEvent(CalendarEvent(
                            title = obj.getString("title"), dateString = obj.getString("dateString"),
                            timeString = obj.optString("timeString", "09:00"), location = obj.optString("location"),
                            notes = obj.optString("notes"), type = obj.optString("type", "Meeting"),
                            isAiGenerated = obj.optBoolean("isAiGenerated"), createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            remindDayBefore = obj.optBoolean("remindDayBefore", true), isDeleted = obj.optBoolean("isDeleted"), remarks = obj.optString("remarks")
                        ))
                    }
                }

                if (root.has("diaryEntries")) {
                    val arr = root.getJSONArray("diaryEntries")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertDiaryEntry(DiaryEntry(
                            dateString = obj.getString("dateString"), text = obj.getString("text"),
                            mood = obj.optString("mood", "Neutral"), photoPath = obj.optString("photoPath"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()), isDeleted = obj.optBoolean("isDeleted"), remarks = obj.optString("remarks")
                        ))
                    }
                }

                if (root.has("notes")) {
                    val arr = root.getJSONArray("notes")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertNote(QuickNote(
                            title = obj.getString("title"), content = obj.getString("content"),
                            isPinned = obj.optBoolean("isPinned"), isFavorite = obj.optBoolean("isFavorite"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()), tag = obj.optString("tag", "Notes"),
                            isDeleted = obj.optBoolean("isDeleted"), remarks = obj.optString("remarks")
                        ))
                    }
                }

                if (root.has("memories")) {
                    val arr = root.getJSONArray("memories")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertMemory(PersonalMemory(
                            content = obj.getString("content"), category = obj.optString("category", "General"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()), isDeleted = obj.optBoolean("isDeleted"), remarks = obj.optString("remarks")
                        ))
                    }
                }

                if (root.has("smartReminders")) {
                    val arr = root.getJSONArray("smartReminders")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertSmartReminder(SmartReminder(
                            title = obj.getString("title"), dueDateTime = obj.getLong("dueDateTime"),
                            priority = obj.getString("priority"), repeatIntervalMinutes = obj.getInt("repeatIntervalMinutes"),
                            maxRepeats = obj.getInt("maxRepeats"), currentRepeat = obj.optInt("currentRepeat"),
                            isAcknowledged = obj.optBoolean("isAcknowledged"), isDeleted = obj.optBoolean("isDeleted"), remarks = obj.optString("remarks")
                        ))
                    }
                }

                if (root.has("voiceNotes")) {
                    val arr = root.getJSONArray("voiceNotes")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertVoiceNote(VoiceNote(
                            filePath = obj.getString("filePath"), transcription = obj.getString("transcription"),
                            duration = obj.getLong("duration"), timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isTranscribed = obj.optBoolean("isTranscribed"), status = obj.optString("status", "Success"),
                            category = obj.optString("category", "General"), isDeleted = obj.optBoolean("isDeleted"), remarks = obj.optString("remarks")
                        ))
                    }
                }

                if (root.has("goals")) {
                    val arr = root.getJSONArray("goals")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertGoal(Goal(
                            title = obj.getString("title"), progress = obj.optInt("progress"),
                            isDone = obj.optBoolean("isDone"), deadline = obj.optString("deadline"),
                            createdAt = obj.optString("createdAt"), milestones = obj.optString("milestones"),
                            isDeleted = obj.optBoolean("isDeleted"), remarks = obj.optString("remarks"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        ))
                    }
                }

                if (root.has("recurringReminders")) {
                    val arr = root.getJSONArray("recurringReminders")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertRecurringReminder(RecurringReminder(
                            title = obj.getString("title"), type = obj.getString("type"),
                            time = obj.getString("time"), isActive = obj.optBoolean("isActive", true),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()), isDeleted = obj.optBoolean("isDeleted"), remarks = obj.optString("remarks")
                        ))
                    }
                }

                if (root.has("remindLinks")) {
                    val arr = root.getJSONArray("remindLinks")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertRemindLink(RemindLink(
                            chatId = obj.getLong("chatId"), text = obj.getString("text"),
                            link = obj.getString("link"), dueDateTime = obj.getString("dueDateTime"),
                            originalMsgId = obj.optLong("originalMsgId"), isAcknowledged = obj.optBoolean("isAcknowledged"),
                            createdAt = obj.optString("createdAt"), isDeleted = obj.optBoolean("isDeleted"), remarks = obj.optString("remarks")
                        ))
                    }
                }

                if (root.has("privateSpaceItems")) {
                    val arr = root.getJSONArray("privateSpaceItems")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        repository.insertPrivateSpaceItem(PrivateSpaceItem(
                            title = obj.getString("title"), content = obj.getString("content"),
                            category = obj.optString("category", "note"), isPinned = obj.optBoolean("isPinned"),
                            photoPath = obj.optString("photoPath"), createdAt = obj.optString("createdAt"),
                            modifiedAt = obj.optString("modifiedAt"), isDeleted = obj.optBoolean("isDeleted"), remarks = obj.optString("remarks")
                        ))
                    }
                }
            }
            true
        } catch (e: Exception) { false }
    }

    fun exportBackupJson(outputStream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = getFullBackupJson()
                outputStream.write(json.toByteArray(Charsets.UTF_8))
                outputStream.close()
                backupDataJson.value = json // Update for UI if needed
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
                
                // Auto-detect if this is a command (Task/Expense/Reminder)
                val autoResult = trySmartAiParsing(text)
                if (autoResult != null) {
                    speak("I've also $autoResult")
                    repository.insertChatMessage(ChatMessage(
                        chatSessionId = currentChatSessionId.value,
                        sender = "Rk",
                        text = "Voice Note processed: $autoResult"
                    ))
                }
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
    fun addPrivateSpaceItem(title: String, content: String, category: String, photoPath: String = "") {
        viewModelScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            repository.insertPrivateSpaceItem(PrivateSpaceItem(
                title = title, content = cryptoManager.encryptString(content), category = category,
                photoPath = photoPath,
                createdAt = now, modifiedAt = now
            ))
        }
    }

    fun updatePrivateSpaceItem(item: PrivateSpaceItem) {
        viewModelScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            // Item in memory is already decrypted for UI, encrypt it before saving
            repository.updatePrivateSpaceItem(item.copy(
                content = cryptoManager.encryptString(item.content),
                modifiedAt = now
            ))
        }
    }

    fun togglePrivateSpaceItemPin(item: PrivateSpaceItem) {
        viewModelScope.launch {
            repository.updatePrivateSpaceItem(item.copy(isPinned = !item.isPinned))
        }
    }

    fun deletePrivateSpaceItem(item: PrivateSpaceItem) {
        viewModelScope.launch { repository.deletePrivateSpaceItem(item) }
    }



    // --- Google Sheets Sync ---
    fun syncToGoogleSheets(scriptUrl: String) {
        // Security Fix: Whitelist script.google.com
        if (!scriptUrl.contains("script.google.com")) {
            lastSyncStatus.value = "Error: Invalid URL. Only script.google.com is allowed."
            return
        }

        if (scriptUrl.isBlank()) {
            lastSyncStatus.value = "Error: Google Script URL is missing."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            isSyncing.value = true
            lastSyncStatus.value = "Syncing data to Google Sheets..."

            try {
                val success = SyncHelper.performSync(db, scriptUrl)
                if (success) {
                    lastSyncStatus.value = "✅ Sync Successful! (${SimpleDateFormat("HH:mm", Locale.US).format(Date())})"
                } else {
                    lastSyncStatus.value = "❌ Sync Failed. Check script doPost(e) & URL."
                }
            } catch (e: Exception) {
                lastSyncStatus.value = "❌ Error: ${e.localizedMessage}"
            } finally {
                isSyncing.value = false
            }
        }
    }

    fun deleteModel() {
        val context = getApplication<Application>()
        val file = java.io.File(context.getExternalFilesDir(null), LocalLLMService.MODEL_PATH)
        if (file.exists()) {
            file.delete()
        }
        LocalLLMService.reset()
        isLocalAiAvailable.value = false
        Toast.makeText(context, "Model deleted.", Toast.LENGTH_SHORT).show()
    }

    fun importModelFromFile(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            isImportingModel.value = true
            try {
                val context = getApplication<Application>()
                val file = java.io.File(context.getExternalFilesDir(null), LocalLLMService.MODEL_PATH)
                val outputStream = java.io.FileOutputStream(file)
                inputStream.copyTo(outputStream)
                outputStream.close()
                inputStream.close()
                
                withContext(Dispatchers.Main) {
                    LocalLLMService.initialize(context)
                    isLocalAiAvailable.value = LocalLLMService.isModelAvailable(context)
                    isImportingModel.value = false
                    Toast.makeText(context, "Model imported successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isImportingModel.value = false
                    Toast.makeText(getApplication(), "Failed to import model: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun downloadOfflineModel() {
        val context = getApplication<Application>()
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        
        val request = android.app.DownloadManager.Request(android.net.Uri.parse(LocalLLMService.MODEL_URL))
            .setTitle("Downloading RK Offline AI Model")
            .setDescription("Qwen 2.5 1.5B Model (1.6GB) - Please wait...")
            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, LocalLLMService.MODEL_PATH)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setAllowedNetworkTypes(android.app.DownloadManager.Request.NETWORK_WIFI or android.app.DownloadManager.Request.NETWORK_MOBILE)
        
        val downloadId = downloadManager.enqueue(request)
        
        viewModelScope.launch(Dispatchers.IO) {
            var downloading = true
            while (downloading) {
                val query = android.app.DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val statusIdx = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS)
                    val bytesDownloadedIdx = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIdx = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    val status = if (statusIdx != -1) cursor.getInt(statusIdx) else -1
                    val bytesDownloaded = if (bytesDownloadedIdx != -1) cursor.getInt(bytesDownloadedIdx) else 0
                    val bytesTotal = if (bytesTotalIdx != -1) cursor.getInt(bytesTotalIdx) else 0
                    
                    if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false
                        modelDownloadProgress.value = 1f
                        withContext(Dispatchers.Main) {
                            LocalLLMService.initialize(context)
                            isLocalAiAvailable.value = LocalLLMService.isModelAvailable(context)
                            if (isLocalAiAvailable.value) {
                                Toast.makeText(context, "Model setup successful!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Setup failed! Model file not found.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else if (status == android.app.DownloadManager.STATUS_FAILED) {
                        downloading = false
                        modelDownloadProgress.value = null
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Download failed!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        if (bytesTotal > 0) {
                            modelDownloadProgress.value = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                        }
                    }
                }
                cursor.close()
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun sendDaySummary() {
        val today = getTodayDateString()
        val completedTasksToday = tasks.value.count { it.isCompleted && it.doneDate == today }
        val pendingTasks = tasks.value.count { !it.isCompleted }
        val todayExpenses = expenses.value.filter { it.dateString == today && !it.isIncome }.sumOf { it.amount }.toInt()
        val waterIntake = todayWaterSum.value
        val waterGoal = prefs.getWaterGoal()
        
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
        val tomorrowEvents = events.value.filter { it.dateString == tomorrow }
        
        val eventText = if (tomorrowEvents.isEmpty()) {
            "Kal ke liye koi meetings schedule nahi hain."
        } else {
            "📅 Aapka kal ka schedule:\n" + tomorrowEvents.joinToString("\n") { "• ${it.timeString}: ${it.title}" }
        }

        val summary = """
            Greetings Boss! RK at your service. 
            
            Aaj aapne $completedTasksToday tasks successfully pure kiye hain, aur $pendingTasks abhi queue mein hain. 
            Financial status: Aaj ka total kharch ₹$todayExpenses hai. 
            Hydration level: ${waterIntake}ml (Goal: ${waterGoal}ml).
            
            $eventText
            
            Systems are nominal. Main hamesha aapki madad ke liye taiyar hoon.
        """.trimIndent()

        viewModelScope.launch {
            val session = currentChatSessionId.value
            repository.insertChatMessage(ChatMessage(chatSessionId = session, sender = "Rk", text = summary))
            speak(summary)
        }
    }
}
