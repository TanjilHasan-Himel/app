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
import java.util.Calendar

@HiltWorker
class SongRecommendationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dataStore: PlayTimeDataStore
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "song_recommendations"
        const val CHANNEL_NAME = "Music Recommendations"
        const val NOTIFICATION_ID = 2001
    }

    override suspend fun doWork(): Result {
        // 1. Check Time Window (6 AM - 9 PM) - User requested extension to Late Night (up to 3 AM)
        // Actually user time table goes from 5 AM to 3 AM... so basically 24/7 or close to it.
        // Let's remove the strict limitation or widen it significantly.
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // User mentioned "Late Night (10PM - 3AM)".
        // If we want to avoid 3 AM - 5 AM (dead zone), we can add check.
        if (hour in 3..4) {
             return Result.success()
        }

        // Send app-level contextual notification (no random song selection).
        val message = EngagementMessages.getMessageForTime(hour)
        sendNotification(message)

        return Result.success()
    }

    private fun sendNotification(message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        createNotificationChannel()

        // Open PlayerScreen/Home and Play this song?
        // Ideally we should start playback or open player with this song queued.
        // For simplicity, launch MainActivity.
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // We could pass an extra here to auto-play this song
            // putExtra("AUTO_PLAY_URI", song.uri)
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Custom Layout for Notification? Standard BigText is fine.
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle("Audia Player")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(applicationContext.resources, R.drawable.audia_player))
            .setSubText("Music update")

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Periodic song recommendations"
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
