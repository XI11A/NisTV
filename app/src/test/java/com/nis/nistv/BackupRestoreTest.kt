package com.nis.nistv

import com.nis.nistv.data.backup.BackupFavoriteItem
import com.nis.nistv.data.backup.BackupParseResult
import com.nis.nistv.data.backup.BackupPlaylistItem
import com.nis.nistv.data.backup.BackupPreferences
import com.nis.nistv.data.backup.BackupRestoreHelper
import com.nis.nistv.data.backup.BackupWatchLaterItem
import com.nis.nistv.data.backup.NisTvBackupData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRestoreTest {

    @Test
    fun roundTripBackupAndParse_succeedsWithMatchingCounts() {
        val testData = NisTvBackupData(
            favorites = listOf(
                BackupFavoriteItem(profileId = "default", channelId = "StarSports.in", createdAt = 1000L),
                BackupFavoriteItem(profileId = "default", channelId = "SonyTen.in", createdAt = 2000L)
            ),
            watchLater = listOf(
                BackupWatchLaterItem(
                    id = "movie_100",
                    mediaId = 100L,
                    mediaType = "movie",
                    title = "Inception",
                    addedAt = 3000L
                )
            ),
            recentSearches = listOf("cricket", "action movies"),
            playlists = listOf(
                BackupPlaylistItem(
                    id = "pl_1",
                    name = "My IPTV",
                    sourceType = "URL",
                    url = "https://example.com/playlist.m3u",
                    channelCount = 50
                )
            ),
            preferences = BackupPreferences(
                preferredLanguage = "Tamil",
                preferredCountry = "IN",
                startupSoundEnabled = true
            )
        )

        val jsonString = BackupRestoreHelper.createBackupJson(
            appVersion = "1.9.0",
            data = testData,
            createdAt = 123456789L
        )

        val result = BackupRestoreHelper.validateAndParseBackup(jsonString)
        assertTrue("Backup parsing should succeed", result is BackupParseResult.Success)

        val preview = (result as BackupParseResult.Success).preview
        assertEquals("1.9.0", preview.appVersion)
        assertEquals(2, preview.favoritesCount)
        assertEquals(1, preview.watchLaterCount)
        assertEquals(2, preview.recentSearchesCount)
        assertEquals(1, preview.playlistsCount)
        assertEquals("Tamil", preview.payload.data.preferences.preferredLanguage)
    }

    @Test
    fun corruptedJson_returnsCorruptResult() {
        val corrupt = "{ \"schemaVersion\": 1, \"appVersion\": \"1.9.0\", data: broken..."
        val result = BackupRestoreHelper.validateAndParseBackup(corrupt)
        assertTrue("Broken JSON must return Corrupt", result is BackupParseResult.Corrupt)
    }

    @Test
    fun modifiedPayload_failsChecksumValidation() {
        val testData = NisTvBackupData(
            favorites = listOf(BackupFavoriteItem(channelId = "StarPlus.in"))
        )
        val validJson = BackupRestoreHelper.createBackupJson(
            appVersion = "1.9.0",
            data = testData
        )

        // Tamper with channel ID inside the JSON without updating the checksum
        val tamperedJson = validJson.replace("StarPlus.in", "HackedChannel.in")

        val result = BackupRestoreHelper.validateAndParseBackup(tamperedJson)
        assertTrue("Tampered backup must trigger ChecksumError", result is BackupParseResult.ChecksumError)
    }

    @Test
    fun futureSchemaVersion_returnsVersionMismatch() {
        val testData = NisTvBackupData()
        val validJson = BackupRestoreHelper.createBackupJson(
            appVersion = "2.5.0",
            data = testData
        )
        // Simulate a backup from a future schema version (e.g. 99)
        val futureJson = validJson.replace("\"schemaVersion\": 2", "\"schemaVersion\": 99")

        val result = BackupRestoreHelper.validateAndParseBackup(futureJson, maxSupportedSchemaVersion = 2)
        assertTrue("Future schema version must be rejected", result is BackupParseResult.VersionMismatch)
        val mismatch = result as BackupParseResult.VersionMismatch
        assertEquals(99, mismatch.foundVersion)
        assertEquals(2, mismatch.maxSupportedVersion)
    }

    @Test
    fun v2Backup_includesProfilesAndPerProfileData() {
        val testData = NisTvBackupData(
            profiles = listOf(
                com.nis.nistv.data.backup.BackupProfileItem(
                    id = "default",
                    name = "Dad",
                    avatarId = "avatar_01",
                    preferredLanguage = "Hindi",
                    preferredCountry = "IN",
                    isKids = false,
                    isLocked = false
                ),
                com.nis.nistv.data.backup.BackupProfileItem(
                    id = "kids_1",
                    name = "Junior",
                    avatarId = "avatar_04",
                    preferredLanguage = "English",
                    preferredCountry = "IN",
                    isKids = true,
                    isLocked = true
                )
            ),
            playbackProgress = listOf(
                com.nis.nistv.data.backup.BackupPlaybackProgressItem(
                    profileId = "default",
                    downloadId = "dl_100",
                    positionMs = 45000L,
                    durationMs = 90000L,
                    lastWatchedAt = 1000L
                )
            ),
            reminders = listOf(
                com.nis.nistv.data.backup.BackupReminderItem(
                    id = "rem_1",
                    profileId = "default",
                    channelId = "StarSports.in",
                    programTitle = "Live Cricket",
                    startEpochMs = 200000L
                )
            )
        )

        val jsonString = BackupRestoreHelper.createBackupJson(
            appVersion = "2.0.0",
            data = testData,
            createdAt = 123456789L
        )

        val result = BackupRestoreHelper.validateAndParseBackup(jsonString)
        assertTrue("v2 backup parsing must succeed", result is BackupParseResult.Success)

        val preview = (result as BackupParseResult.Success).preview
        assertEquals("2.0.0", preview.appVersion)
        assertEquals(2, preview.profilesCount)
        assertEquals(1, preview.payload.data.playbackProgress.size)
        assertEquals(1, preview.payload.data.reminders.size)
        assertEquals(2, preview.payload.schemaVersion)
    }

    @Test
    fun v1LegacyBackup_importsSuccessfully() {
        val testData = NisTvBackupData(
            favorites = listOf(
                BackupFavoriteItem(channelId = "StarSports.in", createdAt = 1000)
            ),
            watchLater = listOf(
                BackupWatchLaterItem(
                    id = "m_1",
                    mediaId = 1,
                    mediaType = "movie",
                    title = "Test Movie",
                    addedAt = 2000
                )
            ),
            recentSearches = listOf("cricket"),
            preferences = BackupPreferences(
                preferredLanguage = "Tamil",
                preferredCountry = "IN"
            )
        )

        val v2Json = BackupRestoreHelper.createBackupJson(
            appVersion = "1.20.0",
            data = testData,
            createdAt = 1700000000000
        )
        // Simulate a legacy v1 backup
        val validV1Json = v2Json.replace("\"schemaVersion\": 2", "\"schemaVersion\": 1")

        val result = BackupRestoreHelper.validateAndParseBackup(validV1Json)
        assertTrue("v1 legacy backup must succeed under schema 2 support", result is BackupParseResult.Success)
        val preview = (result as BackupParseResult.Success).preview
        assertEquals(1, preview.payload.schemaVersion)
        assertEquals(1, preview.favoritesCount)
        assertEquals(1, preview.watchLaterCount)
    }
}
