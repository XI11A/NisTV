package com.nis.nistv.feature.downloads

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import com.nis.nistv.feature.player.components.PlayerBrandingIntro
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import com.nis.nistv.core.database.entity.SubtitleTrackEntity
import com.nis.nistv.core.playback.SleepTimerState
import com.nis.nistv.core.preferences.SubtitleStylePreferences
import com.nis.nistv.data.tmdb.model.TmdbMediaItem
import com.nis.nistv.data.tmdb.model.TmdbMediaType
import com.nis.nistv.feature.downloads.components.OfflinePlayerTrackDialog
import com.nis.nistv.data.download.offline.SeriesKeyHelper
import com.nis.nistv.feature.downloads.components.TrackDialogTab
import com.nis.nistv.feature.downloads.components.MediaTrackOption
import com.nis.nistv.feature.downloads.subtitles.SidecarSubtitleDetector
import com.nis.nistv.feature.downloads.subtitles.SubtitleCharsetDetector
import com.nis.nistv.data.subtitles.SubtitleCue
import com.nis.nistv.data.subtitles.SubtitleCueParser
import com.nis.nistv.feature.player.controls.DualSubtitleOverlay
import com.nis.nistv.feature.player.controls.AutoplayNextOverlay
import com.nis.nistv.feature.player.controls.PlaybackSpeedSheet
import com.nis.nistv.feature.player.controls.SeekRippleOverlay
import com.nis.nistv.feature.player.controls.SleepTimerChip
import com.nis.nistv.feature.player.controls.SleepTimerSheet
import com.nis.nistv.feature.player.controls.clampSeekPosition
import com.nis.nistv.feature.player.controls.resolveNextEpisode
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nis.nistv.NisTvApplication
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.designsystem.component.NisErrorState
import com.nis.nistv.core.designsystem.component.NisIconButton
import com.nis.nistv.core.designsystem.component.NisPrimaryButton
import com.nis.nistv.core.designsystem.component.NisSecondaryButton
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.feature.player.components.BrightnessHud
import com.nis.nistv.feature.player.components.PlayerGestureDetector
import com.nis.nistv.feature.player.components.VolumeHud
import com.nis.nistv.feature.player.components.rememberPlayerOrientationController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun OfflinePlayerScreen(
    downloadId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isInPipMode: Boolean = false,
    onPlayOther: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val app = context.applicationContext as NisTvApplication
    val downloadDao = remember { app.container.database.downloadDao() }
    val playbackProgressDao = remember { app.container.database.playbackProgressDao() }
    val activeProfileId by app.container.profileRepository.activeProfileId.collectAsState()

    // Fetch target download (support both ID and mediaId)
    val downloadState = produceState<DownloadEntity?>(initialValue = null, key1 = downloadId) {
        value = downloadDao.getDownloadById(downloadId) ?: downloadDao.getDownloadByMediaId(downloadId)
    }
    val download = downloadState.value

    // Fetch profile-specific playback progress
    val playbackProgressState = produceState<com.nis.nistv.core.database.entity.PlaybackProgressEntity?>(
        initialValue = null,
        key1 = activeProfileId,
        key2 = download?.id
    ) {
        val targetId = download?.id
        if (targetId != null) {
            value = playbackProgressDao.getProgressSync(activeProfileId, targetId)
        }
    }
    val playbackProgress = playbackProgressState.value

    // Fetch all completed downloads for "Other Downloads / Episodes" rail
    val allCompletedDownloads by downloadDao.getCompletedDownloads().collectAsState(initial = emptyList())

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var playerError by remember { mutableStateOf<String?>(null) }
    var showBrandingIntro by rememberSaveable { mutableStateOf(true) }

    // Player state: canonical ExoPlayer with platform hardware decoders matching v5
    val exoPlayer = remember(context, downloadId) {
        ExoPlayer.Builder(context)
            .setRenderersFactory(
                DefaultRenderersFactory(context).setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                )
            )
            .build().apply {
                playWhenReady = !showBrandingIntro
                volume = if (showBrandingIntro) 0f else 1f
            }
    }

    // Keep screen awake while player is open & stop background live TV
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val initialBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        try {
            app.container.playerController.stop()
        } catch (_: Exception) {}
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.let { act ->
                val win = act.window
                val lp = win?.attributes
                if (lp != null) {
                    lp.screenBrightness = initialBrightness
                    win.attributes = lp
                }
            }
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }

    // User preferences & Subtitle styling
    val userPreferences = remember { app.container.userPreferences }
    val subtitleStyle by userPreferences.subtitleStylePreferences.collectAsState(initial = SubtitleStylePreferences())

    // Aspect ratio with cross-video persistence
    val savedAspectRatio by userPreferences.playerAspectRatio.collectAsState(initial = AspectRatioFrameLayout.RESIZE_MODE_FIT)
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    LaunchedEffect(savedAspectRatio) {
        resizeMode = savedAspectRatio
    }

    // Playback Speed (session scope)
    var showSpeedSheet by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }

    // Sleep Timer
    val sleepTimerManager = remember { app.container.sleepTimerManager }
    val sleepTimerState by sleepTimerManager.timerState.collectAsState()
    var showSleepTimerSheet by remember { mutableStateOf(false) }

    // Seek Double Tap Ripple
    var showLeftSeek by remember { mutableStateOf(false) }
    var showRightSeek by remember { mutableStateOf(false) }

    LaunchedEffect(showLeftSeek) {
        if (showLeftSeek) {
            delay(800)
            showLeftSeek = false
        }
    }

    LaunchedEffect(showRightSeek) {
        if (showRightSeek) {
            delay(800)
            showRightSeek = false
        }
    }

    // Autoplay Next Episode
    val isAutoplayEnabled by userPreferences.autoplayNextEpisode.collectAsState(initial = true)
    var showAutoplayNextOverlay by remember { mutableStateOf(false) }
    var autoplayCountdown by remember { mutableIntStateOf(10) }
    var hasCancelledAutoplay by remember { mutableStateOf(false) }
    val nextEpisode = remember(download, allCompletedDownloads) {
        download?.let { cur ->
            resolveNextEpisode(cur, allCompletedDownloads)
        }
    }

    // External Subtitles DB and File Picker
    val subtitleTrackDao = remember { app.container.database.subtitleTrackDao() }
    val dbSubtitleTracks by subtitleTrackDao.getTracksForDownload(downloadId).collectAsState(initial = emptyList())

    var showTrackDialog by remember { mutableStateOf(false) }
    var showCastDialog by remember { mutableStateOf(false) }
    var activeTrackTab by remember { mutableStateOf(TrackDialogTab.SUBTITLES) }

    val isAnySheetOpen = showTrackDialog || showCastDialog || showSpeedSheet || showSleepTimerSheet

    val castManager = remember { com.nis.nistv.core.cast.CastManager.getInstance(context) }
    val isCastConnected by castManager.isExternalDisplayConnected.collectAsState()

    val isPipActive = isInPipMode || (activity?.isInPictureInPictureMode == true)
    val orientationController = rememberPlayerOrientationController(
        activity = activity,
        isLandscape = isLandscape,
        isLocked = isLocked,
        isInPip = isPipActive,
    )

    var availableAudioTracks by remember { mutableStateOf<List<MediaTrackOption>>(emptyList()) }
    var availableSubtitleTracks by remember { mutableStateOf<List<MediaTrackOption>>(emptyList()) }

    var primarySubtitleDelayMs by remember { mutableLongStateOf(0L) }
    var secondarySubtitleDelayMs by remember { mutableLongStateOf(0L) }
    var selectedSecondarySubtitleTrack by remember { mutableStateOf<MediaTrackOption?>(null) }
    var primarySubtitleCues by remember { mutableStateOf<List<SubtitleCue>?>(null) }
    var secondarySubtitleCues by remember { mutableStateOf<List<SubtitleCue>?>(null) }

    val isDualOrDelayedActive = (selectedSecondarySubtitleTrack != null) || (primarySubtitleDelayMs != 0L) || (secondarySubtitleDelayMs != 0L)

    val targetFile = remember(download) { download?.filePath?.let { File(it) } }
    LaunchedEffect(availableSubtitleTracks, dbSubtitleTracks, targetFile, isDualOrDelayedActive, selectedSecondarySubtitleTrack) {
        if (!isDualOrDelayedActive) {
            primarySubtitleCues = null
            secondarySubtitleCues = null
            return@LaunchedEffect
        }
        val primaryOption = availableSubtitleTracks.firstOrNull { it.isSelected }
        primarySubtitleCues = withContext(Dispatchers.IO) {
            loadCuesForTrack(primaryOption, targetFile, dbSubtitleTracks)
        }
        secondarySubtitleCues = withContext(Dispatchers.IO) {
            loadCuesForTrack(selectedSecondarySubtitleTrack, targetFile, dbSubtitleTracks)
        }
    }

    val updateAvailableTracks: (Tracks) -> Unit = { tracks ->
        val (audioList, textList) = extractTracksFromPlayer(tracks)
        availableAudioTracks = audioList
        availableSubtitleTracks = textList
    }

    val onSelectAudioTrack: (MediaTrackOption) -> Unit = { option ->
        val tracks = exoPlayer.currentTracks
        val group = tracks.groups.getOrNull(option.groupIndex)
        if (group != null) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
                .build()
            updateAvailableTracks(exoPlayer.currentTracks)
        }
    }

    val onSelectSubtitleTrack: (MediaTrackOption?) -> Unit = { option ->
        if (option == null) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .build()
        } else {
            val tracks = exoPlayer.currentTracks
            val group = tracks.groups.getOrNull(option.groupIndex)
            if (group != null) {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
                    .build()
            }
        }
        updateAvailableTracks(exoPlayer.currentTracks)
    }

    val onUpdateSubtitleStyle: (SubtitleStylePreferences) -> Unit = { newStyle ->
        app.applicationScope.launch(Dispatchers.IO) {
            try {
                userPreferences.saveSubtitleStylePreferences(newStyle)
            } catch (_: Exception) {}
        }
    }

    // Volume & Brightness HUDs
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    var currentVolume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume)
    }
    var currentBrightness by remember {
        val window = activity?.window
        val lp = window?.attributes
        val b = lp?.screenBrightness ?: -1f
        mutableFloatStateOf(if (b < 0) 0.5f else b)
    }
    var showVolumeHud by remember { mutableStateOf(false) }
    var showBrightnessHud by remember { mutableStateOf(false) }

    // Auto-hide HUDs
    LaunchedEffect(showVolumeHud) {
        if (showVolumeHud) {
            delay(1500)
            showVolumeHud = false
        }
    }
    LaunchedEffect(showBrightnessHud) {
        if (showBrightnessHud) {
            delay(1500)
            showBrightnessHud = false
        }
    }

    // Resolve media file with comprehensive fallbacks across public Downloads/NisTV, secondary storage and app storage
    val file = remember(download?.filePath, download?.fileName) {
        if (download == null) null
        else {
            val rawPath = download.filePath.removePrefix("file://").removePrefix("file:")
            val rawName = download.fileName.removePrefix("file://").removePrefix("file:")
            val candidatePaths = listOfNotNull(
                rawPath,
                File(rawPath).absolutePath,
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NisTV/$rawName").absolutePath,
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), rawName).absolutePath,
                "/storage/emulated/0/Download/NisTV/$rawName",
                "/storage/emulated/0/Downloads/NisTV/$rawName",
                "/sdcard/Download/NisTV/$rawName",
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, "NisTV/$rawName").absolutePath },
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, "NisTV_Downloads/$rawName").absolutePath },
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, rawName).absolutePath },
                File(context.filesDir, "NisTV/$rawName").absolutePath,
                File(context.filesDir, rawName).absolutePath
            )
            val found = candidatePaths.firstOrNull { it.isNotBlank() && File(it).exists() && File(it).length() > 0 }
            if (found != null) File(found) else File(rawPath)
        }
    }

    // Synchronize DB filePath if file was located at a fallback path
    LaunchedEffect(file?.absolutePath) {
        val f = file
        val d = download
        if (f != null && d != null && f.exists() && f.absolutePath != d.filePath) {
            try {
                downloadDao.updateDownload(d.copy(filePath = f.absolutePath))
            } catch (_: Exception) {}
        }
    }

    // Target download reference
    val currentDownload by rememberUpdatedState(download)
    var hasResumedInitialPosition by remember { mutableStateOf(false) }
    val hasRecordedComplete = remember { mutableStateOf(false) }

    val savePlaybackProgress: (Long) -> Unit = { positionToSave ->
        val target = currentDownload
        if (target != null) {
            val appInstance = context.applicationContext as? NisTvApplication
            val dur = if (duration > 0 && duration != C.TIME_UNSET) duration else target.durationMs
            val isCompleted = (dur > 0 && positionToSave >= (dur * 0.95f)) || target.watchedCompleted
            val now = System.currentTimeMillis()
            val currentProfId = activeProfileId
            appInstance?.applicationScope?.launch(Dispatchers.IO) {
                try {
                    playbackProgressDao.upsertProgress(
                        com.nis.nistv.core.database.entity.PlaybackProgressEntity(
                            profileId = currentProfId,
                            downloadId = target.id,
                            positionMs = positionToSave,
                            durationMs = dur,
                            lastWatchedAt = now,
                            completed = isCompleted,
                            hidden = false
                        )
                    )
                } catch (_: Exception) {}
                try {
                    downloadDao.updatePlaybackProgress(
                        id = target.id,
                        position = positionToSave,
                        duration = dur,
                        lastWatched = now,
                        watchedCompleted = isCompleted
                    )
                } catch (_: Exception) {}
            }
            if (isCompleted && !hasRecordedComplete.value) {
                hasRecordedComplete.value = true
                if (target.mediaType.equals("tv", ignoreCase = true)) {
                    val sKey = SeriesKeyHelper.getSeriesKey(target.seriesId, target.effectiveSeriesName)
                    appInstance?.applicationScope?.launch(Dispatchers.IO) {
                        try {
                            appInstance.container.autopilotManager.runAutopilotForSeries(sKey)
                        } catch (_: Exception) {}
                    }
                }
                appInstance?.applicationScope?.launch(Dispatchers.IO) {
                    try {
                        val rawId = target.mediaId.replace("tv_", "").replace("movie_", "")
                        val numericId = rawId.toLongOrNull() ?: 0L
                        val isTv = target.mediaType.equals("tv", ignoreCase = true)
                        appInstance.container.recommendationEngine.recordMediaInteraction(
                            media = TmdbMediaItem(
                                id = numericId,
                                title = target.title,
                                overview = target.overview.orEmpty(),
                                posterPath = target.posterUrl,
                                backdropPath = target.backdropUrl,
                                releaseYear = target.releaseDate?.take(4),
                                voteAverage = target.rating ?: 0.0,
                                mediaType = if (isTv) TmdbMediaType.TV else TmdbMediaType.MOVIE
                            ),
                            interactionType = "COMPLETE",
                            durationMs = dur
                        )
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val appInstance = context.applicationContext as? NisTvApplication
            appInstance?.applicationScope?.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bytes = inputStream.readBytes()
                        val decodedText = SubtitleCharsetDetector.decodeToText(bytes)
                        val subDir = File(context.filesDir, "subtitles/$downloadId").apply { mkdirs() }
                        val targetFile = File(subDir, "imported_${System.currentTimeMillis()}.srt")
                        targetFile.writeText(decodedText, Charsets.UTF_8)
                        val label = "External (${targetFile.extension.uppercase()})"
                        subtitleTrackDao.insertTrack(
                            SubtitleTrackEntity(
                                id = "sub_${System.currentTimeMillis()}",
                                downloadId = downloadId,
                                path = targetFile.absolutePath,
                                label = label,
                                languageTag = "und",
                                mimeType = MimeTypes.APPLICATION_SUBRIP,
                                isSelected = true
                            )
                        )
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Subtitle added", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("OfflinePlayer", "Failed to import subtitle: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load subtitle file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Set up and prepare media when file is resolved
    val retryPlayback: () -> Unit = {
        val currentFile = file
        if (currentFile != null && currentFile.exists()) {
            playerError = null
            try {
                val mediaItem = buildMediaItem(currentFile, dbSubtitleTracks)
                val resumePos = currentPosition.takeIf { it > 5_000L }
                    ?: playbackProgress?.positionMs?.takeIf { it > 5_000L }
                    ?: (if (activeProfileId == "default") download?.lastPlaybackPositionMs?.takeIf { it > 5_000L } else null)
                    ?: 0L
                if (resumePos > 5_000L) {
                    exoPlayer.setMediaItem(mediaItem, resumePos)
                    exoPlayer.seekTo(resumePos)
                } else {
                    exoPlayer.setMediaItem(mediaItem)
                }
                exoPlayer.prepare()
                exoPlayer.play()
            } catch (e: Exception) {
                playerError = e.message
                isControlsVisible = true
            }
        }
    }

    // Prepare and play media item
    LaunchedEffect(exoPlayer, file, dbSubtitleTracks) {
        val currentFile = file
        if (currentFile != null && currentFile.exists()) {
            playerError = null
            try {
                val mediaItem = buildMediaItem(currentFile, dbSubtitleTracks)
                val target = download
                val resumePos = playbackProgress?.positionMs?.takeIf { it > 5_000L }
                    ?: (if (activeProfileId == "default") target?.lastPlaybackPositionMs?.takeIf { it > 5_000L } else null)
                    ?: 0L
                if (resumePos > 5_000L) {
                    exoPlayer.setMediaItem(mediaItem, resumePos)
                    exoPlayer.seekTo(resumePos)
                    currentPosition = resumePos
                } else {
                    exoPlayer.setMediaItem(mediaItem)
                    hasResumedInitialPosition = true
                }
                exoPlayer.prepare()
                if (!showBrandingIntro) {
                    exoPlayer.volume = 1f
                    exoPlayer.play()
                } else {
                    exoPlayer.volume = 0f
                    exoPlayer.pause()
                }
            } catch (e: Exception) {
                Log.e("OfflinePlayer", "Failed to prepare local media: ${e.message}", e)
                playerError = e.message
                isControlsVisible = true
            }
        }
    }

    // Ensure content only plays after the 5-second branding video finishes
    LaunchedEffect(showBrandingIntro) {
        if (showBrandingIntro) {
            exoPlayer.volume = 0f
            exoPlayer.pause()
        } else {
            exoPlayer.volume = 1f
            val currentFile = file
            if (currentFile != null && currentFile.exists()) {
                exoPlayer.play()
            }
        }
    }


    // Player listeners - KEYED ONLY ON exoPlayer so it is never released when download loads
    DisposableEffect(exoPlayer) {
        val checkAndApplyResume: () -> Unit = {
            if (!hasResumedInitialPosition) {
                val target = currentDownload
                val targetPos = target?.lastPlaybackPositionMs ?: 0L
                val d = exoPlayer.duration
                if (targetPos > 5_000L) {
                    val isNearEnd = d > 0 && d != C.TIME_UNSET && targetPos >= d - 5_000L
                    if (!isNearEnd) {
                        if (Math.abs(exoPlayer.currentPosition - targetPos) > 2_500L) {
                            exoPlayer.seekTo(targetPos)
                            currentPosition = targetPos
                        }
                    }
                }
                hasResumedInitialPosition = true
            }
        }

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing && hasResumedInitialPosition) {
                    val pos = exoPlayer.currentPosition.takeIf { it > 0 } ?: currentPosition
                    if (pos > 0) {
                        val isFinished = duration > 0 && pos >= duration - 5_000L
                        savePlaybackProgress(if (isFinished) 0L else pos)
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        playerError = null
                        val d = exoPlayer.duration
                        if (d > 0 && d != C.TIME_UNSET) {
                            duration = d
                        }
                        checkAndApplyResume()
                        updateAvailableTracks(exoPlayer.currentTracks)
                    }
                    Player.STATE_ENDED -> {
                        isPlaying = false
                        savePlaybackProgress(0L)
                        if (sleepTimerState is SleepTimerState.Active && (sleepTimerState as SleepTimerState.Active).isEndOfEpisode) {
                            sleepTimerManager.notifyEndOfEpisodeReached()
                        }
                    }
                    Player.STATE_BUFFERING -> {}
                    Player.STATE_IDLE -> {}
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateAvailableTracks(tracks)
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                val d = exoPlayer.duration
                if (d > 0 && d != C.TIME_UNSET) {
                    duration = d
                }
                checkAndApplyResume()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("OfflinePlayer", "Playback error [${error.errorCodeName}]: ${error.message}", error)
                // If direct file:// failed, retry with FileProvider content:// URI fallback
                val currentUri = exoPlayer.currentMediaItem?.localConfiguration?.uri
                val currentFile = file
                if (currentFile != null && currentUri?.scheme == "file") {
                    Log.i("OfflinePlayer", "Retrying with FileProvider content:// URI fallback")
                    try {
                        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", currentFile)
                        val mediaItem = buildMediaItemWithUri(contentUri, currentFile, dbSubtitleTracks)
                        val pos = currentPosition.takeIf { it > 5_000L } ?: (download?.lastPlaybackPositionMs ?: 0L)
                        if (pos > 5_000L) {
                            exoPlayer.setMediaItem(mediaItem, pos)
                            exoPlayer.seekTo(pos)
                        } else {
                            exoPlayer.setMediaItem(mediaItem)
                        }
                        exoPlayer.prepare()
                        exoPlayer.play()
                        return
                    } catch (e: Exception) {
                        Log.e("OfflinePlayer", "content:// fallback failed: ${e.message}")
                    }
                }
                playerError = "${error.errorCodeName}: ${error.message ?: "Playback failed"}"
                isControlsVisible = true
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            if (hasResumedInitialPosition) {
                val finalPos = exoPlayer.currentPosition.takeIf { it > 0 } ?: currentPosition
                if (finalPos > 0) {
                    val isFinished = duration > 0 && finalPos >= duration - 5_000L
                    savePlaybackProgress(if (isFinished) 0L else finalPos)
                }
            }
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Lifecycle observer to immediately persist playback position on minimize / lock / app switch
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (hasResumedInitialPosition) {
                    val pos = exoPlayer.currentPosition.takeIf { it > 0 } ?: currentPosition
                    if (pos > 0) {
                        val isFinished = duration > 0 && pos >= duration - 5_000L
                        savePlaybackProgress(if (isFinished) 0L else pos)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Continuous position & duration tracking loop (never stalls when paused)
    LaunchedEffect(exoPlayer, downloadId) {
        var lastSavedSec = (download?.lastPlaybackPositionMs ?: 0L) / 1000L
        while (true) {
            val d = exoPlayer.duration
            if (d > 0 && d != C.TIME_UNSET) {
                duration = d
            }
            isPlaying = exoPlayer.isPlaying
            if (!isDraggingSlider && hasResumedInitialPosition) {
                val curPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                currentPosition = curPos
                val currentSec = curPos / 1000L
                val target = currentDownload
                if (target != null && Math.abs(currentSec - lastSavedSec) >= 5) {
                    lastSavedSec = currentSec
                    val isFinished = duration > 0 && curPos >= duration - 5_000L
                    savePlaybackProgress(if (isFinished) 0L else curPos)
                }
            }
            delay(250)
        }
    }

    // Autoplay Next Episode monitoring
    LaunchedEffect(currentPosition, duration, isPlaying, isAutoplayEnabled, nextEpisode) {
        if (isAutoplayEnabled && nextEpisode != null && duration > 0L) {
            val isNearEnd = currentPosition >= (duration * 0.95f) || (!isPlaying && currentPosition >= duration - 3000L)
            if (isNearEnd && !showAutoplayNextOverlay && !hasCancelledAutoplay) {
                showAutoplayNextOverlay = true
                autoplayCountdown = 10
            }
        }
    }

    // Autoplay Countdown Loop
    LaunchedEffect(showAutoplayNextOverlay) {
        if (showAutoplayNextOverlay) {
            autoplayCountdown = 10
            while (autoplayCountdown > 0 && showAutoplayNextOverlay) {
                delay(1000)
                autoplayCountdown--
            }
            if (showAutoplayNextOverlay && nextEpisode != null) {
                showAutoplayNextOverlay = false
                onPlayOther(nextEpisode.id)
            }
        }
    }

    // Controls Auto-Hide
    LaunchedEffect(isControlsVisible, isPlaying, isLocked) {
        if (isControlsVisible && isPlaying && !isLocked) {
            delay(4000)
            isControlsVisible = false
        }
    }

    // Fullscreen Immersive Mode in Landscape
    DisposableEffect(isLandscape) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isLandscape) {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose {
            val w = activity?.window
            if (w != null) {
                val c = WindowCompat.getInsetsController(w, w.decorView)
                c.show(WindowInsetsCompat.Type.systemBars())
                w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // Back handler: if landscape, return to portrait first
    BackHandler {
        if (isLandscape) {
            orientationController.exitFullscreen()
        } else {
            onBack()
        }
    }

    if (download == null) {
        Box(
            modifier = modifier.fillMaxSize().background(NisTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = NisAccentPrimary)
        }
        return
    }

    if (file == null || !file.exists()) {
        Box(
            modifier = modifier.fillMaxSize().background(NisTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            NisErrorState(
                title = "File Not Found",
                message = "The downloaded file was not found on device storage:\n${download.filePath}",
                onRetry = onBack
            )
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isInPipMode) {
            // Clean edge-to-edge PiP video canvas
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (isLandscape) {
            // ==========================================
            // IMMERSIVE FULLSCREEN LANDSCAPE PLAYBACK
            // ==========================================
            Box(modifier = Modifier.fillMaxSize()) {
                // Video Surface
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            this.resizeMode = resizeMode
                            setBackgroundColor(android.graphics.Color.BLACK)
                            subtitleView?.apply {
                                visibility = if (isDualOrDelayedActive) android.view.View.GONE else android.view.View.VISIBLE
                                setStyle(subtitleStyle.toCaptionStyleCompat())
                                setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitleStyle.textSize.scale)
                            }
                        }
                    },
                    update = { view ->
                        view.player = exoPlayer
                        view.resizeMode = resizeMode
                        view.subtitleView?.apply {
                            visibility = if (isDualOrDelayedActive) android.view.View.GONE else android.view.View.VISIBLE
                            setStyle(subtitleStyle.toCaptionStyleCompat())
                            setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitleStyle.textSize.scale)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isDualOrDelayedActive) {
                    DualSubtitleOverlay(
                        currentPositionMs = currentPosition,
                        primaryCues = primarySubtitleCues,
                        primaryDelayMs = primarySubtitleDelayMs,
                        secondaryCues = secondarySubtitleCues,
                        secondaryDelayMs = secondarySubtitleDelayMs,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (playerError != null) {
                    PlaybackErrorOverlay(
                        error = playerError ?: "Playback error",
                        isCompact = false,
                        onRetry = retryPlayback,
                        onOpenExternal = { openInExternalPlayer(context, file) }
                    )
                }

                // Gesture Detector (Swipe Brightness & Volume, Double-Tap Seek)
                PlayerGestureDetector(
                    onToggleControls = {
                        if (!isAnySheetOpen) isControlsVisible = !isControlsVisible
                    },
                    onAdjustBrightness = { delta ->
                        if (!isLocked && !isAnySheetOpen) {
                            val newB = (currentBrightness - delta).coerceIn(0.01f, 1f)
                            currentBrightness = newB
                            activity?.window?.attributes = activity?.window?.attributes?.apply {
                                screenBrightness = newB
                            }
                            showBrightnessHud = true
                        }
                    },
                    onAdjustVolume = { delta ->
                        if (!isLocked && !isAnySheetOpen) {
                            val newV = (currentVolume - delta).coerceIn(0f, 1f)
                            currentVolume = newV
                            val streamVol = (newV * maxVolume).toInt()
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamVol, 0)
                            showVolumeHud = true
                        }
                    },
                    onDoubleTapLeft = {
                        if (!isLocked && !isAnySheetOpen) {
                            val target = clampSeekPosition(exoPlayer.currentPosition, -10000L, duration)
                            exoPlayer.seekTo(target)
                            currentPosition = target
                            showLeftSeek = true
                        }
                    },
                    onDoubleTapRight = {
                        if (!isLocked && !isAnySheetOpen) {
                            val target = clampSeekPosition(exoPlayer.currentPosition, 10000L, duration)
                            exoPlayer.seekTo(target)
                            currentPosition = target
                            showRightSeek = true
                        }
                    },
                    isLocked = isLocked
                ) {
                    // HUD Overlays
                    BrightnessHud(
                        brightness = currentBrightness,
                        visible = showBrightnessHud,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 48.dp)
                    )
                    VolumeHud(
                        volume = currentVolume,
                        visible = showVolumeHud,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 48.dp)
                    )
                    SeekRippleOverlay(
                        showLeftSeek = showLeftSeek,
                        showRightSeek = showRightSeek,
                        seekSeconds = 10
                    )
                }

                // Controls Overlay
                AnimatedVisibility(
                    visible = isControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Top & Bottom gradient scrims
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .align(Alignment.TopCenter)
                                .background(Brush.verticalGradient(listOf(NisScrimStrong, Color.Transparent)))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .align(Alignment.BottomCenter)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, NisScrimStrong)))
                        )

                        // Top Landscape Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                NisIconButton(
                                    icon = NisIcons.Back,
                                    contentDescription = "Back",
                                    onClick = {
                                        orientationController.exitFullscreen()
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = download.title,
                                        color = NisTextPrimary,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (download.seasonNumber != null && download.episodeNumber != null) {
                                        Text(
                                            text = "S${download.seasonNumber} E${download.episodeNumber}${if (!download.episodeName.isNullOrBlank()) " • ${download.episodeName}" else ""}",
                                            color = NisAccentPrimary,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Right actions: Speed, Sleep timer, Audio, Subtitles, External player, Aspect ratio, PiP, Fullscreen toggle
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                SleepTimerChip(
                                    state = sleepTimerState,
                                    onClick = { showSleepTimerSheet = true }
                                )
                                NisIconButton(
                                    icon = NisIcons.Tune,
                                    contentDescription = "Playback Speed (${currentSpeed}x)",
                                    onClick = { showSpeedSheet = true }
                                )
                                NisIconButton(
                                    icon = NisIcons.Timer,
                                    contentDescription = "Sleep Timer",
                                    onClick = { showSleepTimerSheet = true }
                                )
                                NisIconButton(
                                    icon = NisIcons.Audiotrack,
                                    contentDescription = "Audio Tracks",
                                    onClick = {
                                        activeTrackTab = TrackDialogTab.AUDIO
                                        showTrackDialog = true
                                    }
                                )
                                NisIconButton(
                                    icon = NisIcons.Subtitles,
                                    contentDescription = "Captions & Subtitles",
                                    onClick = {
                                        activeTrackTab = TrackDialogTab.SUBTITLES
                                        showTrackDialog = true
                                    }
                                )
                                NisIconButton(
                                    icon = NisIcons.OpenInNew,
                                    contentDescription = "External Player",
                                    onClick = {
                                        openInExternalPlayer(context, file)
                                    }
                                )
                                NisIconButton(
                                    icon = NisIcons.AspectRatio,
                                    contentDescription = "Aspect Ratio",
                                    onClick = {
                                        val nextMode = when (resizeMode) {
                                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        }
                                        resizeMode = nextMode
                                        app.applicationScope.launch(Dispatchers.IO) {
                                            userPreferences.setPlayerAspectRatio(nextMode)
                                        }
                                    }
                                )
                                NisIconButton(
                                    icon = if (isCastConnected) NisIcons.CastConnected else NisIcons.Cast,
                                    contentDescription = "Screencast & TV",
                                    onClick = { showCastDialog = true }
                                )
                                NisIconButton(
                                    icon = NisIcons.PictureInPicture,
                                    contentDescription = "Picture in Picture",
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            try {
                                                activity?.enterPictureInPictureMode(
                                                    PictureInPictureParams.Builder()
                                                        .setAspectRatio(Rational(16, 9))
                                                        .build()
                                                )
                                            } catch (_: Exception) {}
                                        }
                                    }
                                )
                                NisIconButton(
                                    icon = NisIcons.FullscreenExit,
                                    contentDescription = "Exit Fullscreen",
                                    onClick = {
                                        orientationController.exitFullscreen()
                                    }
                                )
                            }
                        }

                        // Center Controls (Play/Pause, Rewind 10s, Forward 10s)
                        if (!isLocked) {
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(32.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val target = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                        exoPlayer.seekTo(target)
                                        currentPosition = target
                                    },
                                    modifier = Modifier.size(52.dp).clip(CircleShape).background(NisScrimMedium)
                                ) {
                                    Icon(NisIcons.SkipPrevious, contentDescription = "Rewind 10s", tint = NisTextPrimary, modifier = Modifier.size(28.dp))
                                }

                                IconButton(
                                    onClick = {
                                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    },
                                    modifier = Modifier.size(72.dp).clip(CircleShape).background(NisAccentPrimary)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) NisIcons.Pause else NisIcons.Play,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = NisBackground,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val max = if (duration > 0) duration else Long.MAX_VALUE
                                        val target = (exoPlayer.currentPosition + 10000L).coerceAtMost(max)
                                        exoPlayer.seekTo(target)
                                        currentPosition = target
                                    },
                                    modifier = Modifier.size(52.dp).clip(CircleShape).background(NisScrimMedium)
                                ) {
                                    Icon(NisIcons.SkipNext, contentDescription = "Forward 10s", tint = NisTextPrimary, modifier = Modifier.size(28.dp))
                                }
                            }
                        }

                        // Lock Controls Toggle Button (Left side)
                        IconButton(
                            onClick = { isLocked = !isLocked },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 24.dp)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isLocked) NisAccentPrimary else NisScrimMedium)
                        ) {
                            Icon(
                                imageVector = if (isLocked) NisIcons.Lock else NisIcons.LockOpen,
                                contentDescription = "Lock",
                                tint = if (isLocked) NisBackground else NisTextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Bottom Scrubber Bar & Timestamps
                        if (!isLocked) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val displayPos = if (isDraggingSlider) (sliderValue * duration).toLong() else currentPosition
                                    Text(
                                        text = "${formatDuration(displayPos)} / ${formatDuration(duration)}",
                                        color = NisTextPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    val aspectText = when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> "FIT"
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "CROP (FILL)"
                                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> "STRETCH"
                                        else -> "FIT"
                                    }
                                    Text(
                                        text = aspectText,
                                        color = NisAccentCyan,
                                        style = NisTheme.textExtras.micro,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                val sliderPos = if (duration > 0) {
                                    if (isDraggingSlider) sliderValue else (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                                } else 0f

                                Slider(
                                    value = sliderPos,
                                    onValueChange = {
                                        isDraggingSlider = true
                                        sliderValue = it
                                    },
                                    onValueChangeFinished = {
                                        isDraggingSlider = false
                                        val targetMs = (sliderValue * duration).toLong()
                                        exoPlayer.seekTo(targetMs)
                                        currentPosition = targetMs
                                        if (hasResumedInitialPosition) {
                                            val isFinished = duration > 0 && targetMs >= duration - 5_000L
                                            savePlaybackProgress(if (isFinished) 0L else targetMs)
                                        }
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = NisAccentPrimary,
                                        activeTrackColor = NisAccentPrimary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                if (showBrandingIntro) {
                    PlayerBrandingIntro(
                        onFinish = { showBrandingIntro = false },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // ==========================================
            // RICH PORTRAIT MEDIA & PLAYBACK PAGE
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NisTheme.colors.background)
            ) {
                // 1. 16:9 Video Canvas at top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                this.resizeMode = resizeMode
                                setBackgroundColor(android.graphics.Color.BLACK)
                                subtitleView?.apply {
                                    visibility = if (isDualOrDelayedActive) android.view.View.GONE else android.view.View.VISIBLE
                                    setStyle(subtitleStyle.toCaptionStyleCompat())
                                    setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitleStyle.textSize.scale)
                                }
                            }
                        },
                        update = { view ->
                            view.player = exoPlayer
                            view.resizeMode = resizeMode
                            view.subtitleView?.apply {
                                visibility = if (isDualOrDelayedActive) android.view.View.GONE else android.view.View.VISIBLE
                                setStyle(subtitleStyle.toCaptionStyleCompat())
                                setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitleStyle.textSize.scale)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isDualOrDelayedActive) {
                        DualSubtitleOverlay(
                            currentPositionMs = currentPosition,
                            primaryCues = primarySubtitleCues,
                            primaryDelayMs = primarySubtitleDelayMs,
                            secondaryCues = secondarySubtitleCues,
                            secondaryDelayMs = secondarySubtitleDelayMs,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (playerError != null) {
                        PlaybackErrorOverlay(
                            error = playerError ?: "Playback error",
                            isCompact = true,
                            onRetry = retryPlayback,
                            onOpenExternal = { openInExternalPlayer(context, file) }
                        )
                    }

                    // Gestures & HUDs
                    PlayerGestureDetector(
                        onToggleControls = {
                            if (!isAnySheetOpen) isControlsVisible = !isControlsVisible
                        },
                        onAdjustBrightness = { delta ->
                            if (!isLocked && !isAnySheetOpen) {
                                val newB = (currentBrightness - delta).coerceIn(0.01f, 1f)
                                currentBrightness = newB
                                activity?.window?.attributes = activity?.window?.attributes?.apply {
                                    screenBrightness = newB
                                }
                                showBrightnessHud = true
                            }
                        },
                        onAdjustVolume = { delta ->
                            if (!isLocked && !isAnySheetOpen) {
                                val newV = (currentVolume - delta).coerceIn(0f, 1f)
                                currentVolume = newV
                                val streamVol = (newV * maxVolume).toInt()
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamVol, 0)
                                showVolumeHud = true
                            }
                        },
                        onDoubleTapLeft = {
                            if (!isLocked && !isAnySheetOpen) {
                                val target = clampSeekPosition(exoPlayer.currentPosition, -10000L, duration)
                                exoPlayer.seekTo(target)
                                currentPosition = target
                                showLeftSeek = true
                            }
                        },
                        onDoubleTapRight = {
                            if (!isLocked && !isAnySheetOpen) {
                                val target = clampSeekPosition(exoPlayer.currentPosition, 10000L, duration)
                                exoPlayer.seekTo(target)
                                currentPosition = target
                                showRightSeek = true
                            }
                        },
                        isLocked = false
                    ) {
                        BrightnessHud(
                            brightness = currentBrightness,
                            visible = showBrightnessHud,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 24.dp)
                        )
                        VolumeHud(
                            volume = currentVolume,
                            visible = showVolumeHud,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 24.dp)
                        )
                        SeekRippleOverlay(
                            showLeftSeek = showLeftSeek,
                            showRightSeek = showRightSeek,
                            seekSeconds = 10
                        )
                    }

                    // Top Bar overlay in video canvas
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isControlsVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Top scrim
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .align(Alignment.TopCenter)
                                    .background(Brush.verticalGradient(listOf(NisScrimStrong, Color.Transparent)))
                            )
                            // Bottom scrim
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(70.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, NisScrimStrong)))
                            )

                            // Top Back button & actions
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NisIconButton(
                                    icon = NisIcons.Back,
                                    contentDescription = "Back",
                                    onClick = onBack
                                )

                                Row(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .padding(horizontal = 4.dp)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SleepTimerChip(
                                        state = sleepTimerState,
                                        onClick = { showSleepTimerSheet = true }
                                    )
                                    NisIconButton(
                                        icon = NisIcons.Tune,
                                        contentDescription = "Playback Speed (${currentSpeed}x)",
                                        onClick = { showSpeedSheet = true }
                                    )
                                    NisIconButton(
                                        icon = NisIcons.Timer,
                                        contentDescription = "Sleep Timer",
                                        onClick = { showSleepTimerSheet = true }
                                    )
                                    NisIconButton(
                                        icon = NisIcons.Audiotrack,
                                        contentDescription = "Audio Tracks",
                                        onClick = {
                                            activeTrackTab = TrackDialogTab.AUDIO
                                            showTrackDialog = true
                                        }
                                    )
                                    NisIconButton(
                                        icon = NisIcons.Subtitles,
                                        contentDescription = "Captions & Subtitles",
                                        onClick = {
                                            activeTrackTab = TrackDialogTab.SUBTITLES
                                            showTrackDialog = true
                                        }
                                    )
                                    NisIconButton(
                                        icon = NisIcons.AspectRatio,
                                        contentDescription = "Aspect Ratio",
                                        onClick = {
                                            val nextMode = when (resizeMode) {
                                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            }
                                            resizeMode = nextMode
                                            app.applicationScope.launch(Dispatchers.IO) {
                                                userPreferences.setPlayerAspectRatio(nextMode)
                                            }
                                        }
                                    )
                                    NisIconButton(
                                        icon = if (isCastConnected) NisIcons.CastConnected else NisIcons.Cast,
                                        contentDescription = "Screencast & TV",
                                        onClick = { showCastDialog = true }
                                    )
                                    NisIconButton(
                                        icon = NisIcons.PictureInPicture,
                                        contentDescription = "Picture in Picture",
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                try {
                                                    activity?.enterPictureInPictureMode(
                                                        PictureInPictureParams.Builder()
                                                            .setAspectRatio(Rational(16, 9))
                                                            .build()
                                                    )
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    )
                                    NisIconButton(
                                        icon = NisIcons.OpenInNew,
                                        contentDescription = "External Player",
                                        onClick = {
                                            openInExternalPlayer(context, file)
                                        }
                                    )
                                }

                                NisIconButton(
                                    icon = NisIcons.Fullscreen,
                                    contentDescription = "Enter Fullscreen",
                                    onClick = {
                                        orientationController.enterFullscreen()
                                    }
                                )
                            }

                            // Center Play/Pause button
                            IconButton(
                                onClick = {
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(NisAccentPrimary)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) NisIcons.Pause else NisIcons.Play,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = NisBackground,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            // Bottom Slider & Time inside video
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                val displayPos = if (isDraggingSlider) (sliderValue * duration).toLong() else currentPosition
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${formatDuration(displayPos)} / ${formatDuration(duration)}",
                                        color = NisTextPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "OFFLINE",
                                            color = NisSuccess,
                                            style = NisTheme.textExtras.micro,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = { orientationController.enterFullscreen() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = NisIcons.Fullscreen,
                                                contentDescription = "Enter Fullscreen",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                val sliderPos = if (duration > 0) {
                                    if (isDraggingSlider) sliderValue else (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                                } else 0f

                                Slider(
                                    value = sliderPos,
                                    onValueChange = {
                                        isDraggingSlider = true
                                        sliderValue = it
                                    },
                                    onValueChangeFinished = {
                                        isDraggingSlider = false
                                        val targetMs = (sliderValue * duration).toLong()
                                        exoPlayer.seekTo(targetMs)
                                        currentPosition = targetMs
                                        if (hasResumedInitialPosition) {
                                            val isFinished = duration > 0 && targetMs >= duration - 5_000L
                                            savePlaybackProgress(if (isFinished) 0L else targetMs)
                                        }
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = NisAccentPrimary,
                                        activeTrackColor = NisAccentPrimary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(24.dp)
                                )
                            }
                        }
                    }

                    if (showBrandingIntro) {
                        PlayerBrandingIntro(
                            onFinish = { showBrandingIntro = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // 2. Movie/Series Details Section below video in Portrait
                val otherDownloads = remember(allCompletedDownloads, download.id) {
                    allCompletedDownloads.filter { it.id != download.id }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Card (Title, Episode, Metadata badges)
                    item(key = "media_header") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = download.title,
                                color = NisTheme.colors.textPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            if (download.seasonNumber != null && download.episodeNumber != null) {
                                val epLabel = buildString {
                                    append("Season ${download.seasonNumber} Episode ${download.episodeNumber}")
                                    if (!download.episodeName.isNullOrBlank()) {
                                        append(" • ${download.episodeName}")
                                    }
                                }
                                Text(
                                    text = epLabel,
                                    color = NisAccentPrimary,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Badges Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (download.rating != null && download.rating > 0.0) {
                                    Surface(
                                        shape = NisBadgeShape,
                                        color = NisAccentAmber.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "★ ${String.format(Locale.US, "%.1f", download.rating)}",
                                            color = NisAccentAmber,
                                            style = NisTheme.textExtras.micro,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (!download.releaseDate.isNullOrBlank()) {
                                    val year = download.releaseDate.take(4)
                                    Surface(
                                        shape = NisBadgeShape,
                                        color = NisTheme.colors.surfaceElevated
                                    ) {
                                        Text(
                                            text = year,
                                            color = NisTheme.colors.textSecondary,
                                            style = NisTheme.textExtras.micro,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (download.runtimeMinutes != null && download.runtimeMinutes > 0) {
                                    Surface(
                                        shape = NisBadgeShape,
                                        color = NisTheme.colors.surfaceElevated
                                    ) {
                                        Text(
                                            text = "${download.runtimeMinutes} min",
                                            color = NisTheme.colors.textSecondary,
                                            style = NisTheme.textExtras.micro,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = NisBadgeShape,
                                    color = NisSuccess.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${download.formattedTotal} • OFFLINE",
                                        color = NisSuccess,
                                        style = NisTheme.textExtras.micro,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (!download.genres.isNullOrBlank()) {
                                Text(
                                    text = download.genres,
                                    color = NisTheme.colors.textMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons row (Fullscreen, Subtitles, Speed, Sleep Timer, PiP, External)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OfflineActionButton(
                                    icon = NisIcons.Fullscreen,
                                    label = "Fullscreen",
                                    onClick = { orientationController.enterFullscreen() }
                                )
                                OfflineActionButton(
                                    icon = NisIcons.Subtitles,
                                    label = "Subtitles",
                                    onClick = {
                                        activeTrackTab = TrackDialogTab.SUBTITLES
                                        showTrackDialog = true
                                    }
                                )
                                OfflineActionButton(
                                    icon = NisIcons.Tune,
                                    label = "${currentSpeed}x",
                                    onClick = { showSpeedSheet = true }
                                )
                                OfflineActionButton(
                                    icon = NisIcons.Timer,
                                    label = if (sleepTimerState is SleepTimerState.Active) "Timer" else "Timer",
                                    active = sleepTimerState is SleepTimerState.Active,
                                    onClick = { showSleepTimerSheet = true }
                                )
                                OfflineActionButton(
                                    icon = NisIcons.PictureInPicture,
                                    label = "PiP",
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            try {
                                                activity?.enterPictureInPictureMode(
                                                    PictureInPictureParams.Builder()
                                                        .setAspectRatio(Rational(16, 9))
                                                        .build()
                                                )
                                            } catch (_: Exception) {}
                                        }
                                    }
                                )
                                OfflineActionButton(
                                    icon = NisIcons.OpenInNew,
                                    label = "External",
                                    onClick = { openInExternalPlayer(context, file) }
                                )
                            }
                        }
                    }

                    // Synopsis / Overview
                    if (!download.overview.isNullOrBlank()) {
                        item(key = "media_overview") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Overview",
                                    color = NisTheme.colors.textPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = download.overview,
                                    color = NisTheme.colors.textSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Storage and File Details Card
                    item(key = "file_details") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(NisMediaCardShape)
                                .background(NisTheme.colors.surface)
                                .border(1.dp, NisTheme.colors.borderSubtle, NisMediaCardShape)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "File Information",
                                color = NisTheme.colors.textPrimary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = download.fileName,
                                color = NisTheme.colors.textSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Location: ${download.filePath}",
                                color = NisTheme.colors.textMuted,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Other Downloaded Items / Episodes Rail
                    if (otherDownloads.isNotEmpty()) {
                        item(key = "other_downloads_header") {
                            Text(
                                text = if (download.mediaType == "tv") "MORE EPISODES & DOWNLOADS" else "OTHER DOWNLOADED MEDIA",
                                color = NisTheme.colors.textSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        item(key = "other_downloads_rail") {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(otherDownloads, key = { it.id }) { item ->
                                    OfflineMediaTile(
                                        download = item,
                                        onClick = { onPlayOther(item.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Audio & Subtitle Track Selector / Subtitle Style Customizer Dialog
    if (showTrackDialog) {
        val isSubtitlesDisabled = exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT) ||
            availableSubtitleTracks.none { it.isSelected }

        OfflinePlayerTrackDialog(
            initialTab = activeTrackTab,
            audioTracks = availableAudioTracks,
            subtitleTracks = availableSubtitleTracks,
            isSubtitlesDisabled = isSubtitlesDisabled,
            subtitleStyle = subtitleStyle,
            onSelectAudioTrack = onSelectAudioTrack,
            onSelectSubtitleTrack = onSelectSubtitleTrack,
            onUpdateSubtitleStyle = onUpdateSubtitleStyle,
            onDismiss = { showTrackDialog = false },
            onAddSubtitleFile = {
                subtitlePickerLauncher.launch(arrayOf("*/*", "text/*", "application/x-subrip"))
            },
            selectedSecondarySubtitleTrack = selectedSecondarySubtitleTrack,
            onSelectSecondarySubtitleTrack = { secTrack ->
                selectedSecondarySubtitleTrack = secTrack
            },
            primaryDelayMs = primarySubtitleDelayMs,
            onUpdatePrimaryDelay = { delay ->
                primarySubtitleDelayMs = delay
            },
            secondaryDelayMs = secondarySubtitleDelayMs,
            onUpdateSecondaryDelay = { delay ->
                secondarySubtitleDelayMs = delay
            }
        )
    }

    // Playback Speed Sheet
    if (showSpeedSheet) {
        PlaybackSpeedSheet(
            currentSpeed = currentSpeed,
            onSelectSpeed = { speed ->
                currentSpeed = speed
                exoPlayer.setPlaybackSpeed(speed)
                showSpeedSheet = false
            },
            onDismiss = { showSpeedSheet = false }
        )
    }

    // Sleep Timer Sheet
    if (showSleepTimerSheet) {
        SleepTimerSheet(
            state = sleepTimerState,
            onSelectMinutes = { minutes ->
                sleepTimerManager.startTimer(minutes)
                showSleepTimerSheet = false
            },
            onSelectEndOfEpisode = {
                sleepTimerManager.startEndOfEpisode()
                showSleepTimerSheet = false
            },
            onCancelTimer = {
                sleepTimerManager.cancelTimer()
                showSleepTimerSheet = false
            },
            onDismiss = { showSleepTimerSheet = false }
        )
    }

    // Autoplay Next Episode Overlay
    if (showAutoplayNextOverlay && nextEpisode != null) {
        AutoplayNextOverlay(
            nextEpisode = nextEpisode,
            countdownSeconds = 10,
            onPlayNow = {
                showAutoplayNextOverlay = false
                onPlayOther(nextEpisode.id)
            },
            onCancel = {
                showAutoplayNextOverlay = false
                hasCancelledAutoplay = true
            }
        )
    }

    // Screencast & TV Remote Playback Dialog
    if (showCastDialog) {
        val mediaUri = file?.let { android.net.Uri.fromFile(it).toString() } ?: download?.downloadUrl.orEmpty()
        val mediaTitle = download?.fileName ?: "Offline Media"
        com.nis.nistv.feature.player.components.ScreencastDialog(
            streamTitle = mediaTitle,
            streamUrl = mediaUri,
            onDismiss = { showCastDialog = false }
        )
    }
}

@Composable
private fun OfflineMediaTile(
    download: DownloadEntity,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(170.dp)
                .clip(NisThumbnailShape)
                .background(NisSurfaceElevated)
        ) {
            if (!download.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(download.posterUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = download.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(NisIcons.Play, contentDescription = null, tint = NisTextMuted, modifier = Modifier.size(24.dp))
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(NisScrimStrong),
                contentAlignment = Alignment.Center
            ) {
                Icon(NisIcons.Play, contentDescription = "Play", tint = NisAccentPrimary, modifier = Modifier.size(14.dp))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = download.title,
            color = NisTextPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (download.seasonNumber != null && download.episodeNumber != null) {
            Text(
                text = "S${download.seasonNumber} E${download.episodeNumber}",
                color = NisAccentPrimary,
                style = NisTheme.textExtras.micro,
                fontWeight = FontWeight.Medium
            )
        } else {
            Text(
                text = download.formattedTotal,
                color = NisTextSecondary,
                style = NisTheme.textExtras.micro
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

private fun openInExternalPlayer(context: Context, file: java.io.File?) {
    if (file == null || !file.exists()) {
        Toast.makeText(context, "Media file does not exist on disk", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open external player: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun PlaybackErrorOverlay(
    error: String,
    isCompact: Boolean,
    onRetry: () -> Unit,
    onOpenExternal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .padding(if (isCompact) 12.dp else 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = NisIcons.Info,
                contentDescription = null,
                tint = NisError,
                modifier = Modifier.size(if (isCompact) 28.dp else 44.dp)
            )
            Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 12.dp))
            Text(
                text = "Playback Error",
                color = NisTextPrimary,
                style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = error,
                color = NisTextSecondary,
                style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                maxLines = if (isCompact) 2 else 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 18.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NisPrimaryButton(
                    text = "Retry",
                    icon = NisIcons.Refresh,
                    onClick = onRetry,
                    modifier = if (isCompact) Modifier.height(36.dp) else Modifier
                )
                NisSecondaryButton(
                    text = "External Player",
                    onClick = onOpenExternal,
                    modifier = if (isCompact) Modifier.height(36.dp) else Modifier
                )
            }
        }
    }
}

private fun buildMediaItem(file: File, dbSubs: List<SubtitleTrackEntity> = emptyList()): MediaItem {
    val builder = MediaItem.Builder()
        .setUri(Uri.fromFile(file))
        .apply {
            when (file.extension.lowercase()) {
                "mp4", "m4v" -> setMimeType(MimeTypes.VIDEO_MP4)
                "mkv" -> setMimeType(MimeTypes.VIDEO_MATROSKA)
                "webm" -> setMimeType(MimeTypes.VIDEO_WEBM)
                "ts" -> setMimeType(MimeTypes.VIDEO_MP2T)
                "mov" -> setMimeType("video/quicktime")
                "avi" -> setMimeType(MimeTypes.VIDEO_AVI)
            }
        }

    // Auto-detect adjacent sidecar subtitle files (.srt, .vtt, .ass, .ssa) using SidecarSubtitleDetector
    val sidecars = SidecarSubtitleDetector.findSidecarSubtitles(file)
    val sidecarConfigs = sidecars.map { sidecar ->
        MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(sidecar.file))
            .setMimeType(sidecar.mimeType)
            .setLanguage(sidecar.languageTag)
            .setLabel(sidecar.label)
            .build()
    }

    // Include imported subtitles from DB
    val dbConfigs = dbSubs.mapNotNull { track ->
        val subFile = File(track.path)
        if (subFile.exists()) {
            MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(subFile))
                .setMimeType(track.mimeType)
                .setLanguage(track.languageTag)
                .setLabel(track.label)
                .build()
        } else null
    }

    val allConfigs = (sidecarConfigs + dbConfigs).distinctBy { it.uri.toString() }
    if (allConfigs.isNotEmpty()) {
        builder.setSubtitleConfigurations(allConfigs)
    }

    return builder.build()
}

private fun buildMediaItemWithUri(uri: Uri, file: File, dbSubs: List<SubtitleTrackEntity> = emptyList()): MediaItem {
    val builder = MediaItem.Builder()
        .setUri(uri)
        .apply {
            when (file.extension.lowercase()) {
                "mp4", "m4v" -> setMimeType(MimeTypes.VIDEO_MP4)
                "mkv" -> setMimeType(MimeTypes.VIDEO_MATROSKA)
                "webm" -> setMimeType(MimeTypes.VIDEO_WEBM)
                "ts" -> setMimeType(MimeTypes.VIDEO_MP2T)
                "mov" -> setMimeType("video/quicktime")
                "avi" -> setMimeType(MimeTypes.VIDEO_AVI)
            }
        }

    val sidecars = SidecarSubtitleDetector.findSidecarSubtitles(file)
    val sidecarConfigs = sidecars.map { sidecar ->
        MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(sidecar.file))
            .setMimeType(sidecar.mimeType)
            .setLanguage(sidecar.languageTag)
            .setLabel(sidecar.label)
            .build()
    }

    val dbConfigs = dbSubs.mapNotNull { track ->
        val subFile = File(track.path)
        if (subFile.exists()) {
            MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(subFile))
                .setMimeType(track.mimeType)
                .setLanguage(track.languageTag)
                .setLabel(track.label)
                .build()
        } else null
    }

    val allConfigs = (sidecarConfigs + dbConfigs).distinctBy { it.uri.toString() }
    if (allConfigs.isNotEmpty()) {
        builder.setSubtitleConfigurations(allConfigs)
    }

    return builder.build()
}

private fun extractTracksFromPlayer(tracks: Tracks): Pair<List<MediaTrackOption>, List<MediaTrackOption>> {
    val audioList = mutableListOf<MediaTrackOption>()
    val textList = mutableListOf<MediaTrackOption>()

    for ((groupIndex, group) in tracks.groups.withIndex()) {
        val type = group.type
        if (type != C.TRACK_TYPE_AUDIO && type != C.TRACK_TYPE_TEXT) continue

        for (trackIndex in 0 until group.length) {
            val format = group.getTrackFormat(trackIndex)
            val isSelected = group.isTrackSelected(trackIndex)
            val lang = format.language?.takeIf { it.isNotBlank() && it != "und" }
            val langDisplay = lang?.let {
                try {
                    val loc = java.util.Locale.forLanguageTag(it)
                    val disp = loc.getDisplayLanguage(java.util.Locale.US)
                    if (disp.isNotBlank()) disp else it
                } catch (_: Exception) {
                    it
                }
            }

            val label = when {
                !format.label.isNullOrBlank() && langDisplay != null -> "${format.label} ($langDisplay)"
                !format.label.isNullOrBlank() -> format.label!!
                langDisplay != null -> langDisplay
                type == C.TRACK_TYPE_AUDIO -> {
                    val channels = when (format.channelCount) {
                        1 -> "Mono"
                        2 -> "Stereo"
                        6 -> "5.1 Surround"
                        8 -> "7.1 Surround"
                        else -> null
                    }
                    val codec = format.sampleMimeType?.substringAfterLast('/')?.uppercase()?.replace("MP4A-LATM", "AAC")
                    listOfNotNull("Audio Track ${trackIndex + 1}", channels, codec).joinToString(" • ")
                }
                else -> {
                    val codec = format.sampleMimeType?.substringAfterLast('/')?.uppercase()
                    listOfNotNull("Subtitle Track ${trackIndex + 1}", codec).joinToString(" • ")
                }
            }

            val option = MediaTrackOption(
                groupIndex = groupIndex,
                trackIndex = trackIndex,
                type = type,
                label = label,
                language = lang,
                isSelected = isSelected
            )

            if (type == C.TRACK_TYPE_AUDIO) {
                audioList.add(option)
            } else {
                textList.add(option)
            }
        }
    }
    return Pair(audioList, textList)
}

@Composable
private fun OfflineActionButton(
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
            .padding(horizontal = 6.dp, vertical = 6.dp)
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

private fun loadCuesForTrack(
    option: MediaTrackOption?,
    file: File?,
    dbTracks: List<SubtitleTrackEntity>
): List<SubtitleCue>? {
    if (file == null || option == null) return null

    // 1. Check dbTracks matching label or language
    val dbMatch = dbTracks.find {
        it.label.equals(option.label, ignoreCase = true) ||
        (option.language != null && it.languageTag.equals(option.language, ignoreCase = true))
    }
    if (dbMatch != null) {
        val f = File(dbMatch.path)
        if (f.exists()) {
            return try { SubtitleCueParser.parse(f.readText(), f.extension) } catch (_: Exception) { null }
        }
    }

    // 2. Check sidecars
    val sidecars = SidecarSubtitleDetector.findSidecarSubtitles(file)
    val sidecarMatch = sidecars.find {
        it.label.equals(option.label, ignoreCase = true) ||
        (option.language != null && it.languageTag.equals(option.language, ignoreCase = true))
    }
    if (sidecarMatch != null && sidecarMatch.file.exists()) {
        return try { SubtitleCueParser.parse(sidecarMatch.file.readText(), sidecarMatch.file.extension) } catch (_: Exception) { null }
    }

    // 3. Fallback: check any existing subtitle file
    val anySub = dbTracks.map { File(it.path) }.firstOrNull { it.exists() } ?: sidecars.firstOrNull()?.file
    if (anySub != null && anySub.exists()) {
        return try { SubtitleCueParser.parse(anySub.readText(), anySub.extension) } catch (_: Exception) { null }
    }
    return null
}
