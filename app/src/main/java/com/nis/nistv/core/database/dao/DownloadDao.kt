package com.nis.nistv.core.database.dao

import androidx.room.*
import com.nis.nistv.core.database.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE UPPER(status) IN ('QUEUED', 'DOWNLOADING', 'PAUSED', 'FAILED') AND NOT (fileSizeBytes > 0 AND downloadedBytes >= fileSizeBytes) ORDER BY createdAt DESC")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE UPPER(status) = 'COMPLETED' OR (fileSizeBytes > 0 AND downloadedBytes >= fileSizeBytes) ORDER BY createdAt DESC")
    fun getCompletedDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE UPPER(status) = 'COMPLETED' OR (fileSizeBytes > 0 AND downloadedBytes >= fileSizeBytes) ORDER BY createdAt DESC")
    suspend fun getCompletedDownloadsSync(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getDownloadByMediaId(mediaId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Query("UPDATE downloads SET downloadedBytes = :downloaded, speedBytesPerSec = :speed, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProgress(id: String, downloaded: Long, speed: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE downloads SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE downloads SET status = :status, errorMessage = :error, speedBytesPerSec = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markFailed(id: String, status: String, error: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: String)

    @Query("DELETE FROM downloads")
    suspend fun deleteAllDownloads()

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    suspend fun getAllDownloadsSync(): List<DownloadEntity>

    @Update
    suspend fun updateDownloads(downloads: List<DownloadEntity>)

    @Query("SELECT * FROM downloads WHERE seriesId = :seriesId OR seriesId = REPLACE(:seriesId, 'tv_', '') OR seriesName = :seriesId OR (mediaType = 'tv' AND (mediaId LIKE :seriesPrefix OR mediaId LIKE REPLACE(:seriesId, 'tv_', '') || '_%')) ORDER BY seasonNumber ASC, episodeNumber ASC")
    suspend fun getDownloadsBySeriesSync(
        seriesId: String,
        seriesPrefix: String = "${seriesId}_%"
    ): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE seriesId = :seriesId OR seriesId = REPLACE(:seriesId, 'tv_', '') OR seriesName = :seriesId OR (mediaType = 'tv' AND (mediaId LIKE :seriesPrefix OR mediaId LIKE REPLACE(:seriesId, 'tv_', '') || '_%')) ORDER BY seasonNumber ASC, episodeNumber ASC")
    fun getDownloadsBySeries(
        seriesId: String,
        seriesPrefix: String = "${seriesId}_%"
    ): Flow<List<DownloadEntity>>

    @Query("UPDATE downloads SET lastPlaybackPositionMs = :position, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePlaybackPosition(id: String, position: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE downloads SET lastPlaybackPositionMs = :position, durationMs = :duration, lastWatchedAt = :lastWatched, watchedCompleted = :watchedCompleted, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePlaybackProgress(
        id: String,
        position: Long,
        duration: Long,
        lastWatched: Long = System.currentTimeMillis(),
        watchedCompleted: Boolean = false,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE downloads SET hideFromContinueWatching = :hide, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setHideFromContinueWatching(
        id: String,
        hide: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE downloads SET watchedCompleted = :completed, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateWatchedCompleted(
        id: String,
        completed: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )
}
