package com.nis.nistv.data.tmdb.model

import com.nis.nistv.core.database.entity.WatchLaterEntity

/**
 * Centralized TMDB Image URL builder for consistent sizing and fallback caching.
 */
object TmdbImageUrls {
    private const val BASE = "https://image.tmdb.org/t/p"

    fun backdrop(path: String, size: String = "w780"): String =
        if (path.startsWith("http")) path else "$BASE/$size${if (path.startsWith("/")) "" else "/"}$path"

    fun poster(path: String, size: String = "w500"): String =
        if (path.startsWith("http")) path else "$BASE/$size${if (path.startsWith("/")) "" else "/"}$path"
}

/**
 * Thin in-memory preview model carrying essential visual metadata for instant
 * shared element container transforms and placeholder-first details screens.
 */
data class MediaPreview(
    val tmdbId: Int,                 // TMDB id
    val mediaType: String,           // "movie" | "tv"
    val title: String,
    val backdropPath: String?,       // TMDB backdrop_path
    val posterPath: String?,         // TMDB poster_path
    val releaseYear: String? = null,
    val voteAverage: Double? = null,
) {
    val backdropUrl: String? get() = backdropPath?.let { TmdbImageUrls.backdrop(it) }
    val posterUrl: String?   get() = posterPath?.let   { TmdbImageUrls.poster(it) }
    val backdropCacheKey: String get() = "tmdb_backdrop_${mediaType}_$tmdbId"
    val posterCacheKey: String   get() = "tmdb_poster_${mediaType}_$tmdbId"
}

fun TmdbMediaItem.toPreview(): MediaPreview = MediaPreview(
    tmdbId = id.toInt(),
    mediaType = mediaType.value,
    title = title,
    backdropPath = backdropPath,
    posterPath = posterPath,
    releaseYear = releaseYear,
    voteAverage = voteAverage
)

fun TmdbMovieDetails.toPreview(): MediaPreview = MediaPreview(
    tmdbId = id.toInt(),
    mediaType = "movie",
    title = title,
    backdropPath = backdropPath,
    posterPath = posterPath,
    releaseYear = releaseDate?.take(4),
    voteAverage = voteAverage
)

fun TmdbTvDetails.toPreview(): MediaPreview = MediaPreview(
    tmdbId = id.toInt(),
    mediaType = "tv",
    title = name,
    backdropPath = backdropPath,
    posterPath = posterPath,
    releaseYear = firstAirDate?.take(4),
    voteAverage = voteAverage
)

fun TmdbMovieDto.toPreview(): MediaPreview = MediaPreview(
    tmdbId = id.toInt(),
    mediaType = "movie",
    title = title,
    backdropPath = backdropPath,
    posterPath = posterPath,
    releaseYear = releaseDate?.take(4),
    voteAverage = voteAverage
)

fun TmdbTvDto.toPreview(): MediaPreview = MediaPreview(
    tmdbId = id.toInt(),
    mediaType = "tv",
    title = name,
    backdropPath = backdropPath,
    posterPath = posterPath,
    releaseYear = firstAirDate?.take(4),
    voteAverage = voteAverage
)

fun WatchLaterEntity.toPreview(): MediaPreview = MediaPreview(
    tmdbId = mediaId.toInt(),
    mediaType = mediaType,
    title = title,
    backdropPath = backdropPath,
    posterPath = posterPath,
    releaseYear = releaseYear,
    voteAverage = voteAverage
)
