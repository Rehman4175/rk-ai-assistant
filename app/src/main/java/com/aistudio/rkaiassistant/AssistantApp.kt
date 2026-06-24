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

        // 2. Initialize Gemini Service safely
        initializeGeminiService()
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
