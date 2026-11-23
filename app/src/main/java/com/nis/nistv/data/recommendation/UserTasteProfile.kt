package com.nis.nistv.data.recommendation

import com.nis.nistv.core.database.entity.MediaInteractionEntity

/**
 * High-dimensional user taste representation learned on-device from
 * implicit and explicit watch, download, and favorite signals.
 */
data class UserTasteProfile(
    val genreWeights: Map<Int, Double> = emptyMap(),
    val categoryWeights: Map<String, Double> = emptyMap(),
    val languageWeights: Map<String, Double> = emptyMap(),
    val recentWatchedMediaIds: Set<String> = emptySet(),
    val recentInteractions: List<MediaInteractionEntity> = emptyList(),
    val mostRecentWatch: MediaInteractionEntity? = null,
    val totalInteractions: Int = 0
) {
    val isColdStart: Boolean
        get() = totalInteractions == 0

    val topGenreId: Int?
        get() = genreWeights.maxByOrNull { it.value }?.key

    val topCategory: String?
        get() = categoryWeights.maxByOrNull { it.value }?.key

    val topLanguage: String?
        get() = languageWeights.maxByOrNull { it.value }?.key

    fun getGenreAffinity(genreId: Int): Double {
        return genreWeights[genreId] ?: 0.0
    }

    fun getCategoryAffinity(category: String): Double {
        return categoryWeights[category.lowercase().trim()] ?: 0.0
    }

    fun getLanguageAffinity(language: String): Double {
        return languageWeights[language.lowercase().trim()] ?: 0.0
    }
}
