package com.nis.nistv.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nis.nistv.feature.splash.SplashTimeline

/**
 * Pure Jetpack Compose NisTV Brand Emblem.
 *
 * Rendered via Compose Canvas with GPU hardware acceleration and zero per-frame object allocation:
 * 1. Pre-calculated geometry, paths, and static brushes via [NisLogoGeometry] inside `drawWithCache`.
 * 2. Animated parameters read exclusively in the draw phase via `() -> Float` lambdas (zero recompositions).
 * 3. Exact 108x108 brand geometry and color stops preserved.
 *
 * Visual Layers:
 * 1. Obsidian squircle base with cosmic violet/indigo radial core.
 * 2. Glowing squircle border drawn on progressively via [PathMeasure].
 * 3. Ambient cyan/indigo center glow.
 * 4. 3D sculpted monogram 'N' dual shaded vertical pillars rising from bottom.
 * 5. Sweeping Cyber Neon diagonal ribbon with glowing leading head.
 * 6. Luminous Play Core (▶) streaming symbol with elastic bounce.
 * 7. Soft radial light burst emanating from play core.
 * 8. Single specular light sheen sweeping diagonally across the emblem.
 * 9. Live TV Broadcast Ruby Beacon at the upper-right shoulder with pulsing flares.
 */
@Composable
fun NisLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    timeMs: () -> Float = { SplashTimeline.BUILD_END_MS.toFloat() },
    playScale: () -> Float = { 1f },
    beaconIntensity: () -> Float = { 1f }
) {
    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val g = NisLogoGeometry.build(this.size.width, this.size.height)
                onDrawBehind {
                    val t = timeMs()
                    val pScale = playScale()
                    val beacon = beaconIntensity()

                    // 1. Squircle Base Frame
                    val squircleAlpha = SplashTimeline.progress(t, SplashTimeline.Squircle, SplashTimeline.EaseOutQuint)
                    if (squircleAlpha > 0f) {
                        val squircleScale = 0.94f + 0.06f * squircleAlpha
                        withTransform({
                            scale(squircleScale, squircleScale, pivot = g.centerOffset)
                        }) {
                            drawRoundRect(
                                brush = g.baseBrush,
                                topLeft = g.baseTopLeft,
                                size = g.baseSizeObj,
                                cornerRadius = g.baseCornerRadius,
                                alpha = squircleAlpha
                            )
                        }
                    }

                    // 2. Glowing Squircle Border (draws on around the shape via PathMeasure)
                    val borderProgress = SplashTimeline.progress(t, SplashTimeline.Border, SplashTimeline.EaseInOutCubic)
                    if (borderProgress > 0f) {
                        g.borderSegmentPath.reset()
                        val stopD = g.borderLength * borderProgress
                        g.borderMeasure.getSegment(0f, stopD, g.borderSegmentPath, true)
                        drawPath(
                            path = g.borderSegmentPath,
                            brush = g.borderBrush,
                            style = Stroke(width = g.borderStrokeWidth),
                            alpha = borderProgress.coerceIn(0f, 1f)
                        )
                    }

                    // 3. Ambient Center Glow
                    val glowProgress = SplashTimeline.progress(t, SplashTimeline.Glow, LinearEasing)
                    if (glowProgress > 0f) {
                        drawCircle(
                            brush = g.glowBrush,
                            radius = g.glowRadius,
                            center = g.centerOffset,
                            alpha = glowProgress
                        )
                    }

                    // 4. Shaded Vertical Pillars (Clip reveal from bottom + alpha ramp)
                    val pL = SplashTimeline.progress(t, SplashTimeline.PillarL, SplashTimeline.EaseOutQuint)
                    if (pL > 0f) {
                        clipRect(
                            left = 0f,
                            top = 81f * g.sy - (81f - 27f) * g.sy * pL,
                            right = g.width,
                            bottom = g.height
                        ) {
                            drawPath(
                                path = g.leftPillar,
                                brush = g.leftPillarBrush,
                                style = Fill,
                                alpha = pL
                            )
                            drawPath(
                                path = g.leftShadow,
                                brush = g.leftShadowBrush,
                                style = Fill,
                                alpha = pL
                            )
                        }
                    }

                    val pR = SplashTimeline.progress(t, SplashTimeline.PillarR, SplashTimeline.EaseOutQuint)
                    if (pR > 0f) {
                        clipRect(
                            left = 0f,
                            top = 81f * g.sy - (81f - 27f) * g.sy * pR,
                            right = g.width,
                            bottom = g.height
                        ) {
                            drawPath(
                                path = g.rightPillar,
                                brush = g.rightPillarBrush,
                                style = Fill,
                                alpha = pR
                            )
                            drawPath(
                                path = g.rightShadow,
                                brush = g.rightShadowBrush,
                                style = Fill,
                                alpha = pR
                            )
                        }
                    }

                    // 5. Sweeping Diagonal Ribbon with Leading Glowing Head
                    val ribbonProgress = SplashTimeline.progress(t, SplashTimeline.Ribbon, SplashTimeline.EaseInOutCubic)
                    if (ribbonProgress > 0f) {
                        clipRect(
                            left = 0f,
                            top = 0f,
                            right = (28f + 54f * ribbonProgress) * g.sx,
                            bottom = (25f + 58f * ribbonProgress) * g.sy
                        ) {
                            drawPath(
                                path = g.diagonalRibbon,
                                brush = g.ribbonBrush,
                                style = Fill,
                                alpha = 1f
                            )
                            drawPath(
                                path = g.topSheen,
                                color = Color(0x99FFFFFF),
                                style = Fill,
                                alpha = 1f
                            )
                        }

                        // Glowing Leading Head on the wipe front
                        if (ribbonProgress < 1f) {
                            val headX = (36.5f + (71.5f - 36.5f) * ribbonProgress) * g.sx
                            val headY = (27f + (81f - 27f) * ribbonProgress) * g.sy
                            val headAlpha = if (ribbonProgress < 0.85f) 1f else ((1f - ribbonProgress) / 0.15f)
                            withTransform({
                                translate(left = headX, top = headY)
                            }) {
                                drawCircle(
                                    brush = g.leadingHeadBrush,
                                    radius = 7f * g.sx,
                                    center = Offset.Zero,
                                    alpha = headAlpha
                                )
                            }
                        }
                    }

                    // 6. Luminous Play Core (▶) Symbol & Radial Light Burst
                    val burstProgress = SplashTimeline.progress(t, SplashTimeline.Burst, FastOutSlowInEasing)
                    if (burstProgress in 0.001f..0.999f) {
                        val burstAlpha = (1f - burstProgress) * 0.35f
                        val burstRadius = g.width * (0.2f + 0.7f * burstProgress)
                        drawCircle(
                            brush = g.burstBrush,
                            radius = burstRadius,
                            center = g.playCenter,
                            alpha = burstAlpha
                        )
                    }

                    if (pScale > 0.01f) {
                        withTransform({
                            scale(scaleX = pScale, scaleY = pScale, pivot = g.playCenter)
                        }) {
                            drawPath(
                                path = g.playShadow,
                                color = Color(0x66000000),
                                style = Fill
                            )
                            drawPath(
                                path = g.playCore,
                                brush = g.playBrush,
                                style = Fill
                            )
                        }
                    }

                    // 7. Specular Sheen Sweep (Single diagonal sweep between 1300 and 1900 ms)
                    val sheenProgress = SplashTimeline.progress(t, SplashTimeline.Sheen, LinearEasing)
                    if (sheenProgress in 0.001f..0.999f) {
                        val totalDistance = g.width + g.sweepWidth * 2f
                        val sweepPos = -g.sweepWidth + sheenProgress * totalDistance
                        clipRect(
                            left = g.basePadding,
                            top = g.basePadding,
                            right = g.basePadding + g.baseSize,
                            bottom = g.basePadding + g.baseSize
                        ) {
                            withTransform({
                                translate(left = sweepPos, top = sweepPos)
                            }) {
                                drawRect(
                                    brush = g.sheenBrush,
                                    topLeft = Offset(-g.sweepWidth, -g.sweepWidth),
                                    size = Size(g.sweepWidth * 2f, g.sweepWidth * 2f)
                                )
                            }
                        }
                    }

                    // 8. Live TV Broadcast Ruby Beacon (Top-Right Flare)
                    if (beacon > 0.01f) {
                        val haloRadius = (4.5f * beacon * g.sx).coerceAtLeast(1f)
                        val coreRadius = (2.4f * (0.8f + 0.2f * beacon.coerceAtMost(1.5f)) * g.sx).coerceAtLeast(1f)
                        val dotRadius = (0.9f * g.sx).coerceAtLeast(0.5f)

                        drawCircle(
                            color = Color(0x66FF4D5E).copy(alpha = (0.45f * beacon).coerceIn(0f, 0.85f)),
                            radius = haloRadius,
                            center = g.beaconCenter
                        )
                        drawCircle(
                            color = Color(0xFFFF4D5E),
                            radius = coreRadius,
                            center = g.beaconCenter
                        )
                        drawCircle(
                            color = Color(0xFFFFFFFF),
                            radius = dotRadius,
                            center = Offset(g.beaconCenter.x - 0.4f * g.sx, g.beaconCenter.y - 0.4f * g.sy)
                        )
                    }
                }
            }
    )
}

/**
 * Backward-compatible overload for legacy callers.
 */
@Deprecated("Use lambda-based NisLogo overload to avoid per-frame recomposition")
@Composable
fun NisLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    drawProgress: Float,
    ribbonEnergyProgress: Float = 0f,
    playScale: Float = 1f,
    beaconIntensity: Float = 1f
) {
    NisLogo(
        modifier = modifier,
        size = size,
        timeMs = { drawProgress * SplashTimeline.BUILD_END_MS },
        playScale = { playScale },
        beaconIntensity = { beaconIntensity }
    )
}
