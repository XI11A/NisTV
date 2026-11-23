package com.nis.nistv.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nis.nistv.core.designsystem.theme.NisButtonShape
import com.nis.nistv.core.designsystem.theme.NisDimens
import com.nis.nistv.core.designsystem.theme.NisTheme

/**
 * Cinematic Obsidian 2.0 Primary Action Button:
 * - Solid primary with onPrimary typography
 * - Guaranteed 48dp minimum touch target height
 * - Medium (14dp) corner radius from NisShapes
 * - Active pressed color transition to primaryPressed
 * - Subtle 1dp top highlight border
 * - 38% content alpha when disabled
 */
@Composable
fun NisPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val colors = NisTheme.colors
    val containerColor = when {
        !enabled -> colors.surfaceElevated
        isPressed -> colors.primaryPressed
        else -> colors.primary
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = NisDimens.TouchTarget)
            .border(
                width = 1.dp,
                color = if (enabled) colors.borderSubtle else Color.Transparent,
                shape = NisButtonShape
            ),
        enabled = enabled,
        shape = NisButtonShape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = colors.surfaceElevated,
            disabledContentColor = colors.textPrimary.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(NisDimens.IconSmall)
                )
                Spacer(modifier = Modifier.width(NisDimens.Scale8))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * Cinematic Obsidian 2.0 Secondary Action Button:
 * - NisSurfaceElevated background with NisBorderSubtle border
 * - High-contrast NisTextPrimary copy
 * - Guaranteed 48dp minimum touch target height
 */
@Composable
fun NisSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val colors = NisTheme.colors
    val glassBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.35f),
            colors.borderSubtle.copy(alpha = 0.50f)
        )
    )
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = NisDimens.TouchTarget),
        enabled = enabled,
        shape = NisButtonShape,
        border = BorderStroke(1.dp, glassBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surfaceElevated.copy(alpha = 0.55f),
            contentColor = colors.textPrimary,
            disabledContainerColor = colors.surfaceElevated.copy(alpha = 0.25f),
            disabledContentColor = colors.textPrimary.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(NisDimens.IconSmall),
                    tint = if (enabled) colors.textPrimary else colors.textPrimary.copy(alpha = 0.38f)
                )
                Spacer(modifier = Modifier.width(NisDimens.Scale8))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) colors.textPrimary else colors.textPrimary.copy(alpha = 0.38f)
            )
        }
    }
}

/**
 * Standard Icon Action Button:
 * - Guaranteed 48dp minimum hit target
 * - 40dp visual inner container
 * - Tactile press feedback with tinted bounded ripple
 */
@Composable
fun NisIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = NisTheme.colors.textPrimary,
    backgroundColor: Color = Color.Transparent
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = NisDimens.TouchTarget, minHeight = NisDimens.TouchTarget)
            .nisPressable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = contentDescription,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(NisDimens.IconMedium)
            )
        }
    }
}
