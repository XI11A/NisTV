package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: String = "default",
    val name: String = "Primary User",
    val avatarId: String = "avatar_01",
    val preferredLanguage: String = "Hindi",
    val preferredCountry: String = "IN",
    val isKids: Boolean = false,
    val isLocked: Boolean = false,
    val historyPaused: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
