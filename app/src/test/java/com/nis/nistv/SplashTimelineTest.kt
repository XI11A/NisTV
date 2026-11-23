package com.nis.nistv

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import com.nis.nistv.feature.splash.SplashTimeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashTimelineTest {

    private val allSegments = listOf(
        SplashTimeline.Background,
        SplashTimeline.Squircle,
        SplashTimeline.Border,
        SplashTimeline.Glow,
        SplashTimeline.PillarL,
        SplashTimeline.PillarR,
        SplashTimeline.Ribbon,
        SplashTimeline.Burst,
        SplashTimeline.Sheen,
        SplashTimeline.Wordmark,
        SplashTimeline.Tagline
    )

    @Test
    fun testProgressBeforeStartIsZero() {
        allSegments.forEach { seg ->
            assertEquals(0f, SplashTimeline.progress(seg.start - 50f, seg), 0.0001f)
            assertEquals(0f, SplashTimeline.progress(seg.start.toFloat(), seg), 0.0001f)
            assertEquals(0f, SplashTimeline.progress(-100f, seg), 0.0001f)
        }
    }

    @Test
    fun testProgressAfterEndIsOne() {
        allSegments.forEach { seg ->
            assertEquals(1f, SplashTimeline.progress(seg.end.toFloat(), seg), 0.0001f)
            assertEquals(1f, SplashTimeline.progress(seg.end + 50f, seg), 0.0001f)
            assertEquals(1f, SplashTimeline.progress(10_000f, seg), 0.0001f)
        }
    }

    @Test
    fun testProgressMonotonicity() {
        val testEasings = listOf(
            LinearEasing,
            SplashTimeline.EaseOutQuint,
            SplashTimeline.EaseInOutCubic,
            FastOutSlowInEasing
        )

        allSegments.forEach { seg ->
            testEasings.forEach { easing ->
                var lastVal = -1f
                for (step in 0..100) {
                    val t = seg.start + (seg.end - seg.start) * (step / 100f)
                    val progress = SplashTimeline.progress(t, seg, easing)
                    assertTrue("Progress should be >= previous ($progress vs $lastVal)", progress >= lastVal - 0.0001f)
                    assertTrue("Progress should be in 0..1 ($progress)", progress in -0.0001f..1.0001f)
                    lastVal = progress
                }
            }
        }
    }

    @Test
    fun testEasingEndpoints() {
        val testEasings = listOf(
            LinearEasing,
            SplashTimeline.EaseOutQuint,
            SplashTimeline.EaseInOutCubic,
            FastOutSlowInEasing
        )

        testEasings.forEach { easing ->
            assertEquals(0f, easing.transform(0f), 0.0001f)
            assertEquals(1f, easing.transform(1f), 0.0001f)
        }
    }

    @Test
    fun testReadinessRemainingWaitCalculations() {
        // 1. Warm start: boot ready quickly (500ms elapsed), must wait for min display (1900ms)
        val waitWarm = SplashTimeline.computeRemainingWait(
            elapsedMs = 500L,
            isBootReady = true,
            minDisplayMs = 1900L,
            hardTimeoutMs = 4000L
        )
        assertEquals(1400L, waitWarm)

        // 2. Warm start: min display already surpassed (2000ms elapsed), should exit immediately
        val waitReadyImmediate = SplashTimeline.computeRemainingWait(
            elapsedMs = 2000L,
            isBootReady = true,
            minDisplayMs = 1900L,
            hardTimeoutMs = 4000L
        )
        assertEquals(0L, waitReadyImmediate)

        // 3. Cold start slow: boot NOT ready at min display (1900ms), wait until 4000ms hard timeout
        val waitColdSlow = SplashTimeline.computeRemainingWait(
            elapsedMs = 1900L,
            isBootReady = false,
            minDisplayMs = 1900L,
            hardTimeoutMs = 4000L
        )
        assertEquals(2100L, waitColdSlow)

        // 4. Hard timeout reached (4000ms elapsed), must force exit immediately even if boot not ready
        val waitHardTimeout = SplashTimeline.computeRemainingWait(
            elapsedMs = 4000L,
            isBootReady = false,
            minDisplayMs = 1900L,
            hardTimeoutMs = 4000L
        )
        assertEquals(0L, waitHardTimeout)

        // 5. Hard timeout exceeded (4500ms elapsed), returns 0
        val waitExceeded = SplashTimeline.computeRemainingWait(
            elapsedMs = 4500L,
            isBootReady = false,
            minDisplayMs = 1900L,
            hardTimeoutMs = 4000L
        )
        assertEquals(0L, waitExceeded)

        // 6. Reduced motion path: minDisplayMs is 0L, boot is ready, should exit immediately
        val waitReducedMotion = SplashTimeline.computeRemainingWait(
            elapsedMs = 100L,
            isBootReady = true,
            minDisplayMs = 0L,
            hardTimeoutMs = 4000L
        )
        assertEquals(0L, waitReducedMotion)
    }

    @Test
    fun testReducedMotionBuildEndState() {
        // At BUILD_END_MS, all build segments are at 100% completion
        allSegments.forEach { seg ->
            assertEquals(1f, SplashTimeline.progress(SplashTimeline.BUILD_END_MS.toFloat(), seg), 0.0001f)
        }
    }
}
