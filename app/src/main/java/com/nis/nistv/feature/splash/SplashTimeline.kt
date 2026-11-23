package com.nis.nistv.feature.splash

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing

/**
 * Centralized choreography and timing constants for the NisTV splash screen and animated logo.
 * All animation durations, timeline segments, and custom easings live here as the single source of truth.
 */
object SplashTimeline {
    data class Seg(val start: Int, val end: Int)

    const val BUILD_END_MS = 1900          // End of logo build-up (specular sheen completes)
    const val MIN_DISPLAY_MS = 1900L       // Minimum splash display duration before starting exit
    const val HARD_TIMEOUT_MS = 4000L      // Maximum splash wait before forcing exit
    const val EXIT_MS = 280                // Exit fade and scale duration

    val Background = Seg(0, 350)
    val Squircle   = Seg(0, 350)
    val Border     = Seg(100, 700)
    val Glow       = Seg(150, 650)
    val PillarL    = Seg(150, 600)
    val PillarR    = Seg(230, 680)
    val Ribbon     = Seg(500, 1050)
    const val PlayStartMs = 850
    val Burst      = Seg(850, 1300)
    const val BeaconStartMs = 1150
    val Sheen      = Seg(1300, 1900)
    val Wordmark   = Seg(900, 1350)
    val Tagline    = Seg(1100, 1550)

    val EaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

    /**
     * Calculates the normalized [0f..1f] eased progress of [seg] at time [tMs].
     * Guaranteed to return 0f before start and 1f after end.
     */
    fun progress(tMs: Float, seg: Seg, easing: Easing = LinearEasing): Float {
        if (seg.end <= seg.start) return if (tMs >= seg.start) 1f else 0f
        val fraction = ((tMs - seg.start) / (seg.end - seg.start).toFloat()).coerceIn(0f, 1f)
        return easing.transform(fraction)
    }

    /**
     * Pure function calculating the remaining wait duration in milliseconds before initiating
     * the splash exit transition.
     *
     * @param elapsedMs Time elapsed since splash start.
     * @param isBootReady Whether background boot tasks (seed, preferences) are fully ready.
     * @param minDisplayMs Minimum display threshold.
     * @param hardTimeoutMs Maximum allowed display duration before forcing exit.
     * @return Duration in milliseconds to delay before exiting (0L if ready immediately).
     */
    fun computeRemainingWait(
        elapsedMs: Long,
        isBootReady: Boolean,
        minDisplayMs: Long = MIN_DISPLAY_MS,
        hardTimeoutMs: Long = HARD_TIMEOUT_MS
    ): Long {
        if (elapsedMs >= hardTimeoutMs) return 0L
        if (!isBootReady) {
            // Must wait for boot readiness, up to the hard timeout bound
            return (hardTimeoutMs - elapsedMs).coerceAtLeast(0L)
        }
        val remainingForDisplay = minDisplayMs - elapsedMs
        return remainingForDisplay.coerceAtLeast(0L)
    }

    /**
     * Pure function to determine whether the startup mnemonic audio should be played.
     * Splash audio is configured as always-on media stream playback, playing regardless
     * of ringer mode (vibrate/silent) and active background media, only muting if disabled
     * or during active cellular/VoIP calls.
     *
     * @param settingEnabled User preference setting for startup sound.
     * @param ringerMode AudioManager ringer mode (media stream ignores ringer mode).
     * @param inCall Whether a cellular / VoIP phone call is currently active.
     * @param musicActive Whether another audio stream/player is already actively playing audio.
     */
    fun shouldPlayStartupSound(
        settingEnabled: Boolean = true,
        ringerMode: Int = android.media.AudioManager.RINGER_MODE_NORMAL,
        inCall: Boolean = false,
        musicActive: Boolean = false
    ): Boolean {
        if (!settingEnabled) return false
        if (inCall) return false
        return true
    }
}
