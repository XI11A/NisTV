package com.nis.nistv.core.designsystem.component

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nis.nistv.core.designsystem.theme.NisAccentPrimary
import com.nis.nistv.core.designsystem.theme.NisDimens
import com.nis.nistv.core.designsystem.theme.NisMotion
import com.nis.nistv.core.designsystem.theme.isReducedMotionEnabled

/**
 * Backward-compatible alias for isReducedMotionEnabled now housed in NisMotion.
 */
fun isReducedMotionEnabled(context: Context): Boolean = com.nis.nistv.core.designsystem.theme.isReducedMotionEnabled(context)

/**
 * Standard NisTV tactile interaction modifier:
 * - Bounded ripple tinted with NisAccentPrimary
 * - Tactile press-scaling to 0.97 using NisMotion.pressSpring()
 * - Guaranteed 48dp minimum touch target
 * - Reduced-motion safety (disables scaling when animator duration scale is 0)
 * - Optional lifecycle debouncing to prevent double navigation taps
 * - Optional haptic feedback on long-press
 * - Performance-optimized: scale value read inside graphicsLayer draw phase to avoid recomposition
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.nisPressable(
    role: Role? = null,
    onClickLabel: String? = null,
    shape: Shape? = null,
    enabled: Boolean = true,
    debounced: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }

    val scaleState = animateFloatAsState(
        targetValue = if (isPressed && enabled && !reducedMotion) 0.97f else 1.0f,
        animationSpec = NisMotion.pressSpring(),
        label = "nisPressableScale"
    )

    this
        .defaultMinSize(
            minWidth = NisDimens.TouchTarget,
            minHeight = NisDimens.TouchTarget
        )
        .graphicsLayer {
            scaleX = scaleState.value
            scaleY = scaleState.value
        }
        .then(if (shape != null) Modifier.clip(shape) else Modifier)
        .combinedClickable(
            interactionSource = interactionSource,
            indication = ripple(
                bounded = true,
                color = NisAccentPrimary
            ),
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = {
                if (!debounced || lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    onClick()
                }
            },
            onLongClick = onLongClick?.let { action ->
                {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    action()
                }
            }
        )
}
