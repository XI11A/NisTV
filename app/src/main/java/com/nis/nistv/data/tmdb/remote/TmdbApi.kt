package com.nis.nistv.data.tmdb.remote

import android.util.Log
import com.nis.nistv.data.tmdb.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class TmdbApi(
    private val okHttpClient: OkHttpClient,
    private val apiKeyProvider: () -> String,
    private val regionProvider: () -> String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    companion object {
        private const val BASE_URL = "https://api.themoviedb.org/3"
        private const val TAG = "TmdbApi"
        private const val MAX_RETRIES = 3
    }

    private suspend fun executeRequestWithRetry(urlBuilderAction: (okhttp3.HttpUrl.Builder) -> Unit): String = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        val region = regionProvider().trim().ifBlank { "IN" }

        val httpUrlBuilder = BASE_URL.toHttpUrlOrNull()?.newBuilder()
            ?: throw IllegalArgumentException("Invalid Base URL")

        if (apiKey.isNotBlank()) {
            httpUrlBuilder.addQueryParameter("api_key", apiKey)
        }
        httpUrlBuilder.addQueryParameter("language", "en-US")
        httpUrlBuilder.addQueryParameter("region", region)
        httpUrlBuilder.addQueryParameter("include_adult", "false")

        urlBuilderAction(httpUrlBuilder)
        val finalUrl = httpUrlBuilder.build()

        var attempts = 0
        var backoffMs = 500L

        while (true) {
            attempts++
            val request = Request.Builder()
                .url(finalUrl)
                .addHeader("Accept", "application/json")
                .build()

            try {
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        return@withContext response.body?.string() ?: ""
                    }

                    if (response.code == 429 && attempts < MAX_RETRIES) {
                        val retryAfterSec = response.header("Retry-After")?.toLongOrNull() ?: (backoffMs / 1000L)
                        val waitTime = (retryAfterSec * 1000L).coerceAtLeast(backoffMs)
                        Log.w(TAG, "TMDB Rate Limit (429). Retrying in ${waitTime}ms... (attempt $attempts/$MAX_RETRIES)")
                        delay(waitTime)
                        backoffMs *= 2
                        return@use
                    }

                    if (response.code == 401) {
                        throw IOException("Invalid or missing TMDB API Key. Please configure your key in Settings.")
                    }

                    throw IOException("TMDB Request failed with HTTP ${response.code}: ${response.message}")
                }
            } catch (e: IOException) {
                if (attempts >= MAX_RETRIES) {
                    throw e
                }
                Log.w(TAG, "TMDB Request error: ${e.message}. Retrying in ${backoffMs}ms...")
                delay(backoffMs)
                backoffMs *= 2
            }
        }
        @Suppress("UNREACHABLE_CODE")
        ""
    }

    // --- Trending ---
    suspend fun getTrendingMovies(timeWindow: String = "week"): TmdbPagedResponse<TmdbMovieDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("trending")
            builder.addPathSegment("movie")
            builder.addPathSegment(timeWindow)
        }
        return json.decodeFromString(jsonStr)
    }

    suspend fun getTrendingTv(timeWindow: String = "week"): TmdbPagedResponse<TmdbTvDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("trending")
            builder.addPathSegment("tv")
            builder.addPathSegment(timeWindow)
        }
        return json.decodeFromString(jsonStr)
    }

    // --- Movies ---
    suspend fun getPopularMovies(page: Int = 1): TmdbPagedResponse<TmdbMovieDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("movie")
            builder.addPathSegment("popular")
            builder.addQueryParameter("page", page.toString())
        }
        return json.decodeFromString(jsonStr)
    }

    suspend fun getNowPlayingMovies(page: Int = 1): TmdbPagedResponse<TmdbMovieDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("movie")
            builder.addPathSegment("now_playing")
            builder.addQueryParameter("page", page.toString())
        }
        return json.decodeFromString(jsonStr)
    }

    suspend fun getTopRatedMovies(page: Int = 1): TmdbPagedResponse<TmdbMovieDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("movie")
            builder.addPathSegment("top_rated")
            builder.addQueryParameter("page", page.toString())
        }
        return json.decodeFromString(jsonStr)
    }

    suspend fun getUpcomingMovies(page: Int = 1): TmdbPagedResponse<TmdbMovieDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("movie")
            builder.addPathSegment("upcoming")
            builder.addQueryParameter("page", page.toString())
        }
        return json.decodeFromString(jsonStr)
    }

    // --- TV Series ---
    suspend fun getPopularTv(page: Int = 1): TmdbPagedResponse<TmdbTvDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("tv")
            builder.addPathSegment("popular")
            builder.addQueryParameter("page", page.toString())
        }
        return json.decodeFromString(jsonStr)
    }

    suspend fun getAiringTodayTv(page: Int = 1): TmdbPagedResponse<TmdbTvDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("tv")
            builder.addPathSegment("airing_today")
            builder.addQueryParameter("page", page.toString())
        }
        return json.decodeFromString(jsonStr)
    }

    suspend fun getOnTheAirTv(page: Int = 1): TmdbPagedResponse<TmdbTvDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("tv")
            builder.addPathSegment("on_the_air")
            builder.addQueryParameter("page", page.toString())
        }
        return json.decodeFromString(jsonStr)
    }

    suspend fun getTopRatedTv(page: Int = 1): TmdbPagedResponse<TmdbTvDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("tv")
            builder.addPathSegment("top_rated")
            builder.addQueryParameter("page", page.toString())
        }
        return json.decodeFromString(jsonStr)
    }

    // --- Genres ---
    suspend fun getMovieGenres(): List<TmdbGenre> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("genre")
            builder.addPathSegment("movie")
            builder.addPathSegment("list")
        }
        return json.decodeFromString<TmdbGenreResponse>(jsonStr).genres
    }

    suspend fun getTvGenres(): List<TmdbGenre> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("genre")
            builder.addPathSegment("tv")
            builder.addPathSegment("list")
        }
        return json.decodeFromString<TmdbGenreResponse>(jsonStr).genres
    }

    // --- Discover with Genre Filters ---
    suspend fun discoverMovies(genreId: Int?, page: Int = 1): TmdbPagedResponse<TmdbMovieDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("discover")
            builder.addPathSegment("movie")
            builder.addQueryParameter("page", page.toString())
            builder.addQueryParameter("sort_by", "popularity.desc")
            if (genreId != null && genreId > 0) {
                builder.addQueryParameter("with_genres", genreId.toString())
            }
        }
        return json.decodeFromString(jsonStr)
    }

    suspend fun discoverTv(genreId: Int?, page: Int = 1): TmdbPagedResponse<TmdbTvDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("discover")
            builder.addPathSegment("tv")
            builder.addQueryParameter("page", page.toString())
            builder.addQueryParameter("sort_by", "popularity.desc")
            if (genreId != null && genreId > 0) {
                builder.addQueryParameter("with_genres", genreId.toString())
            }
        }
        return json.decodeFromString(jsonStr)
    }

    // --- Discover by Language ---
    suspend fun discoverMoviesByLanguage(languageCode: String, page: Int = 1): TmdbPagedResponse<TmdbMovieDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("discover")
            builder.addPathSegment("movie")
            builder.addQueryParameter("with_original_language", languageCode)
            builder.addQueryParameter("sort_by", "popularity.desc")
            builder.addQueryParameter("page", page.toString())
        }
        return json.decodeFromString(jsonStr)
    }

    suspend fun discoverTvByLanguage(languageCode: String, page: Int = 1): TmdbPagedResponse<TmdbTvDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("discover")
            builder.addPathSegment("tv")
            builder.addQueryParameter("with_original_language", languageCode)
            builder.addQueryParameter("sort_by", "popularity.desc")
            builder.addQueryParameter("page", page.toString())
        }
        return json.decodeFromString(jsonStr)
    }

    // --- Details ---
    suspend fun getMovieDetails(movieId: Long): TmdbMovieDetails {
        val region = regionProvider().trim().ifBlank { "IN" }
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("movie")
            builder.addPathSegment(movieId.toString())
            builder.addQueryParameter("append_to_response", "credits,videos,similar,watch/providers")
            builder.addQueryParameter("watch_region", region)
        }
        return json.decodeFromString(jsonStr)
    }

    suspend fun getTvDetails(tvId: Long): TmdbTvDetails {
        val region = regionProvider().trim().ifBlank { "IN" }
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("tv")
            builder.addPathSegment(tvId.toString())
            builder.addQueryParameter("append_to_response", "credits,videos,similar,watch/providers")
            builder.addQueryParameter("watch_region", region)
        }
        return json.decodeFromString(jsonStr)
    }

    suspend fun getTvSeasonDetails(tvId: Long, seasonNumber: Int): TmdbSeasonDetails {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("tv")
            builder.addPathSegment(tvId.toString())
            builder.addPathSegment("season")
            builder.addPathSegment(seasonNumber.toString())
        }
        return json.decodeFromString(jsonStr)
    }

    // --- Recommendations & Similar ---
    suspend fun getMovieRecommendations(movieId: Long, page: Int = 1): TmdbPagedResponse<TmdbMovieDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("movie")
            builder.addPathSegment(movieId.toString())
            builder.addPathSegment("recommendations")
            builder.addQueryParameter("page", page.toString())
        }
        return json.decodeFromString(jsonStr)
    }

    suspend fun getMovieSimilar(movieId: Long, page: Int = 1): TmdbPagedResponse<TmdbMovieDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("movie")
            builder.addPathSegment(movieId.toString())
            builder.addPathSegment("similar")
            builder.addQueryParameter("page", page.toString())
        }
        return json.decodeFromString(jsonStr)
    }

    suspend fun getTvRecommendations(tvId: Long, page: Int = 1): TmdbPagedResponse<TmdbTvDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("tv")
            builder.addPathSegment(tvId.toString())
            builder.addPathSegment("recommendations")
            builder.addQueryParameter("page", page.toString())
        }
        return json.decodeFromString(jsonStr)
    }

    suspend fun getTvSimilar(tvId: Long, page: Int = 1): TmdbPagedResponse<TmdbTvDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("tv")
            builder.addPathSegment(tvId.toString())
            builder.addPathSegment("similar")
            builder.addQueryParameter("page", page.toString())
        }
        return json.decodeFromString(jsonStr)
    }

    // --- Search ---
    suspend fun searchMovies(query: String, page: Int = 1): TmdbPagedResponse<TmdbMovieDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("search")
            builder.addPathSegment("movie")
            builder.addQueryParameter("query", query)
            builder.addQueryParameter("page", page.toString())
        }
        return json.decodeFromString(jsonStr)
    }

    suspend fun searchTv(query: String, page: Int = 1): TmdbPagedResponse<TmdbTvDto> {
        val jsonStr = executeRequestWithRetry { builder ->
            builder.addPathSegment("search")
            builder.addPathSegment("tv")
            builder.addQueryParameter("query", query)
            builder.addQueryParameter("page", page.toString())
        }
        return json.decodeFromString(jsonStr)
    }
}
