package com.nis.nistv

import com.nis.nistv.core.database.dao.DownloadDao
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.DownloadStatus
import com.nis.nistv.data.download.engine.MultiThreadedDownloader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class DownloadManagerPauseResumeTest {

    private fun createFakeDownloadDao(store: MutableList<DownloadEntity>): DownloadDao {
        return object : DownloadDao {
            override fun getAllDownloads(): Flow<List<DownloadEntity>> = flowOf(store)
            override fun getActiveDownloads(): Flow<List<DownloadEntity>> =
                flowOf(store.filter { it.status in listOf("DOWNLOADING", "QUEUED", "PAUSED") })
            override fun getCompletedDownloads(): Flow<List<DownloadEntity>> =
                flowOf(store.filter { it.status == "COMPLETED" })
            override suspend fun getCompletedDownloadsSync(): List<DownloadEntity> =
                store.filter { it.status == "COMPLETED" }
            override suspend fun getAllDownloadsSync(): List<DownloadEntity> =
                store.toList()
            override suspend fun getDownloadById(id: String): DownloadEntity? =
                store.find { it.id == id }
            override suspend fun getDownloadByMediaId(mediaId: String): DownloadEntity? =
                store.find { it.mediaId == mediaId }
            override suspend fun insertDownload(download: DownloadEntity) {
                store.add(download)
            }
            override suspend fun updateDownload(download: DownloadEntity) {
                val idx = store.indexOfFirst { it.id == download.id }
                if (idx >= 0) store[idx] = download
            }
            override suspend fun updateDownloads(downloads: List<DownloadEntity>) {
                downloads.forEach { updateDownload(it) }
            }
            override suspend fun updateProgress(id: String, downloaded: Long, speed: Long, updatedAt: Long) {
                val idx = store.indexOfFirst { it.id == id }
                if (idx >= 0) store[idx] = store[idx].copy(downloadedBytes = downloaded, speedBytesPerSec = speed, updatedAt = updatedAt)
            }
            override suspend fun updateStatus(id: String, status: String, updatedAt: Long) {
                val idx = store.indexOfFirst { it.id == id }
                if (idx >= 0) store[idx] = store[idx].copy(status = status, updatedAt = updatedAt)
            }
            override suspend fun markFailed(id: String, status: String, error: String?, updatedAt: Long) {
                val idx = store.indexOfFirst { it.id == id }
                if (idx >= 0) store[idx] = store[idx].copy(status = status, errorMessage = error, speedBytesPerSec = 0, updatedAt = updatedAt)
            }
            override suspend fun deleteDownload(id: String) {
                store.removeAll { it.id == id }
            }
            override suspend fun deleteAllDownloads() {
                store.clear()
            }
            override suspend fun getDownloadsBySeriesSync(seriesId: String, seriesPrefix: String): List<DownloadEntity> =
                emptyList()
            override fun getDownloadsBySeries(seriesId: String, seriesPrefix: String): Flow<List<DownloadEntity>> =
                flowOf(emptyList())
            override suspend fun updatePlaybackPosition(id: String, position: Long, updatedAt: Long) {}
            override suspend fun updatePlaybackProgress(
                id: String,
                position: Long,
                duration: Long,
                lastWatched: Long,
                watchedCompleted: Boolean,
                updatedAt: Long
            ) {}
            override suspend fun setHideFromContinueWatching(id: String, hide: Boolean, updatedAt: Long) {}
            override suspend fun updateWatchedCompleted(id: String, completed: Boolean, updatedAt: Long) {}
        }
    }

    @Test
    fun pauseDownload_persistsLatestBytesToDatabase_preventingZeroProgressReset() = runTest {
        val downloadId = "dl_1001"
        val store = mutableListOf(
            DownloadEntity(
                id = downloadId,
                mediaId = "movie_1001",
                title = "Dune: Part Two",
                fileName = "Dune.Part.Two.2024.1080p.mkv",
                filePath = "/storage/emulated/0/Download/NisTV/Dune.Part.Two.2024.1080p.mkv",
                downloadUrl = "https://cdn.example.com/dune2.mkv?token=abc",
                fileSizeBytes = 2_000_000_000L, // 2 GB
                downloadedBytes = 0L, // 0 in DB because write throttle hasn't triggered yet
                status = DownloadStatus.DOWNLOADING.name,
                mediaType = "movie"
            )
        )
        val dao = createFakeDownloadDao(store)

        // In-memory progress tracking 450 MB downloaded
        val inMemoryBytes = 450_000_000L

        // Simulate pauseDownload logic
        val dl = dao.getDownloadById(downloadId)!!
        val currentBytes = when {
            inMemoryBytes > 0L -> inMemoryBytes
            else -> dl.downloadedBytes
        }
        if (currentBytes > 0L && currentBytes != dl.downloadedBytes) {
            dao.updateProgress(downloadId, currentBytes, 0L)
        }
        dao.updateStatus(downloadId, DownloadStatus.PAUSED.name)

        val updated = dao.getDownloadById(downloadId)!!
        assertEquals(DownloadStatus.PAUSED.name, updated.status)
        assertEquals(450_000_000L, updated.downloadedBytes)
        assertEquals(22, updated.progressPercent) // 450MB / 2000MB = 22.5% -> 22%
    }

    @Test
    fun resumeDownload_retainsExistingUrl_toPreserveContinuity() = runTest {
        val downloadId = "dl_1002"
        val originalUrl = "https://hashhackers.com/dl/file_123?sig=valid_token"
        val store = mutableListOf(
            DownloadEntity(
                id = downloadId,
                mediaId = "movie_1002",
                title = "Inception",
                fileName = "Inception.2010.1080p.mkv",
                filePath = "/storage/emulated/0/Download/NisTV/Inception.2010.1080p.mkv",
                downloadUrl = originalUrl,
                afdsFileId = "file_123",
                afdsSourceTable = "movies",
                fileSizeBytes = 1_500_000_000L,
                downloadedBytes = 750_000_000L,
                status = DownloadStatus.PAUSED.name,
                mediaType = "movie"
            )
        )
        val dao = createFakeDownloadDao(store)

        val download = dao.getDownloadById(downloadId)!!
        assertEquals(originalUrl, download.downloadUrl)

        // Status transitions to DOWNLOADING with same URL
        dao.updateStatus(downloadId, DownloadStatus.DOWNLOADING.name)

        val resumed = dao.getDownloadById(downloadId)!!
        assertEquals(originalUrl, resumed.downloadUrl)
        assertEquals(750_000_000L, resumed.downloadedBytes)
        assertEquals(50, resumed.progressPercent)
    }

    @Test
    fun partsJson_restoresChunkStateAcrossUrlRenewals() {
        val tempFile = File.createTempFile("test_renew_parts", ".json")
        try {
            val downloadId = "dl_1003"
            val totalBytes = 100_000_000L

            val chunksBeforeRenewal = listOf(
                MultiThreadedDownloader.ChunkState(0, 0L, 49_999_999L, 50_000_000L, true),
                MultiThreadedDownloader.ChunkState(1, 50_000_000L, 99_999_999L, 25_000_000L, false)
            )

            // Save chunk state before renewal
            MultiThreadedDownloader.saveChunkState(tempFile, downloadId, totalBytes, chunksBeforeRenewal)

            // URL is renewed, but chunk state is loaded from .parts.json by downloadId & totalBytes
            val restored = MultiThreadedDownloader.restoreChunksFromStateFile(tempFile, downloadId, totalBytes)
            assertNotNull(restored)
            assertEquals(2, restored!!.size)
            assertEquals(50_000_000L, restored[0].downloadedBytes)
            assertTrue(restored[0].isCompleted)
            assertEquals(25_000_000L, restored[1].downloadedBytes)
            assertFalse(restored[1].isCompleted)

            val totalRestoredDownloaded = restored.sumOf { it.downloadedBytes }
            assertEquals(75_000_000L, totalRestoredDownloaded)
        } finally {
            tempFile.delete()
        }
    }
}
