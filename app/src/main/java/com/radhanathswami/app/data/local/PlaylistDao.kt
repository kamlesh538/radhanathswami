package com.radhanathswami.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(entity: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(entity: PlaylistEntity)

    @Query("SELECT * FROM playlist_entries WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getEntriesForPlaylist(playlistId: String): Flow<List<PlaylistEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntry(entry: PlaylistEntryEntity)

    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId AND audioId = :audioId")
    suspend fun removeEntry(playlistId: String, audioId: String)

    @Query("SELECT MAX(position) FROM playlist_entries WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: String): Int?
}
