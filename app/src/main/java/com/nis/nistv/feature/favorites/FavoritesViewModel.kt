package com.nis.nistv.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.WatchLaterEntity
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.data.parental.ParentalControlManager
import com.nis.nistv.data.repository.ChannelRepository
import com.nis.nistv.data.repository.WatchLaterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FavoritesTab {
    MOVIES_AND_SHOWS,
    LIVE_CHANNELS
}

class FavoritesViewModel(
    private val channelRepository: ChannelRepository,
    private val watchLaterRepository: WatchLaterRepository? = null,
    private val userPreferences: UserPreferences? = null,
    private val parentalControlManager: ParentalControlManager? = null
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(FavoritesTab.MOVIES_AND_SHOWS)
    val selectedTab: StateFlow<FavoritesTab> = _selectedTab.asStateFlow()

    val favorites: StateFlow<List<ChannelEntity>> = channelRepository.getFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val watchLaterList: StateFlow<List<WatchLaterEntity>> = (watchLaterRepository?.getWatchLaterList() ?: emptyFlow())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val lockedChannelIds: StateFlow<Set<String>> = if (userPreferences != null && parentalControlManager != null) {
        combine(
            channelRepository.getFavorites(),
            userPreferences.isParentalControlEnabled,
            parentalControlManager.isTemporarilyUnlocked,
            userPreferences.parentalLockedCategories
        ) { favs, parentalEnabled, unlocked, lockedCats ->
            if (!parentalEnabled || unlocked) {
                emptySet()
            } else {
                favs.filter { ch ->
                    ParentalControlManager.isChannelRestricted(ch, parentalEnabled, unlocked, lockedCats)
                }.map { it.id }.toSet()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    } else {
        MutableStateFlow(emptySet())
    }

    suspend fun verifyParentalPin(pin: String): Boolean {
        val storedHash = userPreferences?.parentalPinHash?.firstOrNull() ?: ""
        val storedSalt = userPreferences?.parentalPinSalt?.firstOrNull() ?: ""
        val isValid = com.nis.nistv.core.security.ParentalPinHelper.verifyPin(pin, storedHash, storedSalt)
        if (isValid) {
            parentalControlManager?.unlockAdultContent()
        }
        return isValid
    }

    fun selectTab(tab: FavoritesTab) {
        _selectedTab.value = tab
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channelId = channelId)
        }
    }

    fun removeFromWatchLater(mediaType: String, mediaId: Long) {
        viewModelScope.launch {
            watchLaterRepository?.removeFromWatchLater(mediaType = mediaType, mediaId = mediaId)
        }
    }

    companion object {
        fun provideFactory(
            channelRepository: ChannelRepository,
            watchLaterRepository: WatchLaterRepository? = null,
            userPreferences: UserPreferences? = null,
            parentalControlManager: ParentalControlManager? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FavoritesViewModel(channelRepository, watchLaterRepository, userPreferences, parentalControlManager) as T
                }
            }
    }
}
