package com.nis.nistv.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nis.nistv.core.database.dao.DownloadDao
import com.nis.nistv.core.database.dao.PlaybackProgressDao
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.PlaybackProgressEntity
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.data.epg.EpgManager
import com.nis.nistv.data.epg.ProgrammeInfo
import com.nis.nistv.data.recommendation.CandidatePick
import com.nis.nistv.data.recommendation.DynamicRecommendationState
import com.nis.nistv.data.recommendation.IntelligentRecommendationEngine
import com.nis.nistv.data.recommendation.TimeContext
import com.nis.nistv.data.recommendation.TonightPicksHelper
import com.nis.nistv.data.repository.ChannelRepository
import com.nis.nistv.data.repository.ProfileRepository
import com.nis.nistv.data.tmdb.model.LanguageUtils
import com.nis.nistv.data.tmdb.model.TmdbMediaItem
import com.nis.nistv.data.tmdb.model.TmdbMediaType
import com.nis.nistv.data.tmdb.repository.TmdbRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TmdbHomeState(
    val featuredMedia: TmdbMediaItem? = null,
    val trendingMovies: List<TmdbMediaItem> = emptyList(),
    val trendingTv: List<TmdbMediaItem> = emptyList(),
    val languageRecommendedMedia: List<TmdbMediaItem> = emptyList(),
    val isLoaded: Boolean = false
)

data class HomeUiState(
    val featuredChannel: ChannelEntity? = null,
    val featuredMedia: TmdbMediaItem? = null,
    val billboardEpg: ProgrammeInfo? = null,
    val preferredLanguage: String = "Hindi",
    val trendingMovies: List<TmdbMediaItem> = emptyList(),
    val trendingTv: List<TmdbMediaItem> = emptyList(),
    val languageRecommendedMedia: List<TmdbMediaItem> = emptyList(),
    val dynamicRecommendedForYou: List<TmdbMediaItem> = emptyList(),
    val becauseYouWatchedTitle: String? = null,
    val becauseYouWatchedMedia: List<TmdbMediaItem> = emptyList(),
    val smartRecommendedChannels: List<ChannelEntity> = emptyList(),
    val recommendationReason: String? = null,
    val recommendedLanguageChannels: List<ChannelEntity> = emptyList(),
    val top10Channels: List<ChannelEntity> = emptyList(),
    val newsChannels: List<ChannelEntity> = emptyList(),
    val moviesChannels: List<ChannelEntity> = emptyList(),
    val musicChannels: List<ChannelEntity> = emptyList(),
    val kidsChannels: List<ChannelEntity> = emptyList(),
    val recentlyWatched: List<ChannelEntity> = emptyList(),
    val recentChannels: List<ChannelEntity> = emptyList(),
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val categories: List<String> = listOf("All", "Movies", "Series", "Live TV", "News"),
    val selectedFilter: String = "All",
    val favoriteChannelIds: Set<String> = emptySet(),
    val tonightPicks: List<CandidatePick> = emptyList(),
    val isTmdbLoaded: Boolean = false,
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val channelRepository: ChannelRepository,
    private val epgManager: EpgManager,
    private val userPreferences: UserPreferences,
    private val tmdbRepository: TmdbRepository,
    private val recommendationEngine: IntelligentRecommendationEngine,
    private val downloadDao: DownloadDao,
    private val playbackProgressDao: PlaybackProgressDao? = null,
    private val profileRepository: ProfileRepository? = null
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _billboardEpg = MutableStateFlow<ProgrammeInfo?>(null)
    private val _tmdbState = MutableStateFlow(TmdbHomeState())

    private val favoritesFlow = channelRepository.getFavorites().map { list ->
        list.map { it.id }.toSet()
    }

    private val baseChannelsFlow = combine(
        channelRepository.getAllChannels(),
        channelRepository.getRecentlyWatched(),
        favoritesFlow
    ) { all, recent, favs -> Triple(all, recent, favs) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val continueWatchingFlow = if (profileRepository != null && playbackProgressDao != null) {
        profileRepository.activeProfileId.flatMapLatest { profileId ->
            combine(
                downloadDao.getCompletedDownloads(),
                playbackProgressDao.getAllProgressForProfile(profileId)
            ) { downloads: List<DownloadEntity>, progressList: List<PlaybackProgressEntity> ->
                buildContinueWatching(
                    downloads = downloads,
                    playbackProgressList = progressList,
                    profileId = profileId
                )
            }
        }
    } else {
        downloadDao.getCompletedDownloads().map { list ->
            buildContinueWatching(list)
        }
    }

    private val combinedContentFlow = combine(
        baseChannelsFlow,
        _tmdbState,
        recommendationEngine.recommendationState,
        continueWatchingFlow
    ) { channelsTuple, tmdb, recState, continueWatchingItems ->
        ContentPayload(channelsTuple, tmdb, recState, continueWatchingItems)
    }

    private data class ContentPayload(
        val channelsTuple: Triple<List<ChannelEntity>, List<ChannelEntity>, Set<String>>,
        val tmdb: TmdbHomeState,
        val recState: DynamicRecommendationState,
        val continueWatching: List<ContinueWatchingItem>
    )

    val uiState: StateFlow<HomeUiState> = combine(
        combinedContentFlow,
        _selectedFilter,
        _billboardEpg,
        userPreferences.preferredLanguage
    ) { payload, filter, epg, preferredLang ->
        val (all, recent, favIds) = payload.channelsTuple
        val tmdb = payload.tmdb
        val recState = payload.recState
        val continueWatchingItems = payload.continueWatching
        val preferredChannels = all.filter { it.language.equals(preferredLang, ignoreCase = true) }
        val featuredChan = preferredChannels.firstOrNull() ?: all.firstOrNull()

        val top10 = all.take(15)
        val recommendedChan = preferredChannels.take(20)
        val news = all.filter { it.category.equals("News", ignoreCase = true) }.take(15)
        val movies = all.filter { it.category.equals("Movies", ignoreCase = true) || it.category.equals("Animation", ignoreCase = true) }.take(15)
        val music = all.filter { it.category.equals("Music", ignoreCase = true) }.take(15)
        val kids = all.filter { it.category.equals("Kids", ignoreCase = true) || it.category.equals("Family", ignoreCase = true) }.take(15)

        val candidates = mutableListOf<CandidatePick>()
        for (item in continueWatchingItems) {
            val d = item.download
            candidates.add(
                CandidatePick(
                    id = d.id,
                    tmdbId = d.id.substringAfterLast('_').toLongOrNull(),
                    title = d.title,
                    mediaType = d.mediaType,
                    posterUrl = d.posterUrl,
                    runtimeMinutes = (d.durationMs / 60000).toInt().takeIf { it > 0 } ?: d.runtimeMinutes,
                    affinityScore = 1.8,
                    isDownloaded = true,
                    isWatched = false
                )
            )
        }
        for (media in recState.recommendedForYou.ifEmpty { tmdb.trendingMovies + tmdb.trendingTv }) {
            val isTv = media.mediaType == TmdbMediaType.TV
            candidates.add(
                CandidatePick(
                    id = if (isTv) "tv_${media.id}" else "movie_${media.id}",
                    tmdbId = media.id,
                    title = media.title,
                    mediaType = if (isTv) "tv" else "movie",
                    posterUrl = media.posterPath,
                    runtimeMinutes = if (isTv) 45 else 110,
                    affinityScore = (media.voteAverage / 10.0) * 1.5,
                    isDownloaded = false,
                    isWatched = false
                )
            )
        }
        val tonight = TonightPicksHelper.rankTonightPicks(
            candidates = candidates.distinctBy { it.id },
            timeContext = TimeContext.current(),
            isOnline = tmdb.isLoaded || recState.recommendedForYou.isNotEmpty()
        )

        HomeUiState(
            featuredChannel = featuredChan,
            featuredMedia = tmdb.featuredMedia,
            billboardEpg = epg,
            preferredLanguage = preferredLang,
            trendingMovies = tmdb.trendingMovies,
            trendingTv = tmdb.trendingTv,
            languageRecommendedMedia = tmdb.languageRecommendedMedia,
            dynamicRecommendedForYou = recState.recommendedForYou,
            becauseYouWatchedTitle = recState.becauseYouWatchedTitle,
            becauseYouWatchedMedia = recState.becauseYouWatchedMedia,
            smartRecommendedChannels = recState.recommendedChannels,
            recommendationReason = recState.recommendationReason,
            recommendedLanguageChannels = recommendedChan,
            top10Channels = top10,
            newsChannels = news,
            moviesChannels = movies,
            musicChannels = music,
            kidsChannels = kids,
            recentlyWatched = recent,
            recentChannels = recent.distinctBy { it.id }.take(10),
            continueWatching = continueWatchingItems,
            categories = listOf("All", "Movies", "Series", "Live TV", "News"),
            selectedFilter = filter,
            favoriteChannelIds = favIds,
            tonightPicks = tonight,
            isTmdbLoaded = tmdb.isLoaded && (tmdb.trendingMovies.isNotEmpty() || tmdb.trendingTv.isNotEmpty()),
            isLoading = !tmdb.isLoaded || (tmdb.trendingMovies.isEmpty() && tmdb.trendingTv.isEmpty() && all.isEmpty())
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        viewModelScope.launch {
            channelRepository.ensureSeeded()
            // Load billboard EPG
            channelRepository.getAllChannels().collect { list ->
                val hero = list.firstOrNull()
                if (hero != null) {
                    val info = epgManager.getProgrammeForChannel(
                        channelId = hero.id,
                        channelName = hero.cleanName.ifEmpty { hero.name },
                        category = hero.category
                    )
                    _billboardEpg.value = info
                }
            }
        }

        viewModelScope.launch {
            userPreferences.preferredLanguage.collect { lang ->
                loadTmdbContent(lang)
            }
        }
    }

    private suspend fun loadTmdbContent(preferredLang: String) {
        try {
            val movies = tmdbRepository.getTrending(TmdbMediaType.MOVIE, "week")
            val tv = tmdbRepository.getTrending(TmdbMediaType.TV, "week")
            val langCode = LanguageUtils.toIso6391(preferredLang)

            val langMedia = try {
                val recMovies = tmdbRepository.discoverByLanguage(TmdbMediaType.MOVIE, langCode, 1)
                val recTv = tmdbRepository.discoverByLanguage(TmdbMediaType.TV, langCode, 1)
                (recMovies.take(10) + recTv.take(10)).shuffled()
            } catch (_: Exception) {
                emptyList()
            }

            val hero = movies.firstOrNull() ?: tv.firstOrNull()
            _tmdbState.value = TmdbHomeState(
                featuredMedia = hero,
                trendingMovies = movies.take(15),
                trendingTv = tv.take(15),
                languageRecommendedMedia = langMedia,
                isLoaded = true
            )
        } catch (_: Exception) {
            _tmdbState.value = _tmdbState.value.copy(isLoaded = true)
        }
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun recordPickViewed(pick: CandidatePick) {
        val numericId = pick.tmdbId ?: pick.id.substringAfterLast('_').toLongOrNull() ?: 0L
        val isTv = pick.mediaType.equals("tv", ignoreCase = true)
        viewModelScope.launch {
            recommendationEngine.recordMediaInteraction(
                media = TmdbMediaItem(
                    id = numericId,
                    title = pick.title,
                    overview = "",
                    posterPath = pick.posterUrl,
                    backdropPath = null,
                    releaseYear = null,
                    voteAverage = 0.0,
                    mediaType = if (isTv) TmdbMediaType.TV else TmdbMediaType.MOVIE
                ),
                interactionType = "VIEW"
            )
        }
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channelId = channelId)
        }
    }

    fun removeFromContinueWatching(downloadId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeId = profileRepository?.activeProfileId?.value ?: "default"
            try {
                playbackProgressDao?.setHidden(activeId, downloadId, true)
            } catch (_: Exception) {}
            try {
                downloadDao.setHideFromContinueWatching(downloadId, true)
            } catch (_: Exception) {}
        }
    }

    companion object {
        fun provideFactory(
            repository: ChannelRepository,
            epgManager: EpgManager,
            userPreferences: UserPreferences,
            tmdbRepository: TmdbRepository,
            recommendationEngine: IntelligentRecommendationEngine,
            downloadDao: DownloadDao,
            playbackProgressDao: PlaybackProgressDao? = null,
            profileRepository: ProfileRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    repository,
                    epgManager,
                    userPreferences,
                    tmdbRepository,
                    recommendationEngine,
                    downloadDao,
                    playbackProgressDao,
                    profileRepository
                ) as T
            }
        }
    }
}
