package com.nis.nistv.feature.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nis.nistv.core.database.dao.TrackedSeriesDao
import com.nis.nistv.core.database.entity.TrackedSeriesEntity
import com.nis.nistv.core.database.entity.WatchLaterEntity
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.data.download.engine.DownloadManager
import com.nis.nistv.data.download.offline.SeriesKeyHelper
import com.nis.nistv.data.download.provider.AfdsAuthService
import com.nis.nistv.data.download.provider.AfdsDownloadProvider
import com.nis.nistv.data.download.provider.DownloadItemCandidate
import com.nis.nistv.data.download.provider.DownloadMediaType
import com.nis.nistv.data.recommendation.IntelligentRecommendationEngine
import com.nis.nistv.data.repository.WatchLaterRepository
import com.nis.nistv.data.tmdb.model.*
import com.nis.nistv.data.tmdb.repository.MediaPreviewStore
import com.nis.nistv.data.tmdb.repository.TmdbRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DownloadLimitExceededInfo(
    val used: Int,
    val limit: Int,
    val pendingCandidate: DownloadItemCandidate? = null
)

data class DownloadStartedNotification(
    val title: String,
    val fileName: String,
    val posterUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class MediaDetailsUiState(
    val mediaType: TmdbMediaType = TmdbMediaType.MOVIE,
    val mediaId: Long = 0L,
    val movieDetails: TmdbMovieDetails? = null,
    val tvDetails: TmdbTvDetails? = null,
    val currentSeasonDetails: TmdbSeasonDetails? = null,
    val selectedSeasonNumber: Int = 1,
    val userRegion: String = "IN",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isSearchingDownload: Boolean = false,
    val downloadCandidates: List<DownloadItemCandidate> = emptyList(),
    val showDownloadDialog: Boolean = false,
    val downloadDialogTitle: String? = null,
    val downloadDialogMessage: String? = null,
    val showDownloadLimitDialog: Boolean = false,
    val downloadLimitExceededInfo: DownloadLimitExceededInfo? = null,
    val targetEpisodeSeason: Int? = null,
    val targetEpisodeNumber: Int? = null,
    val targetEpisodeTitle: String? = null,
    val showAfdsLoginDialog: Boolean = false,
    val isAfdsLoading: Boolean = false,
    val afdsAuthError: String? = null,
    val afdsOtpStep: Int = 0,
    val afdsCurrentEmail: String = "",
    val afdsBotId: String? = null,
    val afdsLoginType: String? = null,
    val downloadStartedNotification: DownloadStartedNotification? = null,
    val isWatchLater: Boolean = false,
    val recommendations: List<TmdbMediaItem> = emptyList(),
    val similarMedia: List<TmdbMediaItem> = emptyList(),
    val preview: MediaPreview? = null,
    val isEpisodeRadarNotifyEnabled: Boolean = false
) {
    val detailsPreview: MediaPreview?
        get() = movieDetails?.toPreview() ?: tvDetails?.toPreview()
    val title: String
        get() = movieDetails?.title ?: tvDetails?.name ?: ""

    val originalTitle: String?
        get() = movieDetails?.originalTitle ?: tvDetails?.originalName

    val overview: String
        get() = movieDetails?.overview ?: tvDetails?.overview ?: ""

    val posterPath: String?
        get() = movieDetails?.posterPath ?: tvDetails?.posterPath

    val backdropPath: String?
        get() = movieDetails?.backdropPath ?: tvDetails?.backdropPath

    val voteAverage: Double
        get() = movieDetails?.voteAverage ?: tvDetails?.voteAverage ?: 0.0

    val releaseYear: String?
        get() = movieDetails?.releaseDate?.take(4) ?: tvDetails?.firstAirDate?.take(4)

    val genres: List<TmdbGenre>
        get() = movieDetails?.genres ?: tvDetails?.genres ?: emptyList()

    val trailerVideo: TmdbVideoDto?
        get() {
            val videos = movieDetails?.videos?.results ?: tvDetails?.videos?.results ?: emptyList()
            return videos.firstOrNull { it.isYouTubeTrailer }
                ?: videos.firstOrNull { it.site.equals("YouTube", ignoreCase = true) }
        }

    val trailerUrl: String?
        get() = trailerVideo?.youTubeUrl

    val regionWatchProviders: TmdbCountryWatchProviders?
        get() {
            val map = movieDetails?.watchProviders?.results ?: tvDetails?.watchProviders?.results ?: emptyMap()
            return map[userRegion] ?: map["IN"] ?: map["US"] ?: map.values.firstOrNull()
        }

    val cast: List<TmdbCastMember>
        get() = movieDetails?.credits?.cast ?: tvDetails?.credits?.cast ?: emptyList()

    val similar: List<TmdbMediaItem>
        get() = similarMedia.ifEmpty {
            movieDetails?.similar?.results?.map { it.toMediaItem() }
                ?: tvDetails?.similar?.results?.map { it.toMediaItem() }
                ?: emptyList()
        }
}

class MediaDetailsViewModel(
    private val repository: TmdbRepository,
    private val downloadManager: DownloadManager,
    private val afdsProvider: AfdsDownloadProvider,
    private val userPreferences: UserPreferences,
    private val afdsAuthService: AfdsAuthService,
    private val recommendationEngine: IntelligentRecommendationEngine,
    private val watchLaterRepository: WatchLaterRepository,
    private val mediaPreviewStore: MediaPreviewStore,
    private val trackedSeriesDao: TrackedSeriesDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaDetailsUiState())
    val uiState: StateFlow<MediaDetailsUiState> = _uiState.asStateFlow()

    private var searchDownloadJob: Job? = null
    private var watchLaterJob: Job? = null
    private var trackedSeriesJob: Job? = null

    fun loadDetails(mediaType: TmdbMediaType, mediaId: Long) {
        val cachedPreview = mediaPreviewStore.get(mediaType.value, mediaId)
        _uiState.update {
            it.copy(
                mediaType = mediaType,
                mediaId = mediaId,
                preview = cachedPreview,
                isLoading = it.movieDetails == null && it.tvDetails == null,
                errorMessage = null
            )
        }

        watchLaterJob?.cancel()
        watchLaterJob = viewModelScope.launch {
            watchLaterRepository.isInWatchLater(mediaType.value, mediaId).collect { isSaved ->
                _uiState.update { it.copy(isWatchLater = isSaved) }
            }
        }

        if (mediaType == TmdbMediaType.TV) {
            trackedSeriesJob?.cancel()
            val seriesKey = SeriesKeyHelper.buildSeriesKey(mediaId)
            trackedSeriesJob = viewModelScope.launch {
                trackedSeriesDao.getTrackedSeries(seriesKey).collect { tracked ->
                    _uiState.update { it.copy(isEpisodeRadarNotifyEnabled = tracked?.notifyEnabled ?: false) }
                }
            }
        }

        viewModelScope.launch {
            val region = userPreferences.preferredCountry.firstOrNull() ?: "IN"
            _uiState.update { it.copy(userRegion = region) }

            try {
                when (mediaType) {
                    TmdbMediaType.MOVIE -> {
                        val details = repository.getMovieDetails(mediaId)
                        mediaPreviewStore.put(details.toPreview())
                        _uiState.update {
                            it.copy(
                                movieDetails = details,
                                preview = details.toPreview(),
                                isLoading = false
                            )
                        }
                        recommendationEngine.recordMovieDetailsInteraction(details, "VIEW")
                    }

                    TmdbMediaType.TV -> {
                        val details = repository.getTvDetails(mediaId)
                        mediaPreviewStore.put(details.toPreview())
                        _uiState.update {
                            it.copy(
                                tvDetails = details,
                                preview = details.toPreview(),
                                isLoading = false
                            )
                        }
                        recommendationEngine.recordTvDetailsInteraction(details, "VIEW")
                        if (details.seasons.isNotEmpty()) {
                            val firstValidSeason = details.seasons.firstOrNull { it.seasonNumber > 0 }
                                ?: details.seasons.first()
                            loadSeason(firstValidSeason.seasonNumber)
                        }
                    }
                }

                // Fetch Recommendations & Similar Content using dedicated TMDB endpoints
                try {
                    val recs = repository.getRecommendations(mediaType, mediaId)
                    val similar = repository.getSimilar(mediaType, mediaId)
                    _uiState.update {
                        it.copy(
                            recommendations = recs,
                            similarMedia = similar
                        )
                    }
                } catch (_: Exception) {}
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load media details"
                    )
                }
            }
        }
    }

    fun loadSeason(seasonNumber: Int) {
        val tvId = _uiState.value.mediaId
        if (tvId == 0L) return

        viewModelScope.launch {
            try {
                val season = repository.getTvSeason(tvId, seasonNumber)
                _uiState.update {
                    it.copy(
                        currentSeasonDetails = season,
                        selectedSeasonNumber = seasonNumber
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun onDownloadClick() {
        val state = _uiState.value
        val title = state.title
        val originalTitle = state.originalTitle
        val year = state.releaseYear?.toIntOrNull()
        val type = if (state.mediaType == TmdbMediaType.MOVIE) DownloadMediaType.MOVIE else DownloadMediaType.TV
        val seasonNum = if (type == DownloadMediaType.TV) state.selectedSeasonNumber else null

        _uiState.update {
            it.copy(
                targetEpisodeSeason = null,
                targetEpisodeNumber = null,
                targetEpisodeTitle = null,
                downloadDialogTitle = title
            )
        }

        searchDownloadJob?.cancel()
        searchDownloadJob = viewModelScope.launch {
            val token = userPreferences.afdsAuthToken.firstOrNull()?.trim().orEmpty()
            if (token.isBlank()) {
                _uiState.update {
                    it.copy(
                        showAfdsLoginDialog = true,
                        showDownloadDialog = false,
                        isSearchingDownload = false,
                        afdsOtpStep = 0,
                        afdsAuthError = null
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isSearchingDownload = true,
                    showDownloadDialog = true,
                    downloadCandidates = emptyList(),
                    downloadDialogMessage = "Searching AFDS download sources for '$title'..."
                )
            }

            try {
                afdsProvider.searchFlow(
                    title = title,
                    year = year,
                    mediaType = type,
                    seasonNumber = seasonNum,
                    episodeNumber = null,
                    originalTitle = originalTitle
                ).collect { updatedCandidates ->
                    _uiState.update { current ->
                        current.copy(
                            downloadCandidates = updatedCandidates,
                            downloadDialogMessage = if (updatedCandidates.isEmpty()) "Searching AFDS download sources for '$title'..." else null
                        )
                    }
                }

                val finalCandidates = _uiState.value.downloadCandidates
                _uiState.update {
                    it.copy(
                        isSearchingDownload = false,
                        downloadDialogMessage = if (finalCandidates.isEmpty()) {
                            "No download sources currently found for '$title' in AFDS archive."
                        } else null
                    )
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    val finalCandidates = _uiState.value.downloadCandidates
                    _uiState.update {
                        it.copy(
                            isSearchingDownload = false,
                            downloadDialogMessage = if (finalCandidates.isEmpty()) "Failed to search download sources: ${e.message}" else null
                        )
                    }
                }
            }
        }
    }

    fun onDownloadEpisode(seasonNumber: Int, episodeNumber: Int, episodeName: String = "") {
        val state = _uiState.value
        val title = state.title
        val originalTitle = state.originalTitle
        val year = state.releaseYear?.toIntOrNull()
        val epTag = String.format(java.util.Locale.US, "S%02dE%02d", seasonNumber, episodeNumber)
        val dialogHeading = if (episodeName.isNotBlank()) "$title $epTag: $episodeName" else "$title $epTag"

        _uiState.update {
            it.copy(
                targetEpisodeSeason = seasonNumber,
                targetEpisodeNumber = episodeNumber,
                targetEpisodeTitle = episodeName,
                downloadDialogTitle = dialogHeading
            )
        }

        searchDownloadJob?.cancel()
        searchDownloadJob = viewModelScope.launch {
            val token = userPreferences.afdsAuthToken.firstOrNull()?.trim().orEmpty()
            if (token.isBlank()) {
                _uiState.update {
                    it.copy(
                        showAfdsLoginDialog = true,
                        showDownloadDialog = false,
                        isSearchingDownload = false,
                        afdsOtpStep = 0,
                        afdsAuthError = null
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isSearchingDownload = true,
                    showDownloadDialog = true,
                    downloadCandidates = emptyList(),
                    downloadDialogMessage = "Searching AFDS for $epTag..."
                )
            }

            try {
                afdsProvider.searchFlow(
                    title = title,
                    year = year,
                    mediaType = DownloadMediaType.TV,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    originalTitle = originalTitle
                ).collect { updatedCandidates ->
                    _uiState.update { current ->
                        current.copy(
                            downloadCandidates = updatedCandidates,
                            downloadDialogMessage = if (updatedCandidates.isEmpty()) "Searching AFDS for $epTag..." else null
                        )
                    }
                }

                val finalCandidates = _uiState.value.downloadCandidates
                _uiState.update {
                    it.copy(
                        isSearchingDownload = false,
                        downloadDialogMessage = if (finalCandidates.isEmpty()) {
                            "No download sources found for '$dialogHeading' in AFDS archive."
                        } else null
                    )
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    val finalCandidates = _uiState.value.downloadCandidates
                    _uiState.update {
                        it.copy(
                            isSearchingDownload = false,
                            downloadDialogMessage = if (finalCandidates.isEmpty()) "Failed to search download sources: ${e.message}" else null
                        )
                    }
                }
            }
        }
    }

    fun selectCandidateToDownload(candidate: DownloadItemCandidate) {
        searchDownloadJob?.cancel()
        searchDownloadJob = null
        viewModelScope.launch {
            val limitCheck = userPreferences.checkAfdsLimitExceeded()
            if (limitCheck.isExceeded) {
                _uiState.update {
                    it.copy(
                        showDownloadLimitDialog = true,
                        downloadLimitExceededInfo = DownloadLimitExceededInfo(
                            used = limitCheck.used,
                            limit = limitCheck.limit,
                            pendingCandidate = candidate
                        )
                    )
                }
                return@launch
            }
            executeDownload(candidate)
        }
    }

    fun confirmDownloadDespiteLimit() {
        val info = _uiState.value.downloadLimitExceededInfo
        val candidate = info?.pendingCandidate ?: return
        _uiState.update { it.copy(showDownloadLimitDialog = false, downloadLimitExceededInfo = null) }
        executeDownload(candidate)
    }

    fun dismissDownloadLimitDialog() {
        _uiState.update { it.copy(showDownloadLimitDialog = false, downloadLimitExceededInfo = null) }
    }

    private fun executeDownload(candidate: DownloadItemCandidate) {
        val state = _uiState.value
        _uiState.update { it.copy(isSearchingDownload = true, downloadDialogMessage = "Resolving link from AFDS...") }

        viewModelScope.launch {
            try {
                val sNum = state.targetEpisodeSeason
                val eNum = state.targetEpisodeNumber
                val (effectiveMediaId, effectiveTitle) = if (sNum != null && eNum != null) {
                    val epTag = String.format(java.util.Locale.US, "S%02dE%02d", sNum, eNum)
                    Pair("${state.mediaId}_${epTag}", "${state.title} $epTag")
                } else {
                    Pair(state.mediaId.toString(), state.title)
                }

                val resolved = afdsProvider.resolve(candidate)
                val poster = state.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                val backdrop = state.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }
                val genreStr = (state.movieDetails?.genres ?: state.tvDetails?.genres ?: emptyList())
                    .joinToString(", ") { it.name }
                val runtime = state.movieDetails?.runtime
                val rDate = state.movieDetails?.releaseDate ?: state.tvDetails?.firstAirDate ?: state.releaseYear

                val afdsTable = if (candidate.extraInfo.startsWith("tbl:")) {
                    candidate.extraInfo.substringAfter("tbl:")
                } else {
                    "files"
                }

                downloadManager.enqueueDownload(
                    mediaId = effectiveMediaId,
                    title = effectiveTitle,
                    downloadUrl = resolved.url,
                    fileName = resolved.fileName,
                    posterUrl = poster,
                    mediaType = state.mediaType.value,
                    sizeBytes = resolved.sizeBytes,
                    overview = state.overview.ifBlank { null },
                    releaseDate = rDate,
                    rating = state.voteAverage,
                    backdropUrl = backdrop,
                    genres = genreStr.ifBlank { null },
                    runtimeMinutes = runtime,
                    seasonNumber = sNum,
                    episodeNumber = eNum,
                    episodeName = state.targetEpisodeTitle,
                    afdsFileId = candidate.id,
                    afdsSourceTable = afdsTable,
                    sourceProviderId = candidate.sourceProviderId,
                    seriesId = if (state.mediaType == TmdbMediaType.TV) state.mediaId.toString() else null,
                    seriesName = if (state.mediaType == TmdbMediaType.TV) state.title else null
                )

                // Record download event in recommendation engine
                val genreIds = (state.movieDetails?.genres ?: state.tvDetails?.genres ?: emptyList()).map { it.id }
                recommendationEngine.recordMediaInteraction(
                    media = TmdbMediaItem(
                        id = state.mediaId,
                        title = state.title,
                        overview = state.overview,
                        posterPath = state.posterPath,
                        backdropPath = state.backdropPath,
                        releaseYear = state.releaseYear,
                        voteAverage = state.voteAverage,
                        mediaType = state.mediaType,
                        genreIds = genreIds
                    ),
                    interactionType = "DOWNLOAD"
                )

                _uiState.update {
                    it.copy(
                        isSearchingDownload = false,
                        showDownloadDialog = false,
                        downloadCandidates = emptyList(),
                        downloadStartedNotification = DownloadStartedNotification(
                            title = effectiveTitle,
                            fileName = resolved.fileName,
                            posterUrl = poster
                        )
                    )
                }
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (msg.contains("Daily", ignoreCase = true) && msg.contains("limit", ignoreCase = true)) {
                    val used = userPreferences.afdsDailyLinksUsed.firstOrNull() ?: 0
                    val limit = userPreferences.afdsDailyLinksLimit.firstOrNull() ?: 100
                    _uiState.update {
                        it.copy(
                            isSearchingDownload = false,
                            showDownloadDialog = false,
                            showDownloadLimitDialog = true,
                            downloadLimitExceededInfo = DownloadLimitExceededInfo(
                                used = used,
                                limit = limit,
                                pendingCandidate = candidate
                            )
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isSearchingDownload = false,
                            downloadDialogMessage = "Resolution failed: ${e.message}"
                        )
                    }
                }
            }
        }
    }

    fun requestAfdsOtp(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            _uiState.update { it.copy(afdsAuthError = "Please enter a valid email address.") }
            return
        }
        _uiState.update { it.copy(isAfdsLoading = true, afdsAuthError = null, afdsCurrentEmail = cleanEmail) }
        viewModelScope.launch {
            afdsAuthService.requestLoginOtp(cleanEmail)
                .onSuccess { resp ->
                    _uiState.update {
                        it.copy(
                            isAfdsLoading = false,
                            afdsOtpStep = 1,
                            afdsBotId = resp.botId,
                            afdsLoginType = resp.loginType ?: "telegram",
                            afdsAuthError = null
                        )
                    }
                }
                .onFailure { err ->
                    afdsAuthService.requestLoginOtpEmail(cleanEmail)
                        .onSuccess {
                            _uiState.update {
                                it.copy(
                                    isAfdsLoading = false,
                                    afdsOtpStep = 1,
                                    afdsBotId = null,
                                    afdsLoginType = "email",
                                    afdsAuthError = null
                                )
                            }
                        }
                        .onFailure { emailErr ->
                            _uiState.update {
                                it.copy(
                                    isAfdsLoading = false,
                                    afdsAuthError = emailErr.message ?: err.message ?: "Failed to send code"
                                )
                            }
                        }
                }
        }
    }

    fun verifyAfdsOtp(code: String) {
        val email = _uiState.value.afdsCurrentEmail
        val cleanCode = code.trim()
        if (cleanCode.length != 6) {
            _uiState.update { it.copy(afdsAuthError = "Please enter the complete 6-digit code.") }
            return
        }
        _uiState.update { it.copy(isAfdsLoading = true, afdsAuthError = null) }
        viewModelScope.launch {
            afdsAuthService.verifyLoginOtp(email, cleanCode)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isAfdsLoading = false,
                            showAfdsLoginDialog = false,
                            afdsAuthError = null
                        )
                    }
                    onDownloadClick()
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isAfdsLoading = false,
                            afdsAuthError = err.message ?: "Verification failed. Check the code."
                        )
                    }
                }
        }
    }

    fun onAfdsAuthSuccess() {
        _uiState.update {
            it.copy(
                showAfdsLoginDialog = false,
                afdsAuthError = null,
                afdsOtpStep = 0
            )
        }
        val sNum = _uiState.value.targetEpisodeSeason
        val epNum = _uiState.value.targetEpisodeNumber
        val epTitle = _uiState.value.targetEpisodeTitle.orEmpty()
        if (sNum != null && epNum != null) {
            onDownloadEpisode(sNum, epNum, epTitle)
        } else {
            onDownloadClick()
        }
    }

    fun submitManualAfdsToken(token: String) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) return
        viewModelScope.launch {
            userPreferences.setAfdsAuthToken(cleanToken)
            _uiState.update {
                it.copy(
                    showAfdsLoginDialog = false,
                    afdsAuthError = null
                )
            }
            onDownloadClick()
        }
    }

    fun dismissAfdsLoginDialog() {
        _uiState.update {
            it.copy(
                showAfdsLoginDialog = false,
                afdsAuthError = null,
                afdsOtpStep = 0
            )
        }
    }

    fun dismissDownloadDialog() {
        searchDownloadJob?.cancel()
        searchDownloadJob = null
        _uiState.update {
            it.copy(
                showDownloadDialog = false,
                isSearchingDownload = false,
                downloadCandidates = emptyList(),
                downloadDialogMessage = null
            )
        }
    }

    fun toggleWatchLater() {
        val state = _uiState.value
        val genreStr = (state.movieDetails?.genres ?: state.tvDetails?.genres ?: emptyList())
            .joinToString(", ") { it.name }
        val item = WatchLaterEntity(
            id = "${state.mediaType.value}_${state.mediaId}",
            mediaId = state.mediaId,
            mediaType = state.mediaType.value,
            title = state.title,
            posterPath = state.posterPath,
            backdropPath = state.backdropPath,
            overview = state.overview.ifBlank { null },
            releaseYear = state.releaseYear,
            voteAverage = state.voteAverage,
            genres = genreStr.ifBlank { null }
        )
        viewModelScope.launch {
            val added = watchLaterRepository.toggleWatchLater(item)
            _uiState.update { it.copy(isWatchLater = added) }

            val genreIds = (state.movieDetails?.genres ?: state.tvDetails?.genres ?: emptyList()).map { it.id }
            recommendationEngine.recordMediaInteraction(
                media = TmdbMediaItem(
                    id = state.mediaId,
                    title = state.title,
                    overview = state.overview,
                    posterPath = state.posterPath,
                    backdropPath = state.backdropPath,
                    releaseYear = state.releaseYear,
                    voteAverage = state.voteAverage,
                    mediaType = state.mediaType,
                    genreIds = genreIds
                ),
                interactionType = if (added) "FAVORITE" else "UNFAVORITE"
            )
        }
    }

    fun toggleEpisodeRadarNotification(enabled: Boolean) {
        val state = _uiState.value
        if (state.mediaType != TmdbMediaType.TV) return
        val seriesKey = SeriesKeyHelper.buildSeriesKey(state.mediaId)
        viewModelScope.launch {
            val existing = trackedSeriesDao.getTrackedSeriesSync(seriesKey)
            if (existing != null) {
                trackedSeriesDao.setNotifyEnabled(seriesKey, enabled)
            } else {
                trackedSeriesDao.insertOrUpdate(
                    TrackedSeriesEntity(
                        seriesKey = seriesKey,
                        tmdbId = state.mediaId,
                        name = state.title.ifBlank { "Series" },
                        notifyEnabled = enabled,
                        nextAirDate = state.tvDetails?.nextEpisodeToAir?.airDate
                    )
                )
            }
            _uiState.update { it.copy(isEpisodeRadarNotifyEnabled = enabled) }
        }
    }

    fun dismissDownloadStartedNotification() {
        _uiState.update {
            it.copy(downloadStartedNotification = null)
        }
    }

    companion object {
        fun provideFactory(
            repository: TmdbRepository,
            downloadManager: DownloadManager,
            afdsProvider: AfdsDownloadProvider,
            userPreferences: UserPreferences,
            afdsAuthService: AfdsAuthService,
            recommendationEngine: IntelligentRecommendationEngine,
            watchLaterRepository: WatchLaterRepository,
            mediaPreviewStore: MediaPreviewStore,
            trackedSeriesDao: TrackedSeriesDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MediaDetailsViewModel(
                    repository,
                    downloadManager,
                    afdsProvider,
                    userPreferences,
                    afdsAuthService,
                    recommendationEngine,
                    watchLaterRepository,
                    mediaPreviewStore,
                    trackedSeriesDao
                ) as T
            }
        }
    }
}
