package com.nis.nistv

import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.DownloadStatus
import com.nis.nistv.data.download.engine.DownloadByteBatcher
import com.nis.nistv.data.download.engine.MultiThreadedDownloader
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class MultiThreadedDownloaderTest {

    @Test
    fun chunkState_totalBytesCalculation() {
        val chunk = MultiThreadedDownloader.ChunkState(
            index = 0,
            startByte = 0L,
            endByte = 1023L,
            downloadedBytes = 512L,
            isCompleted = false
        )
        assertEquals(1024L, chunk.totalBytes)
        assertFalse(chunk.isCompleted)

        chunk.downloadedBytes = 1024L
        chunk.isCompleted = true
        assertTrue(chunk.isCompleted)
    }

    @Test
    fun chunkCalculation_partitionsContiguousRanges() {
        val totalBytes = 100_000_000L // 100 MB
        val threadCount = 4
        val chunkSize = totalBytes / threadCount

        val chunks = mutableListOf<MultiThreadedDownloader.ChunkState>()
        for (i in 0 until threadCount) {
            val start = i * chunkSize
            val end = if (i == threadCount - 1) totalBytes - 1L else (i + 1) * chunkSize - 1L
            chunks.add(
                MultiThreadedDownloader.ChunkState(
                    index = i,
                    startByte = start,
                    endByte = end
                )
            )
        }

        assertEquals(4, chunks.size)
        // Verify contiguous boundaries without gaps or overlaps
        assertEquals(0L, chunks[0].startByte)
        assertEquals(24_999_999L, chunks[0].endByte)

        assertEquals(25_000_000L, chunks[1].startByte)
        assertEquals(49_999_999L, chunks[1].endByte)

        assertEquals(50_000_000L, chunks[2].startByte)
        assertEquals(74_999_999L, chunks[2].endByte)

        assertEquals(75_000_000L, chunks[3].startByte)
        assertEquals(99_999_999L, chunks[3].endByte)

        val sumBytes = chunks.sumOf { it.totalBytes }
        assertEquals(totalBytes, sumBytes)
    }

    @Test
    fun chunkStateSerialization_andDeserialization() {
        val tempFile = File.createTempFile("test_parts", ".json")
        try {
            val totalBytes = 50_000_000L
            val downloadId = "dl_test_123"

            val originalChunks = listOf(
                MultiThreadedDownloader.ChunkState(
                    index = 0,
                    startByte = 0L,
                    endByte = 24_999_999L,
                    downloadedBytes = 24_999_999L,
                    isCompleted = true
                ),
                MultiThreadedDownloader.ChunkState(
                    index = 1,
                    startByte = 25_000_000L,
                    endByte = 49_999_999L,
                    downloadedBytes = 10_000_000L,
                    isCompleted = false
                )
            )

            MultiThreadedDownloader.saveChunkState(tempFile, downloadId, totalBytes, originalChunks)

            val restored = MultiThreadedDownloader.restoreChunksFromStateFile(tempFile, downloadId, totalBytes)
            assertNotNull(restored)
            assertEquals(2, restored!!.size)

            val chunk1 = restored[0]
            assertEquals(0, chunk1.index)
            assertTrue(chunk1.isCompleted)
            assertEquals(24_999_999L, chunk1.downloadedBytes)

            val chunk2 = restored[1]
            assertEquals(1, chunk2.index)
            assertFalse(chunk2.isCompleted)
            assertEquals(10_000_000L, chunk2.downloadedBytes)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun threadProgress_calculationAndFormatting() {
        val chunk = MultiThreadedDownloader.ChunkState(
            index = 2,
            startByte = 20_000_000L,
            endByte = 39_999_999L,
            downloadedBytes = 10_000_000L,
            isCompleted = false
        )
        val threadProgress = chunk.toThreadProgress()
        assertEquals(2, threadProgress.index)
        assertEquals(20_000_000L, threadProgress.totalBytes)
        assertEquals(10_000_000L, threadProgress.downloadedBytes)
        assertEquals(0.5f, threadProgress.progressFloat, 0.001f)
        assertEquals(50, threadProgress.progressPercent)
        assertFalse(threadProgress.isCompleted)

        val completedChunk = MultiThreadedDownloader.ChunkState(
            index = 0,
            startByte = 0L,
            endByte = 9_999_999L,
            downloadedBytes = 10_000_000L,
            isCompleted = true
        )
        val completedProgress = completedChunk.toThreadProgress()
        assertEquals(1.0f, completedProgress.progressFloat, 0.001f)
        assertEquals(100, completedProgress.progressPercent)
        assertTrue(completedProgress.isCompleted)
    }

    @Test
    fun downloadEntity_progressPercent_isAlways100OnCompleted() {
        // Even if downloadedBytes is slightly less due to rounding/formatting, COMPLETED status returns 100%
        val download = DownloadEntity(
            id = "test_completed",
            mediaId = "m1",
            title = "Test Movie",
            downloadUrl = "https://example.com/movie.mp4",
            fileName = "movie.mp4",
            filePath = "/path/to/movie.mp4",
            fileSizeBytes = 1_000_000_000L,
            downloadedBytes = 999_999_900L, // 99.99999%
            status = DownloadStatus.COMPLETED.name
        )
        assertEquals(100, download.progressPercent)
    }

    @Test
    fun downloadEntity_progressPercent_capsAt99WhileDownloading() {
        // When status is DOWNLOADING and not all bytes are reached, it stays at 99% max until completed
        val download = DownloadEntity(
            id = "test_downloading",
            mediaId = "m2",
            title = "Test Movie 2",
            downloadUrl = "https://example.com/movie2.mp4",
            fileName = "movie2.mp4",
            filePath = "/path/to/movie2.mp4",
            fileSizeBytes = 1_000_000_000L,
            downloadedBytes = 999_999_900L, // 99.99999%
            status = DownloadStatus.DOWNLOADING.name
        )
        assertEquals(99, download.progressPercent)
    }

    @Test
    fun downloadByteBatcher_recordsEachByteOnceAcrossMultipleFlushes() {
        val batcher = DownloadByteBatcher(initialBytes = 0L)
        var totalRecorded = 0L

        // Simulate 500 MB download in 5 MB increments (100 ticks)
        val mb = 1024L * 1024L
        for (i in 1..100) {
            val current = i * 5L * mb
            val flushed = batcher.takePendingIfReady(
                currentBytes = current,
                elapsedSinceLastFlushMs = 500L
            )
            totalRecorded += flushed
        }
        totalRecorded += batcher.takeRemaining(500L * mb)

        // Must equal exact 500 MB, never inflated O(N^2)
        assertEquals(500L * mb, totalRecorded)
    }

    @Test
    fun downloadByteBatcher_onResume_recordsOnlyNewBytes() {
        val mb = 1024L * 1024L
        // Resuming a download that already has 300 MB on disk
        val batcher = DownloadByteBatcher(initialBytes = 300L * mb)
        var totalRecorded = 0L

        // Below 5 MB threshold and below 10s interval -> nothing flushed yet
        assertEquals(0L, batcher.takePendingIfReady(302L * mb, elapsedSinceLastFlushMs = 1_000L))

        // Time threshold (10s) triggers flush of only the 2 MB delta
        totalRecorded += batcher.takePendingIfReady(302L * mb, elapsedSinceLastFlushMs = 10_000L)
        assertEquals(2L * mb, totalRecorded)

        // Next 5 MB delta (302 MB -> 307 MB)
        totalRecorded += batcher.takePendingIfReady(307L * mb, elapsedSinceLastFlushMs = 500L)
        assertEquals(7L * mb, totalRecorded)

        // Final remaining 3 MB (307 MB -> 310 MB)
        totalRecorded += batcher.takeRemaining(310L * mb)
        assertEquals(10L * mb, totalRecorded)
    }
}
