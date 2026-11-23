package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "program_reminders",
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["channelId"]),
        Index(value = ["startEpochMs"])
    ]
)
data class ProgramReminderEntity(
    @PrimaryKey
    val id: String,
    val profileId: String = "default",
    val channelId: String,
    val programTitle: String,
    val startEpochMs: Long,
    val notified: Boolean = false
)
