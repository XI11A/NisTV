package com.nis.nistv.feature.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.nis.nistv.NisTvApplication
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.designsystem.component.NisErrorState
import com.nis.nistv.core.designsystem.component.NisIconButton
import com.nis.nistv.core.designsystem.component.NisLiveBadge
import com.nis.nistv.core.designsystem.component.NisResolutionBadge
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.NisAccentAmber
import com.nis.nistv.core.designsystem.theme.NisAccentCyan
import com.nis.nistv.core.designsystem.theme.NisAccentPrimary
import com.nis.nistv.core.designsystem.theme.NisBackground
import com.nis.nistv.core.designsystem.theme.NisBadgeShape
import com.nis.nistv.core.designsystem.theme.NisBorder
import com.nis.nistv.core.designsystem.theme.NisCardBorder
import com.nis.nistv.core.designsystem.theme.NisDialogShape
import com.nis.nistv.core.designsystem.theme.NisDivider
import com.nis.nistv.core.designsystem.theme.NisLiveRed
import com.nis.nistv.core.designsystem.theme.NisMediaCardShape
import com.nis.nistv.core.designsystem.theme.NisPillShape
import com.nis.nistv.core.designsystem.theme.NisScrimMedium
import com.nis.nistv.core.designsystem.theme.NisScrimSoft
import com.nis.nistv.core.designsystem.theme.NisScrimStrong
import com.nis.nistv.core.designsystem.theme.NisShapes
import com.nis.nistv.core.designsystem.theme.NisSuccess
import com.nis.nistv.core.designsystem.theme.NisSurface
import com.nis.nistv.core.designsystem.theme.NisSurfaceElevated
import com.nis.nistv.core.designsystem.theme.NisTextMuted
import com.nis.nistv.core.designsystem.theme.NisTextPrimary
import com.nis.nistv.core.designsystem.theme.NisTextSecondary
import com.nis.nistv.core.designsystem.theme.NisTheme
import com.nis.nistv.core.designsystem.theme.NisThumbnailShape
import com.nis.nistv.core.designsystem.theme.NisWarning
import com.nis.nistv.feature.player.components.BrightnessHud
import com.nis.nistv.feature.player.components.ChannelQuickDrawer
import com.nis.nistv.feature.player.components.LiveEpgCard
import com.nis.nistv.feature.player.components.PlayerBrandingIntro
import com.nis.nistv.feature.player.components.PlayerGestureDetector
import com.nis.nistv.feature.player.components.VolumeHud
import com.nis.nistv.feature.player.components.rememberPlayerOrientationController
import com.nis.nistv.feature.player.controls.ChannelJumpDialog
import com.nis.nistv.feature.player.controls.ZapOverlay
import com.nis.nistv.feature.player.multiview.MultiViewChannelPicker
import com.nis.nistv.feature.player.multiview.MultiViewScreen
import com.nis.nistv.playback.AspectRatioMode
import com.nis.nistv.playback.PlaybackStatus
import com.nis.nistv.playback.multiview.MultiViewSlot
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channelId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isInPipMode: Boolean = false,
    viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.provideFactory(
            LocalContext.current,
            (LocalContext.current.applicationContext as NisTvApplication).container.channelRepository,
            (LocalContext.current.applicationContext as NisTvApplication).container.playerController,
            (LocalContext.current.applicationContext as NisTvApplication).container.epgManager,
            (LocalContext.current.applicationContext as NisTvApplication).container.userPreferences,
            channelId,
            (LocalContext.current.applicationContext as NisTvApplication).container.zapContext,
            (LocalContext.current.applicationContext as NisTvApplication).container.dataUsageManager
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()
    val relatedChannels by viewModel.relatedChannels.collectAsState()
    val programmeInfo by viewModel.programmeInfo.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val showBrightnessHud by viewModel.showBrightnessHud.collectAsState()
    val showVolumeHud by viewModel.showVolumeHud.collectAsState()
    val isLocked by viewModel.isLocked.collectAsState()
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()

    val context = LocalContext.current
    val activity = context as? Activity
    val app = context.applicationContext as NisTvApplication
    val playerController = remember { app.container.playerController }
    val exoPlayer = remember { playerController.player }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showSourceDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showCastDialog by remember { mutableStateOf(false) }
    var showTrackSelectorSheet by remember { mutableStateOf(false) }

    val castManager = remember { com.nis.nistv.core.cast.CastManager.getInstance(context) }
    val isCastConnected by castManager.isExternalDisplayConnected.collectAsState()

    val multiViewController = remember { app.container.multiViewController }
    val multiViewState by multiViewController.uiState.collectAsState()
    var showMultiViewPicker by remember { mutableStateOf(false) }
    var multiViewSlotToReplace by remember { mutableStateOf<MultiViewSlot?>(null) }
    var showChannelJumpDialog by remember { mutableStateOf(false) }

    val showZapHud by viewModel.showZapHud.collectAsState()
    val parentalEnabled by app.container.userPreferences.isParentalControlEnabled.collectAsState(initial = false)
    val lockedCategories by app.container.userPreferences.parentalLockedCategories.collectAsState(initial = emptySet())
    val isUnlocked by app.container.parentalControlManager.isTemporarilyUnlocked.collectAsState()
    val lockedChannelIds = remember(allChannels, parentalEnabled, lockedCategories, isUnlocked) {
        if (!parentalEnabled || isUnlocked) emptySet<String>()
        else allChannels.filter { com.nis.nistv.data.parental.ParentalControlManager.isChannelRestricted(it, parentalEnabled, isUnlocked, lockedCategories) }.map { it.id }.toSet()
    }
    val isAlreadyActiveChannel = remember(channelId) {
        val currentUi = playerController.uiState.value
        currentUi.channel?.id == channelId && currentUi.status != PlaybackStatus.IDLE
    }
    var showBrandingIntro by rememberSaveable(channelId) { mutableStateOf(!isAlreadyActiveChannel) }
    val zapContext = remember { app.container.zapContext }
    val autoListenEnabled by app.container.userPreferences.isAutoListenOnCellularScreenOffEnabled.collectAsState(initial = false)
    val networkMonitor = remember { app.container.networkMonitor }

    LaunchedEffect(showBrandingIntro) {
        playerController.setBrandingIntroActive(showBrandingIntro)
    }

    DisposableEffect(Unit) {
        onDispose {
            playerController.setBrandingIntroActive(false)
        }
    }

    val onOpenMultiView: () -> Unit = {
        if (multiViewController.isLowRamDevice()) {
            Toast.makeText(context, "Low memory device detected: Multi-View performance may vary", Toast.LENGTH_SHORT).show()
        }
        if (networkMonitor.isCurrentlyMetered()) {
            Toast.makeText(context, "Streaming 2 channels simultaneously on metered cellular data", Toast.LENGTH_SHORT).show()
        }
        multiViewSlotToReplace = null
        showMultiViewPicker = true
    }

    // Auto-Listen on metered cellular connection when screen turns off
    DisposableEffect(autoListenEnabled) {
        if (!autoListenEnabled) return@DisposableEffect onDispose {}
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    if (networkMonitor.isCurrentlyMetered() && app.container.playerController.player.isPlaying) {
                        app.container.playerController.setListenMode(true)
                    }
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    // Back handler for Multi-View: exits to regular single player
    BackHandler(enabled = multiViewState.isActive) {
        multiViewController.stop()
        app.container.playerController.play()
    }

    val googleUser by app.container.userPreferences.googleUser.collectAsState(initial = null)
    var remainingSeconds by remember { androidx.compose.runtime.mutableIntStateOf(60) }
    var showDemoExpiredDialog by remember { mutableStateOf(false) }

    // Demo Mode 1-Minute Grace Timer
    LaunchedEffect(googleUser?.isDemo, googleUser?.demoExpiresAt) {
        val user = googleUser
        if (user != null && user.isDemo) {
            val expiresAt = user.demoExpiresAt
            while (true) {
                val diff = ((expiresAt - System.currentTimeMillis()) / 1000).toInt()
                remainingSeconds = diff.coerceAtLeast(0)
                if (diff <= 0) {
                    app.container.playerController.stop()
                    multiViewController.stop()
                    app.container.userPreferences.clearGoogleUser()
                    showDemoExpiredDialog = true
                    break
                }
                delay(1000)
            }
        }
    }

    val isPipActive = isInPipMode || (activity?.isInPictureInPictureMode == true)

    val orientationController = rememberPlayerOrientationController(
        activity = activity,
        isLandscape = isLandscape,
        isLocked = isLocked,
        isInPip = isPipActive,
    )

    // Auto-hide controls after 4 seconds
    LaunchedEffect(uiState.isControlsVisible) {
        if (uiState.isControlsVisible && uiState.isPlaying && !isDrawerOpen) {
            delay(4000)
            viewModel.setControlsVisible(false)
        }
    }

    // Hardware Back Button: In landscape fullscreen, exit to portrait first
    BackHandler(enabled = isLandscape && !isPipActive) {
        orientationController.exitFullscreen()
    }

    // Hardware Back Button: In portrait mode with drawer open, close drawer
    BackHandler(enabled = !isLandscape && isDrawerOpen && !isPipActive) {
        viewModel.closeDrawer()
    }

    // Dynamic Immersive Fullscreen Controller for Landscape Mode
    DisposableEffect(isLandscape, isPipActive) {
        val window = activity?.window
        if (window != null && !isPipActive) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isLandscape) {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val win = activity?.window
            if (win != null && !isPipActive) {
                val controller = WindowCompat.getInsetsController(win, win.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Screen Keep-Awake & Full Lifecycle Cleanup on Departure
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.let { act ->
                val win = act.window
                val controller = WindowCompat.getInsetsController(win, win.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                val lp = win.attributes
                lp.screenBrightness = -1f
                win.attributes = lp
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (multiViewState.isActive) {
            MultiViewScreen(
                controller = multiViewController,
                onOpenFullscreen = { ch ->
                    multiViewController.stop()
                    viewModel.loadAndPlayChannel(ch.id)
                },
                onCloseMultiView = {
                    multiViewController.stop()
                    app.container.playerController.play()
                },
                onReplaceChannel = { slot ->
                    multiViewSlotToReplace = slot
                    showMultiViewPicker = true
                }
            )
        } else if (isPipActive) {
            // Edge-to-edge clean video canvas in PiP mode
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                },
                onRelease = { view ->
                    view.player = null
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (isLandscape) {
            // ==========================================
            // LANDSCAPE IMMERSIVE NETFLIX FULLSCREEN MODE
            // ==========================================
            LandscapePlayerLayout(
                exoPlayer = exoPlayer,
                uiState = uiState,
                programmeInfo = programmeInfo,
                isFavorite = isFavorite,
                isLocked = isLocked,
                brightness = brightness,
                volume = volume,
                showBrightnessHud = showBrightnessHud,
                showVolumeHud = showVolumeHud,
                sleepTimerMinutes = sleepTimerMinutes,
                onBack = { orientationController.exitFullscreen() },
                onToggleControls = { viewModel.setControlsVisible(!uiState.isControlsVisible) },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onPreviousChannel = {
                    showBrandingIntro = false
                    viewModel.playPreviousChannel(lockedChannelIds)
                },
                onNextChannel = {
                    showBrandingIntro = false
                    viewModel.playNextChannel(lockedChannelIds)
                },
                onToggleFavorite = { uiState.channel?.id?.let { viewModel.toggleFavorite(it) } },
                onCycleAspectRatio = { viewModel.cycleAspectRatio() },
                onToggleLock = { viewModel.toggleLock() },
                onOpenDrawer = { viewModel.openDrawer() },
                onOpenSourceDialog = { showSourceDialog = true },
                onOpenSleepTimer = { showSleepTimerDialog = true },
                onOpenTrackSelector = { showTrackSelectorSheet = true },
                onToggleDiagnostics = { viewModel.toggleDiagnostics() },
                onQuickSwitchPreviousChannel = {
                    showBrandingIntro = false
                    viewModel.quickSwitchPreviousChannel()
                },
                hasPreviousChannel = !uiState.previousChannelId.isNullOrBlank(),
                isCastConnected = isCastConnected,
                onOpenCastDialog = { showCastDialog = true },
                onAdjustBrightness = { delta -> viewModel.adjustBrightness(delta, activity) },
                onAdjustVolume = { delta -> viewModel.adjustVolume(delta) },
                onToggleFullscreen = {
                    orientationController.exitFullscreen()
                },
                onEnterPip = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            activity?.enterPictureInPictureMode(
                                PictureInPictureParams.Builder()
                                    .setAspectRatio(Rational(16, 9))
                                    .build()
                            )
                        } catch (_: Exception) {}
                    }
                },
                onRetry = { viewModel.retry() },
                isDemo = googleUser?.isDemo == true,
                remainingSeconds = remainingSeconds,
                onSwipeLeft = {
                    showBrandingIntro = false
                    viewModel.playNextChannel(lockedChannelIds)
                },
                onSwipeRight = {
                    showBrandingIntro = false
                    viewModel.playPreviousChannel(lockedChannelIds)
                },
                onToggleListenMode = { viewModel.toggleListenMode() },
                onOpenMultiView = onOpenMultiView,
                onOpenChannelJump = { showChannelJumpDialog = true },
                showZapHud = showZapHud,
                currentZapNumber = zapContext.getCurrentChannelNumber(),
                totalZapChannels = zapContext.getTotalChannels().coerceAtLeast(1),
                zapContextTitle = zapContext.getContextTitle(),
                onToggleLastChannel = {
                    showBrandingIntro = false
                    viewModel.toggleLastChannel(lockedChannelIds)
                },
                showBrandingIntro = showBrandingIntro,
                onDismissBrandingIntro = { showBrandingIntro = false }
            )
        } else {
            // ==========================================
            // PORTRAIT RICH STREAMING & EPG MODE
            // ==========================================
            PortraitPlayerLayout(
                exoPlayer = exoPlayer,
                uiState = uiState,
                programmeInfo = programmeInfo,
                relatedChannels = relatedChannels,
                isFavorite = isFavorite,
                isLocked = isLocked,
                brightness = brightness,
                volume = volume,
                showBrightnessHud = showBrightnessHud,
                showVolumeHud = showVolumeHud,
                sleepTimerMinutes = sleepTimerMinutes,
                onBack = onBack,
                onToggleControls = { viewModel.setControlsVisible(!uiState.isControlsVisible) },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onPreviousChannel = {
                    showBrandingIntro = false
                    viewModel.playPreviousChannel(lockedChannelIds)
                },
                onNextChannel = {
                    showBrandingIntro = false
                    viewModel.playNextChannel(lockedChannelIds)
                },
                onToggleFavorite = { uiState.channel?.id?.let { viewModel.toggleFavorite(it) } },
                onCycleAspectRatio = { viewModel.cycleAspectRatio() },
                onToggleLock = { viewModel.toggleLock() },
                onOpenDrawer = { viewModel.openDrawer() },
                onOpenSourceDialog = { showSourceDialog = true },
                onOpenSleepTimer = { showSleepTimerDialog = true },
                onOpenTrackSelector = { showTrackSelectorSheet = true },
                onToggleDiagnostics = { viewModel.toggleDiagnostics() },
                onQuickSwitchPreviousChannel = {
                    showBrandingIntro = false
                    viewModel.quickSwitchPreviousChannel()
                },
                hasPreviousChannel = !uiState.previousChannelId.isNullOrBlank(),
                isCastConnected = isCastConnected,
                onOpenCastDialog = { showCastDialog = true },
                onAdjustBrightness = { delta -> viewModel.adjustBrightness(delta, activity) },
                onAdjustVolume = { delta -> viewModel.adjustVolume(delta) },
                onSelectRelatedChannel = {
                    showBrandingIntro = false
                    viewModel.loadAndPlayChannel(it)
                },
                onToggleFullscreen = {
                    orientationController.enterFullscreen()
                },
                onEnterPip = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            activity?.enterPictureInPictureMode(
                                PictureInPictureParams.Builder()
                                    .setAspectRatio(Rational(16, 9))
                                    .build()
                            )
                        } catch (_: Exception) {}
                    }
                },
                onRetry = { viewModel.retry() },
                isDemo = googleUser?.isDemo == true,
                remainingSeconds = remainingSeconds,
                onSwipeLeft = {
                    showBrandingIntro = false
                    viewModel.playNextChannel(lockedChannelIds)
                },
                onSwipeRight = {
                    showBrandingIntro = false
                    viewModel.playPreviousChannel(lockedChannelIds)
                },
                onToggleListenMode = { viewModel.toggleListenMode() },
                onOpenMultiView = onOpenMultiView,
                onOpenChannelJump = { showChannelJumpDialog = true },
                showZapHud = showZapHud,
                currentZapNumber = zapContext.getCurrentChannelNumber(),
                totalZapChannels = zapContext.getTotalChannels().coerceAtLeast(1),
                zapContextTitle = zapContext.getContextTitle(),
                onToggleLastChannel = {
                    showBrandingIntro = false
                    viewModel.toggleLastChannel(lockedChannelIds)
                },
                showBrandingIntro = showBrandingIntro,
                onDismissBrandingIntro = { showBrandingIntro = false }
            )
        }

        if (!isPipActive) {
            // ==========================================
            // QUICK CHANNEL DRAWER (PORTRAIT & LANDSCAPE)
            // ==========================================
            ChannelQuickDrawer(
                visible = isDrawerOpen,
                currentChannelId = uiState.channel?.id ?: "",
                channels = allChannels,
                isLandscape = isLandscape,
                onSelectChannel = { viewModel.loadAndPlayChannel(it) },
                onClose = { viewModel.closeDrawer() }
            )

            // ==========================================
            // SOURCE SELECTOR DIALOG
            // ==========================================
            if (showSourceDialog) {
                SourceSelectionDialog(
                    streams = uiState.streams,
                    currentIndex = uiState.currentStreamIndex,
                    onSelect = { idx ->
                        viewModel.selectSource(idx)
                        showSourceDialog = false
                    },
                    onDismiss = { showSourceDialog = false }
                )
            }

            // ==========================================
            // SLEEP TIMER DIALOG
            // ==========================================
            if (showSleepTimerDialog) {
                SleepTimerDialog(
                    currentTimer = sleepTimerMinutes,
                    onSelectTimer = { mins ->
                        viewModel.setSleepTimer(mins)
                        showSleepTimerDialog = false
                    },
                    onDismiss = { showSleepTimerDialog = false }
                )
            }

            // ==========================================
            // SCREENCAST & REMOTE PLAYBACK DIALOG
            // ==========================================
            if (showCastDialog) {
                val activeUrl = uiState.streams.getOrNull(uiState.currentStreamIndex)?.url.orEmpty()
                val channelTitle = uiState.channel?.cleanName ?: uiState.channel?.name ?: "Live Channel"
                com.nis.nistv.feature.player.components.ScreencastDialog(
                    streamTitle = channelTitle,
                    streamUrl = activeUrl,
                    onDismiss = { showCastDialog = false }
                )
            }

            // ==========================================
            // DEMO GRACE TIME EXPIRED DIALOG
            // ==========================================
            if (showDemoExpiredDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = {
                        showDemoExpiredDialog = false
                        onBack()
                    },
                    title = {
                        Text(
                            text = "Demo Preview Ended",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Text(
                            text = "Your 1-minute demo grace period has concluded. Please sign in with your Google Account to unlock unlimited, high-speed streaming across all 900+ live channels.",
                            color = NisTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        androidx.compose.material3.Button(
                            onClick = {
                                showDemoExpiredDialog = false
                                onBack()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = NisAccentPrimary
                            )
                        ) {
                            Text("Sign In with Google", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = NisSurfaceElevated
                )
            }

            // ==========================================
            // TRACK SELECTOR BOTTOM SHEET
            // ==========================================
            if (showTrackSelectorSheet) {
                com.nis.nistv.feature.player.components.TrackSelectorBottomSheet(
                    audioTracks = uiState.audioTrackItems,
                    subtitleTracks = uiState.subtitleTrackItems,
                    onSelectAudio = { groupIdx, trackIdx ->
                        viewModel.selectAudioTrack(groupIdx, trackIdx)
                    },
                    onSelectSubtitle = { groupIdx, trackIdx ->
                        viewModel.selectSubtitleTrack(groupIdx, trackIdx)
                    },
                    onDismiss = { showTrackSelectorSheet = false }
                )
            }

            // ==========================================
            // STREAM DIAGNOSTICS OVERLAY
            // ==========================================
            if (uiState.showDiagnostics) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 50.dp, start = 16.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    com.nis.nistv.feature.player.components.StreamDiagnosticsOverlay(
                        diagnostics = uiState.diagnostics,
                        onClose = { viewModel.toggleDiagnostics() }
                    )
                }
            }

            // ==========================================
            // MULTI-VIEW CHANNEL PICKER DIALOG
            // ==========================================
            if (showMultiViewPicker) {
                MultiViewChannelPicker(
                    availableChannels = allChannels,
                    onChannelSelected = { selected ->
                        val slot = multiViewSlotToReplace
                        if (slot != null) {
                            viewModel.replaceMultiViewChannel(slot, selected, multiViewController)
                            multiViewSlotToReplace = null
                        } else {
                            viewModel.startMultiView(selected, multiViewController)
                        }
                        showMultiViewPicker = false
                    },
                    onDismiss = {
                        showMultiViewPicker = false
                        multiViewSlotToReplace = null
                    }
                )
            }

            // ==========================================
            // CHANNEL JUMP (NUMERIC DIAL) DIALOG
            // ==========================================
            if (showChannelJumpDialog) {
                ChannelJumpDialog(
                    totalChannels = zapContext.getTotalChannels().coerceAtLeast(1),
                    contextTitle = zapContext.getContextTitle(),
                    onJump = { number ->
                        showBrandingIntro = false
                        viewModel.jumpToChannelNumber(number, lockedChannelIds)
                        showChannelJumpDialog = false
                    },
                    onDismiss = { showChannelJumpDialog = false }
                )
            }
        }
    }
}

// ==========================================
// PORTRAIT COMPOSABLE
// ==========================================
@Composable
fun PortraitPlayerLayout(
    exoPlayer: androidx.media3.exoplayer.ExoPlayer,
    uiState: com.nis.nistv.playback.PlayerUiState,
    programmeInfo: com.nis.nistv.data.epg.ProgrammeInfo?,
    relatedChannels: List<ChannelEntity>,
    isFavorite: Boolean,
    isLocked: Boolean,
    brightness: Float,
    volume: Float,
    showBrightnessHud: Boolean,
    showVolumeHud: Boolean,
    sleepTimerMinutes: Int?,
    onBack: () -> Unit,
    onToggleControls: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPreviousChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onToggleLock: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSourceDialog: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenTrackSelector: () -> Unit = {},
    onToggleDiagnostics: () -> Unit = {},
    onQuickSwitchPreviousChannel: () -> Unit = {},
    hasPreviousChannel: Boolean = false,
    isCastConnected: Boolean = false,
    onOpenCastDialog: () -> Unit,
    onAdjustBrightness: (Float) -> Unit,
    onAdjustVolume: (Float) -> Unit,
    onSelectRelatedChannel: (String) -> Unit,
    onToggleFullscreen: () -> Unit,
    onEnterPip: () -> Unit,
    onRetry: () -> Unit,
    isDemo: Boolean = false,
    remainingSeconds: Int = 0,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onToggleListenMode: () -> Unit = {},
    onOpenMultiView: () -> Unit = {},
    onOpenChannelJump: () -> Unit = {},
    showZapHud: Boolean = false,
    currentZapNumber: Int = 1,
    totalZapChannels: Int = 1,
    zapContextTitle: String = "All Channels",
    onToggleLastChannel: () -> Unit = {},
    showBrandingIntro: Boolean = false,
    onDismissBrandingIntro: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NisTheme.colors.background)
    ) {
        // Status Bar Protection: Solid black container for the status bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .statusBarsPadding()
        )

        // Top 16:9 Video Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                    view.resizeMode = when (uiState.aspectRatioMode) {
                        AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }
                },
                onRelease = { view ->
                    view.player = null
                },
                modifier = Modifier.fillMaxSize()
            )

            // Gestures & OSD
            PlayerGestureDetector(
                onToggleControls = onToggleControls,
                onAdjustBrightness = onAdjustBrightness,
                onAdjustVolume = onAdjustVolume,
                onDoubleTapLeft = onPreviousChannel,
                onDoubleTapRight = onNextChannel,
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight,
                isLocked = isLocked,
                modifier = Modifier.fillMaxSize()
            ) {
                // Zap Overlay HUD
                ZapOverlay(
                    visible = showZapHud,
                    channel = uiState.channel,
                    channelNumber = currentZapNumber,
                    totalChannels = totalZapChannels,
                    contextTitle = zapContextTitle,
                    onDoubleTap = onToggleLastChannel,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )

                // HUDs
                BrightnessHud(
                    brightness = brightness,
                    visible = showBrightnessHud,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                )
                VolumeHud(
                    volume = volume,
                    visible = showVolumeHud,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
                )

                // Controls Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.isControlsVisible && uiState.status != PlaybackStatus.ERROR,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NisScrimMedium)
                    ) {
                        // Top bar inside video overlay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(NisIcons.Collapse, "Collapse to miniplayer", tint = Color.White)
                            }
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                                Text(
                                    text = uiState.channel?.cleanName ?: "",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = programmeInfo?.title ?: "Live Broadcast",
                                    color = NisAccentPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                            if (isDemo && remainingSeconds > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(NisBadgeShape)
                                        .background(NisAccentAmber)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${remainingSeconds}s",
                                        color = Color.Black,
                                        style = NisTheme.textExtras.micro,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            IconButton(onClick = onToggleListenMode) {
                                Icon(
                                    imageVector = NisIcons.Headphones,
                                    contentDescription = "Listen Mode",
                                    tint = if (uiState.isListenMode) NisAccentPrimary else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = onOpenMultiView) {
                                Icon(
                                    imageVector = NisIcons.MultiView,
                                    contentDescription = "Multi-View",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = onCycleAspectRatio) {
                                Icon(NisIcons.AspectRatio, "Aspect", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = onToggleFullscreen) {
                                Icon(NisIcons.Fullscreen, "Fullscreen", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Center Controls
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            IconButton(onClick = onPreviousChannel) {
                                Icon(NisIcons.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(NisScrimMedium)
                                    .clickable { onTogglePlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isBuffering) {
                                    CircularProgressIndicator(color = NisAccentPrimary, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                                } else {
                                    Icon(
                                        imageVector = if (uiState.isPlaying) NisIcons.Pause else NisIcons.Play,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            IconButton(onClick = onNextChannel) {
                                Icon(NisIcons.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }

                // Resilient Failover Notice
                if (!uiState.failoverMessage.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clip(NisShapes.small)
                            .background(NisScrimMedium)
                            .border(1.dp, NisBorder, NisShapes.small)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = uiState.failoverMessage ?: "",
                            color = NisAccentPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Resilient Error State Overlay per iptv-player-ux
                if (uiState.status == PlaybackStatus.ERROR) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NisScrimStrong),
                        contentAlignment = Alignment.Center
                    ) {
                        NisErrorState(
                            title = "Stream Temporarily Offline",
                            message = uiState.errorMessage ?: "Live broadcast stream failed. IPTV links are community-maintained and may be intermittent.",
                            onRetry = onRetry,
                            alternativeActionText = "Try Next Channel",
                            onAlternativeAction = onNextChannel
                        )
                    }
                }
            }

            // Listen Mode Audio-Only Overlay
            if (uiState.isListenMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NisBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(NisAccentPrimary.copy(alpha = 0.2f))
                                .border(1.5.dp, NisAccentPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = NisIcons.Headphones,
                                contentDescription = "Listen Mode",
                                tint = NisAccentPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Listen Mode (Audio Only)",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Saving cellular data • Screen can be turned off",
                            color = NisTextMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        androidx.compose.material3.OutlinedButton(
                            onClick = onToggleListenMode,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NisAccentPrimary),
                            shape = NisPillShape,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text("Resume Video", color = NisAccentPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (showBrandingIntro) {
                PlayerBrandingIntro(
                    onFinish = onDismissBrandingIntro,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Details ScrollView (Under player)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Channel Title & Metadata Pills
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Channel Logo
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(NisThumbnailShape)
                                .background(NisTheme.colors.surfaceElevated)
                                .border(1.dp, NisTheme.colors.borderSubtle, NisThumbnailShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!uiState.channel?.logoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = uiState.channel!!.logoUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(36.dp)
                                )
                            } else {
                                Text(
                                    text = (uiState.channel?.cleanName ?: "TV").take(2).uppercase(),
                                    color = NisAccentCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.channel?.cleanName ?: "Channel",
                                    color = NisTheme.colors.textPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                NisLiveBadge()
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(NisBadgeShape)
                                        .background(NisAccentCyan.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = uiState.channel?.resolution?.ifEmpty { "HD" } ?: "HD",
                                        color = NisAccentCyan,
                                        style = NisTheme.textExtras.micro,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${uiState.channel?.category ?: ""} • ${uiState.channel?.language ?: ""}",
                                    color = NisTheme.colors.textMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons row (Fullscreen, Favorite, Sleep Timer, Channels, Source, PiP, Listen, Multi-View, Jump)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            PlayerActionButton(
                                icon = NisIcons.Fullscreen,
                                label = "Expand",
                                onClick = onToggleFullscreen
                            )
                        }

                        item {
                            PlayerActionButton(
                                icon = if (isFavorite) NisIcons.Favorites else NisIcons.FavoritesBorder,
                                label = if (isFavorite) "In List" else "Add List",
                                active = isFavorite,
                                onClick = onToggleFavorite
                            )
                        }

                        item {
                            PlayerActionButton(
                                icon = NisIcons.Headphones,
                                label = if (uiState.isListenMode) "Audio Only" else "Listen",
                                active = uiState.isListenMode,
                                onClick = onToggleListenMode
                            )
                        }

                        item {
                            PlayerActionButton(
                                icon = NisIcons.MultiView,
                                label = "Multi-View",
                                onClick = onOpenMultiView
                            )
                        }

                        item {
                            PlayerActionButton(
                                icon = NisIcons.FilterList,
                                label = "Channels",
                                onClick = onOpenDrawer
                            )
                        }

                        item {
                            PlayerActionButton(
                                icon = NisIcons.Search,
                                label = "Jump",
                                onClick = onOpenChannelJump
                            )
                        }

                        item {
                            PlayerActionButton(
                                icon = NisIcons.Timer,
                                label = if (sleepTimerMinutes != null) "${sleepTimerMinutes}m" else "Timer",
                                active = sleepTimerMinutes != null,
                                onClick = onOpenSleepTimer
                            )
                        }

                        item {
                            PlayerActionButton(
                                icon = NisIcons.Settings,
                                label = "Source",
                                onClick = onOpenSourceDialog
                            )
                        }

                        item {
                            PlayerActionButton(
                                icon = if (isCastConnected) NisIcons.CastConnected else NisIcons.Cast,
                                label = if (isCastConnected) "Casting" else "Cast",
                                active = isCastConnected,
                                onClick = onOpenCastDialog
                            )
                        }

                        item {
                            PlayerActionButton(
                                icon = NisIcons.Subtitles,
                                label = "Tracks",
                                onClick = onOpenTrackSelector
                            )
                        }

                        item {
                            PlayerActionButton(
                                icon = NisIcons.Tune,
                                label = "Stats",
                                active = uiState.showDiagnostics,
                                onClick = onToggleDiagnostics
                            )
                        }

                        if (hasPreviousChannel) {
                            item {
                                PlayerActionButton(
                                    icon = NisIcons.SkipPrevious,
                                    label = "Previous",
                                    onClick = onQuickSwitchPreviousChannel
                                )
                            }
                        }

                        item {
                            PlayerActionButton(
                                icon = NisIcons.PictureInPicture,
                                label = "PiP",
                                onClick = onEnterPip
                            )
                        }
                    }
                }
            }

            // 2. Live EPG Programme Card
            item {
                LiveEpgCard(programmeInfo = programmeInfo)
            }

            // 3. Related Channels Rail
            if (relatedChannels.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "More Channels in ${uiState.channel?.category ?: "Live TV"}",
                            color = NisTheme.colors.textPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(relatedChannels, key = { it.id }) { ch ->
                                com.nis.nistv.core.designsystem.component.NisRailChannelCard(
                                    channel = ch,
                                    onClick = { onSelectRelatedChannel(ch.id) },
                                    isFavorite = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// LANDSCAPE COMPOSABLE
// ==========================================
@Composable
fun LandscapePlayerLayout(
    exoPlayer: androidx.media3.exoplayer.ExoPlayer,
    uiState: com.nis.nistv.playback.PlayerUiState,
    programmeInfo: com.nis.nistv.data.epg.ProgrammeInfo?,
    isFavorite: Boolean,
    isLocked: Boolean,
    brightness: Float,
    volume: Float,
    showBrightnessHud: Boolean,
    showVolumeHud: Boolean,
    sleepTimerMinutes: Int?,
    onBack: () -> Unit,
    onToggleControls: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPreviousChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onToggleLock: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSourceDialog: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenTrackSelector: () -> Unit = {},
    onToggleDiagnostics: () -> Unit = {},
    onQuickSwitchPreviousChannel: () -> Unit = {},
    hasPreviousChannel: Boolean = false,
    isCastConnected: Boolean = false,
    onOpenCastDialog: () -> Unit,
    onAdjustBrightness: (Float) -> Unit,
    onAdjustVolume: (Float) -> Unit,
    onToggleFullscreen: () -> Unit,
    onEnterPip: () -> Unit,
    onRetry: () -> Unit,
    isDemo: Boolean = false,
    remainingSeconds: Int = 0,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onToggleListenMode: () -> Unit = {},
    onOpenMultiView: () -> Unit = {},
    onOpenChannelJump: () -> Unit = {},
    showZapHud: Boolean = false,
    currentZapNumber: Int = 1,
    totalZapChannels: Int = 1,
    zapContextTitle: String = "All Channels",
    onToggleLastChannel: () -> Unit = {},
    showBrandingIntro: Boolean = false,
    onDismissBrandingIntro: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Fullscreen ExoPlayer
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            update = { view ->
                view.player = exoPlayer
                view.resizeMode = when (uiState.aspectRatioMode) {
                    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
            },
            onRelease = { view ->
                view.player = null
            },
            modifier = Modifier.fillMaxSize()
        )

        // Listen Mode Audio-Only Fullscreen Overlay
        if (uiState.isListenMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NisBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(NisAccentPrimary.copy(alpha = 0.2f))
                            .border(2.dp, NisAccentPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NisIcons.Headphones,
                            contentDescription = "Listen Mode",
                            tint = NisAccentPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Listen Mode Active (Audio Only)",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Video rendering paused to save data • Audio continues in background",
                        color = NisTextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    androidx.compose.material3.OutlinedButton(
                        onClick = onToggleListenMode,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NisAccentPrimary),
                        shape = NisPillShape
                    ) {
                        Text("Resume Video", color = NisAccentPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Touch Gestures & HUDs
        PlayerGestureDetector(
            onToggleControls = onToggleControls,
            onAdjustBrightness = onAdjustBrightness,
            onAdjustVolume = onAdjustVolume,
            onDoubleTapLeft = onPreviousChannel,
            onDoubleTapRight = onNextChannel,
            onSwipeLeft = onSwipeLeft,
            onSwipeRight = onSwipeRight,
            isLocked = isLocked,
            modifier = Modifier.fillMaxSize()
        ) {
            // Zap Overlay HUD
            ZapOverlay(
                visible = showZapHud,
                channel = uiState.channel,
                channelNumber = currentZapNumber,
                totalChannels = totalZapChannels,
                contextTitle = zapContextTitle,
                onDoubleTap = onToggleLastChannel,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
            )

            BrightnessHud(
                brightness = brightness,
                visible = showBrightnessHud,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp)
            )

            VolumeHud(
                volume = volume,
                visible = showVolumeHud,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp)
            )

            // Screen Lock button (When locked, only show unlock)
            if (isLocked) {
                IconButton(
                    onClick = onToggleLock,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 20.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NisScrimMedium)
                ) {
                    Icon(NisIcons.Lock, "Unlock", tint = NisAccentCyan, modifier = Modifier.size(22.dp))
                }
            }

            // OSD Controls
            AnimatedVisibility(
                visible = uiState.isControlsVisible && !isLocked && uiState.status != PlaybackStatus.ERROR,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NisScrimMedium)
                ) {
                    // Top Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(NisScrimStrong, Color.Transparent)
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onToggleFullscreen) {
                                Icon(NisIcons.Back, "Exit Fullscreen", tint = Color.White)
                            }

                            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = uiState.channel?.cleanName ?: "",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    NisLiveBadge()
                                }
                                Text(
                                    text = "${programmeInfo?.title ?: "Live Broadcast"} (${programmeInfo?.timeString ?: ""})",
                                    color = NisAccentPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }

                            // Favorite
                            IconButton(onClick = onToggleFavorite) {
                                Icon(
                                    imageVector = if (isFavorite) NisIcons.Favorites else NisIcons.FavoritesBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) NisLiveRed else Color.White
                                )
                            }

                            if (isDemo && remainingSeconds > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(NisBadgeShape)
                                        .background(NisAccentAmber)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Demo: ${remainingSeconds}s",
                                        color = Color.Black,
                                        style = NisTheme.textExtras.micro,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            // Listen Mode (Audio Only)
                            IconButton(onClick = onToggleListenMode) {
                                Icon(
                                    imageVector = NisIcons.Headphones,
                                    contentDescription = "Listen Mode",
                                    tint = if (uiState.isListenMode) NisAccentPrimary else Color.White
                                )
                            }

                            // Multi-View
                            IconButton(onClick = onOpenMultiView) {
                                Icon(
                                    imageVector = NisIcons.MultiView,
                                    contentDescription = "Multi-View",
                                    tint = Color.White
                                )
                            }

                            // Channel Jump
                            IconButton(onClick = onOpenChannelJump) {
                                Icon(
                                    imageVector = NisIcons.FilterList,
                                    contentDescription = "Jump to Channel",
                                    tint = Color.White
                                )
                            }

                            // Aspect Ratio
                            IconButton(onClick = onCycleAspectRatio) {
                                Icon(NisIcons.AspectRatio, "Aspect", tint = Color.White)
                            }

                            // Source
                            IconButton(onClick = onOpenSourceDialog) {
                                Icon(NisIcons.Settings, "Sources", tint = Color.White)
                            }

                            // Sleep Timer
                            IconButton(onClick = onOpenSleepTimer) {
                                Icon(NisIcons.Timer, "Timer", tint = if (sleepTimerMinutes != null) NisAccentPrimary else Color.White)
                            }

                            // Screencast & TV
                            IconButton(onClick = onOpenCastDialog) {
                                Icon(
                                    imageVector = if (isCastConnected) NisIcons.CastConnected else NisIcons.Cast,
                                    contentDescription = "Screencast & TV",
                                    tint = if (isCastConnected) NisSuccess else Color.White
                                )
                            }

                            // Tracks
                            IconButton(onClick = onOpenTrackSelector) {
                                Icon(NisIcons.Subtitles, "Tracks", tint = Color.White)
                            }

                            // Stats / Diagnostics
                            IconButton(onClick = onToggleDiagnostics) {
                                Icon(
                                    imageVector = NisIcons.Tune,
                                    contentDescription = "Diagnostics",
                                    tint = if (uiState.showDiagnostics) NisAccentPrimary else Color.White
                                )
                            }

                            // Previous Channel
                            if (hasPreviousChannel) {
                                IconButton(onClick = onQuickSwitchPreviousChannel) {
                                    Icon(NisIcons.SkipPrevious, "Previous Channel", tint = Color.White)
                                }
                            }

                            // PiP
                            IconButton(onClick = onEnterPip) {
                                Icon(NisIcons.PictureInPicture, "PiP", tint = Color.White)
                            }

                            // Fullscreen toggle (Back to portrait)
                            IconButton(onClick = onToggleFullscreen) {
                                Icon(NisIcons.FullscreenExit, "Exit Fullscreen", tint = Color.White)
                            }
                        }
                    }

                    // Center Transport Controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(36.dp)
                    ) {
                        IconButton(onClick = onPreviousChannel) {
                            Icon(NisIcons.SkipPrevious, "Prev Channel", tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(NisScrimMedium)
                                .clickable { onTogglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isBuffering) {
                                CircularProgressIndicator(color = NisAccentPrimary, strokeWidth = 3.5.dp, modifier = Modifier.size(36.dp))
                            } else {
                                Icon(
                                    imageVector = if (uiState.isPlaying) NisIcons.Pause else NisIcons.Play,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        IconButton(onClick = onNextChannel) {
                            Icon(NisIcons.SkipNext, "Next Channel", tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }

                    // Lock button on left
                    IconButton(
                        onClick = onToggleLock,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 20.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NisScrimSoft)
                    ) {
                        Icon(NisIcons.LockOpen, "Lock", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    // Bottom Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, NisScrimStrong)
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(NisPillShape)
                                    .background(NisBorder)
                                    .clickable { onOpenDrawer() }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(NisIcons.FilterList, null, tint = NisAccentPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Quick Channels", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }

                            Text(
                                text = "Aspect: ${uiState.aspectRatioMode.label}",
                                color = NisTextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Resilient Failover Notice
            if (!uiState.failoverMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .clip(NisShapes.small)
                        .background(NisScrimMedium)
                        .border(1.dp, NisBorder, NisShapes.small)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = uiState.failoverMessage ?: "",
                        color = NisAccentPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Resilient Error State Overlay per iptv-player-ux
            if (uiState.status == PlaybackStatus.ERROR) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NisScrimStrong),
                    contentAlignment = Alignment.Center
                ) {
                    NisErrorState(
                        title = "Stream Temporarily Offline",
                        message = uiState.errorMessage ?: "Live broadcast stream failed. IPTV links are community-maintained and may be intermittent.",
                        onRetry = onRetry,
                        alternativeActionText = "Try Next Channel",
                        onAlternativeAction = onNextChannel
                    )
                }
            }
        }

        if (showBrandingIntro) {
            PlayerBrandingIntro(
                onFinish = onDismissBrandingIntro,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// Small Action Button in Portrait Details
@Composable
private fun PlayerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(NisMediaCardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (active) NisAccentPrimary.copy(alpha = 0.2f) else NisTheme.colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) NisAccentPrimary else NisTheme.colors.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (active) NisAccentPrimary else NisTheme.colors.textSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

// Dialog for Multi-stream source picker
@Composable
fun SourceSelectionDialog(
    streams: List<com.nis.nistv.core.database.entity.StreamSourceEntity>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NisScrimStrong)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(NisDialogShape)
                .background(NisTheme.colors.surfaceElevated)
                .border(1.dp, NisTheme.colors.borderSubtle, NisDialogShape)
                .padding(20.dp)
                .clickable(enabled = false) {}
        ) {
            Text(
                text = "Select Stream Source",
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            streams.forEachIndexed { index, source ->
                val selected = index == currentIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(NisShapes.small)
                        .background(if (selected) NisAccentCyan.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Source ${index + 1} (${source.quality ?: "HD"})",
                            color = if (selected) NisAccentCyan else NisTheme.colors.textPrimary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (!source.labels.isNullOrBlank()) {
                            Text(text = source.labels, color = NisTheme.colors.textMuted, style = NisTheme.textExtras.micro)
                        }
                    }
                    if (selected) {
                        Icon(NisIcons.Check, null, tint = NisAccentCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// Dialog for Sleep Timer picker
@Composable
fun SleepTimerDialog(
    currentTimer: Int?,
    onSelectTimer: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        Pair("Turn Off Timer", null),
        Pair("15 Minutes", 15),
        Pair("30 Minutes", 30),
        Pair("45 Minutes", 45),
        Pair("60 Minutes", 60)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NisScrimStrong)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .clip(NisDialogShape)
                .background(NisTheme.colors.surfaceElevated)
                .border(1.dp, NisTheme.colors.borderSubtle, NisDialogShape)
                .padding(20.dp)
                .clickable(enabled = false) {}
        ) {
            Text(
                text = "Sleep Timer",
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            options.forEach { (label, mins) ->
                val selected = currentTimer == mins
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(NisShapes.small)
                        .background(if (selected) NisAccentCyan.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onSelectTimer(mins) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = if (selected) NisAccentCyan else NisTheme.colors.textPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (selected) {
                        Icon(NisIcons.Check, null, tint = NisAccentCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
