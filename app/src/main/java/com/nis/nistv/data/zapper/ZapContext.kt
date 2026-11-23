package com.nis.nistv.data.zapper

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory origin context holder for Live TV Channel Zapping.
 * Preserves the channel list, current index, origin title, and previous channel history
 * without polluting Jetpack Compose navigation arguments.
 */
class ZapContext {

    private val _contextTitle = MutableStateFlow("All Channels")
    val contextTitle: StateFlow<String> = _contextTitle.asStateFlow()

    private val _channelIds = MutableStateFlow<List<String>>(emptyList())
    val channelIds: StateFlow<List<String>> = _channelIds.asStateFlow()

    private val _currentChannelId = MutableStateFlow<String?>(null)
    val currentChannelId: StateFlow<String?> = _currentChannelId.asStateFlow()

    private val _previousChannelId = MutableStateFlow<String?>(null)
    val previousChannelId: StateFlow<String?> = _previousChannelId.asStateFlow()

    fun setContext(title: String, channels: List<String>, currentId: String) {
        val trimmed = channels.distinct()
        _contextTitle.value = title.ifBlank { "Live Channels" }
        _channelIds.value = trimmed
        if (_currentChannelId.value != null && _currentChannelId.value != currentId) {
            _previousChannelId.value = _currentChannelId.value
        }
        _currentChannelId.value = currentId
    }

    fun updateCurrentChannel(channelId: String) {
        val prev = _currentChannelId.value
        if (prev != null && prev != channelId) {
            _previousChannelId.value = prev
        }
        _currentChannelId.value = channelId
    }

    /**
     * Resolves the next channel in the current context, wrapping around at list boundaries
     * and skipping any parental locked channels.
     */
    fun getNextChannelId(lockedChannelIds: Set<String> = emptySet()): String? {
        val list = _channelIds.value
        if (list.isEmpty()) return null

        val currentId = _currentChannelId.value
        val currentIndex = list.indexOf(currentId).takeIf { it >= 0 } ?: 0

        for (offset in 1..list.size) {
            val candidateIndex = (currentIndex + offset) % list.size
            val candidateId = list[candidateIndex]
            if (candidateId !in lockedChannelIds) {
                return candidateId
            }
        }
        return null // All channels locked or empty
    }

    /**
     * Resolves the previous channel in the current context, wrapping around at list boundaries
     * and skipping any parental locked channels.
     */
    fun getPreviousChannelId(lockedChannelIds: Set<String> = emptySet()): String? {
        val list = _channelIds.value
        if (list.isEmpty()) return null

        val currentId = _currentChannelId.value
        val currentIndex = list.indexOf(currentId).takeIf { it >= 0 } ?: 0

        for (offset in 1..list.size) {
            val candidateIndex = (currentIndex - offset + list.size * offset) % list.size
            val candidateId = list[candidateIndex]
            if (candidateId !in lockedChannelIds) {
                return candidateId
            }
        }
        return null
    }

    /**
     * Toggles to the previously watched channel if valid and not locked.
     */
    fun getLastChannelId(lockedChannelIds: Set<String> = emptySet()): String? {
        val prev = _previousChannelId.value ?: return null
        return if (prev !in lockedChannelIds) prev else null
    }

    /**
     * Resolves channel by 1-based index (e.g. keying "1" -> first channel).
     */
    fun getChannelByIndex(oneBasedIndex: Int, lockedChannelIds: Set<String> = emptySet()): String? {
        val list = _channelIds.value
        if (list.isEmpty() || oneBasedIndex < 1 || oneBasedIndex > list.size) return null

        val candidateId = list[oneBasedIndex - 1]
        return if (candidateId !in lockedChannelIds) candidateId else null
    }

    fun getCurrentChannelNumber(): Int {
        val currentId = _currentChannelId.value ?: return 0
        val idx = _channelIds.value.indexOf(currentId)
        return if (idx >= 0) idx + 1 else 1
    }

    fun getTotalChannelsCount(): Int = _channelIds.value.size
    fun getTotalChannels(): Int = _channelIds.value.size
    fun getContextTitle(): String = _contextTitle.value
}
