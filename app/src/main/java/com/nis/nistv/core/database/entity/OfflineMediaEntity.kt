package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class OfflineCastMember(
    val id: Long,
    val name: String,
    val character: String? = null,
    val localProfilePath: String? = null
)

@Serializable
data class OfflineCrewMember(
    val id: Long,
    val name: String,
    val job: String, // "Director", "Creator", "Writer", etc.
    val department: String? = null
)

@Entity(
    tableName = "offline_media",
    indices = [
        Index(value = ["tmdbId", "mediaType"]),
        Index(value = ["savedAt"])
    ]
)
data class OfflineMediaEntity(
    @PrimaryKey
    val id: String, // "${mediaType}_${tmdbId}"
    val tmdbId: Long,
    val mediaType: String, // "movie" or "tv"
    val title: String,
    val originalTitle: String? = null,
    val tagline: String? = null,
    val overview: String = "",
    val releaseDate: String? = null,
    val status: String? = null,
    val rating: Double = 0.0,
    val voteCount: Int = 0,
    val runtimeMinutes: Int? = null,
    val genres: String? = null,
    val originalLanguage: String? = null,
    val certification: String? = null,
    val numberOfSeasons: Int? = null,
    val numberOfEpisodes: Int? = null,
    val castJson: String = "[]",
    val crewJson: String = "[]",
    val localPosterPath: String? = null,
    val localBackdropPath: String? = null,
    val localLogoPath: String? = null,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "offline_seasons",
    primaryKeys = ["seriesKey", "seasonNumber"],
    indices = [
        Index(value = ["seriesKey"])
    ]
)
data class OfflineSeasonEntity(
    val seriesKey: String, // e.g. "tv_1399"
    val seasonNumber: Int,
    val name: String,
    val overview: String? = null,
    val airDate: String? = null,
    val episodeCount: Int = 0,
    val localPosterPath: String? = null
)

@Entity(
    tableName = "offline_episodes",
    primaryKeys = ["seriesKey", "seasonNumber", "episodeNumber"],
    indices = [
        Index(value = ["seriesKey"]),
        Index(value = ["seriesKey", "seasonNumber"])
    ]
)
data class OfflineEpisodeEntity(
    val seriesKey: String, // e.g. "tv_1399"
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String,
    val overview: String = "",
    val airDate: String? = null,
    val runtimeMinutes: Int? = null,
    val rating: Double = 0.0,
    val localStillPath: String? = null
)
