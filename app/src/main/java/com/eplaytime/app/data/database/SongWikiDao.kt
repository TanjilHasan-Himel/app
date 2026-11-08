package com.eplaytime.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SongWikiDao {
    @Query("SELECT * FROM song_wiki WHERE songKey = :key LIMIT 1")
    suspend fun getWikiByKey(key: String): SongWikiEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWiki(entity: SongWikiEntity)

    @Query("DELETE FROM song_wiki WHERE cachedAt < :expiryTimestamp")
    suspend fun pruneOldWiki(expiryTimestamp: Long)
}

