package com.nis.nistv.playback.multiview

import android.app.ActivityManager
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class MultiViewController(
    private val context: Context,
    private val streamHealthRecorder: StreamHealthRecorder? = null
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _uiState = MutableStateFlow(MultiViewState())
    val uiState: StateFlow<MultiViewState> = _uiState.asStateFlow()

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    fun isLowRamDevice(): Boolean {
        return activityManager?.isLowRamDevice ?: false
    }

    private fun createPlayer(maxVideoWidth: Int, maxVideoHeight: Int): ExoPlayer {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(10_000, 30_000, 2_000, 3_000)
            .build()

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(DefaultDataSource.Factory(context, httpDataSourceFactory))

        return ExoPlayer.Builder(context)
            .setRenderersFactory(
                DefaultRenderersFactory(context).setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                )
            )
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
                // Cap multi-view video bandwidth to conserve memory and network
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setMaxVideoSize(maxVideoWidth, maxVideoHeight)
                    .build()
            }
    }

    val primaryPlayer: ExoPlayer by lazy {
        createPlayer(1280, 720).apply {
            volume = 1f
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> _uiState.update {
                            it.copy(primarySlot = it.primarySlot.copy(isBuffering = true))
                        }
                        Player.STATE_READY -> _uiState.update {
                            it.copy(primarySlot = it.primarySlot.copy(isBuffering = false, isPlaying = playWhenReady, errorMessage = null))
                        }
                        Player.STATE_ENDED, Player.STATE_IDLE -> _uiState.update {
                            it.copy(primarySlot = it.primarySlot.copy(isBuffering = false, isPlaying = false))
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.update { it.copy(primarySlot = it.primarySlot.copy(isPlaying = isPlaying)) }
                }

                override fun onPlayerError(error: PlaybackException) {
                    handleSlotError(MultiViewSlot.PRIMARY)
                }
            })
        }
    }

    val secondaryPlayer: ExoPlayer by lazy {
        createPlayer(854, 480).apply {
            volume = 0f // Inactive audio by default
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> _uiState.update {
                            it.copy(secondarySlot = it.secondarySlot.copy(isBuffering = true))
                        }
                        Player.STATE_READY -> _uiState.update {
                            it.copy(secondarySlot = it.secondarySlot.copy(isBuffering = false, isPlaying = playWhenReady, errorMessage = null))
                        }
                        Player.STATE_ENDED, Player.STATE_IDLE -> _uiState.update {
                            it.copy(secondarySlot = it.secondarySlot.copy(isBuffering = false, isPlaying = false))
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.update { it.copy(secondarySlot = it.secondarySlot.copy(isPlaying = isPlaying)) }
                }

                override fun onPlayerError(error: PlaybackException) {
                    handleSlotError(MultiViewSlot.SECONDARY)
                }
            })
        }
    }

    fun startMultiView(
        primaryChannel: ChannelEntity,
        primaryStreams: List<StreamSourceEntity>,
        secondaryChannel: ChannelEntity,
        secondaryStreams: List<StreamSourceEntity>
    ) {
        _uiState.update {
            it.copy(
                primarySlot = MultiViewSlotState(channel = primaryChannel, streams = primaryStreams),
                secondarySlot = MultiViewSlotState(channel = secondaryChannel, streams = secondaryStreams),
                activeAudioSlot = MultiViewSlot.PRIMARY,
                isActive = true
            )
        }

        playSlot(MultiViewSlot.PRIMARY, 0)
        playSlot(MultiViewSlot.SECONDARY, 0)
        setAudioSlot(MultiViewSlot.PRIMARY)
    }

    fun setSecondaryChannel(channel: ChannelEntity, streams: List<StreamSourceEntity>) {
        _uiState.update {
            it.copy(
                secondarySlot = MultiViewSlotState(channel = channel, streams = streams),
                isActive = true
            )
        }
        playSlot(MultiViewSlot.SECONDARY, 0)
    }

    fun setAudioSlot(slot: MultiViewSlot) {
        _uiState.update { it.copy(activeAudioSlot = slot) }
        when (slot) {
            MultiViewSlot.PRIMARY -> {
                primaryPlayer.volume = 1f
                secondaryPlayer.volume = 0f
            }
            MultiViewSlot.SECONDARY -> {
                primaryPlayer.volume = 0f
                secondaryPlayer.volume = 1f
            }
        }
    }

    private fun playSlot(slot: MultiViewSlot, streamIndex: Int) {
        val (player, slotState) = when (slot) {
            MultiViewSlot.PRIMARY -> Pair(primaryPlayer, _uiState.value.primarySlot)
            MultiViewSlot.SECONDARY -> Pair(secondaryPlayer, _uiState.value.secondarySlot)
        }

        val streams = slotState.streams
        val channel = slotState.channel ?: return
        if (streamIndex !in streams.indices) return

        val source = streams[streamIndex]
        try {
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(channel.cleanName.ifEmpty { channel.name })
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(source.url)
                .setMediaMetadata(mediaMetadata)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        } catch (_: Exception) {
            handleSlotError(slot)
        }
    }

    private fun handleSlotError(slot: MultiViewSlot) {
        val slotState = if (slot == MultiViewSlot.PRIMARY) _uiState.value.primarySlot else _uiState.value.secondarySlot
        val nextIndex = slotState.currentStreamIndex + 1

        if (nextIndex < slotState.streams.size) {
            _uiState.update {
                if (slot == MultiViewSlot.PRIMARY) {
                    it.copy(primarySlot = it.primarySlot.copy(currentStreamIndex = nextIndex, isBuffering = true))
                } else {
                    it.copy(secondarySlot = it.secondarySlot.copy(currentStreamIndex = nextIndex, isBuffering = true))
                }
            }
            scope.launch {
                delay(1000)
                playSlot(slot, nextIndex)
            }
        } else {
            _uiState.update {
                if (slot == MultiViewSlot.PRIMARY) {
                    it.copy(primarySlot = it.primarySlot.copy(isBuffering = false, errorMessage = "Playback error"))
                } else {
                    it.copy(secondarySlot = it.secondarySlot.copy(isBuffering = false, errorMessage = "Playback error"))
                }
            }
        }
    }

    fun pauseBoth() {
        if (primaryPlayer.isPlaying) primaryPlayer.pause()
        if (secondaryPlayer.isPlaying) secondaryPlayer.pause()
    }

    fun resumeBoth() {
        if (_uiState.value.isActive) {
            primaryPlayer.play()
            secondaryPlayer.play()
        }
    }

    fun closeSlot(slot: MultiViewSlot) {
        when (slot) {
            MultiViewSlot.PRIMARY -> {
                primaryPlayer.stop()
                _uiState.update { it.copy(primarySlot = MultiViewSlotState()) }
            }
            MultiViewSlot.SECONDARY -> {
                secondaryPlayer.stop()
                _uiState.update { it.copy(secondarySlot = MultiViewSlotState(), activeAudioSlot = MultiViewSlot.PRIMARY) }
                primaryPlayer.volume = 1f
            }
        }
        if (_uiState.value.primarySlot.channel == null && _uiState.value.secondarySlot.channel == null) {
            _uiState.update { it.copy(isActive = false) }
        }
    }

    fun stop() {
        primaryPlayer.stop()
        secondaryPlayer.stop()
        _uiState.value = MultiViewState()
    }

    fun release() {
        primaryPlayer.stop()
        secondaryPlayer.stop()
        primaryPlayer.release()
        secondaryPlayer.release()
        _uiState.value = MultiViewState()
    }
}
