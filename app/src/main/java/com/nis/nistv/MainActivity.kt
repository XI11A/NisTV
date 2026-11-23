@file:OptIn(ExperimentalSharedTransitionApi::class)
package com.nis.nistv

import android.animation.ObjectAnimator
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Rational
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.nis.nistv.core.designsystem.component.AfdsAuthGateDialog
import com.nis.nistv.core.designsystem.component.AppUpdateDialog
import com.nis.nistv.core.designsystem.component.LanguageOnboardingDialog
import com.nis.nistv.core.designsystem.theme.NisBackground
import com.nis.nistv.core.designsystem.theme.NisMotion
import com.nis.nistv.core.designsystem.theme.NisTvTheme
import com.nis.nistv.core.designsystem.theme.isReducedMotionEnabled
import com.nis.nistv.core.designsystem.transition.LocalSharedTransitionScope
import com.nis.nistv.core.preferences.AppThemeConfig
import com.nis.nistv.core.designsystem.transition.SharedOrigins
import com.nis.nistv.core.navigation.NisBottomNavigationBar
import com.nis.nistv.core.navigation.Screen
import com.nis.nistv.core.navigation.nisComposable
import com.nis.nistv.core.update.AppUpdateChecker
import com.nis.nistv.core.update.UpdateInfo
import com.nis.nistv.data.tmdb.model.TmdbMediaType
import com.nis.nistv.data.tmdb.model.toPreview
import com.nis.nistv.feature.details.MediaDetailsScreen
import com.nis.nistv.feature.discover.DiscoverScreen
import com.nis.nistv.feature.downloads.DownloadsScreen
import com.nis.nistv.feature.downloads.OfflineDetailsScreen
import com.nis.nistv.feature.downloads.OfflinePlayerScreen
import com.nis.nistv.feature.favorites.FavoritesScreen
import com.nis.nistv.feature.home.HomeScreen
import com.nis.nistv.feature.livetv.LiveTvScreen
import com.nis.nistv.feature.player.PlayerScreen
import com.nis.nistv.feature.player.components.NisMiniPlayer
import com.nis.nistv.feature.profile.ProfileScreen
import com.nis.nistv.feature.search.SearchScreen
import com.nis.nistv.feature.splash.SplashScreen
import com.nis.nistv.feature.splash.SplashTimeline
import com.nis.nistv.playback.PlaybackStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var isInPipMode by mutableStateOf(false)
    private var pendingNotificationChannelId by mutableStateOf<String?>(null)
    private var pendingShortcutDestination by mutableStateOf<String?>(null)

    override fun onStop() {
        super.onStop()
        (application as? NisTvApplication)?.container?.parentalControlManager?.onAppBackgrounded()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("channelId")?.let {
            pendingNotificationChannelId = it
        }
        intent.getStringExtra("shortcut_destination")?.let {
            pendingShortcutDestination = it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        intent?.getStringExtra("channelId")?.let {
            pendingNotificationChannelId = it
        }
        intent?.getStringExtra("shortcut_destination")?.let {
            pendingShortcutDestination = it
        }

        var firstSplashFrameReady = false
        val splashStartMs = SystemClock.elapsedRealtime()
        splashScreen.setKeepOnScreenCondition {
            !firstSplashFrameReady && (SystemClock.elapsedRealtime() - splashStartMs < 300L)
        }

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val fadeOut = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.ALPHA,
                1f,
                0f
            )
            fadeOut.duration = 150L
            fadeOut.interpolator = DecelerateInterpolator()
            fadeOut.doOnEnd { splashScreenView.remove() }
            fadeOut.start()
        }

        enableEdgeToEdge()
        setContent {
            SideEffect {
                firstSplashFrameReady = true
            }
            val app = applicationContext as NisTvApplication
            val appTheme by app.container.userPreferences.appTheme.collectAsState(initial = AppThemeConfig.DARK)
            NisTvTheme(appTheme = appTheme) {
                NisTvAppNav(
                    isInPipMode = isInPipMode,
                    pendingNotificationChannelId = pendingNotificationChannelId,
                    onClearNotificationChannel = { pendingNotificationChannelId = null },
                    pendingShortcutDestination = pendingShortcutDestination,
                    onClearShortcutDestination = { pendingShortcutDestination = null },
                    onUpdatePipParams = { shouldAutoPip ->
                        updatePipParams(shouldAutoPip)
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            val app = applicationContext as NisTvApplication
            app.container.playerController.stop()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val app = applicationContext as NisTvApplication
        val uiState = app.container.playerController.uiState.value
        val isWatching = uiState.channel != null && (uiState.isPlaying || uiState.isBuffering)
        if (isWatching && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val pipParams = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(pipParams)
            } catch (_: Exception) {}
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    private fun updatePipParams(shouldAutoPip: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val pipParams = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .setAutoEnterEnabled(shouldAutoPip)
                    .build()
                setPictureInPictureParams(pipParams)
            } catch (_: Exception) {}
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun NisTvAppNav(
    isInPipMode: Boolean = false,
    pendingNotificationChannelId: String? = null,
    onClearNotificationChannel: () -> Unit = {},
    pendingShortcutDestination: String? = null,
    onClearShortcutDestination: () -> Unit = {},
    onUpdatePipParams: (Boolean) -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    val reducedMotion = remember(context) { isReducedMotionEnabled(context) }
    val app = context.applicationContext as NisTvApplication
    val userPreferences = app.container.userPreferences
    val googleAuthManager = app.container.googleAuthManager

    val isAppLoggedIn by userPreferences.isAppLoggedIn.collectAsState(initial = false)
    val googleUser by userPreferences.googleUser.collectAsState(initial = null)
    val currentUiState by app.container.playerController.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showAuthGateDialog by remember { mutableStateOf(false) }
    var pendingPlayChannelId by remember { mutableStateOf<String?>(null) }
    var demoSeconds by remember { mutableIntStateOf(60) }

    // Dynamic Update Checker
    val updateChecker = remember { AppUpdateChecker() }
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        try {
            val update = updateChecker.checkForUpdate(BuildConfig.VERSION_CODE)
            if (update != null) {
                availableUpdate = update
            }
        } catch (_: Exception) {}
    }

    val isPlayer = currentRoute?.startsWith("player/") == true || currentRoute?.startsWith("offline_player/") == true
    val isOfflineExempt = isPlayer || currentRoute?.startsWith("offline_details/") == true

    // Offline Network Detection: Automatically redirect to Downloads if mobile is offline
    val isOnline by app.container.networkMonitor.isOnline.collectAsState(initial = true)
    LaunchedEffect(isOnline) {
        if (!isOnline && currentRoute != Screen.Downloads.route && !isOfflineExempt) {
            navController.navigate(Screen.Downloads.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            Toast.makeText(context, "Offline mode: Showing your downloaded media", Toast.LENGTH_SHORT).show()
        }
    }

    // PiP Auto-Navigate to Player Screen if active channel is playing
    LaunchedEffect(isInPipMode) {
        if (isInPipMode) {
            val activeChannel = currentUiState.channel
            if (activeChannel != null && !isPlayer) {
                navController.navigate(Screen.Player.createRoute(activeChannel.id)) {
                    launchSingleTop = true
                }
            }
        }
    }

    // Picture-in-Picture Auto-Enter state updater
    LaunchedEffect(currentUiState.isPlaying, currentUiState.channel, currentUiState.isBuffering) {
        val shouldAutoPip = currentUiState.channel != null && 
            (currentUiState.isPlaying || currentUiState.isBuffering)
        onUpdatePipParams(shouldAutoPip)
    }

    // First-Launch Language Selection Trigger
    val hasSelectedLanguage by userPreferences.hasSelectedLanguage.collectAsState(initial = true)
    val preferredLanguage by userPreferences.preferredLanguage.collectAsState(initial = "Tamil")
    var showLanguageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(hasSelectedLanguage) {
        if (!hasSelectedLanguage) {
            showLanguageDialog = true
        }
    }

    // Demo Mode 1-Minute Grace Timer Listener
    LaunchedEffect(googleUser?.isDemo, googleUser?.demoExpiresAt) {
        val user = googleUser
        if (user != null && user.isDemo) {
            val expiresAt = user.demoExpiresAt
            while (true) {
                val diff = ((expiresAt - System.currentTimeMillis()) / 1000).toInt()
                demoSeconds = diff.coerceAtLeast(0)
                if (diff <= 0) {
                    app.container.playerController.stop()
                    userPreferences.clearGoogleUser()
                    break
                }
                delay(1000)
            }
        }
    }

    // Auto-stop playback when logged out
    LaunchedEffect(isAppLoggedIn) {
        if (!isAppLoggedIn && app.container.playerController.uiState.value.channel != null) {
            app.container.playerController.stop()
        }
    }

    val isDetails = currentRoute?.startsWith("details/") == true
    val isFullscreenOverlay = currentRoute == Screen.Guide.route ||
            currentRoute == Screen.Playlists.route ||
            currentRoute == Screen.HistoryPrivacy.route ||
            currentRoute == Screen.ParentalControls.route ||
            currentRoute == Screen.BackupRestore.route ||
            currentRoute == Screen.Profiles.route ||
            currentRoute?.startsWith("profile_edit/") == true
    val showMiniPlayer = !isPlayer && !isFullscreenOverlay && !isInPipMode && currentRoute != Screen.Splash.route && currentUiState.channel != null && currentUiState.status != PlaybackStatus.IDLE
    val showBottomBar = !isPlayer && !isDetails && !isFullscreenOverlay && !isInPipMode && currentRoute != Screen.Splash.route

    // Gate: User MUST be logged in with AFDS account to watch content
    val handleWatchRequest: (String) -> Unit = { channelId ->
        if (isAppLoggedIn) {
            navController.navigate(Screen.Player.createRoute(channelId))
        } else {
            pendingPlayChannelId = channelId
            showAuthGateDialog = true
        }
    }

    LaunchedEffect(pendingNotificationChannelId) {
        pendingNotificationChannelId?.let { chId ->
            handleWatchRequest(chId)
            onClearNotificationChannel()
        }
    }

    LaunchedEffect(pendingShortcutDestination) {
        pendingShortcutDestination?.let { destination ->
            when (destination) {
                "livetv" -> navController.navigate(Screen.LiveTv.route) { launchSingleTop = true }
                "search" -> navController.navigate(Screen.Search.route) { launchSingleTop = true }
                "downloads" -> navController.navigate(Screen.Downloads.route) { launchSingleTop = true }
                "player" -> {
                    pendingNotificationChannelId?.let { chId ->
                        handleWatchRequest(chId)
                        onClearNotificationChannel()
                    }
                }
            }
            onClearShortcutDestination()
        }
    }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    AnimatedVisibility(
                        visible = showBottomBar,
                        enter = if (reducedMotion) {
                            fadeIn(tween(NisMotion.DurationFastMs, easing = NisMotion.EasingStandard))
                        } else {
                            slideInVertically(
                                animationSpec = tween(NisMotion.DurationNormalMs, easing = NisMotion.EasingEmphasized),
                                initialOffsetY = { it }
                            ) + fadeIn(tween(NisMotion.DurationFastMs, easing = NisMotion.EasingStandard))
                        },
                        exit = if (reducedMotion) {
                            fadeOut(tween(NisMotion.DurationFastMs, easing = NisMotion.EasingStandard))
                        } else {
                            slideOutVertically(
                                animationSpec = tween(NisMotion.DurationNormalMs, easing = NisMotion.EasingEmphasized),
                                targetOffsetY = { it }
                            ) + fadeOut(tween(NisMotion.DurationFastMs, easing = NisMotion.EasingStandard))
                        }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            AnimatedVisibility(
                                visible = showMiniPlayer,
                                enter = if (reducedMotion) {
                                    fadeIn(tween(NisMotion.DurationFastMs, easing = NisMotion.EasingStandard))
                                } else {
                                    slideInVertically(
                                        animationSpec = tween(NisMotion.DurationNormalMs, easing = NisMotion.EasingEmphasized),
                                        initialOffsetY = { it }
                                    ) + fadeIn(tween(NisMotion.DurationNormalMs, easing = NisMotion.EasingStandard))
                                },
                                exit = if (reducedMotion) {
                                    fadeOut(tween(NisMotion.DurationFastMs, easing = NisMotion.EasingStandard))
                                } else {
                                    slideOutVertically(
                                        animationSpec = tween(NisMotion.DurationNormalMs, easing = NisMotion.EasingEmphasized),
                                        targetOffsetY = { it }
                                    ) + fadeOut(tween(NisMotion.DurationFastMs, easing = NisMotion.EasingStandard))
                                }
                            ) {
                                currentUiState.channel?.let { activeChannel ->
                                    NisMiniPlayer(
                                        channel = activeChannel,
                                        programmeTitle = currentUiState.programmeTitle,
                                        isPlaying = currentUiState.isPlaying,
                                        isBuffering = currentUiState.isBuffering,
                                        exoPlayer = app.container.playerController.player,
                                        onExpand = {
                                            navController.navigate(Screen.Player.createRoute(activeChannel.id))
                                        },
                                        onTogglePlayPause = {
                                            app.container.playerController.togglePlayPause()
                                        },
                                        onClose = {
                                            app.container.playerController.stop()
                                        },
                                        isDemo = googleUser?.isDemo == true,
                                        demoRemainingSeconds = demoSeconds
                                    )
                                }
                            }

                            NisBottomNavigationBar(
                                currentRoute = currentRoute,
                                onNavigate = { screen ->
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            ) { _ ->
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(
                            route = Screen.Splash.route,
                            exitTransition = {
                                fadeOut(animationSpec = tween(SplashTimeline.EXIT_MS, easing = FastOutSlowInEasing))
                            }
                        ) {
                            SplashScreen(
                                onSplashFinished = {
                                    coroutineScope.launch {
                                        val profs = app.container.database.profileDao().getAllProfilesSync()
                                        val askOnLaunch = userPreferences.askProfileOnLaunch.firstOrNull() ?: (profs.size > 1)
                                        val dest = if (profs.size > 1 && askOnLaunch) Screen.Profiles.route else Screen.Home.route
                                        navController.navigate(dest) {
                                            popUpTo(Screen.Splash.route) {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }
                        nisComposable(Screen.Home.route) {
                            HomeScreen(
                                onChannelClick = handleWatchRequest,
                                onContinueWatchingClick = { downloadId ->
                                    navController.navigate(Screen.OfflinePlayer.createRoute(downloadId))
                                },
                                onMediaClick = { preview, origin ->
                                    app.container.mediaPreviewStore.put(preview)
                                    preview.posterUrl?.let { pUrl ->
                                        SingletonImageLoader.get(context).enqueue(
                                            ImageRequest.Builder(context)
                                                .data(pUrl)
                                                .memoryCacheKey(preview.posterCacheKey)
                                                .build()
                                        )
                                    }
                                    preview.backdropUrl?.let { bUrl ->
                                        SingletonImageLoader.get(context).enqueue(
                                            ImageRequest.Builder(context)
                                                .data(bUrl)
                                                .memoryCacheKey(preview.backdropCacheKey)
                                                .build()
                                        )
                                    }
                                    navController.navigate(
                                        Screen.MediaDetails.createRoute(
                                            mediaType = preview.mediaType,
                                            mediaId = preview.tmdbId.toLong(),
                                            origin = origin
                                        )
                                    ) {
                                        launchSingleTop = true
                                    }
                                },
                                onSearchClick = {
                                    navController.navigate(Screen.Search.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onProfileClick = {
                                    navController.navigate(Screen.Profile.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onManageProfiles = {
                                    navController.navigate(Screen.Profiles.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onFavoritesClick = {
                                    navController.navigate(Screen.Favorites.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        nisComposable(Screen.Discover.route) {
                            DiscoverScreen(
                                onMediaClick = { preview, origin ->
                                    app.container.mediaPreviewStore.put(preview)
                                    preview.posterUrl?.let { pUrl ->
                                        SingletonImageLoader.get(context).enqueue(
                                            ImageRequest.Builder(context)
                                                .data(pUrl)
                                                .memoryCacheKey(preview.posterCacheKey)
                                                .build()
                                        )
                                    }
                                    preview.backdropUrl?.let { bUrl ->
                                        SingletonImageLoader.get(context).enqueue(
                                            ImageRequest.Builder(context)
                                                .data(bUrl)
                                                .memoryCacheKey(preview.backdropCacheKey)
                                                .build()
                                        )
                                    }
                                    navController.navigate(
                                        Screen.MediaDetails.createRoute(
                                            mediaType = preview.mediaType,
                                            mediaId = preview.tmdbId.toLong(),
                                            origin = origin
                                        )
                                    ) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(Screen.LiveTv.route) {
                            LiveTvScreen(
                                onChannelClick = handleWatchRequest,
                                onOpenGuide = {
                                    navController.navigate(Screen.Guide.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenPlaylists = {
                                    navController.navigate(Screen.Playlists.route) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(Screen.Downloads.route) {
                            DownloadsScreen(
                                onPlayDownload = { downloadId ->
                                    navController.navigate(Screen.OfflinePlayer.createRoute(downloadId))
                                },
                                onOpenOfflineDetails = { mediaType, mediaId ->
                                    navController.navigate(Screen.OfflineDetails.createRoute(mediaType, mediaId))
                                }
                            )
                        }
                        composable(
                            route = Screen.OfflineDetails.route,
                            arguments = listOf(
                                navArgument("mediaType") { type = NavType.StringType },
                                navArgument("mediaId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
                            val mediaId = backStackEntry.arguments?.getString("mediaId") ?: "0"
                            OfflineDetailsScreen(
                                mediaType = mediaType,
                                mediaId = mediaId,
                                onBack = { navController.popBackStack() },
                                onPlayDownload = { downloadId ->
                                    navController.navigate(Screen.OfflinePlayer.createRoute(downloadId))
                                }
                            )
                        }
                        composable(
                            route = Screen.OfflinePlayer.route,
                            arguments = listOf(
                                navArgument("downloadId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val downloadId = backStackEntry.arguments?.getString("downloadId") ?: ""
                            OfflinePlayerScreen(
                                downloadId = downloadId,
                                onBack = { navController.popBackStack() },
                                isInPipMode = isInPipMode,
                                onPlayOther = { nextId ->
                                    navController.navigate(Screen.OfflinePlayer.createRoute(nextId)) {
                                        popUpTo(Screen.Downloads.route)
                                    }
                                }
                            )
                        }
                        nisComposable(Screen.Search.route) {
                            SearchScreen(
                                onChannelClick = handleWatchRequest,
                                onMediaClick = { preview, origin ->
                                    app.container.mediaPreviewStore.put(preview)
                                    preview.posterUrl?.let { pUrl ->
                                        SingletonImageLoader.get(context).enqueue(
                                            ImageRequest.Builder(context)
                                                .data(pUrl)
                                                .memoryCacheKey(preview.posterCacheKey)
                                                .build()
                                        )
                                    }
                                    preview.backdropUrl?.let { bUrl ->
                                        SingletonImageLoader.get(context).enqueue(
                                            ImageRequest.Builder(context)
                                                .data(bUrl)
                                                .memoryCacheKey(preview.backdropCacheKey)
                                                .build()
                                        )
                                    }
                                    navController.navigate(
                                        Screen.MediaDetails.createRoute(
                                            mediaType = preview.mediaType,
                                            mediaId = preview.tmdbId.toLong(),
                                            origin = origin
                                        )
                                    ) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        nisComposable(Screen.Favorites.route) {
                            FavoritesScreen(
                                onChannelClick = handleWatchRequest,
                                onExploreClick = {
                                    navController.navigate(Screen.LiveTv.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onMediaClick = { preview, origin ->
                                    app.container.mediaPreviewStore.put(preview)
                                    preview.posterUrl?.let { pUrl ->
                                        SingletonImageLoader.get(context).enqueue(
                                            ImageRequest.Builder(context)
                                                .data(pUrl)
                                                .memoryCacheKey(preview.posterCacheKey)
                                                .build()
                                        )
                                    }
                                    preview.backdropUrl?.let { bUrl ->
                                        SingletonImageLoader.get(context).enqueue(
                                            ImageRequest.Builder(context)
                                                .data(bUrl)
                                                .memoryCacheKey(preview.backdropCacheKey)
                                                .build()
                                        )
                                    }
                                    navController.navigate(
                                        Screen.MediaDetails.createRoute(
                                            mediaType = preview.mediaType,
                                            mediaId = preview.tmdbId.toLong(),
                                            origin = origin
                                        )
                                    ) {
                                        launchSingleTop = true
                                    }
                                },
                                onExploreMediaClick = {
                                    navController.navigate(Screen.Discover.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        composable(Screen.Profile.route) {
                            ProfileScreen(
                                onNavigateToPlaylists = {
                                    navController.navigate(Screen.Playlists.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToHistoryPrivacy = {
                                    navController.navigate(Screen.HistoryPrivacy.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToParentalControls = {
                                    navController.navigate(Screen.ParentalControls.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToBackupRestore = {
                                    navController.navigate(Screen.BackupRestore.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToProfiles = {
                                    navController.navigate(Screen.Profiles.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToProfileEdit = { profileId ->
                                    navController.navigate(Screen.ProfileEdit.createRoute(profileId)) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(Screen.Profiles.route) {
                            com.nis.nistv.feature.profile.ProfilesScreen(
                                fromLaunch = (navController.previousBackStackEntry == null || navController.previousBackStackEntry?.destination?.route == Screen.Splash.route),
                                onProfileSelected = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Profiles.route) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                },
                                onEditProfile = { profileId ->
                                    navController.navigate(Screen.ProfileEdit.createRoute(profileId)) {
                                        launchSingleTop = true
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = Screen.ProfileEdit.route,
                            arguments = listOf(
                                navArgument("profileId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val profileId = backStackEntry.arguments?.getString("profileId") ?: "new"
                            com.nis.nistv.feature.profile.ProfileEditScreen(
                                profileId = profileId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.HistoryPrivacy.route) {
                            val histVm: com.nis.nistv.feature.history.HistoryAndPrivacyViewModel = viewModel(
                                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                        return com.nis.nistv.feature.history.HistoryAndPrivacyViewModel(
                                            database = app.container.database,
                                            userPreferences = app.container.userPreferences,
                                            recommendationEngine = app.container.recommendationEngine
                                        ) as T
                                    }
                                }
                            )
                            com.nis.nistv.feature.history.HistoryAndPrivacyScreen(
                                viewModel = histVm,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToChannel = handleWatchRequest,
                                onNavigateToDownload = { downloadId ->
                                    navController.navigate(Screen.OfflinePlayer.createRoute(downloadId))
                                }
                            )
                        }
                        composable(Screen.ParentalControls.route) {
                            val parVm: com.nis.nistv.feature.parental.ParentalControlsViewModel = viewModel(
                                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                        return com.nis.nistv.feature.parental.ParentalControlsViewModel(
                                            database = app.container.database,
                                            userPreferences = app.container.userPreferences,
                                            parentalControlManager = app.container.parentalControlManager
                                        ) as T
                                    }
                                }
                            )
                            com.nis.nistv.feature.parental.ParentalControlsScreen(
                                viewModel = parVm,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.BackupRestore.route) {
                            val backVm: com.nis.nistv.feature.backup.BackupRestoreViewModel = viewModel(
                                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                        return com.nis.nistv.feature.backup.BackupRestoreViewModel(
                                            backupRestoreManager = app.container.backupRestoreManager
                                        ) as T
                                    }
                                }
                            )
                            com.nis.nistv.feature.backup.BackupRestoreScreen(
                                viewModel = backVm,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Playlists.route) {
                            com.nis.nistv.feature.livetv.playlist.PlaylistsScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Guide.route) {
                            com.nis.nistv.feature.guide.GuideScreen(
                                onBackClick = { navController.popBackStack() },
                                onChannelClick = handleWatchRequest
                            )
                        }
                        nisComposable(
                            route = Screen.MediaDetails.route,
                            arguments = listOf(
                                navArgument("mediaType") { type = NavType.StringType },
                                navArgument("mediaId") { type = NavType.LongType },
                                navArgument("origin") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                }
                            )
                        ) { backStackEntry ->
                            val typeStr = backStackEntry.arguments?.getString("mediaType") ?: "movie"
                            val id = backStackEntry.arguments?.getLong("mediaId") ?: 0L
                            val origin = backStackEntry.arguments?.getString("origin").orEmpty()
                            val mediaType = if (typeStr == "tv") TmdbMediaType.TV else TmdbMediaType.MOVIE
                            MediaDetailsScreen(
                                mediaType = mediaType,
                                mediaId = id,
                                origin = origin,
                                onBack = { navController.popBackStack() },
                                onSimilarClick = { nextType, nextId ->
                                    navController.navigate(
                                        Screen.MediaDetails.createRoute(
                                            mediaType = nextType.value,
                                            mediaId = nextId,
                                            origin = SharedOrigins.SIMILAR
                                        )
                                    ) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                composable(
                    route = Screen.Player.route,
                    arguments = listOf(navArgument("channelId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                    if (!isAppLoggedIn) {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                            pendingPlayChannelId = channelId
                            showAuthGateDialog = true
                        }
                    } else {
                        PlayerScreen(
                            channelId = channelId,
                            onBack = { navController.popBackStack() },
                            isInPipMode = isInPipMode
                        )
                    }
                }
            }

            // Fallback video canvas while navigating to PlayerScreen in PiP mode
            if (isInPipMode && !isPlayer && currentUiState.channel != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = app.container.playerController.player
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        update = { view ->
                            view.player = app.container.playerController.player
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
}

    // AFDS Auth Gate Modal Sheet
    if (showAuthGateDialog && !isInPipMode) {
        AfdsAuthGateDialog(
            afdsAuthService = app.container.afdsAuthService,
            googleAuthManager = googleAuthManager,
            onSuccess = {
                showAuthGateDialog = false
                pendingPlayChannelId?.let { targetChannelId ->
                    navController.navigate(Screen.Player.createRoute(targetChannelId))
                    pendingPlayChannelId = null
                }
            },
            onDismiss = {
                showAuthGateDialog = false
                pendingPlayChannelId = null
            }
        )
    }

    // First-Launch Indian TV Language Selection Dialog
    if (showLanguageDialog && !isInPipMode) {
        LanguageOnboardingDialog(
            initialLanguage = preferredLanguage,
            onLanguageSelected = { selectedLang ->
                coroutineScope.launch {
                    userPreferences.setPreferredLanguage(selectedLang)
                    userPreferences.setHasSelectedLanguage(true)
                    showLanguageDialog = false
                }
            }
        )
    }

    // Dynamic In-App Update Dialog
    availableUpdate?.let { updateInfo ->
        if (!isInPipMode) {
            AppUpdateDialog(
                updateInfo = updateInfo,
                onUpdate = {
                    updateChecker.startDownloadAndInstall(context, updateInfo)
                },
                onDismiss = {
                    if (!updateInfo.forceUpdate) {
                        availableUpdate = null
                    }
                }
            )
        }
    }
}
