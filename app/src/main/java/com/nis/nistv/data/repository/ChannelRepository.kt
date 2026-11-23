package com.nis.nistv.data.repository

import android.content.Context
import com.nis.nistv.core.database.NisTvDatabase
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.FavoriteEntity
import com.nis.nistv.core.database.entity.RecentSearchEntity
import com.nis.nistv.core.database.entity.WatchHistoryEntity
import com.nis.nistv.core.database.model.ChannelWithStreams
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.core.security.KidsContentFilter
import com.nis.nistv.data.health.StreamHealthScorer
import com.nis.nistv.data.local.SeedImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

interface ChannelRepository {
    fun getAllChannels(): Flow<List<ChannelEntity>>
    fun getChannelsByCategory(category: String): Flow<List<ChannelEntity>>
    fun getChannelsByLanguage(language: String): Flow<List<ChannelEntity>>
    fun getChannelsByCountry(country: String): Flow<List<ChannelEntity>>
    fun getFeaturedChannels(): Flow<List<ChannelEntity>>
    fun getCategories(): Flow<List<String>>
    fun getLanguages(): Flow<List<String>>
    fun getCountries(): Flow<List<String>>
    fun searchChannels(query: String): Flow<List<ChannelEntity>>
    fun getChannelWithStreams(channelId: String): Flow<ChannelWithStreams?>
    suspend fun getChannelWithStreamsSync(channelId: String): ChannelWithStreams?
    suspend fun ensureSeeded(): Result<Int>

    // Favorites
    fun getFavorites(profileId: String? = null): Flow<List<ChannelEntity>>
    fun isFavorite(profileId: String? = null, channelId: String): Flow<Boolean>
    suspend fun toggleFavorite(profileId: String? = null, channelId: String)

    // History
    fun getRecentlyWatched(profileId: String? = null): Flow<List<ChannelEntity>>
    suspend fun recordWatch(profileId: String? = null, channelId: String, durationMs: Long = 0L)

    // Searches
    fun getRecentSearches(profileId: String? = null): Flow<List<String>>
    suspend fun addRecentSearch(profileId: String? = null, query: String)
    suspend fun clearRecentSearches(profileId: String? = null)

    // Stream Failures
    suspend fun recordStreamFailure(streamId: Long)
    suspend fun resetStreamFailure(streamId: Long)

    // Sync
    fun getSyncMetadata(key: String = "CATALOGUE_FULL"): Flow<com.nis.nistv.core.database.entity.SyncMetadataEntity?>
    fun triggerWorkManagerSync()

    // Playlists
    fun getChannelsByPlaylist(playlistId: String): Flow<List<ChannelEntity>>
    fun getAllCustomChannels(): Flow<List<ChannelEntity>>
}

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultChannelRepository(
    private val context: Context,
    private val database: NisTvDatabase,
    private val userPreferences: UserPreferences = UserPreferences(context),
    private val profileRepository: ProfileRepository? = null
) : ChannelRepository {

    private val seedImporter = SeedImporter(context, database)
    private val channelDao = database.channelDao()
    private val streamDao = database.streamSourceDao()
    private val favoriteDao = database.favoriteDao()
    private val watchHistoryDao = database.watchHistoryDao()
    private val recentSearchDao = database.recentSearchDao()
    private val syncMetadataDao = database.syncMetadataDao()

    private fun resolveProfileId(specifiedProfileId: String?): String {
        return specifiedProfileId ?: profileRepository?.activeProfileId?.value ?: "default"
    }

    private fun filterForKidsAndNsfw(channels: List<ChannelEntity>, nsfw: Boolean, isKids: Boolean): List<ChannelEntity> {
        var result = if (nsfw && !isKids) channels else channels.filter { !it.isNsfw }
        if (isKids) {
            result = result.filter { KidsContentFilter.isChannelAllowedForKids(it) }
        }
        return result
    }

    private val isKidsProfileFlow: Flow<Boolean> =
        profileRepository?.activeProfile?.let { activeFlow ->
            combine(activeFlow) { it[0].isKids }
        } ?: flowOf(false)

    override fun getAllChannels(): Flow<List<ChannelEntity>> =
        combine(channelDao.getAllChannels(), userPreferences.isNsfwEnabled, isKidsProfileFlow) { list, nsfw, isKids ->
            filterForKidsAndNsfw(list, nsfw, isKids)
        }.flowOn(Dispatchers.IO)

    override fun getChannelsByCategory(category: String): Flow<List<ChannelEntity>> =
        combine(channelDao.getChannelsByCategory(category), userPreferences.isNsfwEnabled, isKidsProfileFlow) { list, nsfw, isKids ->
            filterForKidsAndNsfw(list, nsfw, isKids)
        }.flowOn(Dispatchers.IO)

    override fun getChannelsByLanguage(language: String): Flow<List<ChannelEntity>> =
        combine(channelDao.getChannelsByLanguage(language), userPreferences.isNsfwEnabled, isKidsProfileFlow) { list, nsfw, isKids ->
            filterForKidsAndNsfw(list, nsfw, isKids)
        }.flowOn(Dispatchers.IO)

    override fun getChannelsByCountry(country: String): Flow<List<ChannelEntity>> =
        combine(channelDao.getChannelsByCountry(country), userPreferences.isNsfwEnabled, isKidsProfileFlow) { list, nsfw, isKids ->
            filterForKidsAndNsfw(list, nsfw, isKids)
        }.flowOn(Dispatchers.IO)

    override fun getFeaturedChannels(): Flow<List<ChannelEntity>> =
        combine(channelDao.getFeaturedChannels(12), userPreferences.isNsfwEnabled, isKidsProfileFlow) { list, nsfw, isKids ->
            filterForKidsAndNsfw(list, nsfw, isKids)
        }.flowOn(Dispatchers.IO)

    override fun getCategories(): Flow<List<String>> =
        combine(channelDao.getCategories(), isKidsProfileFlow) { categories, isKids ->
            if (isKids) {
                categories.filter { cat -> KidsContentFilter.ALLOWED_CHANNEL_CATEGORIES.any { it.equals(cat, ignoreCase = true) } }
            } else {
                categories
            }
        }.flowOn(Dispatchers.IO)

    override fun getLanguages(): Flow<List<String>> =
        channelDao.getLanguages().flowOn(Dispatchers.IO)

    override fun getCountries(): Flow<List<String>> =
        channelDao.getCountries().flowOn(Dispatchers.IO)

    override fun searchChannels(query: String): Flow<List<ChannelEntity>> =
        combine(channelDao.searchChannels(query), userPreferences.isNsfwEnabled, isKidsProfileFlow) { list, nsfw, isKids ->
            filterForKidsAndNsfw(list, nsfw, isKids)
        }.flowOn(Dispatchers.IO)

    override fun getChannelWithStreams(channelId: String): Flow<ChannelWithStreams?> =
        combine(
            channelDao.getChannelWithStreams(channelId),
            database.streamHealthDao().getHealthForChannel(channelId)
        ) { relation, healthList ->
            if (relation == null) return@combine null
            val healthMap = healthList.associateBy { it.streamKey }
            val sortedStreams = StreamHealthScorer.sortStreamSources(relation.streams, healthMap)
            relation.copy(streams = sortedStreams)
        }.flowOn(Dispatchers.IO)

    override suspend fun getChannelWithStreamsSync(channelId: String): ChannelWithStreams? =
        withContext(Dispatchers.IO) {
            val relation = channelDao.getChannelWithStreamsSync(channelId) ?: return@withContext null
            val healthList = database.streamHealthDao().getHealthForChannelSync(channelId)
            val healthMap = healthList.associateBy { it.streamKey }
            val sortedStreams = StreamHealthScorer.sortStreamSources(relation.streams, healthMap)
            relation.copy(streams = sortedStreams)
        }

    override suspend fun ensureSeeded(): Result<Int> =
        seedImporter.importSeedIfNeeded()

    override fun getFavorites(profileId: String?): Flow<List<ChannelEntity>> {
        val targetProfileFlow = if (profileId != null) {
            flowOf(profileId)
        } else {
            profileRepository?.activeProfileId ?: flowOf("default")
        }

        return targetProfileFlow.flatMapLatest { pId ->
            combine(favoriteDao.getFavoriteChannels(pId), userPreferences.isNsfwEnabled, isKidsProfileFlow) { list, nsfw, isKids ->
                filterForKidsAndNsfw(list, nsfw, isKids)
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun isFavorite(profileId: String?, channelId: String): Flow<Boolean> {
        val targetProfileFlow = if (profileId != null) {
            flowOf(profileId)
        } else {
            profileRepository?.activeProfileId ?: flowOf("default")
        }

        return targetProfileFlow.flatMapLatest { pId ->
            favoriteDao.isFavorite(pId, channelId)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun toggleFavorite(profileId: String?, channelId: String) =
        withContext(Dispatchers.IO) {
            val effectiveProfileId = resolveProfileId(profileId)
            val exists = favoriteDao.isFavoriteSync(effectiveProfileId, channelId)
            if (exists) {
                favoriteDao.removeFavorite(effectiveProfileId, channelId)
            } else {
                favoriteDao.addFavorite(
                    FavoriteEntity(
                        profileId = effectiveProfileId,
                        channelId = channelId
                    )
                )
                try {
                    val relation = channelDao.getChannelWithStreamsSync(channelId)
                    if (relation != null) {
                        val ch = relation.channel
                        database.mediaInteractionDao().recordInteraction(
                            com.nis.nistv.core.database.entity.MediaInteractionEntity(
                                profileId = effectiveProfileId,
                                mediaType = "CHANNEL",
                                mediaId = ch.id,
                                title = ch.cleanName.ifEmpty { ch.name },
                                categoryOrGenres = ch.category,
                                language = ch.language,
                                posterOrLogoUrl = ch.logoUrl,
                                interactionType = "FAVORITE",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
        }

    override fun getRecentlyWatched(profileId: String?): Flow<List<ChannelEntity>> {
        val targetProfileFlow = if (profileId != null) {
            flowOf(profileId)
        } else {
            profileRepository?.activeProfileId ?: flowOf("default")
        }

        return targetProfileFlow.flatMapLatest { pId ->
            combine(watchHistoryDao.getRecentlyWatchedChannels(pId), userPreferences.isNsfwEnabled, isKidsProfileFlow) { list, nsfw, isKids ->
                filterForKidsAndNsfw(list, nsfw, isKids)
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun recordWatch(profileId: String?, channelId: String, durationMs: Long) =
        withContext(Dispatchers.IO) {
            val effectiveProfileId = resolveProfileId(profileId)
            val isPaused = profileRepository?.activeProfile?.value?.historyPaused
                ?: (userPreferences.isWatchHistoryPaused.firstOrNull() ?: false)
            if (isPaused) return@withContext

            watchHistoryDao.recordWatch(
                WatchHistoryEntity(
                    profileId = effectiveProfileId,
                    channelId = channelId,
                    watchDurationMs = durationMs
                )
            )
            try {
                val relation = channelDao.getChannelWithStreamsSync(channelId)
                if (relation != null) {
                    val ch = relation.channel
                    database.mediaInteractionDao().recordInteraction(
                        com.nis.nistv.core.database.entity.MediaInteractionEntity(
                            profileId = effectiveProfileId,
                            mediaType = "CHANNEL",
                            mediaId = ch.id,
                            title = ch.cleanName.ifEmpty { ch.name },
                            categoryOrGenres = ch.category,
                            language = ch.language,
                            posterOrLogoUrl = ch.logoUrl,
                            interactionType = "WATCH",
                            watchDurationMs = durationMs,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            } catch (_: Exception) {}
        }

    override fun getRecentSearches(profileId: String?): Flow<List<String>> {
        val targetProfileFlow = if (profileId != null) {
            flowOf(profileId)
        } else {
            profileRepository?.activeProfileId ?: flowOf("default")
        }

        return targetProfileFlow.flatMapLatest { pId ->
            recentSearchDao.getRecentSearches(pId)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun addRecentSearch(profileId: String?, query: String) =
        withContext(Dispatchers.IO) {
            val effectiveProfileId = resolveProfileId(profileId)
            if (query.isNotBlank()) {
                recentSearchDao.deleteSearch(effectiveProfileId, query.trim())
                recentSearchDao.insertSearch(
                    RecentSearchEntity(
                        profileId = effectiveProfileId,
                        query = query.trim()
                    )
                )
            }
        }

    override suspend fun clearRecentSearches(profileId: String?) =
        withContext(Dispatchers.IO) {
            val effectiveProfileId = resolveProfileId(profileId)
            recentSearchDao.clearSearches(effectiveProfileId)
        }

    override suspend fun recordStreamFailure(streamId: Long) =
        withContext(Dispatchers.IO) {
            streamDao.recordStreamFailure(streamId)
        }

    override suspend fun resetStreamFailure(streamId: Long) =
        withContext(Dispatchers.IO) {
            streamDao.resetStreamFailure(streamId)
        }

    override fun getSyncMetadata(key: String): Flow<com.nis.nistv.core.database.entity.SyncMetadataEntity?> =
        syncMetadataDao.getMetadataFlow(key).flowOn(Dispatchers.IO)

    override fun triggerWorkManagerSync() {
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.nis.nistv.data.sync.CatalogueSyncWorker>()
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .build()

        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "manual_catalogue_sync",
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    override fun getChannelsByPlaylist(playlistId: String): Flow<List<ChannelEntity>> =
        combine(channelDao.getChannelsByPlaylist(playlistId), userPreferences.isNsfwEnabled, isKidsProfileFlow) { list, nsfw, isKids ->
            filterForKidsAndNsfw(list, nsfw, isKids)
        }.flowOn(Dispatchers.IO)

    override fun getAllCustomChannels(): Flow<List<ChannelEntity>> =
        combine(channelDao.getAllCustomChannels(), userPreferences.isNsfwEnabled, isKidsProfileFlow) { list, nsfw, isKids ->
            filterForKidsAndNsfw(list, nsfw, isKids)
        }.flowOn(Dispatchers.IO)
}
