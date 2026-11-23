package com.nis.nistv.data.download.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nis.nistv.MainActivity
import com.nis.nistv.R
import com.nis.nistv.core.database.dao.DownloadDao
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.DownloadStatus
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.data.download.provider.AfdsDownloadProvider
import com.nis.nistv.data.download.offline.OfflineMediaRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class ActiveDownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long
)

data class StorageStats(
    val nistvUsedBytes: Long,
    val availableBytes: Long,
    val totalBytes: Long
) {
    val formattedNistvUsed: String
        get() = DownloadEntity.formatBytes(nistvUsedBytes)

    val formattedAvailable: String
        get() = DownloadEntity.formatBytes(availableBytes)

    val formattedTotal: String
        get() = DownloadEntity.formatBytes(totalBytes)

    val usedFraction: Float
        get() = if (totalBytes > 0) ((totalBytes - availableBytes).toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

class DownloadManager(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val userPreferences: UserPreferences,
    private val okHttpClient: OkHttpClient,
    private val afdsDownloadProvider: AfdsDownloadProvider? = null,
    private val offlineMediaRepository: OfflineMediaRepository? = null
) {
    companion object {
        private const val TAG = "DownloadManager"
        const val CHANNEL_ID = "nistv_downloads_channel"
        const val NOTIFICATION_ID = 1001
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val downloadOkHttpClient: OkHttpClient = okHttpClient.newBuilder()
        .dispatcher(Dispatcher().apply {
            maxRequests = 32
            maxRequestsPerHost = 16
        })
        .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val multiThreadedDownloader = MultiThreadedDownloader(context, downloadOkHttpClient)

    private val _activeThreadProgress = MutableStateFlow<Map<String, List<MultiThreadedDownloader.ThreadProgress>>>(emptyMap())
    val activeThreadProgress: StateFlow<Map<String, List<MultiThreadedDownloader.ThreadProgress>>> = _activeThreadProgress.asStateFlow()

    private val _activeProgressMap = MutableStateFlow<Map<String, ActiveDownloadProgress>>(emptyMap())
    val activeProgressMap: StateFlow<Map<String, ActiveDownloadProgress>> = _activeProgressMap.asStateFlow()

    @Volatile
    private var cachedDownloadDir: File? = null

    @Volatile
    private var lastStorageStats: StorageStats? = null

    @Volatile
    private var lastStorageStatsScanTime = 0L

    fun invalidateStorageStats() {
        lastStorageStatsScanTime = 0L
    }

    init {
        createNotificationChannel()
        // Resume active/queued downloads after app restart
        scope.launch {
            resumeIncompleteDownloadsOnStartup()
        }
    }

    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()
    val activeDownloads: Flow<List<DownloadEntity>> = downloadDao.getActiveDownloads()
    val completedDownloads: Flow<List<DownloadEntity>> = downloadDao.getCompletedDownloads()

    fun getThreadProgressFor(download: DownloadEntity): List<MultiThreadedDownloader.ThreadProgress>? {
        val stateFile = File("${download.filePath}.parts.json")
        if (stateFile.exists()) {
            val chunks = MultiThreadedDownloader.restoreChunksFromStateFile(
                stateFile = stateFile,
                expectedDownloadId = download.id,
                expectedTotalBytes = download.fileSizeBytes
            )
            if (chunks != null) {
                return chunks.map { it.toThreadProgress() }
            }
        }

        val active = _activeThreadProgress.value[download.id]
        if (active != null) return active

        if (download.status == DownloadStatus.COMPLETED.name || (download.fileSizeBytes > 0 && download.downloadedBytes >= download.fileSizeBytes)) {
            return listOf(
                MultiThreadedDownloader.ThreadProgress(
                    index = 0,
                    startByte = 0L,
                    endByte = if (download.fileSizeBytes > 0) download.fileSizeBytes - 1L else 0L,
                    downloadedBytes = download.fileSizeBytes,
                    totalBytes = download.fileSizeBytes,
                    isCompleted = true
                )
            )
        }
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress and status of active NisTV media downloads"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private suspend fun resumeIncompleteDownloadsOnStartup() {
        try {
            // Migrate legacy download directory files if any exist
            val legacyDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "NisTV_Downloads")
            if (legacyDir.exists() && legacyDir.isDirectory) {
                val targetDir = getDownloadDirectory()
                legacyDir.listFiles()?.forEach { f ->
                    if (f.isFile) {
                        val destination = File(targetDir, f.name)
                        f.renameTo(destination)
                    }
                }
                legacyDir.delete()
            }

            val activeList = downloadDao.getAllDownloads().firstOrNull().orEmpty()
            for (download in activeList) {
                // If path pointed to old NisTV_Downloads, update to current directory if file relocated
                if (download.filePath.contains("NisTV_Downloads")) {
                    val relocatedFile = File(getDownloadDirectory(), download.fileName)
                    if (relocatedFile.exists()) {
                        downloadDao.updateDownload(download.copy(filePath = relocatedFile.absolutePath))
                    }
                }
                // Pre-populate active thread progress from parts file if exists
                getThreadProgressFor(download)?.let { tp ->
                    _activeThreadProgress.update { map -> map + (download.id to tp) }
                }

                if (download.status == DownloadStatus.DOWNLOADING.name || download.status == DownloadStatus.QUEUED.name) {
                    Log.i(TAG, "Auto-resuming incomplete download on startup: ${download.title}")
                    startDownloadJob(download.id)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recovering downloads on startup: ${e.message}")
        }
    }

    /**
     * Returns the target downloads directory inside Downloads/NisTV.
     * Uses public Downloads/NisTV (/storage/emulated/0/Download/NisTV) so that files
     * are accessible in system file managers, with safe fallback to app external files.
     */
    fun getDownloadDirectory(): File {
        cachedDownloadDir?.let { if (it.exists() && it.canWrite()) return it }
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val publicNisTv = File(publicDownloads, "NisTV")
        try {
            if (!publicNisTv.exists()) {
                publicNisTv.mkdirs()
            }
            if (publicNisTv.exists() && publicNisTv.canWrite()) {
                cachedDownloadDir = publicNisTv
                return publicNisTv
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot write to public Downloads/NisTV (${e.message}), using app external files")
        }

        val appExtDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "NisTV")
        if (!appExtDir.exists()) {
            appExtDir.mkdirs()
        }
        cachedDownloadDir = appExtDir
        return appExtDir
    }

    suspend fun enqueueDownload(
        mediaId: String,
        title: String,
        downloadUrl: String,
        fileName: String,
        posterUrl: String? = null,
        mediaType: String = "movie",
        sizeBytes: Long = 0L,
        overview: String? = null,
        releaseDate: String? = null,
        rating: Double? = null,
        backdropUrl: String? = null,
        genres: String? = null,
        runtimeMinutes: Int? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        episodeName: String? = null,
        afdsFileId: String? = null,
        afdsSourceTable: String? = null,
        sourceProviderId: String? = null,
        seriesId: String? = null,
        seriesName: String? = null
    ): String {
        val id = UUID.randomUUID().toString()
        val targetFile = File(getDownloadDirectory(), sanitizeFileName(fileName))

        val entity = DownloadEntity(
            id = id,
            mediaId = mediaId,
            title = title,
            posterUrl = posterUrl,
            mediaType = mediaType,
            downloadUrl = downloadUrl,
            fileName = targetFile.name,
            filePath = targetFile.absolutePath,
            fileSizeBytes = sizeBytes,
            downloadedBytes = 0L,
            speedBytesPerSec = 0L,
            status = DownloadStatus.QUEUED.name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            overview = overview,
            releaseDate = releaseDate,
            rating = rating,
            backdropUrl = backdropUrl,
            genres = genres,
            runtimeMinutes = runtimeMinutes,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            episodeName = episodeName,
            afdsFileId = afdsFileId,
            afdsSourceTable = afdsSourceTable,
            sourceProviderId = sourceProviderId,
            seriesId = seriesId,
            seriesName = seriesName
        )

        downloadDao.insertDownload(entity)
        offlineMediaRepository?.let { repo ->
            scope.launch {
                try {
                    repo.saveFor(entity)
                } catch (e: Exception) {
                    Log.w(TAG, "Offline metadata save failed for ${entity.title}: ${e.message}")
                }
            }
        }
        startDownloadJob(id)
        return id
    }

    fun startDownload(id: String) {
        scope.launch {
            startDownloadJob(id)
        }
    }

    fun pauseDownload(id: String) {
        val inMemoryProgress = _activeProgressMap.value[id]
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        _activeProgressMap.update { map -> map.filterKeys { it != id } }
        scope.launch {
            val dl = downloadDao.getDownloadById(id)
            if (dl != null) {
                val partsBytes = try {
                    val stateFile = File("${dl.filePath}.parts.json")
                    if (stateFile.exists()) {
                        MultiThreadedDownloader.restoreChunksFromStateFile(stateFile, dl.id, dl.fileSizeBytes)
                            ?.sumOf { it.downloadedBytes } ?: 0L
                    } else 0L
                } catch (_: Exception) {
                    0L
                }
                val fileBytes = try {
                    val file = File(dl.filePath)
                    if (file.exists()) file.length() else 0L
                } catch (_: Exception) {
                    0L
                }

                val currentBytes = when {
                    (inMemoryProgress?.downloadedBytes ?: 0L) > 0L -> inMemoryProgress!!.downloadedBytes
                    partsBytes > 0L -> partsBytes
                    fileBytes > 0L -> fileBytes
                    else -> dl.downloadedBytes
                }

                if (currentBytes > 0L && currentBytes != dl.downloadedBytes) {
                    downloadDao.updateProgress(id, currentBytes, 0L)
                }
            }
            downloadDao.updateStatus(id, DownloadStatus.PAUSED.name)
            downloadDao.getDownloadById(id)?.let { updatedDl ->
                getThreadProgressFor(updatedDl)?.let { tp ->
                    _activeThreadProgress.update { map -> map + (id to tp) }
                }
            }
            checkStopForegroundService()
        }
    }

    fun resumeDownload(id: String) {
        scope.launch {
            val download = downloadDao.getDownloadById(id) ?: return@launch
            // Note: We deliberately do NOT proactively refresh the download URL on resume.
            // OkDownload stores chunk breakpoints by (URL + directory + filename). Eagerly refreshing
            // the signed AFDS link produces a different URL, altering OkDownload's task ID, wiping
            // the stored breakpoint, and forcing the file to restart from 0 bytes.
            // If the existing URL is expired, MultiThreadedDownloader's retry loop will invoke
            // onRenewUrl to renew the link and migrate the breakpoint info seamlessly.
            startDownloadJob(id)
        }
    }

    fun cancelDownload(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        _activeThreadProgress.update { map -> map.filterKeys { it != id } }
        _activeProgressMap.update { map -> map.filterKeys { it != id } }
        invalidateStorageStats()
        scope.launch {
            val download = downloadDao.getDownloadById(id)
            if (download != null) {
                try {
                    deleteFileWithFallbacks(download)
                } catch (_: Exception) {}
                downloadDao.deleteDownload(id)
                offlineMediaRepository?.let { repo ->
                    try {
                        repo.onDownloadDeletedOrCancelled(download)
                    } catch (_: Exception) {}
                }
            } else {
                downloadDao.deleteDownload(id)
            }
            checkStopForegroundService()
        }
    }

    fun deleteDownload(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        _activeThreadProgress.update { map -> map.filterKeys { it != id } }
        _activeProgressMap.update { map -> map.filterKeys { it != id } }
        invalidateStorageStats()
        scope.launch {
            val download = downloadDao.getDownloadById(id)
            if (download != null) {
                try {
                    deleteFileWithFallbacks(download)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed deleting file for ${download.title}: ${e.message}")
                }
                downloadDao.deleteDownload(id)
                offlineMediaRepository?.let { repo ->
                    try {
                        repo.onDownloadDeletedOrCancelled(download)
                    } catch (_: Exception) {}
                }
            } else {
                downloadDao.deleteDownload(id)
            }
            checkStopForegroundService()
        }
    }

    fun deleteDownloadsForSeries(seriesId: String) {
        scope.launch {
            val downloads = downloadDao.getDownloadsBySeriesSync(seriesId)
            for (dl in downloads) {
                activeJobs[dl.id]?.cancel()
                activeJobs.remove(dl.id)
                _activeThreadProgress.update { map -> map.filterKeys { it != dl.id } }
                _activeProgressMap.update { map -> map.filterKeys { it != dl.id } }
                try {
                    deleteFileWithFallbacks(dl)
                } catch (_: Exception) {}
                downloadDao.deleteDownload(dl.id)
            }
            invalidateStorageStats()
            if (downloads.isNotEmpty()) {
                offlineMediaRepository?.let { repo ->
                    try {
                        repo.onDownloadDeletedOrCancelled(downloads.first())
                    } catch (_: Exception) {}
                }
            }
            checkStopForegroundService()
        }
    }

    fun retryDownload(id: String) {
        scope.launch {
            val download = downloadDao.getDownloadById(id) ?: return@launch
            // Refresh AFDS link if download originated from AFDS archive
            if (!download.afdsFileId.isNullOrBlank() && afdsDownloadProvider != null) {
                try {
                    val refreshRes = afdsDownloadProvider.regenerateDownloadUrl(
                        fileId = download.afdsFileId,
                        sourceTable = download.afdsSourceTable ?: "files"
                    )
                    refreshRes.getOrNull()?.let { freshUrl ->
                        if (freshUrl.isNotBlank() && freshUrl != download.downloadUrl) {
                            Log.i(TAG, "Refreshed AFDS link on retry for '${download.title}'")
                            downloadDao.updateDownload(download.copy(downloadUrl = freshUrl))
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed AFDS link renewal on retry: ${e.message}")
                }
            }
            downloadDao.markFailed(id, DownloadStatus.QUEUED.name, null)
            startDownloadJob(id)
        }
    }

    private fun deleteFileWithFallbacks(download: DownloadEntity) {
        // Cancel any active job
        activeJobs[download.id]?.cancel()
        activeJobs.remove(download.id)

        val primary = File(download.filePath)
        if (primary.exists()) primary.delete()

        val partsFile = File("${download.filePath}.parts.json")
        if (partsFile.exists()) partsFile.delete()

        val publicFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NisTV/${download.fileName}")
        if (publicFile.exists()) publicFile.delete()
        val publicParts = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NisTV/${download.fileName}.parts.json")
        if (publicParts.exists()) publicParts.delete()

        val appFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "NisTV/${download.fileName}")
        if (appFile.exists()) appFile.delete()
        val appParts = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "NisTV/${download.fileName}.parts.json")
        if (appParts.exists()) appParts.delete()

        val legacyFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "NisTV_Downloads/${download.fileName}")
        if (legacyFile.exists()) legacyFile.delete()
    }

    private fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun checkStopForegroundService() {
        if (activeJobs.isEmpty()) {
            DownloadForegroundService.stop(context)
            updateNotification(null)
        }
    }

    private suspend fun startDownloadJob(id: String) {
        activeJobs[id]?.cancel()

        val job = scope.launch {
            val initialDownload = downloadDao.getDownloadById(id) ?: return@launch
            val isWifiOnly = userPreferences.isWifiOnlyDownloads.firstOrNull() ?: false

            getThreadProgressFor(initialDownload)?.let { tp ->
                _activeThreadProgress.update { map -> map + (id to tp) }
            }

            if (initialDownload.downloadedBytes > 0L) {
                _activeProgressMap.update { map ->
                    map + (id to ActiveDownloadProgress(
                        downloadedBytes = initialDownload.downloadedBytes,
                        totalBytes = initialDownload.fileSizeBytes,
                        speedBytesPerSec = 0L
                    ))
                }
            }

            if (isWifiOnly && !isWifiConnected()) {
                downloadDao.markFailed(id, DownloadStatus.PAUSED.name, "Waiting for Wi-Fi connection")
                checkStopForegroundService()
                return@launch
            }

            downloadDao.updateStatus(id, DownloadStatus.DOWNLOADING.name)
            val threadCount = userPreferences.downloadThreads.firstOrNull() ?: 4

            var retryCount = 0
            val maxRetries = 3
            var isDownloadComplete = false

            while (isActive && retryCount <= maxRetries && !isDownloadComplete) {
                val download = downloadDao.getDownloadById(id) ?: break
                if (download.status == DownloadStatus.PAUSED.name || download.status == DownloadStatus.CANCELLED.name) {
                    break
                }

                DownloadForegroundService.startOrUpdate(
                    context = context,
                    title = download.title,
                    progress = download.progressPercent,
                    speed = download.formattedSpeed,
                    indeterminate = download.fileSizeBytes <= 0L
                )

                try {
                    var lastDbUpdateTime = 0L
                    var lastNotificationUpdateTime = 0L

                    val success = multiThreadedDownloader.download(
                        download = download,
                        threadCount = threadCount,
                        onProgress = { currentBytes, totalBytes, speed, threadProgress ->
                            // Update live chunk progress in-memory (0ms latency, zero disk I/O)
                            _activeThreadProgress.update { map -> map + (id to threadProgress) }
                            _activeProgressMap.update { map ->
                                map + (id to ActiveDownloadProgress(currentBytes, totalBytes, speed))
                            }

                            val now = System.currentTimeMillis()

                            // Checkpoint Room SQLite disk writes at most once per 15s to eliminate InvalidationTracker thrashing
                            if (now - lastDbUpdateTime >= 15_000L) {
                                lastDbUpdateTime = now
                                downloadDao.updateProgress(id, currentBytes, speed)
                                if (totalBytes > 0 && download.fileSizeBytes <= 0L) {
                                    downloadDao.updateDownload(download.copy(fileSizeBytes = totalBytes))
                                }
                            }

                            // Throttle system NotificationManager Binder IPC to once per 1000ms
                            if (now - lastNotificationUpdateTime >= 1000L) {
                                lastNotificationUpdateTime = now
                                val progressEntity = download.copy(
                                    downloadedBytes = currentBytes,
                                    fileSizeBytes = if (totalBytes > 0) totalBytes else download.fileSizeBytes,
                                    speedBytesPerSec = speed
                                )
                                updateNotification(progressEntity)
                            }
                        },
                        onRecordDownloaded = { bytes ->
                            userPreferences.recordDownloadedBytes(bytes)
                        },
                        checkDailyLimit = {
                            val limit = userPreferences.checkDailyLimitExceeded()
                            if (limit.isExceeded) {
                                downloadDao.markFailed(id, DownloadStatus.PAUSED.name, "Daily download limit exceeded (${limit.limitGb} GB)")
                            }
                            limit
                        },
                        onRenewUrl = {
                            if (!download.afdsFileId.isNullOrBlank() && afdsDownloadProvider != null) {
                                try {
                                    val refreshRes = afdsDownloadProvider.regenerateDownloadUrl(
                                        fileId = download.afdsFileId,
                                        sourceTable = download.afdsSourceTable ?: "files"
                                    )
                                    val freshUrl = refreshRes.getOrNull()
                                    if (!freshUrl.isNullOrBlank() && freshUrl != download.downloadUrl) {
                                        Log.i(TAG, "Renewed AFDS download URL for '${download.title}'")
                                        downloadDao.updateDownload(download.copy(downloadUrl = freshUrl))
                                        freshUrl
                                    } else null
                                } catch (e: Exception) {
                                    Log.w(TAG, "AFDS renewal failed: ${e.message}")
                                    null
                                }
                            } else null
                        }
                    )

                    if (success) {
                        _activeThreadProgress.update { map -> map.filterKeys { it != id } }
                        _activeProgressMap.update { map -> map.filterKeys { it != id } }
                        val finalFile = File(download.filePath)
                        val finalBytes = if (finalFile.exists()) finalFile.length() else download.fileSizeBytes
                        val completedBytes = if (finalBytes > 0L) finalBytes else download.fileSizeBytes
                        downloadDao.updateDownload(
                            download.copy(
                                downloadedBytes = completedBytes,
                                fileSizeBytes = if (download.fileSizeBytes > 0L) download.fileSizeBytes else completedBytes,
                                speedBytesPerSec = 0L,
                                status = DownloadStatus.COMPLETED.name,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        showCompletedNotification(download.title)
                        isDownloadComplete = true
                        invalidateStorageStats()
                    } else {
                        _activeThreadProgress.update { map -> map.filterKeys { it != id } }
                        _activeProgressMap.update { map -> map.filterKeys { it != id } }
                        // Check if it was paused by user or daily limit
                        val current = downloadDao.getDownloadById(id)
                        if (current?.status == DownloadStatus.PAUSED.name || current?.status == DownloadStatus.CANCELLED.name) {
                            break
                        }
                        // Persist partial progress before retrying or failing
                        val partialBytes = try {
                            val stateFile = File("${download.filePath}.parts.json")
                            if (stateFile.exists()) {
                                MultiThreadedDownloader.restoreChunksFromStateFile(stateFile, download.id, download.fileSizeBytes)
                                    ?.sumOf { it.downloadedBytes } ?: 0L
                            } else {
                                val f = File(download.filePath)
                                if (f.exists()) f.length() else 0L
                            }
                        } catch (_: Exception) { 0L }
                        if (partialBytes > 0L) {
                            downloadDao.updateProgress(id, partialBytes, 0L)
                        }

                        retryCount++
                        if (retryCount <= maxRetries && isActive) {
                            delay(retryCount * 2000L)
                        } else {
                            downloadDao.markFailed(id, DownloadStatus.FAILED.name, "Download incomplete")
                        }
                    }
                } catch (e: CancellationException) {
                    Log.d(TAG, "Download job cancelled for $id")
                    return@launch
                } catch (e: Exception) {
                    retryCount++
                    if (retryCount <= maxRetries && isActive) {
                        Log.w(TAG, "Download error for $id (attempt $retryCount/$maxRetries): ${e.message}")
                        delay(retryCount * 2000L)
                    } else {
                        val partialBytes = try {
                            val stateFile = File("${download.filePath}.parts.json")
                            if (stateFile.exists()) {
                                MultiThreadedDownloader.restoreChunksFromStateFile(stateFile, download.id, download.fileSizeBytes)
                                    ?.sumOf { it.downloadedBytes } ?: 0L
                            } else {
                                val f = File(download.filePath)
                                if (f.exists()) f.length() else 0L
                            }
                        } catch (_: Exception) { 0L }
                        if (partialBytes > 0L) {
                            downloadDao.updateProgress(id, partialBytes, 0L)
                        }
                        _activeProgressMap.update { map -> map.filterKeys { it != id } }
                        _activeThreadProgress.update { map -> map.filterKeys { it != id } }
                        downloadDao.markFailed(id, DownloadStatus.FAILED.name, e.message ?: "Connection error")
                    }
                }
            }

            activeJobs.remove(id)
            checkStopForegroundService()
        }

        activeJobs[id] = job
    }

    private fun updateNotification(current: DownloadEntity?) {
        try {
            if (current == null) {
                if (activeJobs.isEmpty()) {
                    notificationManager.cancel(NOTIFICATION_ID)
                }
                return
            }

            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val progress = current.progressPercent
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Downloading: ${current.title}")
                .setContentText("${current.formattedDownloaded} / ${current.formattedTotal} • ${current.formattedSpeed}")
                .setProgress(100, progress, current.fileSizeBytes <= 0L)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)

            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (_: Exception) {}
    }

    private fun showCompletedNotification(title: String) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Download Complete")
                .setContentText(title)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (_: Exception) {}
    }

    fun getStorageStats(): StorageStats {
        val now = System.currentTimeMillis()
        val cached = lastStorageStats
        if (cached != null && now - lastStorageStatsScanTime < 30_000L) {
            return cached
        }

        var nistvBytes = 0L
        val dir = getDownloadDirectory()
        val scannedPaths = mutableSetOf<String>()

        fun scanDir(d: File) {
            if (d.exists() && scannedPaths.add(d.absolutePath)) {
                d.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        nistvBytes += file.length()
                    }
                }
            }
        }

        scanDir(dir)
        try {
            scanDir(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NisTV"))
        } catch (_: Exception) {}
        try {
            scanDir(File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "NisTV"))
        } catch (_: Exception) {}

        val (availableBytes, totalBytes) = try {
            val stat = StatFs(dir.path)
            Pair(stat.availableBytes, stat.totalBytes)
        } catch (_: Exception) {
            Pair(0L, 0L)
        }

        val stats = StorageStats(
            nistvUsedBytes = nistvBytes,
            availableBytes = availableBytes,
            totalBytes = totalBytes
        )
        lastStorageStats = stats
        lastStorageStatsScanTime = now
        return stats
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }
}
