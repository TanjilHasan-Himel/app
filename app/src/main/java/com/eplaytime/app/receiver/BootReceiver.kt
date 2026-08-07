package com.eplaytime.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.eplaytime.app.worker.BootRescheduleWorker
import java.util.concurrent.TimeUnit

/**
 * BootReceiver - Reschedules alarms after device restart.
 * Ensures scheduled playback tasks persist across reboots.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val PREFS_NAME = "boot_receiver_prefs"
        private const val KEY_LAST_ENQUEUE_MS = "last_enqueue_ms"
        private const val DUPLICATE_WINDOW_MS = 60_000L
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val last = prefs.getLong(KEY_LAST_ENQUEUE_MS, 0L)
            if (now - last < DUPLICATE_WINDOW_MS) {
                Log.d(TAG, "Ignoring duplicate boot broadcast within debounce window")
                return
            }
            prefs.edit().putLong(KEY_LAST_ENQUEUE_MS, now).apply()

            Log.d(TAG, "Device boot completed - enqueueing alarm reschedule worker")
            val request = OneTimeWorkRequestBuilder<BootRescheduleWorker>()
                .setInitialDelay(45, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "boot_reschedule_alarms",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
