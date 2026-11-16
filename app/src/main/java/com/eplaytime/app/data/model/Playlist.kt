package com.eplaytime.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Playlist entity representing a user-created or imported playlist.
 */
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val description: String = "",
    val customArtUri: String? = null
)

