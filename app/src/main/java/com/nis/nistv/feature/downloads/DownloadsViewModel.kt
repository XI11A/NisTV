package com.nis.nistv.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nis.nistv.core.database.dao.OfflineMediaDao
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.DownloadStatus
import com.nis.nistv.core.database.entity.OfflineMediaEntity
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.data.download.engine.DownloadManager
import com.nis.nistv.data.download.engine.MultiThreadedDownloader
import com.nis.nistv.data.download.engine.StorageStats
import com.nis.nistv.data.download.offline.DownloadsRepairManager
import com.nis.nistv.data.download.offline.MediaNameParser
import com.nis.nistv.data.download.offline.OfflineMediaRepository
import com.nis.nistv.data.download.offline.SeriesKeyHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SeasonDownloadGroup(
    val seasonNumber: Int,
    val name: String,
    val episodes: List<DownloadEntity>
)

data class SeriesDownloadGroup(
    val seriesId: String,
    val seriesName: String,
    val posterUrl: String?,
    val localPosterPath: String? = null,
    val seasonsCount: Int,
    val episodesCount: Int,
    val totalSizeBytes: Long,
    val seasons: List<SeasonDownloadGroup>
) {
    val formattedTotalSize: String
        get() = DownloadEntity.formatBytes(totalSizeBytes)

    val episodesLabel: String
        get() = buildString {
            if (seasonsCount > 1) {
                append("$seasonsCount seasons, ")
            } else {
                append("1 season, ")
            }
            if (episodesCount > 1) {
                append("$episodesCount episodes")
            } else {
                append("1 episode")
            }
        }
}

object DownloadsGrouping {
    fun groupMovies(completed: List<DownloadEntity>): List<DownloadEntity> {
        return completed.filter { dl ->
            (dl.status.equals(DownloadStatus.COMPLETED.name, ignoreCase = true) || dl.status.isBlank() || (dl.fileSizeBytes > 0 && dl.downloadedBytes >= dl.fileSizeBytes)) &&
                    (dl.mediaType.lowercase() == "movie" && !MediaNameParser.parse(dl.fileName).isTv)
        }.sortedByDescending { it.createdAt }
    }

    fun groupTvSeries(
        completed: List<DownloadEntity>,
        offlineMediaMap: Map<String, OfflineMediaEntity> = emptyMap()
    ): List<SeriesDownloadGroup> {
        val tvDownloads = completed.filter { dl ->
            (dl.status.equals(DownloadStatus.COMPLETED.name, ignoreCase = true) || dl.status.isBlank() || (dl.fileSizeBytes > 0 && dl.downloadedBytes >= dl.fileSizeBytes)) &&
                    (dl.mediaType.lowercase() == "tv" || MediaNameParser.parse(dl.fileName).isTv)
        }
        if (tvDownloads.isEmpty()) return emptyList()

        // Group by reliable series identifier (TMDB ID or fallback slug)
        val groupedBySeries = tvDownloads.groupBy { dl ->
            val parsed = MediaNameParser.parse(dl.fileName.ifBlank { dl.title })
            val sName = dl.seriesName?.ifBlank { null }
                ?: parsed.seriesName
                ?: if (parsed.isTv || dl.title.contains(Regex("""\bS\d+E\d+\b""", RegexOption.IGNORE_CASE))) {
                    dl.effectiveSeriesName?.ifBlank { null }
                } else null

            if (sName == null && dl.seriesId.isNullOrBlank()) {
                if (dl.mediaId.startsWith("unknown_") || dl.mediaId == "unknown") {
                    "unknown"
                } else {
                    SeriesKeyHelper.UNSORTED_KEY
                }
            } else {
                SeriesKeyHelper.getSeriesKey(dl.seriesId, sName)
            }
        }

        return groupedBySeries.map { (groupKey, downloads) ->
            val first = downloads.first()
            val parsedFirst = MediaNameParser.parse(first.fileName.ifBlank { first.title })
            val isUnsorted = groupKey == SeriesKeyHelper.UNSORTED_KEY
            val resolvedSeriesId = if (isUnsorted) {
                SeriesKeyHelper.UNSORTED_KEY
            } else if (groupKey == "unknown") {
                "unknown"
            } else {
                first.seriesId?.ifBlank { null } ?: first.effectiveSeriesId ?: groupKey
            }

            val candidateName = if (isUnsorted) {
                "Other / Unsorted"
            } else {
                first.seriesName?.ifBlank { null }
                    ?: parsedFirst.seriesName
                    ?: first.effectiveSeriesName
                    ?: first.title.substringBefore(" S")
            }
            val resolvedSeriesName = candidateName?.ifBlank { "Other / Unsorted" } ?: "Other / Unsorted"

            val offlineMedia = offlineMediaMap[groupKey]
                ?: offlineMediaMap["tv_$resolvedSeriesId"]
                ?: offlineMediaMap[SeriesKeyHelper.buildFallbackSeriesKey(resolvedSeriesName)]

            // Group episodes by seasonNumber (null -> season 1)
            val seasonGroups = downloads
                .groupBy { dl ->
                    dl.seasonNumber ?: MediaNameParser.parse(dl.fileName).seasonNumber ?: 1
                }
                .map { (seasonNum, epDownloads) ->
                    SeasonDownloadGroup(
                        seasonNumber = seasonNum,
                        name = "Season $seasonNum",
                        episodes = epDownloads.sortedWith(
                            compareBy(
                                { it.seasonNumber ?: 1 },
                                { it.episodeNumber ?: MediaNameParser.parse(it.fileName).episodeNumber ?: 0 }
                            )
                        )
                    )
                }
                .sortedBy { it.seasonNumber }

            val totalSize = downloads.sumOf { it.fileSizeBytes.coerceAtLeast(it.downloadedBytes) }

            SeriesDownloadGroup(
                seriesId = resolvedSeriesId,
                seriesName = offlineMedia?.title ?: resolvedSeriesName,
                posterUrl = downloads.firstNotNullOfOrNull { it.posterUrl },
                localPosterPath = offlineMedia?.localPosterPath,
                seasonsCount = seasonGroups.size,
                episodesCount = downloads.size,
                totalSizeBytes = totalSize,
                seasons = seasonGroups
            )
        }.sortedByDescending { it.seasons.maxOfOrNull { s -> s.episodes.maxOfOrNull { e -> e.updatedAt } ?: 0L } ?: 0L }
    }
}

data class DownloadsUiState(
    val activeDownloads: List<DownloadEntity> = emptyList(),
    val completedDownloads: List<DownloadEntity> = emptyList(),
    val movies: List<DownloadEntity> = emptyList(),
    val tvSeries: List<SeriesDownloadGroup> = emptyList(),
    val isWifiOnly: Boolean = false,
    val storageStats: StorageStats? = null,
    val showAddUrlDialog: Boolean = false,
    val todayDownloadedBytes: Long = 0L,
    val afdsDailyLinksUsed: Int = 0,
    val afdsDailyLinksLimit: Int = 100,
    val isSavingOfflineDetails: Boolean = false,
    val activeThreadProgress: Map<String, List<MultiThreadedDownloader.ThreadProgress>> = emptyMap()
)

private data class DownloadsPrefsData(
    val wifiOnly: Boolean,
    val todayBytes: Long,
    val afdsDailyLinksUsed: Int,
    val afdsDailyLinksLimit: Int,
    val isSavingOfflineDetails: Boolean
)

class DownloadsViewModel(
    private val downloadManager: DownloadManager,
    private val userPreferences: UserPreferences,
    private val offlineMediaDao: OfflineMediaDao? = null,
    private val offlineMediaRepository: OfflineMediaRepository? = null,
    private val downloadsRepairManager: DownloadsRepairManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private val _storageStats = MutableStateFlow<StorageStats?>(null)

    init {
        viewModelScope.launch {
            try {
                downloadsRepairManager?.runRepairIfNeeded()
            } catch (_: Exception) {}
        }

        refreshStorageStats()

        viewModelScope.launch {
            val offlineFlow = offlineMediaDao?.getAllMedia() ?: flowOf(emptyList())

            // 1. Completed flow: isolated so grouping regexes ONLY run when completed media actually changes
            val completedFlow = combine(
                downloadManager.completedDownloads,
                offlineFlow
            ) { completed, offlineMediaList ->
                val offlineMap = offlineMediaList.associateBy { it.id }
                val moviesList = DownloadsGrouping.groupMovies(completed)
                val tvSeriesList = DownloadsGrouping.groupTvSeries(completed, offlineMap)
                Triple(completed, moviesList, tvSeriesList)
            }.flowOn(Dispatchers.Default)

            // 2. Active downloads flow: merges in-memory progress and thread state without touching Room or disk
            val activeFlow = combine(
                downloadManager.activeDownloads,
                downloadManager.activeProgressMap,
                downloadManager.activeThreadProgress
            ) { active, progressMap, threadProgressMap ->
                val activeWithProgress = active.map { dl ->
                    val prog = progressMap[dl.id]
                    if (prog != null) {
                        dl.copy(
                            downloadedBytes = prog.downloadedBytes,
                            fileSizeBytes = if (prog.totalBytes > 0) prog.totalBytes else dl.fileSizeBytes,
                            speedBytesPerSec = prog.speedBytesPerSec
                        )
                    } else {
                        dl
                    }
                }
                Pair(activeWithProgress, threadProgressMap)
            }.flowOn(Dispatchers.Default)

            // 3. User preferences and quota flow
            val prefsFlow = combine(
                userPreferences.isWifiOnlyDownloads,
                userPreferences.todayDownloadedBytes,
                userPreferences.afdsDailyLinksUsed,
                userPreferences.afdsDailyLinksLimit,
                offlineMediaRepository?.isSavingOfflineDetails ?: flowOf(false)
            ) { wifiOnly, todayBytes, usedQuota, limitQuota, isSaving ->
                DownloadsPrefsData(wifiOnly, todayBytes, usedQuota, limitQuota, isSaving)
            }.flowOn(Dispatchers.Default)

            // 4. Combine completely decoupled pipelines into UiState
            combine(
                activeFlow,
                completedFlow,
                prefsFlow,
                _storageStats
            ) { (activeList, threadProgressMap), (completed, movies, tvSeries), prefs, stats ->
                DownloadsUiState(
                    activeDownloads = activeList,
                    completedDownloads = completed,
                    movies = movies,
                    tvSeries = tvSeries,
                    isWifiOnly = prefs.wifiOnly,
                    storageStats = stats,
                    todayDownloadedBytes = prefs.todayBytes,
                    afdsDailyLinksUsed = prefs.afdsDailyLinksUsed,
                    afdsDailyLinksLimit = prefs.afdsDailyLinksLimit,
                    isSavingOfflineDetails = prefs.isSavingOfflineDetails,
                    activeThreadProgress = threadProgressMap
                )
            }.collect { newState ->
                _uiState.update { current ->
                    newState.copy(showAddUrlDialog = current.showAddUrlDialog)
                }
            }
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val stats = downloadManager.getStorageStats()
            _storageStats.value = stats
        }
    }

    fun scanAndImportDownloads() {
        viewModelScope.launch {
            downloadsRepairManager?.scanAndImportLocalFiles()
            downloadsRepairManager?.runRepairIfNeeded(force = true)
            refreshStorageStats()
        }
    }

    fun toggleWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setWifiOnlyDownloads(enabled)
        }
    }

    fun pauseDownload(id: String) {
        downloadManager.pauseDownload(id)
        refreshStorageStats()
    }

    fun resumeDownload(id: String) {
        downloadManager.resumeDownload(id)
        refreshStorageStats()
    }

    fun cancelDownload(id: String) {
        downloadManager.cancelDownload(id)
        refreshStorageStats()
    }

    fun retryDownload(id: String) {
        downloadManager.retryDownload(id)
        refreshStorageStats()
    }

    fun deleteDownload(id: String) {
        downloadManager.deleteDownload(id)
        refreshStorageStats()
    }

    fun deleteSeries(seriesId: String) {
        downloadManager.deleteDownloadsForSeries(seriesId)
        refreshStorageStats()
    }

    fun openAddUrlDialog() {
        _uiState.update { it.copy(showAddUrlDialog = true) }
    }

    fun dismissAddUrlDialog() {
        _uiState.update { it.copy(showAddUrlDialog = false) }
    }

    fun addManualDownload(title: String, url: String) {
        if (url.isBlank() || title.isBlank()) return
        viewModelScope.launch {
            val fileName = if (title.contains(".")) title else "$title.mp4"
            val parsed = MediaNameParser.parse(fileName)
            val isTv = parsed.isTv

            downloadManager.enqueueDownload(
                mediaId = if (isTv) "manual_tv_${System.currentTimeMillis()}" else "manual_${System.currentTimeMillis()}",
                title = title.trim(),
                downloadUrl = url.trim(),
                fileName = fileName,
                mediaType = if (isTv) "tv" else "movie",
                seasonNumber = parsed.seasonNumber,
                episodeNumber = parsed.episodeNumber,
                seriesName = parsed.seriesName
            )
            dismissAddUrlDialog()
        }
    }

    companion object {
        fun provideFactory(
            downloadManager: DownloadManager,
            userPreferences: UserPreferences,
            offlineMediaDao: OfflineMediaDao? = null,
            offlineMediaRepository: OfflineMediaRepository? = null,
            downloadsRepairManager: DownloadsRepairManager? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(DownloadsViewModel::class.java)) {
                    return DownloadsViewModel(
                        downloadManager,
                        userPreferences,
                        offlineMediaDao,
                        offlineMediaRepository,
                        downloadsRepairManager
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
