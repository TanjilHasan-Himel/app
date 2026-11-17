package com.eplaytime.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eplaytime.app.MainActivity
import com.eplaytime.app.R
import com.eplaytime.app.data.datastore.PlayTimeDataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class EngagementWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dataStore: PlayTimeDataStore
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "daily_reminders"
        const val CHANNEL_NAME = "Daily Reminders"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val lastNotified = dataStore.lastNotificationTime.first()
        val enableDailyReminders = dataStore.enableDailyReminders.first()
        val lastOpened = dataStore.lastOpenedTime.first()
        if (!enableDailyReminders) return Result.success()
        val userName = dataStore.userName.first() ?: "Friend"
        val lastSongTitle = dataStore.playbackState.first().lastSongTitle
        
        val currentTime = System.currentTimeMillis()
        val hoursSinceNotified = TimeUnit.MILLISECONDS.toHours(currentTime - lastNotified)
        val hoursSinceOpened = TimeUnit.MILLISECONDS.toHours(currentTime - lastOpened)

        // Don't spam: Ensure at least 4 hours since last notification
        if (hoursSinceNotified < 4) {
            return Result.success()
        }

        // Only notify if app hasn't been opened in 24 hours, OR if there's a recent paused song (>3h)
        val isRetention = hoursSinceOpened >= 24
        val isResumeReminder = hoursSinceOpened in 3..23 && lastSongTitle.isNotEmpty()

        if (!isRetention && !isResumeReminder) {
            return Result.success()
        }

        val calendar = java.util.Calendar.getInstance()
        val hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY)

        // Randomly decide whether to show a "resume song" message if a song is paused (1 in 3 chance)
        val showResumeSong = isResumeReminder

        val message = if (showResumeSong) {
            "Continue listening to $lastSongTitle?"
        } else {
            when (hourOfDay) {
                in 6..11 -> "Good morning, $userName. Start your day with great sound."
                in 12..16 -> "Good afternoon, $userName. Take a break with your favorite tracks."
                in 17..21 -> "Good evening, $userName. Unwind with your music library."
                else -> "Late night hours. Relax with your favorite tunes, $userName."
            }
        }

        if (sendNotification(message)) {
            dataStore.setLastNotificationTime(currentTime)
        }

        return Result.success()
    }
    private fun sendNotification(message: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        createNotificationChannel()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_audia_notification)
            .setContentTitle("Audia Player")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(applicationContext.resources, R.drawable.audia_player_logo))

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, builder.build())
        return true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Daily reminders to listen to music"
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
