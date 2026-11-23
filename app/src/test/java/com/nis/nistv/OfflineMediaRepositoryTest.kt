package com.nis.nistv

import android.content.Context
import android.content.ContextWrapper
import com.nis.nistv.core.database.dao.DownloadDao
import com.nis.nistv.core.database.dao.OfflineMediaDao
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.DownloadStatus
import com.nis.nistv.core.database.entity.OfflineEpisodeEntity
import com.nis.nistv.core.database.entity.OfflineMediaEntity
import com.nis.nistv.core.database.entity.OfflineSeasonEntity
import com.nis.nistv.data.download.offline.DefaultOfflineMediaRepository
import com.nis.nistv.data.tmdb.model.TmdbGenre
import com.nis.nistv.data.tmdb.model.TmdbMediaItem
import com.nis.nistv.data.tmdb.model.TmdbMediaType
import com.nis.nistv.data.tmdb.model.TmdbMovieDetails
import com.nis.nistv.data.tmdb.model.TmdbSeasonDetails
import com.nis.nistv.data.tmdb.model.TmdbTvDetails
import com.nis.nistv.data.tmdb.repository.TmdbRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class OfflineMediaRepositoryTest {

    private lateinit var tempDir: File
    private val deletedMediaKeys = mutableListOf<String>()
    private val deletedSeasonKeys = mutableListOf<String>()
    private val deletedEpisodeKeys = mutableListOf<String>()
    private val downloadStore = mutableListOf<DownloadEntity>()

    private val fakeOfflineMediaDao = object : OfflineMediaDao {
        override fun getMedia(id: String): Flow<OfflineMediaEntity?> = flowOf(null)
        override suspend fun getMediaSync(id: String): OfflineMediaEntity? = null
        override fun getAllMedia(): Flow<List<OfflineMediaEntity>> = flowOf(emptyList())
        override suspend fun insertMedia(media: OfflineMediaEntity) {}
        override suspend fun deleteMedia(id: String) {
            deletedMediaKeys.add(id)
        }
        override fun getSeasons(seriesKey: String): Flow<List<OfflineSeasonEntity>> = flowOf(emptyList())
        override suspend fun getSeasonsSync(seriesKey: String): List<OfflineSeasonEntity> = emptyList()
        override suspend fun insertSeasons(seasons: List<OfflineSeasonEntity>) {}
        override suspend fun deleteSeasons(seriesKey: String) {
            deletedSeasonKeys.add(seriesKey)
        }
        override suspend fun deleteSeason(seriesKey: String, seasonNumber: Int) {}
        override fun getEpisodes(seriesKey: String): Flow<List<OfflineEpisodeEntity>> = flowOf(emptyList())
        override suspend fun getEpisodesSync(seriesKey: String): List<OfflineEpisodeEntity> = emptyList()
        override fun getEpisodesForSeason(seriesKey: String, seasonNumber: Int): Flow<List<OfflineEpisodeEntity>> = flowOf(emptyList())
        override suspend fun insertEpisodes(episodes: List<OfflineEpisodeEntity>) {}
        override suspend fun deleteEpisodes(seriesKey: String) {
            deletedEpisodeKeys.add(seriesKey)
        }
        override suspend fun deleteEpisodesForSeason(seriesKey: String, seasonNumber: Int) {}
    }

    private val fakeDownloadDao = object : DownloadDao {
        override fun getAllDownloads(): Flow<List<DownloadEntity>> = flowOf(downloadStore.toList())
        override fun getActiveDownloads(): Flow<List<DownloadEntity>> = flowOf(emptyList())
        override fun getCompletedDownloads(): Flow<List<DownloadEntity>> = flowOf(downloadStore.filter { it.status == DownloadStatus.COMPLETED.name })
        override suspend fun getCompletedDownloadsSync(): List<DownloadEntity> = downloadStore.filter { it.status == DownloadStatus.COMPLETED.name }
        override suspend fun getDownloadById(id: String): DownloadEntity? = downloadStore.find { it.id == id }
        override suspend fun getDownloadByMediaId(mediaId: String): DownloadEntity? = downloadStore.find { it.mediaId == mediaId }
        override suspend fun getAllDownloadsSync(): List<DownloadEntity> = downloadStore.toList()
        override suspend fun insertDownload(download: DownloadEntity) { downloadStore.add(download) }
        override suspend fun updateDownload(download: DownloadEntity) {}
        override suspend fun updateDownloads(downloads: List<DownloadEntity>) {}
        override suspend fun updateProgress(id: String, downloaded: Long, speed: Long, updatedAt: Long) {}
        override suspend fun updateStatus(id: String, status: String, updatedAt: Long) {}
        override suspend fun markFailed(id: String, status: String, error: String?, updatedAt: Long) {}
        override suspend fun deleteDownload(id: String) { downloadStore.removeAll { it.id == id } }
        override suspend fun deleteAllDownloads() { downloadStore.clear() }
        override suspend fun getDownloadsBySeriesSync(seriesId: String, seriesPrefix: String): List<DownloadEntity> =
            downloadStore.filter { it.effectiveSeriesId == seriesId }
        override fun getDownloadsBySeries(seriesId: String, seriesPrefix: String): Flow<List<DownloadEntity>> =
            flowOf(downloadStore.filter { it.effectiveSeriesId == seriesId })
        override suspend fun updatePlaybackPosition(id: String, position: Long, updatedAt: Long) {}
        override suspend fun updatePlaybackProgress(id: String, position: Long, duration: Long, lastWatched: Long, watchedCompleted: Boolean, updatedAt: Long) {}
        override suspend fun setHideFromContinueWatching(id: String, hide: Boolean, updatedAt: Long) {}
        override suspend fun updateWatchedCompleted(id: String, completed: Boolean, updatedAt: Long) {}
    }

    private val fakeTmdbRepository = object : TmdbRepository {
        override suspend fun getTrending(type: TmdbMediaType, timeWindow: String, forceRefresh: Boolean): List<TmdbMediaItem> = emptyList()
        override suspend fun getPopular(type: TmdbMediaType, page: Int, forceRefresh: Boolean): List<TmdbMediaItem> = emptyList()
        override suspend fun getNowPlayingOrAiring(type: TmdbMediaType, page: Int, forceRefresh: Boolean): List<TmdbMediaItem> = emptyList()
        override suspend fun getTopRated(type: TmdbMediaType, page: Int, forceRefresh: Boolean): List<TmdbMediaItem> = emptyList()
        override suspend fun getUpcomingOrOnTheAir(type: TmdbMediaType, page: Int, forceRefresh: Boolean): List<TmdbMediaItem> = emptyList()
        override suspend fun getGenres(type: TmdbMediaType): List<TmdbGenre> = emptyList()
        override suspend fun discoverByGenre(type: TmdbMediaType, genreId: Int?, page: Int): List<TmdbMediaItem> = emptyList()
        override suspend fun discoverByLanguage(type: TmdbMediaType, languageCode: String, page: Int, forceRefresh: Boolean): List<TmdbMediaItem> = emptyList()
        override suspend fun getMovieDetails(movieId: Long, forceRefresh: Boolean): TmdbMovieDetails = throw NotImplementedError()
        override suspend fun getTvDetails(tvId: Long, forceRefresh: Boolean): TmdbTvDetails = throw NotImplementedError()
        override suspend fun getTvSeason(tvId: Long, seasonNumber: Int): TmdbSeasonDetails = throw NotImplementedError()
        override suspend fun getRecommendations(type: TmdbMediaType, mediaId: Long, page: Int): List<TmdbMediaItem> = emptyList()
        override suspend fun getSimilar(type: TmdbMediaType, mediaId: Long, page: Int): List<TmdbMediaItem> = emptyList()
        override suspend fun search(type: TmdbMediaType, query: String, page: Int): List<TmdbMediaItem> = emptyList()
        override fun clearCache() {}
    }

    private lateinit var repository: DefaultOfflineMediaRepository

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("offline_media_test").toFile()
        deletedMediaKeys.clear()
        deletedSeasonKeys.clear()
        deletedEpisodeKeys.clear()
        downloadStore.clear()

        repository = DefaultOfflineMediaRepository(
            context = ContextWrapper(null),
            offlineMediaDao = fakeOfflineMediaDao,
            downloadDao = fakeDownloadDao,
            tmdbRepository = fakeTmdbRepository,
            okHttpClient = OkHttpClient(),
            baseFilesDir = tempDir
        )
    }

    @Test
    fun cleanup_tvSeries_partialDelete_keepsSeriesMetadataAndAssets() = runTest {
        val ep1 = DownloadEntity(
            id = "dl_ep1",
            mediaId = "1399_S01E01",
            seriesId = "1399",
            seriesName = "Game of Thrones",
            seasonNumber = 1,
            episodeNumber = 1,
            title = "Game of Thrones S01E01",
            downloadUrl = "https://example.com/ep1.mp4",
            fileName = "ep1.mp4",
            filePath = "/fake/ep1.mp4",
            fileSizeBytes = 1000L,
            status = DownloadStatus.COMPLETED.name,
            mediaType = "tv"
        )
        val ep2 = DownloadEntity(
            id = "dl_ep2",
            mediaId = "1399_S01E02",
            seriesId = "1399",
            seriesName = "Game of Thrones",
            seasonNumber = 1,
            episodeNumber = 2,
            title = "Game of Thrones S01E02",
            downloadUrl = "https://example.com/ep2.mp4",
            fileName = "ep2.mp4",
            filePath = "/fake/ep2.mp4",
            fileSizeBytes = 1000L,
            status = DownloadStatus.COMPLETED.name,
            mediaType = "tv"
        )

        downloadStore.add(ep1)
        downloadStore.add(ep2)

        // Create asset folder
        val assetDir = repository.getMediaAssetDir("tv", 1399L)
        val poster = File(assetDir, "poster.jpg")
        poster.writeText("fake poster bytes")
        assertTrue(poster.exists())

        // Simulate deleting ep1 from downloadDao, then calling onDownloadDeletedOrCancelled
        downloadStore.remove(ep1)
        repository.onDownloadDeletedOrCancelled(ep1)

        // Verification: Since ep2 remains, series metadata and assets MUST NOT be deleted
        assertFalse(deletedMediaKeys.contains("tv_1399"))
        assertFalse(deletedSeasonKeys.contains("tv_1399"))
        assertFalse(deletedEpisodeKeys.contains("tv_1399"))
        assertTrue(assetDir.exists())
        assertTrue(poster.exists())
    }

    @Test
    fun cleanup_tvSeries_lastEpisodeDelete_deletesSeriesMetadataAndAssets() = runTest {
        val ep1 = DownloadEntity(
            id = "dl_ep1",
            mediaId = "1399_S01E01",
            seriesId = "1399",
            seriesName = "Game of Thrones",
            seasonNumber = 1,
            episodeNumber = 1,
            title = "Game of Thrones S01E01",
            downloadUrl = "https://example.com/ep1.mp4",
            fileName = "ep1.mp4",
            filePath = "/fake/ep1.mp4",
            fileSizeBytes = 1000L,
            status = DownloadStatus.COMPLETED.name,
            mediaType = "tv"
        )

        downloadStore.add(ep1)

        val assetDir = repository.getMediaAssetDir("tv", 1399L)
        val poster = File(assetDir, "poster.jpg")
        poster.writeText("fake poster bytes")
        assertTrue(poster.exists())

        // Delete last episode
        downloadStore.remove(ep1)
        repository.onDownloadDeletedOrCancelled(ep1)

        // Verification: No remaining episodes -> metadata and asset folder removed
        assertTrue(deletedMediaKeys.contains("tv_1399"))
        assertTrue(deletedSeasonKeys.contains("tv_1399"))
        assertTrue(deletedEpisodeKeys.contains("tv_1399"))
        assertFalse(poster.exists())
        assertFalse(assetDir.exists())
    }

    @Test
    fun cleanup_movie_lastDownloadDelete_deletesMovieMetadataAndAssets() = runTest {
        val movie = DownloadEntity(
            id = "dl_m1",
            mediaId = "550",
            title = "Fight Club",
            downloadUrl = "https://example.com/fc.mp4",
            fileName = "fc.mp4",
            filePath = "/fake/fc.mp4",
            fileSizeBytes = 2000L,
            status = DownloadStatus.COMPLETED.name,
            mediaType = "movie"
        )

        downloadStore.add(movie)

        val assetDir = repository.getMediaAssetDir("movie", 550L)
        val backdrop = File(assetDir, "backdrop.jpg")
        backdrop.writeText("fake backdrop bytes")
        assertTrue(backdrop.exists())

        // Delete movie download
        downloadStore.remove(movie)
        repository.onDownloadDeletedOrCancelled(movie)

        // Verification: Metadata and asset directory removed
        assertTrue(deletedMediaKeys.contains("movie_550"))
        assertFalse(backdrop.exists())
        assertFalse(assetDir.exists())
    }
}
