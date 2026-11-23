package com.nis.nistv.data.recommendation

import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.data.tmdb.model.TmdbMediaItem

/**
 * State representing dynamic recommendations that update automatically
 * whenever the user watches content across NisTV.
 */
data class DynamicRecommendationState(
    val recommendedForYou: List<TmdbMediaItem> = emptyList(),
    val becauseYouWatchedTitle: String? = null,
    val becauseYouWatchedMedia: List<TmdbMediaItem> = emptyList(),
    val recommendedChannels: List<ChannelEntity> = emptyList(),
    val tasteProfile: UserTasteProfile = UserTasteProfile(),
    val recommendationReason: String? = null,
    val isLoading: Boolean = false
)
