package com.nis.nistv.core.security

import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.data.tmdb.model.TmdbMediaItem

object KidsContentFilter {

    /**
     * Kids profile help text for UI disclaimer.
     */
    const val KIDS_HELP_TEXT =
        "Kids mode provides best-effort protection by filtering channels and media to family-friendly content. It is not an official certification guarantee."

    /**
     * Canonical single constant defining kid-friendly Live TV category names.
     */
    val ALLOWED_CHANNEL_CATEGORIES: Set<String> = setOf(
        "Animation",
        "Education",
        "Family",
        "Kids"
    )

    /**
     * TMDB genre IDs for Animation (16), Family (10751), Kids (10762).
     */
    val ALLOWED_TMDB_GENRE_IDS: Set<Int> = setOf(16, 10751, 10762)

    /**
     * Pure predicate to test if a Live TV channel is permissible for a kids profile.
     */
    fun isChannelAllowedForKids(channel: ChannelEntity): Boolean {
        if (channel.isNsfw) return false
        val cat = channel.category.trim()
        return ALLOWED_CHANNEL_CATEGORIES.any { it.equals(cat, ignoreCase = true) }
    }

    fun isGenreAllowedForKids(genreId: Int): Boolean = genreId in ALLOWED_TMDB_GENRE_IDS

    /**
     * Pure predicate to test if a TMDB media item is permissible for a kids profile.
     */
    fun isMediaAllowedForKids(item: TmdbMediaItem): Boolean {
        if (item.genreIds.isEmpty()) return false
        return item.genreIds.any { it in ALLOWED_TMDB_GENRE_IDS }
    }

    /**
     * Pure predicate for comma-separated genre string.
     */
    fun areGenresAllowedForKids(genresString: String?): Boolean {
        if (genresString.isNullOrBlank()) return false
        val parsedIds = genresString.split(",", " ", "|")
            .mapNotNull { it.trim().toIntOrNull() }
        return parsedIds.any { it in ALLOWED_TMDB_GENRE_IDS }
    }

    /**
     * Filters a list of channels respecting kids mode and optional parental locks.
     */
    fun filterChannels(
        channels: List<ChannelEntity>,
        isKidsProfile: Boolean,
        isParentalEnabled: Boolean = false,
        isUnlocked: Boolean = false,
        lockedCategories: Set<String> = emptySet()
    ): List<ChannelEntity> {
        return channels.filter { channel ->
            if (isKidsProfile) {
                isChannelAllowedForKids(channel)
            } else if (isParentalEnabled && !isUnlocked) {
                !channel.isNsfw && channel.category !in lockedCategories
            } else {
                true
            }
        }
    }
}
