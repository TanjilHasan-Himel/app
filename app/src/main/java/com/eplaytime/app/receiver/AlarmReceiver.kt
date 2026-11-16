package com.eplaytime.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.eplaytime.app.service.MusicService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * AlarmReceiver - Handles scheduled playback alarms (USP Feature).
 * Fired by AlarmManager to start music playback at scheduled time.
 *
 * USP Feature: Automatically wakes device and plays music without user interaction
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SCHEDULED_PLAYBACK = "com.eplaytime.app.ACTION_SCHEDULED_PLAYBACK"
        const val EXTRA_SONG_URI = "extra_song_uri"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_TARGET_VOLUME = "extra_target_volume"
        private const val TAG = "AlarmReceiver"
        private const val WAKELOCK_TIMEOUT = 10_000L // 10 seconds
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.e(TAG, "❌ Context or Intent is null!")
            return
        }

        if (intent.action == ACTION_SCHEDULED_PLAYBACK) {
            Log.d(TAG, "🔔 ========== ALARM TRIGGERED ==========")

            // Extract alarm data
            val songUri = intent.getStringExtra(EXTRA_SONG_URI)
            val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
            val targetVolume = intent.getFloatExtra(EXTRA_TARGET_VOLUME, 0.5f)

            Log.d(TAG, "Alarm ID: $alarmId | Song URI: $songUri | Target Volume: $targetVolume")

            // Acquire WakeLock immediately to keep device awake
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "PlayTime::AlarmWakeLock"
            ).apply {
                acquire(WAKELOCK_TIMEOUT)
                Log.d(TAG, "✓ WakeLock acquired for $WAKELOCK_TIMEOUT ms")
            }

            val pendingResult = goAsync()
            val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

            scope.launch {
                try {
                    // 1. Fetch Task from Database
                    Log.d(TAG, "Fetching task from database...")
                    val db = com.eplaytime.app.data.database.PlayTimeDatabase.getDatabase(context)
                    val task = db.scheduledTaskDao().getTaskById(alarmId)

                    if (task == null) {
                        Log.w(TAG, "❌ Task $alarmId not found in database!")
                        return@launch
                    }

                    if (!task.isEnabled) {
                        Log.w(TAG, "⏸️  Task $alarmId is disabled. Skipping playback.")
                        return@launch
                    }

                    // Persist the next occurrence before starting playback, so a service crash
                    // cannot erase a recurring schedule's next cycle.
                    if (task.repeatDays.isNotEmpty()) {
                        val nextRunAt = com.eplaytime.app.util.AlarmScheduler(context).scheduleAlarm(task)
                        if (nextRunAt != null) {
                            db.scheduledTaskDao().updateSchedulingState(task.id, nextRunAt, task.lastResult)
                        }
                    }

                    val scheduledUris = task.contentUris.lineSequence()
                        .filter(String::isNotBlank)
                        .toList()
                        .ifEmpty { listOf(task.songUri) }
                    val availableUris = scheduledUris.filter { uri ->
                        runCatching {
                            context.contentResolver.openAssetFileDescriptor(
                                android.net.Uri.parse(uri),
                                "r"
                            )?.use { true } ?: false
                        }.getOrDefault(false)
                    }
                    if (availableUris.isEmpty()) {
                        db.scheduledTaskDao().updateRunState(
                            task.id,
                            System.currentTimeMillis(),
                            "MISSING_CONTENT",
                            "",
                            0,
                            0
                        )
                        if (task.repeatDays.isEmpty()) db.scheduledTaskDao().toggleTask(task.id, false)
                        Log.e(TAG, "Scheduled content is no longer readable for task ${task.id}")
                        return@launch
                    }

                    // 2. Validate Day (CRITICAL LOGIC - Must check selected days)
                    Log.d(TAG, "Validating day... Repeat days: ${task.repeatDays}")
                    val shouldPlay = if (task.repeatDays.isEmpty()) {
                        Log.d(TAG, "One-time alarm (no repeat days selected)")
                        true
                    } else {
                        val selectedDays = task.repeatDays
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .toSet()

                        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
                        val dayMap = mapOf(
                            java.util.Calendar.SUNDAY to "SUN",
                            java.util.Calendar.MONDAY to "MON",
                            java.util.Calendar.TUESDAY to "TUE",
                            java.util.Calendar.WEDNESDAY to "WED",
                            java.util.Calendar.THURSDAY to "THU",
                            java.util.Calendar.FRIDAY to "FRI",
                            java.util.Calendar.SATURDAY to "SAT"
                        )
                        val todayString = dayMap[today]
                        Log.d(TAG, "Today: $todayString | Selected days: $selectedDays")
                        todayString != null && selectedDays.contains(todayString)
                    }

                    if (shouldPlay) {
                        Log.d(TAG, "✓ ✓ ✓ Day validation PASSED. Starting playback now.")
                        // Start MusicService as foreground service
                        val serviceIntent = Intent(context, MusicService::class.java).apply {
                            action = MusicService.ACTION_PLAY_ALARM
                            putExtra(EXTRA_SONG_URI, if (task.songUri.isNotEmpty()) task.songUri else songUri)
                            putExtra(EXTRA_ALARM_ID, alarmId)
                            putExtra(EXTRA_TARGET_VOLUME, task.targetVolume)
                            putExtra("extra_song_title", task.songTitle)
                            putStringArrayListExtra(
                                MusicService.EXTRA_ALARM_CONTENT_URIS,
                                ArrayList(
                                    availableUris
                                )
                            )
                            putExtra(MusicService.EXTRA_ALARM_FADE_SECONDS, task.fadeInSeconds)
                            putExtra(MusicService.EXTRA_ALARM_STOP_AFTER_MINUTES, task.stopAfterMinutes)
                            putExtra(MusicService.EXTRA_ALARM_SINGLE_SHOT, true)
                        }
                        db.scheduledTaskDao().updateRunState(
                            taskId = task.id,
                            runAt = System.currentTimeMillis(),
                            result = "STARTING",
                            cycleId = "${task.id}:${System.currentTimeMillis()}",
                            trackIndex = 0,
                            recoveryAttempts = 0
                        )
                        ContextCompat.startForegroundService(context, serviceIntent)
                        Log.d(TAG, "✓ MusicService started as foreground service")
                    } else {
                        Log.d(TAG, "❌ Day validation FAILED. Today is not in repeat days. Alarm will NOT play.")
                    }

                    // 3. Reschedule repeating alarms or disable one-time alarms
                    if (task.repeatDays.isEmpty()) {
                        if (shouldPlay) {
                            Log.d(TAG, "One-time alarm played. Disabling...")
                            db.scheduledTaskDao().toggleTask(task.id, false)
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Critical error in AlarmReceiver", e)
                    e.printStackTrace()
                } finally {
                    if (wakeLock.isHeld) {
                        wakeLock.release()
                        Log.d(TAG, "✓ WakeLock released")
                    }
                    pendingResult.finish()
                    Log.d(TAG, "🔔 ========== ALARM HANDLING COMPLETE ==========")
                }
            }
        }
    }
}
