package com.nis.nistv.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nis.nistv.core.database.dao.AutopilotRuleDao
import com.nis.nistv.core.database.dao.DownloadDao
import com.nis.nistv.core.database.dao.OfflineMediaDao
import com.nis.nistv.core.database.dao.TrackedSeriesDao
import com.nis.nistv.core.database.entity.AutopilotRuleEntity
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.DownloadStatus
import com.nis.nistv.core.database.entity.OfflineCastMember
import com.nis.nistv.core.database.entity.OfflineCrewMember
import com.nis.nistv.core.database.entity.OfflineEpisodeEntity
import com.nis.nistv.core.database.entity.OfflineMediaEntity
import com.nis.nistv.core.database.entity.OfflineSeasonEntity
import com.nis.nistv.core.database.entity.TrackedSeriesEntity
import com.nis.nistv.core.database.entity.WatchLaterEntity
import com.nis.nistv.data.autopilot.AutopilotManager
import com.nis.nistv.data.download.offline.MediaNameParser
import com.nis.nistv.data.download.offline.OfflineMediaRepository
import com.nis.nistv.data.download.offline.SeriesKeyHelper
import com.nis.nistv.data.recommendation.IntelligentRecommendationEngine
import com.nis.nistv.data.repository.WatchLaterRepository
import com.nis.nistv.data.tmdb.model.TmdbMediaItem
import com.nis.nistv.data.tmdb.model.TmdbMediaType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class OfflineDetailsUiState(
    val mediaType: String = "movie",
    val tmdbId: Long = 0L,
    val mediaId: String = "",
    val media: OfflineMediaEntity? = null,
    val seasons: List<OfflineSeasonEntity> = emptyList(),
    val selectedSeasonNumber: Int = 1,
    val episodes: List<OfflineEpisodeEntity> = emptyList(),
    val downloadedEpisodesMap: Map<String, DownloadEntity> = emptyMap(),
    val movieDownload: DownloadEntity? = null,
    val allSeriesDownloads: List<DownloadEntity> = emptyList(),
    val cast: List<OfflineCastMember> = emptyList(),
    val crew: List<OfflineCrewMember> = emptyList(),
    val directorsOrCreators: String = "",
    val isWatchLater: Boolean = false,
    val isLoading: Boolean = true,
    val lastPlaybackPositionMs: Long = 0L,
    val nextEpisodeToPlay: DownloadEntity? = null,
    val trackedSeries: TrackedSeriesEntity? = null,
    val autopilotRule: AutopilotRuleEntity? = null
)

class OfflineDetailsViewModel(
    val mediaType: String,
    val mediaId: String,
    private val offlineMediaDao: OfflineMediaDao,
    private val downloadDao: DownloadDao,
    private val offlineMediaRepository: OfflineMediaRepository,
    private val watchLaterRepository: WatchLaterRepository,
    private val recommendationEngine: IntelligentRecommendationEngine,
    private val trackedSeriesDao: TrackedSeriesDao,
    private val autopilotRuleDao: AutopilotRuleDao,
    private val autopilotManager: AutopilotManager
) : ViewModel() {

    // Overload for Long-based constructors
    constructor(
        mediaType: String,
        tmdbId: Long,
        offlineMediaDao: OfflineMediaDao,
        downloadDao: DownloadDao,
        offlineMediaRepository: OfflineMediaRepository,
        watchLaterRepository: WatchLaterRepository,
        recommendationEngine: IntelligentRecommendationEngine,
        trackedSeriesDao: TrackedSeriesDao,
        autopilotRuleDao: AutopilotRuleDao,
        autopilotManager: AutopilotManager
    ) : this(
        mediaType = mediaType,
        mediaId = tmdbId.toString(),
        offlineMediaDao = offlineMediaDao,
        downloadDao = downloadDao,
        offlineMediaRepository = offlineMediaRepository,
        watchLaterRepository = watchLaterRepository,
        recommendationEngine = recommendationEngine,
        trackedSeriesDao = trackedSeriesDao,
        autopilotRuleDao = autopilotRuleDao,
        autopilotManager = autopilotManager
    )

    val tmdbId: Long = SeriesKeyHelper.extractTmdbId(mediaId) ?: mediaId.toLongOrNull() ?: 0L
    val seriesKey: String = if (mediaType.equals("tv", ignoreCase = true)) {
        SeriesKeyHelper.getSeriesKey(mediaId, null, tmdbId)
    } else {
        if (tmdbId > 0) "movie_$tmdbId" else "movie_$mediaId"
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val _uiState = MutableStateFlow(
        OfflineDetailsUiState(
            mediaType = mediaType,
            tmdbId = tmdbId,
            mediaId = mediaId
        )
    )
    val uiState: StateFlow<OfflineDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Tracked Series & Autopilot for TV shows
            if (mediaType.equals("tv", ignoreCase = true)) {
                launch {
                    trackedSeriesDao.getTrackedSeries(seriesKey).collect { tracked ->
                        _uiState.update { it.copy(trackedSeries = tracked) }
                    }
                }
                launch {
                    autopilotRuleDao.getRule(seriesKey).collect { rule ->
                        _uiState.update { it.copy(autopilotRule = rule) }
                    }
                }
            }

            // 1. Observe Watch Later status
            if (tmdbId > 0) {
                launch {
                    watchLaterRepository.isInWatchLater(mediaType, tmdbId).collect { saved ->
                        _uiState.update { it.copy(isWatchLater = saved) }
                    }
                }
            }

            // 2. Observe offline media metadata
            launch {
                offlineMediaDao.getMedia(seriesKey).collect { mediaEntity ->
                    if (mediaEntity != null) {
                        val castList = parseCast(mediaEntity.castJson)
                        val crewList = parseCrew(mediaEntity.crewJson)
                        val directorsText = crewList.filter {
                            it.job.equals("Director", ignoreCase = true) || it.job.equals("Creator", ignoreCase = true)
                        }.joinToString(", ") { it.name }

                        _uiState.update {
                            it.copy(
                                media = mediaEntity,
                                cast = castList,
                                crew = crewList,
                                directorsOrCreators = directorsText,
                                isLoading = false
                            )
                        }
                    } else {
                        // Fallback: build synthetic OfflineMediaEntity from downloads if available
                        val currentDownloads = _uiState.value.allSeriesDownloads
                        val movieDl = _uiState.value.movieDownload
                        if (currentDownloads.isNotEmpty()) {
                            val first = currentDownloads.first()
                            val parsed = MediaNameParser.parse(first.fileName)
                            val title = first.seriesName ?: parsed.seriesName ?: first.title.substringBefore(" S")
                            val fallbackMedia = OfflineMediaEntity(
                                id = seriesKey,
                                tmdbId = tmdbId,
                                mediaType = "tv",
                                title = title,
                                overview = first.overview ?: "Offline TV Series",
                                releaseDate = first.releaseDate,
                                rating = first.rating ?: 0.0,
                                voteCount = 0,
                                castJson = "[]",
                                crewJson = "[]",
                                localPosterPath = null,
                                localBackdropPath = null,
                                savedAt = first.createdAt
                            )
                            _uiState.update { it.copy(media = fallbackMedia, isLoading = false) }
                        } else if (movieDl != null) {
                            val fallbackMedia = OfflineMediaEntity(
                                id = seriesKey,
                                tmdbId = tmdbId,
                                mediaType = "movie",
                                title = movieDl.title,
                                overview = movieDl.overview ?: "Offline Movie",
                                releaseDate = movieDl.releaseDate,
                                rating = movieDl.rating ?: 0.0,
                                voteCount = 0,
                                castJson = "[]",
                                crewJson = "[]",
                                localPosterPath = null,
                                localBackdropPath = null,
                                savedAt = movieDl.createdAt
                            )
                            _uiState.update { it.copy(media = fallbackMedia, isLoading = false) }
                        }
                    }
                }
            }

            // 3. Observe downloads
            if (mediaType.equals("movie", ignoreCase = true)) {
                launch {
                    downloadDao.getAllDownloads().collect { all ->
                        val movieDl = all.firstOrNull {
                            it.mediaType.equals("movie", ignoreCase = true) &&
                                    (it.id == mediaId || it.mediaId == mediaId || it.mediaId == tmdbId.toString() || it.mediaId.startsWith("${tmdbId}_"))
                        }
                        _uiState.update {
                            it.copy(
                                movieDownload = movieDl,
                                lastPlaybackPositionMs = movieDl?.lastPlaybackPositionMs ?: 0L,
                                isLoading = false
                            )
                        }
                    }
                }
            } else {
                // TV series: observe seasons from Room
                launch {
                    offlineMediaDao.getSeasons(seriesKey).collect { seasonsList ->
                        val currentDownloads = _uiState.value.allSeriesDownloads
                        val effectiveSeasons = if (seasonsList.isNotEmpty()) {
                            seasonsList
                        } else {
                            // Synthesize seasons from downloads if Room table is empty
                            currentDownloads.map { it.seasonNumber ?: MediaNameParser.parse(it.fileName).seasonNumber ?: 1 }
                                .distinct()
                                .sorted()
                                .map { sNum ->
                                    OfflineSeasonEntity(
                                        seriesKey = seriesKey,
                                        seasonNumber = sNum,
                                        name = "Season $sNum",
                                        episodeCount = currentDownloads.count { (it.seasonNumber ?: 1) == sNum },
                                        localPosterPath = null
                                    )
                                }
                        }

                        val currentSelected = _uiState.value.selectedSeasonNumber
                        val validSeason = if (effectiveSeasons.any { it.seasonNumber == currentSelected }) {
                            currentSelected
                        } else {
                            effectiveSeasons.firstOrNull()?.seasonNumber ?: 1
                        }
                        _uiState.update { it.copy(seasons = effectiveSeasons, selectedSeasonNumber = validSeason) }
                    }
                }

                // Observe series episodes from downloadDao
                launch {
                    val searchKey = if (tmdbId > 0) tmdbId.toString() else mediaId
                    downloadDao.getDownloadsBySeries(searchKey).collect { seriesDownloads ->
                        val effectiveDownloads = if (seriesDownloads.isNotEmpty()) {
                            seriesDownloads
                        } else {
                            // Fallback: search all completed downloads matching series name / id
                            downloadDao.getAllDownloadsSync().filter { dl ->
                                dl.seriesId == mediaId || dl.seriesId == seriesKey ||
                                        (dl.effectiveSeriesName != null && dl.effectiveSeriesName.equals(mediaId.removePrefix("tv_").replace('_', ' '), ignoreCase = true))
                            }
                        }

                        val epMap = effectiveDownloads.associateBy {
                            val s = it.seasonNumber ?: MediaNameParser.parse(it.fileName).seasonNumber ?: 1
                            val e = it.episodeNumber ?: MediaNameParser.parse(it.fileName).episodeNumber ?: 1
                            "${s}_$e"
                        }
                        val nextEp = resolveNextPlayableEpisode(effectiveDownloads)

                        _uiState.update { current ->
                            val syntheticSeasons = if (current.seasons.isEmpty() && effectiveDownloads.isNotEmpty()) {
                                effectiveDownloads.map { it.seasonNumber ?: MediaNameParser.parse(it.fileName).seasonNumber ?: 1 }
                                    .distinct()
                                    .sorted()
                                    .map { sNum ->
                                        OfflineSeasonEntity(
                                            seriesKey = seriesKey,
                                            seasonNumber = sNum,
                                            name = "Season $sNum",
                                            episodeCount = effectiveDownloads.count { (it.seasonNumber ?: 1) == sNum },
                                            localPosterPath = null
                                        )
                                    }
                            } else current.seasons

                            current.copy(
                                allSeriesDownloads = effectiveDownloads,
                                downloadedEpisodesMap = epMap,
                                nextEpisodeToPlay = nextEp,
                                seasons = syntheticSeasons,
                                lastPlaybackPositionMs = nextEp?.lastPlaybackPositionMs ?: 0L,
                                isLoading = false
                            )
                        }
                    }
                }

                // Observe episodes for currently selected season
                launch {
                    _uiState.map { it.selectedSeasonNumber }.distinctUntilChanged().collect { seasonNum ->
                        offlineMediaDao.getEpisodesForSeason(seriesKey, seasonNum).collect { epList ->
                            val currentDownloads = _uiState.value.allSeriesDownloads
                            val effectiveEpisodes = if (epList.isNotEmpty()) {
                                epList
                            } else {
                                // Synthesize episodes from downloads for this season
                                currentDownloads.filter { (it.seasonNumber ?: MediaNameParser.parse(it.fileName).seasonNumber ?: 1) == seasonNum }
                                    .sortedWith(compareBy({ it.seasonNumber ?: 1 }, { it.episodeNumber ?: MediaNameParser.parse(it.fileName).episodeNumber ?: 0 }))
                                    .map { dl ->
                                        val ep = dl.episodeNumber ?: MediaNameParser.parse(dl.fileName).episodeNumber ?: 1
                                        OfflineEpisodeEntity(
                                            seriesKey = seriesKey,
                                            seasonNumber = seasonNum,
                                            episodeNumber = ep,
                                            name = dl.episodeName ?: "Episode $ep",
                                            overview = dl.overview ?: "",
                                            airDate = dl.releaseDate,
                                            runtimeMinutes = dl.runtimeMinutes,
                                            rating = dl.rating ?: 0.0,
                                            localStillPath = null
                                        )
                                    }
                            }
                            _uiState.update { it.copy(episodes = effectiveEpisodes) }
                        }
                    }
                }
            }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        _uiState.update { it.copy(selectedSeasonNumber = seasonNumber) }
    }

    fun refreshOfflineDetails() {
        viewModelScope.launch {
            val target = _uiState.value.movieDownload ?: _uiState.value.allSeriesDownloads.firstOrNull()
            if (target != null) {
                offlineMediaRepository.saveFor(target, forceRefresh = true)
            }
        }
    }

    fun toggleWatchLater() {
        val state = _uiState.value
        val media = state.media
        val item = WatchLaterEntity(
            id = seriesKey,
            mediaId = tmdbId,
            mediaType = mediaType.lowercase(),
            title = media?.title ?: if (mediaType == "tv") "TV Series" else "Movie",
            posterPath = media?.localPosterPath,
            backdropPath = media?.localBackdropPath,
            overview = media?.overview,
            releaseYear = media?.releaseDate?.take(4),
            voteAverage = media?.rating,
            genres = media?.genres
        )
        viewModelScope.launch {
            val added = watchLaterRepository.toggleWatchLater(item)
            if (added && tmdbId > 0) {
                recommendationEngine.recordMediaInteraction(
                    media = TmdbMediaItem(
                        id = tmdbId,
                        title = item.title,
                        overview = item.overview.orEmpty(),
                        posterPath = item.posterPath,
                        backdropPath = item.backdropPath,
                        releaseYear = item.releaseYear,
                        voteAverage = item.voteAverage ?: 0.0,
                        mediaType = if (mediaType == "tv") TmdbMediaType.TV else TmdbMediaType.MOVIE
                    ),
                    interactionType = "FAVORITE"
                )
            }
        }
    }

    fun deleteMovieDownload(onComplete: () -> Unit) {
        val dl = _uiState.value.movieDownload ?: return
        viewModelScope.launch {
            downloadDao.deleteDownload(dl.id)
            offlineMediaRepository.onDownloadDeletedOrCancelled(dl)
            onComplete()
        }
    }

    fun deleteSeason(seasonNumber: Int) {
        viewModelScope.launch {
            offlineMediaRepository.deleteSeason(seriesKey, seasonNumber)
        }
    }

    fun deleteAllSeriesEpisodes(onComplete: () -> Unit) {
        viewModelScope.launch {
            val downloads = _uiState.value.allSeriesDownloads
            for (dl in downloads) {
                downloadDao.deleteDownload(dl.id)
            }
            if (downloads.isNotEmpty()) {
                offlineMediaRepository.onDownloadDeletedOrCancelled(downloads.first())
            }
            onComplete()
        }
    }

    private fun resolveNextPlayableEpisode(downloads: List<DownloadEntity>): DownloadEntity? {
        val completed = downloads.filter { 
            it.status.equals(DownloadStatus.COMPLETED.name, ignoreCase = true) || 
            (it.fileSizeBytes > 0 && it.downloadedBytes >= it.fileSizeBytes)
        }
        if (completed.isEmpty()) return downloads.firstOrNull()

        // 1. If an episode was partially played (position > 0), resume that episode
        val inProgress = completed.filter { it.lastPlaybackPositionMs > 0 }
            .maxByOrNull { it.updatedAt }
        if (inProgress != null) return inProgress

        // 2. Otherwise sort by season and episode and return the first
        return completed.sortedWith(
            compareBy<DownloadEntity> { it.seasonNumber ?: 1 }
                .thenBy { it.episodeNumber ?: 1 }
        ).firstOrNull()
    }

    fun setNotifyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val existing = trackedSeriesDao.getTrackedSeriesSync(seriesKey)
            if (existing != null) {
                trackedSeriesDao.setNotifyEnabled(seriesKey, enabled)
            } else {
                val title = _uiState.value.media?.title
                    ?: _uiState.value.allSeriesDownloads.firstOrNull()?.effectiveSeriesName
                    ?: "Series"
                trackedSeriesDao.insertOrUpdate(
                    TrackedSeriesEntity(
                        seriesKey = seriesKey,
                        tmdbId = tmdbId,
                        name = title,
                        notifyEnabled = enabled
                    )
                )
            }
        }
    }

    fun updateAutopilotRule(rule: AutopilotRuleEntity) {
        viewModelScope.launch {
            autopilotRuleDao.insertOrUpdate(rule)
            if (rule.enabled) {
                autopilotManager.runAutopilotForSeries(seriesKey)
            }
        }
    }

    private fun parseCast(jsonStr: String?): List<OfflineCastMember> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<OfflineCastMember>>(jsonStr)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseCrew(jsonStr: String?): List<OfflineCrewMember> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<OfflineCrewMember>>(jsonStr)
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        fun provideFactory(
            mediaType: String,
            mediaId: String,
            offlineMediaDao: OfflineMediaDao,
            downloadDao: DownloadDao,
            offlineMediaRepository: OfflineMediaRepository,
            watchLaterRepository: WatchLaterRepository,
            recommendationEngine: IntelligentRecommendationEngine,
            trackedSeriesDao: TrackedSeriesDao,
            autopilotRuleDao: AutopilotRuleDao,
            autopilotManager: AutopilotManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OfflineDetailsViewModel(
                    mediaType = mediaType,
                    mediaId = mediaId,
                    offlineMediaDao = offlineMediaDao,
                    downloadDao = downloadDao,
                    offlineMediaRepository = offlineMediaRepository,
                    watchLaterRepository = watchLaterRepository,
                    recommendationEngine = recommendationEngine,
                    trackedSeriesDao = trackedSeriesDao,
                    autopilotRuleDao = autopilotRuleDao,
                    autopilotManager = autopilotManager
                ) as T
            }
        }

        fun provideFactory(
            mediaType: String,
            tmdbId: Long,
            offlineMediaDao: OfflineMediaDao,
            downloadDao: DownloadDao,
            offlineMediaRepository: OfflineMediaRepository,
            watchLaterRepository: WatchLaterRepository,
            recommendationEngine: IntelligentRecommendationEngine,
            trackedSeriesDao: TrackedSeriesDao,
            autopilotRuleDao: AutopilotRuleDao,
            autopilotManager: AutopilotManager
        ): ViewModelProvider.Factory = provideFactory(
            mediaType = mediaType,
            mediaId = tmdbId.toString(),
            offlineMediaDao = offlineMediaDao,
            downloadDao = downloadDao,
            offlineMediaRepository = offlineMediaRepository,
            watchLaterRepository = watchLaterRepository,
            recommendationEngine = recommendationEngine,
            trackedSeriesDao = trackedSeriesDao,
            autopilotRuleDao = autopilotRuleDao,
            autopilotManager = autopilotManager
        )
    }
}
