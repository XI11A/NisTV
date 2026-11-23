package com.nis.nistv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.NisDimens
import com.nis.nistv.core.designsystem.theme.NisShapes
import com.nis.nistv.core.designsystem.theme.NisTheme

/**
 * Reusable cinematic search input bar:
 * - NisTheme.colors.surfaceElevated background
 * - Medium (14dp) corner radius
 * - 48dp standard height
 * - 1.5dp primary focus ring
 * - Placeholder in NisTheme.colors.textMuted
 * - 48dp touch target for clear button
 */
@Composable
fun NisSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Search channels, genres, languages...",
    onSearchSubmit: ((String) -> Unit)? = null,
    onClearClick: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val colors = NisTheme.colors

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = query, selection = TextRange(query.length)))
    }

    // Keep internal TextFieldValue in sync if query was changed externally (e.g. clear, chip selection)
    if (query != textFieldValue.text) {
        textFieldValue = textFieldValue.copy(
            text = query,
            selection = TextRange(query.length)
        )
    }

    val borderStrokeWidth = if (isFocused) 1.5.dp else 1.dp
    val borderColor = if (isFocused) colors.primary else colors.borderSubtle

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(NisDimens.TouchTarget)
            .clip(NisShapes.medium)
            .background(colors.surfaceElevated)
            .border(borderStrokeWidth, borderColor, NisShapes.medium)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Leading Search Icon
        Icon(
            imageVector = NisIcons.Search,
            contentDescription = "Search",
            tint = if (isFocused) colors.primary else colors.textSecondary,
            modifier = Modifier
                .size(NisDimens.IconSmall)
                .align(Alignment.CenterStart)
        )

        // Text Field Input
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                if (newValue.text != query) {
                    onQueryChange(newValue.text)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 30.dp, end = if (textFieldValue.text.isNotEmpty()) 44.dp else 0.dp)
                .onFocusChanged { isFocused = it.isFocused },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearchSubmit?.invoke(textFieldValue.text)
                }
            ),
            decorationBox = { innerTextField ->
                if (textFieldValue.text.isEmpty()) {
                    Text(
                        text = placeholderText,
                        color = colors.textMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                innerTextField()
            }
        )

        // Trailing Clear Icon
        if (textFieldValue.text.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = NisDimens.TouchTarget, minHeight = NisDimens.TouchTarget)
                    .align(Alignment.CenterEnd)
                    .nisPressable(
                        onClick = {
                            textFieldValue = TextFieldValue("", TextRange.Zero)
                            onClearClick?.invoke() ?: onQueryChange("")
                        },
                        role = Role.Button,
                        onClickLabel = "Clear search query",
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = NisIcons.Close,
                    contentDescription = "Clear search",
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
