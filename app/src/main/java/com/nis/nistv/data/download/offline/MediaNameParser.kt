package com.nis.nistv.data.download.offline

import java.util.Locale

/**
 * Result of parsing a media filename or release title.
 */
data class ParsedMediaInfo(
    val seriesName: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val year: Int? = null,
    val isTv: Boolean = false,
    val movieTitle: String? = null
)

/**
 * Robust parser for extracting series name, season number, episode number,
 * and year from filenames, torrent release names, or arbitrary titles.
 *
 * Supports:
 * - Case-insensitive patterns (S06E05, s01e01, s06.e05, s6e5)
 * - 1x05, 01x05 format
 * - "Season 1 Episode 5", "Season.01.Episode.05"
 * - "Ep 05", "Ep. 05", "Episode 5"
 * - Dotted, dashed, or underscored names (e.g. "Silicon.Valley", "stranger_things")
 * - Stripping trailing release/codec tags (1080p, 720p, WEBRip, WEB-DL, BluRay, x264, x265, HEVC, etc.)
 */
object MediaNameParser {

    private val EXTENSIONS = listOf("mkv", "mp4", "avi", "webm", "ts", "m4v")

    // Pattern 1: S01E02, S1E2, Season 1 Episode 2, s01.e02
    private val PATTERN_S_E = Regex(
        """^(.*?)[._\s-]+(?:s|season)[._\s]*(\d{1,2})[._\s-]*(?:e|ep|episode)[._\s]*(\d{1,3})(?:[._\s-]+.*)?$""",
        RegexOption.IGNORE_CASE
    )

    // Pattern 2: 1x05, 01x05
    private val PATTERN_X = Regex(
        """^(.*?)[._\s-]+(\d{1,2})x(\d{1,3})(?:[._\s-]+.*)?$""",
        RegexOption.IGNORE_CASE
    )

    // Pattern 3: Season only + Episode only
    private val PATTERN_SEASON_ONLY = Regex(
        """^(.*?)[._\s-]+(?:s|season)[._\s]*(\d{1,2})(?:[._\s-]+.*)?$""",
        RegexOption.IGNORE_CASE
    )

    // Pattern 4: Ep 05, Episode 5 (defaults to Season 1)
    private val PATTERN_EP_ONLY = Regex(
        """^(.*?)[._\s-]+(?:ep|episode)[._\s]*(\d{1,3})(?:[._\s-]+.*)?$""",
        RegexOption.IGNORE_CASE
    )

    // Year pattern: 19xx or 20xx
    private val PATTERN_YEAR = Regex(
        """[._\s-]+(19\d\d|20\d\d)(?:[._\s-]+|$)"""
    )

    // Trailing quality / release tags to clean up from movie titles
    private val TAG_CLEANUP = Regex(
        """(?i)[._\s-]+(1080p|720p|4k|2160p|480p|webrip|web-dl|bluray|bdrip|brrip|hdtv|dvdrip|x264|x265|hevc|h\.264|h\.265|aac|dts|ddp5\.1|dd5\.1|ac3|repack|proper|amzn|nf|dsnp|atvp).*$"""
    )

    /**
     * Parses a raw string (filename, title, or filepath).
     */
    fun parse(rawInput: String?): ParsedMediaInfo {
        if (rawInput.isNullOrBlank()) {
            return ParsedMediaInfo(seriesName = null, seasonNumber = null, episodeNumber = null, isTv = false)
        }

        // Clean path to file name if a path was passed
        val fileName = rawInput.substringAfterLast('/').substringAfterLast('\\')

        // Strip known video extension
        var clean = fileName
        for (ext in EXTENSIONS) {
            if (clean.endsWith(".$ext", ignoreCase = true)) {
                clean = clean.substring(0, clean.length - (ext.length + 1))
                break
            }
        }

        // 1. Try Pattern SxxExx
        val matchSE = PATTERN_S_E.matchEntire(clean)
        if (matchSE != null) {
            val rawTitle = matchSE.groupValues[1]
            val season = matchSE.groupValues[2].toIntOrNull() ?: 1
            val episode = matchSE.groupValues[3].toIntOrNull() ?: 1
            val (seriesTitle, year) = extractTitleAndYear(rawTitle)
            return ParsedMediaInfo(
                seriesName = seriesTitle,
                seasonNumber = season,
                episodeNumber = episode,
                year = year,
                isTv = true
            )
        }

        // 2. Try Pattern 1x05
        val matchX = PATTERN_X.matchEntire(clean)
        if (matchX != null) {
            val rawTitle = matchX.groupValues[1]
            val season = matchX.groupValues[2].toIntOrNull() ?: 1
            val episode = matchX.groupValues[3].toIntOrNull() ?: 1
            val (seriesTitle, year) = extractTitleAndYear(rawTitle)
            return ParsedMediaInfo(
                seriesName = seriesTitle,
                seasonNumber = season,
                episodeNumber = episode,
                year = year,
                isTv = true
            )
        }

        // 3. Try Pattern Ep only (e.g. Ep.05)
        val matchEp = PATTERN_EP_ONLY.matchEntire(clean)
        if (matchEp != null) {
            val rawTitle = matchEp.groupValues[1]
            val episode = matchEp.groupValues[2].toIntOrNull() ?: 1
            val (seriesTitle, year) = extractTitleAndYear(rawTitle)
            return ParsedMediaInfo(
                seriesName = seriesTitle,
                seasonNumber = 1,
                episodeNumber = episode,
                year = year,
                isTv = true
            )
        }

        // 4. Try Pattern Season only
        val matchSeason = PATTERN_SEASON_ONLY.matchEntire(clean)
        if (matchSeason != null) {
            val rawTitle = matchSeason.groupValues[1]
            val season = matchSeason.groupValues[2].toIntOrNull() ?: 1
            val (seriesTitle, year) = extractTitleAndYear(rawTitle)
            return ParsedMediaInfo(
                seriesName = seriesTitle,
                seasonNumber = season,
                episodeNumber = 1,
                year = year,
                isTv = true
            )
        }

        // 5. Not recognized as TV: parse as movie
        val (movieTitle, movieYear) = extractMovieTitleAndYear(clean)
        return ParsedMediaInfo(
            seriesName = null,
            seasonNumber = null,
            episodeNumber = null,
            year = movieYear,
            isTv = false,
            movieTitle = movieTitle
        )
    }

    private fun extractTitleAndYear(rawTitle: String): Pair<String, Int?> {
        val yearMatch = PATTERN_YEAR.find(rawTitle)
        val year = yearMatch?.groupValues?.get(1)?.toIntOrNull()

        val titleWithoutYear = if (yearMatch != null) {
            rawTitle.substring(0, yearMatch.range.first)
        } else {
            rawTitle
        }

        val cleanedTitle = normalizeTitle(titleWithoutYear)
        return Pair(cleanedTitle, year)
    }

    private fun extractMovieTitleAndYear(rawTitle: String): Pair<String, Int?> {
        val yearMatch = PATTERN_YEAR.find(rawTitle)
        val year = yearMatch?.groupValues?.get(1)?.toIntOrNull()

        val titlePart = if (yearMatch != null) {
            rawTitle.substring(0, yearMatch.range.first)
        } else {
            TAG_CLEANUP.replace(rawTitle, "")
        }

        val cleanedTitle = normalizeTitle(titlePart)
        return Pair(cleanedTitle, year)
    }

    /**
     * Replaces dots and underscores with spaces, cleans punctuation, and applies Title Case.
     */
    fun normalizeTitle(raw: String): String {
        val withSpaces = raw.replace('.', ' ')
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (withSpaces.isBlank()) return "Unknown Title"

        return withSpaces.split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                // Preserve acronyms or Roman numerals like "US", "II", "III"
                if (word.all { it.isUpperCase() } && word.length <= 4) {
                    word
                } else {
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
                }
            }
    }
}
