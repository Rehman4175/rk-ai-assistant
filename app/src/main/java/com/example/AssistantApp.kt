package com.example

import android.app.Application

class AssistantApp : Application() {
    companion object {
        init {
            try {
                // Ensure SQLCipher native libraries are loaded correctly
                System.loadLibrary("sqlcipher")
            } catch (e: Throwable) {
                // Ignore errors here; it might be already loaded or not needed yet
                android.util.Log.w("RKAI", "SQLCipher native load attempt: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Gemini Service
        val prefs = com.example.data.SecurePrefHelper(this)
        com.example.data.GeminiService.initialize(prefs.getGeminiApiKey())
    }
}
