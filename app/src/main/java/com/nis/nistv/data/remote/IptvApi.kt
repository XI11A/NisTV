package com.nis.nistv.data.remote

import android.content.Context
import android.util.JsonReader
import com.nis.nistv.core.database.entity.ChannelEntity
import com.nis.nistv.core.database.entity.StreamSourceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

data class FetchResult(
    val isNotModified: Boolean,
    val etag: String?,
    val lastModified: String?,
    val file: File?
)

class IptvApi(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        const val BASE_URL = "https://iptv-org.github.io/api/"
        const val CHANNELS_ENDPOINT = "channels.json"
        const val STREAMS_ENDPOINT = "streams.json"
        const val CATEGORIES_ENDPOINT = "categories.json"
        const val LANGUAGES_ENDPOINT = "languages.json"
        const val BLOCKLIST_ENDPOINT = "blocklist.json"
        const val LOGOS_ENDPOINT = "logos.json"
    }

    suspend fun fetchResource(
        endpoint: String,
        etag: String? = null,
        lastModified: String? = null
    ): FetchResult = withContext(Dispatchers.IO) {
        val url = "$BASE_URL$endpoint"
        val requestBuilder = Request.Builder().url(url)
        etag?.let { requestBuilder.header("If-None-Match", it) }
        lastModified?.let { requestBuilder.header("If-Modified-Since", it) }

        val request = requestBuilder.build()
        val response = client.newCall(request).execute()

        if (response.code == 304) {
            response.close()
            return@withContext FetchResult(
                isNotModified = true,
                etag = etag,
                lastModified = lastModified,
                file = null
            )
        }

        if (!response.isSuccessful) {
            val code = response.code
            val message = response.message
            response.close()
            throw IOException("HTTP request for $endpoint failed with status $code: $message")
        }

        val body = response.body ?: throw IOException("Empty response body for $endpoint")
        val tempFile = File.createTempFile("iptv_${endpoint.replace('/', '_')}_", ".tmp", context.cacheDir)

        try {
            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        coroutineContext.ensureActive()
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }

            FetchResult(
                isNotModified = false,
                etag = response.header("ETag"),
                lastModified = response.header("Last-Modified"),
                file = tempFile
            )
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        } finally {
            response.close()
        }
    }

    suspend fun parseCategories(file: File): Map<String, String> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, String>()
        FileInputStream(file).bufferedReader(Charsets.UTF_8).use { br ->
            JsonReader(br).use { reader ->
                if (reader.peek() != android.util.JsonToken.BEGIN_ARRAY) return@withContext result
                reader.beginArray()
                while (reader.hasNext()) {
                    coroutineContext.ensureActive()
                    reader.beginObject()
                    var id: String? = null
                    var name: String? = null
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "id" -> id = reader.nextStringSafe()
                            "name" -> name = reader.nextStringSafe()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    if (!id.isNullOrBlank() && !name.isNullOrBlank()) {
                        result[id] = name
                    }
                }
                reader.endArray()
            }
        }
        result
    }

    suspend fun parseLanguages(file: File): Map<String, String> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, String>()
        FileInputStream(file).bufferedReader(Charsets.UTF_8).use { br ->
            JsonReader(br).use { reader ->
                if (reader.peek() != android.util.JsonToken.BEGIN_ARRAY) return@withContext result
                reader.beginArray()
                while (reader.hasNext()) {
                    coroutineContext.ensureActive()
                    reader.beginObject()
                    var code: String? = null
                    var name: String? = null
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "code" -> code = reader.nextStringSafe()
                            "name" -> name = reader.nextStringSafe()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    if (!code.isNullOrBlank() && !name.isNullOrBlank()) {
                        result[code] = name
                    }
                }
                reader.endArray()
            }
        }
        result
    }

    suspend fun parseBlocklist(file: File): Set<String> = withContext(Dispatchers.IO) {
        val result = mutableSetOf<String>()
        FileInputStream(file).bufferedReader(Charsets.UTF_8).use { br ->
            JsonReader(br).use { reader ->
                if (reader.peek() != android.util.JsonToken.BEGIN_ARRAY) return@withContext result
                reader.beginArray()
                while (reader.hasNext()) {
                    coroutineContext.ensureActive()
                    reader.beginObject()
                    var channel: String? = null
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "channel" -> channel = reader.nextStringSafe()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    channel?.let { result.add(it) }
                }
                reader.endArray()
            }
        }
        result
    }

    private data class LogoCandidate(
        val url: String,
        val format: String,
        val inUse: Boolean,
        val score: Int
    )

    suspend fun parseLogos(file: File, allowedChannelIds: Set<String>): Map<String, String> = withContext(Dispatchers.IO) {
        val candidates = mutableMapOf<String, LogoCandidate>()
        FileInputStream(file).bufferedReader(Charsets.UTF_8).use { br ->
            JsonReader(br).use { reader ->
                if (reader.peek() != android.util.JsonToken.BEGIN_ARRAY) return@withContext emptyMap()
                reader.beginArray()
                while (reader.hasNext()) {
                    coroutineContext.ensureActive()
                    reader.beginObject()
                    var channel: String? = null
                    var url: String? = null
                    var format: String = "PNG"
                    var inUse: Boolean = false
                    var width = 0
                    var height = 0

                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "channel" -> channel = reader.nextStringSafe()
                            "url" -> url = reader.nextStringSafe()
                            "format" -> format = reader.nextStringSafe() ?: "PNG"
                            "in_use" -> inUse = reader.nextBooleanSafe()
                            "width" -> width = reader.nextIntSafe()
                            "height" -> height = reader.nextIntSafe()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()

                    if (channel != null && allowedChannelIds.contains(channel) && !url.isNullOrBlank()) {
                        // Ranking: Raster preferred over SVG, in_use = true preferred, dimensions larger
                        val isRaster = !format.equals("SVG", ignoreCase = true)
                        var score = 0
                        if (isRaster) score += 1000
                        if (inUse) score += 500
                        score += (width * height) / 1000

                        val existing = candidates[channel]
                        if (existing == null || score > existing.score) {
                            candidates[channel] = LogoCandidate(url, format, inUse, score)
                        }
                    }
                }
                reader.endArray()
            }
        }
        candidates.mapValues { it.value.url }
    }

    suspend fun parseChannels(
        file: File,
        allowedCountries: Set<String> = emptySet(),
        blocklist: Set<String> = emptySet(),
        categoryMap: Map<String, String> = emptyMap(),
        languageMap: Map<String, String> = emptyMap(),
        logosMap: Map<String, String> = emptyMap()
    ): List<ChannelEntity> = withContext(Dispatchers.IO) {
        val channels = mutableListOf<ChannelEntity>()
        FileInputStream(file).bufferedReader(Charsets.UTF_8).use { br ->
            JsonReader(br).use { reader ->
                if (reader.peek() != android.util.JsonToken.BEGIN_ARRAY) return@withContext emptyList()
                reader.beginArray()
                while (reader.hasNext()) {
                    coroutineContext.ensureActive()
                    reader.beginObject()

                    var id: String? = null
                    var name: String? = null
                    var country: String? = null
                    var isNsfw: Boolean = false
                    var network: String? = null
                    var website: String? = null
                    val altNames = mutableListOf<String>()
                    val categories = mutableListOf<String>()
                    val languages = mutableListOf<String>()

                    while (reader.hasNext()) {
                        when (val field = reader.nextName()) {
                            "id" -> id = reader.nextStringSafe()
                            "name" -> name = reader.nextStringSafe()
                            "country" -> country = reader.nextStringSafe()
                            "is_nsfw" -> isNsfw = reader.nextBooleanSafe()
                            "network" -> network = reader.nextStringSafe()
                            "website" -> website = reader.nextStringSafe()
                            "alt_names" -> reader.readStringList(altNames)
                            "categories" -> reader.readStringList(categories)
                            "languages" -> reader.readStringList(languages)
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()

                    // Filter criteria: if allowedCountries is empty, accept all regions
                    if (id != null && !name.isNullOrBlank() &&
                        (allowedCountries.isEmpty() || allowedCountries.contains(country)) &&
                        !isNsfw &&
                        !blocklist.contains(id)
                    ) {
                        val mappedCategory = mapToTaxonomy(categories, categoryMap)
                        val mappedLanguage = mapToLanguage(languages, languageMap)
                        val logoUrl = logosMap[id] ?: ""

                        channels.add(
                            ChannelEntity(
                                id = id,
                                name = name,
                                cleanName = cleanChannelName(name),
                                altNames = altNames.joinToString(", "),
                                country = country ?: "",
                                network = network ?: "",
                                website = website ?: "",
                                language = mappedLanguage,
                                category = mappedCategory,
                                logoUrl = logoUrl,
                                resolution = "HD",
                                isNsfw = false,
                                labels = "",
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
                reader.endArray()
            }
        }
        channels
    }

    suspend fun parseStreams(
        file: File,
        validChannelIds: Set<String>
    ): Map<String, List<StreamSourceEntity>> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, MutableList<StreamSourceEntity>>()
        FileInputStream(file).bufferedReader(Charsets.UTF_8).use { br ->
            JsonReader(br).use { reader ->
                if (reader.peek() != android.util.JsonToken.BEGIN_ARRAY) return@withContext emptyMap()
                reader.beginArray()
                while (reader.hasNext()) {
                    coroutineContext.ensureActive()
                    reader.beginObject()

                    var channel: String? = null
                    var url: String? = null
                    var quality: String? = null
                    var userAgent: String? = null
                    var referrer: String? = null
                    val labels = mutableListOf<String>()

                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "channel" -> channel = reader.nextStringSafe()
                            "url" -> url = reader.nextStringSafe()
                            "quality" -> quality = reader.nextStringSafe()
                            "user_agent" -> userAgent = reader.nextStringSafe()
                            "referrer" -> referrer = reader.nextStringSafe()
                            "labels" -> reader.readStringList(labels)
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()

                    if (channel != null && validChannelIds.contains(channel) && !url.isNullOrBlank()) {
                        // Calculate priority
                        var priority = 0
                        val isGeoBlocked = labels.any { it.contains("Geo-blocked", ignoreCase = true) }
                        val isNot247 = labels.any { it.contains("Not 24/7", ignoreCase = true) }

                        if (isGeoBlocked) priority += 10
                        if (isNot247) priority += 5

                        when (quality?.lowercase()) {
                            "1080p", "fhd" -> priority += 0
                            "720p", "hd" -> priority += 1
                            else -> priority += 2
                        }

                        val entity = StreamSourceEntity(
                            channelId = channel,
                            url = url,
                            referrer = referrer,
                            userAgent = userAgent,
                            quality = quality ?: "HD",
                            labels = labels.joinToString(", "),
                            priority = priority
                        )

                        result.getOrPut(channel) { mutableListOf() }.add(entity)
                    }
                }
                reader.endArray()
            }
        }
        result
    }

    private fun mapToTaxonomy(categories: List<String>, categoryMap: Map<String, String>): String {
        for (raw in categories) {
            val cat = raw.lowercase()
            when {
                cat.contains("news") -> return "News"
                cat.contains("music") -> return "Music"
                cat.contains("kid") || cat.contains("animation") -> return "Kids"
                cat.contains("relig") -> return "Religious"
                cat.contains("sport") || cat.contains("auto") -> return "Sports"
                cat.contains("movie") || cat.contains("series") || cat.contains("documentary") -> return "Movies"
                cat.contains("entertain") || cat.contains("general") || cat.contains("comedy") ||
                cat.contains("culture") || cat.contains("lifestyle") || cat.contains("cooking") -> return "Entertainment"
            }
        }
        return "Entertainment"
    }

    private fun mapToLanguage(languages: List<String>, languageMap: Map<String, String>): String {
        for (lang in languages) {
            when (lang.lowercase()) {
                "hin" -> return "Hindi"
                "tam" -> return "Tamil"
                "tel" -> return "Telugu"
                "mal" -> return "Malayalam"
                "ben" -> return "Bengali"
                "kan" -> return "Kannada"
                "mar" -> return "Marathi"
                "guj" -> return "Gujarati"
                "pan" -> return "Punjabi"
                "eng" -> return "English"
                "urd" -> return "Urdu"
                "ori" -> return "Odia"
                "asm" -> return "Assamese"
            }
            languageMap[lang]?.let { return it }
        }
        return "Hindi"
    }

    private fun cleanChannelName(rawName: String): String {
        return rawName.replace(Regex("""\s*\[.*?\]|\s*\(.*?\)"""), "").trim()
    }
}

// Extension helpers for safe JsonReader parsing
private fun JsonReader.nextStringSafe(): String? {
    return if (peek() == android.util.JsonToken.NULL) {
        nextNull()
        null
    } else {
        nextString()
    }
}

private fun JsonReader.nextBooleanSafe(): Boolean {
    return if (peek() == android.util.JsonToken.NULL) {
        nextNull()
        false
    } else if (peek() == android.util.JsonToken.BOOLEAN) {
        nextBoolean()
    } else {
        skipValue()
        false
    }
}

private fun JsonReader.nextIntSafe(): Int {
    return if (peek() == android.util.JsonToken.NULL) {
        nextNull()
        0
    } else if (peek() == android.util.JsonToken.NUMBER) {
        nextInt()
    } else {
        skipValue()
        0
    }
}

private fun JsonReader.readStringList(target: MutableList<String>) {
    if (peek() == android.util.JsonToken.BEGIN_ARRAY) {
        beginArray()
        while (hasNext()) {
            val str = nextStringSafe()
            if (!str.isNullOrBlank()) {
                target.add(str)
            }
        }
        endArray()
    } else {
        skipValue()
    }
}
