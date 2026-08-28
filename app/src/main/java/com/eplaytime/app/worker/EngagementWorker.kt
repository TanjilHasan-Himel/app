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
        val lastOpened = dataStore.lastOpenedTime.first()
        val lastNotified = dataStore.lastNotificationTime.first()
        val userName = dataStore.userName.first() ?: "Friend"
        
        val currentTime = System.currentTimeMillis()
        val hoursSinceOpen = TimeUnit.MILLISECONDS.toHours(currentTime - lastOpened)
        val hoursSinceNotified = TimeUnit.MILLISECONDS.toHours(currentTime - lastNotified)

        // Don't spam: Ensure at least 12 hours since last notification
        if (hoursSinceNotified < 12) {
            return Result.success()
        }

        // Logic for message
        val message = when {
            hoursSinceOpen in 12..23 -> "Hey $userName, take a music break! 🎵"
            hoursSinceOpen in 24..71 -> "Come back and continue your vibe on Audia. 🎧"
            hoursSinceOpen >= 72 -> "We miss you, $userName! The music is waiting. 🥺"
            else -> return Result.success() // Less than 12 hours inactive
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
            .setSmallIcon(R.drawable.logo_icon)
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
