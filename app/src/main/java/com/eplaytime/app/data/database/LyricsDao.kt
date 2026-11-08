package com.eplaytime.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics_cache WHERE songUri = :uri LIMIT 1")
    suspend fun getLyrics(uri: String): LyricsEntity?

    @Query("SELECT * FROM lyrics_cache WHERE title = :title AND artist = :artist LIMIT 1")
    suspend fun getLyricsByMetadata(title: String, artist: String): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: LyricsEntity)

    @Query("DELETE FROM lyrics_cache WHERE songUri = :uri")
    suspend fun deleteLyrics(uri: String)
}

