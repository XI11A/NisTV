package com.nis.nistv.data.health

import java.security.MessageDigest

object StreamHealthKeyHelper {

    /**
     * Generates a stable, canonical stream key from a stream URL.
     * Strips volatile query tokens if present and produces a SHA-256 hash.
     */
    fun computeStreamKey(url: String): String {
        val canonical = url.trim()
            .substringBefore("?") // Strip transient tokens/auth params for stable health tracking
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(canonical.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            canonical.hashCode().toString()
        }
    }
}
