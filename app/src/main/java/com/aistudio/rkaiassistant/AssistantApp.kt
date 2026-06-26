package com.aistudio.rkaiassistant

import android.app.Application
import android.util.Log
import com.aistudio.rkaiassistant.data.GeminiService
import com.aistudio.rkaiassistant.data.SecurePrefHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class AssistantApp : Application() {

    companion object {
        private const val TAG = "AssistantApp"
        lateinit var instance: AssistantApp
            private set

        // Application-wide coroutine scope
        val appScope = CoroutineScope(Dispatchers.IO)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Log.d(TAG, "Application is initializing...")

        // 1. Initialize SQLCipher native libraries (CRITICAL for encrypted database)
        try {
            System.loadLibrary("sqlcipher")
            Log.d(TAG, "SQLCipher native libraries loaded successfully.")
        } catch (t: Throwable) {
            Log.e(TAG, "CRITICAL: SQLCipher load failed: ${t.message}", t)
        }

        // 2. Initialize Gemini Service safely
        initializeGeminiService()

        // 3. Initialize Google Auth with your Client ID
        com.aistudio.rkaiassistant.data.GoogleAuthHelper.serverClientId = "879298355170-rp7s7vngjg60vhpn6p55cfoi86c1ort2.apps.googleusercontent.com"

        // 4. Schedule background workers (Sync, Backup, Reminders)
        try {
            com.aistudio.rkaiassistant.data.scheduleAllWorkers(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule workers", e)
        }
    }

    /**
     * Initialize Gemini API service
     */
    private fun initializeGeminiService() {
        try {
            val prefs = SecurePrefHelper(this)
            val apiKey = prefs.getGeminiApiKey()

            if (apiKey.isNullOrEmpty()) {
                Log.w(TAG, "Gemini API key not found in SecurePrefs. Please add it.")
            } else {
                // This triggers object initialization of GeminiService
                GeminiService.initialize(apiKey)
                Log.d(TAG, "Gemini Service initialized successfully.")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "CRITICAL: Gemini Service initialization failed: ${t.message}", t)
        }
    }
}
