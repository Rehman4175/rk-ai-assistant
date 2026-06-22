package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

class SecurePrefHelper(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Versioned filename to avoid crashes when migrating from unencrypted prefs
    private val prefs: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "rk_assistant_secure_v2",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback for edge cases or corrupted keystore
        context.getSharedPreferences("rk_assistant_secure_v2_fallback", Context.MODE_PRIVATE)
    }

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
        return prefs.getBoolean("settings_dark_theme", true)
    }

    fun setDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean("settings_dark_theme", isDark).apply()
    }

    fun getFontSize(): Float {
        return prefs.getFloat("settings_font_size", 16f)
    }

    fun setFontSize(scale: Float) {
        prefs.edit().putFloat("settings_font_size", scale).apply()
    }

    fun getWaterGoal(): Int {
        return prefs.getInt("settings_water_goal", 2500)
    }

    fun setWaterGoal(goal: Int) {
        prefs.edit().putInt("settings_water_goal", goal).apply()
    }

    fun getExpenseBudget(): Double {
        return prefs.getFloat("settings_ex_budget", 5000f).toDouble()
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

    fun isWelcomeSoundEnabled(): Boolean {
        return prefs.getBoolean("settings_welcome_sound", true)
    }

    fun setWelcomeSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("settings_welcome_sound", enabled).apply()
    }

    fun saveGeminiApiKey(key: String) {
        prefs.edit().putString("GEMINI_API_KEY", key).apply()
    }

    fun getGeminiApiKey(): String {
        return prefs.getString("GEMINI_API_KEY", "") ?: ""
    }

    fun saveWeatherApiKey(key: String) {
        prefs.edit().putString("WEATHER_API_KEY", key).apply()
    }

    fun getWeatherApiKey(): String {
        return prefs.getString("WEATHER_API_KEY", "") ?: ""
    }

    private fun hashString(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input
        }
    }
}
