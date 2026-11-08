package com.eplaytime.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_wiki")
data class SongWikiEntity(
    @PrimaryKey
    val songKey: String, // "title_artist" normalized
    val title: String,
    val artist: String,
    val summary: String,
    val description: String?,
    val thumbnailUrl: String?,
    val composer: String?,
    val lyricist: String?,
    val producer: String?,
    val releaseDate: String?,
    val sourceUrl: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

