package com.nis.nistv.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nis.nistv.core.database.NisTvDatabase
import com.nis.nistv.core.database.entity.SyncMetadataEntity
import com.nis.nistv.data.remote.FetchResult
import com.nis.nistv.data.remote.IptvApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CatalogueSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val database = NisTvDatabase.getInstance(appContext)
    private val channelDao = database.channelDao()
    private val streamDao = database.streamSourceDao()
    private val favoriteDao = database.favoriteDao()
    private val syncMetadataDao = database.syncMetadataDao()
    private val iptvApi = IptvApi(appContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val tempFiles = mutableListOf<File>()
        try {
            setProgress(workDataOf("status" to "CHECKING_METADATA"))

            val channelMeta = syncMetadataDao.getMetadata(IptvApi.CHANNELS_ENDPOINT)
            val streamMeta = syncMetadataDao.getMetadata(IptvApi.STREAMS_ENDPOINT)

            // 1. Fetch Categories & Languages
            setProgress(workDataOf("status" to "FETCHING_METADATA_RESOURCES"))
            val catResult = iptvApi.fetchResource(IptvApi.CATEGORIES_ENDPOINT)
            catResult.file?.let { tempFiles.add(it) }
            val categoryMap = if (catResult.file != null) iptvApi.parseCategories(catResult.file) else emptyMap()

            val langResult = iptvApi.fetchResource(IptvApi.LANGUAGES_ENDPOINT)
            langResult.file?.let { tempFiles.add(it) }
            val languageMap = if (langResult.file != null) iptvApi.parseLanguages(langResult.file) else emptyMap()

            val blocklistResult = iptvApi.fetchResource(IptvApi.BLOCKLIST_ENDPOINT)
            blocklistResult.file?.let { tempFiles.add(it) }
            val blocklist = if (blocklistResult.file != null) iptvApi.parseBlocklist(blocklistResult.file) else emptySet()

            // 2. Fetch Channels with conditional request
            setProgress(workDataOf("status" to "FETCHING_CHANNELS"))
            val channelResult = iptvApi.fetchResource(
                endpoint = IptvApi.CHANNELS_ENDPOINT,
                etag = channelMeta?.etag,
                lastModified = channelMeta?.lastModified
            )
            channelResult.file?.let { tempFiles.add(it) }

            // 3. Fetch Streams with conditional request
            setProgress(workDataOf("status" to "FETCHING_STREAMS"))
            val streamResult = iptvApi.fetchResource(
                endpoint = IptvApi.STREAMS_ENDPOINT,
                etag = streamMeta?.etag,
                lastModified = streamMeta?.lastModified
            )
            streamResult.file?.let { tempFiles.add(it) }

            // If neither has changed, report success and exit early
            if (channelResult.isNotModified && streamResult.isNotModified) {
                syncMetadataDao.insertOrUpdate(
                    SyncMetadataEntity(
                        resourceKey = "CATALOGUE_FULL",
                        lastSyncTimestamp = System.currentTimeMillis(),
                        status = "SUCCESS",
                        errorMessage = null
                    )
                )
                return@withContext Result.success(workDataOf("message" to "Catalogue not modified"))
            }

            // Fetch Logos for ranking
            val logosResult = iptvApi.fetchResource(IptvApi.LOGOS_ENDPOINT)
            logosResult.file?.let { tempFiles.add(it) }

            // Parse Channels
            val channelFile = channelResult.file
            if (channelFile == null && !channelResult.isNotModified) {
                throw IllegalStateException("Channels file missing from successful fetch")
            }

            setProgress(workDataOf("status" to "PARSING_CHANNELS"))
            val channels = if (channelFile != null) {
                // Parse channels across all available regions from iptv-org
                val prelimChannels = iptvApi.parseChannels(
                    file = channelFile,
                    allowedCountries = emptySet(),
                    blocklist = blocklist,
                    categoryMap = categoryMap,
                    languageMap = languageMap,
                    logosMap = emptyMap()
                )
                val channelIds = prelimChannels.map { it.id }.toSet()

                // Parse logos only for allowed channel IDs
                val logosMap = if (logosResult.file != null) {
                    iptvApi.parseLogos(logosResult.file, channelIds)
                } else {
                    emptyMap()
                }

                // Re-map with deterministic logos
                prelimChannels.map { ch ->
                    val logo = logosMap[ch.id]
                    if (!logo.isNullOrBlank()) ch.copy(logoUrl = logo) else ch
                }
            } else {
                emptyList()
            }

            if (channels.isEmpty() && channelFile != null) {
                throw IllegalStateException("Parsed channel list is empty. Aborting sync to preserve local database.")
            }

            // Parse Streams
            val streamFile = streamResult.file
            setProgress(workDataOf("status" to "PARSING_STREAMS"))
            val targetChannelIds = if (channels.isNotEmpty()) {
                channels.map { it.id }.toSet()
            } else {
                emptySet()
            }

            val streamsMap = if (streamFile != null && targetChannelIds.isNotEmpty()) {
                iptvApi.parseStreams(streamFile, targetChannelIds)
            } else {
                emptyMap()
            }

            // Apply updates transactionally
            setProgress(workDataOf("status" to "APPLYING_DATABASE_UPDATES"))
            if (channels.isNotEmpty()) {
                channelDao.insertChannels(channels)
            }

            val flattenedStreams = streamsMap.values.flatten()
            if (flattenedStreams.isNotEmpty()) {
                streamDao.insertStreams(flattenedStreams)
            }

            // Save sync metadata
            syncMetadataDao.insertOrUpdate(
                SyncMetadataEntity(
                    resourceKey = IptvApi.CHANNELS_ENDPOINT,
                    etag = channelResult.etag,
                    lastModified = channelResult.lastModified,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    status = "SUCCESS"
                )
            )

            syncMetadataDao.insertOrUpdate(
                SyncMetadataEntity(
                    resourceKey = IptvApi.STREAMS_ENDPOINT,
                    etag = streamResult.etag,
                    lastModified = streamResult.lastModified,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    status = "SUCCESS"
                )
            )

            syncMetadataDao.insertOrUpdate(
                SyncMetadataEntity(
                    resourceKey = "CATALOGUE_FULL",
                    lastSyncTimestamp = System.currentTimeMillis(),
                    status = "SUCCESS",
                    errorMessage = null
                )
            )

            Result.success(
                workDataOf(
                    "channels_count" to channels.size,
                    "streams_count" to flattenedStreams.size
                )
            )
        } catch (e: Exception) {
            syncMetadataDao.insertOrUpdate(
                SyncMetadataEntity(
                    resourceKey = "CATALOGUE_FULL",
                    lastSyncTimestamp = System.currentTimeMillis(),
                    status = "FAILURE",
                    errorMessage = e.message ?: "Unknown sync error"
                )
            )
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error" to (e.message ?: "Sync failed")))
            }
        } finally {
            // Clean up all temporary files
            for (file in tempFiles) {
                try {
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
