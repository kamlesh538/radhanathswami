package com.radhanathswami.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY lastPlayedAt DESC")
    fun getAll(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HistoryEntity)

    @Query("UPDATE history SET lastPositionMs = :positionMs, durationMs = :durationMs, lastPlayedAt = :playedAt WHERE id = :id")
    suspend fun updateProgress(id: String, positionMs: Long, durationMs: Long, playedAt: Long)

    @Query("SELECT * FROM history WHERE id = :id")
    suspend fun getById(id: String): HistoryEntity?

    @Delete
    suspend fun delete(entity: HistoryEntity)
}
