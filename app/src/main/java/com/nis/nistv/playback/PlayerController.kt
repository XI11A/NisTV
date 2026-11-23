package com.nis.nistv.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.StreamSourceEntity
import com.nis.nistv.data.health.StreamHealthRecorder
import com.nis.nistv.data.repository.ChannelRepository
import com.nis.nistv.data.saver.DataSaverHelper
import com.nis.nistv.data.saver.DataUsageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@UnstableApi
class PlayerController(
    private val context: Context,
    private val channelRepository: ChannelRepository,
    private val streamHealthRecorder: StreamHealthRecorder? = null,
    private val dataUsageManager: DataUsageManager? = null
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var retryCount = 0
    private val maxRetries = 3
    private var totalDroppedFrames = 0L
    private var streamLatencyMs = 0L
    private var previousChannelId: String? = null
    private var streamPlaybackStartTime = 0L
    private var activeStreamUrl: String? = null

    private val transferListener = object : androidx.media3.datasource.TransferListener {
        override fun onTransferInitializing(source: androidx.media3.datasource.DataSource, dataSpec: androidx.media3.datasource.DataSpec, isNetwork: Boolean) {}
        override fun onTransferStart(source: androidx.media3.datasource.DataSource, dataSpec: androidx.media3.datasource.DataSpec, isNetwork: Boolean) {}
        override fun onBytesTransferred(source: androidx.media3.datasource.DataSource, dataSpec: androidx.media3.datasource.DataSpec, isNetwork: Boolean, bytesTransferred: Int) {
            if (bytesTransferred > 0) {
                dataUsageManager?.recordStreamBytes(bytesTransferred.toLong())
            }
        }
        override fun onTransferEnd(source: androidx.media3.datasource.DataSource, dataSpec: androidx.media3.datasource.DataSpec, isNetwork: Boolean) {}
    }

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(15000)
        .setTransferListener(transferListener)

    val player: ExoPlayer by lazy {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000, // minBufferMs
                50_000, // maxBufferMs
                2_500,  // bufferForPlaybackMs
                5_000   // bufferForPlaybackAfterRebufferMs
            )
            .build()

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(DefaultDataSource.Factory(context, httpDataSourceFactory))

        ExoPlayer.Builder(context)
            .setRenderersFactory(
                DefaultRenderersFactory(context).setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                )
            )
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
            .apply {
                playWhenReady = true
                addListener(playerListener)
                addAnalyticsListener(analyticsListener)
            }
    }

    private val analyticsListener = object : androidx.media3.exoplayer.analytics.AnalyticsListener {
        override fun onDroppedVideoFrames(
            eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
            droppedFrames: Int,
            elapsedMs: Long
        ) {
            totalDroppedFrames += droppedFrames
            updateDiagnostics()
        }

        override fun onLoadCompleted(
            eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
            loadEventInfo: androidx.media3.exoplayer.source.LoadEventInfo,
            mediaLoadData: androidx.media3.exoplayer.source.MediaLoadData
        ) {
            streamLatencyMs = loadEventInfo.loadDurationMs
            updateDiagnostics()
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _uiState.update {
                        it.copy(
                            status = PlaybackStatus.BUFFERING,
                            isBuffering = true
                        )
                    }
                }
                Player.STATE_READY -> {
                    retryCount = 0
                    _uiState.update {
                        it.copy(
                            status = if (player.playWhenReady) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED,
                            isPlaying = player.playWhenReady,
                            isBuffering = false,
                            errorMessage = null,
                            failoverMessage = null
                        )
                    }
                }
                Player.STATE_ENDED -> {
                    _uiState.update {
                        it.copy(
                            status = PlaybackStatus.IDLE,
                            isPlaying = false,
                            isBuffering = false
                        )
                    }
                }
                Player.STATE_IDLE -> {
                    if (_uiState.value.status != PlaybackStatus.ERROR) {
                        _uiState.update {
                            it.copy(
                                status = PlaybackStatus.IDLE,
                                isPlaying = false,
                                isBuffering = false
                            )
                        }
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlayerError(error: PlaybackException) {
            val classifiedError = classifyError(error)
            handlePlaybackError(classifiedError)
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateAvailableTracks(tracks)
        }

        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
            updateDiagnostics()
        }

        override fun onRenderedFirstFrame() {
            val startupMs = if (streamPlaybackStartTime > 0L) {
                System.currentTimeMillis() - streamPlaybackStartTime
            } else 0L
            activeStreamUrl?.let { url ->
                _uiState.value.channel?.let { ch ->
                    streamHealthRecorder?.recordSuccess(url, ch.id, startupMs)
                }
            }
        }
    }

    fun playChannel(channel: ChannelEntity, streams: List<StreamSourceEntity>, initialIndex: Int = 0) {
        val prev = _uiState.value.channel?.id
        if (prev != null && prev != channel.id) {
            previousChannelId = prev
        }
        totalDroppedFrames = 0L
        val safeIndex = if (initialIndex in streams.indices) initialIndex else 0
        _uiState.update {
            it.copy(
                channel = channel,
                streams = streams,
                currentStreamIndex = safeIndex,
                status = PlaybackStatus.LOADING,
                isBuffering = true,
                errorMessage = null,
                failoverMessage = null,
                previousChannelId = previousChannelId
            )
        }
        retryCount = 0

        if (streams.isNotEmpty()) {
            prepareAndPlay(streams[safeIndex], channel)
        } else {
            _uiState.update {
                it.copy(
                    status = PlaybackStatus.ERROR,
                    isBuffering = false,
                    errorMessage = "No stream sources available for this channel."
                )
            }
        }
    }

    private fun prepareAndPlay(source: StreamSourceEntity, channel: ChannelEntity) {
        try {
            streamPlaybackStartTime = System.currentTimeMillis()
            activeStreamUrl = source.url

            // Set stream-specific headers
            val headers = mutableMapOf<String, String>()
            source.userAgent?.let { if (it.isNotBlank()) headers["User-Agent"] = it }
            source.referrer?.let { if (it.isNotBlank()) headers["Referer"] = it }
            if (headers.isNotEmpty()) {
                httpDataSourceFactory.setDefaultRequestProperties(headers)
            } else {
                httpDataSourceFactory.setDefaultRequestProperties(emptyMap())
            }

            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(channel.cleanName.ifEmpty { channel.name })
                .setArtist(channel.category)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(source.url)
                .setMediaMetadata(mediaMetadata)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            if (isBrandingIntroActive) {
                player.playWhenReady = false
                player.volume = 0f
            } else {
                player.volume = 1f
                player.playWhenReady = true
            }
        } catch (e: Throwable) {
            handlePlaybackError(e.localizedMessage ?: "Failed to prepare stream: ${e.javaClass.simpleName}")
        }
    }

    private fun handlePlaybackError(message: String) {
        val currentState = _uiState.value
        val streams = currentState.streams
        val currentIndex = currentState.currentStreamIndex

        // Record failure in local database
        if (currentIndex in streams.indices) {
            val failedStream = streams[currentIndex]
            scope.launch {
                channelRepository.recordStreamFailure(failedStream.id)
            }
            currentState.channel?.let { ch ->
                streamHealthRecorder?.recordFailure(failedStream.url, ch.id)
            }
        }

        // Check if next source is available
        val nextIndex = currentIndex + 1
        if (nextIndex < streams.size) {
            _uiState.update {
                it.copy(
                    currentStreamIndex = nextIndex,
                    status = PlaybackStatus.LOADING,
                    isBuffering = true,
                    failoverMessage = "Trying another source (${nextIndex + 1} of ${streams.size})...",
                    errorMessage = null
                )
            }
            scope.launch {
                delay(1000)
                currentState.channel?.let { channel ->
                    prepareAndPlay(streams[nextIndex], channel)
                }
            }
        } else {
            // No more sources
            _uiState.update {
                it.copy(
                    status = PlaybackStatus.ERROR,
                    isBuffering = false,
                    errorMessage = message,
                    failoverMessage = null
                )
            }
        }
    }

    fun retryCurrentStream() {
        val currentState = _uiState.value
        val streams = currentState.streams
        val currentIndex = currentState.currentStreamIndex

        if (currentIndex in streams.indices && currentState.channel != null) {
            _uiState.update {
                it.copy(
                    status = PlaybackStatus.LOADING,
                    isBuffering = true,
                    errorMessage = null,
                    failoverMessage = "Retrying source..."
                )
            }
            prepareAndPlay(streams[currentIndex], currentState.channel)
        }
    }

    fun selectSource(index: Int) {
        val currentState = _uiState.value
        val streams = currentState.streams

        if (index in streams.indices && currentState.channel != null) {
            _uiState.update {
                it.copy(
                    currentStreamIndex = index,
                    status = PlaybackStatus.LOADING,
                    isBuffering = true,
                    errorMessage = null,
                    failoverMessage = null
                )
            }
            prepareAndPlay(streams[index], currentState.channel)
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
            _uiState.update { it.copy(isPlaying = false, status = PlaybackStatus.PAUSED) }
        } else {
            if (player.playbackState == Player.STATE_IDLE && player.mediaItemCount > 0) {
                player.prepare()
            }
            player.play()
            _uiState.update { it.copy(isPlaying = true, status = PlaybackStatus.PLAYING) }
        }
    }

    fun cycleAspectRatio() {
        val nextMode = when (_uiState.value.aspectRatioMode) {
            AspectRatioMode.FIT -> AspectRatioMode.ZOOM
            AspectRatioMode.ZOOM -> AspectRatioMode.STRETCH
            AspectRatioMode.STRETCH -> AspectRatioMode.FIT
        }
        _uiState.update { it.copy(aspectRatioMode = nextMode) }
    }

    fun setControlsVisible(visible: Boolean) {
        _uiState.update { it.copy(isControlsVisible = visible) }
    }

    private fun updateAvailableTracks(tracks: Tracks) {
        val audioList = mutableListOf<String>()
        val subtitleList = mutableListOf<String>()
        val audioItems = mutableListOf<AvailableTrack>()
        val subtitleItems = mutableListOf<AvailableTrack>()

        for ((groupIndex, group) in tracks.groups.withIndex()) {
            val trackType = group.type
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val label = format.label ?: format.language ?: "Track ${i + 1}"
                val isSelected = group.isTrackSelected(i)
                val trackItem = AvailableTrack(
                    groupIndex = groupIndex,
                    trackIndex = i,
                    label = label,
                    language = format.language,
                    isSelected = isSelected
                )
                when (trackType) {
                    C.TRACK_TYPE_AUDIO -> {
                        audioList.add(label)
                        audioItems.add(trackItem)
                    }
                    C.TRACK_TYPE_TEXT -> {
                        subtitleList.add(label)
                        subtitleItems.add(trackItem)
                    }
                }
            }
        }

        _uiState.update {
            it.copy(
                audioTracks = audioList,
                subtitleTracks = subtitleList,
                audioTrackItems = audioItems,
                subtitleTrackItems = subtitleItems
            )
        }
        updateDiagnostics()
    }

    fun updateDiagnostics() {
        val videoFormat = player.videoFormat
        val audioFormat = player.audioFormat
        val videoSize = player.videoSize
        val res = if (videoSize.width > 0 && videoSize.height > 0) {
            "${videoSize.width}x${videoSize.height}"
        } else null

        val bitrate = if (videoFormat != null && videoFormat.bitrate > 0) {
            videoFormat.bitrate / 1000
        } else 0

        val bufferSec = (player.totalBufferedDuration / 1000f).coerceAtLeast(0f)

        _uiState.update { current ->
            current.copy(
                diagnostics = StreamDiagnostics(
                    videoCodec = videoFormat?.sampleMimeType ?: videoFormat?.codecs,
                    resolution = res,
                    bitrateKbps = bitrate,
                    audioSampleRate = audioFormat?.sampleRate ?: 0,
                    audioChannels = audioFormat?.channelCount ?: 0,
                    droppedFrames = totalDroppedFrames,
                    bufferHealthSeconds = bufferSec,
                    sourceIndex = current.currentStreamIndex + 1,
                    totalSources = current.streams.size,
                    latencyMs = streamLatencyMs
                )
            )
        }
    }

    fun selectAudioTrack(groupIndex: Int, trackIndex: Int) {
        val tracks = player.currentTracks
        val group = tracks.groups.getOrNull(groupIndex) ?: return
        val override = TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .setOverrideForType(override)
            .build()
        updateAvailableTracks(player.currentTracks)
    }

    fun selectSubtitleTrack(groupIndex: Int, trackIndex: Int) {
        if (trackIndex < 0) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            val tracks = player.currentTracks
            val group = tracks.groups.getOrNull(groupIndex) ?: return
            val override = TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(override)
                .build()
        }
        updateAvailableTracks(player.currentTracks)
    }

    fun toggleDiagnostics() {
        _uiState.update { it.copy(showDiagnostics = !it.showDiagnostics) }
    }

    private fun classifyError(error: PlaybackException): String {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "Network connection timed out. Check your internet connection."
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                "Stream server returned an HTTP error."
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                "Unsupported stream playlist format."
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                "Media decoder failure on this device."
            else ->
                error.localizedMessage ?: "Playback error encountered."
        }
    }

    fun setListenMode(enabled: Boolean) {
        _uiState.update { it.copy(isListenMode = enabled) }
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, enabled)
            .build()
    }

    fun toggleListenMode() {
        setListenMode(!_uiState.value.isListenMode)
    }

    fun applyCellularCap(cap: String, isMetered: Boolean) {
        val size = DataSaverHelper.resolveMaxVideoSize(cap, isMetered)
        player.trackSelectionParameters = if (size != null) {
            player.trackSelectionParameters.buildUpon()
                .setMaxVideoSize(size.first, size.second)
                .build()
        } else {
            player.trackSelectionParameters.buildUpon()
                .clearVideoSizeConstraints()
                .build()
        }
    }

    fun setProgrammeTitle(title: String?) {
        _uiState.update { it.copy(programmeTitle = title) }
    }

    var isBrandingIntroActive: Boolean = false
        private set

    fun setBrandingIntroActive(active: Boolean) {
        isBrandingIntroActive = active
        if (active) {
            player.playWhenReady = false
            player.volume = 0f
        } else {
            player.volume = 1f
            player.playWhenReady = true
            play()
        }
    }

    fun play() {
        if (!player.isPlaying) {
            player.play()
            _uiState.update { it.copy(isPlaying = true, status = PlaybackStatus.PLAYING) }
        }
    }

    fun pause() {
        if (player.isPlaying) {
            player.pause()
            _uiState.update { it.copy(isPlaying = false, status = PlaybackStatus.PAUSED) }
        }
    }

    suspend fun fadeVolumeAndPause(durationMs: Long = 5000L) {
        val startVolume = player.volume
        val steps = 10
        val stepDelay = durationMs / steps
        for (i in 1..steps) {
            val vol = (startVolume * (1f - (i.toFloat() / steps))).coerceIn(0f, 1f)
            player.volume = vol
            kotlinx.coroutines.delay(stepDelay)
        }
        pause()
        player.volume = startVolume
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        _uiState.value = PlayerUiState()
    }

    fun release() {
        player.removeListener(playerListener)
        player.release()
    }
}
