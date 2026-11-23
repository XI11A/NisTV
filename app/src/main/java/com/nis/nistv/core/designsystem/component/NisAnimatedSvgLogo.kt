package com.nis.nistv.core.designsystem.component

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nis.nistv.core.designsystem.theme.NisAccentCyan
import com.nis.nistv.core.designsystem.theme.NisAccentPrimary
import com.nis.nistv.core.designsystem.theme.NisBackground
import com.nis.nistv.feature.splash.SplashTimeline
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Checks if the system has reduced-motion enabled (animator duration scale = 0).
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Choreographed NisTV Brand Emblem with deterministic, single-timeline motion.
 *
 * Sequence (0 to 1900 ms):
 * - 0..350 ms: Squircle base fades in and scales 0.94 -> 1.0.
 * - 100..700 ms: Border draws on progressively via [PathMeasure].
 * - 150..650 ms: Ambient center glow fades in.
 * - 150..600 ms: Left pillar rises from bottom.
 * - 230..680 ms: Right pillar rises (staggered).
 * - 500..1050 ms: Cyber neon diagonal ribbon wipes with glowing leading head.
 * - 850..1300 ms: Play core pops with spring (damping 0.65) and soft radial burst.
 * - 1150..1550 ms: Live ruby beacon flares and emits exactly 3 slow ping rings.
 * - 1300..1900 ms: Single specular sheen sweep diagonally across ribbon.
 * - 1500 ms..exit: Calm breathing pulse (0.985 <-> 1.015).
 */
@Composable
fun NisAnimatedSvgLogo(
    modifier: Modifier = Modifier,
    logoSize: Dp = 120.dp,
    reducedMotion: Boolean = rememberReducedMotion()
) {
    val timeline = remember { Animatable(if (reducedMotion) SplashTimeline.BUILD_END_MS.toFloat() else 0f) }
    val playScale = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val beacon = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val pingAnim = remember { Animatable(0f) }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) return@LaunchedEffect

        // 1. Master build timeline
        launch {
            timeline.animateTo(
                targetValue = SplashTimeline.BUILD_END_MS.toFloat(),
                animationSpec = tween(SplashTimeline.BUILD_END_MS, easing = LinearEasing)
            )
        }

        // 2. Play core spring pop
        launch {
            delay(SplashTimeline.PlayStartMs.toLong())
            playScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        // 3. Ruby beacon flare followed by exactly 3 finite ping rings
        launch {
            delay(SplashTimeline.BeaconStartMs.toLong())
            beacon.animateTo(1.6f, tween(220, easing = FastOutSlowInEasing))
            beacon.animateTo(1.0f, tween(300, easing = FastOutSlowInEasing))

            repeat(3) {
                pingAnim.snapTo(0f)
                pingAnim.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
            }
        }
    }

    // Post-build calm breathing pulse
    val breathing = rememberInfiniteTransition(label = "NisLogoBreathing").animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    Box(
        modifier = modifier.size(logoSize * 1.5f),
        contentAlignment = Alignment.Center
    ) {
        // A. Static Ambient Glow (alpha from timeline, NO rotation)
        Canvas(
            modifier = Modifier
                .size(logoSize * 1.35f)
                .graphicsLayer {
                    val t = timeline.value
                    alpha = SplashTimeline.progress(t, SplashTimeline.Glow, LinearEasing)
                }
        ) {
            val glowRadius = (size.width * 0.5f).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NisAccentCyan.copy(alpha = 0.28f),
                        NisAccentPrimary.copy(alpha = 0.22f),
                        Color(0xFFA855F7).copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )
        }

        // B. Emblem with Breathing graphicsLayer and draw-phase timeline inputs
        Box(
            modifier = Modifier
                .size(logoSize)
                .graphicsLayer {
                    val s = if (timeline.value >= 1500f && !reducedMotion) breathing.value else 1f
                    scaleX = s
                    scaleY = s
                },
            contentAlignment = Alignment.Center
        ) {
            NisLogo(
                size = logoSize,
                timeMs = { timeline.value },
                playScale = { playScale.value },
                beaconIntensity = { beacon.value }
            )

            // C. Finite Ruby Beacon Ping Wave (3 slow pulses only)
            if (!reducedMotion) {
                Canvas(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-logoSize * 0.13f), y = (logoSize * 0.08f))
                ) {
                    val p = pingAnim.value
                    if (p in 0.001f..0.999f) {
                        val pingRadius = (size.width * 0.18f + p * size.width * 0.38f).coerceAtLeast(1f)
                        val pingAlpha = (1f - p).coerceIn(0f, 1f) * 0.75f
                        drawCircle(
                            color = Color(0xFFFF4D5E).copy(alpha = pingAlpha),
                            radius = pingRadius,
                            center = center,
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive debug scrubber preview to visually inspect logo choreography across time.
 */
@Preview(showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
fun NisLogoTimelineScrubberPreview() {
    var scrubTimeMs by remember { mutableFloatStateOf(1900f) }

    Column(
        modifier = Modifier
            .background(NisBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Timeline: ${scrubTimeMs.toInt()} ms / ${SplashTimeline.BUILD_END_MS} ms",
            color = Color.White,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            val playScale = if (scrubTimeMs >= SplashTimeline.PlayStartMs) 1f else 0f
            val beaconIntensity = when {
                scrubTimeMs < SplashTimeline.BeaconStartMs -> 0f
                scrubTimeMs in SplashTimeline.BeaconStartMs.toFloat()..1370f -> 1.6f
                else -> 1f
            }

            NisLogo(
                size = 120.dp,
                timeMs = { scrubTimeMs },
                playScale = { playScale },
                beaconIntensity = { beaconIntensity }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Slider(
            value = scrubTimeMs,
            onValueChange = { scrubTimeMs = it },
            valueRange = 0f..SplashTimeline.BUILD_END_MS.toFloat()
        )
    }
}
