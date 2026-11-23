package com.nis.nistv.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// =============================================================================
// NisTV Revamped — Cinematic Obsidian 2.0 Shapes System
// =============================================================================

/**
 * Standard Material 3 Shapes scale for NisTV:
 * - extraSmall: 6dp (badges, small tags, live indicators)
 * - small: 10dp (inner tiles, channel logos, compact thumbnails)
 * - medium: 14dp (buttons, cards, search bar, poster & backdrop media cards)
 * - large: 18dp (featured hero cards, floating mini-player container)
 * - extraLarge: 24dp (dialogs, bottom sheets top corners)
 */
val NisShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// Reusable / Semantic Component Shapes
val NisSmallControlShape = RoundedCornerShape(6.dp)
val NisBadgeShape = RoundedCornerShape(6.dp)
val NisThumbnailShape = RoundedCornerShape(10.dp)
val NisButtonShape = RoundedCornerShape(14.dp)
val NisChannelCardShape = RoundedCornerShape(14.dp)
val NisMediaCardShape = RoundedCornerShape(14.dp) // Shared morph token for Home cards & Details header
val NisPosterMediaCardShape = NisMediaCardShape
val NisBackdropMediaCardShape = NisMediaCardShape
val NisFeaturedCardShape = RoundedCornerShape(18.dp)
val NisMiniPlayerShape = RoundedCornerShape(18.dp)
val NisDialogShape = RoundedCornerShape(24.dp)
val NisBottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val NisSideDrawerShape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
val NisChipShape = RoundedCornerShape(999.dp)
val NisPillShape = RoundedCornerShape(999.dp)
