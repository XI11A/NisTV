package com.nis.nistv.data.recommendation

import android.util.Log
import com.nis.nistv.core.database.dao.MediaInteractionDao
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.MediaInteractionEntity
import com.nis.nistv.core.database.entity.ProfileEntity
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.core.security.KidsContentFilter
import com.nis.nistv.data.repository.ChannelRepository
import com.nis.nistv.data.repository.ProfileRepository
import com.nis.nistv.data.tmdb.model.LanguageUtils
import com.nis.nistv.data.tmdb.model.TmdbMediaItem
import com.nis.nistv.data.tmdb.model.TmdbMediaType
import com.nis.nistv.data.tmdb.model.TmdbMovieDetails
import com.nis.nistv.data.tmdb.model.TmdbTvDetails
import com.nis.nistv.data.tmdb.repository.TmdbRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.math.exp

@OptIn(ExperimentalCoroutinesApi::class)
class IntelligentRecommendationEngine(
    private val mediaInteractionDao: MediaInteractionDao,
    private val channelRepository: ChannelRepository,
    private val tmdbRepository: TmdbRepository,
    private val userPreferences: UserPreferences,
    private val profileRepository: ProfileRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    companion object {
        private const val TAG = "RecommendationEngine"
        // 7-day half life in milliseconds for time-decay weighting
        private const val HALF_LIFE_MS = 7.0 * 24.0 * 60.0 * 60.0 * 1000.0
    }

    private val _recommendationState = MutableStateFlow(DynamicRecommendationState(isLoading = true))
    val recommendationState: StateFlow<DynamicRecommendationState> = _recommendationState.asStateFlow()

    init {
        scope.launch {
            val activeFlow = profileRepository?.activeProfile ?: flowOf(
                ProfileEntity(id = "default", name = "Me", preferredLanguage = "Hindi")
            )

            activeFlow.flatMapLatest { profile ->
                combine(
                    mediaInteractionDao.getRecentInteractions(profile.id, 60),
                    channelRepository.getAllChannels()
                ) { interactions, allChannels ->
                    val effectiveLang = profile.preferredLanguage.ifBlank { "Hindi" }
                    RecommendationContext(profile, interactions, effectiveLang, allChannels)
                }
            }.distinctUntilChanged().collect { ctx ->
                refreshRecommendations(ctx.profile, ctx.interactions, ctx.preferredLang, ctx.allChannels)
            }
        }
    }

    private data class RecommendationContext(
        val profile: ProfileEntity,
        val interactions: List<MediaInteractionEntity>,
        val preferredLang: String,
        val allChannels: List<ChannelEntity>
    )

    private fun currentProfileId(): String =
        profileRepository?.activeProfileId?.value ?: "default"

    private suspend fun isHistoryPaused(): Boolean {
        return profileRepository?.activeProfile?.value?.historyPaused
            ?: (userPreferences.isWatchHistoryPaused.firstOrNull() ?: false)
    }

    // --- Interaction Recording Touchpoints ---

    fun recordChannelWatch(channel: ChannelEntity, durationMs: Long = 0L) {
        scope.launch {
            if (isHistoryPaused()) return@launch
            try {
                mediaInteractionDao.recordInteraction(
                    MediaInteractionEntity(
                        profileId = currentProfileId(),
                        mediaType = "CHANNEL",
                        mediaId = channel.id,
                        title = channel.cleanName.ifEmpty { channel.name },
                        categoryOrGenres = channel.category,
                        language = channel.language,
                        posterOrLogoUrl = channel.logoUrl,
                        interactionType = "WATCH",
                        watchDurationMs = durationMs,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to record channel watch: ${e.message}")
            }
        }
    }

    fun recordChannelFavorite(channel: ChannelEntity, isFavorite: Boolean) {
        if (!isFavorite) return
        scope.launch {
            if (isHistoryPaused()) return@launch
            try {
                mediaInteractionDao.recordInteraction(
                    MediaInteractionEntity(
                        profileId = currentProfileId(),
                        mediaType = "CHANNEL",
                        mediaId = channel.id,
                        title = channel.cleanName.ifEmpty { channel.name },
                        categoryOrGenres = channel.category,
                        language = channel.language,
                        posterOrLogoUrl = channel.logoUrl,
                        interactionType = "FAVORITE",
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to record channel favorite: ${e.message}")
            }
        }
    }

    fun recordMediaInteraction(
        media: TmdbMediaItem,
        interactionType: String = "WATCH",
        durationMs: Long = 0L
    ) {
        scope.launch {
            if (isHistoryPaused()) return@launch
            try {
                val genresStr = media.genreIds.joinToString(",")
                mediaInteractionDao.recordInteraction(
                    MediaInteractionEntity(
                        profileId = currentProfileId(),
                        mediaType = media.mediaType.value.uppercase(),
                        mediaId = media.id.toString(),
                        title = media.title,
                        categoryOrGenres = genresStr,
                        language = "",
                        posterOrLogoUrl = media.fullPosterUrl,
                        interactionType = interactionType,
                        watchDurationMs = durationMs,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to record media interaction: ${e.message}")
            }
        }
    }

    fun recordMovieDetailsInteraction(details: TmdbMovieDetails, interactionType: String = "VIEW") {
        scope.launch {
            if (isHistoryPaused()) return@launch
            try {
                val genresStr = details.genres.map { it.id }.joinToString(",")
                mediaInteractionDao.recordInteraction(
                    MediaInteractionEntity(
                        profileId = currentProfileId(),
                        mediaType = "MOVIE",
                        mediaId = details.id.toString(),
                        title = details.title,
                        categoryOrGenres = genresStr,
                        language = "",
                        posterOrLogoUrl = details.posterPath?.let { "https://image.tmdb.org/t/p/w342$it" },
                        interactionType = interactionType,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to record movie details interaction: ${e.message}")
            }
        }
    }

    fun recordTvDetailsInteraction(details: TmdbTvDetails, interactionType: String = "VIEW") {
        scope.launch {
            if (isHistoryPaused()) return@launch
            try {
                val genresStr = details.genres.map { it.id }.joinToString(",")
                mediaInteractionDao.recordInteraction(
                    MediaInteractionEntity(
                        profileId = currentProfileId(),
                        mediaType = "TV",
                        mediaId = details.id.toString(),
                        title = details.name,
                        categoryOrGenres = genresStr,
                        language = "",
                        posterOrLogoUrl = details.posterPath?.let { "https://image.tmdb.org/t/p/w342$it" },
                        interactionType = interactionType,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to record tv details interaction: ${e.message}")
            }
        }
    }

    fun resetRecommendations() {
        scope.launch {
            try {
                val profileId = currentProfileId()
                mediaInteractionDao.clearForProfile(profileId)
                _recommendationState.value = DynamicRecommendationState(isLoading = false)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to reset recommendations: ${e.message}")
            }
        }
    }

    // --- On-Device Intelligence & Profiling Engine ---

    private suspend fun refreshRecommendations(
        profile: ProfileEntity,
        interactions: List<MediaInteractionEntity>,
        preferredLang: String,
        allChannels: List<ChannelEntity>
    ) {
        val now = System.currentTimeMillis()
        val isKids = profile.isKids
        val tasteProfile = buildTasteProfile(interactions, preferredLang, now)

        try {
            if (tasteProfile.isColdStart) {
                // Cold-Start
                val langCode = LanguageUtils.toIso6391(preferredLang)
                val langMovies = try {
                    tmdbRepository.discoverByLanguage(TmdbMediaType.MOVIE, langCode, 1).take(10)
                } catch (_: Exception) { emptyList() }
                val langTv = try {
                    tmdbRepository.discoverByLanguage(TmdbMediaType.TV, langCode, 1).take(10)
                } catch (_: Exception) { emptyList() }
                val trending = try {
                    tmdbRepository.getTrending(TmdbMediaType.MOVIE, "week").take(15)
                } catch (_: Exception) { emptyList() }

                var recommendedVod = (langMovies + langTv).ifEmpty { trending }
                if (isKids) {
                    recommendedVod = recommendedVod.filter { KidsContentFilter.isMediaAllowedForKids(it) }
                }

                var recommendedChannels = allChannels.filter {
                    it.language.equals(preferredLang, ignoreCase = true)
                }.take(15).ifEmpty { allChannels.take(15) }
                if (isKids) {
                    recommendedChannels = recommendedChannels.filter { KidsContentFilter.isChannelAllowedForKids(it) }
                }

                _recommendationState.value = DynamicRecommendationState(
                    recommendedForYou = recommendedVod,
                    becauseYouWatchedTitle = null,
                    becauseYouWatchedMedia = emptyList(),
                    recommendedChannels = recommendedChannels,
                    tasteProfile = tasteProfile,
                    recommendationReason = "Trending in $preferredLang",
                    isLoading = false
                )
            } else {
                // Dynamic Adaptive Pipeline
                val mostRecent = tasteProfile.mostRecentWatch

                var becauseTitle: String? = null
                var becauseMedia: List<TmdbMediaItem> = emptyList()

                if (mostRecent != null) {
                    becauseTitle = mostRecent.title
                    val rawBecause = fetchBecauseYouWatchedMedia(mostRecent, tasteProfile)
                    becauseMedia = if (isKids) {
                        rawBecause.filter { KidsContentFilter.isMediaAllowedForKids(it) }
                    } else rawBecause
                }

                val candidates = mutableListOf<TmdbMediaItem>()

                tasteProfile.topGenreId?.let { genreId ->
                    try {
                        val movieByGenre = tmdbRepository.discoverByGenre(TmdbMediaType.MOVIE, genreId, 1)
                        val tvByGenre = tmdbRepository.discoverByGenre(TmdbMediaType.TV, genreId, 1)
                        candidates.addAll(movieByGenre.take(10))
                        candidates.addAll(tvByGenre.take(10))
                    } catch (_: Exception) {}
                }

                val topLangCode = LanguageUtils.toIso6391(tasteProfile.topLanguage ?: preferredLang)
                try {
                    val langRecs = tmdbRepository.discoverByLanguage(TmdbMediaType.MOVIE, topLangCode, 1)
                    candidates.addAll(langRecs.take(10))
                } catch (_: Exception) {}

                try {
                    val trending = tmdbRepository.getTrending(TmdbMediaType.MOVIE, "week")
                    candidates.addAll(trending.take(10))
                } catch (_: Exception) {}

                var scoredMedia = candidates
                    .distinctBy { "${it.mediaType}_${it.id}" }
                    .filter { !tasteProfile.recentWatchedMediaIds.contains(it.id.toString()) }
                    .map { item ->
                        val score = computeMediaScore(item, tasteProfile)
                        Pair(item, score)
                    }
                    .sortedByDescending { it.second }
                    .map { it.first }

                if (isKids) {
                    scoredMedia = scoredMedia.filter { KidsContentFilter.isMediaAllowedForKids(it) }
                }
                scoredMedia = scoredMedia.take(20)

                var scoredChannels = allChannels
                    .map { channel ->
                        val score = computeChannelScore(channel, tasteProfile)
                        Pair(channel, score)
                    }
                    .sortedByDescending { it.second }
                    .map { it.first }

                if (isKids) {
                    scoredChannels = scoredChannels.filter { KidsContentFilter.isChannelAllowedForKids(it) }
                }
                scoredChannels = scoredChannels.take(15)

                val reason = when {
                    mostRecent != null -> "Updated from watching ${mostRecent.title}"
                    tasteProfile.topCategory != null -> "Based on your love for ${tasteProfile.topCategory}"
                    else -> "Personalized for your taste"
                }

                _recommendationState.value = DynamicRecommendationState(
                    recommendedForYou = scoredMedia.ifEmpty { becauseMedia },
                    becauseYouWatchedTitle = becauseTitle,
                    becauseYouWatchedMedia = becauseMedia,
                    recommendedChannels = scoredChannels,
                    tasteProfile = tasteProfile,
                    recommendationReason = reason,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating dynamic recommendations: ${e.message}", e)
            _recommendationState.value = _recommendationState.value.copy(isLoading = false)
        }
    }

    private fun buildTasteProfile(
        interactions: List<MediaInteractionEntity>,
        preferredLang: String,
        now: Long
    ): UserTasteProfile {
        if (interactions.isEmpty()) {
            return UserTasteProfile(
                languageWeights = mapOf(preferredLang.lowercase().trim() to 1.0),
                totalInteractions = 0
            )
        }

        val genreScores = mutableMapOf<Int, Double>()
        val categoryScores = mutableMapOf<String, Double>()
        val languageScores = mutableMapOf<String, Double>()
        val watchedIds = mutableSetOf<String>()

        interactions.forEach { interaction ->
            watchedIds.add(interaction.mediaId)

            val elapsedMs = (now - interaction.timestamp).coerceAtLeast(0L)
            val decay = exp(-elapsedMs / HALF_LIFE_MS)

            val typeMultiplier = when (interaction.interactionType) {
                "COMPLETE" -> 1.5
                "FAVORITE" -> 1.4
                "DOWNLOAD" -> 1.3
                "WATCH" -> 1.0
                "VIEW" -> 0.4
                else -> 0.5
            }

            val durationBonus = if (interaction.watchDurationMs > 60_000L) {
                ((interaction.watchDurationMs / 60_000L).coerceAtMost(10) * 0.1)
            } else 0.0

            val weight = decay * (typeMultiplier + durationBonus)

            if (interaction.mediaType == "CHANNEL") {
                if (interaction.categoryOrGenres.isNotBlank()) {
                    val catKey = interaction.categoryOrGenres.lowercase().trim()
                    categoryScores[catKey] = (categoryScores[catKey] ?: 0.0) + weight

                    when (catKey) {
                        "movies", "cinema" -> {
                            genreScores[28] = (genreScores[28] ?: 0.0) + 0.5 * weight
                            genreScores[18] = (genreScores[18] ?: 0.0) + 0.5 * weight
                            genreScores[53] = (genreScores[53] ?: 0.0) + 0.4 * weight
                        }
                        "animation", "kids" -> {
                            genreScores[16] = (genreScores[16] ?: 0.0) + 0.8 * weight
                            genreScores[10751] = (genreScores[10751] ?: 0.0) + 0.7 * weight
                        }
                        "music" -> {
                            genreScores[10402] = (genreScores[10402] ?: 0.0) + 0.8 * weight
                        }
                        "documentary" -> {
                            genreScores[99] = (genreScores[99] ?: 0.0) + 0.8 * weight
                        }
                        "comedy" -> {
                            genreScores[35] = (genreScores[35] ?: 0.0) + 0.7 * weight
                        }
                    }
                }
                if (interaction.language.isNotBlank()) {
                    val langKey = interaction.language.lowercase().trim()
                    languageScores[langKey] = (languageScores[langKey] ?: 0.0) + weight
                }
            } else {
                if (interaction.categoryOrGenres.isNotBlank()) {
                    interaction.categoryOrGenres.split(",").forEach { idStr ->
                        idStr.trim().toIntOrNull()?.let { genreId ->
                            genreScores[genreId] = (genreScores[genreId] ?: 0.0) + weight

                            when (genreId) {
                                16, 10751 -> {
                                    categoryScores["kids"] = (categoryScores["kids"] ?: 0.0) + 0.6 * weight
                                    categoryScores["animation"] = (categoryScores["animation"] ?: 0.0) + 0.6 * weight
                                }
                                99 -> {
                                    categoryScores["documentary"] = (categoryScores["documentary"] ?: 0.0) + 0.6 * weight
                                }
                                10402 -> {
                                    categoryScores["music"] = (categoryScores["music"] ?: 0.0) + 0.6 * weight
                                }
                                28, 18, 53, 878 -> {
                                    categoryScores["movies"] = (categoryScores["movies"] ?: 0.0) + 0.6 * weight
                                }
                            }
                        }
                    }
                }
                if (interaction.language.isNotBlank()) {
                    val langKey = interaction.language.lowercase().trim()
                    languageScores[langKey] = (languageScores[langKey] ?: 0.0) + weight
                }
            }
        }

        val totalGenre = genreScores.values.sum().coerceAtLeast(1e-6)
        val normGenre = genreScores.mapValues { it.value / totalGenre }

        val totalCategory = categoryScores.values.sum().coerceAtLeast(1e-6)
        val normCategory = categoryScores.mapValues { it.value / totalCategory }

        val totalLanguage = languageScores.values.sum().coerceAtLeast(1e-6)
        val normLanguage = languageScores.mapValues { it.value / totalLanguage }

        val mostRecentWatch = interactions.firstOrNull { it.interactionType == "WATCH" } ?: interactions.firstOrNull()

        return UserTasteProfile(
            genreWeights = normGenre,
            categoryWeights = normCategory,
            languageWeights = normLanguage,
            recentWatchedMediaIds = watchedIds,
            recentInteractions = interactions,
            mostRecentWatch = mostRecentWatch,
            totalInteractions = interactions.size
        )
    }

    private suspend fun fetchBecauseYouWatchedMedia(
        seed: MediaInteractionEntity,
        profile: UserTasteProfile
    ): List<TmdbMediaItem> {
        return try {
            val isMovie = seed.mediaType.equals("MOVIE", ignoreCase = true)
            val mediaType = if (isMovie) TmdbMediaType.MOVIE else TmdbMediaType.TV
            val numericId = seed.mediaId.replace("movie_", "").replace("tv_", "").toLongOrNull() ?: return emptyList()

            val recs = tmdbRepository.getRecommendations(mediaType, numericId, 1)
            recs.filter { !profile.recentWatchedMediaIds.contains(it.id.toString()) }.take(10)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun computeMediaScore(item: TmdbMediaItem, profile: UserTasteProfile): Double {
        var score = 0.0

        if (item.genreIds.isNotEmpty()) {
            val genreMatchSum = item.genreIds.sumOf { profile.getGenreAffinity(it) }
            val avgMatch = (genreMatchSum / item.genreIds.size.toDouble()).coerceIn(0.0, 1.0)
            score += 0.45 * avgMatch
        }

        val ratingNorm = (item.voteAverage / 10.0).coerceIn(0.0, 1.0)
        score += 0.30 * ratingNorm

        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val releaseYear = item.releaseYear?.toIntOrNull() ?: (currentYear - 5)
        val yearDiff = (currentYear - releaseYear).coerceAtLeast(0)
        val recencyNorm = exp(-yearDiff / 10.0)
        score += 0.25 * recencyNorm

        return score
    }

    private fun computeChannelScore(channel: ChannelEntity, profile: UserTasteProfile): Double {
        var score = 0.0

        score += 0.55 * profile.getCategoryAffinity(channel.category)
        score += 0.45 * profile.getLanguageAffinity(channel.language)

        if (profile.recentWatchedMediaIds.contains(channel.id)) {
            score *= 0.6
        }

        return score
    }
}
