package com.nis.nistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nis.nistv.core.database.entity.PlaybackProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackProgressDao {

    @Query("SELECT * FROM playback_progress WHERE profileId = :profileId AND downloadId = :downloadId LIMIT 1")
    fun getProgress(profileId: String, downloadId: String): Flow<PlaybackProgressEntity?>

    @Query("SELECT * FROM playback_progress WHERE profileId = :profileId AND downloadId = :downloadId LIMIT 1")
    suspend fun getProgressSync(profileId: String, downloadId: String): PlaybackProgressEntity?

    @Query("SELECT * FROM playback_progress WHERE profileId = :profileId ORDER BY lastWatchedAt DESC")
    fun getAllProgressForProfile(profileId: String): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress WHERE profileId = :profileId ORDER BY lastWatchedAt DESC")
    suspend fun getAllProgressForProfileSync(profileId: String): List<PlaybackProgressEntity>

    @Query("SELECT * FROM playback_progress")
    suspend fun getAllProgressSync(): List<PlaybackProgressEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM playback_progress WHERE downloadId = :downloadId AND completed = 1 LIMIT 1)")
    suspend fun isCompletedByAnyProfile(downloadId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: PlaybackProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PlaybackProgressEntity>)

    @Query("UPDATE playback_progress SET hidden = :hidden WHERE profileId = :profileId AND downloadId = :downloadId")
    suspend fun setHidden(profileId: String, downloadId: String, hidden: Boolean)

    @Query("DELETE FROM playback_progress WHERE profileId = :profileId AND downloadId = :downloadId")
    suspend fun deleteProgress(profileId: String, downloadId: String)

    @Query("DELETE FROM playback_progress WHERE profileId = :profileId")
    suspend fun clearProgressForProfile(profileId: String)

    @Query("DELETE FROM playback_progress")
    suspend fun clearAll()
}
