package com.nis.nistv

import com.nis.nistv.core.database.NisTvDatabase
import com.nis.nistv.core.database.dao.WatchLaterDao
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.DownloadStatus
import com.nis.nistv.core.database.entity.WatchLaterEntity
import com.nis.nistv.core.database.entity.toTmdbMediaItem
import com.nis.nistv.data.repository.DefaultWatchLaterRepository
import com.nis.nistv.data.tmdb.model.TmdbMediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchLaterTest {

    @Test
    fun watchLaterEntity_toTmdbMediaItem_movie() {
        val entity = WatchLaterEntity(
            id = "movie_550",
            mediaId = 550L,
            mediaType = "movie",
            title = "Fight Club",
            posterPath = "/bptfVGEQuv6vDTIMVCHjJ9Dz8PX.jpg",
            backdropPath = "/hZkgoQYus5vegHoetLkCJzb17zJ.jpg",
            overview = "An ticking-time-bomb insomniac...",
            releaseYear = "1999",
            voteAverage = 8.433
        )

        val mediaItem = entity.toTmdbMediaItem()
        assertEquals(550L, mediaItem.id)
        assertEquals("Fight Club", mediaItem.title)
        assertEquals(TmdbMediaType.MOVIE, mediaItem.mediaType)
        assertEquals("1999", mediaItem.releaseYear)
        assertEquals(8.433, mediaItem.voteAverage, 0.001)
        assertEquals("★ 8.4", mediaItem.formattedRating)
        assertEquals("https://image.tmdb.org/t/p/w342/bptfVGEQuv6vDTIMVCHjJ9Dz8PX.jpg", mediaItem.fullPosterUrl)
    }

    @Test
    fun watchLaterEntity_toTmdbMediaItem_tv() {
        val entity = WatchLaterEntity(
            id = "tv_1399",
            mediaId = 1399L,
            mediaType = "tv",
            title = "Game of Thrones",
            posterPath = "/u3bZgnGQ9T01sWNhyveQz0wH0Hl.jpg",
            backdropPath = "/suaEOtk1N1sgg2MTM7oZd2cfVp3.jpg",
            overview = "Seven noble families fight for control...",
            releaseYear = "2011",
            voteAverage = 8.4
        )

        val mediaItem = entity.toTmdbMediaItem()
        assertEquals(1399L, mediaItem.id)
        assertEquals("Game of Thrones", mediaItem.title)
        assertEquals(TmdbMediaType.TV, mediaItem.mediaType)
        assertEquals("2011", mediaItem.releaseYear)
        assertEquals(8.4, mediaItem.voteAverage, 0.001)
    }

    @Test
    fun downloadEntity_persistsAfdsRenewalFields() {
        val download = DownloadEntity(
            id = "dl_1234",
            mediaId = "550",
            title = "Fight Club",
            downloadUrl = "https://server.afds.org/download/temporary-key",
            fileName = "Fight.Club.1999.1080p.mkv",
            filePath = "/storage/emulated/0/Download/Fight.Club.1999.1080p.mkv",
            fileSizeBytes = 2_000_000_000L,
            downloadedBytes = 500_000_000L,
            status = DownloadStatus.PAUSED.name,
            afdsFileId = "afds_file_987",
            afdsSourceTable = "movies",
            sourceProviderId = "afds"
        )

        assertEquals("afds_file_987", download.afdsFileId)
        assertEquals("movies", download.afdsSourceTable)
        assertEquals("afds", download.sourceProviderId)
    }

    @Test
    fun watchLaterRepository_toggleWatchLater_addsAndRemovesCorrectly() = runTest {
        val store = mutableMapOf<String, WatchLaterEntity>()

        val fakeDao = object : WatchLaterDao {
            override fun getAllWatchLater(profileId: String): Flow<List<WatchLaterEntity>> =
                flowOf(store.values.filter { it.profileId == profileId })

            override fun getWatchLaterByType(profileId: String, mediaType: String): Flow<List<WatchLaterEntity>> =
                flowOf(store.values.filter { it.profileId == profileId && it.mediaType == mediaType })

            override fun isInWatchLater(profileId: String, id: String): Flow<Boolean> =
                flowOf(store[id]?.profileId == profileId)

            override suspend fun isInWatchLaterSync(profileId: String, id: String): Boolean =
                store[id]?.profileId == profileId

            override suspend fun insertWatchLater(item: WatchLaterEntity) {
                store[item.id] = item
            }

            override suspend fun insertWatchLaterItems(items: List<WatchLaterEntity>) {
                items.forEach { store[it.id] = it }
            }

            override suspend fun getAllWatchLaterSync(profileId: String): List<WatchLaterEntity> =
                store.values.filter { it.profileId == profileId }

            override suspend fun getAllWatchLaterAcrossProfilesSync(): List<WatchLaterEntity> =
                store.values.toList()

            override suspend fun deleteWatchLater(profileId: String, id: String) {
                if (store[id]?.profileId == profileId) {
                    store.remove(id)
                }
            }

            override suspend fun deleteWatchLater(profileId: String, mediaType: String, mediaId: Long) {
                val key = "${mediaType}_$mediaId"
                if (store[key]?.profileId == profileId) {
                    store.remove(key)
                }
            }

            override suspend fun clearWatchLater(profileId: String) {
                val toRemove = store.filterValues { it.profileId == profileId }.keys
                toRemove.forEach { store.remove(it) }
            }

            override suspend fun clearAll() {
                store.clear()
            }

            override suspend fun migrateWatchLaterProfile(sourceProfileId: String, targetProfileId: String) {
                val updated = store.values.map {
                    if (it.profileId == sourceProfileId) it.copy(profileId = targetProfileId) else it
                }
                store.clear()
                updated.forEach { store[it.id] = it }
            }
        }

        val repository = DefaultWatchLaterRepository(fakeDao)
        val entity = WatchLaterEntity(
            id = "",
            mediaId = 100L,
            mediaType = "movie",
            title = "Inception"
        )

        // Initial toggle: should insert and return true
        val added = repository.toggleWatchLater(entity)
        assertTrue(added)
        assertTrue(fakeDao.isInWatchLaterSync("default", "movie_100"))
        assertEquals("Inception", store["movie_100"]?.title)

        // Second toggle: should delete and return false
        val removed = repository.toggleWatchLater(entity)
        assertFalse(removed)
        assertFalse(fakeDao.isInWatchLaterSync("default", "movie_100"))
        assertNull(store["movie_100"])
    }

    @Test
    fun databaseMigration_4_to_5_versionConfiguration() {
        assertEquals(4, NisTvDatabase.MIGRATION_4_5.startVersion)
        assertEquals(5, NisTvDatabase.MIGRATION_4_5.endVersion)
    }

    @Test
    fun watchLaterRepository_handlesTvSeriesCompositeKey() = runTest {
        val store = mutableMapOf<String, WatchLaterEntity>()
        val fakeDao = object : WatchLaterDao {
            override fun getAllWatchLater(profileId: String): Flow<List<WatchLaterEntity>> =
                flowOf(store.values.filter { it.profileId == profileId })

            override fun getWatchLaterByType(profileId: String, mediaType: String): Flow<List<WatchLaterEntity>> =
                flowOf(store.values.filter { it.profileId == profileId && it.mediaType == mediaType })

            override fun isInWatchLater(profileId: String, id: String): Flow<Boolean> =
                flowOf(store[id]?.profileId == profileId)

            override suspend fun isInWatchLaterSync(profileId: String, id: String): Boolean =
                store[id]?.profileId == profileId

            override suspend fun insertWatchLater(item: WatchLaterEntity) {
                store[item.id] = item
            }

            override suspend fun insertWatchLaterItems(items: List<WatchLaterEntity>) {
                items.forEach { store[it.id] = it }
            }

            override suspend fun getAllWatchLaterSync(profileId: String): List<WatchLaterEntity> =
                store.values.filter { it.profileId == profileId }

            override suspend fun getAllWatchLaterAcrossProfilesSync(): List<WatchLaterEntity> =
                store.values.toList()

            override suspend fun deleteWatchLater(profileId: String, id: String) {
                if (store[id]?.profileId == profileId) {
                    store.remove(id)
                }
            }

            override suspend fun deleteWatchLater(profileId: String, mediaType: String, mediaId: Long) {
                val key = "${mediaType}_$mediaId"
                if (store[key]?.profileId == profileId) {
                    store.remove(key)
                }
            }

            override suspend fun clearWatchLater(profileId: String) {
                val toRemove = store.filterValues { it.profileId == profileId }.keys
                toRemove.forEach { store.remove(it) }
            }

            override suspend fun clearAll() {
                store.clear()
            }

            override suspend fun migrateWatchLaterProfile(sourceProfileId: String, targetProfileId: String) {
                val updated = store.values.map {
                    if (it.profileId == sourceProfileId) it.copy(profileId = targetProfileId) else it
                }
                store.clear()
                updated.forEach { store[it.id] = it }
            }
        }

        val repository = DefaultWatchLaterRepository(fakeDao)
        val tvSeries = WatchLaterEntity(
            id = "tv_1399",
            mediaId = 1399L,
            mediaType = "tv",
            title = "Game of Thrones"
        )

        val added = repository.toggleWatchLater(tvSeries)
        assertTrue(added)
        assertTrue(fakeDao.isInWatchLaterSync("default", "tv_1399"))

        val removed = repository.toggleWatchLater(tvSeries)
        assertFalse(removed)
        assertFalse(fakeDao.isInWatchLaterSync("default", "tv_1399"))
    }
}
