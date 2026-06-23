package com.aistudio.rkaiassistant.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reschedules all WorkManager workers after device reboot
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            scheduleAllWorkers(context)
            
            // Reschedule exact alarms
            CoroutineScope(Dispatchers.IO).launch {
                AlarmHelper.rescheduleAllAlarms(context)
            }
        }
    }
}
