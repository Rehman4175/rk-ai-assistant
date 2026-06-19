package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R

object NotificationHelper {
    private const val CHANNEL_ID = "rk_ai_assistant_channel"
    private const val CHANNEL_NAME = "RK AI Assistant"
    private const val PREF_TONE_URI = "notification_tone_uri"

    fun createNotificationChannel(context: Context, toneUri: Uri? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "RK AI Assistant notifications"

                // ✅ Custom notification tone
                if (toneUri != null) {
                    setSound(toneUri, AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                }
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun setCustomTone(context: Context, uri: Uri?) {
        if (uri != null) {
            context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_TONE_URI, uri.toString())
                .apply()

            createNotificationChannel(context, uri)
        }
    }

    fun getCustomTone(context: Context): Uri? {
        val uriString = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
            .getString(PREF_TONE_URI, null)
        return uriString?.let { Uri.parse(it) }
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        val manager = NotificationManagerCompat.from(context)
        val tone = getCustomTone(context) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(tone)
            .setVibrate(longArrayOf(0, 500, 200, 500))

        manager.notify(notificationId, builder.build())
    }
}