package com.nis.nistv.playback

import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.StreamSourceEntity

enum class AspectRatioMode(val label: String) {
    FIT("Fit (16:9)"),
    ZOOM("Zoom (Fill)"),
    STRETCH("Stretch")
}

enum class PlaybackStatus {
    IDLE,
    LOADING,
    BUFFERING,
    PLAYING,
    PAUSED,
    ERROR
}

data class StreamDiagnostics(
    val videoCodec: String? = null,
    val resolution: String? = null,
    val bitrateKbps: Int = 0,
    val audioSampleRate: Int = 0,
    val audioChannels: Int = 0,
    val droppedFrames: Long = 0L,
    val bufferHealthSeconds: Float = 0f,
    val sourceIndex: Int = 0,
    val totalSources: Int = 0,
    val latencyMs: Long = 0L
)

data class AvailableTrack(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String? = null,
    val isSelected: Boolean = false
)

data class PlayerUiState(
    val channel: ChannelEntity? = null,
    val streams: List<StreamSourceEntity> = emptyList(),
    val currentStreamIndex: Int = 0,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null,
    val failoverMessage: String? = null,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val isControlsVisible: Boolean = true,
    val isFavorite: Boolean = false,
    val audioTracks: List<String> = emptyList(),
    val subtitleTracks: List<String> = emptyList(),
    val audioTrackItems: List<AvailableTrack> = emptyList(),
    val subtitleTrackItems: List<AvailableTrack> = emptyList(),
    val selectedAudioTrackIndex: Int = -1,
    val selectedSubtitleIndex: Int = -1,
    val programmeTitle: String? = null,
    val diagnostics: StreamDiagnostics = StreamDiagnostics(),
    val showDiagnostics: Boolean = false,
    val previousChannelId: String? = null,
    val isListenMode: Boolean = false
)
