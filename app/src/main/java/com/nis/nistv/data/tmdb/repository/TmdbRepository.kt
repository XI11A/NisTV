package com.nis.nistv.data.tmdb.repository

import com.nis.nistv.data.tmdb.model.*
import com.nis.nistv.data.tmdb.remote.TmdbApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

interface TmdbRepository {
    suspend fun getTrending(type: TmdbMediaType, timeWindow: String = "week", forceRefresh: Boolean = false): List<TmdbMediaItem>
    suspend fun getPopular(type: TmdbMediaType, page: Int = 1, forceRefresh: Boolean = false): List<TmdbMediaItem>
    suspend fun getNowPlayingOrAiring(type: TmdbMediaType, page: Int = 1, forceRefresh: Boolean = false): List<TmdbMediaItem>
    suspend fun getTopRated(type: TmdbMediaType, page: Int = 1, forceRefresh: Boolean = false): List<TmdbMediaItem>
    suspend fun getUpcomingOrOnTheAir(type: TmdbMediaType, page: Int = 1, forceRefresh: Boolean = false): List<TmdbMediaItem>
    suspend fun getGenres(type: TmdbMediaType): List<TmdbGenre>
    suspend fun discoverByGenre(type: TmdbMediaType, genreId: Int?, page: Int = 1): List<TmdbMediaItem>
    suspend fun discoverByLanguage(type: TmdbMediaType, languageCode: String, page: Int = 1, forceRefresh: Boolean = false): List<TmdbMediaItem>
    suspend fun getMovieDetails(movieId: Long, forceRefresh: Boolean = false): TmdbMovieDetails
    suspend fun getTvDetails(tvId: Long, forceRefresh: Boolean = false): TmdbTvDetails
    suspend fun getTvSeason(tvId: Long, seasonNumber: Int): TmdbSeasonDetails
    suspend fun getRecommendations(type: TmdbMediaType, mediaId: Long, page: Int = 1): List<TmdbMediaItem>
    suspend fun getSimilar(type: TmdbMediaType, mediaId: Long, page: Int = 1): List<TmdbMediaItem>
    suspend fun search(type: TmdbMediaType, query: String, page: Int = 1): List<TmdbMediaItem>
    fun clearCache()
}

class DefaultTmdbRepository(
    private val api: TmdbApi
) : TmdbRepository {

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long
    )

    private val cache = ConcurrentHashMap<String, CacheEntry<Any>>()
    private val CACHE_TTL_MS = 6 * 60 * 60 * 1000L // 6 hours TTL per spec

    private inline fun <reified T : Any> getCachedOrFetch(
        key: String,
        forceRefresh: Boolean,
        fetcher: () -> T
    ): T {
        val now = System.currentTimeMillis()
        val entry = cache[key]

        if (!forceRefresh && entry != null && (now - entry.timestamp < CACHE_TTL_MS)) {
            @Suppress("UNCHECKED_CAST")
            return entry.data as T
        }

        return try {
            val fresh = fetcher()
            cache[key] = CacheEntry(fresh as Any, now)
            fresh
        } catch (e: Exception) {
            // Offline fallback: if fetch fails but we have stale cache, return it
            if (entry != null) {
                @Suppress("UNCHECKED_CAST")
                entry.data as T
            } else {
                throw e
            }
        }
    }

    override suspend fun getTrending(
        type: TmdbMediaType,
        timeWindow: String,
        forceRefresh: Boolean
    ): List<TmdbMediaItem> = withContext(Dispatchers.IO) {
        val cacheKey = "trending_${type.value}_$timeWindow"
        getCachedOrFetch(cacheKey, forceRefresh) {
            when (type) {
                TmdbMediaType.MOVIE -> api.getTrendingMovies(timeWindow).results.map { it.toMediaItem() }
                TmdbMediaType.TV -> api.getTrendingTv(timeWindow).results.map { it.toMediaItem() }
            }
        }
    }

    override suspend fun getPopular(
        type: TmdbMediaType,
        page: Int,
        forceRefresh: Boolean
    ): List<TmdbMediaItem> = withContext(Dispatchers.IO) {
        val cacheKey = "popular_${type.value}_$page"
        getCachedOrFetch(cacheKey, forceRefresh) {
            when (type) {
                TmdbMediaType.MOVIE -> api.getPopularMovies(page).results.map { it.toMediaItem() }
                TmdbMediaType.TV -> api.getPopularTv(page).results.map { it.toMediaItem() }
            }
        }
    }

    override suspend fun getNowPlayingOrAiring(
        type: TmdbMediaType,
        page: Int,
        forceRefresh: Boolean
    ): List<TmdbMediaItem> = withContext(Dispatchers.IO) {
        val cacheKey = "now_playing_${type.value}_$page"
        getCachedOrFetch(cacheKey, forceRefresh) {
            when (type) {
                TmdbMediaType.MOVIE -> api.getNowPlayingMovies(page).results.map { it.toMediaItem() }
                TmdbMediaType.TV -> api.getAiringTodayTv(page).results.map { it.toMediaItem() }
            }
        }
    }

    override suspend fun getTopRated(
        type: TmdbMediaType,
        page: Int,
        forceRefresh: Boolean
    ): List<TmdbMediaItem> = withContext(Dispatchers.IO) {
        val cacheKey = "top_rated_${type.value}_$page"
        getCachedOrFetch(cacheKey, forceRefresh) {
            when (type) {
                TmdbMediaType.MOVIE -> api.getTopRatedMovies(page).results.map { it.toMediaItem() }
                TmdbMediaType.TV -> api.getTopRatedTv(page).results.map { it.toMediaItem() }
            }
        }
    }

    override suspend fun getUpcomingOrOnTheAir(
        type: TmdbMediaType,
        page: Int,
        forceRefresh: Boolean
    ): List<TmdbMediaItem> = withContext(Dispatchers.IO) {
        val cacheKey = "upcoming_${type.value}_$page"
        getCachedOrFetch(cacheKey, forceRefresh) {
            when (type) {
                TmdbMediaType.MOVIE -> api.getUpcomingMovies(page).results.map { it.toMediaItem() }
                TmdbMediaType.TV -> api.getOnTheAirTv(page).results.map { it.toMediaItem() }
            }
        }
    }

    override suspend fun getGenres(type: TmdbMediaType): List<TmdbGenre> = withContext(Dispatchers.IO) {
        val cacheKey = "genres_${type.value}"
        getCachedOrFetch(cacheKey, false) {
            when (type) {
                TmdbMediaType.MOVIE -> api.getMovieGenres()
                TmdbMediaType.TV -> api.getTvGenres()
            }
        }
    }

    override suspend fun discoverByGenre(
        type: TmdbMediaType,
        genreId: Int?,
        page: Int
    ): List<TmdbMediaItem> = withContext(Dispatchers.IO) {
        val cacheKey = "discover_${type.value}_${genreId ?: 0}_$page"
        getCachedOrFetch(cacheKey, false) {
            when (type) {
                TmdbMediaType.MOVIE -> api.discoverMovies(genreId, page).results.map { it.toMediaItem() }
                TmdbMediaType.TV -> api.discoverTv(genreId, page).results.map { it.toMediaItem() }
            }
        }
    }

    override suspend fun getMovieDetails(
        movieId: Long,
        forceRefresh: Boolean
    ): TmdbMovieDetails = withContext(Dispatchers.IO) {
        val cacheKey = "movie_details_$movieId"
        getCachedOrFetch(cacheKey, forceRefresh) {
            api.getMovieDetails(movieId)
        }
    }

    override suspend fun getTvDetails(
        tvId: Long,
        forceRefresh: Boolean
    ): TmdbTvDetails = withContext(Dispatchers.IO) {
        val cacheKey = "tv_details_$tvId"
        getCachedOrFetch(cacheKey, forceRefresh) {
            api.getTvDetails(tvId)
        }
    }

    override suspend fun getTvSeason(tvId: Long, seasonNumber: Int): TmdbSeasonDetails = withContext(Dispatchers.IO) {
        val cacheKey = "tv_season_${tvId}_$seasonNumber"
        getCachedOrFetch(cacheKey, false) {
            api.getTvSeasonDetails(tvId, seasonNumber)
        }
    }

    override suspend fun getRecommendations(
        type: TmdbMediaType,
        mediaId: Long,
        page: Int
    ): List<TmdbMediaItem> = withContext(Dispatchers.IO) {
        val cacheKey = "rec_${type.value}_${mediaId}_$page"
        getCachedOrFetch(cacheKey, false) {
            when (type) {
                TmdbMediaType.MOVIE -> api.getMovieRecommendations(mediaId, page).results.map { it.toMediaItem() }
                TmdbMediaType.TV -> api.getTvRecommendations(mediaId, page).results.map { it.toMediaItem() }
            }
        }
    }

    override suspend fun getSimilar(
        type: TmdbMediaType,
        mediaId: Long,
        page: Int
    ): List<TmdbMediaItem> = withContext(Dispatchers.IO) {
        val cacheKey = "similar_${type.value}_${mediaId}_$page"
        getCachedOrFetch(cacheKey, false) {
            when (type) {
                TmdbMediaType.MOVIE -> api.getMovieSimilar(mediaId, page).results.map { it.toMediaItem() }
                TmdbMediaType.TV -> api.getTvSimilar(mediaId, page).results.map { it.toMediaItem() }
            }
        }
    }

    override suspend fun discoverByLanguage(
        type: TmdbMediaType,
        languageCode: String,
        page: Int,
        forceRefresh: Boolean
    ): List<TmdbMediaItem> = withContext(Dispatchers.IO) {
        val cacheKey = "lang_${type.value}_${languageCode}_$page"
        getCachedOrFetch(cacheKey, forceRefresh) {
            when (type) {
                TmdbMediaType.MOVIE -> api.discoverMoviesByLanguage(languageCode, page).results.map { it.toMediaItem() }
                TmdbMediaType.TV -> api.discoverTvByLanguage(languageCode, page).results.map { it.toMediaItem() }
            }
        }
    }

    override suspend fun search(
        type: TmdbMediaType,
        query: String,
        page: Int
    ): List<TmdbMediaItem> = withContext(Dispatchers.IO) {
        when (type) {
            TmdbMediaType.MOVIE -> api.searchMovies(query, page).results.map { it.toMediaItem() }
            TmdbMediaType.TV -> api.searchTv(query, page).results.map { it.toMediaItem() }
        }
    }

    override fun clearCache() {
        cache.clear()
    }
}
