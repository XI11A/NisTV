package com.nis.nistv.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nis.nistv.core.designsystem.theme.NisAccentPrimary
import com.nis.nistv.core.designsystem.theme.NisBorderSubtle
import com.nis.nistv.core.designsystem.theme.NisMotion
import com.nis.nistv.core.designsystem.theme.isReducedMotionEnabled

@Composable
fun NisLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            color = NisAccentPrimary,
            trackColor = NisBorderSubtle,
            strokeWidth = 3.dp
        )
    }
}

/**
 * Animated linear progress bar with NisMotion.progressSpec and reduced-motion fallback.
 * Uses lambda-based progress provider to run in draw phase without per-frame recomposition.
 */
@Composable
fun NisLinearProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = NisAccentPrimary,
    trackColor: Color = NisBorderSubtle,
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
    val animatedProgress = animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (reducedMotion) snap() else NisMotion.progressSpec(),
        label = "nisLinearProgress"
    )

    LinearProgressIndicator(
        progress = { animatedProgress.value },
        modifier = modifier,
        color = color,
        trackColor = trackColor
    )
}

/**
 * Lambda-based overload of [NisLinearProgressBar] for deferred progress evaluation.
 */
@Composable
fun NisLinearProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = NisAccentPrimary,
    trackColor: Color = NisBorderSubtle,
) {
    NisLinearProgressBar(
        progress = progress(),
        modifier = modifier,
        color = color,
        trackColor = trackColor
    )
}
