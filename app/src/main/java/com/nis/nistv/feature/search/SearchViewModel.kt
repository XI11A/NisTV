package com.nis.nistv.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.data.parental.ParentalControlManager
import com.nis.nistv.data.repository.ChannelRepository
import com.nis.nistv.data.tmdb.model.TmdbMediaItem
import com.nis.nistv.data.tmdb.model.TmdbMediaType
import com.nis.nistv.data.tmdb.repository.TmdbRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val channelResults: List<ChannelEntity> = emptyList(),
    val movieResults: List<TmdbMediaItem> = emptyList(),
    val tvResults: List<TmdbMediaItem> = emptyList(),
    val selectedFilterTab: String = "All", // "All", "Channels", "Movies", "Series"
    val recentSearches: List<String> = emptyList(),
    val favoriteChannelIds: Set<String> = emptySet(),
    val isSearching: Boolean = false,
    val isLoadingTmdb: Boolean = false
) {
    val totalCount: Int
        get() = channelResults.size + movieResults.size + tvResults.size
}

private data class SearchTmdbResult(
    val movies: List<TmdbMediaItem> = emptyList(),
    val tv: List<TmdbMediaItem> = emptyList(),
    val isLoading: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val channelRepository: ChannelRepository,
    private val tmdbRepository: TmdbRepository,
    private val userPreferences: UserPreferences? = null,
    private val parentalControlManager: ParentalControlManager? = null
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedFilterTab = MutableStateFlow("All")
    val selectedFilterTab: StateFlow<String> = _selectedFilterTab.asStateFlow()

    private val _tmdbResult = MutableStateFlow(SearchTmdbResult())

    private val channelResultsFlow = _query
        .debounce(250L)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) {
                flowOf(emptyList())
            } else {
                channelRepository.searchChannels(q.trim())
            }
        }

    init {
        // Debounced search for TMDB movies and TV series
        viewModelScope.launch {
            _query
                .debounce(350L)
                .distinctUntilChanged()
                .collect { q ->
                    if (q.isBlank()) {
                        _tmdbResult.value = SearchTmdbResult()
                    } else {
                        _tmdbResult.value = _tmdbResult.value.copy(isLoading = true)
                        try {
                            val movies = tmdbRepository.search(TmdbMediaType.MOVIE, q.trim())
                            val tv = tmdbRepository.search(TmdbMediaType.TV, q.trim())
                            _tmdbResult.value = SearchTmdbResult(
                                movies = movies,
                                tv = tv,
                                isLoading = false
                            )
                        } catch (_: Exception) {
                            _tmdbResult.value = SearchTmdbResult(isLoading = false)
                        }
                    }
                }
        }
    }

    private val contentPairFlow = combine(channelResultsFlow, _tmdbResult) { channels, tmdb ->
        Pair(channels, tmdb)
    }

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        _selectedFilterTab,
        contentPairFlow,
        channelRepository.getRecentSearches(),
        channelRepository.getFavorites().map { list -> list.map { it.id }.toSet() }
    ) { q, tab, (channels, tmdb), recents, favIds ->
        val visibleChannels = if (userPreferences != null && parentalControlManager != null) {
            val parentalEnabled = userPreferences.isParentalControlEnabled.firstOrNull() ?: false
            val unlocked = parentalControlManager.isTemporarilyUnlocked.value
            val lockedCats = userPreferences.parentalLockedCategories.firstOrNull() ?: emptySet()
            channels.filter { ch ->
                ParentalControlManager.isChannelVisible(ch, parentalEnabled, unlocked, lockedCats, false)
            }
        } else {
            channels
        }
        SearchUiState(
            query = q,
            channelResults = visibleChannels,
            movieResults = tmdb.movies,
            tvResults = tmdb.tv,
            selectedFilterTab = tab,
            recentSearches = recents,
            favoriteChannelIds = favIds,
            isSearching = q.isNotBlank(),
            isLoadingTmdb = tmdb.isLoading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun setFilterTab(tab: String) {
        _selectedFilterTab.value = tab
    }

    fun submitSearch(query: String) {
        if (query.isNotBlank()) {
            viewModelScope.launch {
                channelRepository.addRecentSearch(query = query.trim())
            }
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            channelRepository.clearRecentSearches()
        }
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channelId = channelId)
        }
    }

    companion object {
        fun provideFactory(
            repository: ChannelRepository,
            tmdbRepository: TmdbRepository,
            userPreferences: UserPreferences? = null,
            parentalControlManager: ParentalControlManager? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SearchViewModel(repository, tmdbRepository, userPreferences, parentalControlManager) as T
                }
            }
    }
}
