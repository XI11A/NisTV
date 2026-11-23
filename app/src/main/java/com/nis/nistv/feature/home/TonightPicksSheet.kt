package com.nis.nistv.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.data.recommendation.CandidatePick
import com.nis.nistv.data.recommendation.TimeContext
import com.nis.nistv.data.recommendation.TonightPicksHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TonightPicksSheet(
    picks: List<CandidatePick>,
    onPickSelected: (CandidatePick) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var surprisePickedId by remember { mutableStateOf<String?>(null) }

    val timeContext = remember { TimeContext.current() }
    val timeLabel = remember(timeContext) {
        when {
            timeContext.hourOfDay >= 23 || timeContext.hourOfDay < 4 -> "Late Night Short & Sweet"
            timeContext.isWeekend -> "Weekend Feature Night"
            timeContext.hourOfDay in 18..22 -> "Prime Time Entertainment"
            else -> "Quick Refresh"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NisSurface,
        shape = NisBottomSheetShape,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = NisBorder)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = NisDimens.MaxContentWidth)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✨ Tonight's Picks",
                            style = MaterialTheme.typography.titleLarge,
                            color = NisTextPrimary
                        )
                    }
                    Text(
                        text = "$timeLabel • Curated for right now",
                        style = MaterialTheme.typography.bodySmall,
                        color = NisAccentAmber
                    )
                }

                // Surprise Me Button
                Button(
                    onClick = {
                        val chosen = TonightPicksHelper.pickSurpriseItem(picks)
                        if (chosen != null) {
                            surprisePickedId = chosen.id
                            onPickSelected(chosen)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NisAccentPrimary),
                    shape = NisButtonShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "🎲 Surprise Me",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (picks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Finding your top picks for tonight...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NisTextMuted
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(picks, key = { it.id }) { pick ->
                        val isHighlighted = surprisePickedId == pick.id
                        TonightPickCard(
                            pick = pick,
                            isHighlighted = isHighlighted,
                            onClick = { onPickSelected(pick) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TonightPickCard(
    pick: CandidatePick,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isHighlighted) NisAccentPrimary else NisBorder
    val borderWidth = if (isHighlighted) 2.dp else 1.dp

    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(190.dp)
                .clip(NisMediaCardShape)
                .background(NisSurfaceElevated)
                .border(borderWidth, borderColor, NisMediaCardShape)
        ) {
            if (!pick.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = pick.posterUrl,
                    contentDescription = pick.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(NisSurfaceElevated, NisSurface)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pick.title.take(2).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = NisTextMuted
                    )
                }
            }

            // Badges overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (pick.isDownloaded) {
                    Surface(
                        shape = NisBadgeShape,
                        color = NisSuccess
                    ) {
                        Text(
                            text = "OFFLINE",
                            color = Color.White,
                            style = NisTheme.textExtras.micro,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = NisBadgeShape,
                        color = NisScrimStrong
                    ) {
                        Text(
                            text = pick.mediaType.uppercase(),
                            color = NisTextSecondary,
                            style = NisTheme.textExtras.micro,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                pick.runtimeMinutes?.let { mins ->
                    Surface(
                        shape = NisBadgeShape,
                        color = NisScrimStrong
                    ) {
                        Text(
                            text = "${mins}m",
                            color = NisTextSecondary,
                            style = NisTheme.textExtras.micro,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = pick.title,
            color = NisTextPrimary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
