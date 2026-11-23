package com.nis.nistv

import com.nis.nistv.data.recommendation.CandidatePick
import com.nis.nistv.data.recommendation.TimeContext
import com.nis.nistv.data.recommendation.TonightPicksHelper
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class TonightPicksTest {

    private val sampleCandidates = listOf(
        CandidatePick(
            id = "movie_short",
            title = "Quick Short Film",
            mediaType = "movie",
            runtimeMinutes = 40,
            affinityScore = 1.0,
            isDownloaded = true,
            isWatched = false
        ),
        CandidatePick(
            id = "movie_epic",
            title = "Epic Blockbuster",
            mediaType = "movie",
            runtimeMinutes = 150,
            affinityScore = 1.2,
            isDownloaded = false,
            isWatched = false
        ),
        CandidatePick(
            id = "tv_show",
            title = "Comedy Series Ep",
            mediaType = "tv",
            runtimeMinutes = 25,
            affinityScore = 1.0,
            isDownloaded = true,
            isWatched = false
        ),
        CandidatePick(
            id = "watched_item",
            title = "Already Watched Movie",
            mediaType = "movie",
            runtimeMinutes = 100,
            affinityScore = 2.0,
            isDownloaded = true,
            isWatched = true
        )
    )

    @Test
    fun testExcludesWatchedItems() {
        val context = TimeContext(hourOfDay = 20, isWeekend = false)
        val ranked = TonightPicksHelper.rankTonightPicks(sampleCandidates, context, isOnline = true)

        assertFalse(ranked.any { it.isWatched })
        assertFalse(ranked.any { it.id == "watched_item" })
    }

    @Test
    fun testOfflineRestrictsToDownloadedOnly() {
        val context = TimeContext(hourOfDay = 20, isWeekend = false)
        val offlineRanked = TonightPicksHelper.rankTonightPicks(sampleCandidates, context, isOnline = false)

        assertTrue(offlineRanked.all { it.isDownloaded })
        assertFalse(offlineRanked.any { it.id == "movie_epic" })
    }

    @Test
    fun testLateNightPrefersShorterContent() {
        // Hour 23 (Late Night)
        val lateNightContext = TimeContext(hourOfDay = 23, isWeekend = false)
        val ranked = TonightPicksHelper.rankTonightPicks(sampleCandidates, lateNightContext, isOnline = true)

        // Shorter items should score higher than the 150 min epic movie
        assertTrue(ranked.isNotEmpty())
        assertEquals("tv_show", ranked.first().id)
    }

    @Test
    fun testWeekendPrefersFeatureLengthMovies() {
        // Saturday Evening
        val weekendContext = TimeContext(hourOfDay = 19, isWeekend = true)
        val ranked = TonightPicksHelper.rankTonightPicks(sampleCandidates, weekendContext, isOnline = true)

        // Feature movie with high affinity should be first
        assertTrue(ranked.isNotEmpty())
        assertEquals("movie_epic", ranked.first().id)
    }

    @Test
    fun testPickSurpriseItemDeterministicWithSeed() {
        val picks = sampleCandidates.filter { !it.isWatched }
        val random1 = Random(42)
        val random2 = Random(42)

        val pick1 = TonightPicksHelper.pickSurpriseItem(picks, random1)
        val pick2 = TonightPicksHelper.pickSurpriseItem(picks, random2)

        assertNotNull(pick1)
        assertNotNull(pick2)
        assertEquals(pick1?.id, pick2?.id)
    }

    @Test
    fun testPickSurpriseItemEmptyReturnsNull() {
        val picked = TonightPicksHelper.pickSurpriseItem(emptyList())
        assertNull(picked)
    }
}
