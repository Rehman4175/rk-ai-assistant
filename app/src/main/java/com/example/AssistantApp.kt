package com.example

import android.app.Application
import com.example.data.SecurePrefHelper
import com.example.data.GeminiService
import android.util.Log

class AssistantApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        try {
            // Load SQLCipher native libraries
            System.loadLibrary("sqlcipher")
        } catch (e: Throwable) {
            Log.e("RKAI", "SQLCipher native load failed: ${e.message}")
        }
        
        // Initialize Gemini Service
        val prefs = SecurePrefHelper(this)
        GeminiService.initialize(prefs.getGeminiApiKey())
    }
}
