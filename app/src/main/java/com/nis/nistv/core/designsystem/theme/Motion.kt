package com.nis.nistv.core.designsystem.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

// =============================================================================
// NisTV Revamped — Cinematic Obsidian 2.0 Motion System
// =============================================================================

/**
 * Checks system settings to determine if accessibility reduced-motion is requested.
 * When ANIMATOR_DURATION_SCALE is 0, animations should snap or fall back to short fades.
 */
fun isReducedMotionEnabled(context: Context): Boolean {
    return try {
        val durationScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        durationScale == 0f
    } catch (_: Exception) {
        false
    }
}

object NisMotion {
    // Duration Tokens (milliseconds)
    const val DurationFastMs = 120    // Micro-interactions, ripples, badge toggles
    const val DurationNormalMs = 220  // Card press, tab indicators, dialog entrances
    const val DurationSlowMs = 360    // Large sheet expansions, crossfades

    // Easing Curves
    val EasingEmphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EasingStandard: Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    // Spring Specifications for tactile press feedback
    const val PressDampingRatio = 0.7f
    const val PressStiffness = 800f

    /**
     * Reusable spring specification for button and card press scaling.
     */
    fun <T> pressSpring(): SpringSpec<T> = spring(
        dampingRatio = PressDampingRatio,
        stiffness = PressStiffness
    )

    /**
     * Spring specification for list item reordering and placement.
     */
    fun <T> itemPlacementSpring(): SpringSpec<T> = spring(
        dampingRatio = 0.8f,
        stiffness = 400f
    )

    /**
     * Standard animation spec for progress indicators.
     */
    fun <T> progressSpec(): FiniteAnimationSpec<T> = tween(
        durationMillis = DurationNormalMs,
        easing = EasingStandard
    )

    /**
     * Standard animation spec for tab indicators, chips, and color transitions.
     */
    fun <T> tabIndicatorSpec(): FiniteAnimationSpec<T> = tween(
        durationMillis = DurationNormalMs,
        easing = EasingStandard
    )

    /**
     * Dialog enter transition: Subtle scale + fade in normal mode; fade-only in reduced motion.
     */
    fun dialogEnterTransition(reducedMotion: Boolean): EnterTransition =
        if (reducedMotion) {
            fadeIn(tween(DurationFastMs, easing = EasingStandard))
        } else {
            fadeIn(tween(DurationNormalMs, easing = EasingStandard)) +
                scaleIn(tween(DurationNormalMs, easing = EasingEmphasized), initialScale = 0.94f)
        }

    /**
     * Dialog exit transition: Subtle scale + fade out in normal mode; fade-only in reduced motion.
     */
    fun dialogExitTransition(reducedMotion: Boolean): ExitTransition =
        if (reducedMotion) {
            fadeOut(tween(DurationFastMs, easing = EasingStandard))
        } else {
            fadeOut(tween(DurationFastMs, easing = EasingStandard)) +
                scaleOut(tween(DurationFastMs, easing = EasingStandard), targetScale = 0.94f)
        }

    /**
     * Bottom sheet enter transition: Smooth slide up + fade in; fade-only in reduced motion.
     */
    fun sheetEnterTransition(reducedMotion: Boolean): EnterTransition =
        if (reducedMotion) {
            fadeIn(tween(DurationFastMs, easing = EasingStandard))
        } else {
            slideInVertically(
                animationSpec = tween(DurationSlowMs, easing = EasingEmphasized),
                initialOffsetY = { it }
            ) + fadeIn(tween(DurationNormalMs, easing = EasingStandard))
        }

    /**
     * Bottom sheet exit transition: Smooth slide down + fade out; fade-only in reduced motion.
     */
    fun sheetExitTransition(reducedMotion: Boolean): ExitTransition =
        if (reducedMotion) {
            fadeOut(tween(DurationFastMs, easing = EasingStandard))
        } else {
            slideOutVertically(
                animationSpec = tween(DurationNormalMs, easing = EasingEmphasized),
                targetOffsetY = { it }
            ) + fadeOut(tween(DurationFastMs, easing = EasingStandard))
        }
}
