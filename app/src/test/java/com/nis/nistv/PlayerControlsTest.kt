package com.nis.nistv

import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.playback.SleepTimerStateMachine
import com.nis.nistv.core.playback.SleepTimerTickResult
import com.nis.nistv.feature.player.controls.clampSeekPosition
import com.nis.nistv.feature.player.controls.resolveNextEpisode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerControlsTest {

    private fun createEpisode(
        id: String,
        seriesId: String,
        season: Int,
        episode: Int,
        isCompleted: Boolean = true
    ): DownloadEntity {
        return DownloadEntity(
            id = id,
            mediaId = id,
            title = "Episode $season-$episode",
            mediaType = "tv",
            seriesId = seriesId,
            seasonNumber = season,
            episodeNumber = episode,
            downloadUrl = "http://example.com/$id.mp4",
            fileName = "$id.mp4",
            filePath = "/downloads/$id.mp4",
            fileSizeBytes = 1000L,
            downloadedBytes = 1000L,
            status = if (isCompleted) "COMPLETED" else "DOWNLOADING"
        )
    }

    @Test
    fun nextEpisodeResolver_resolvesNextEpisodeInSameSeason() {
        val s1e1 = createEpisode("ep1", "series_1", 1, 1)
        val s1e2 = createEpisode("ep2", "series_1", 1, 2)
        val s1e3 = createEpisode("ep3", "series_1", 1, 3)

        val next = resolveNextEpisode(s1e1, listOf(s1e1, s1e2, s1e3))
        assertNotNull(next)
        assertEquals("ep2", next!!.id)
    }

    @Test
    fun nextEpisodeResolver_resolvesFirstEpisodeOfNextSeason_whenSeasonFinished() {
        val s1e8 = createEpisode("s1_8", "series_1", 1, 8)
        val s2e1 = createEpisode("s2_1", "series_1", 2, 1)
        val s2e2 = createEpisode("s2_2", "series_1", 2, 2)

        val next = resolveNextEpisode(s1e8, listOf(s1e8, s2e1, s2e2))
        assertNotNull(next)
        assertEquals("s2_1", next!!.id)
    }

    @Test
    fun nextEpisodeResolver_returnsNull_forMoviesOrLastEpisode() {
        val movie = DownloadEntity(
            id = "mov1",
            mediaId = "mov1",
            title = "Inception",
            mediaType = "movie",
            downloadUrl = "http://example.com/inc.mp4",
            fileName = "inc.mp4",
            filePath = "/downloads/inc.mp4"
        )
        assertNull(resolveNextEpisode(movie, emptyList()))

        val lastEp = createEpisode("ep_last", "series_1", 3, 10)
        assertNull(resolveNextEpisode(lastEp, listOf(lastEp)))
    }

    @Test
    fun seekClamp_properlyClampsToZeroAndDuration() {
        val duration = 120_000L

        // Seek backwards beyond 0
        val clampedBack = clampSeekPosition(5_000L, -10_000L, duration)
        assertEquals(0L, clampedBack)

        // Normal seek forward
        val clampedMid = clampSeekPosition(30_000L, 10_000L, duration)
        assertEquals(40_000L, clampedMid)

        // Seek forward beyond duration
        val clampedForward = clampSeekPosition(115_000L, 10_000L, duration)
        assertEquals(120_000L, clampedForward)
    }

    @Test
    fun sleepTimerStateMachine_tracksCountdownAndExpires() {
        val sm = SleepTimerStateMachine()
        val now = 100_000L

        sm.start(minutes = 15, now = now)
        assertTrue(sm.isRunning)

        // Tick midway (5 minutes in)
        val tickMid = sm.tick(now = now + 300_000L)
        assertTrue(tickMid is SleepTimerTickResult.Active)
        assertEquals(600L, (tickMid as SleepTimerTickResult.Active).remainingSeconds)

        // Tick on expiry
        val tickExp = sm.tick(now = now + 900_000L)
        assertTrue(tickExp is SleepTimerTickResult.Expired)
        assertTrue(!sm.isRunning)
    }

    @Test
    fun sleepTimerStateMachine_handlesEndOfEpisode() {
        val sm = SleepTimerStateMachine()
        sm.startEndOfEpisode()
        assertTrue(sm.isRunning)

        // Normal tick does not expire
        val tick1 = sm.tick(System.currentTimeMillis(), endOfEpisodeReached = false)
        assertTrue(tick1 is SleepTimerTickResult.Active)

        // When end of episode reached
        val tick2 = sm.tick(System.currentTimeMillis(), endOfEpisodeReached = true)
        assertTrue(tick2 is SleepTimerTickResult.Expired)
        assertTrue(!sm.isRunning)
    }
}
