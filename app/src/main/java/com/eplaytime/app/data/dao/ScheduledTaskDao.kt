package com.eplaytime.app.data.dao

import androidx.room.*
import com.eplaytime.app.data.model.ScheduledTask
import kotlinx.coroutines.flow.Flow

/**
 * DAO for scheduled tasks
 */
@Dao
interface ScheduledTaskDao {

    @Query("SELECT * FROM scheduled_tasks ORDER BY hour, minute")
    fun getAllTasks(): Flow<List<ScheduledTask>>

    @Query("SELECT * FROM scheduled_tasks WHERE isEnabled = 1 ORDER BY hour, minute")
    fun getEnabledTasks(): Flow<List<ScheduledTask>>

    @Query("SELECT * FROM scheduled_tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): ScheduledTask?

    @Query(
        """SELECT * FROM scheduled_tasks
           WHERE activeCycleId != '' AND lastResult IN ('STARTING', 'PLAYING', 'INTERRUPTED')
           ORDER BY lastRunAt DESC LIMIT 1"""
    )
    suspend fun getRecoverableTask(): ScheduledTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ScheduledTask): Long

    @Update
    suspend fun updateTask(task: ScheduledTask)

    @Delete
    suspend fun deleteTask(task: ScheduledTask)

    @Query("DELETE FROM scheduled_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("UPDATE scheduled_tasks SET isEnabled = :enabled WHERE id = :taskId")
    suspend fun toggleTask(taskId: Long, enabled: Boolean)

    @Query(
        """UPDATE scheduled_tasks SET lastRunAt = :runAt, lastResult = :result,
           activeCycleId = :cycleId, activeTrackIndex = :trackIndex,
           recoveryAttempts = :recoveryAttempts WHERE id = :taskId"""
    )
    suspend fun updateRunState(
        taskId: Long,
        runAt: Long,
        result: String,
        cycleId: String,
        trackIndex: Int,
        recoveryAttempts: Int
    )

    @Query("UPDATE scheduled_tasks SET originalSystemVolume = :volume WHERE id = :taskId")
    suspend fun updateOriginalSystemVolume(taskId: Long, volume: Int)

    @Query("UPDATE scheduled_tasks SET nextRunAt = :nextRunAt, lastResult = :result WHERE id = :taskId")
    suspend fun updateSchedulingState(taskId: Long, nextRunAt: Long, result: String)
}
