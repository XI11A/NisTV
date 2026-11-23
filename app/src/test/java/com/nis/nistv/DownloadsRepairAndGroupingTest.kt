package com.nis.nistv

import android.content.Context
import com.nis.nistv.core.database.dao.DownloadDao
import com.nis.nistv.core.database.dao.OfflineMediaDao
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.DownloadStatus
import com.nis.nistv.core.database.entity.OfflineEpisodeEntity
import com.nis.nistv.core.database.entity.OfflineMediaEntity
import com.nis.nistv.core.database.entity.OfflineSeasonEntity
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.data.download.offline.*
import com.nis.nistv.data.tmdb.model.*
import com.nis.nistv.data.tmdb.repository.TmdbRepository
import com.nis.nistv.feature.downloads.DownloadsGrouping
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class DownloadsRepairAndGroupingTest {

    // --- 1. Parser Tests (19 Real-world Patterns) ---
    @Test
    fun mediaNameParser_standardSxxExx_parsesAccurately() {
        val info1 = MediaNameParser.parse("Silicon.Valley.S05E07.1080p.WEBRip.x264-AMZN.mkv")
        assertTrue(info1.isTv)
        assertEquals("Silicon Valley", info1.seriesName)
        assertEquals(5, info1.seasonNumber)
        assertEquals(7, info1.episodeNumber)

        val info2 = MediaNameParser.parse("Silicon.Valley.S05E08.1080p.WEBRip.x264.mkv")
        assertTrue(info2.isTv)
        assertEquals("Silicon Valley", info2.seriesName)
        assertEquals(5, info2.seasonNumber)
        assertEquals(8, info2.episodeNumber)

        val info3 = MediaNameParser.parse("Silicon.Valley.S06E01.1080p.WEBRip.x264.mkv")
        assertTrue(info3.isTv)
        assertEquals("Silicon Valley", info3.seriesName)
        assertEquals(6, info3.seasonNumber)
        assertEquals(1, info3.episodeNumber)

        val info4 = MediaNameParser.parse("Silicon.Valley.S06E05.1080p.WEBRip.x264.mkv")
        assertTrue(info4.isTv)
        assertEquals("Silicon Valley", info4.seriesName)
        assertEquals(6, info4.seasonNumber)
        assertEquals(5, info4.episodeNumber)
    }

    @Test
    fun mediaNameParser_caseInsensitiveAndDotted_parsesAccurately() {
        val info = MediaNameParser.parse("stranger.things.s01e01.1080p.webrip.mkv")
        assertTrue(info.isTv)
        assertEquals("Stranger Things", info.seriesName)
        assertEquals(1, info.seasonNumber)
        assertEquals(1, info.episodeNumber)
    }

    @Test
    fun mediaNameParser_1x05Format_parsesAccurately() {
        val info = MediaNameParser.parse("Breaking.Bad.1x05.720p.HDTV.x264.mp4")
        assertTrue(info.isTv)
        assertEquals("Breaking Bad", info.seriesName)
        assertEquals(1, info.seasonNumber)
        assertEquals(5, info.episodeNumber)
    }

    @Test
    fun mediaNameParser_seasonAndEpisodeSpelledOut_parsesAccurately() {
        val info1 = MediaNameParser.parse("The.Office.US.Season.1.Episode.5.1080p.WEBRip.mkv")
        assertTrue(info1.isTv)
        assertEquals("The Office US", info1.seriesName)
        assertEquals(1, info1.seasonNumber)
        assertEquals(5, info1.episodeNumber)

        val info2 = MediaNameParser.parse("The Office Season 2 Episode 10.mkv")
        assertTrue(info2.isTv)
        assertEquals("The Office", info2.seriesName)
        assertEquals(2, info2.seasonNumber)
        assertEquals(10, info2.episodeNumber)
    }

    @Test
    fun mediaNameParser_episodeOnlyToken_defaultsToSeasonOne() {
        val info = MediaNameParser.parse("Game.of.Thrones.Ep.05.720p.mkv")
        assertTrue(info.isTv)
        assertEquals("Game Of Thrones", info.seriesName)
        assertEquals(1, info.seasonNumber)
        assertEquals(5, info.episodeNumber)
    }

    @Test
    fun mediaNameParser_withYearInSeriesTitle_extractsYearAndTitle() {
        val info = MediaNameParser.parse("Fargo.2014.S02E04.1080p.mkv")
        assertTrue(info.isTv)
        assertEquals("Fargo", info.seriesName)
        assertEquals(2014, info.year)
        assertEquals(2, info.seasonNumber)
        assertEquals(4, info.episodeNumber)
    }

    @Test
    fun mediaNameParser_additionalPopularReleases() {
        val c = MediaNameParser.parse("Chernobyl.S01E03.1080p.BluRay.x265.HEVC.mkv")
        assertTrue(c.isTv)
        assertEquals("Chernobyl", c.seriesName)
        assertEquals(1, c.seasonNumber)
        assertEquals(3, c.episodeNumber)

        val s = MediaNameParser.parse("Severance.s01e09.2160p.WEB-DL.mkv")
        assertTrue(s.isTv)
        assertEquals("Severance", s.seriesName)
        assertEquals(1, s.seasonNumber)
        assertEquals(9, s.episodeNumber)

        val bcs = MediaNameParser.parse("Better.Call.Saul.S06E13.720p.x264.mp4")
        assertTrue(bcs.isTv)
        assertEquals("Better Call Saul", bcs.seriesName)
        assertEquals(6, bcs.seasonNumber)
        assertEquals(13, bcs.episodeNumber)

        val hotd = MediaNameParser.parse("House.of.the.Dragon.S01E01.mkv")
        assertTrue(hotd.isTv)
        assertEquals("House Of The Dragon", hotd.seriesName)
        assertEquals(1, hotd.seasonNumber)
        assertEquals(1, hotd.episodeNumber)

        val rm = MediaNameParser.parse("Rick.and.Morty.S04E06.1080p.mkv")
        assertTrue(rm.isTv)
        assertEquals("Rick And Morty", rm.seriesName)
        assertEquals(4, rm.seasonNumber)
        assertEquals(6, rm.episodeNumber)

        val tl = MediaNameParser.parse("Ted.Lasso.2x12.1080p.mkv")
        assertTrue(tl.isTv)
        assertEquals("Ted Lasso", tl.seriesName)
        assertEquals(2, tl.seasonNumber)
        assertEquals(12, tl.episodeNumber)
    }

    @Test
    fun mediaNameParser_movies_notClassifiedAsTv() {
        val m1 = MediaNameParser.parse("Inception.2010.1080p.BluRay.x264.mkv")
        assertFalse(m1.isTv)
        assertEquals("Inception", m1.movieTitle)
        assertEquals(2010, m1.year)

        val m2 = MediaNameParser.parse("Interstellar.2014.IMAX.1080p.mkv")
        assertFalse(m2.isTv)
        assertEquals("Interstellar", m2.movieTitle)
        assertEquals(2014, m2.year)
    }

    @Test
    fun mediaNameParser_unrecognizedStrings_gracefullyHandled() {
        val nonTv1 = MediaNameParser.parse("random_home_video.mp4")
        assertFalse(nonTv1.isTv)
        assertNull(nonTv1.seriesName)

        val nonTv2 = MediaNameParser.parse("sample_clip_without_ext")
        assertFalse(nonTv2.isTv)

        val empty = MediaNameParser.parse("")
        assertFalse(empty.isTv)
        assertNull(empty.seriesName)
    }

    // --- 2. SeriesKeyHelper Tests ---
    @Test
    fun seriesKeyHelper_buildsCanonicalAndFallbackKeys() {
        assertEquals("tv_60573", SeriesKeyHelper.buildSeriesKey(60573L))
        assertEquals("tv_unsorted", SeriesKeyHelper.buildSeriesKey(null))

        assertEquals("tv_silicon_valley", SeriesKeyHelper.buildFallbackSeriesKey("Silicon Valley"))
        assertEquals("tv_stranger_things", SeriesKeyHelper.buildFallbackSeriesKey("stranger things"))
        assertEquals("tv_the_office_us", SeriesKeyHelper.buildFallbackSeriesKey("The Office (US)"))

        assertEquals("tv_60573", SeriesKeyHelper.getSeriesKey("60573", "Silicon Valley"))
        assertEquals("tv_silicon_valley", SeriesKeyHelper.getSeriesKey("tv_silicon_valley", "Silicon Valley"))
        assertEquals("tv_silicon_valley", SeriesKeyHelper.getSeriesKey(null, "Silicon Valley"))

        assertFalse(SeriesKeyHelper.isFallbackKey("tv_60573"))
        assertTrue(SeriesKeyHelper.isFallbackKey("tv_silicon_valley"))
        assertEquals(60573L, SeriesKeyHelper.extractTmdbId("tv_60573"))
        assertNull(SeriesKeyHelper.extractTmdbId("tv_silicon_valley"))
    }

    // --- 3. Pre-v6 Fixture Test: Exact 8 Files from Device ---
    @Test
    fun preV6Fixture_repairsAndGroupsAll8FilesAccurately() = runTest {
        val downloadStore = mutableListOf<DownloadEntity>()

        // 7 pre-v6 rows for Silicon Valley (null seriesId and seriesName)
        // Note: out-of-order episodes E05, E04, E03, E01, E02 for Season 6
        val svFiles = listOf(
            "Silicon.Valley.S05E07.1080p.WEBRip.x264-AMZN.mkv" to 5 to 7,
            "Silicon.Valley.S05E08.1080p.WEBRip.x264.mkv" to 5 to 8,
            "Silicon.Valley.S06E05.1080p.WEBRip.x264.mkv" to 6 to 5,
            "Silicon.Valley.S06E04.1080p.WEBRip.x264.mkv" to 6 to 4,
            "Silicon.Valley.S06E03.1080p.WEBRip.x264.mkv" to 6 to 3,
            "Silicon.Valley.S06E01.1080p.WEBRip.x264.mkv" to 6 to 1,
            "Silicon.Valley.S06E02.1080p.WEBRip.x264.mkv" to 6 to 2
        )

        var counter = 1
        for (item in svFiles) {
            val fn = item.first.first
            downloadStore.add(
                DownloadEntity(
                    id = "dl_sv_${counter++}",
                    mediaId = "manual_afds_$counter",
                    seriesId = null, // Old pre-v6 row: null seriesId
                    seriesName = null, // Old pre-v6 row: null seriesName
                    seasonNumber = null, // Old pre-v6 row: null
                    episodeNumber = null, // Old pre-v6 row: null
                    title = fn.substringBeforeLast('.'),
                    downloadUrl = "https://example.com/$fn",
                    fileName = fn,
                    filePath = "/storage/emulated/0/Download/NisTV/$fn",
                    fileSizeBytes = 1_500_000_000L,
                    downloadedBytes = 1_500_000_000L,
                    status = DownloadStatus.COMPLETED.name,
                    mediaType = "tv"
                )
            )
        }

        // 1 post-v6 row for Stranger Things
        downloadStore.add(
            DownloadEntity(
                id = "dl_st_1",
                mediaId = "66732_S01E01",
                seriesId = "66732",
                seriesName = "Stranger Things",
                seasonNumber = 1,
                episodeNumber = 1,
                title = "Stranger Things S01E01",
                downloadUrl = "https://example.com/st01.mkv",
                fileName = "stranger.things.s01e01.1080p.webrip.mkv",
                filePath = "/storage/emulated/0/Download/NisTV/stranger.things.s01e01.1080p.webrip.mkv",
                fileSizeBytes = 2_000_000_000L,
                downloadedBytes = 2_000_000_000L,
                status = DownloadStatus.COMPLETED.name,
                mediaType = "tv"
            )
        )

        assertEquals(8, downloadStore.size)

        // Set up fakes for repair
        val fakeDownloadDao = object : DownloadDao {
            override fun getAllDownloads(): Flow<List<DownloadEntity>> = flowOf(downloadStore.toList())
            override fun getActiveDownloads(): Flow<List<DownloadEntity>> = flowOf(emptyList())
            override fun getCompletedDownloads(): Flow<List<DownloadEntity>> = flowOf(downloadStore.filter { it.status == DownloadStatus.COMPLETED.name })
            override suspend fun getCompletedDownloadsSync(): List<DownloadEntity> = downloadStore.filter { it.status == DownloadStatus.COMPLETED.name }
            override suspend fun getAllDownloadsSync(): List<DownloadEntity> = downloadStore.toList()
            override suspend fun getDownloadById(id: String): DownloadEntity? = downloadStore.find { it.id == id }
            override suspend fun getDownloadByMediaId(mediaId: String): DownloadEntity? = downloadStore.find { it.mediaId == mediaId }
            override suspend fun insertDownload(download: DownloadEntity) { downloadStore.add(download) }
            override suspend fun updateDownload(download: DownloadEntity) {
                val index = downloadStore.indexOfFirst { it.id == download.id }
                if (index >= 0) downloadStore[index] = download
            }
            override suspend fun updateDownloads(downloads: List<DownloadEntity>) {
                downloads.forEach { updateDownload(it) }
            }
            override suspend fun updateProgress(id: String, downloaded: Long, speed: Long, updatedAt: Long) {}
            override suspend fun updateStatus(id: String, status: String, updatedAt: Long) {}
            override suspend fun markFailed(id: String, status: String, error: String?, updatedAt: Long) {}
            override suspend fun deleteDownload(id: String) { downloadStore.removeAll { it.id == id } }
            override suspend fun deleteAllDownloads() { downloadStore.clear() }
            override suspend fun getDownloadsBySeriesSync(seriesId: String, seriesPrefix: String): List<DownloadEntity> =
                downloadStore.filter { it.seriesId == seriesId || it.seriesId == seriesId.removePrefix("tv_") || it.seriesName == seriesId }
            override fun getDownloadsBySeries(seriesId: String, seriesPrefix: String): Flow<List<DownloadEntity>> =
                flowOf(downloadStore.filter { it.seriesId == seriesId || it.seriesId == seriesId.removePrefix("tv_") || it.seriesName == seriesId })
            override suspend fun updatePlaybackPosition(id: String, position: Long, updatedAt: Long) {}
            override suspend fun updatePlaybackProgress(id: String, position: Long, duration: Long, lastWatched: Long, watchedCompleted: Boolean, updatedAt: Long) {}
            override suspend fun setHideFromContinueWatching(id: String, hide: Boolean, updatedAt: Long) {}
            override suspend fun updateWatchedCompleted(id: String, completed: Boolean, updatedAt: Long) {}
        }

        val fakeOfflineMediaDao = object : OfflineMediaDao {
            override fun getMedia(id: String): Flow<OfflineMediaEntity?> = flowOf(null)
            override suspend fun getMediaSync(id: String): OfflineMediaEntity? = null
            override fun getAllMedia(): Flow<List<OfflineMediaEntity>> = flowOf(emptyList())
            override suspend fun insertMedia(media: OfflineMediaEntity) {}
            override suspend fun deleteMedia(id: String) {}
            override fun getSeasons(seriesKey: String): Flow<List<OfflineSeasonEntity>> = flowOf(emptyList())
            override suspend fun getSeasonsSync(seriesKey: String): List<OfflineSeasonEntity> = emptyList()
            override suspend fun insertSeasons(seasons: List<OfflineSeasonEntity>) {}
            override suspend fun deleteSeasons(seriesKey: String) {}
            override fun getEpisodes(seriesKey: String): Flow<List<OfflineEpisodeEntity>> = flowOf(emptyList())
            override suspend fun getEpisodesSync(seriesKey: String): List<OfflineEpisodeEntity> = emptyList()
            override fun getEpisodesForSeason(seriesKey: String, seasonNumber: Int): Flow<List<OfflineEpisodeEntity>> = flowOf(emptyList())
            override suspend fun insertEpisodes(episodes: List<OfflineEpisodeEntity>) {}
            override suspend fun deleteEpisodes(seriesKey: String) {}
            override suspend fun deleteSeason(seriesKey: String, seasonNumber: Int) {}
            override suspend fun deleteEpisodesForSeason(seriesKey: String, seasonNumber: Int) {}
        }

        val fakeTmdbRepo = object : TmdbRepository {
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
            override suspend fun search(type: TmdbMediaType, query: String, page: Int): List<TmdbMediaItem> {
                if (query.contains("Silicon", ignoreCase = true)) {
                    return listOf(
                        TmdbMediaItem(
                            id = 60573L,
                            title = "Silicon Valley",
                            overview = "Silicon Valley follows the struggle of Richard Hendricks",
                            posterPath = "/poster.jpg",
                            backdropPath = "/backdrop.jpg",
                            releaseYear = "2014",
                            voteAverage = 8.5,
                            mediaType = TmdbMediaType.TV
                        )
                    )
                }
                return emptyList()
            }
            override fun clearCache() {}
        }

        val fakeOfflineMediaRepo = object : OfflineMediaRepository {
            override val isSavingOfflineDetails = MutableStateFlow(false)
            override fun getOfflineMedia(mediaType: String, tmdbId: Long): Flow<OfflineMediaEntity?> = flowOf(null)
            override suspend fun getOfflineMediaSync(mediaType: String, tmdbId: Long): OfflineMediaEntity? = null
            override fun getOfflineSeasons(seriesKey: String): Flow<List<OfflineSeasonEntity>> = flowOf(emptyList())
            override fun getOfflineEpisodesForSeason(seriesKey: String, seasonNumber: Int): Flow<List<OfflineEpisodeEntity>> = flowOf(emptyList())
            override fun getOfflineEpisodesForSeries(seriesKey: String): Flow<List<OfflineEpisodeEntity>> = flowOf(emptyList())
            override suspend fun saveFor(download: DownloadEntity, forceRefresh: Boolean) {}
            override suspend fun backfillPendingMetadata() {}
            override suspend fun onDownloadDeletedOrCancelled(deletedDownload: DownloadEntity) {}
            override suspend fun deleteSeason(seriesKey: String, seasonNumber: Int) {}
        }

        val repairManager = DownloadsRepairManager(
            context = null,
            downloadDao = fakeDownloadDao,
            offlineMediaDao = fakeOfflineMediaDao,
            offlineMediaRepository = fakeOfflineMediaRepo,
            tmdbRepository = fakeTmdbRepo,
            userPreferences = null
        )

        // 1. Run repair
        val repairResult = repairManager.runRepairIfNeeded(force = true)
        assertEquals(7, repairResult.repairedRowsCount)

        // 2. Perform Grouping
        val completed = fakeDownloadDao.getAllDownloadsSync()
        val movies = DownloadsGrouping.groupMovies(completed)
        val tvSeries = DownloadsGrouping.groupTvSeries(completed)

        // Verify Movies: none
        assertEquals(0, movies.size)

        // Verify TV Series: exactly 2 series
        assertEquals(2, tvSeries.size)

        val svGroup = tvSeries.find { it.seriesName == "Silicon Valley" }
        assertNotNull(svGroup)
        assertEquals("60573", svGroup?.seriesId)
        assertEquals(7, svGroup?.episodesCount)
        assertEquals(2, svGroup?.seasonsCount)

        // Verify Silicon Valley Season 5: exactly 2 episodes in numeric order
        val season5 = svGroup?.seasons?.find { it.seasonNumber == 5 }
        assertNotNull(season5)
        assertEquals(2, season5?.episodes?.size)
        assertEquals(7, season5?.episodes?.get(0)?.episodeNumber)
        assertEquals(8, season5?.episodes?.get(1)?.episodeNumber)

        // Verify Silicon Valley Season 6: exactly 5 episodes in numeric order
        // (even though downloaded out of order: E05, E04, E03, E01, E02)
        val season6 = svGroup?.seasons?.find { it.seasonNumber == 6 }
        assertNotNull(season6)
        assertEquals(5, season6?.episodes?.size)
        assertEquals(1, season6?.episodes?.get(0)?.episodeNumber)
        assertEquals(2, season6?.episodes?.get(1)?.episodeNumber)
        assertEquals(3, season6?.episodes?.get(2)?.episodeNumber)
        assertEquals(4, season6?.episodes?.get(3)?.episodeNumber)
        assertEquals(5, season6?.episodes?.get(4)?.episodeNumber)

        // Verify Stranger Things: unchanged, not duplicated
        val stGroup = tvSeries.find { it.seriesName == "Stranger Things" }
        assertNotNull(stGroup)
        assertEquals("66732", stGroup?.seriesId)
        assertEquals(1, stGroup?.episodesCount)
        assertEquals(1, stGroup?.seasonsCount)
        assertEquals(1, stGroup?.seasons?.get(0)?.seasonNumber)
        assertEquals(1, stGroup?.seasons?.get(0)?.episodes?.get(0)?.episodeNumber)

        // --- 4. Repair Idempotency Test ---
        // Running repair a second time must repair 0 additional rows and produce identical database state
        val secondRepairResult = repairManager.runRepairIfNeeded(force = true)
        assertEquals(0, secondRepairResult.repairedRowsCount)

        val secondCompleted = fakeDownloadDao.getAllDownloadsSync()
        assertEquals(8, secondCompleted.size)

        val secondTvSeries = DownloadsGrouping.groupTvSeries(secondCompleted)
        assertEquals(2, secondTvSeries.size)
        val secondSv = secondTvSeries.find { it.seriesName == "Silicon Valley" }
        assertEquals(7, secondSv?.episodesCount)
    }

    // --- 5. Grouping With Null seriesId Rows Test ---
    @Test
    fun groupingWithNullSeriesIdRows_neverDropsDownload() {
        val legacy1 = DownloadEntity(
            id = "dl_leg_1",
            mediaId = "raw_download_1",
            seriesId = null,
            seriesName = null,
            seasonNumber = null,
            episodeNumber = null,
            title = "Westworld.S01E03.1080p.mkv",
            downloadUrl = "https://example.com/ww.mkv",
            fileName = "Westworld.S01E03.1080p.mkv",
            filePath = "/storage/emulated/0/ww.mkv",
            status = DownloadStatus.COMPLETED.name,
            mediaType = "tv"
        )
        val legacy2 = DownloadEntity(
            id = "dl_leg_2",
            mediaId = "raw_download_2",
            seriesId = null,
            seriesName = null,
            seasonNumber = null,
            episodeNumber = null,
            title = "Unparseable Mystery Video",
            downloadUrl = "https://example.com/mv.mp4",
            fileName = "mystery.mp4",
            filePath = "/storage/emulated/0/mv.mp4",
            status = DownloadStatus.COMPLETED.name,
            mediaType = "tv"
        )

        val seriesList = DownloadsGrouping.groupTvSeries(listOf(legacy1, legacy2))
        // Both downloads must be preserved across groups
        val totalEpisodes = seriesList.sumOf { it.episodesCount }
        assertEquals(2, totalEpisodes)

        val ww = seriesList.find { it.seriesName == "Westworld" }
        assertNotNull(ww)
        assertEquals(1, ww?.episodesCount)
        assertEquals(1, ww?.seasons?.get(0)?.seasonNumber)

        // Unparseable video placed in Other / Unsorted group, never dropped
        val unsorted = seriesList.find { it.seriesId == SeriesKeyHelper.UNSORTED_KEY || it.seriesName.contains("Unsorted") }
        assertNotNull(unsorted)
        assertEquals(1, unsorted?.episodesCount)
    }

    @Test
    fun pausedDownloadWithPartialFile_remainsPausedAndNotCompletedOnRepair() = runBlocking {
        val tempFile = File.createTempFile("partial_movie", ".mp4")
        try {
            tempFile.writeBytes(ByteArray(10_000)) // 10 KB downloaded out of 100 KB total
            val store = mutableListOf(
                DownloadEntity(
                    id = "paused_dl_1",
                    mediaId = "movie_123",
                    title = "Incomplete Movie",
                    downloadUrl = "https://example.com/movie.mp4",
                    fileName = tempFile.name,
                    filePath = tempFile.absolutePath,
                    fileSizeBytes = 100_000L,
                    downloadedBytes = 10_000L,
                    status = DownloadStatus.PAUSED.name,
                    mediaType = "movie"
                )
            )

            val fakeDao = object : DownloadDao {
                override fun getAllDownloads(): Flow<List<DownloadEntity>> = flowOf(store)
                override fun getActiveDownloads(): Flow<List<DownloadEntity>> = flowOf(store)
                override fun getCompletedDownloads(): Flow<List<DownloadEntity>> = flowOf(emptyList())
                override suspend fun getCompletedDownloadsSync(): List<DownloadEntity> = emptyList()
                override suspend fun getAllDownloadsSync(): List<DownloadEntity> = store.toList()
                override suspend fun getDownloadById(id: String): DownloadEntity? = store.find { it.id == id }
                override suspend fun getDownloadByMediaId(mediaId: String): DownloadEntity? = store.find { it.mediaId == mediaId }
                override suspend fun insertDownload(download: DownloadEntity) { store.add(download) }
                override suspend fun updateDownload(download: DownloadEntity) {
                    val idx = store.indexOfFirst { it.id == download.id }
                    if (idx >= 0) store[idx] = download
                }
                override suspend fun updateDownloads(downloads: List<DownloadEntity>) { downloads.forEach { updateDownload(it) } }
                override suspend fun updateProgress(id: String, downloaded: Long, speed: Long, updatedAt: Long) {}
                override suspend fun updateStatus(id: String, status: String, updatedAt: Long) {}
                override suspend fun markFailed(id: String, status: String, error: String?, updatedAt: Long) {}
                override suspend fun deleteDownload(id: String) { store.removeAll { it.id == id } }
                override suspend fun deleteAllDownloads() { store.clear() }
                override suspend fun getDownloadsBySeriesSync(seriesId: String, seriesPrefix: String): List<DownloadEntity> = emptyList()
                override fun getDownloadsBySeries(seriesId: String, seriesPrefix: String): Flow<List<DownloadEntity>> = flowOf(emptyList())
                override suspend fun updatePlaybackPosition(id: String, position: Long, updatedAt: Long) {}
                override suspend fun updatePlaybackProgress(id: String, position: Long, duration: Long, lastWatched: Long, watchedCompleted: Boolean, updatedAt: Long) {}
                override suspend fun setHideFromContinueWatching(id: String, hide: Boolean, updatedAt: Long) {}
                override suspend fun updateWatchedCompleted(id: String, completed: Boolean, updatedAt: Long) {}
            }

            val fakeOfflineMediaRepo = object : OfflineMediaRepository {
                override val isSavingOfflineDetails = MutableStateFlow(false)
                override fun getOfflineMedia(mediaType: String, tmdbId: Long): Flow<OfflineMediaEntity?> = flowOf(null)
                override suspend fun getOfflineMediaSync(mediaType: String, tmdbId: Long): OfflineMediaEntity? = null
                override fun getOfflineSeasons(seriesKey: String): Flow<List<OfflineSeasonEntity>> = flowOf(emptyList())
                override fun getOfflineEpisodesForSeason(seriesKey: String, seasonNumber: Int): Flow<List<OfflineEpisodeEntity>> = flowOf(emptyList())
                override fun getOfflineEpisodesForSeries(seriesKey: String): Flow<List<OfflineEpisodeEntity>> = flowOf(emptyList())
                override suspend fun saveFor(download: DownloadEntity, forceRefresh: Boolean) {}
                override suspend fun backfillPendingMetadata() {}
                override suspend fun onDownloadDeletedOrCancelled(deletedDownload: DownloadEntity) {}
                override suspend fun deleteSeason(seriesKey: String, seasonNumber: Int) {}
            }

            val fakeOfflineMediaDao = object : OfflineMediaDao {
                override fun getMedia(id: String): Flow<OfflineMediaEntity?> = flowOf(null)
                override suspend fun getMediaSync(id: String): OfflineMediaEntity? = null
                override fun getAllMedia(): Flow<List<OfflineMediaEntity>> = flowOf(emptyList())
                override suspend fun insertMedia(media: OfflineMediaEntity) {}
                override suspend fun deleteMedia(id: String) {}
                override fun getSeasons(seriesKey: String): Flow<List<OfflineSeasonEntity>> = flowOf(emptyList())
                override suspend fun getSeasonsSync(seriesKey: String): List<OfflineSeasonEntity> = emptyList()
                override suspend fun insertSeasons(seasons: List<OfflineSeasonEntity>) {}
                override suspend fun deleteSeasons(seriesKey: String) {}
                override fun getEpisodes(seriesKey: String): Flow<List<OfflineEpisodeEntity>> = flowOf(emptyList())
                override suspend fun getEpisodesSync(seriesKey: String): List<OfflineEpisodeEntity> = emptyList()
                override fun getEpisodesForSeason(seriesKey: String, seasonNumber: Int): Flow<List<OfflineEpisodeEntity>> = flowOf(emptyList())
                override suspend fun insertEpisodes(episodes: List<OfflineEpisodeEntity>) {}
                override suspend fun deleteEpisodes(seriesKey: String) {}
                override suspend fun deleteSeason(seriesKey: String, seasonNumber: Int) {}
                override suspend fun deleteEpisodesForSeason(seriesKey: String, seasonNumber: Int) {}
            }

            val fakeTmdb = object : TmdbRepository {
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

            val repairManager = DownloadsRepairManager(
                context = null,
                downloadDao = fakeDao,
                offlineMediaDao = fakeOfflineMediaDao,
                offlineMediaRepository = fakeOfflineMediaRepo,
                tmdbRepository = fakeTmdb,
                userPreferences = null
            )

            repairManager.runRepairIfNeeded(force = true)

            val updated = store.first()
            assertEquals("Paused download must remain PAUSED", DownloadStatus.PAUSED.name, updated.status)
            assertEquals("File size must remain 100,000", 100_000L, updated.fileSizeBytes)
            assertEquals("Downloaded bytes must match partial file", 10_000L, updated.downloadedBytes)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun incompleteDownloadMarkedCompleted_repairedBackToPausedWithActualBytes() = runBlocking {
        val tempFile = File.createTempFile("corrupted_completed", ".mp4")
        try {
            tempFile.writeBytes(ByteArray(15_000)) // Only 15 KB on disk, but was marked COMPLETED with 150 KB
            val store = mutableListOf(
                DownloadEntity(
                    id = "err_completed_1",
                    mediaId = "movie_999",
                    title = "Truncated Movie",
                    downloadUrl = "https://example.com/truncated.mp4",
                    fileName = tempFile.name,
                    filePath = tempFile.absolutePath,
                    fileSizeBytes = 150_000L,
                    downloadedBytes = 15_000L,
                    status = DownloadStatus.COMPLETED.name, // Erroneously marked completed
                    mediaType = "movie"
                )
            )

            val fakeDao = object : DownloadDao {
                override fun getAllDownloads(): Flow<List<DownloadEntity>> = flowOf(store)
                override fun getActiveDownloads(): Flow<List<DownloadEntity>> = flowOf(emptyList())
                override fun getCompletedDownloads(): Flow<List<DownloadEntity>> = flowOf(store)
                override suspend fun getCompletedDownloadsSync(): List<DownloadEntity> = store.toList()
                override suspend fun getAllDownloadsSync(): List<DownloadEntity> = store.toList()
                override suspend fun getDownloadById(id: String): DownloadEntity? = store.find { it.id == id }
                override suspend fun getDownloadByMediaId(mediaId: String): DownloadEntity? = store.find { it.mediaId == mediaId }
                override suspend fun insertDownload(download: DownloadEntity) { store.add(download) }
                override suspend fun updateDownload(download: DownloadEntity) {
                    val idx = store.indexOfFirst { it.id == download.id }
                    if (idx >= 0) store[idx] = download
                }
                override suspend fun updateDownloads(downloads: List<DownloadEntity>) { downloads.forEach { updateDownload(it) } }
                override suspend fun updateProgress(id: String, downloaded: Long, speed: Long, updatedAt: Long) {}
                override suspend fun updateStatus(id: String, status: String, updatedAt: Long) {}
                override suspend fun markFailed(id: String, status: String, error: String?, updatedAt: Long) {}
                override suspend fun deleteDownload(id: String) { store.removeAll { it.id == id } }
                override suspend fun deleteAllDownloads() { store.clear() }
                override suspend fun getDownloadsBySeriesSync(seriesId: String, seriesPrefix: String): List<DownloadEntity> = emptyList()
                override fun getDownloadsBySeries(seriesId: String, seriesPrefix: String): Flow<List<DownloadEntity>> = flowOf(emptyList())
                override suspend fun updatePlaybackPosition(id: String, position: Long, updatedAt: Long) {}
                override suspend fun updatePlaybackProgress(id: String, position: Long, duration: Long, lastWatched: Long, watchedCompleted: Boolean, updatedAt: Long) {}
                override suspend fun setHideFromContinueWatching(id: String, hide: Boolean, updatedAt: Long) {}
                override suspend fun updateWatchedCompleted(id: String, completed: Boolean, updatedAt: Long) {}
            }

            val fakeOfflineMediaRepo = object : OfflineMediaRepository {
                override val isSavingOfflineDetails = MutableStateFlow(false)
                override fun getOfflineMedia(mediaType: String, tmdbId: Long): Flow<OfflineMediaEntity?> = flowOf(null)
                override suspend fun getOfflineMediaSync(mediaType: String, tmdbId: Long): OfflineMediaEntity? = null
                override fun getOfflineSeasons(seriesKey: String): Flow<List<OfflineSeasonEntity>> = flowOf(emptyList())
                override fun getOfflineEpisodesForSeason(seriesKey: String, seasonNumber: Int): Flow<List<OfflineEpisodeEntity>> = flowOf(emptyList())
                override fun getOfflineEpisodesForSeries(seriesKey: String): Flow<List<OfflineEpisodeEntity>> = flowOf(emptyList())
                override suspend fun saveFor(download: DownloadEntity, forceRefresh: Boolean) {}
                override suspend fun backfillPendingMetadata() {}
                override suspend fun onDownloadDeletedOrCancelled(deletedDownload: DownloadEntity) {}
                override suspend fun deleteSeason(seriesKey: String, seasonNumber: Int) {}
            }

            val fakeOfflineMediaDao = object : OfflineMediaDao {
                override fun getMedia(id: String): Flow<OfflineMediaEntity?> = flowOf(null)
                override suspend fun getMediaSync(id: String): OfflineMediaEntity? = null
                override fun getAllMedia(): Flow<List<OfflineMediaEntity>> = flowOf(emptyList())
                override suspend fun insertMedia(media: OfflineMediaEntity) {}
                override suspend fun deleteMedia(id: String) {}
                override fun getSeasons(seriesKey: String): Flow<List<OfflineSeasonEntity>> = flowOf(emptyList())
                override suspend fun getSeasonsSync(seriesKey: String): List<OfflineSeasonEntity> = emptyList()
                override suspend fun insertSeasons(seasons: List<OfflineSeasonEntity>) {}
                override suspend fun deleteSeasons(seriesKey: String) {}
                override fun getEpisodes(seriesKey: String): Flow<List<OfflineEpisodeEntity>> = flowOf(emptyList())
                override suspend fun getEpisodesSync(seriesKey: String): List<OfflineEpisodeEntity> = emptyList()
                override fun getEpisodesForSeason(seriesKey: String, seasonNumber: Int): Flow<List<OfflineEpisodeEntity>> = flowOf(emptyList())
                override suspend fun insertEpisodes(episodes: List<OfflineEpisodeEntity>) {}
                override suspend fun deleteEpisodes(seriesKey: String) {}
                override suspend fun deleteSeason(seriesKey: String, seasonNumber: Int) {}
                override suspend fun deleteEpisodesForSeason(seriesKey: String, seasonNumber: Int) {}
            }

            val fakeTmdb = object : TmdbRepository {
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

            val repairManager = DownloadsRepairManager(
                context = null,
                downloadDao = fakeDao,
                offlineMediaDao = fakeOfflineMediaDao,
                offlineMediaRepository = fakeOfflineMediaRepo,
                tmdbRepository = fakeTmdb,
                userPreferences = null
            )

            repairManager.runRepairIfNeeded(force = true)

            val updated = store.first()
            assertEquals("Incomplete file erroneously marked COMPLETED must be reverted to PAUSED", DownloadStatus.PAUSED.name, updated.status)
            assertEquals("File size must remain 150,000", 150_000L, updated.fileSizeBytes)
            assertEquals("Downloaded bytes must match actual file size on disk", 15_000L, updated.downloadedBytes)
        } finally {
            tempFile.delete()
        }
    }
}
