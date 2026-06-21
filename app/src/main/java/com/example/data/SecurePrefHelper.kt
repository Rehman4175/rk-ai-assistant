package com.example.data

import android.content.Context
import java.security.MessageDigest

class SecurePrefHelper(context: Context) {
    private val prefs = context.getSharedPreferences("rk_assistant_secure_prefs", Context.MODE_PRIVATE)

    fun isPinEnabled(): Boolean {
        return prefs.getString("secure_pin_hash", null) != null
    }

    fun savePin(pin: String) {
        val hash = hashString(pin)
        prefs.edit().putString("secure_pin_hash", hash).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val savedHash = prefs.getString("secure_pin_hash", null) ?: return true
        return savedHash == hashString(pin)
    }

    fun clearPin() {
        prefs.edit().remove("secure_pin_hash").apply()
    }

    fun isAppLockActive(): Boolean {
        return prefs.getBoolean("settings_app_lock_active", false)
    }

    fun setAppLockActive(active: Boolean) {
        prefs.edit().putBoolean("settings_app_lock_active", active).apply()
    }

    fun isDarkTheme(): Boolean {
        return prefs.getBoolean("settings_dark_theme", true) // Default is dark theme
    }

    fun setDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean("settings_dark_theme", isDark).apply()
    }

    fun getFontSize(): Float {
        return prefs.getFloat("settings_font_size", 16f) // Default size: 16sp
    }

    fun setFontSize(scale: Float) {
        prefs.edit().putFloat("settings_font_size", scale).apply()
    }

    fun getWaterGoal(): Int {
        return prefs.getInt("settings_water_goal", 2500) // Default 2500ml
    }

    fun setWaterGoal(goal: Int) {
        prefs.edit().putInt("settings_water_goal", goal).apply()
    }

    fun getExpenseBudget(): Double {
        return prefs.getFloat("settings_ex_budget", 5000f).toDouble() // Default budget
    }

    fun setExpenseBudget(budget: Double) {
        prefs.edit().putFloat("settings_ex_budget", budget.toFloat()).apply()
    }

    fun getGoogleScriptUrl(): String {
        return prefs.getString("google_script_url", "") ?: ""
    }

    fun setGoogleScriptUrl(url: String) {
        prefs.edit().putString("google_script_url", url).apply()
    }

    fun setBatteryOptimizedPromptShown(shown: Boolean) {
        prefs.edit().putBoolean("battery_opt_prompt_shown", shown).apply()
    }

    fun isBatteryOptimizedPromptShown(): Boolean {
        return prefs.getBoolean("battery_opt_prompt_shown", false)
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean("settings_biometric_enabled", true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("settings_biometric_enabled", enabled).apply()
    }

    // --- New Preferences ---
    fun saveNotificationTune(uri: String) {
        prefs.edit().putString("notification_tune_uri", uri).apply()
    }

    fun getNotificationTune(): String {
        return prefs.getString("notification_tune_uri", "") ?: ""
    }

    fun hasPrivateSpacePassword(): Boolean {
        return prefs.getString("private_space_password_hash", null) != null
    }

    fun savePrivateSpacePassword(password: String) {
        val hash = hashString(password)
        prefs.edit().putString("private_space_password_hash", hash).apply()
    }

    fun verifyPrivateSpacePassword(password: String): Boolean {
        val savedHash = prefs.getString("private_space_password_hash", null) ?: return false
        return savedHash == hashString(password)
    }

    private fun hashString(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input // Fallback
        }
    }
}
