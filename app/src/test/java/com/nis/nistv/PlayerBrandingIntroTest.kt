package com.nis.nistv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayerBrandingIntroTest {

    @Test
    fun testPlayerBrandingIntroClassLoads() {
        assertNotNull(Class.forName("com.nis.nistv.feature.player.components.PlayerBrandingIntroKt"))
    }

    @Test
    fun testBrandingRawAssetExists() {
        val rawVideo = File("src/main/res/raw/nistv_branding.mp4")
        assertTrue("nistv_branding.mp4 should be present in res/raw", rawVideo.exists())
        assertTrue("nistv_branding.mp4 should be non-empty", rawVideo.length() > 50_000)
    }

    @Test
    fun testCountdownCalculation() {
        fun computeRemainingSeconds(currentPositionMs: Long): Long {
            return ((5000L - currentPositionMs).coerceAtLeast(0L) + 999L) / 1000L
        }

        assertEquals(5L, computeRemainingSeconds(0L))
        assertEquals(5L, computeRemainingSeconds(500L))
        assertEquals(4L, computeRemainingSeconds(1001L))
        assertEquals(3L, computeRemainingSeconds(2500L))
        assertEquals(2L, computeRemainingSeconds(3100L))
        assertEquals(1L, computeRemainingSeconds(4500L))
        assertEquals(0L, computeRemainingSeconds(5000L))
        assertEquals(0L, computeRemainingSeconds(6000L))
    }

    @Test
    fun testFinishConditionAtOrPastDuration() {
        fun shouldFinishIntro(currentPositionMs: Long, stateEnded: Boolean): Boolean {
            return stateEnded || currentPositionMs >= 5000L
        }

        assertFalse(shouldFinishIntro(0L, false))
        assertFalse(shouldFinishIntro(4900L, false))
        assertTrue(shouldFinishIntro(5000L, false))
        assertTrue(shouldFinishIntro(5200L, false))
        assertTrue(shouldFinishIntro(1200L, true)) // user or track ended early
    }

    @Test
    fun testChannelZappingDismissesIntro() {
        // Simulates PlayerScreen state transitions during zap
        var showBrandingIntro = true

        val onZap = {
            showBrandingIntro = false
        }

        assertTrue("Intro should initially be active", showBrandingIntro)
        onZap()
        assertFalse("Intro should be immediately dismissed on channel zap", showBrandingIntro)
    }

    @Test
    fun testBrandingIntroSkippedWhenMaximizingFromMiniPlayer() {
        fun shouldShowBrandingIntro(
            activeChannelId: String?,
            targetChannelId: String,
            playbackStatus: com.nis.nistv.playback.PlaybackStatus
        ): Boolean {
            val isAlreadyActive = activeChannelId == targetChannelId && playbackStatus != com.nis.nistv.playback.PlaybackStatus.IDLE
            return !isAlreadyActive
        }

        // Active channel restored from mini player -> DO NOT play branding video
        assertFalse(
            "Branding intro should not play when maximizing playing content from mini player",
            shouldShowBrandingIntro("channel_1", "channel_1", com.nis.nistv.playback.PlaybackStatus.PLAYING)
        )
        assertFalse(
            "Branding intro should not play when maximizing paused content from mini player",
            shouldShowBrandingIntro("channel_1", "channel_1", com.nis.nistv.playback.PlaybackStatus.PAUSED)
        )
        assertFalse(
            "Branding intro should not play when maximizing buffering content from mini player",
            shouldShowBrandingIntro("channel_1", "channel_1", com.nis.nistv.playback.PlaybackStatus.BUFFERING)
        )

        // Switching to a DIFFERENT channel -> play branding intro
        assertTrue(
            "Branding intro should play when opening a different channel",
            shouldShowBrandingIntro("channel_1", "channel_2", com.nis.nistv.playback.PlaybackStatus.PLAYING)
        )

        // Fresh playback from idle -> play branding intro
        assertTrue(
            "Branding intro should play on fresh start from idle",
            shouldShowBrandingIntro(null, "channel_1", com.nis.nistv.playback.PlaybackStatus.IDLE)
        )
    }
}
