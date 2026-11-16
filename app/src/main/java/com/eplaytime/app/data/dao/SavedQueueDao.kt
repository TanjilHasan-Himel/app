package com.eplaytime.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.eplaytime.app.data.model.SavedQueue
import com.eplaytime.app.data.model.SavedQueueItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedQueueDao {
    @Query("SELECT * FROM saved_queues ORDER BY updatedAt DESC")
    fun getAllSavedQueues(): Flow<List<SavedQueue>>

    @Query("SELECT * FROM saved_queues WHERE id = :id LIMIT 1")
    suspend fun getSavedQueueById(id: Long): SavedQueue?

    @Query("SELECT * FROM saved_queue_items WHERE queueId = :queueId ORDER BY position ASC")
    suspend fun getQueueItems(queueId: Long): List<SavedQueueItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueue(queue: SavedQueue): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<SavedQueueItem>)

    @Query("DELETE FROM saved_queues WHERE id = :queueId")
    suspend fun deleteQueue(queueId: Long)

    @Query("DELETE FROM saved_queue_items WHERE queueId = :queueId")
    suspend fun deleteQueueItems(queueId: Long)

    @Transaction
    suspend fun saveFullQueue(queue: SavedQueue, items: List<SavedQueueItem>): Long {
        val queueId = insertQueue(queue)
        deleteQueueItems(queueId)
        val itemsWithId = items.mapIndexed { index, item ->
            item.copy(queueId = queueId, position = index)
        }
        insertQueueItems(itemsWithId)
        return queueId
    }
}
