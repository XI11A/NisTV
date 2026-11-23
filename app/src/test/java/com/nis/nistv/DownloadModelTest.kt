package com.nis.nistv

import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.DownloadStatus
import com.nis.nistv.data.download.provider.AfdsDownloadProvider
import com.nis.nistv.data.download.provider.DownloadItemCandidate
import com.nis.nistv.data.download.provider.DownloadMediaType
import com.nis.nistv.data.download.provider.GenericUrlDownloadProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DownloadModelTest {

    @Test
    fun downloadEntity_progressAndFormatCalculation() {
        val entity = DownloadEntity(
            id = "test_1",
            mediaId = "101",
            title = "Test Video",
            downloadUrl = "https://example.com/test.mp4",
            fileName = "test.mp4",
            filePath = "/storage/emulated/0/test.mp4",
            fileSizeBytes = 1000L * 1024L * 1024L, // 1000 MB (~1GB)
            downloadedBytes = 500L * 1024L * 1024L, // 500 MB (50%)
            speedBytesPerSec = 5L * 1024L * 1024L, // 5 MB/s
            status = DownloadStatus.DOWNLOADING.name
        )

        assertEquals(0.5f, entity.progressFloat, 0.01f)
        assertEquals(50, entity.progressPercent)
        assertTrue(entity.formattedDownloaded.contains("500.0 MB"))
        assertTrue(entity.formattedTotal.contains("1000.0 MB") || entity.formattedTotal.contains("1.0 GB"))
        assertTrue(entity.formattedSpeed.contains("5.0 MB/s"))
        assertFalse(entity.isTerminal)

        val completed = entity.copy(status = DownloadStatus.COMPLETED.name)
        assertTrue(completed.isTerminal)
    }

    @Test
    fun genericDownloadProvider_resolvesDirectUrls() = runTest {
        val provider = GenericUrlDownloadProvider()
        val candidate = DownloadItemCandidate(
            id = "https://example.com/sample.mp4",
            title = "sample",
            sizeBytes = 2048L,
            quality = "1080p",
            sourceProviderId = "generic"
        )

        val resolved = provider.resolve(candidate)

        assertEquals("https://example.com/sample.mp4", resolved.url)
        assertEquals("sample.mp4", resolved.fileName)
        assertEquals(2048L, resolved.sizeBytes)
    }

    @Test
    fun afdsProvider_cleanMediaTitleRemovesSpecialCharacters() {
        // Strips &, *, @, #, !, ?, :, quotes, hyphens, brackets
        assertEquals("Fast Furious", AfdsDownloadProvider.cleanMediaTitle("Fast & Furious"))
        assertEquals("Spider Man No Way Home", AfdsDownloadProvider.cleanMediaTitle("Spider-Man: No Way Home"))
        assertEquals("Marvels Daredevil", AfdsDownloadProvider.cleanMediaTitle("Marvel's Daredevil"))
        assertEquals("Money Heist Netflix Special", AfdsDownloadProvider.cleanMediaTitle("Money Heist (@Netflix #Special*)"))
        assertEquals("Money Heist", AfdsDownloadProvider.cleanMediaTitle("Money Heist *@#&!"))
        assertEquals("What If", AfdsDownloadProvider.cleanMediaTitle("What If...?!"))
    }

    @Test
    fun afdsProvider_buildSearchQueryCandidates_tvEpisodeConstructions() {
        val candidates = AfdsDownloadProvider.buildSearchQueryCandidates(
            title = "Money Heist",
            year = 2017,
            mediaType = DownloadMediaType.TV,
            seasonNumber = 1,
            episodeNumber = 1,
            originalTitle = "La Casa de Papel"
        )

        // Verify prioritized variations as requested
        assertTrue(candidates.contains("Money Heist S01E01"))
        assertTrue(candidates.contains("Money.Heist.S01E01"))
        assertTrue(candidates.contains("Money Heist Season 1 Episode 1"))
        assertTrue(candidates.contains("Money Heist Season 01 Episode 01"))
        assertTrue(candidates.contains("Money Heist S1 E1"))
        assertTrue(candidates.contains("Money Heist 1x01"))
        // Original title fallbacks
        assertTrue(candidates.contains("La Casa de Papel S01E01"))
        assertTrue(candidates.contains("La.Casa.de.Papel.S01E01"))
        // Season fallbacks
        assertTrue(candidates.contains("Money Heist S01"))
        assertTrue(candidates.contains("Money Heist Season 1"))
        // General show fallback
        assertTrue(candidates.contains("Money Heist"))
    }

    @Test
    fun afdsProvider_buildSearchQueryCandidates_stripsSpecialCharactersFromQueries() {
        val candidates = AfdsDownloadProvider.buildSearchQueryCandidates(
            title = "Fast & Furious*",
            year = 2009,
            mediaType = DownloadMediaType.MOVIE
        )

        for (candidate in candidates) {
            assertFalse("Candidate '$candidate' should not contain '&'", candidate.contains("&"))
            assertFalse("Candidate '$candidate' should not contain '*'", candidate.contains("*"))
            assertFalse("Candidate '$candidate' should not contain '@'", candidate.contains("@"))
        }

        assertTrue(candidates.contains("Fast Furious 2009"))
        assertTrue(candidates.contains("Fast.Furious.2009"))
        assertTrue(candidates.contains("Fast and Furious 2009"))
    }

    @Test
    fun afdsProvider_generateTitleVariants_supportsRomanNumeralsAndArticles() {
        val duneVariants = AfdsDownloadProvider.generateTitleVariants("Dune: Part Two")
        assertTrue(duneVariants.contains("Dune Part Two"))
        assertTrue(duneVariants.contains("Dune Part 2"))

        val batmanVariants = AfdsDownloadProvider.generateTitleVariants("The Batman")
        assertTrue(batmanVariants.contains("The Batman"))
        assertTrue(batmanVariants.contains("Batman"))

        val chapterVariants = AfdsDownloadProvider.generateTitleVariants("John Wick: Chapter 2")
        assertTrue(chapterVariants.contains("John Wick Chapter 2"))
        assertTrue(chapterVariants.contains("John Wick Chapter II"))
    }

    @Test
    fun afdsProvider_candidateComparator_ranksQualityAndSizeCorrectly() {
        val cand4k = DownloadItemCandidate(
            id = "1",
            title = "Movie.2160p.UHD.mkv",
            sizeBytes = 15_000_000_000L,
            quality = "4K UHD",
            sourceProviderId = "afds"
        )
        val cand1080pLarge = DownloadItemCandidate(
            id = "2",
            title = "Movie.1080p.FHD.Remux.mkv",
            sizeBytes = 8_000_000_000L,
            quality = "1080p FHD",
            sourceProviderId = "afds"
        )
        val cand1080pSmall = DownloadItemCandidate(
            id = "3",
            title = "Movie.1080p.WEBRip.mkv",
            sizeBytes = 2_000_000_000L,
            quality = "1080p FHD",
            sourceProviderId = "afds"
        )
        val cand720p = DownloadItemCandidate(
            id = "4",
            title = "Movie.720p.HD.mkv",
            sizeBytes = 1_000_000_000L,
            quality = "720p HD",
            sourceProviderId = "afds"
        )

        val list = listOf(cand720p, cand1080pSmall, cand4k, cand1080pLarge)
        val sorted = list.sortedWith(AfdsDownloadProvider.candidateComparator)

        assertEquals(cand4k, sorted[0])
        assertEquals(cand1080pLarge, sorted[1])
        assertEquals(cand1080pSmall, sorted[2])
        assertEquals(cand720p, sorted[3])
    }

    @Test
    fun afdsProvider_buildEpisodeRegex_validatesAccurately() {
        val s1e1Regex = AfdsDownloadProvider.buildEpisodeRegex(seasonNumber = 1, episodeNumber = 1)
        assertTrue(s1e1Regex.containsMatchIn("Money.Heist.S01E01.1080p.mkv"))
        assertTrue(s1e1Regex.containsMatchIn("Money Heist S1E1 720p.mp4"))
        assertTrue(s1e1Regex.containsMatchIn("Money Heist Season 1 Episode 1.mkv"))
        assertTrue(s1e1Regex.containsMatchIn("Money.Heist.1x01.HDTV.mkv"))
        assertTrue(s1e1Regex.containsMatchIn("Anime Show Episode 1 English.mkv"))
        assertFalse(s1e1Regex.containsMatchIn("Money.Heist.S01E02.1080p.mkv"))

        val s2e3Regex = AfdsDownloadProvider.buildEpisodeRegex(seasonNumber = 2, episodeNumber = 3)
        assertTrue(s2e3Regex.containsMatchIn("Show.S02E03.1080p.mkv"))
        assertTrue(s2e3Regex.containsMatchIn("Show.2x03.720p.mkv"))
        assertFalse(s2e3Regex.containsMatchIn("Show.S01E03.1080p.mkv"))
        assertFalse(s2e3Regex.containsMatchIn("Show.S02E01.1080p.mkv"))
    }

    @Test
    fun afdsProvider_buildSearchQueryTiers_separatesPrioritiesCorrectly() {
        val tiers = AfdsDownloadProvider.buildSearchQueryTiers(
            title = "Money Heist",
            year = 2017,
            mediaType = DownloadMediaType.TV,
            seasonNumber = 1,
            episodeNumber = 1,
            originalTitle = "La Casa de Papel"
        )

        // Tier 1 contains high-yield exact scene releases
        assertTrue(tiers.tier1.contains("Money Heist S01E01"))
        assertTrue(tiers.tier1.contains("Money.Heist.S01E01"))
        assertTrue(tiers.tier1.contains("Money Heist Season 1 Episode 1"))
        assertTrue(tiers.tier1.contains("Money Heist 1x01"))
        // Tier 1 should NOT contain broad show fallbacks or original title variations
        assertFalse(tiers.tier1.contains("Money Heist"))
        assertFalse(tiers.tier1.contains("Money Heist S01"))
        assertFalse(tiers.tier1.contains("La Casa de Papel S01E01"))

        // Tier 2 contains alternative formats & original title variations
        assertTrue(tiers.tier2.contains("Money Heist S1 E1"))
        assertTrue(tiers.tier2.contains("La Casa de Papel S01E01"))

        // Tier 3 contains broad season & show fallbacks
        assertTrue(tiers.tier3.contains("Money Heist S01"))
        assertTrue(tiers.tier3.contains("Money Heist Season 1"))
        assertTrue(tiers.tier3.contains("Money Heist"))
        assertTrue(tiers.tier3.contains("La Casa de Papel"))

        // Backward compatibility: allQueries aggregates all tiers
        val all = tiers.allQueries
        assertTrue(all.contains("Money Heist S01E01"))
        assertTrue(all.contains("Money Heist S1 E1"))
        assertTrue(all.contains("Money Heist"))
    }

    @Test
    fun afdsProvider_isSufficientResult_determinesEarlyExitCorrectly() {
        val c1080 = DownloadItemCandidate(
            id = "1",
            title = "Movie.1080p.mkv",
            sizeBytes = 2_000_000_000L,
            quality = "1080p FHD",
            sourceProviderId = "afds"
        )
        val c720 = DownloadItemCandidate(
            id = "2",
            title = "Movie.720p.mkv",
            sizeBytes = 1_000_000_000L,
            quality = "720p HD",
            sourceProviderId = "afds"
        )
        val cStd1 = DownloadItemCandidate(
            id = "3",
            title = "Movie.mkv",
            sizeBytes = 500_000_000L,
            quality = "Standard",
            sourceProviderId = "afds"
        )
        val cStd2 = DownloadItemCandidate(
            id = "4",
            title = "Movie.cam.mkv",
            sizeBytes = 400_000_000L,
            quality = "Standard",
            sourceProviderId = "afds"
        )

        // 3 items (even standard) is sufficient
        assertTrue(AfdsDownloadProvider.isSufficientResult(listOf(cStd1, cStd2, c720)))

        // 2 items with at least one HD/FHD is sufficient
        assertTrue(AfdsDownloadProvider.isSufficientResult(listOf(c1080, c720)))
        assertTrue(AfdsDownloadProvider.isSufficientResult(listOf(c1080, cStd1)))

        // 2 items with only standard quality is NOT sufficient (allow Tier 2 to run)
        assertFalse(AfdsDownloadProvider.isSufficientResult(listOf(cStd1, cStd2)))

        // 1 item is NOT sufficient (allow Tier 2 to run for more quality options)
        assertFalse(AfdsDownloadProvider.isSufficientResult(listOf(c1080)))

        // Empty list is NOT sufficient
        assertFalse(AfdsDownloadProvider.isSufficientResult(emptyList()))
    }

    @Test
    fun afdsProvider_searchCache_storesAndExpiresCorrectly() {
        val cache = AfdsDownloadProvider.SearchCache(maxEntries = 2, ttlMs = 1000L)
        val items = listOf(
            DownloadItemCandidate(
                id = "1",
                title = "File.mkv",
                sizeBytes = 1000L,
                quality = "1080p FHD",
                sourceProviderId = "afds"
            )
        )

        cache.put("key1", items)
        assertEquals(items, cache.get("key1"))

        // Overfill capacity (maxEntries = 2)
        cache.put("key2", items)
        cache.put("key3", items)
        // key1 should be evicted due to LRU limit
        assertNull(cache.get("key1"))
        assertNotNull(cache.get("key2"))
        assertNotNull(cache.get("key3"))

        cache.clear()
        assertNull(cache.get("key2"))
    }
}
