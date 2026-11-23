package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks on-device interactions (watch, download, favorite, view)
 * used by the intelligent recommendation engine to dynamically update
 * user affinity vectors and recommend personalized channels and TMDB media.
 */
@Entity(
    tableName = "media_interactions",
    indices = [
        Index(value = ["profileId", "timestamp"]),
        Index(value = ["mediaType", "mediaId"]),
        Index(value = ["timestamp"]),
        Index(value = ["interactionType"])
    ]
)
data class MediaInteractionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val profileId: String = "default",
    val mediaType: String, // "CHANNEL", "MOVIE", "TV"
    val mediaId: String,   // Channel ID or TMDB ID
    val title: String,
    val categoryOrGenres: String = "", // e.g. "News" or comma-separated TMDB genre IDs "28,12,878"
    val language: String = "",         // e.g. "Tamil", "Hindi", "en", "es"
    val posterOrLogoUrl: String? = null,
    val interactionType: String = "WATCH", // "WATCH", "COMPLETE", "FAVORITE", "DOWNLOAD", "VIEW"
    val watchDurationMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)
