package com.nis.nistv.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nis.nistv.core.designsystem.theme.NisAccentCyan
import com.nis.nistv.core.designsystem.theme.NisAccentPrimary
import com.nis.nistv.core.designsystem.theme.NisTextPrimary

/**
 * World-class NTV Brand Emblem & Wordmark.
 * Inspired by cinematic streaming typography with an ultra-sleek 3D ribbon 'N'
 * in Electric Indigo & Cyber Neon Cyan, paired with bold geometric 'TV' typography.
 */
@Composable
fun NtvLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    showWordmark: Boolean = true,
    fontSize: Int = 20
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
    ) {
        // 3D Sculpted Ribbon 'N'
        NtvRibbonMonogram(
            width = size * 0.75f,
            height = size
        )

        if (showWordmark) {
            Spacer(modifier = Modifier.width(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 1.dp)
            ) {
                Text(
                    text = "TV",
                    color = Color.White,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.SansSerif
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Pulsing Live Broadcast Neon Dot
                Box(
                    modifier = Modifier
                        .size((fontSize * 0.28f).dp)
                        .clip(CircleShape)
                        .background(NisAccentCyan)
                )
            }
        }
    }
}

/**
 * Pure 3D Ribbon 'N' Monogram with multi-layer depth, concave base curves,
 * and realistic ambient drop shadows.
 */
@Composable
fun NtvRibbonMonogram(
    modifier: Modifier = Modifier,
    width: Dp = 24.dp,
    height: Dp = 32.dp
) {
    Canvas(
        modifier = modifier.size(width = width, height = height)
    ) {
        val w = size.width
        val h = size.height

        // Stem width: ~31% of total width
        val stemW = w * 0.31f
        // Bottom concave arch depth (~6% of height)
        val archH = h * 0.06f

        // ==========================================
        // 1. LEFT VERTICAL STEM (Deep Indigo to Electric Indigo)
        // ==========================================
        val leftStem = Path().apply {
            moveTo(0f, 0f)
            lineTo(stemW, 0f)
            lineTo(stemW, h - archH * 0.5f)
            // Concave bottom curve
            quadraticTo(stemW * 0.5f, h - archH, 0f, h)
            close()
        }
        drawPath(
            path = leftStem,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF4338CA), // Deep Indigo
                    Color(0xFF6366F1)  // Electric Indigo
                ),
                startY = 0f,
                endY = h
            ),
            style = Fill
        )

        // ==========================================
        // 2. RIGHT VERTICAL STEM (Electric Indigo to Violet Slate)
        // ==========================================
        val rightX = w - stemW
        val rightStem = Path().apply {
            moveTo(rightX, 0f)
            lineTo(w, 0f)
            lineTo(w, h)
            // Concave bottom curve
            quadraticTo(rightX + stemW * 0.5f, h - archH, rightX, h - archH * 0.5f)
            close()
        }
        drawPath(
            path = rightStem,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF4F46E5), // Indigo
                    Color(0xFF3730A3)  // Deep Midnight Violet
                ),
                startY = 0f,
                endY = h
            ),
            style = Fill
        )

        // ==========================================
        // 3. AMBIENT DROP SHADOW UNDER DIAGONAL
        // ==========================================
        val shadowPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(stemW, 0f)
            lineTo(stemW, h * 0.55f)
            lineTo(0f, h * 0.40f)
            close()
        }
        drawPath(
            path = shadowPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0x66000000),
                    Color(0x99000000),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(stemW, h * 0.55f)
            )
        )

        // ==========================================
        // 4. DIAGONAL RIBBON (Cyber Neon Cyan to Electric Indigo)
        // Sweeps over the stems with intense luminous gradient
        // ==========================================
        val diagonalRibbon = Path().apply {
            moveTo(0f, 0f)
            lineTo(stemW, 0f)
            lineTo(w, h - archH * 0.5f)
            // Bottom concave curve on right
            quadraticTo(rightX + stemW * 0.5f, h - archH, rightX, h)
            close()
        }
        drawPath(
            path = diagonalRibbon,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00E5FF), // Vivid Cyber Cyan
                    Color(0xFF38BDF8), // Sky Blue
                    Color(0xFF6366F1), // Electric Indigo
                    Color(0xFF818CF8)  // Soft Violet Highlight
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            style = Fill
        )

        // ==========================================
        // 5. TOP LIGHT REFLECTION BEVEL (Micro-sheen)
        // ==========================================
        val topBevel = Path().apply {
            moveTo(0f, 0f)
            lineTo(stemW, 0f)
            lineTo(stemW * 0.8f, 2f)
            lineTo(0f, 2f)
            close()
        }
        drawPath(
            path = topBevel,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xAAFFFFFF), Color(0x22FFFFFF))
            )
        )
    }
}
