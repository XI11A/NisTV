package com.nis.nistv

import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.DownloadStatus
import com.nis.nistv.core.database.entity.OfflineMediaEntity
import com.nis.nistv.feature.downloads.DownloadsGrouping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadsGroupingTest {

    @Test
    fun groupMovies_filtersOutTvAndKeepsCompletedMovies() {
        val movie1 = DownloadEntity(
            id = "dl_m1",
            mediaId = "100",
            title = "Inception",
            downloadUrl = "https://example.com/inception.mp4",
            fileName = "inception.mp4",
            filePath = "/storage/emulated/0/inception.mp4",
            fileSizeBytes = 2_000_000_000L,
            downloadedBytes = 2_000_000_000L,
            status = DownloadStatus.COMPLETED.name,
            mediaType = "movie",
            createdAt = 1000L
        )
        val movie2 = DownloadEntity(
            id = "dl_m2",
            mediaId = "101",
            title = "Interstellar",
            downloadUrl = "https://example.com/interstellar.mp4",
            fileName = "interstellar.mp4",
            filePath = "/storage/emulated/0/interstellar.mp4",
            fileSizeBytes = 3_000_000_000L,
            downloadedBytes = 3_000_000_000L,
            status = DownloadStatus.COMPLETED.name,
            mediaType = "movie",
            createdAt = 2000L
        )
        val tvEpisode = DownloadEntity(
            id = "dl_tv1",
            mediaId = "500_S01E01",
            seriesId = "500",
            seriesName = "Breaking Bad",
            seasonNumber = 1,
            episodeNumber = 1,
            title = "Breaking Bad S01E01",
            downloadUrl = "https://example.com/bb.mp4",
            fileName = "bb.mp4",
            filePath = "/storage/emulated/0/bb.mp4",
            fileSizeBytes = 1_000_000_000L,
            downloadedBytes = 1_000_000_000L,
            status = DownloadStatus.COMPLETED.name,
            mediaType = "tv"
        )
        val activeMovie = DownloadEntity(
            id = "dl_m3",
            mediaId = "102",
            title = "Tenet",
            downloadUrl = "https://example.com/tenet.mp4",
            fileName = "tenet.mp4",
            filePath = "/storage/emulated/0/tenet.mp4",
            fileSizeBytes = 2_000_000_000L,
            downloadedBytes = 500_000_000L,
            status = DownloadStatus.DOWNLOADING.name,
            mediaType = "movie"
        )

        val movies = DownloadsGrouping.groupMovies(
            completed = listOf(movie1, movie2, tvEpisode, activeMovie)
        )

        // Only completed movies should be present, sorted by createdAt desc
        assertEquals(2, movies.size)
        assertEquals("dl_m2", movies[0].id) // Interstellar (createdAt 2000)
        assertEquals("dl_m1", movies[1].id) // Inception (createdAt 1000)
    }

    @Test
    fun groupTvSeries_groupsBySeriesThenSeasonThenEpisode_withAggregates() {
        val ep1 = DownloadEntity(
            id = "dl_ep1",
            mediaId = "1399_S01E01",
            seriesId = "1399",
            seriesName = "Game of Thrones",
            seasonNumber = 1,
            episodeNumber = 1,
            episodeName = "Winter Is Coming",
            title = "Game of Thrones S01E01",
            downloadUrl = "https://example.com/got1.mp4",
            fileName = "got1.mp4",
            filePath = "/storage/emulated/0/got1.mp4",
            fileSizeBytes = 1_200_000_000L,
            downloadedBytes = 1_200_000_000L,
            status = DownloadStatus.COMPLETED.name,
            mediaType = "tv"
        )
        val ep3 = DownloadEntity(
            id = "dl_ep3",
            mediaId = "1399_S01E03",
            seriesId = "1399",
            seriesName = "Game of Thrones",
            seasonNumber = 1,
            episodeNumber = 3,
            episodeName = "Lord Snow",
            title = "Game of Thrones S01E03",
            downloadUrl = "https://example.com/got3.mp4",
            fileName = "got3.mp4",
            filePath = "/storage/emulated/0/got3.mp4",
            fileSizeBytes = 1_400_000_000L,
            downloadedBytes = 1_400_000_000L,
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
            episodeName = "The Kingsroad",
            title = "Game of Thrones S01E02",
            downloadUrl = "https://example.com/got2.mp4",
            fileName = "got2.mp4",
            filePath = "/storage/emulated/0/got2.mp4",
            fileSizeBytes = 1_300_000_000L,
            downloadedBytes = 1_300_000_000L,
            status = DownloadStatus.COMPLETED.name,
            mediaType = "tv"
        )
        val s2ep1 = DownloadEntity(
            id = "dl_s2ep1",
            mediaId = "1399_S02E01",
            seriesId = "1399",
            seriesName = "Game of Thrones",
            seasonNumber = 2,
            episodeNumber = 1,
            episodeName = "The North Remembers",
            title = "Game of Thrones S02E01",
            downloadUrl = "https://example.com/got21.mp4",
            fileName = "got21.mp4",
            filePath = "/storage/emulated/0/got21.mp4",
            fileSizeBytes = 1_500_000_000L,
            downloadedBytes = 1_500_000_000L,
            status = DownloadStatus.COMPLETED.name,
            mediaType = "tv"
        )

        val seriesList = DownloadsGrouping.groupTvSeries(
            completed = listOf(ep3, s2ep1, ep1, ep2),
            offlineMediaMap = emptyMap()
        )

        assertEquals(1, seriesList.size)
        val got = seriesList[0]
        assertEquals("1399", got.seriesId)
        assertEquals("Game of Thrones", got.seriesName)
        assertEquals(4, got.episodesCount)
        assertEquals(2, got.seasonsCount)
        assertEquals(5_400_000_000L, got.totalSizeBytes)

        // Verify seasons are sorted
        assertEquals(2, got.seasons.size)
        val s1 = got.seasons[0]
        assertEquals(1, s1.seasonNumber)
        assertEquals(3, s1.episodes.size)

        // Verify episodes in Season 1 are sorted by episodeNumber asc
        assertEquals(1, s1.episodes[0].episodeNumber)
        assertEquals(2, s1.episodes[1].episodeNumber)
        assertEquals(3, s1.episodes[2].episodeNumber)

        val s2 = got.seasons[1]
        assertEquals(2, s2.seasonNumber)
        assertEquals(1, s2.episodes.size)
        assertEquals(1, s2.episodes[0].episodeNumber)
    }

    @Test
    fun groupTvSeries_handlesNullAndLegacyFieldsGracefullyWithoutCrashing() {
        val legacyRow1 = DownloadEntity(
            id = "dl_legacy1",
            mediaId = "999_S01E02",
            seriesId = null,
            seriesName = null,
            seasonNumber = null,
            episodeNumber = null,
            episodeName = null,
            title = "Stranger Things S01E02",
            downloadUrl = "https://example.com/st.mp4",
            fileName = "st.mp4",
            filePath = "/storage/emulated/0/st.mp4",
            fileSizeBytes = 1_000_000_000L,
            downloadedBytes = 1_000_000_000L,
            status = DownloadStatus.COMPLETED.name,
            mediaType = "tv"
        )
        val legacyRow2 = DownloadEntity(
            id = "dl_legacy2",
            mediaId = "unknown_item",
            seriesId = null,
            seriesName = null,
            seasonNumber = null,
            episodeNumber = null,
            episodeName = null,
            title = "Mysterious Show",
            downloadUrl = "https://example.com/ms.mp4",
            fileName = "ms.mp4",
            filePath = "/storage/emulated/0/ms.mp4",
            fileSizeBytes = 800_000_000L,
            downloadedBytes = 800_000_000L,
            status = DownloadStatus.COMPLETED.name,
            mediaType = "tv"
        )

        val seriesList = DownloadsGrouping.groupTvSeries(
            completed = listOf(legacyRow1, legacyRow2),
            offlineMediaMap = emptyMap()
        )

        assertEquals(2, seriesList.size)
        // Check that series 1 extracted seriesId = 999 and title parsed
        val st = seriesList.find { it.seriesId == "999" }
        assertNotNull(st)
        assertEquals("Stranger Things", st?.seriesName)
        assertEquals(1, st?.episodesCount)
        // Episode placed under season 1
        assertEquals(1, st?.seasons?.get(0)?.seasonNumber)

        val ms = seriesList.find { it.seriesId == "unknown" }
        assertNotNull(ms)
        assertEquals("Mysterious Show", ms?.seriesName)
        assertEquals(1, ms?.episodesCount)
    }

    @Test
    fun groupTvSeries_filtersOutNonCompletedDownloads() {
        val completedEp = DownloadEntity(
            id = "dl_comp",
            mediaId = "200_S01E01",
            seriesId = "200",
            seriesName = "Fargo",
            seasonNumber = 1,
            episodeNumber = 1,
            title = "Fargo S01E01",
            downloadUrl = "https://example.com/fargo.mp4",
            fileName = "fargo.mp4",
            filePath = "/storage/emulated/0/fargo.mp4",
            fileSizeBytes = 1_000_000_000L,
            downloadedBytes = 1_000_000_000L,
            status = DownloadStatus.COMPLETED.name,
            mediaType = "tv"
        )
        val pausedEp = DownloadEntity(
            id = "dl_paused",
            mediaId = "200_S01E02",
            seriesId = "200",
            seriesName = "Fargo",
            seasonNumber = 1,
            episodeNumber = 2,
            title = "Fargo S01E02",
            downloadUrl = "https://example.com/fargo2.mp4",
            fileName = "fargo2.mp4",
            filePath = "/storage/emulated/0/fargo2.mp4",
            fileSizeBytes = 1_000_000_000L,
            downloadedBytes = 300_000_000L,
            status = DownloadStatus.PAUSED.name,
            mediaType = "tv"
        )

        val seriesList = DownloadsGrouping.groupTvSeries(
            completed = listOf(completedEp, pausedEp),
            offlineMediaMap = emptyMap()
        )

        assertEquals(1, seriesList.size)
        assertEquals(1, seriesList[0].episodesCount)
        assertEquals("dl_comp", seriesList[0].seasons[0].episodes[0].id)
    }
}
