package com.nis.nistv.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.StreamSourceEntity

data class ChannelWithStreams(
    @Embedded
    val channel: ChannelEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "channelId"
    )
    val streams: List<StreamSourceEntity>
)
