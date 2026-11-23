package com.nis.nistv.data.parental

import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ParentalControlManager(
    private val userPreferences: UserPreferences,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    companion object {
        const val UNLOCK_DURATION_MS = 15 * 60 * 1000L // 15 minutes

        /**
         * Pure predicate to determine if a channel should appear in a given list.
         * In favorites, locked channels are kept so that a lock badge can be shown.
         * In Live TV, Search, Home, and Recommendations, locked channels are hidden.
         */
        fun isChannelVisible(
            channel: ChannelEntity,
            isParentalEnabled: Boolean,
            isUnlocked: Boolean,
            lockedCategories: Set<String>,
            isFavoriteContext: Boolean = false
        ): Boolean {
            if (!isParentalEnabled || isUnlocked) return true
            val isRestricted = channel.isNsfw || lockedCategories.any { it.equals(channel.category, ignoreCase = true) }
            return if (isFavoriteContext) {
                true // Retained in favorites with a lock badge
            } else {
                !isRestricted
            }
        }

        /**
         * Pure check for whether a channel requires PIN unlock.
         */
        fun isChannelRestricted(
            channel: ChannelEntity,
            isParentalEnabled: Boolean,
            isUnlocked: Boolean,
            lockedCategories: Set<String>
        ): Boolean {
            if (!isParentalEnabled || isUnlocked) return false
            return channel.isNsfw || lockedCategories.any { it.equals(channel.category, ignoreCase = true) }
        }
    }

    private val _isTemporarilyUnlocked = MutableStateFlow(false)
    val isTemporarilyUnlocked: StateFlow<Boolean> = _isTemporarilyUnlocked.asStateFlow()

    private var autoLockJob: Job? = null

    fun unlockAdultContent() {
        _isTemporarilyUnlocked.value = true
        autoLockJob?.cancel()
        autoLockJob = scope.launch {
            delay(UNLOCK_DURATION_MS)
            _isTemporarilyUnlocked.value = false
        }
    }

    fun lockAdultContent() {
        autoLockJob?.cancel()
        autoLockJob = null
        _isTemporarilyUnlocked.value = false
    }

    fun onAppBackgrounded() {
        lockAdultContent()
    }
}
