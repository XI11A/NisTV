package com.nis.nistv.data.download.offline

/**
 * Shared series-key helper used by the downloader, repair manager,
 * offline media repository, and ViewModels.
 *
 * Ensures series keys are strictly formatted as "tv_<tmdbId>" when a TMDB ID is known,
 * or "tv_<normalized_name>" as a stable deterministic fallback when offline/unresolved.
 */
object SeriesKeyHelper {

    const val PREFIX = "tv_"
    const val UNSORTED_KEY = "tv_unsorted"

    /**
     * Normalizes a series name into a safe, deterministic slug for fallback keys.
     * e.g. "Silicon Valley" -> "silicon_valley", "Stranger Things" -> "stranger_things".
     */
    fun normalizeForSeriesKey(name: String): String {
        val slug = name.lowercase()
            .replace(Regex("""[^a-z0-9]+"""), "_")
            .trim('_')
        return slug.ifBlank { "unknown" }
    }

    /**
     * Builds a canonical key from a numeric TMDB ID.
     */
    fun buildSeriesKey(tmdbId: Long?): String {
        return if (tmdbId != null && tmdbId > 0) {
            "$PREFIX$tmdbId"
        } else {
            UNSORTED_KEY
        }
    }

    /**
     * Builds a deterministic fallback series key from a series name.
     */
    fun buildFallbackSeriesKey(seriesName: String): String {
        return "$PREFIX${normalizeForSeriesKey(seriesName)}"
    }

    /**
     * Resolves the canonical series key given any combination of seriesId, seriesName, or tmdbId.
     */
    fun getSeriesKey(seriesId: String?, seriesName: String?, tmdbId: Long? = null): String {
        if (tmdbId != null && tmdbId > 0) {
            return "$PREFIX$tmdbId"
        }

        if (!seriesId.isNullOrBlank()) {
            val numericId = seriesId.toLongOrNull()
            if (numericId != null && numericId > 0) {
                return "$PREFIX$numericId"
            }
            if (seriesId.startsWith(PREFIX, ignoreCase = true)) {
                return seriesId.lowercase()
            }
            return "$PREFIX${normalizeForSeriesKey(seriesId)}"
        }

        if (!seriesName.isNullOrBlank()) {
            return buildFallbackSeriesKey(seriesName)
        }

        return UNSORTED_KEY
    }

    /**
     * Returns true if the key is a fallback key (not backed by a numeric TMDB ID).
     */
    fun isFallbackKey(seriesKey: String): Boolean {
        val payload = seriesKey.removePrefix(PREFIX)
        return payload.toLongOrNull() == null
    }

    /**
     * Extracts a numeric TMDB ID from a series key if present.
     */
    fun extractTmdbId(seriesKey: String?): Long? {
        if (seriesKey.isNullOrBlank()) return null
        val payload = if (seriesKey.startsWith(PREFIX, ignoreCase = true)) {
            seriesKey.substring(PREFIX.length)
        } else {
            seriesKey
        }
        return payload.toLongOrNull()
    }
}
