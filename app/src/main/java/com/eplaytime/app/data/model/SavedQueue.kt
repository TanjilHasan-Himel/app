package com.eplaytime.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * SavedQueue entity representing a snapshot of playback queue with position.
 */
@Entity(tableName = "saved_queues")
data class SavedQueue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val activeTrackIndex: Int = 0,
    val activePositionMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * SavedQueueItem entity representing an ordered track in a saved queue.
 */
@Entity(
    tableName = "saved_queue_items",
    indices = [
        Index(value = ["queueId"]),
        Index(value = ["queueId", "position"])
    ]
)
data class SavedQueueItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val queueId: Long,
    val songId: Long,
    val songUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val albumArtUri: String? = null,
    val position: Int
)
