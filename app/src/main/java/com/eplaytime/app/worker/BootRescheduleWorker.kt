package com.eplaytime.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eplaytime.app.data.database.PlayTimeDatabase
import com.eplaytime.app.util.AlarmScheduler
import kotlinx.coroutines.flow.first

/**
 * Runs after boot to restore enabled alarms outside BroadcastReceiver time limits.
 */
class BootRescheduleWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "BootRescheduleWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val database = PlayTimeDatabase.getDatabase(applicationContext)
            val taskDao = database.scheduledTaskDao()
            val alarmScheduler = AlarmScheduler(applicationContext)

            val enabledTasks = taskDao.getEnabledTasks().first()
            enabledTasks.forEach { task ->
                alarmScheduler.scheduleAlarm(task)
            }

            Log.d(TAG, "Rescheduled ${enabledTasks.size} alarms after boot")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule alarms after boot", e)
            Result.retry()
        }
    }
}
