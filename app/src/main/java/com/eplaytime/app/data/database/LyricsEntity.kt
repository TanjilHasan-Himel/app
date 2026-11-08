package com.eplaytime.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics_cache")
data class LyricsEntity(
    @PrimaryKey
    val songUri: String,
    val title: String,
    val artist: String,
    val syncedLyrics: String?,
    val plainLyrics: String?,
    val source: String, // "LOCAL" or "LRCLIB"
    val timestamp: Long = System.currentTimeMillis()
)

