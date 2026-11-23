package com.nis.nistv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.NisBackground
import com.nis.nistv.core.designsystem.theme.NisDimens
import com.nis.nistv.core.designsystem.theme.NisTextMuted
import com.nis.nistv.core.designsystem.theme.NisTextPrimary
import com.nis.nistv.core.designsystem.theme.NisTextSecondary
import com.nis.nistv.core.designsystem.theme.NisTvTheme
import com.nis.nistv.core.preferences.AppThemeConfig

/**
 * Cinematic Obsidian 2.0 Component Gallery Preview.
 * Showcases the entire core design system component library in both standard
 * and accessibility-scaled (fontScale 1.3) typography.
 */
@Composable
fun NisDesignGalleryContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(NisDimens.Scale24),
        verticalArrangement = Arrangement.spacedBy(NisDimens.Scale20)
    ) {
        // Section: Typography Header
        Text(
            text = "Cinematic Obsidian 2.0 Gallery",
            style = MaterialTheme.typography.headlineLarge,
            color = NisTextPrimary
        )

        Text(
            text = "Design System Components & States",
            style = MaterialTheme.typography.bodyMedium,
            color = NisTextMuted
        )

        // Section: Buttons
        NisSectionHeader(title = "Buttons")
        Row(
            horizontalArrangement = Arrangement.spacedBy(NisDimens.Scale8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NisPrimaryButton(
                text = "Primary Action",
                onClick = {},
                icon = NisIcons.Play
            )
            NisSecondaryButton(
                text = "Secondary",
                onClick = {}
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(NisDimens.Scale8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NisPrimaryButton(
                text = "Disabled",
                onClick = {},
                enabled = false
            )
            NisIconButton(
                icon = NisIcons.Bookmark,
                contentDescription = "Save",
                onClick = {}
            )
        }

        // Section: Badges & Chips
        NisSectionHeader(title = "Badges & Category Chips")
        Row(
            horizontalArrangement = Arrangement.spacedBy(NisDimens.Scale8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NisLiveBadge()
            NisResolutionBadge(resolution = "4K HDR")
            NisWarningBadge(text = "Expiring")
            NisBadge(text = "1080p")
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(NisDimens.Scale8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NisCategoryChip(
                text = "Movies",
                isSelected = true,
                onClick = {}
            )
            NisCategoryChip(
                text = "TV Series",
                isSelected = false,
                onClick = {}
            )
            NisCategoryChip(
                text = "Live TV",
                isSelected = false,
                onClick = {}
            )
        }

        // Section: Search Bar
        NisSectionHeader(title = "Search Bar")
        NisSearchBar(
            query = "Interstellar",
            onQueryChange = {},
            onClearClick = {}
        )

        // Section: Progress & Loading
        NisSectionHeader(title = "Loading Indicators")
        Row(
            horizontalArrangement = Arrangement.spacedBy(NisDimens.Scale16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NisLoadingIndicator(size = 32.dp)
            NisLoadingIndicator(size = 24.dp)
            Text(
                text = "Buffering stream...",
                style = MaterialTheme.typography.bodySmall,
                color = NisTextSecondary
            )
        }

        // Section: Skeletons
        NisSectionHeader(title = "Skeleton Loaders")
        Row(
            horizontalArrangement = Arrangement.spacedBy(NisDimens.Scale8)
        ) {
            ChannelRailCardSkeleton()
            ChannelRailCardSkeleton()
        }
        ChannelCardSkeleton()

        // Section: Empty & Error States
        NisSectionHeader(title = "Empty State")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            NisEmptyState(
                title = "No Bookmarks Yet",
                message = "Movies and TV series you save will appear here for fast access.",
                icon = NisIcons.BookmarkBorder,
                actionText = "Browse Catalog",
                onActionClick = {}
            )
        }

        Spacer(modifier = Modifier.height(NisDimens.Scale32))
    }
}

@Preview(name = "Obsidian 2.0 Standard Scale (1.0x)", showBackground = true, widthDp = 380, heightDp = 1200)
@Composable
private fun NisDesignGalleryPreviewStandard() {
    NisTvTheme {
        NisDesignGalleryContent()
    }
}

@Preview(name = "Obsidian 2.0 Accessibility Scale (1.3x)", fontScale = 1.3f, showBackground = true, widthDp = 420, heightDp = 1400)
@Composable
private fun NisDesignGalleryPreviewLargeFont() {
    NisTvTheme {
        NisDesignGalleryContent()
    }
}

@Preview(name = "Cinematic Light Palette Standard Scale (1.0x)", showBackground = true, widthDp = 380, heightDp = 1200)
@Composable
private fun NisDesignGalleryPreviewLight() {
    NisTvTheme(appTheme = AppThemeConfig.LIGHT) {
        NisDesignGalleryContent()
    }
}
