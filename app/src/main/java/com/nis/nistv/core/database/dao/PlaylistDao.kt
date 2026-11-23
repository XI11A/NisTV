package com.nis.nistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nis.nistv.core.database.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    suspend fun getAllPlaylistsSync(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE enabled = 1 ORDER BY name ASC")
    fun getEnabledPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getPlaylistById(id: String): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistByIdSync(id: String): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE autoRefresh = 1 AND enabled = 1")
    suspend fun getAutoRefreshPlaylists(): List<PlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun updatePlaylistName(id: String, name: String)

    @Query("UPDATE playlists SET enabled = :enabled WHERE id = :id")
    suspend fun setPlaylistEnabled(id: String, enabled: Boolean)

    @Query("UPDATE playlists SET channelCount = :channelCount, lastSyncedAt = :lastSyncedAt WHERE id = :id")
    suspend fun updatePlaylistSyncStatus(id: String, channelCount: Int, lastSyncedAt: Long)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsForPlaylist(playlistId: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: String)

    @Transaction
    suspend fun deletePlaylistCascade(playlistId: String) {
        deleteChannelsForPlaylist(playlistId)
        deletePlaylistById(playlistId)
    }
}
