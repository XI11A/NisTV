package com.nis.nistv.data.tmdb.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbPagedResponse<T>(
    val page: Int = 1,
    val results: List<T> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_results") val totalResults: Int = 0
)

@Serializable
data class TmdbMovieDto(
    val id: Long,
    val title: String = "",
    @SerialName("original_title") val originalTitle: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    val popularity: Double = 0.0
)

@Serializable
data class TmdbTvDto(
    val id: Long,
    val name: String = "",
    @SerialName("original_name") val originalName: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    val popularity: Double = 0.0
)

@Serializable
data class TmdbGenre(
    val id: Int,
    val name: String
)

@Serializable
data class TmdbGenreResponse(
    val genres: List<TmdbGenre> = emptyList()
)

@Serializable
data class TmdbCastMember(
    val id: Long,
    val name: String,
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null
) {
    val fullProfileUrl: String?
        get() = profilePath?.let { "https://image.tmdb.org/t/p/w185$it" }
}

@Serializable
data class TmdbCrewMember(
    val id: Long,
    val name: String,
    val job: String = "",
    val department: String? = null
)

@Serializable
data class TmdbCreator(
    val id: Long,
    val name: String
)

@Serializable
data class TmdbCredits(
    val cast: List<TmdbCastMember> = emptyList(),
    val crew: List<TmdbCrewMember> = emptyList()
)

@Serializable
data class TmdbVideoDto(
    val id: String,
    val key: String,
    val name: String,
    val site: String,
    val type: String,
    val official: Boolean = false
) {
    val isYouTubeTrailer: Boolean
        get() = site.equals("YouTube", ignoreCase = true) &&
                (type.equals("Trailer", ignoreCase = true) || type.equals("Teaser", ignoreCase = true))

    val youTubeUrl: String
        get() = "https://www.youtube.com/watch?v=$key"
}

@Serializable
data class TmdbVideosResponse(
    val results: List<TmdbVideoDto> = emptyList()
)

@Serializable
data class TmdbProviderInfo(
    @SerialName("provider_id") val providerId: Int,
    @SerialName("provider_name") val providerName: String,
    @SerialName("logo_path") val logoPath: String? = null
) {
    val fullLogoUrl: String?
        get() = logoPath?.let { "https://image.tmdb.org/t/p/w92$it" }
}

@Serializable
data class TmdbCountryWatchProviders(
    val link: String? = null,
    val flatrate: List<TmdbProviderInfo> = emptyList(),
    val rent: List<TmdbProviderInfo> = emptyList(),
    val buy: List<TmdbProviderInfo> = emptyList()
)

@Serializable
data class TmdbWatchProvidersResponse(
    val results: Map<String, TmdbCountryWatchProviders> = emptyMap()
)

@Serializable
data class TmdbSeasonDto(
    val id: Long,
    @SerialName("season_number") val seasonNumber: Int,
    val name: String,
    @SerialName("episode_count") val episodeCount: Int = 0,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("air_date") val airDate: String? = null
)

@Serializable
data class TmdbEpisodeDto(
    val id: Long,
    @SerialName("episode_number") val episodeNumber: Int,
    @SerialName("season_number") val seasonNumber: Int = 1,
    val name: String,
    val overview: String = "",
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0
) {
    val fullStillUrl: String?
        get() = stillPath?.let { "https://image.tmdb.org/t/p/w300$it" }
}

@Serializable
data class TmdbSeasonDetails(
    val id: Long,
    @SerialName("season_number") val seasonNumber: Int,
    val name: String,
    val episodes: List<TmdbEpisodeDto> = emptyList()
)

@Serializable
data class TmdbMovieDetails(
    val id: Long,
    val title: String,
    @SerialName("original_title") val originalTitle: String? = null,
    val tagline: String? = null,
    val overview: String = "",
    val status: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val voteCount: Int = 0,
    val runtime: Int? = null,
    val genres: List<TmdbGenre> = emptyList(),
    val credits: TmdbCredits? = null,
    val videos: TmdbVideosResponse? = null,
    val similar: TmdbPagedResponse<TmdbMovieDto>? = null,
    @SerialName("watch/providers") val watchProviders: TmdbWatchProvidersResponse? = null
)

@Serializable
data class TmdbTvDetails(
    val id: Long,
    val name: String,
    @SerialName("original_name") val originalName: String? = null,
    val tagline: String? = null,
    val overview: String = "",
    val status: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val voteCount: Int = 0,
    @SerialName("number_of_seasons") val numberOfSeasons: Int = 0,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int = 0,
    @SerialName("created_by") val createdBy: List<TmdbCreator> = emptyList(),
    val genres: List<TmdbGenre> = emptyList(),
    val seasons: List<TmdbSeasonDto> = emptyList(),
    val credits: TmdbCredits? = null,
    val videos: TmdbVideosResponse? = null,
    val similar: TmdbPagedResponse<TmdbTvDto>? = null,
    @SerialName("last_episode_to_air") val lastEpisodeToAir: TmdbEpisodeDto? = null,
    @SerialName("next_episode_to_air") val nextEpisodeToAir: TmdbEpisodeDto? = null,
    @SerialName("watch/providers") val watchProviders: TmdbWatchProvidersResponse? = null
)

/**
 * Unified UI Domain Model for rails and grids
 */
data class TmdbMediaItem(
    val id: Long,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseYear: String?,
    val voteAverage: Double,
    val mediaType: TmdbMediaType,
    val genreIds: List<Int> = emptyList()
) {
    val fullPosterUrl: String?
        get() = posterPath?.let { "https://image.tmdb.org/t/p/w342$it" }

    val fullBackdropUrl: String?
        get() = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }

    val formattedRating: String
        get() = if (voteAverage > 0) String.format(java.util.Locale.US, "★ %.1f", voteAverage) else "New"
}

enum class TmdbMediaType(val value: String) {
    MOVIE("movie"),
    TV("tv")
}

fun TmdbMovieDto.toMediaItem(): TmdbMediaItem {
    val year = releaseDate?.take(4)
    return TmdbMediaItem(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseYear = year,
        voteAverage = voteAverage,
        mediaType = TmdbMediaType.MOVIE,
        genreIds = genreIds
    )
}

fun TmdbTvDto.toMediaItem(): TmdbMediaItem {
    val year = firstAirDate?.take(4)
    return TmdbMediaItem(
        id = id,
        title = name,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseYear = year,
        voteAverage = voteAverage,
        mediaType = TmdbMediaType.TV,
        genreIds = genreIds
    )
}
