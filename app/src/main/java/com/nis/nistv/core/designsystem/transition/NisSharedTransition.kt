@file:OptIn(ExperimentalSharedTransitionApi::class)
package com.nis.nistv.core.designsystem.transition

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

enum class SharedRole { CONTAINER, IMAGE, TITLE }

object SharedKeys {
    /** e.g. movie_image_550_home_trending */
    fun of(mediaType: String, tmdbId: Int, role: SharedRole, origin: String): String =
        "${mediaType.lowercase()}_${role.name.lowercase()}_${tmdbId}_${origin.ifBlank { "default" }}"
}

object SharedOrigins {
    fun homeRow(rowId: String) = "home_$rowId"
    fun discoverRow(rowId: String) = "discover_$rowId"
    const val SIMILAR = "similar"
    const val SEARCH = "search"
    const val WATCH_LATER = "watchlater"
}

/** Spring = interruptible and velocity-preserving (gesture reversal never snaps). */
val NisBoundsTransform = BoundsTransform { _, _ ->
    spring<Rect>(
        dampingRatio = 0.88f,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = Rect.VisibilityThreshold,
    )
}

@Composable
fun Modifier.nisSharedBounds(
    key: String,
    resizeMode: SharedTransitionScope.ResizeMode =
        SharedTransitionScope.ResizeMode.ScaleToBounds(ContentScale.Crop, Alignment.Center),
    clipShape: Shape? = null,
    zIndex: Float = 0f,
    enter: EnterTransition = fadeIn(tween(220)),
    exit: ExitTransition = fadeOut(tween(220)),
): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this      // previews/tests: no-op
    val visibilityScope = LocalNavAnimatedVisibilityScope.current ?: return this
    return with(sharedScope) {
        if (clipShape != null) {
            this@nisSharedBounds.sharedBounds(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = visibilityScope,
                enter = enter,
                exit = exit,
                boundsTransform = NisBoundsTransform,
                resizeMode = resizeMode,
                renderInOverlayDuringTransition = true,
                zIndexInOverlay = zIndex,
                clipInOverlayDuringTransition = OverlayClip(clipShape),
            )
        } else {
            this@nisSharedBounds.sharedBounds(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = visibilityScope,
                enter = enter,
                exit = exit,
                boundsTransform = NisBoundsTransform,
                resizeMode = resizeMode,
                renderInOverlayDuringTransition = true,
                zIndexInOverlay = zIndex,
            )
        }
    }
}

/** Non-shared Details content: soft fade + small slide, slightly delayed. */
@Composable
fun Modifier.nisContentEnterExit(): Modifier {
    val scope = LocalNavAnimatedVisibilityScope.current ?: return this
    return with(scope) {
        this@nisContentEnterExit.animateEnterExit(
            enter = fadeIn(tween(300, delayMillis = 120)) +
                slideInVertically(tween(300, delayMillis = 120)) { it / 14 },
            exit = fadeOut(tween(120)),
        )
    }
}

/** Blocks double taps / taps while a transition is running (lifecycle not RESUMED). */
@Composable
fun Modifier.debouncedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role
    ) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) onClick()
    }
}

@Composable
fun Modifier.nisRenderInOverlay(zIndex: Float = 4f): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    return with(sharedScope) {
        this@nisRenderInOverlay.renderInSharedTransitionScopeOverlay(zIndexInOverlay = zIndex)
    }
}
