package com.eplaytime.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * PlaylistItem entity representing an ordered song entry in a playlist.
 * Preserves metadata and order even if underlying local storage reorganizes.
 */
@Entity(
    tableName = "playlist_items",
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["playlistId", "position"])
    ]
)
data class PlaylistItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val songId: Long,
    val songUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val albumArtUri: String? = null,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

