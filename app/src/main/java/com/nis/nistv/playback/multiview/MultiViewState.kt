package com.nis.nistv.playback.multiview

import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.StreamSourceEntity

enum class MultiViewSlot {
    PRIMARY,
    SECONDARY
}

data class MultiViewSlotState(
    val channel: ChannelEntity? = null,
    val streams: List<StreamSourceEntity> = emptyList(),
    val currentStreamIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null
)

data class MultiViewState(
    val primarySlot: MultiViewSlotState = MultiViewSlotState(),
    val secondarySlot: MultiViewSlotState = MultiViewSlotState(),
    val activeAudioSlot: MultiViewSlot = MultiViewSlot.PRIMARY,
    val isActive: Boolean = false
) {
    val isDualStreaming: Boolean
        get() = isActive && primarySlot.channel != null && secondarySlot.channel != null
}
