package com.nis.nistv.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nis.nistv.core.designsystem.theme.NisAccentCyan
import com.nis.nistv.core.designsystem.theme.NisBadgeShape
import com.nis.nistv.core.designsystem.theme.NisChipShape
import com.nis.nistv.core.designsystem.theme.NisDimens
import com.nis.nistv.core.designsystem.theme.NisMotion
import com.nis.nistv.core.designsystem.theme.NisTheme
import com.nis.nistv.core.designsystem.theme.isReducedMotionEnabled

/**
 * Standard content badge with 6dp corner radius and unified 6hx2v padding.
 */
@Composable
fun NisBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NisTheme.colors.surfaceElevated,
    textColor: Color = NisTheme.colors.textPrimary
) {
    Box(
        modifier = modifier
            .clip(NisBadgeShape)
            .background(backgroundColor)
            .border(1.dp, textColor.copy(alpha = 0.3f), NisBadgeShape)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            style = NisTheme.textExtras.micro
        )
    }
}

/**
 * Cinematic LIVE badge:
 * - Solid red background with crisp white typography
 * - Small pulsing dot (reduced-motion safe: static when animator duration scale is 0)
 * - 6dp corner radius and unified 6hx2v padding
 */
@Composable
fun NisLiveBadge(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }

    val transition = rememberInfiniteTransition(label = "liveDotPulse")
    val dotAlphaState = if (!reducedMotion) {
        transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = NisMotion.EasingStandard),
                repeatMode = RepeatMode.Reverse
            ),
            label = "liveDotAlpha"
        )
    } else {
        remember { mutableFloatStateOf(1.0f) }
    }

    val liveColor = NisTheme.colors.live

    Row(
        modifier = modifier
            .clip(NisBadgeShape)
            .background(liveColor)
            .border(1.dp, liveColor.copy(alpha = 0.8f), NisBadgeShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .graphicsLayer { alpha = dotAlphaState.value }
                .background(Color.White, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "LIVE",
            color = Color.White,
            style = NisTheme.textExtras.micro
        )
    }
}

/**
 * HD / Stream Resolution Badge:
 * - Cyan text on cyan 12% fill with subtle border
 * - 6dp corner radius and unified 6hx2v padding
 */
@Composable
fun NisResolutionBadge(
    resolution: String,
    modifier: Modifier = Modifier
) {
    val cyan = NisTheme.colors.accentCyan
    Box(
        modifier = modifier
            .clip(NisBadgeShape)
            .background(cyan.copy(alpha = 0.12f))
            .border(1.dp, cyan.copy(alpha = 0.35f), NisBadgeShape)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = resolution.uppercase(),
            color = cyan,
            style = NisTheme.textExtras.micro
        )
    }
}

/**
 * Warning / Offline Status Badge:
 * - Amber text on amber 14% fill
 * - 6dp corner radius and unified 6hx2v padding
 */
@Composable
fun NisWarningBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    val amber = NisTheme.colors.warning
    Box(
        modifier = modifier
            .clip(NisBadgeShape)
            .background(amber.copy(alpha = 0.14f))
            .border(1.dp, amber.copy(alpha = 0.40f), NisBadgeShape)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = amber,
            style = NisTheme.textExtras.micro
        )
    }
}

/**
 * Category & Filter Chip (Frosted Glassmorphism):
 * - Full 999dp pill radius (NisChipShape)
 * - Selected: Glowing periwinkle/cyan glass gradient + specular top border + bold text
 * - Unselected: Translucent frosted glass + subtle specular border + textSecondary
 * - Guaranteed 48dp minimum touch target
 * - Tactile press feedback via nisPressable
 */
@Composable
fun NisCategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
    val colorSpec = if (reducedMotion) {
        snap<Color>()
    } else {
        NisMotion.tabIndicatorSpec<Color>()
    }
    val colors = NisTheme.colors

    val selectedBgBrush = Brush.horizontalGradient(
        listOf(
            colors.accentCyan.copy(alpha = 0.28f),
            colors.primary.copy(alpha = 0.40f)
        )
    )
    val unselectedBgBrush = Brush.verticalGradient(
        listOf(
            colors.surfaceElevated.copy(alpha = 0.55f),
            colors.surface.copy(alpha = 0.45f)
        )
    )
    val selectedBorderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.60f),
            colors.primary
        )
    )
    val unselectedBorderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.22f),
            colors.borderSubtle.copy(alpha = 0.35f)
        )
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else colors.textSecondary,
        animationSpec = colorSpec,
        label = "chipContentColor"
    )

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = NisDimens.TouchTarget)
            .nisPressable(
                onClick = onClick,
                role = Role.Tab,
                onClickLabel = text,
                shape = NisChipShape
            )
            .border(
                width = if (isSelected) 1.2.dp else 1.dp,
                brush = if (isSelected) selectedBorderBrush else unselectedBorderBrush,
                shape = NisChipShape
            )
            .background(
                brush = if (isSelected) selectedBgBrush else unselectedBgBrush,
                shape = NisChipShape
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
