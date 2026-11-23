package com.nis.nistv.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.nis.nistv.core.designsystem.theme.NisShapes
import com.nis.nistv.core.designsystem.theme.NisTheme

@Composable
fun NisCardSurface(
    modifier: Modifier = Modifier,
    shape: Shape = NisShapes.large,
    backgroundColor: Color = NisTheme.colors.surface,
    borderColor: Color = NisTheme.colors.border,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(BorderStroke(1.dp, borderColor), shape),
        content = content
    )
}

@Composable
fun NisElevatedSurface(
    modifier: Modifier = Modifier,
    shape: Shape = NisShapes.large,
    content: @Composable BoxScope.() -> Unit
) {
    NisCardSurface(
        modifier = modifier,
        shape = shape,
        backgroundColor = NisTheme.colors.surfaceElevated,
        borderColor = NisTheme.colors.border,
        content = content
    )
}
