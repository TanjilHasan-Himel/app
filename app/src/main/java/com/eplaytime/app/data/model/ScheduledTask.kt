package com.eplaytime.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ScheduledTask - Represents a scheduled music playback alarm
 */
@Entity(tableName = "scheduled_tasks")
data class ScheduledTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songUri: String,
    val songTitle: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val targetVolume: Float = 0.5f, // 0.0 to 1.0 (50% default)
    val repeatDays: String = "", // Comma-separated: "MON,TUE,WED" or empty for one-time
    val label: String = "",
    val snoozeEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    /** TRACK, ALBUM, FOLDER, PLAYLIST or QUEUE. */
    val contentType: String = "TRACK",
    /** Newline-delimited local content URIs; legacy TRACK rows fall back to songUri. */
    val contentUris: String = "",
    /** Zero means play until the selected content finishes. */
    val stopAfterMinutes: Int = 0,
    val fadeInSeconds: Int = 10,
    val lastRunAt: Long = 0L,
    val lastResult: String = "NEVER",
    val activeCycleId: String = "",
    val activeTrackIndex: Int = 0,
    val recoveryAttempts: Int = 0,
    val originalSystemVolume: Int = -1,
    val nextRunAt: Long = 0L
)
