package com.nis.nistv.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nis.nistv.core.designsystem.theme.NisDimens
import com.nis.nistv.core.designsystem.theme.NisShapes
import com.nis.nistv.core.designsystem.theme.NisTheme

/**
 * Standard SectionHeader with cinematic typography:
 * - Title uses MaterialTheme.typography.headlineMedium
 * - Optional subtitle in bodySmall
 * - Optional "See all" / action with guaranteed 48dp touch target
 */
@Composable
fun NisSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionText: String? = null,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = title,
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.headlineMedium
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (onActionClick != null && (!actionText.isNullOrBlank() || actionIcon != null)) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = NisDimens.TouchTarget)
                    .nisPressable(
                        onClick = onActionClick,
                        role = Role.Button,
                        onClickLabel = actionText ?: "See all",
                        shape = NisShapes.small
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!actionText.isNullOrBlank()) {
                    Text(
                        text = actionText,
                        color = NisTheme.colors.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (actionIcon != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionText,
                        tint = NisTheme.colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
