package com.nis.nistv.core.designsystem.component

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure

/**
 * Immutable pre-calculated geometry, paths, and static brushes for [NisLogo].
 *
 * Instantiated exactly once per unique (width, height) within `drawWithCache`.
 * All paths and gradients use coordinates normalized to a 108x108 grid, scaled by
 * `sx = w / 108f` and `sy = h / 108f`, exactly matching the brand geometry.
 */
class NisLogoGeometry private constructor(
    val width: Float,
    val height: Float,
    val sx: Float,
    val sy: Float,
    val basePadding: Float,
    val baseSize: Float,
    val baseCornerR: Float,
    val baseTopLeft: Offset,
    val baseSizeObj: Size,
    val baseCornerRadius: CornerRadius,
    val centerOffset: Offset,
    val glowRadius: Float,
    val borderStrokeWidth: Float,
    val borderMeasure: PathMeasure,
    val borderLength: Float,
    val borderSegmentPath: Path,
    val leftPillar: Path,
    val rightPillar: Path,
    val leftShadow: Path,
    val rightShadow: Path,
    val diagonalRibbon: Path,
    val topSheen: Path,
    val playCenter: Offset,
    val playShadow: Path,
    val playCore: Path,
    val beaconCenter: Offset,
    val baseBrush: Brush,
    val borderBrush: Brush,
    val glowBrush: Brush,
    val leftPillarBrush: Brush,
    val rightPillarBrush: Brush,
    val leftShadowBrush: Brush,
    val rightShadowBrush: Brush,
    val ribbonBrush: Brush,
    val playBrush: Brush,
    val leadingHeadBrush: Brush,
    val burstBrush: Brush,
    val sheenBrush: Brush,
    val sweepWidth: Float,
    val burstMaxRadius: Float
) {
    companion object {
        fun build(w: Float, h: Float): NisLogoGeometry {
            val sx = w / 108f
            val sy = h / 108f

            val basePadding = 6f * sx
            val baseSize = 96f * sx
            val baseCornerR = 18f * sx
            val baseTopLeft = Offset(basePadding, basePadding)
            val baseSizeObj = Size(baseSize, baseSize)
            val baseCornerRadius = CornerRadius(baseCornerR, baseCornerR)

            val centerOffset = Offset(54f * sx, 54f * sy)
            val glowRadius = 30f * sx
            val borderStrokeWidth = (1.2f * sx).coerceAtLeast(1f)

            // Squircle border path & PathMeasure for draw-on animation
            val borderPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = basePadding,
                        top = basePadding,
                        right = basePadding + baseSize,
                        bottom = basePadding + baseSize,
                        cornerRadius = CornerRadius(baseCornerR, baseCornerR)
                    )
                )
            }
            val borderMeasure = PathMeasure().apply {
                setPath(borderPath, forceClosed = false)
            }
            val borderLength = borderMeasure.length
            val borderSegmentPath = Path()

            // Left Pillar (rounded top, flat bottom)
            val leftPillar = Path().apply {
                moveTo(30f * sx, 33f * sy)
                cubicTo(30f * sx, 29.7f * sy, 32.7f * sx, 27f * sy, 36f * sx, 27f * sy)
                lineTo(42f * sx, 27f * sy)
                lineTo(42f * sx, 81f * sy)
                lineTo(30f * sx, 81f * sy)
                close()
            }

            // Right Pillar (flat top, rounded bottom)
            val rightPillar = Path().apply {
                moveTo(66f * sx, 27f * sy)
                lineTo(78f * sx, 27f * sy)
                lineTo(78f * sx, 75f * sy)
                cubicTo(78f * sx, 78.3f * sy, 75.3f * sx, 81f * sy, 72f * sx, 81f * sy)
                lineTo(66f * sx, 81f * sy)
                close()
            }

            // Pillar Ambient Drop Shadows under Ribbon
            val leftShadow = Path().apply {
                moveTo(30f * sx, 27f * sy)
                lineTo(42f * sx, 27f * sy)
                lineTo(42f * sx, 54f * sy)
                lineTo(30f * sx, 40f * sy)
                close()
            }

            val rightShadow = Path().apply {
                moveTo(66f * sx, 54f * sy)
                lineTo(78f * sx, 68f * sy)
                lineTo(78f * sx, 81f * sy)
                lineTo(66f * sx, 81f * sy)
                close()
            }

            // Diagonal Ribbon
            val diagonalRibbon = Path().apply {
                moveTo(30f * sx, 33f * sy)
                cubicTo(30f * sx, 29.7f * sy, 32.7f * sx, 27f * sy, 36f * sx, 27f * sy)
                lineTo(43f * sx, 27f * sy)
                lineTo(78f * sx, 75f * sy)
                cubicTo(78f * sx, 78.3f * sy, 75.3f * sx, 81f * sy, 72f * sx, 81f * sy)
                lineTo(65f * sx, 81f * sy)
                close()
            }

            // Top Sheen on Ribbon Ridge
            val topSheen = Path().apply {
                moveTo(30f * sx, 33f * sy)
                cubicTo(30f * sx, 30.3f * sy, 32f * sx, 28.2f * sy, 34.5f * sx, 27.5f * sy)
                lineTo(42f * sx, 27.5f * sy)
                lineTo(42f * sx, 27f * sy)
                lineTo(36f * sx, 27f * sy)
                cubicTo(32.7f * sx, 27f * sy, 30f * sx, 29.7f * sy, 30f * sx, 33f * sy)
                close()
            }

            // Play Core & Drop Shadow
            val playCenter = Offset(55.75f * sx, 55f * sy)
            val playShadow = Path().apply {
                moveTo(49f * sx, 50.5f * sy)
                cubicTo(49f * sx, 49.2f * sy, 50.4f * sx, 48.3f * sy, 51.5f * sx, 49.0f * sy)
                lineTo(62.5f * sx, 54.8f * sy)
                cubicTo(63.6f * sx, 55.4f * sy, 63.6f * sx, 56.6f * sy, 62.5f * sx, 57.2f * sy)
                lineTo(51.5f * sx, 63.0f * sy)
                cubicTo(50.4f * sx, 63.7f * sy, 49f * sx, 62.8f * sy, 49f * sx, 61.5f * sy)
                close()
            }

            val playCore = Path().apply {
                moveTo(49f * sx, 48.5f * sy)
                cubicTo(49f * sx, 47.2f * sy, 50.4f * sx, 46.3f * sy, 51.5f * sx, 47.0f * sy)
                lineTo(62.5f * sx, 52.8f * sy)
                cubicTo(63.6f * sx, 53.4f * sy, 63.6f * sx, 54.6f * sy, 62.5f * sx, 55.2f * sy)
                lineTo(51.5f * sx, 61.0f * sy)
                cubicTo(50.4f * sx, 61.7f * sy, 49f * sx, 60.8f * sy, 49f * sx, 59.5f * sy)
                close()
            }

            val beaconCenter = Offset(75f * sx, 23.5f * sy)

            // Static Brushes
            val baseBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF221A44),
                    Color(0xFF131028),
                    Color(0xFF07090E)
                ),
                center = centerOffset,
                radius = 50f * sx
            )

            val borderBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0x8000F2FE),
                    Color(0x666366F1),
                    Color(0x26FFFFFF)
                ),
                start = baseTopLeft,
                end = Offset(basePadding + baseSize, basePadding + baseSize)
            )

            val glowBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0x3338BDF8),
                    Color(0x1A6366F1),
                    Color.Transparent
                ),
                center = centerOffset,
                radius = glowRadius
            )

            val leftPillarBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF4338CA),
                    Color(0xFF3730A3),
                    Color(0xFF1E1B4B)
                ),
                startY = 27f * sy,
                endY = 81f * sy
            )

            val rightPillarBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF312E81),
                    Color(0xFF4C1D95),
                    Color(0xFF2E1065)
                ),
                startY = 27f * sy,
                endY = 81f * sy
            )

            val leftShadowBrush = Brush.linearGradient(
                colors = listOf(Color(0x99000000), Color.Transparent),
                start = Offset(36f * sx, 27f * sy),
                end = Offset(36f * sx, 54f * sy)
            )

            val rightShadowBrush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color(0x99000000)),
                start = Offset(72f * sx, 54f * sy),
                end = Offset(72f * sx, 81f * sy)
            )

            val ribbonBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00F2FE),
                    Color(0xFF0EA5E9),
                    Color(0xFF6366F1),
                    Color(0xFFA855F7)
                ),
                start = Offset(30f * sx, 27f * sy),
                end = Offset(78f * sx, 81f * sy)
            )

            val playBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFE0F2FE),
                    Color(0xFF67E8F9)
                ),
                start = Offset(49f * sx, 47f * sy),
                end = Offset(63f * sx, 61f * sy)
            )

            // Leading glowing head brush (centered at 0,0 so translation puts it at front)
            val leadingHeadBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color(0xFF00F2FE).copy(alpha = 0.75f),
                    Color(0xFF0EA5E9).copy(alpha = 0.40f),
                    Color.Transparent
                ),
                center = Offset.Zero,
                radius = 7f * sx
            )

            // Soft radial burst from play core (radius centered at playCenter, up to 0.9 of logo)
            val burstMaxRadius = w * 0.45f
            val burstBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF67E8F9).copy(alpha = 0.45f),
                    Color(0xFF38BDF8).copy(alpha = 0.25f),
                    Color(0xFF6366F1).copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = playCenter,
                radius = burstMaxRadius
            )

            // Single specular light sweep brush (diagonal)
            val sweepWidth = w * 0.45f
            val sheenBrush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.28f),
                    Color(0xFFE0F2FE).copy(alpha = 0.15f),
                    Color.Transparent
                ),
                start = Offset(-sweepWidth, -sweepWidth),
                end = Offset(sweepWidth, sweepWidth)
            )

            return NisLogoGeometry(
                width = w,
                height = h,
                sx = sx,
                sy = sy,
                basePadding = basePadding,
                baseSize = baseSize,
                baseCornerR = baseCornerR,
                baseTopLeft = baseTopLeft,
                baseSizeObj = baseSizeObj,
                baseCornerRadius = baseCornerRadius,
                centerOffset = centerOffset,
                glowRadius = glowRadius,
                borderStrokeWidth = borderStrokeWidth,
                borderMeasure = borderMeasure,
                borderLength = borderLength,
                borderSegmentPath = borderSegmentPath,
                leftPillar = leftPillar,
                rightPillar = rightPillar,
                leftShadow = leftShadow,
                rightShadow = rightShadow,
                diagonalRibbon = diagonalRibbon,
                topSheen = topSheen,
                playCenter = playCenter,
                playShadow = playShadow,
                playCore = playCore,
                beaconCenter = beaconCenter,
                baseBrush = baseBrush,
                borderBrush = borderBrush,
                glowBrush = glowBrush,
                leftPillarBrush = leftPillarBrush,
                rightPillarBrush = rightPillarBrush,
                leftShadowBrush = leftShadowBrush,
                rightShadowBrush = rightShadowBrush,
                ribbonBrush = ribbonBrush,
                playBrush = playBrush,
                leadingHeadBrush = leadingHeadBrush,
                burstBrush = burstBrush,
                sheenBrush = sheenBrush,
                sweepWidth = sweepWidth,
                burstMaxRadius = burstMaxRadius
            )
        }
    }
}
