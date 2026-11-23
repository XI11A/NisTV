package com.nis.nistv.core.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nis.nistv.core.designsystem.component.nisPressable
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.NisAccentPrimary
import com.nis.nistv.core.designsystem.theme.NisAccentPrimaryContainer
import com.nis.nistv.core.designsystem.theme.NisBorderSubtle
import com.nis.nistv.core.designsystem.theme.NisBottomSheetShape
import com.nis.nistv.core.designsystem.theme.NisDimens
import com.nis.nistv.core.designsystem.theme.NisMotion
import com.nis.nistv.core.designsystem.theme.NisNavbarBg
import com.nis.nistv.core.designsystem.theme.NisPillShape
import com.nis.nistv.core.designsystem.theme.NisShapes
import com.nis.nistv.core.designsystem.theme.NisTextMuted
import com.nis.nistv.core.designsystem.theme.NisTheme
import com.nis.nistv.core.designsystem.theme.isReducedMotionEnabled
import com.nis.nistv.core.designsystem.transition.LocalNavAnimatedVisibilityScope

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Splash : Screen("splash", "Splash", NisIcons.Play)
    data object Home : Screen("home", "Home", NisIcons.Home)
    data object Discover : Screen("discover", "Discover", NisIcons.Discover)
    data object LiveTv : Screen("livetv", "Live TV", NisIcons.LiveTv)
    data object Downloads : Screen("downloads", "Downloads", NisIcons.Download)
    data object Search : Screen("search", "Search", NisIcons.Search)
    data object Favorites : Screen("favorites", "My List", NisIcons.Favorites)
    data object Profile : Screen("profile", "Settings", NisIcons.Profile)

    data object Player : Screen("player/{channelId}", "Player", NisIcons.Play) {
        fun createRoute(channelId: String): String = "player/$channelId"
    }

    data object MediaDetails : Screen("details/{mediaType}/{mediaId}?origin={origin}", "Details", NisIcons.Info) {
        fun createRoute(mediaType: String, mediaId: Long, origin: String = ""): String =
            if (origin.isBlank()) "details/$mediaType/$mediaId"
            else {
                val encoded = java.net.URLEncoder.encode(origin, "UTF-8").replace("+", "%20")
                "details/$mediaType/$mediaId?origin=$encoded"
            }

        fun createRoute(mediaType: String, mediaId: Int, origin: String = ""): String =
            createRoute(mediaType, mediaId.toLong(), origin)
    }

    data object OfflinePlayer : Screen("offline_player/{downloadId}", "Offline Player", NisIcons.Play) {
        fun createRoute(downloadId: String): String = "offline_player/$downloadId"
    }

    data object OfflineDetails : Screen("offline_details/{mediaType}/{mediaId}", "Offline Details", NisIcons.Info) {
        fun createRoute(mediaType: String, mediaId: String): String = "offline_details/$mediaType/$mediaId"
        fun createRoute(mediaType: String, mediaId: Long): String = "offline_details/$mediaType/$mediaId"
    }

    data object Playlists : Screen("playlists", "My Playlists", NisIcons.Playlists)

    data object Guide : Screen("guide", "TV Guide", NisIcons.LiveTv)

    data object HistoryPrivacy : Screen("history_privacy", "History & Privacy", NisIcons.Info)

    data object ParentalControls : Screen("parental_controls", "Parental Controls", NisIcons.Info)

    data object BackupRestore : Screen("backup_restore", "Backup & Restore", NisIcons.Download)

    data object Profiles : Screen("profiles", "Profiles", NisIcons.Profile)

    data object ProfileEdit : Screen("profile_edit/{profileId}", "Edit Profile", NisIcons.Profile) {
        fun createRoute(profileId: String): String = "profile_edit/$profileId"
    }

    companion object {
        val bottomNavItems = listOf(Home, Discover, LiveTv, Downloads, Profile)
    }
}

/**
 * Cinematic Obsidian 2.0 Bottom Navigation Bar:
 * - 94% alpha translucent background (NisNavbarBg)
 * - 24dp top corner radius (NisBottomSheetShape)
 * - 1dp NisBorderSubtle top border
 * - Active tab: 28x3dp pill indicator in NisAccentPrimaryContainer, primary icon & label
 * - Inactive tab: WCAG AA compliant NisTextMuted
 * - 64dp standard bar height + system navigation bars inset
 */
@Composable
fun NisBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
    val colors = NisTheme.colors

    val glassBackground = Brush.verticalGradient(
        listOf(
            colors.surfaceElevated.copy(alpha = 0.72f),
            colors.surface.copy(alpha = 0.60f),
            colors.background.copy(alpha = 0.76f)
        )
    )
    val glassBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.42f),
            colors.primary.copy(alpha = 0.35f),
            colors.borderSubtle.copy(alpha = 0.15f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(NisPillShape)
                .background(glassBackground)
                .border(
                    width = 1.2.dp,
                    brush = glassBorder,
                    shape = NisPillShape
                )
                .padding(horizontal = 6.dp, vertical = 5.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) colors.primary else colors.textMuted,
                        animationSpec = if (reducedMotion) snap() else NisMotion.tabIndicatorSpec(),
                        label = "bottomNavColor"
                    )
                    val indicatorWidth by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 0.dp,
                        animationSpec = if (reducedMotion) snap() else NisMotion.tabIndicatorSpec(),
                        label = "bottomNavIndicatorWidth"
                    )
                    val indicatorAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = if (reducedMotion) snap() else NisMotion.tabIndicatorSpec(),
                        label = "bottomNavIndicatorAlpha"
                    )

                    val activePillBrush = Brush.horizontalGradient(
                        listOf(
                            colors.accentCyan.copy(alpha = 0.22f),
                            colors.primary.copy(alpha = 0.30f)
                        )
                    )
                    val activePillBorder = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.45f),
                            colors.primary.copy(alpha = 0.35f)
                        )
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(NisPillShape)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .background(activePillBrush, NisPillShape)
                                        .border(1.dp, activePillBorder, NisPillShape)
                                } else Modifier
                            )
                            .nisPressable(
                                onClick = { onNavigate(screen) },
                                role = Role.Tab,
                                onClickLabel = screen.title,
                                shape = NisPillShape
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Selected indicator pill with smooth width & alpha transition
                        Box(
                            modifier = Modifier
                                .size(width = indicatorWidth, height = 3.dp)
                                .graphicsLayer { alpha = indicatorAlpha }
                                .clip(NisPillShape)
                                .background(colors.primary)
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            modifier = Modifier.size(NisDimens.IconMedium),
                            tint = contentColor
                        )

                        Text(
                            text = screen.title,
                            color = contentColor,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Navigation destination wrapper supporting shared element transitions with custom enter/exit
 * fades and providing LocalNavAnimatedVisibilityScope for shared bounds.
 */
fun NavGraphBuilder.nisComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable(
    route = route,
    arguments = arguments,
    enterTransition = { fadeIn(tween(220)) },
    exitTransition = { fadeOut(tween(160)) },
    popEnterTransition = { fadeIn(tween(220)) },
    popExitTransition = { fadeOut(tween(160)) },
) { entry ->
    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
        content(entry)
    }
}
