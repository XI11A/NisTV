package com.nis.nistv.data.playlist

import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.StreamSourceEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale

data class ParsedPlaylistResult(
    val channels: List<ChannelEntity>,
    val streams: List<StreamSourceEntity>,
    val importedCount: Int,
    val skippedCount: Int
) {
    val summary: String
        get() = "$importedCount imported, $skippedCount skipped"
}

object M3uParser {

    private const val DEFAULT_MAX_BYTES = 50L * 1024L * 1024L // 50 MB
    private const val DEFAULT_MAX_ENTRIES = 100_000

    private val ATTRIBUTE_REGEX = Regex("""([a-zA-Z0-9_-]+)\s*=\s*"([^"]*)"""")

    fun maskUrl(url: String): String {
        return url.replace(Regex("(?i)(token|key|auth|pass|password|secret|hash)=([^&]+)")) { match ->
            "${match.groupValues[1]}=***"
        }
    }

    fun parse(
        inputStream: InputStream,
        playlistId: String,
        playlistName: String = "Custom Playlist",
        maxBytes: Long = DEFAULT_MAX_BYTES,
        maxEntries: Int = DEFAULT_MAX_ENTRIES
    ): ParsedPlaylistResult {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        var totalBytesRead = 0L
        var entriesCount = 0
        var skippedCount = 0

        // Maps channelKey -> ChannelEntity and list of stream URLs
        val channelMap = linkedMapOf<String, ChannelEntity>()
        val streamMap = linkedMapOf<String, MutableList<String>>()

        var currentExtInf: String? = null

        while (true) {
            val line = reader.readLine() ?: break
            totalBytesRead += line.toByteArray(Charsets.UTF_8).size + 1
            if (totalBytesRead > maxBytes || entriesCount >= maxEntries) {
                break
            }

            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("#EXTINF", ignoreCase = true)) {
                currentExtInf = trimmed
                continue
            }

            if (trimmed.startsWith("#")) {
                // Other M3U directive (#EXTM3U, #EXTVLCOPT, #EXTGRP, etc.) - ignore
                continue
            }

            // Line is expected to be a stream URL
            if (currentExtInf != null) {
                val streamUrl = trimmed
                if (!isValidStreamUrl(streamUrl)) {
                    skippedCount++
                    currentExtInf = null
                    continue
                }

                val extInf = currentExtInf
                currentExtInf = null

                val parsedChannel = parseExtInf(extInf, playlistId, playlistName)
                if (parsedChannel == null) {
                    skippedCount++
                    continue
                }

                val channelKey = parsedChannel.id
                if (!channelMap.containsKey(channelKey)) {
                    channelMap[channelKey] = parsedChannel
                    streamMap[channelKey] = mutableListOf(streamUrl)
                    entriesCount++
                } else {
                    // Group multiple URLs for the same channel as failover streams
                    val existingStreams = streamMap[channelKey]
                    if (existingStreams != null && !existingStreams.contains(streamUrl)) {
                        existingStreams.add(streamUrl)
                        entriesCount++
                    }
                }
            }
        }

        // Flatten channels and streams
        val channelsList = channelMap.values.toList()
        val streamsList = mutableListOf<StreamSourceEntity>()

        for (channel in channelsList) {
            val urls = streamMap[channel.id] ?: emptyList()
            for ((index, url) in urls.withIndex()) {
                streamsList.add(
                    StreamSourceEntity(
                        channelId = channel.id,
                        url = url,
                        priority = index,
                        quality = channel.resolution.ifEmpty { "HD" }
                    )
                )
            }
        }

        return ParsedPlaylistResult(
            channels = channelsList,
            streams = streamsList,
            importedCount = channelsList.size,
            skippedCount = skippedCount
        )
    }

    private fun isValidStreamUrl(url: String): Boolean {
        val lower = url.lowercase(Locale.ROOT)
        return (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("rtsp://"))
                && !url.contains(" ")
    }

    private fun findTitleSeparatorIndex(line: String): Int {
        var inQuotes = false
        for (i in line.indices) {
            val c = line[i]
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                return i
            }
        }
        return -1
    }

    private fun parseExtInf(
        line: String,
        playlistId: String,
        playlistName: String
    ): ChannelEntity? {
        val commaIndex = findTitleSeparatorIndex(line)
        val rawTitle = if (commaIndex != -1 && commaIndex < line.length - 1) {
            line.substring(commaIndex + 1).trim()
        } else {
            ""
        }

        val attributesPart = if (commaIndex != -1) line.substring(0, commaIndex) else line

        var tvgId: String? = null
        var tvgName: String? = null
        var tvgLogo: String? = null
        var groupTitle: String? = null
        var tvgLanguage: String? = null

        ATTRIBUTE_REGEX.findAll(attributesPart).forEach { match ->
            val key = match.groupValues[1].lowercase(Locale.ROOT)
            val value = match.groupValues[2].trim()
            when (key) {
                "tvg-id" -> tvgId = value
                "tvg-name" -> tvgName = value
                "tvg-logo" -> tvgLogo = value
                "group-title" -> groupTitle = value
                "tvg-language" -> tvgLanguage = value
            }
        }

        val name = when {
            rawTitle.isNotBlank() -> rawTitle
            !tvgName.isNullOrBlank() -> tvgName!!
            !tvgId.isNullOrBlank() -> tvgId!!
            else -> "Channel"
        }

        val cleanName = cleanChannelName(name)
        val category = groupTitle?.ifBlank { "General" } ?: "General"
        val language = tvgLanguage?.ifBlank { "Unknown" } ?: "Unknown"

        // Generate collision-proof ID: custom_{playlistId}_{stableHash}
        val keyToHash = (tvgId?.takeIf { it.isNotBlank() } ?: "$cleanName|$category").lowercase(Locale.ROOT)
        val hash = (keyToHash.hashCode() and 0x7FFFFFFF).toString(16)
        val customChannelId = "custom_${playlistId}_$hash"

        return ChannelEntity(
            id = customChannelId,
            name = name,
            cleanName = cleanName,
            altNames = tvgName.orEmpty(),
            country = "XX",
            network = playlistName,
            website = "",
            language = language,
            category = category,
            logoUrl = tvgLogo.orEmpty(),
            resolution = if (name.contains("4K", ignoreCase = true)) "4K" else if (name.contains("FHD") || name.contains("1080")) "1080p" else "HD",
            isNsfw = category.contains("adult", ignoreCase = true) || category.contains("xxx", ignoreCase = true),
            labels = playlistName,
            playlistId = playlistId
        )
    }

    private fun cleanChannelName(name: String): String {
        return name
            .replace(Regex("""(?i)\[.*?\]|\(.*?\)|fhd|uhd|4k|hd|sd|hevc|raw|1080p|720p"""), "")
            .replace(Regex("""[|_.-]+"""), " ")
            .trim()
            .ifEmpty { name.trim() }
    }
}
