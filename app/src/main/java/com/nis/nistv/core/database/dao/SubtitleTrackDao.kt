package com.nis.nistv.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nis.nistv.core.database.entity.SubtitleTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtitleTrackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: SubtitleTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<SubtitleTrackEntity>)

    @Query("SELECT * FROM subtitle_tracks WHERE downloadId = :downloadId ORDER BY createdAt ASC")
    fun getTracksForDownload(downloadId: String): Flow<List<SubtitleTrackEntity>>

    @Query("SELECT * FROM subtitle_tracks WHERE downloadId = :downloadId ORDER BY createdAt ASC")
    suspend fun getTracksForDownloadSync(downloadId: String): List<SubtitleTrackEntity>

    @Query("SELECT * FROM subtitle_tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): SubtitleTrackEntity?

    @Query("UPDATE subtitle_tracks SET isSelected = 0 WHERE downloadId = :downloadId")
    suspend fun clearSelection(downloadId: String)

    @Query("UPDATE subtitle_tracks SET isSelected = 1 WHERE id = :trackId")
    suspend fun selectTrack(trackId: String)

    @Transaction
    suspend fun setSelectedTrack(downloadId: String, trackId: String?) {
        clearSelection(downloadId)
        if (trackId != null) {
            selectTrack(trackId)
        }
    }

    @Delete
    suspend fun deleteTrack(track: SubtitleTrackEntity)

    @Query("DELETE FROM subtitle_tracks WHERE id = :id")
    suspend fun deleteTrackById(id: String)

    @Query("DELETE FROM subtitle_tracks WHERE downloadId = :downloadId")
    suspend fun deleteTracksForDownload(downloadId: String)

    @Query("UPDATE subtitle_tracks SET delayMs = :delayMs WHERE id = :trackId")
    suspend fun updateTrackDelay(trackId: String, delayMs: Long)

    @Query("UPDATE subtitle_tracks SET role = :role WHERE id = :trackId")
    suspend fun updateTrackRole(trackId: String, role: String)

    @Query("SELECT * FROM subtitle_tracks WHERE downloadId = :downloadId AND isSelected = 1")
    suspend fun getSelectedTracksForDownload(downloadId: String): List<SubtitleTrackEntity>

    @Transaction
    suspend fun setDualSelectedTracks(
        downloadId: String,
        primaryTrackId: String?,
        secondaryTrackId: String?
    ) {
        clearSelection(downloadId)
        if (primaryTrackId != null) {
            selectTrack(primaryTrackId)
            updateTrackRole(primaryTrackId, "PRIMARY")
        }
        if (secondaryTrackId != null && secondaryTrackId != primaryTrackId) {
            selectTrack(secondaryTrackId)
            updateTrackRole(secondaryTrackId, "SECONDARY")
        }
    }
}
