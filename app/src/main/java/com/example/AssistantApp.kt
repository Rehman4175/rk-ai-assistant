package com.example

import android.app.Application

class AssistantApp : Application() {
    companion object {
        init {
            try {
                System.loadLibrary("sqlcipher")
            } catch (e: Exception) {
                // Ignore if it fails here, will try again in DB init
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
