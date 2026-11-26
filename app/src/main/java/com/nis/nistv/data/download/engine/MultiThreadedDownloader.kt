package com.nis.nistv.data.download.engine

import android.content.Context
import android.media.MediaScannerConnection
import android.os.SystemClock
import android.util.Log
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.preferences.DailyLimitCheck
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Modern high-performance multi-threaded chunk downloader powered by Kotlin Coroutines,
 * OkHttp, and Java NIO FileChannel.
 *
 * Replaces legacy OkDownload with concurrent positional writes (pwrite), eliminating FUSE
 * filesystem lock contention, signed-URL breakpoint wiping, and the 99% completion hang.
 */
class MultiThreadedDownloader(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "MultiThreadedDownloader"
        private const val BUFFER_SIZE = 64 * 1024 // 64 KB streaming buffer

        @Deprecated("OkDownload is replaced with modern Coroutines + NIO FileChannel engine; this call is a no-op.")
        fun ensureOkDownloadInitialized(context: Context, okHttpClient: OkHttpClient) {
            // No-op kept for binary / source compatibility
        }

        private val jsonFormat = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }

        fun saveChunkState(
            stateFile: File,
            downloadId: String,
            totalBytes: Long,
            chunks: List<ChunkState>
        ) {
            try {
                val payload = ChunksPayload(
                    downloadId = downloadId,
                    totalBytes = totalBytes,
                    chunks = chunks
                )
                stateFile.writeText(jsonFormat.encodeToString(payload))
            } catch (e: Exception) {
                Log.w(TAG, "Failed writing chunk state file: ${e.message}")
            }
        }

        fun restoreChunksFromStateFile(
            stateFile: File,
            expectedDownloadId: String,
            expectedTotalBytes: Long
        ): List<ChunkState>? {
            return try {
                if (!stateFile.exists() || stateFile.length() == 0L) return null
                val payload = jsonFormat.decodeFromString<ChunksPayload>(stateFile.readText())
                if (payload.downloadId != expectedDownloadId && expectedDownloadId.isNotBlank()) {
                    // Check if totalBytes match before rejecting
                    if (expectedTotalBytes > 0 && payload.totalBytes != expectedTotalBytes) return null
                }
                if (payload.totalBytes != expectedTotalBytes && expectedTotalBytes > 0) return null
                payload.chunks
            } catch (e: Exception) {
                Log.w(TAG, "Failed reading chunk state file: ${e.message}")
                null
            }
        }
    }

    @Serializable
    data class ThreadProgress(
        val index: Int,
        val startByte: Long,
        val endByte: Long,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val isCompleted: Boolean
    ) {
        val progressFloat: Float
            get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else if (isCompleted) 1f else 0f

        val progressPercent: Int
            get() = if (isCompleted || (totalBytes > 0 && downloadedBytes >= totalBytes)) 100 else (progressFloat * 100).toInt().coerceIn(0, 99)

        val formattedDownloaded: String
            get() = DownloadEntity.formatBytes(downloadedBytes)

        val formattedTotal: String
            get() = DownloadEntity.formatBytes(totalBytes)
    }

    @Serializable
    data class ChunkState(
        val index: Int,
        val startByte: Long,
        val endByte: Long,
        var downloadedBytes: Long = 0L,
        var isCompleted: Boolean = false
    ) {
        val totalBytes: Long
            get() = if (endByte >= startByte) endByte - startByte + 1L else 0L

        fun toThreadProgress(): ThreadProgress {
            val completed = isCompleted || (totalBytes > 0 && downloadedBytes >= totalBytes)
            return ThreadProgress(
                index = index,
                startByte = startByte,
                endByte = endByte,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                isCompleted = completed
            )
        }
    }

    @Serializable
    data class ChunksPayload(
        val downloadId: String,
        val totalBytes: Long,
        val chunks: List<ChunkState>
    )

    data class ProbeResult(
        val isRangeSupported: Boolean,
        val totalLength: Long,
        val resolvedUrl: String
    )

    /**
     * Probes the server to inspect Range support and extract exact content length.
     */
    suspend fun probeUrl(url: String): ProbeResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) NisTV/2.0")
            .addHeader("Range", "bytes=0-0")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val resolved = response.request.url.toString()
                if (response.code == 206) {
                    val contentRange = response.header("Content-Range")
                    val total = parseTotalLengthFromContentRange(contentRange)
                    if (total > 0) {
                        return@withContext ProbeResult(
                            isRangeSupported = true,
                            totalLength = total,
                            resolvedUrl = resolved
                        )
                    }
                }

                // Check Accept-Ranges header
                val acceptRanges = response.header("Accept-Ranges")
                val contentLength = response.body?.contentLength() ?: -1L
                val isRange = acceptRanges?.equals("bytes", ignoreCase = true) == true && contentLength > 0
                ProbeResult(
                    isRangeSupported = isRange,
                    totalLength = contentLength,
                    resolvedUrl = resolved
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed probing URL Range support: ${e.message}")
            ProbeResult(
                isRangeSupported = false,
                totalLength = -1L,
                resolvedUrl = url
            )
        }
    }

    private fun parseTotalLengthFromContentRange(contentRange: String?): Long {
        if (contentRange.isNullOrBlank()) return -1L
        val slashIndex = contentRange.lastIndexOf('/')
        if (slashIndex != -1 && slashIndex + 1 < contentRange.length) {
            val totalStr = contentRange.substring(slashIndex + 1).trim()
            return totalStr.toLongOrNull() ?: -1L
        }
        return -1L
    }

    /**
     * Partitions totalBytes into contiguous non-overlapping chunk ranges.
     */
    fun partitionChunks(totalBytes: Long, threadCount: Int): List<ChunkState> {
        val effectiveThreads = threadCount.coerceIn(1, 8)
        if (totalBytes <= 0 || effectiveThreads <= 1) {
            return listOf(
                ChunkState(
                    index = 0,
                    startByte = 0L,
                    endByte = if (totalBytes > 0) totalBytes - 1L else -1L,
                    downloadedBytes = 0L,
                    isCompleted = false
                )
            )
        }

        val chunkSize = totalBytes / effectiveThreads
        val chunks = mutableListOf<ChunkState>()
        for (i in 0 until effectiveThreads) {
            val start = i * chunkSize
            val end = if (i == effectiveThreads - 1) totalBytes - 1L else (i + 1) * chunkSize - 1L
            chunks.add(
                ChunkState(
                    index = i,
                    startByte = start,
                    endByte = end,
                    downloadedBytes = 0L,
                    isCompleted = false
                )
            )
        }
        return chunks
    }

    /**
     * Executes the download using Kotlin Coroutines + OkHttp + NIO FileChannel positional writes.
     * Divides large files across multiple parallel chunk workers writing directly to disk offsets
     * without FUSE seek contention.
     */
    suspend fun download(
        download: DownloadEntity,
        threadCount: Int,
        onProgress: suspend (currentBytes: Long, totalBytes: Long, speedBytesPerSec: Long, threadProgress: List<ThreadProgress>) -> Unit,
        onRecordDownloaded: suspend (bytes: Long) -> Unit,
        checkDailyLimit: suspend () -> DailyLimitCheck,
        onRenewUrl: suspend () -> String?
    ): Boolean = withContext(Dispatchers.IO) {
        val targetFile = File(download.filePath)
        val parentDir = targetFile.parentFile ?: context.cacheDir
        parentDir.mkdirs()

        val stateFile = File("${download.filePath}.parts.json")
        var currentUrl = download.downloadUrl
        val effectiveThreads = threadCount.coerceIn(1, 8)

        // Probe URL if total size unknown or to confirm Range support
        var totalLength = download.fileSizeBytes
        var isRangeSupported = true

        if (totalLength <= 0L) {
            val probe = probeUrl(currentUrl)
            if (probe.totalLength > 0L) {
                totalLength = probe.totalLength
            }
            isRangeSupported = probe.isRangeSupported
            currentUrl = probe.resolvedUrl
        }

        // Initialize or restore chunk states
        var chunks: List<ChunkState>? = null
        if (stateFile.exists()) {
            chunks = restoreChunksFromStateFile(stateFile, download.id, totalLength)
        }

        if (chunks == null || chunks.isEmpty() || !targetFile.exists()) {
            chunks = if (isRangeSupported && totalLength > 0L) {
                partitionChunks(totalLength, effectiveThreads)
            } else {
                listOf(
                    ChunkState(
                        index = 0,
                        startByte = 0L,
                        endByte = if (totalLength > 0L) totalLength - 1L else -1L,
                        downloadedBytes = 0L,
                        isCompleted = false
                    )
                )
            }
            saveChunkState(stateFile, download.id, totalLength, chunks)
        }

        // Open target file with RandomAccessFile and obtain FileChannel
        val raf = try {
            RandomAccessFile(targetFile, "rw")
        } catch (e: Exception) {
            Log.e(TAG, "Failed opening target file for writing: ${e.message}")
            return@withContext false
        }

        // Pre-allocate file size if known to reserve contiguous disk blocks
        if (totalLength > 0L && raf.length() < totalLength) {
            try {
                raf.setLength(totalLength)
            } catch (e: Exception) {
                Log.d(TAG, "File pre-allocation note: ${e.message}")
            }
        }

        val channel: FileChannel = raf.channel
        val channelLock = Any()

        val initialDownloaded = chunks.sumOf { it.downloadedBytes }
        val totalDownloadedAtomic = AtomicLong(initialDownloaded)
        val isAborted = AtomicBoolean(false)
        val isAbortedByLimit = AtomicBoolean(false)

        val activeUrlRef = java.util.concurrent.atomic.AtomicReference(currentUrl)

        // Speed calculation tracking
        var lastSpeedCalcTime = SystemClock.elapsedRealtime()
        var lastSpeedCalcBytes = initialDownloaded
        var currentSpeed = 0L

        // DataStore batching tracking
        val byteBatcher = DownloadByteBatcher(initialDownloaded)
        var lastDataStoreFlushTime = SystemClock.elapsedRealtime()
        var lastLimitCheckTime = 0L
        var lastStateFileSaveTime = SystemClock.elapsedRealtime()

        // Helper to produce current thread progress snapshots
        fun currentThreadProgress(): List<ThreadProgress> {
            return chunks!!.map { it.toThreadProgress() }
        }

        // Helper to flush accumulated bytes to DataStore safely
        suspend fun flushAccumulatedDataStore(bytes: Long) {
            if (bytes > 0) {
                try {
                    onRecordDownloaded(bytes)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed recording downloaded bytes: ${e.message}")
                }
            }
        }

        // Launch progress ticker & watchdog coroutine
        val tickerJob = launch(Dispatchers.IO) {
            while (isActive && !isAborted.get()) {
                delay(500L)
                val now = SystemClock.elapsedRealtime()

                // Calculate speed
                val speedElapsed = now - lastSpeedCalcTime
                val curDownloaded = totalDownloadedAtomic.get()
                if (speedElapsed >= 500L) {
                    val bytesDiff = curDownloaded - lastSpeedCalcBytes
                    currentSpeed = if (bytesDiff > 0L) (bytesDiff * 1000L / speedElapsed) else 0L
                    lastSpeedCalcBytes = curDownloaded
                    lastSpeedCalcTime = now
                }

                // Batch DataStore disk writes (flush every 10s or when 5MB accumulated)
                val toFlush = byteBatcher.takePendingIfReady(
                    currentBytes = curDownloaded,
                    elapsedSinceLastFlushMs = now - lastDataStoreFlushTime
                )
                if (toFlush > 0L) {
                    lastDataStoreFlushTime = now
                    flushAccumulatedDataStore(toFlush)
                }

                // Periodic state file save (every 5s)
                if (now - lastStateFileSaveTime >= 5_000L) {
                    lastStateFileSaveTime = now
                    saveChunkState(stateFile, download.id, totalLength, chunks!!)
                }

                // Check daily download limit every 10s
                if (now - lastLimitCheckTime >= 10_000L) {
                    lastLimitCheckTime = now
                    try {
                        val limit = checkDailyLimit()
                        if (limit.isExceeded) {
                            isAbortedByLimit.set(true)
                            isAborted.set(true)
                            break
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed checking daily limit: ${e.message}")
                    }
                }

                // Emit progress
                val tpList = currentThreadProgress()
                val effectiveTotal = if (totalLength > 0L) totalLength else download.fileSizeBytes
                onProgress(curDownloaded, effectiveTotal, currentSpeed, tpList)
            }
        }

        var downloadSuccess = false

        try {
            // Run chunk downloads in parallel coroutines
            coroutineScope {
                val chunkJobs = chunks!!.map { chunk ->
                    launch(Dispatchers.IO) {
                        var chunkSuccess = chunk.isCompleted || (chunk.totalBytes > 0 && chunk.downloadedBytes >= chunk.totalBytes)
                        var retryCount = 0
                        val maxChunkRetries = 3

                        while (isActive && !chunkSuccess && !isAborted.get() && retryCount <= maxChunkRetries) {
                            val startPos = chunk.startByte + chunk.downloadedBytes
                            val endPos = chunk.endByte

                            if (endPos in 0 until startPos) {
                                chunk.isCompleted = true
                                chunkSuccess = true
                                break
                            }

                            val requestBuilder = Request.Builder()
                                .url(activeUrlRef.get())
                                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) NisTV/2.0")

                            if (isRangeSupported && (startPos > 0L || endPos > 0L)) {
                                if (endPos > 0L) {
                                    requestBuilder.addHeader("Range", "bytes=$startPos-$endPos")
                                } else {
                                    requestBuilder.addHeader("Range", "bytes=$startPos-")
                                }
                            }

                            try {
                                okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                                    if (response.code in 401..403) {
                                        Log.w(TAG, "Chunk ${chunk.index} HTTP ${response.code}, requesting URL renewal...")
                                        val renewed = onRenewUrl()
                                        if (!renewed.isNullOrBlank() && renewed != activeUrlRef.get()) {
                                            activeUrlRef.set(renewed)
                                            retryCount = 0
                                            return@use
                                        }
                                    }

                                    if (response.code == 416) {
                                        // Range Not Satisfiable: chunk is already at EOF or completed
                                        Log.i(TAG, "Chunk ${chunk.index} received 416 (completed)")
                                        chunk.isCompleted = true
                                        chunkSuccess = true
                                        return@use
                                    }

                                    if (!response.isSuccessful) {
                                        Log.w(TAG, "Chunk ${chunk.index} received HTTP ${response.code}")
                                        retryCount++
                                        delay(1000L * retryCount)
                                        return@use
                                    }

                                    val body = response.body ?: throw IllegalStateException("Empty response body")
                                    val stream = body.byteStream()
                                    val buffer = ByteArray(BUFFER_SIZE)

                                    while (isActive && !isAborted.get()) {
                                        val bytesRead = stream.read(buffer)
                                        if (bytesRead == -1) {
                                            break // EOF reached
                                        }

                                        val writeOffset = chunk.startByte + chunk.downloadedBytes
                                        val byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead)

                                        // Positional write to FileChannel with thread-safe lock
                                        synchronized(channelLock) {
                                            var written = 0
                                            while (written < bytesRead) {
                                                val n = channel.write(byteBuffer, writeOffset + written)
                                                if (n <= 0) break
                                                written += n
                                            }
                                        }

                                        chunk.downloadedBytes += bytesRead
                                        totalDownloadedAtomic.addAndGet(bytesRead.toLong())

                                        if (chunk.totalBytes > 0 && chunk.downloadedBytes >= chunk.totalBytes) {
                                            break
                                        }
                                    }

                                    if (chunk.totalBytes <= 0 || chunk.downloadedBytes >= chunk.totalBytes) {
                                        chunk.isCompleted = true
                                        chunkSuccess = true
                                        saveChunkState(stateFile, download.id, totalLength, chunks!!)
                                    }
                                }
                            } catch (e: Exception) {
                                if (!isActive || isAborted.get()) return@launch
                                Log.w(TAG, "Chunk ${chunk.index} encountered error: ${e.message}")
                                retryCount++
                                delay(1000L * retryCount)
                            }
                        }
                    }
                }

                chunkJobs.joinAll()
            }

            tickerJob.cancelAndJoin()

            // Check if all chunks completed successfully
            val allChunksDone = chunks!!.all { it.isCompleted || (it.totalBytes > 0 && it.downloadedBytes >= it.totalBytes) }

            if (allChunksDone && !isAbortedByLimit.get()) {
                // Force all pending buffers to underlying storage
                try {
                    channel.force(true)
                } catch (e: Exception) {
                    Log.w(TAG, "FileChannel force note: ${e.message}")
                }

                // If file size wasn't known beforehand, read actual file length
                val finalFileLength = targetFile.length()
                val verifiedTotal = if (totalLength > 0L) totalLength else finalFileLength

                // Delete .parts.json state file upon completion
                if (stateFile.exists()) {
                    stateFile.delete()
                }

                // Scan file with MediaScanner for instant media library visibility
                try {
                    MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), null, null)
                } catch (_: Exception) {}

                // Emit final 100% progress
                val completedThreadProgress = chunks!!.map {
                    it.toThreadProgress().copy(
                        downloadedBytes = it.totalBytes,
                        isCompleted = true
                    )
                }
                onProgress(verifiedTotal, verifiedTotal, 0L, completedThreadProgress)

                downloadSuccess = true
            } else {
                // Save current state on pause/cancel/limit
                saveChunkState(stateFile, download.id, totalLength, chunks!!)
            }

            // Flush remaining DataStore bytes
            val remainingBytes = byteBatcher.takeRemaining(totalDownloadedAtomic.get())
            if (remainingBytes > 0L) {
                flushAccumulatedDataStore(remainingBytes)
            }

        } finally {
            tickerJob.cancel()
            try {
                channel.close()
            } catch (_: Exception) {}
            try {
                raf.close()
            } catch (_: Exception) {}
        }

        downloadSuccess
    }
}

/**
 * Tracks DataStore byte-usage deltas so each downloaded byte is recorded at most once
 * across periodic flushes and final completion.
 */
internal class DownloadByteBatcher(initialBytes: Long) {
    private var lastFlushedBytes: Long = initialBytes

    @Synchronized
    fun takePendingIfReady(
        currentBytes: Long,
        elapsedSinceLastFlushMs: Long,
        minBatchBytes: Long = 5 * 1024 * 1024L,
        maxBatchIntervalMs: Long = 10_000L
    ): Long {
        val pending = currentBytes - lastFlushedBytes
        if (pending <= 0L) return 0L
        if (pending >= minBatchBytes || elapsedSinceLastFlushMs >= maxBatchIntervalMs) {
            lastFlushedBytes = currentBytes
            return pending
        }
        return 0L
    }

    @Synchronized
    fun takeRemaining(currentBytes: Long): Long {
        val remaining = currentBytes - lastFlushedBytes
        if (remaining <= 0L) return 0L
        lastFlushedBytes = currentBytes
        return remaining
    }
}
