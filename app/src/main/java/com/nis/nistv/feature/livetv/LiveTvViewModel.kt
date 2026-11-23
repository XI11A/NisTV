package com.nis.nistv.feature.livetv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nis.nistv.core.database.dao.PlaylistDao
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.PlaylistEntity
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.data.parental.ParentalControlManager
import com.nis.nistv.data.repository.ChannelRepository
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

data class LiveTvFilter(
    val category: String? = null,
    val language: String? = null,
    val country: String? = null,
    val isMyPlaylists: Boolean = false,
    val playlistId: String? = null
)

data class LiveTvUiState(
    val channels: List<ChannelEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val selectedCategory: String? = null,
    val selectedLanguage: String? = null,
    val selectedCountry: String? = null,
    val isMyPlaylistsSelected: Boolean = false,
    val selectedPlaylistId: String? = null,
    val favoriteChannelIds: Set<String> = emptySet(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class LiveTvViewModel(
    private val channelRepository: ChannelRepository,
    private val playlistDao: PlaylistDao,
    private val userPreferences: UserPreferences,
    private val parentalControlManager: ParentalControlManager
) : ViewModel() {

    private val _filter = MutableStateFlow(LiveTvFilter())
    val filter: StateFlow<LiveTvFilter> = _filter.asStateFlow()

    private val baseChannelsFlow = _filter.flatMapLatest { f ->
        when {
            f.isMyPlaylists && f.playlistId != null -> channelRepository.getChannelsByPlaylist(f.playlistId)
            f.isMyPlaylists -> channelRepository.getAllCustomChannels()
            f.category != null -> channelRepository.getChannelsByCategory(f.category)
            f.language != null -> channelRepository.getChannelsByLanguage(f.language)
            f.country != null -> channelRepository.getChannelsByCountry(f.country)
            else -> channelRepository.getAllChannels()
        }
    }

    private val metaFlow = combine(
        channelRepository.getCategories(),
        channelRepository.getLanguages(),
        channelRepository.getCountries(),
        playlistDao.getAllPlaylists()
    ) { cats, langs, countries, playlists ->
        object {
            val categories = cats
            val languages = langs
            val countries = countries
            val playlists = playlists
        }
    }

    private val favoritesFlow = channelRepository.getFavorites().map { list ->
        list.map { it.id }.toSet()
    }

    private val parentalFlow = combine(
        userPreferences.isParentalControlEnabled,
        parentalControlManager.isTemporarilyUnlocked,
        userPreferences.parentalLockedCategories
    ) { enabled, unlocked, lockedCats ->
        Triple(enabled, unlocked, lockedCats)
    }

    val uiState: StateFlow<LiveTvUiState> = combine(
        baseChannelsFlow,
        metaFlow,
        favoritesFlow,
        _filter,
        parentalFlow
    ) { channels, meta, favIds, currentFilter, parentalTriple ->
        val (parentalEnabled, unlocked, lockedCats) = parentalTriple
        val visibleChannels = channels.filter { ch ->
            ParentalControlManager.isChannelVisible(
                channel = ch,
                isParentalEnabled = parentalEnabled,
                isUnlocked = unlocked,
                lockedCategories = lockedCats,
                isFavoriteContext = false
            )
        }
        LiveTvUiState(
            channels = visibleChannels,
            categories = meta.categories.filter { cat ->
                !parentalEnabled || unlocked || !lockedCats.contains(cat)
            },
            languages = meta.languages,
            countries = meta.countries,
            playlists = meta.playlists,
            selectedCategory = currentFilter.category,
            selectedLanguage = currentFilter.language,
            selectedCountry = currentFilter.country,
            isMyPlaylistsSelected = currentFilter.isMyPlaylists,
            selectedPlaylistId = currentFilter.playlistId,
            favoriteChannelIds = favIds,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LiveTvUiState()
    )

    fun selectMyPlaylists(playlistId: String? = null) {
        if (_filter.value.isMyPlaylists && _filter.value.playlistId == playlistId) {
            _filter.value = LiveTvFilter()
        } else {
            _filter.value = LiveTvFilter(
                isMyPlaylists = true,
                playlistId = playlistId
            )
        }
    }

    fun selectCategory(category: String?) {
        _filter.value = LiveTvFilter(
            category = if (_filter.value.category == category) null else category,
            language = null,
            country = null,
            isMyPlaylists = false,
            playlistId = null
        )
    }

    fun selectLanguage(language: String?) {
        _filter.value = LiveTvFilter(
            category = null,
            language = if (_filter.value.language == language) null else language,
            country = null,
            isMyPlaylists = false,
            playlistId = null
        )
    }

    fun selectCountry(country: String?) {
        _filter.value = LiveTvFilter(
            category = null,
            language = null,
            country = if (_filter.value.country == country) null else country,
            isMyPlaylists = false,
            playlistId = null
        )
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channelId = channelId)
        }
    }

    companion object {
        fun provideFactory(
            repository: ChannelRepository,
            playlistDao: PlaylistDao,
            userPreferences: UserPreferences,
            parentalControlManager: ParentalControlManager
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LiveTvViewModel(repository, playlistDao, userPreferences, parentalControlManager) as T
                }
            }
    }
}
