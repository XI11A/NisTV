package com.nis.nistv.data.radar

object EpisodeKeyHelper {

    /**
     * Formats season and episode integers into a canonical episode key: e.g. "S02E05".
     */
    fun formatEpisodeKey(season: Int, episode: Int): String {
        return "S%02dE%02d".format(java.util.Locale.US, season.coerceAtLeast(0), episode.coerceAtLeast(0))
    }

    /**
     * Alias for [formatEpisodeKey] to construct canonical episode keys.
     */
    fun buildEpisodeKey(season: Int, episode: Int): String = formatEpisodeKey(season, episode)

    /**
     * Parses an episode key string ("S02E05", "s2e5", "2x05") into (season, episode) Pair.
     */
    fun parseEpisodeKey(key: String?): Pair<Int, Int>? {
        if (key.isNullOrBlank()) return null
        val trimmed = key.trim().uppercase()

        // Match SxxExx
        val sRegex = Regex("""S(\d+)E(\d+)""")
        val sMatch = sRegex.find(trimmed)
        if (sMatch != null) {
            val season = sMatch.groupValues[1].toIntOrNull() ?: return null
            val episode = sMatch.groupValues[2].toIntOrNull() ?: return null
            return Pair(season, episode)
        }

        // Match 1x05
        val xRegex = Regex("""(\d+)X(\d+)""")
        val xMatch = xRegex.find(trimmed)
        if (xMatch != null) {
            val season = xMatch.groupValues[1].toIntOrNull() ?: return null
            val episode = xMatch.groupValues[2].toIntOrNull() ?: return null
            return Pair(season, episode)
        }

        return null
    }

    /**
     * Determines whether [candidateKey] represents an episode newer than [baseKey].
     * If [baseKey] is null or invalid, any valid candidate is considered newer.
     */
    fun isNewerEpisode(candidateKey: String?, baseKey: String?): Boolean {
        val candidate = parseEpisodeKey(candidateKey) ?: return false
        val base = parseEpisodeKey(baseKey) ?: return true

        return when {
            candidate.first > base.first -> true
            candidate.first < base.first -> false
            else -> candidate.second > base.second
        }
    }
}
