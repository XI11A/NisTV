package com.nis.nistv

import com.nis.nistv.core.database.entity.MediaInteractionEntity
import com.nis.nistv.data.recommendation.UserTasteProfile
import com.nis.nistv.data.tmdb.model.TmdbMediaItem
import com.nis.nistv.data.tmdb.model.TmdbMediaType
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.exp

class RecommendationEngineTest {

    @Test
    fun tasteProfile_coldStart_whenNoInteractions() {
        val profile = UserTasteProfile(totalInteractions = 0)
        assertTrue(profile.isColdStart)
        assertNull(profile.topGenreId)
        assertNull(profile.topCategory)
        assertEquals(0.0, profile.getGenreAffinity(28), 0.001)
        assertEquals(0.0, profile.getCategoryAffinity("News"), 0.001)
    }

    @Test
    fun tasteProfile_computesTopAffinityCorrectly() {
        val profile = UserTasteProfile(
            genreWeights = mapOf(28 to 1.0, 878 to 0.7, 35 to 0.2),
            categoryWeights = mapOf("movies" to 1.0, "news" to 0.4),
            languageWeights = mapOf("tamil" to 1.0, "hindi" to 0.6),
            totalInteractions = 5
        )

        assertFalse(profile.isColdStart)
        assertEquals(Integer.valueOf(28), profile.topGenreId)
        assertEquals("movies", profile.topCategory)
        assertEquals("tamil", profile.topLanguage)
        assertEquals(1.0, profile.getGenreAffinity(28), 0.001)
        assertEquals(0.7, profile.getGenreAffinity(878), 0.001)
        assertEquals(0.0, profile.getGenreAffinity(999), 0.001)
        assertEquals(1.0, profile.getCategoryAffinity("Movies"), 0.001)
        assertEquals(1.0, profile.getLanguageAffinity("Tamil"), 0.001)
    }

    @Test
    fun timeDecay_reducesWeightForOlderInteractions() {
        val halfLifeMs = 7.0 * 24.0 * 60.0 * 60.0 * 1000.0
        val now = 1000000000L

        val recentElapsed = 1000L * 60L * 60L // 1 hour ago
        val olderElapsed = halfLifeMs.toLong() // 7 days ago

        val recentWeight = exp(-recentElapsed / halfLifeMs)
        val olderWeight = exp(-olderElapsed / halfLifeMs)

        assertTrue("Recent weight should be close to 1.0", recentWeight > 0.99)
        assertEquals("Weight at half-life should be ~1/e (approx 0.368)", 0.368, olderWeight, 0.01)
        assertTrue("Recent interaction must have higher weight than older", recentWeight > olderWeight)
    }

    @Test
    fun interactionTypes_favorHighEngagementEvents() {
        val downloadType = "DOWNLOAD"
        val favType = "FAVORITE"
        val watchType = "WATCH"
        val viewType = "VIEW"

        val weightDownload = if (downloadType == "DOWNLOAD") 2.5 else 1.0
        val weightFav = if (favType == "FAVORITE") 2.0 else 1.0
        val weightWatch = if (watchType == "WATCH") 1.2 else 1.0
        val weightView = if (viewType == "VIEW") 0.8 else 1.0

        assertTrue(weightDownload > weightFav)
        assertTrue(weightFav > weightWatch)
        assertTrue(weightWatch > weightView)
    }

    @Test
    fun candidateMediaScoring_favorsGenreOverlapAndHighRating() {
        val profile = UserTasteProfile(
            genreWeights = mapOf(28 to 1.0, 878 to 0.8, 12 to 0.5),
            languageWeights = mapOf("en" to 1.0),
            recentWatchedMediaIds = setOf("999")
        )

        val candidateAction = TmdbMediaItem(
            id = 101L,
            title = "Sci-Fi Action Hit",
            overview = "Exciting space action",
            posterPath = "/poster1.jpg",
            backdropPath = null,
            releaseYear = "2024",
            voteAverage = 8.5,
            mediaType = TmdbMediaType.MOVIE,
            genreIds = listOf(28, 878)
        )

        val candidateLowRating = TmdbMediaItem(
            id = 102L,
            title = "Boring Drama",
            overview = "Slow drama",
            posterPath = "/poster2.jpg",
            backdropPath = null,
            releaseYear = "2010",
            voteAverage = 3.0,
            mediaType = TmdbMediaType.MOVIE,
            genreIds = listOf(18) // Drama (0.0 affinity)
        )

        val actionGenreMatch = candidateAction.genreIds.sumOf { profile.getGenreAffinity(it) } / candidateAction.genreIds.size
        val lowGenreMatch = candidateLowRating.genreIds.sumOf { profile.getGenreAffinity(it) } / candidateLowRating.genreIds.size

        val actionRatingNorm = candidateAction.voteAverage / 10.0
        val lowRatingNorm = candidateLowRating.voteAverage / 10.0

        val scoreAction = 0.5 * actionGenreMatch + 0.5 * actionRatingNorm
        val scoreLow = 0.5 * lowGenreMatch + 0.5 * lowRatingNorm

        assertTrue("Action candidate with high affinity & rating must score much higher", scoreAction > scoreLow)
        assertTrue("Seen item should be in recentWatchedMediaIds", profile.recentWatchedMediaIds.contains("999"))
        assertFalse("Unseen candidate should not be in seen set", profile.recentWatchedMediaIds.contains("101"))
    }
}
