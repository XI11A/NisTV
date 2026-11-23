package com.nis.nistv.data.repository

import com.nis.nistv.core.database.dao.WatchLaterDao
import com.nis.nistv.core.database.entity.WatchLaterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

interface WatchLaterRepository {
    fun getWatchLaterList(): Flow<List<WatchLaterEntity>>
    fun getAllWatchLater(): Flow<List<WatchLaterEntity>> = getWatchLaterList()
    fun getWatchLaterByType(mediaType: String): Flow<List<WatchLaterEntity>>
    fun isInWatchLater(mediaType: String, mediaId: Long): Flow<Boolean>
    suspend fun toggleWatchLater(item: WatchLaterEntity): Boolean
    suspend fun addToWatchLater(item: WatchLaterEntity)
    suspend fun removeFromWatchLater(mediaType: String, mediaId: Long)
    suspend fun clearAll()
}

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultWatchLaterRepository(
    private val watchLaterDao: WatchLaterDao,
    private val profileRepository: ProfileRepository? = null
) : WatchLaterRepository {

    private fun currentProfileId(): String =
        profileRepository?.activeProfileId?.value ?: "default"

    override fun getWatchLaterList(): Flow<List<WatchLaterEntity>> {
        return if (profileRepository != null) {
            profileRepository.activeProfileId.flatMapLatest { profileId ->
                watchLaterDao.getAllWatchLater(profileId)
            }.flowOn(Dispatchers.IO)
        } else {
            watchLaterDao.getAllWatchLater("default").flowOn(Dispatchers.IO)
        }
    }

    override fun getWatchLaterByType(mediaType: String): Flow<List<WatchLaterEntity>> {
        return if (profileRepository != null) {
            profileRepository.activeProfileId.flatMapLatest { profileId ->
                watchLaterDao.getWatchLaterByType(profileId, mediaType)
            }.flowOn(Dispatchers.IO)
        } else {
            watchLaterDao.getWatchLaterByType("default", mediaType).flowOn(Dispatchers.IO)
        }
    }

    override fun isInWatchLater(mediaType: String, mediaId: Long): Flow<Boolean> {
        val compositeId = "${mediaType}_$mediaId"
        return if (profileRepository != null) {
            profileRepository.activeProfileId.flatMapLatest { profileId ->
                watchLaterDao.isInWatchLater(profileId, compositeId)
            }.flowOn(Dispatchers.IO)
        } else {
            watchLaterDao.isInWatchLater("default", compositeId).flowOn(Dispatchers.IO)
        }
    }

    override suspend fun toggleWatchLater(item: WatchLaterEntity): Boolean = withContext(Dispatchers.IO) {
        val profileId = currentProfileId()
        val compositeId = "${item.mediaType}_${item.mediaId}"
        val exists = watchLaterDao.isInWatchLaterSync(profileId, compositeId)
        if (exists) {
            watchLaterDao.deleteWatchLater(profileId, compositeId)
            false
        } else {
            watchLaterDao.insertWatchLater(
                item.copy(
                    profileId = profileId,
                    id = compositeId,
                    addedAt = System.currentTimeMillis()
                )
            )
            true
        }
    }

    override suspend fun addToWatchLater(item: WatchLaterEntity) = withContext(Dispatchers.IO) {
        val profileId = currentProfileId()
        val compositeId = "${item.mediaType}_${item.mediaId}"
        watchLaterDao.insertWatchLater(
            item.copy(
                profileId = profileId,
                id = compositeId,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeFromWatchLater(mediaType: String, mediaId: Long) = withContext(Dispatchers.IO) {
        val profileId = currentProfileId()
        val compositeId = "${mediaType}_$mediaId"
        watchLaterDao.deleteWatchLater(profileId, compositeId)
    }

    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        val profileId = currentProfileId()
        watchLaterDao.clearWatchLater(profileId)
    }
}
