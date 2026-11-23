package com.nis.nistv.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.nis.nistv.data.tmdb.model.TmdbMediaItem
import com.nis.nistv.data.tmdb.model.TmdbMediaType

/**
 * Entity representing a movie or series saved to the user's "Watch Later" / "My List".
 */
@Entity(
    tableName = "watch_later",
    primaryKeys = ["profileId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"])
    ]
)
data class WatchLaterEntity(
    val profileId: String = "default",
    val id: String, // Composite unique key: "${mediaType}_${mediaId}" e.g. "movie_550" or "tv_1399"
    val mediaId: Long,
    val mediaType: String, // "movie" or "tv"
    val title: String,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val overview: String? = null,
    val releaseYear: String? = null,
    val voteAverage: Double? = null,
    val genres: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

fun WatchLaterEntity.toTmdbMediaItem(): TmdbMediaItem = TmdbMediaItem(
    id = mediaId,
    title = title,
    overview = overview ?: "",
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseYear = releaseYear,
    voteAverage = voteAverage ?: 0.0,
    mediaType = if (mediaType.equals("tv", ignoreCase = true)) TmdbMediaType.TV else TmdbMediaType.MOVIE
)
