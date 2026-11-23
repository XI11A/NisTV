package com.nis.nistv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nis.nistv.core.designsystem.theme.NisAccentAmber
import com.nis.nistv.core.designsystem.theme.NisAccentCyan
import com.nis.nistv.core.designsystem.theme.NisAccentPrimary
import com.nis.nistv.core.designsystem.theme.NisAccentViolet
import com.nis.nistv.core.designsystem.theme.NisBackground
import com.nis.nistv.core.designsystem.theme.NisBorder
import com.nis.nistv.core.designsystem.theme.NisLiveEmerald
import com.nis.nistv.core.designsystem.theme.NisLiveRed
import com.nis.nistv.core.designsystem.theme.NisScrimMedium
import com.nis.nistv.core.designsystem.theme.NisSurfaceHigh
import com.nis.nistv.core.designsystem.theme.NisTextPrimary

data class AvatarPreset(
    val id: String,
    val name: String,
    val gradientColors: List<Color>,
    val emoji: String
)

object NisAvatarPresets {
    val presets = listOf(
        AvatarPreset("avatar_01", "Popcorn", listOf(NisAccentCyan, NisAccentPrimary), "🍿"),
        AvatarPreset("avatar_02", "Cinema", listOf(NisAccentPrimary, NisAccentViolet), "🎬"),
        AvatarPreset("avatar_03", "Rocket", listOf(NisAccentViolet, NisAccentCyan), "🚀"),
        AvatarPreset("avatar_04", "Lion", listOf(NisLiveEmerald, NisAccentCyan), "🦁"),
        AvatarPreset("avatar_05", "Tiger", listOf(NisAccentAmber, NisLiveRed), "🐯"),
        AvatarPreset("avatar_06", "Fox", listOf(NisAccentPrimary, NisLiveEmerald), "🦊"),
        AvatarPreset("avatar_07", "Panda", listOf(NisAccentCyan, NisAccentViolet), "🐼"),
        AvatarPreset("avatar_08", "Star", listOf(NisLiveRed, NisAccentAmber), "🌟"),
        AvatarPreset("avatar_09", "Headphones", listOf(NisAccentViolet, NisAccentPrimary), "🎧"),
        AvatarPreset("avatar_10", "Gamepad", listOf(NisLiveEmerald, NisAccentPrimary), "🎮"),
        AvatarPreset("avatar_11", "Crown", listOf(NisAccentAmber, NisAccentViolet), "👑"),
        AvatarPreset("avatar_12", "Planet", listOf(NisAccentPrimary, NisAccentCyan), "🪐")
    )

    fun get(avatarId: String?): AvatarPreset {
        val normalized = when (avatarId) {
            "avatar_1" -> "avatar_01"
            "avatar_2" -> "avatar_02"
            "avatar_3" -> "avatar_03"
            "avatar_4" -> "avatar_04"
            "avatar_5" -> "avatar_05"
            "avatar_6" -> "avatar_06"
            "avatar_7" -> "avatar_07"
            "avatar_8" -> "avatar_08"
            "avatar_9" -> "avatar_09"
            else -> avatarId ?: "avatar_01"
        }
        return presets.find { it.id.equals(normalized, ignoreCase = true) } ?: presets.first()
    }
}

@Composable
fun NisProfileAvatar(
    avatarId: String?,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    name: String? = null,
    contentDescription: String? = null,
    isSelected: Boolean = false,
    isEditMode: Boolean = false,
    isKids: Boolean = false,
    isLocked: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val preset = NisAvatarPresets.get(avatarId)
    val desc = contentDescription ?: name ?: preset.name
    val borderWidth = if (isSelected) 3.dp else 1.5.dp
    val borderColor = if (isSelected) NisAccentPrimary else NisBorder

    val baseModifier = modifier
        .size(size)
        .clip(CircleShape)
        .then(
            if (onClick != null) {
                Modifier.nisPressable(
                    role = Role.Button,
                    onClickLabel = desc,
                    shape = CircleShape,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center
    ) {
        // Gradient circle background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(preset.gradientColors), CircleShape)
                .border(borderWidth, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val emojiStyle = when {
                size >= 90.dp -> MaterialTheme.typography.displayMedium
                size >= 64.dp -> MaterialTheme.typography.headlineMedium
                size >= 40.dp -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.bodySmall
            }

            Text(
                text = preset.emoji,
                style = emojiStyle,
                color = NisTextPrimary
            )
        }

        // Edit overlay when manage mode is active
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NisScrimMedium, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (size >= 80.dp) 36.dp else 24.dp)
                        .background(NisSurfaceHigh, CircleShape)
                        .border(1.dp, NisAccentPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = NisAccentPrimary,
                        modifier = Modifier.size(if (size >= 80.dp) 20.dp else 14.dp)
                    )
                }
            }
        }

        // Lock indicator badge
        if (isLocked && !isEditMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(if (size >= 80.dp) 26.dp else 18.dp)
                    .background(NisBackground, CircleShape)
                    .border(1.dp, NisBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Profile Locked",
                    tint = NisAccentAmber,
                    modifier = Modifier.size(if (size >= 80.dp) 14.dp else 10.dp)
                )
            }
        }

        // Kids indicator badge
        if (isKids && !isEditMode && !isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(if (size >= 80.dp) 24.dp else 16.dp)
                    .background(NisLiveEmerald, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "K",
                    style = MaterialTheme.typography.labelSmall,
                    color = NisBackground
                )
            }
        }
    }
}
