package com.nis.nistv.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.data.recommendation.DynamicRecommendationState
import com.nis.nistv.data.recommendation.IntelligentRecommendationEngine
import com.nis.nistv.data.tmdb.model.*
import com.nis.nistv.data.tmdb.repository.TmdbRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val selectedMediaType: TmdbMediaType = TmdbMediaType.MOVIE,
    val selectedGenreId: Int? = null,
    val genres: List<TmdbGenre> = emptyList(),
    val heroItem: TmdbMediaItem? = null,
    val preferredLanguage: String = "Hindi",
    val recommendedForYou: List<TmdbMediaItem> = emptyList(),
    val recommendationReason: String? = null,
    val becauseYouWatchedTitle: String? = null,
    val becauseYouWatchedMedia: List<TmdbMediaItem> = emptyList(),
    val trendingInPreferredLanguage: List<TmdbMediaItem> = emptyList(),
    val trendingRail: List<TmdbMediaItem> = emptyList(),
    val popularRail: List<TmdbMediaItem> = emptyList(),
    val topRatedRail: List<TmdbMediaItem> = emptyList(),
    val nowPlayingOrAiringRail: List<TmdbMediaItem> = emptyList(),
    val gridItems: List<TmdbMediaItem> = emptyList(),
    val currentPage: Int = 1,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val searchResults: List<TmdbMediaItem> = emptyList(),
    val isSearching: Boolean = false
)

class DiscoverViewModel(
    private val repository: TmdbRepository,
    private val recommendationEngine: IntelligentRecommendationEngine,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        // Collect user's preferred language and reload language recommendations
        viewModelScope.launch {
            userPreferences.preferredLanguage.collect { lang ->
                _uiState.update { it.copy(preferredLanguage = lang) }
                loadDiscoverData()
            }
        }

        // Collect intelligent recommendations reactively
        viewModelScope.launch {
            recommendationEngine.recommendationState.collect { recState ->
                updateRecommendationsInUi(recState)
            }
        }
    }

    private fun updateRecommendationsInUi(recState: DynamicRecommendationState) {
        val type = _uiState.value.selectedMediaType
        val filteredRecs = recState.recommendedForYou.filter { it.mediaType == type }
            .ifEmpty { recState.recommendedForYou }
        val filteredBecause = recState.becauseYouWatchedMedia.filter { it.mediaType == type }
            .ifEmpty { recState.becauseYouWatchedMedia }

        _uiState.update { current ->
            val updatedHero = filteredRecs.firstOrNull() ?: current.heroItem
            current.copy(
                recommendedForYou = filteredRecs,
                recommendationReason = recState.recommendationReason,
                becauseYouWatchedTitle = recState.becauseYouWatchedTitle,
                becauseYouWatchedMedia = filteredBecause,
                heroItem = updatedHero
            )
        }
    }

    fun setMediaType(mediaType: TmdbMediaType) {
        if (_uiState.value.selectedMediaType == mediaType) return
        _uiState.update {
            it.copy(
                selectedMediaType = mediaType,
                selectedGenreId = null,
                currentPage = 1,
                gridItems = emptyList(),
                isLoading = true,
                errorMessage = null
            )
        }
        loadDiscoverData()
    }

    fun selectGenre(genreId: Int?) {
        _uiState.update {
            it.copy(
                selectedGenreId = if (it.selectedGenreId == genreId) null else genreId,
                currentPage = 1,
                gridItems = emptyList()
            )
        }
        loadGenreGrid(page = 1)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoading) return
        val nextPage = state.currentPage + 1
        loadGenreGrid(page = nextPage)
    }

    fun searchMedia(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            try {
                val results = repository.search(_uiState.value.selectedMediaType, query.trim(), 1)
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadDiscoverData(forceRefresh = true)
    }

    private fun loadDiscoverData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val type = _uiState.value.selectedMediaType
            val preferredLang = _uiState.value.preferredLanguage
            val langCode = LanguageUtils.toIso6391(preferredLang)

            try {
                val genres = repository.getGenres(type)
                val trending = repository.getTrending(type, "week", forceRefresh)
                val popular = repository.getPopular(type, 1, forceRefresh)
                val topRated = repository.getTopRated(type, 1, forceRefresh)
                val nowPlaying = repository.getNowPlayingOrAiring(type, 1, forceRefresh)

                // Trending in User's Preferred Language
                val langTrending = try {
                    repository.discoverByLanguage(type, langCode, 1, forceRefresh)
                } catch (_: Exception) {
                    emptyList()
                }

                val recState = recommendationEngine.recommendationState.value
                val filteredRecs = recState.recommendedForYou.filter { it.mediaType == type }
                    .ifEmpty { recState.recommendedForYou }
                val filteredBecause = recState.becauseYouWatchedMedia.filter { it.mediaType == type }
                    .ifEmpty { recState.becauseYouWatchedMedia }

                val hero = filteredRecs.firstOrNull() ?: langTrending.firstOrNull() ?: trending.firstOrNull() ?: popular.firstOrNull()

                _uiState.update {
                    it.copy(
                        genres = genres,
                        heroItem = hero,
                        recommendedForYou = filteredRecs,
                        recommendationReason = recState.recommendationReason,
                        becauseYouWatchedTitle = recState.becauseYouWatchedTitle,
                        becauseYouWatchedMedia = filteredBecause,
                        trendingInPreferredLanguage = langTrending,
                        trendingRail = trending,
                        popularRail = popular,
                        topRatedRail = topRated,
                        nowPlayingOrAiringRail = nowPlaying,
                        isLoading = false,
                        errorMessage = null
                    )
                }

                loadGenreGrid(page = 1)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load TMDB content. Check your network or API key."
                    )
                }
            }
        }
    }

    private fun loadGenreGrid(page: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            if (page > 1) {
                _uiState.update { it.copy(isLoadingMore = true) }
            }
            try {
                val items = repository.discoverByGenre(
                    type = state.selectedMediaType,
                    genreId = state.selectedGenreId,
                    page = page
                )

                _uiState.update { current ->
                    val combined = if (page == 1) items else (current.gridItems + items).distinctBy { it.id }
                    current.copy(
                        gridItems = combined,
                        currentPage = page,
                        isLoadingMore = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: TmdbRepository,
            recommendationEngine: IntelligentRecommendationEngine,
            userPreferences: UserPreferences
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DiscoverViewModel(repository, recommendationEngine, userPreferences) as T
                }
            }
    }
}
