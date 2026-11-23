package com.nis.nistv.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nis.nistv.core.database.NisTvDatabase
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.data.recommendation.IntelligentRecommendationEngine
import com.nis.nistv.data.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class HistoryItemUi(
    val channelId: String,
    val title: String,
    val logoUrl: String,
    val category: String,
    val language: String,
    val lastWatchedAt: Long,
    val durationFormatted: String
)

data class DownloadHistoryItemUi(
    val downloadId: String,
    val title: String,
    val posterUrl: String?,
    val lastWatchedAt: Long,
    val progressPercent: Int
)

data class HistoryAndPrivacyUiState(
    val isHistoryPaused: Boolean = false,
    val todayItems: List<HistoryItemUi> = emptyList(),
    val yesterdayItems: List<HistoryItemUi> = emptyList(),
    val earlierItems: List<HistoryItemUi> = emptyList(),
    val recentDownloads: List<DownloadHistoryItemUi> = emptyList(),
    val isResettingTaste: Boolean = false,
    val isClearingHistory: Boolean = false,
    val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryAndPrivacyViewModel(
    private val database: NisTvDatabase,
    private val userPreferences: UserPreferences,
    private val recommendationEngine: IntelligentRecommendationEngine,
    private val profileRepository: ProfileRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryAndPrivacyUiState())
    val uiState: StateFlow<HistoryAndPrivacyUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun currentProfileId(): String =
        profileRepository?.activeProfileId?.value ?: "default"

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            val targetProfileFlow = profileRepository?.activeProfileId ?: flowOf("default")

            targetProfileFlow.flatMapLatest { profileId ->
                val isPausedFlow = profileRepository?.activeProfile?.let { activeFlow ->
                    combine(activeFlow) { it[0].historyPaused }
                } ?: userPreferences.isWatchHistoryPaused

                val progressFlow = database.playbackProgressDao().getAllProgressForProfile(profileId)

                combine(
                    isPausedFlow,
                    database.watchHistoryDao().getWatchHistoryWithDetails(profileId),
                    database.downloadDao().getAllDownloads(),
                    progressFlow
                ) { isPaused, channelHistory, downloads, progressList ->
                    val now = System.currentTimeMillis()
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = now
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val todayStart = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    val yesterdayStart = cal.timeInMillis

                    val mappedChannels = channelHistory.map {
                        val durationMins = (it.watchDurationMs / 60000L).coerceAtLeast(1)
                        HistoryItemUi(
                            channelId = it.channelId,
                            title = it.channelName,
                            logoUrl = it.channelLogoUrl,
                            category = it.category,
                            language = it.language,
                            lastWatchedAt = it.lastWatchedAt,
                            durationFormatted = "${durationMins}m watched"
                        )
                    }

                    val today = mappedChannels.filter { it.lastWatchedAt >= todayStart }
                    val yesterday = mappedChannels.filter { it.lastWatchedAt in yesterdayStart until todayStart }
                    val earlier = mappedChannels.filter { it.lastWatchedAt < yesterdayStart }

                    val progressMap = progressList.associateBy { it.downloadId }
                    val downloadMap = downloads.associateBy { it.id }

                    val watchedDownloads = progressList
                        .filter { it.lastWatchedAt > 0 }
                        .sortedByDescending { it.lastWatchedAt }
                        .take(20)
                        .mapNotNull { progress ->
                            val dl = downloadMap[progress.downloadId] ?: return@mapNotNull null
                            val pct = if (progress.durationMs > 0) {
                                ((progress.positionMs.toFloat() / progress.durationMs.toFloat()) * 100f).toInt().coerceIn(0, 100)
                            } else 0
                            DownloadHistoryItemUi(
                                downloadId = dl.id,
                                title = dl.title,
                                posterUrl = dl.posterUrl,
                                lastWatchedAt = progress.lastWatchedAt,
                                progressPercent = pct
                            )
                        }

                    // Fallback to downloads table for default profile if playback_progress is empty
                    val finalDownloads = if (watchedDownloads.isEmpty() && profileId == "default") {
                        downloads
                            .filter { it.lastWatchedAt > 0 }
                            .sortedByDescending { it.lastWatchedAt }
                            .take(20)
                            .map { dl ->
                                val pct = if (dl.durationMs > 0) {
                                    ((dl.lastPlaybackPositionMs.toFloat() / dl.durationMs.toFloat()) * 100f).toInt().coerceIn(0, 100)
                                } else 0
                                DownloadHistoryItemUi(
                                    downloadId = dl.id,
                                    title = dl.title,
                                    posterUrl = dl.posterUrl,
                                    lastWatchedAt = dl.lastWatchedAt,
                                    progressPercent = pct
                                )
                            }
                    } else {
                        watchedDownloads
                    }

                    HistoryAndPrivacyUiState(
                        isHistoryPaused = isPaused,
                        todayItems = today,
                        yesterdayItems = yesterday,
                        earlierItems = earlier,
                        recentDownloads = finalDownloads,
                        message = _uiState.value.message
                    )
                }
            }.collect { newState ->
                _uiState.update { newState }
            }
        }
    }

    fun togglePauseHistory(paused: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val pId = currentProfileId()
            if (profileRepository != null) {
                profileRepository.setHistoryPaused(pId, paused)
            } else {
                userPreferences.setWatchHistoryPaused(paused)
            }
            _uiState.update {
                it.copy(
                    isHistoryPaused = paused,
                    message = if (paused) "Watch history paused" else "Watch history resumed"
                )
            }
        }
    }

    fun deleteChannelHistory(channelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val pId = currentProfileId()
            database.watchHistoryDao().deleteWatchHistoryItem(pId, channelId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val pId = currentProfileId()
            database.watchHistoryDao().clearWatchHistory(pId)
            database.playbackProgressDao().clearProgressForProfile(pId)
            _uiState.update { it.copy(message = "Watch history cleared") }
        }
    }

    fun resetRecommendations() {
        viewModelScope.launch(Dispatchers.IO) {
            recommendationEngine.resetRecommendations()
            _uiState.update { it.copy(message = "Taste and recommendation profile reset") }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
