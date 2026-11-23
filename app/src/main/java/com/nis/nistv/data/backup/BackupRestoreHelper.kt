package com.nis.nistv.data.backup

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

object BackupRestoreHelper {

    const val CURRENT_SCHEMA_VERSION = 2

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val compactJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun computeChecksum(dataJson: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(dataJson.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun createBackupJson(
        appVersion: String,
        data: NisTvBackupData,
        createdAt: Long = System.currentTimeMillis()
    ): String {
        val serializedData = compactJson.encodeToString(data)
        val checksum = computeChecksum(serializedData)

        val payload = NisTvBackupPayload(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            appVersion = appVersion,
            createdAt = createdAt,
            data = data,
            checksum = checksum
        )
        return json.encodeToString(payload)
    }

    fun validateAndParseBackup(
        jsonString: String,
        maxSupportedSchemaVersion: Int = CURRENT_SCHEMA_VERSION
    ): BackupParseResult {
        if (jsonString.isBlank()) {
            return BackupParseResult.Corrupt("Backup file is empty")
        }

        val payload: NisTvBackupPayload = try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            return BackupParseResult.Corrupt("Failed to parse JSON: ${e.message}")
        }

        if (payload.schemaVersion > maxSupportedSchemaVersion) {
            return BackupParseResult.VersionMismatch(
                foundVersion = payload.schemaVersion,
                maxSupportedVersion = maxSupportedSchemaVersion
            )
        }

        // Verify checksum integrity
        val serializedData = compactJson.encodeToString(payload.data)
        val expectedChecksum = computeChecksum(serializedData)
        if (!expectedChecksum.equals(payload.checksum, ignoreCase = true)) {
            return BackupParseResult.ChecksumError("Backup checksum mismatch. The file may be corrupted or modified.")
        }

        val preview = BackupPreview(
            schemaVersion = payload.schemaVersion,
            appVersion = payload.appVersion,
            createdAt = payload.createdAt,
            favoritesCount = payload.data.favorites.size,
            watchLaterCount = payload.data.watchLater.size,
            recentSearchesCount = payload.data.recentSearches.size,
            playlistsCount = payload.data.playlists.size,
            profilesCount = payload.data.profiles.size,
            payload = payload
        )

        return BackupParseResult.Success(preview)
    }
}
