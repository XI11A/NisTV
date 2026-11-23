package com.nis.nistv

import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.DownloadStatus
import com.nis.nistv.feature.home.buildContinueWatching
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinueWatchingTest {

    private fun createDownload(
        id: String,
        title: String,
        mediaType: String = "movie",
        seriesId: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        lastPlaybackPositionMs: Long = 0L,
        durationMs: Long = 0L,
        lastWatchedAt: Long = 0L,
        watchedCompleted: Boolean = false,
        hideFromContinueWatching: Boolean = false,
        status: String = DownloadStatus.COMPLETED.name
    ): DownloadEntity {
        return DownloadEntity(
            id = id,
            mediaId = id,
            title = title,
            mediaType = mediaType,
            downloadUrl = "http://example.com/$id.mp4",
            fileName = "$id.mp4",
            filePath = "/downloads/$id.mp4",
            fileSizeBytes = 1000L,
            downloadedBytes = 1000L,
            status = status,
            seriesId = seriesId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            lastPlaybackPositionMs = lastPlaybackPositionMs,
            durationMs = durationMs,
            lastWatchedAt = lastWatchedAt,
            watchedCompleted = watchedCompleted,
            hideFromContinueWatching = hideFromContinueWatching
        )
    }

    @Test
    fun legacyRowsWithDurationZero_areOmitted() {
        val legacyMovie = createDownload(
            id = "legacy_1",
            title = "Old Download",
            durationMs = 0L,
            lastPlaybackPositionMs = 120_000L
        )
        val result = buildContinueWatching(listOf(legacyMovie))
        assertTrue("Legacy download with duration = 0 should be omitted", result.isEmpty())
    }

    @Test
    fun thresholds_ignoresBelow5Percent_andAbove95Percent() {
        val duration = 100_000L
        val below5Percent = createDownload(
            id = "below_5",
            title = "Movie Below 5%",
            durationMs = duration,
            lastPlaybackPositionMs = 4_000L // 4%
        )
        val above95Percent = createDownload(
            id = "above_95",
            title = "Movie Above 95%",
            durationMs = duration,
            lastPlaybackPositionMs = 96_000L // 96%
        )
        val validProgress = createDownload(
            id = "valid_mid",
            title = "Valid In-Progress Movie",
            durationMs = duration,
            lastPlaybackPositionMs = 50_000L // 50%
        )

        val result = buildContinueWatching(listOf(below5Percent, above95Percent, validProgress))
        assertEquals(1, result.size)
        assertEquals("valid_mid", result.first().download.id)
        assertEquals(0.5f, result.first().progressFraction, 0.001f)
    }

    @Test
    fun hiddenItems_areOmittedFromContinueWatching() {
        val hiddenMovie = createDownload(
            id = "hidden_1",
            title = "Hidden Movie",
            durationMs = 100_000L,
            lastPlaybackPositionMs = 50_000L,
            hideFromContinueWatching = true
        )
        val result = buildContinueWatching(listOf(hiddenMovie))
        assertTrue("Hidden downloads must not appear in Continue Watching", result.isEmpty())
    }

    @Test
    fun seriesCollapsing_showsOnlyOneCardPerSeries() {
        val duration = 2_000_000L
        val ep1 = createDownload(
            id = "ep_1",
            title = "Game of Thrones S01E01",
            mediaType = "tv",
            seriesId = "tv_got",
            seasonNumber = 1,
            episodeNumber = 1,
            durationMs = duration,
            lastPlaybackPositionMs = 500_000L,
            lastWatchedAt = 1000L
        )
        val ep2 = createDownload(
            id = "ep_2",
            title = "Game of Thrones S01E02",
            mediaType = "tv",
            seriesId = "tv_got",
            seasonNumber = 1,
            episodeNumber = 2,
            durationMs = duration,
            lastPlaybackPositionMs = 800_000L,
            lastWatchedAt = 2000L // watched more recently
        )

        val result = buildContinueWatching(listOf(ep1, ep2))
        assertEquals("Only one card per series should be displayed", 1, result.size)
        assertEquals("ep_2", result.first().download.id)
        assertEquals("S1 E2", result.first().subtitleText)
    }

    @Test
    fun nextEpisodePickAcrossSeasons_picksS2E1_whenS1E10Finished() {
        val duration = 2_000_000L
        val s1e10 = createDownload(
            id = "s1_e10",
            title = "Stranger Things S01E10",
            mediaType = "tv",
            seriesId = "tv_st",
            seasonNumber = 1,
            episodeNumber = 10,
            durationMs = duration,
            lastPlaybackPositionMs = 1_950_000L, // >= 95% -> completed
            watchedCompleted = true,
            lastWatchedAt = 5000L
        )
        val s2e1 = createDownload(
            id = "s2_e1",
            title = "Stranger Things S02E01",
            mediaType = "tv",
            seriesId = "tv_st",
            seasonNumber = 2,
            episodeNumber = 1,
            durationMs = duration,
            lastPlaybackPositionMs = 0L,
            watchedCompleted = false,
            lastWatchedAt = 0L
        )

        val result = buildContinueWatching(listOf(s1e10, s2e1))
        assertEquals(1, result.size)
        assertEquals("s2_e1", result.first().download.id)
        assertEquals("S2 E1", result.first().subtitleText)
    }
}
