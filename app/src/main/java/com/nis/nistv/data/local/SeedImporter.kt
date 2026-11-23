package com.nis.nistv.data.local

import android.content.Context
import com.nis.nistv.core.database.NisTvDatabase
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.ProfileEntity
import com.nis.nistv.core.database.entity.StreamSourceEntity
import com.nis.nistv.data.model.SeedChannelDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SeedImporter(
    private val context: Context,
    private val database: NisTvDatabase
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun importSeedIfNeeded(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Ensure default profile exists
            val profileDao = database.profileDao()
            if (profileDao.getProfileById("default") == null) {
                profileDao.insertProfile(
                    ProfileEntity(
                        id = "default",
                        name = "Primary Profile",
                        preferredLanguage = "Hindi"
                    )
                )
            }

            // Check if channels are already present
            val channelDao = database.channelDao()
            val existingCount = channelDao.getChannelsCount()
            if (existingCount > 0) {
                return@withContext Result.success(existingCount)
            }

            // Read seed from assets
            val seedString = context.assets.open("seed/channels_seed.json").bufferedReader().use {
                it.readText()
            }

            val seedList = json.decodeFromString<List<SeedChannelDto>>(seedString)

            val channelEntities = ArrayList<ChannelEntity>(seedList.size)
            val streamEntities = ArrayList<StreamSourceEntity>(seedList.size)

            for (seed in seedList) {
                channelEntities.add(
                    ChannelEntity(
                        id = seed.id,
                        name = seed.name,
                        cleanName = seed.cleanName.ifEmpty { seed.name },
                        altNames = seed.altNames,
                        country = seed.country,
                        network = seed.network,
                        website = seed.website,
                        language = seed.language,
                        category = seed.category,
                        logoUrl = seed.logoUrl,
                        resolution = seed.resolution,
                        isNsfw = seed.isNsfw,
                        labels = seed.labels
                    )
                )
                streamEntities.add(
                    StreamSourceEntity(
                        channelId = seed.id,
                        url = seed.streamUrl,
                        priority = 0,
                        quality = seed.resolution
                    )
                )
            }

            // Transactional insert
            channelDao.insertChannels(channelEntities)
            channelDao.insertStreams(streamEntities)

            Result.success(channelEntities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
