package com.nis.nistv.feature.downloads.components

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nis.nistv.NisTvApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.media3.common.C
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.SubtitleView
import com.nis.nistv.core.preferences.SubtitleStylePreferences
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*
import kotlinx.coroutines.delay
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun OfflineMediaPlayerDialog(
    download: DownloadEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(download.filePath, download.fileName) {
        val primary = File(download.filePath)
        if (primary.exists()) primary
        else {
            val publicFallback = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NisTV/${download.fileName}")
            if (publicFallback.exists()) publicFallback
            else {
                val appFallback = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "NisTV/${download.fileName}")
                if (appFallback.exists()) appFallback else primary
            }
        }
    }

    if (!file.exists()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = NisTheme.colors.surface,
            title = { Text("File Not Found", color = NisTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "The downloaded media file no longer exists on disk:\n${download.filePath}",
                    color = NisTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NisAccentPrimary)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        )
        return
    }

    val app = context.applicationContext as? NisTvApplication
    val downloadDao = remember { app?.container?.database?.downloadDao() }
    var hasResumedInitialPosition by remember { mutableStateOf(false) }

    val savePositionToDb: (Long) -> Unit = { pos ->
        if (downloadDao != null) {
            app?.applicationScope?.launch(Dispatchers.IO) {
                try {
                    downloadDao.updatePlaybackPosition(download.id, pos)
                } catch (_: Exception) {}
            }
        }
    }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setRenderersFactory(
                DefaultRenderersFactory(context).setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                )
            )
            .build().apply {
                playWhenReady = true
            }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var controlsVisible by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // User preferences & Subtitle styling
    val userPreferences = remember { app?.container?.userPreferences }
    val subtitleStyle by (userPreferences?.subtitleStylePreferences ?: kotlinx.coroutines.flow.flowOf(SubtitleStylePreferences()))
        .collectAsState(initial = SubtitleStylePreferences())

    var showTrackDialog by remember { mutableStateOf(false) }
    var activeTrackTab by remember { mutableStateOf(TrackDialogTab.SUBTITLES) }

    var availableAudioTracks by remember { mutableStateOf<List<MediaTrackOption>>(emptyList()) }
    var availableSubtitleTracks by remember { mutableStateOf<List<MediaTrackOption>>(emptyList()) }

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
        app?.applicationScope?.launch(Dispatchers.IO) {
            try {
                userPreferences?.saveSubtitleStylePreferences(newStyle)
            } catch (_: Exception) {}
        }
    }

    // Prepare media
    LaunchedEffect(file.absolutePath) {
        val mediaItem = MediaItem.Builder()
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
            .build()
        val resumePos = download.lastPlaybackPositionMs.takeIf { it > 5_000L } ?: 0L
        if (resumePos > 5_000L) {
            exoPlayer.setMediaItem(mediaItem, resumePos)
            exoPlayer.seekTo(resumePos)
            currentPosition = resumePos
        } else {
            exoPlayer.setMediaItem(mediaItem)
            hasResumedInitialPosition = true
        }
        exoPlayer.prepare()
        exoPlayer.play()
    }

    // Player events listener
    DisposableEffect(exoPlayer) {
        val checkAndApplyResume: () -> Unit = {
            if (!hasResumedInitialPosition) {
                val targetPos = download.lastPlaybackPositionMs
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
                        savePositionToDb(if (isFinished) 0L else pos)
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        val d = exoPlayer.duration
                        if (d > 0 && d != C.TIME_UNSET) duration = d
                        checkAndApplyResume()
                        updateAvailableTracks(exoPlayer.currentTracks)
                    }
                    Player.STATE_ENDED -> {
                        isPlaying = false
                        savePositionToDb(0L)
                    }
                    else -> {}
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateAvailableTracks(tracks)
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                val d = exoPlayer.duration
                if (d > 0 && d != C.TIME_UNSET) duration = d
                checkAndApplyResume()
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            if (hasResumedInitialPosition) {
                val finalPos = exoPlayer.currentPosition.takeIf { it > 0 } ?: currentPosition
                if (finalPos > 0) {
                    val isFinished = duration > 0 && finalPos >= duration - 5_000L
                    savePositionToDb(if (isFinished) 0L else finalPos)
                }
            }
            exoPlayer.removeListener(listener)
            exoPlayer.release()
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
                        savePositionToDb(if (isFinished) 0L else pos)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Position tracking loop
    LaunchedEffect(exoPlayer) {
        var lastSavedSec = download.lastPlaybackPositionMs / 1000L
        while (true) {
            val d = exoPlayer.duration
            if (d > 0 && d != C.TIME_UNSET) duration = d
            isPlaying = exoPlayer.isPlaying
            if (!isDraggingSlider && hasResumedInitialPosition) {
                val curPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                currentPosition = curPos
                val currentSec = curPos / 1000L
                if (Math.abs(currentSec - lastSavedSec) >= 5) {
                    lastSavedSec = currentSec
                    val isFinished = duration > 0 && curPos >= duration - 5_000L
                    savePositionToDb(if (isFinished) 0L else curPos)
                }
            }
            delay(250)
        }
    }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(4000)
            controlsVisible = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    controlsVisible = !controlsVisible
                }
        ) {
            // 1. ExoPlayer Video Surface
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        this.player = exoPlayer
                        this.resizeMode = resizeMode
                        setBackgroundColor(android.graphics.Color.BLACK)
                        subtitleView?.apply {
                            setStyle(subtitleStyle.toCaptionStyleCompat())
                            setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitleStyle.textSize.scale)
                        }
                    }
                },
                update = { playerView ->
                    playerView.resizeMode = resizeMode
                    playerView.subtitleView?.apply {
                        setStyle(subtitleStyle.toCaptionStyleCompat())
                        setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitleStyle.textSize.scale)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Overlay Controls
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top gradient scrim
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(NisScrimStrong, Color.Transparent)
                                )
                            )
                    )

                    // Bottom gradient scrim
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, NisScrimStrong)
                                )
                            )
                    )

                    // Top Bar (Back, Title, Aspect Ratio, External App)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(NisScrimSoft)
                            ) {
                                Icon(
                                    imageVector = NisIcons.Back,
                                    contentDescription = "Back",
                                    tint = NisTextPrimary
                                )
                            }

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
                                if (!download.episodeName.isNullOrBlank() || download.seasonNumber != null) {
                                    val seasonEpText = buildString {
                                        if (download.seasonNumber != null && download.episodeNumber != null) {
                                            append("S${download.seasonNumber} E${download.episodeNumber}")
                                        }
                                        if (!download.episodeName.isNullOrBlank()) {
                                            if (isNotEmpty()) append(" • ")
                                            append(download.episodeName)
                                        }
                                    }
                                    Text(
                                        text = seasonEpText,
                                        color = NisAccentPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = "Offline Playback • ${download.formattedTotal}",
                                        color = NisTextSecondary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        // Right actions: Audio, Subtitles, Aspect Ratio toggle & Open in external app
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Audio Tracks button
                            IconButton(
                                onClick = {
                                    activeTrackTab = TrackDialogTab.AUDIO
                                    showTrackDialog = true
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(NisScrimSoft)
                            ) {
                                Icon(
                                    imageVector = NisIcons.Audiotrack,
                                    contentDescription = "Audio Tracks",
                                    tint = NisTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Subtitles & Captions button
                            IconButton(
                                onClick = {
                                    activeTrackTab = TrackDialogTab.SUBTITLES
                                    showTrackDialog = true
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(NisScrimSoft)
                            ) {
                                Icon(
                                    imageVector = NisIcons.Subtitles,
                                    contentDescription = "Captions & Subtitles",
                                    tint = NisTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Aspect Ratio Mode Button
                            IconButton(
                                onClick = {
                                    resizeMode = when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(NisScrimSoft)
                            ) {
                                Icon(
                                    imageVector = NisIcons.AspectRatio,
                                    contentDescription = "Aspect Ratio",
                                    tint = NisTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Open in external player button
                            IconButton(
                                onClick = {
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "video/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open external player: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(NisScrimSoft)
                            ) {
                                Icon(
                                    imageVector = NisIcons.Fullscreen,
                                    contentDescription = "External Player",
                                    tint = NisTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Center Play/Pause & Quick Seek Buttons
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        // Rewind 10 seconds
                        IconButton(
                            onClick = {
                                val target = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(target)
                                currentPosition = target
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(NisScrimMedium)
                        ) {
                            Icon(
                                imageVector = NisIcons.SkipPrevious,
                                contentDescription = "Rewind 10s",
                                tint = NisTextPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Play/Pause Big Button
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            },
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(NisAccentPrimary)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) NisIcons.Pause else NisIcons.Play,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = NisBackground,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Fast Forward 10 seconds
                        IconButton(
                            onClick = {
                                val max = if (duration > 0) duration else Long.MAX_VALUE
                                val target = (exoPlayer.currentPosition + 10000L).coerceAtMost(max)
                                exoPlayer.seekTo(target)
                                currentPosition = target
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(NisScrimMedium)
                        ) {
                            Icon(
                                imageVector = NisIcons.SkipNext,
                                contentDescription = "Forward 10s",
                                tint = NisTextPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Bottom Scrubber & Timestamps
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        // Position timestamps & aspect mode badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val displayPos = if (isDraggingSlider) (sliderValue * duration).toLong() else currentPosition
                            Text(
                                text = "${formatDuration(displayPos)} / ${formatDuration(duration)}",
                                color = NisTextPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            val aspectLabel = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> "FIT"
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "CROP (FILL)"
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> "STRETCH"
                                else -> "FIT"
                            }
                            Text(
                                text = aspectLabel,
                                color = NisAccentCyan,
                                style = NisTheme.textExtras.micro,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Scrubber Slider
                        val sliderPosition = if (duration > 0) {
                            if (isDraggingSlider) sliderValue else (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        Slider(
                            value = sliderPosition,
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
                                    savePositionToDb(if (isFinished) 0L else targetMs)
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
    }

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
            onDismiss = { showTrackDialog = false }
        )
    }
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

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    return if (hours > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}
