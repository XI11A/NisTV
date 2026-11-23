package com.nis.nistv.data.download.provider

/**
 * AFDS Download Provider Adapter
 *
 * Adapted from AFDS-Android (https://github.com/CloudflareHackers/AFDS-Android)
 * Licensed under the MIT License - Copyright (c) 2024 CloudflareHackers
 *
 * This adapter ports only the necessary search and link resolution logic
 * into NisTV's pluggable DownloadProvider interface.
 */

import android.util.Log
import com.nis.nistv.core.preferences.UserPreferences
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.Collections

class AfdsDownloadProvider(
    private val okHttpClient: OkHttpClient,
    private val userPreferences: UserPreferences
) : DownloadProvider {

    override val id: String = "afds"
    override val name: String = "AFDS (HashHackers Archive)"

    /**
     * Tiered query container for phased execution.
     */
    data class SearchQueryTiers(
        val tier1: List<String> = emptyList(),
        val tier2: List<String> = emptyList(),
        val tier3: List<String> = emptyList()
    ) {
        val allQueries: List<String>
            get() = (tier1 + tier2 + tier3).distinct().filter { it.isNotBlank() }
    }

    /**
     * In-memory LRU cache with TTL for AFDS search results to avoid redundant network round-trips.
     */
    class SearchCache(
        private val maxEntries: Int = 100,
        private val ttlMs: Long = 5 * 60 * 1000L // 5 minutes TTL
    ) {
        private data class CacheEntry(val timestamp: Long, val results: List<DownloadItemCandidate>)
        private val map = object : LinkedHashMap<String, CacheEntry>(maxEntries, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean {
                return size > maxEntries
            }
        }
        private val lock = Any()

        fun get(key: String): List<DownloadItemCandidate>? = synchronized(lock) {
            val entry = map[key] ?: return null
            if (System.currentTimeMillis() - entry.timestamp > ttlMs) {
                map.remove(key)
                return null
            }
            return entry.results
        }

        fun put(key: String, results: List<DownloadItemCandidate>) = synchronized(lock) {
            map[key] = CacheEntry(System.currentTimeMillis(), results)
        }

        fun clear() = synchronized(lock) {
            map.clear()
        }

        val size: Int
            get() = synchronized(lock) { map.size }
    }

    companion object {
        private const val BASE_URL = "https://tga-hd.api.hashhackers.com"
        private const val TAG = "AfdsProvider"
        val SEARCH_CATEGORIES = listOf("files", "mix_media_files")

        val searchCache = SearchCache()

        private val qualityRank = mapOf(
            "4K UHD" to 5,
            "1080p FHD" to 4,
            "720p HD" to 3,
            "480p" to 2,
            "Standard" to 1
        )

        val candidateComparator = Comparator<DownloadItemCandidate> { a, b ->
            val rankA = qualityRank[a.quality] ?: 1
            val rankB = qualityRank[b.quality] ?: 1
            if (rankA != rankB) {
                rankB.compareTo(rankA) // higher quality first
            } else {
                b.sizeBytes.compareTo(a.sizeBytes) // larger file / higher bitrate first
            }
        }

        /**
         * Determines if search results are sufficient to stop early without exhausting further query tiers.
         * Returns true if we have >= 3 candidates, or >= 2 candidates with at least one high-definition source.
         */
        fun isSufficientResult(candidates: List<DownloadItemCandidate>): Boolean {
            if (candidates.size >= 3) return true
            val hasHighQuality = candidates.any {
                it.quality == "4K UHD" || it.quality == "1080p FHD" || it.quality == "720p HD"
            }
            return candidates.size >= 2 && hasHighQuality
        }

        /**
         * Cleans special characters from a movie/series title.
         * Strips characters like '&', '*', '@', '#', '$', '%', '^', '!', '?', ':', ';',
         * quotes, brackets, slashes, dashes, etc.
         */
        fun cleanMediaTitle(raw: String): String {
            if (raw.isBlank()) return ""
            return raw
                .replace("'", "")
                .replace("`", "")
                .replace("’", "")
                .replace(Regex("[&*@#$%^!?:\",.\\-+=\\~|/\\\\()\\[\\]{}<>;_]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        /**
         * Generates normalized variants of the given title string:
         * - Ampersand swapped (& -> and)
         * - Roman numerals swapped (Part 1 <-> Part I, Chapter 2 <-> Chapter II, etc.)
         * - Stripping leading "The " / "A "
         * - Subtitle split on colon/hyphen (e.g. "Spider-Man: No Way Home" -> "Spider Man No Way Home", "No Way Home")
         */
        fun generateTitleVariants(rawTitle: String): List<String> {
            val variants = LinkedHashSet<String>()
            val clean = cleanMediaTitle(rawTitle)
            if (clean.isBlank()) return emptyList()

            variants.add(clean)

            // Ampersand variations
            if (rawTitle.contains("&")) {
                val withAnd = cleanMediaTitle(rawTitle.replace("&", " and "))
                if (withAnd.isNotBlank()) variants.add(withAnd)
            }
            if (clean.contains(" and ", ignoreCase = true)) {
                val withoutAnd = clean.replace(Regex("(?i)\\band\\b"), "").replace(Regex("\\s+"), " ").trim()
                if (withoutAnd.isNotBlank()) variants.add(withoutAnd)
            }

            // Subtitle extraction (e.g. "Mission: Impossible - Fallout" -> "Fallout", "Mission Impossible")
            if (rawTitle.contains(":") || rawTitle.contains(" - ")) {
                val subtitle = rawTitle.substringAfter(":").substringAfter(" - ")
                val cleanSubtitle = cleanMediaTitle(subtitle)
                if (cleanSubtitle.length >= 4 && !variants.contains(cleanSubtitle)) {
                    variants.add(cleanSubtitle)
                }
            }

            // Roman numeral and number word variations (Part Two <-> Part 2 <-> Part II)
            val romanReplacements = listOf(
                Regex("(?i)\\bPart\\s+One\\b") to "Part 1",
                Regex("(?i)\\bPart\\s+Two\\b") to "Part 2",
                Regex("(?i)\\bPart\\s+Three\\b") to "Part 3",
                Regex("(?i)\\bPart\\s+Four\\b") to "Part 4",
                Regex("(?i)\\bPart\\s+1\\b") to "Part I",
                Regex("(?i)\\bPart\\s+I\\b") to "Part 1",
                Regex("(?i)\\bPart\\s+2\\b") to "Part II",
                Regex("(?i)\\bPart\\s+II\\b") to "Part 2",
                Regex("(?i)\\bPart\\s+3\\b") to "Part III",
                Regex("(?i)\\bPart\\s+III\\b") to "Part 3",
                Regex("(?i)\\bPart\\s+4\\b") to "Part IV",
                Regex("(?i)\\bPart\\s+IV\\b") to "Part 4",
                Regex("(?i)\\bChapter\\s+One\\b") to "Chapter 1",
                Regex("(?i)\\bChapter\\s+Two\\b") to "Chapter 2",
                Regex("(?i)\\bChapter\\s+Three\\b") to "Chapter 3",
                Regex("(?i)\\bChapter\\s+1\\b") to "Chapter I",
                Regex("(?i)\\bChapter\\s+I\\b") to "Chapter 1",
                Regex("(?i)\\bChapter\\s+2\\b") to "Chapter II",
                Regex("(?i)\\bChapter\\s+II\\b") to "Chapter 2",
                Regex("(?i)\\bChapter\\s+3\\b") to "Chapter III",
                Regex("(?i)\\bChapter\\s+III\\b") to "Chapter 3",
                Regex("(?i)\\bVol\\s+1\\b") to "Vol I",
                Regex("(?i)\\bVol\\s+I\\b") to "Vol 1",
                Regex("(?i)\\bVol\\s+2\\b") to "Vol II",
                Regex("(?i)\\bVol\\s+II\\b") to "Vol 2",
                Regex("(?i)\\b2\\b") to "II",
                Regex("(?i)\\bII\\b") to "2",
                Regex("(?i)\\b3\\b") to "III",
                Regex("(?i)\\bIII\\b") to "3"
            )

            // Multiple passes to catch chained conversions e.g. "Part Two" -> "Part 2" -> "Part II"
            for (pass in 0..1) {
                val snapshot = variants.toList()
                for (v in snapshot) {
                    for ((regex, replacement) in romanReplacements) {
                        if (regex.containsMatchIn(v)) {
                            val replaced = v.replace(regex, replacement).trim()
                            if (replaced.isNotBlank()) variants.add(replaced)
                        }
                    }
                }
            }

            // Leading article removal ("The Batman" -> "Batman")
            if (clean.startsWith("The ", ignoreCase = true) && clean.length > 5) {
                val withoutThe = clean.substring(4).trim()
                if (withoutThe.isNotBlank()) variants.add(withoutThe)
            }

            return variants.toList()
        }

        /**
         * Builds regex pattern to strictly validate episode matching in file names.
         */
        fun buildEpisodeRegex(seasonNumber: Int, episodeNumber: Int): Regex {
            val s = seasonNumber
            val e = episodeNumber
            val pattern = if (s > 1) {
                "(?i)(S0?${s}[\\s._-]*E0?${e}\\b|\\b0?${s}x0?${e}\\b|Season[\\s._-]*0?${s}[^a-z0-9]*(?:Episode|Ep)[\\s._-]*0?${e}\\b)"
            } else {
                "(?i)(S0?${s}[\\s._-]*E0?${e}\\b|\\b0?${s}x0?${e}\\b|Season[\\s._-]*0?${s}[^a-z0-9]*(?:Episode|Ep)[\\s._-]*0?${e}\\b|\\bEpisode[\\s._-]*0?${e}\\b|\\bEP0?${e}\\b)"
            }
            return Regex(pattern)
        }

        /**
         * Generates prioritized, tiered query strings for AFDS search:
         * - Tier 1: Highest-yield standard scene formats (S01E01, Season 1 Episode 1, etc.)
         * - Tier 2: Additional release variants, dot-notations, short ep forms, and original title variations
         * - Tier 3: Broad season-level and show-level fallbacks (filtered client-side by episode regex)
         */
        fun buildSearchQueryTiers(
            title: String,
            year: Int? = null,
            mediaType: DownloadMediaType = DownloadMediaType.MOVIE,
            seasonNumber: Int? = null,
            episodeNumber: Int? = null,
            originalTitle: String? = null
        ): SearchQueryTiers {
            val titleVariants = generateTitleVariants(title)
            if (titleVariants.isEmpty()) return SearchQueryTiers()

            val origVariants = originalTitle?.let { orig ->
                generateTitleVariants(orig).filter { origV ->
                    !titleVariants.any { it.equals(origV, ignoreCase = true) }
                }
            } ?: emptyList()

            val tier1 = mutableListOf<String>()
            val tier2 = mutableListOf<String>()
            val tier3 = mutableListOf<String>()

            when {
                // Case 1: Specific TV Episode (e.g. Money Heist Season 1 Episode 1)
                mediaType == DownloadMediaType.TV && seasonNumber != null && episodeNumber != null -> {
                    val s02 = String.format(java.util.Locale.US, "%02d", seasonNumber)
                    val e02 = String.format(java.util.Locale.US, "%02d", episodeNumber)
                    val s1 = seasonNumber.toString()
                    val e1 = episodeNumber.toString()

                    // Tier 1: Highest-yield standard scene releases and primary full-word tags
                    for (base in titleVariants) {
                        val dot = base.replace(' ', '.')
                        tier1.add("$base S${s02}E${e02}")
                        tier1.add("$dot.S${s02}E${e02}")
                        tier1.add("$base Season $s1 Episode $e1")
                        tier1.add("$base Season $s02 Episode $e02")
                        tier1.add("$base ${s1}x${e02}")
                    }

                    // Tier 2: Additional release variants, dot-notations, short ep forms & original title
                    for (base in titleVariants) {
                        val dot = base.replace(' ', '.')
                        tier2.add("$base S${s02}.E${e02}")
                        tier2.add("$dot.S${s02}.E${e02}")
                        tier2.add("$dot.Season.$s1.Episode.$e1")
                        tier2.add("$dot.Season.$s02.Episode.$e02")
                        tier2.add("$base Season $s1 Ep $e1")
                        tier2.add("$base S${s02} Ep${e02}")
                        tier2.add("$base S$s1 E$e1")
                        tier2.add("$base S${s02} E${e02}")
                        tier2.add("$dot.${s1}x${e02}")
                        tier2.add("$base ${s02}x${e02}")
                        tier2.add("$base S${s1}E${e02}")
                        // Episode-only tags
                        tier2.add("$base Episode $e1")
                        tier2.add("$base Episode $e02")
                        tier2.add("$base EP$e02")
                        tier2.add("$base E${e02}")
                        tier2.add("$dot.E${e02}")
                    }

                    // Original title episode variations in Tier 2
                    for (cleanOrig in origVariants) {
                        val dotOrig = cleanOrig.replace(' ', '.')
                        tier2.add("$cleanOrig S${s02}E${e02}")
                        tier2.add("$dotOrig.S${s02}E${e02}")
                        tier2.add("$cleanOrig Season $s1 Episode $e1")
                        tier2.add("$cleanOrig S$s1 E$e1")
                        tier2.add("$cleanOrig ${s1}x${e02}")
                        tier2.add("$cleanOrig Episode $e1")
                    }

                    // Tier 3: Season-level and show-level broad fallbacks (filtered by episode regex)
                    for (base in titleVariants) {
                        val dot = base.replace(' ', '.')
                        tier3.add("$base S${s02}")
                        tier3.add("$base Season $s1")
                        tier3.add("$dot.S${s02}")
                        tier3.add("$base Season $s02")
                        tier3.add(base)
                    }
                    for (cleanOrig in origVariants) {
                        tier3.add("$cleanOrig S${s02}")
                        tier3.add("$cleanOrig Season $s1")
                        tier3.add(cleanOrig)
                    }
                }

                // Case 2: TV Show / Season (without specific episode)
                mediaType == DownloadMediaType.TV -> {
                    if (seasonNumber != null) {
                        val s02 = String.format(java.util.Locale.US, "%02d", seasonNumber)
                        val s1 = seasonNumber.toString()
                        for (base in titleVariants) {
                            val dot = base.replace(' ', '.')
                            tier1.add("$base Season $s1")
                            tier1.add("$base S${s02}")
                            tier1.add("$dot.S${s02}")

                            tier2.add("$base Season $s02")
                            tier2.add("$base Season $s1 Complete")
                            tier2.add("$base S${s02} Complete")
                        }
                        for (cleanOrig in origVariants) {
                            tier2.add("$cleanOrig Season $s1")
                            tier2.add("$cleanOrig S${s02}")
                        }
                    }
                    for (base in titleVariants) {
                        val dot = base.replace(' ', '.')
                        tier3.add(base)
                        tier3.add(dot)
                    }
                    for (cleanOrig in origVariants) {
                        tier3.add(cleanOrig)
                        tier3.add(cleanOrig.replace(' ', '.'))
                    }
                }

                // Case 3: Movie
                else -> {
                    if (year != null && year > 1900) {
                        for (base in titleVariants) {
                            val dot = base.replace(' ', '.')
                            tier1.add("$base $year")
                            tier1.add("$dot.$year")
                            tier2.add("$base ${year - 1}")
                            tier2.add("$base ${year + 1}")
                        }
                        for (cleanOrig in origVariants) {
                            val dotOrig = cleanOrig.replace(' ', '.')
                            tier2.add("$cleanOrig $year")
                            tier2.add("$dotOrig.$year")
                        }
                    }
                    for (base in titleVariants) {
                        val dot = base.replace(' ', '.')
                        tier3.add(base)
                        tier3.add(dot)
                    }
                    for (cleanOrig in origVariants) {
                        tier3.add(cleanOrig)
                        tier3.add(cleanOrig.replace(' ', '.'))
                    }
                }
            }

            return SearchQueryTiers(
                tier1 = tier1.distinct().filter { it.isNotBlank() },
                tier2 = tier2.distinct().filter { it.isNotBlank() },
                tier3 = tier3.distinct().filter { it.isNotBlank() }
            )
        }

        /**
         * Backward-compatible candidate list returning all queries across all tiers.
         */
        fun buildSearchQueryCandidates(
            title: String,
            year: Int? = null,
            mediaType: DownloadMediaType = DownloadMediaType.MOVIE,
            seasonNumber: Int? = null,
            episodeNumber: Int? = null,
            originalTitle: String? = null
        ): List<String> {
            val tiers = buildSearchQueryTiers(
                title = title,
                year = year,
                mediaType = mediaType,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                originalTitle = originalTitle
            )
            return (tiers.tier1 + tiers.tier2 + tiers.tier3).distinct().filter { it.isNotBlank() }
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    private data class AfdsSearchResponse(
        val files: List<AfdsFileItem> = emptyList()
    )

    @Serializable
    private data class AfdsFileItem(
        val id: JsonElement? = null,
        @SerialName("file_id") val fileId: String? = null,
        @SerialName("file_name") val fileName: String? = null,
        @SerialName("file_size") val fileSize: JsonElement? = null,
        @SerialName("source_table") val sourceTable: String? = null,
        val category: String? = null,
        val caption: String? = null
    ) {
        val effectiveId: String
            get() = fileId ?: when (id) {
                is JsonPrimitive -> id.content
                else -> ""
            }

        val effectiveName: String
            get() = fileName ?: caption ?: "Media File"

        val effectiveTable: String
            get() = sourceTable ?: category ?: "files"

        val fileSizeBytes: Long
            get() = when (fileSize) {
                is JsonPrimitive -> fileSize.longOrNull ?: fileSize.content.toLongOrNull() ?: 0L
                else -> 0L
            }
    }

    @Serializable
    private data class AfdsGenLinkResponse(
        val success: Boolean = false,
        val url: String? = null,
        val error: String? = null
    )

    /**
     * Executes AFDS candidate searches in phased tiers with early termination:
     * - Checks fast in-memory cache first.
     * - Phase 1: High-precision Tier 1 queries on 'files' category. Early-exits if sufficient.
     * - Phase 2: Variant Tier 2 queries on 'files' (and 'mix_media_files' if 0 so far). Early-exits if candidates found.
     * - Phase 3: Deep broad show/season fallback (strictly filtered by episode regex) only if still 0 candidates.
     */
    override fun searchFlow(
        title: String,
        year: Int?,
        mediaType: DownloadMediaType,
        seasonNumber: Int?,
        episodeNumber: Int?,
        originalTitle: String?
    ): Flow<List<DownloadItemCandidate>> = channelFlow {
        val cacheKey = "$mediaType|$title|${year ?: ""}|${seasonNumber ?: ""}|${episodeNumber ?: ""}|${originalTitle ?: ""}"
        val cached = searchCache.get(cacheKey)
        if (!cached.isNullOrEmpty()) {
            Log.d(TAG, "Cache HIT for '$cacheKey' (${cached.size} items)")
            send(cached)
            return@channelFlow
        }

        val token = userPreferences.afdsAuthToken.firstOrNull()?.trim().orEmpty()
        val tiers = buildSearchQueryTiers(
            title = title,
            year = year,
            mediaType = mediaType,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            originalTitle = originalTitle
        )

        if (tiers.tier1.isEmpty() && tiers.tier2.isEmpty() && tiers.tier3.isEmpty()) {
            send(emptyList())
            return@channelFlow
        }

        Log.d(TAG, "Tiered search starting for '$title': Tier1=${tiers.tier1.size}, Tier2=${tiers.tier2.size}, Tier3=${tiers.tier3.size}")

        val episodeRegex = if (seasonNumber != null && episodeNumber != null) {
            buildEpisodeRegex(seasonNumber, episodeNumber)
        } else null

        val combinedResults = mutableListOf<DownloadItemCandidate>()
        val seenIds = mutableSetOf<String>()
        val seenSignatures = mutableSetOf<String>()
        val resultsMutex = Mutex()

        suspend fun processAndDeduplicate(candidates: List<DownloadItemCandidate>): Int {
            return resultsMutex.withLock {
                var addedCount = 0
                for (item in candidates) {
                    val sig = "${item.title.lowercase().trim()}_${item.sizeBytes}"
                    if (seenIds.add(item.id) && seenSignatures.add(sig)) {
                        combinedResults.add(item)
                        addedCount++
                    }
                }
                addedCount
            }
        }

        // --- Phase 1: Tier 1 (High precision scene queries on primary 'files' category) ---
        if (tiers.tier1.isNotEmpty()) {
            val tier1Results = executeBatchQueries(tiers.tier1, listOf("files"), token, episodeRegex)
            if (processAndDeduplicate(tier1Results) > 0) {
                val sorted = combinedResults.toList().sortedWith(candidateComparator)
                send(sorted)
            }

            if (isSufficientResult(combinedResults)) {
                Log.d(TAG, "Early exit after Tier 1 for '$title' (${combinedResults.size} candidates found)")
                searchCache.put(cacheKey, combinedResults.toList().sortedWith(candidateComparator))
                return@channelFlow
            }
        }

        // --- Phase 2: Tier 2 (Alternative variants, original title, and secondary category) ---
        if (tiers.tier2.isNotEmpty()) {
            val categories = if (combinedResults.isEmpty()) {
                listOf("files", "mix_media_files")
            } else {
                listOf("files")
            }
            val tier2Results = executeBatchQueries(tiers.tier2, categories, token, episodeRegex)
            if (processAndDeduplicate(tier2Results) > 0) {
                val sorted = combinedResults.toList().sortedWith(candidateComparator)
                send(sorted)
            }

            if (combinedResults.isNotEmpty()) {
                Log.d(TAG, "Early exit after Tier 2 for '$title' (${combinedResults.size} candidates found)")
                searchCache.put(cacheKey, combinedResults.toList().sortedWith(candidateComparator))
                return@channelFlow
            }
        }

        // --- Phase 3: Tier 3 (Deep fallback - season/show broad query only if 0 results so far) ---
        if (combinedResults.isEmpty() && tiers.tier3.isNotEmpty()) {
            Log.d(TAG, "Executing Tier 3 broad fallbacks for '$title' (${tiers.tier3.size} queries)")
            val tier3Results = executeBatchQueries(tiers.tier3, listOf("files", "mix_media_files"), token, episodeRegex)
            if (processAndDeduplicate(tier3Results) > 0) {
                val sorted = combinedResults.toList().sortedWith(candidateComparator)
                send(sorted)
                searchCache.put(cacheKey, sorted)
                return@channelFlow
            }
        }

        // If nothing was found across all tiers
        if (combinedResults.isEmpty()) {
            send(emptyList())
        } else {
            val sorted = combinedResults.toList().sortedWith(candidateComparator)
            searchCache.put(cacheKey, sorted)
        }
    }

    override suspend fun search(
        title: String,
        year: Int?,
        mediaType: DownloadMediaType,
        seasonNumber: Int?,
        episodeNumber: Int?,
        originalTitle: String?
    ): List<DownloadItemCandidate> {
        var finalResult: List<DownloadItemCandidate> = emptyList()
        searchFlow(title, year, mediaType, seasonNumber, episodeNumber, originalTitle).collect {
            finalResult = it
        }
        return finalResult
    }

    /**
     * Executes queries concurrently bounded by a Semaphore to avoid overwhelming the server.
     */
    private suspend fun executeBatchQueries(
        queries: List<String>,
        categories: List<String>,
        token: String,
        episodeRegex: Regex?,
        maxParallel: Int = 4
    ): List<DownloadItemCandidate> = coroutineScope {
        if (queries.isEmpty() || categories.isEmpty()) return@coroutineScope emptyList()

        val semaphore = Semaphore(maxParallel)
        val tasks = queries.flatMap { q -> categories.map { cat -> q to cat } }

        tasks.map { (query, category) ->
            async {
                semaphore.withPermit {
                    try {
                        val results = queryAfdsCategory(query, category, token)
                        if (episodeRegex != null) {
                            val isExactEpisodeQuery = query.contains("E", ignoreCase = true) ||
                                    query.contains("Episode", ignoreCase = true) ||
                                    query.contains("x", ignoreCase = true)

                            results.filter { candidate ->
                                episodeRegex.containsMatchIn(candidate.title) ||
                                        (isExactEpisodeQuery && candidate.title.contains(query, ignoreCase = true))
                            }
                        } else {
                            results
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error querying AFDS category '$category' for '$query': ${e.message}")
                        emptyList()
                    }
                }
            }
        }.awaitAll().flatten()
    }

    /**
     * Asynchronously queries an AFDS category using cancellable OkHttp calls.
     */
    private suspend fun queryAfdsCategory(query: String, category: String, token: String): List<DownloadItemCandidate> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = "$BASE_URL/$category/search?q=$encodedQuery&page=1"

                val requestBuilder = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")

                if (token.isNotBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }

                val request = requestBuilder.build()
                val call = okHttpClient.newCall(request)

                suspendCancellableCoroutine<List<DownloadItemCandidate>> { cont ->
                    cont.invokeOnCancellation {
                        call.cancel()
                    }
                    call.enqueue(object : okhttp3.Callback {
                        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                            response.use { resp ->
                                if (!resp.isSuccessful) {
                                    Log.w(TAG, "AFDS search failed ($category) with code ${resp.code} for query: $query")
                                    cont.resume(emptyList())
                                    return
                                }
                                try {
                                    val bodyStr = resp.body?.string().orEmpty()
                                    val parsed = json.decodeFromString<AfdsSearchResponse>(bodyStr)
                                    val items = parsed.files
                                        .filter { it.effectiveId.isNotBlank() }
                                        .map { file ->
                                            val fileName = file.effectiveName
                                            val quality = parseQualityFromFileName(fileName)
                                            DownloadItemCandidate(
                                                id = file.effectiveId,
                                                title = fileName,
                                                sizeBytes = file.fileSizeBytes,
                                                quality = quality,
                                                extraInfo = "tbl:${file.effectiveTable}",
                                                sourceProviderId = id
                                            )
                                        }
                                    cont.resume(items)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing AFDS response ($category, query: $query): ${e.message}")
                                    cont.resume(emptyList())
                                }
                            }
                        }

                        override fun onFailure(call: okhttp3.Call, e: IOException) {
                            if (cont.isCancelled) return
                            Log.e(TAG, "Network error querying AFDS ($category, query: $query): ${e.message}")
                            cont.resume(emptyList())
                        }
                    })
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in queryAfdsCategory ($category, query: $query): ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun resolve(candidate: DownloadItemCandidate): ResolvedDownloadSource = withContext(Dispatchers.IO) {
        val token = userPreferences.afdsAuthToken.firstOrNull()?.trim().orEmpty()
        val table = if (candidate.extraInfo.startsWith("tbl:")) {
            candidate.extraInfo.substringAfter("tbl:")
        } else {
            "files"
        }
        val url = "$BASE_URL/genLink?type=$table&id=${candidate.id}"

        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")

        if (token.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (response.code == 429) {
                throw IOException("Daily download limit reached on AFDS server. Try again tomorrow.")
            }
            if (response.code == 401) {
                throw IOException("AFDS session expired. Please sign in again.")
            }
            if (!response.isSuccessful) {
                throw IOException("AFDS genLink returned HTTP ${response.code}")
            }

            val body = response.body?.string().orEmpty()
            val result = json.decodeFromString<AfdsGenLinkResponse>(body)

            if (!result.success || result.url.isNullOrBlank()) {
                throw IOException(result.error ?: "Failed to generate download link from AFDS")
            }

            userPreferences.incrementAfdsLinksUsed()

            ResolvedDownloadSource(
                url = result.url,
                fileName = sanitizeFileName(candidate.title),
                sizeBytes = candidate.sizeBytes
            )
        }
    }

    /**
     * Regenerates a fresh download link for an existing AFDS file ID and table.
     * Used when resuming an active, paused, or failed download where the signed link has expired.
     */
    suspend fun regenerateDownloadUrl(fileId: String, sourceTable: String = "files"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = userPreferences.afdsAuthToken.firstOrNull()?.trim().orEmpty()
            val table = if (sourceTable.startsWith("tbl:")) sourceTable.substringAfter("tbl:") else sourceTable
            val url = "$BASE_URL/genLink?type=$table&id=$fileId"

            val requestBuilder = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")

            if (token.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("AFDS genLink returned HTTP ${response.code}"))
                }

                val body = response.body?.string().orEmpty()
                val result = json.decodeFromString<AfdsGenLinkResponse>(body)

                if (!result.success || result.url.isNullOrBlank()) {
                    return@withContext Result.failure(IOException(result.error ?: "Failed to regenerate download link from AFDS"))
                }

                Result.success(result.url)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error regenerating AFDS link for fileId '$fileId': ${e.message}")
            Result.failure(e)
        }
    }

    private fun parseQualityFromFileName(name: String): String {
        val upper = name.uppercase()
        return when {
            upper.contains("2160P") || upper.contains("4K") -> "4K UHD"
            upper.contains("1080P") || upper.contains("FHD") -> "1080p FHD"
            upper.contains("720P") || upper.contains("HD") -> "720p HD"
            upper.contains("480P") || upper.contains("SD") -> "480p"
            else -> "Standard"
        }
    }

    private fun sanitizeFileName(name: String): String {
        val clean = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return if (clean.contains(".")) clean else "$clean.mp4"
    }
}
