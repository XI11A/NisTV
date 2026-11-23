package com.nis.nistv.data.download.offline

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.nis.nistv.core.database.dao.DownloadDao
import com.nis.nistv.core.database.dao.OfflineMediaDao
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.DownloadStatus
import com.nis.nistv.core.preferences.UserPreferences
import com.nis.nistv.data.tmdb.model.TmdbMediaType
import com.nis.nistv.data.tmdb.repository.TmdbRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

data class RepairResult(
    val importedFilesCount: Int,
    val repairedRowsCount: Int,
    val totalDownloads: Int
)

/**
 * Manages one-time idempotent repair for legacy pre-v6 downloads
 * and scans the device Download/NisTV directories for unindexed media.
 */
class DownloadsRepairManager(
    private val context: Context? = null,
    private val downloadDao: DownloadDao,
    private val offlineMediaDao: OfflineMediaDao,
    private val offlineMediaRepository: OfflineMediaRepository,
    private val tmdbRepository: TmdbRepository,
    private val userPreferences: UserPreferences? = null,
    private val additionalScanDirs: List<File> = emptyList()
) {

    companion object {
        private const val TAG = "DownloadsRepairManager"
        private val VIDEO_EXTENSIONS = listOf("mkv", "mp4", "avi", "webm", "ts", "m4v")
    }

    /**
     * Executes the idempotent repair process.
     * Safe to run multiple times; never modifies correct rows or duplicates series.
     */
    suspend fun runRepairIfNeeded(force: Boolean = false): RepairResult = withContext(Dispatchers.IO) {
        val isDone = userPreferences?.isDownloadsRepairV6Done?.firstOrNull() ?: false

        // 1. Scan device Download/NisTV folders and MediaStore for any unindexed files
        val imported = scanAndImportLocalFiles()

        val allDownloads = downloadDao.getAllDownloadsSync()

        val needsRepair = allDownloads.any { dl ->
            val f = File(dl.filePath)
            val fileExists = f.exists() && f.length() > 0
            (dl.mediaType.equals("tv", ignoreCase = true) && (dl.seriesId.isNullOrBlank() || dl.seriesName.isNullOrBlank())) ||
                    (!dl.mediaType.equals("tv", ignoreCase = true) && MediaNameParser.parse(dl.fileName).isTv) ||
                    (!dl.status.equals("COMPLETED", ignoreCase = true) && (dl.fileSizeBytes > 0 && dl.downloadedBytes >= dl.fileSizeBytes)) ||
                    (dl.status.equals("COMPLETED", ignoreCase = true) && dl.fileSizeBytes > 0 && fileExists && f.length() < dl.fileSizeBytes)
        }

        if (isDone && !force && !needsRepair && imported == 0) {
            Log.d(TAG, "Repair already completed and no broken rows detected, skipping.")
            return@withContext RepairResult(0, 0, allDownloads.size)
        }

        Log.i(TAG, "Starting downloads repair (force=$force, existingDownloads=${allDownloads.size}, imported=$imported)...")

        // 2. Fetch all downloads (including any freshly imported)
        val currentDownloads = downloadDao.getAllDownloadsSync()
        var repairedCount = 0

        for (dl in currentDownloads) {
            val parsedInfo = MediaNameParser.parse(dl.fileName.ifBlank { dl.title })

            val isTv = dl.mediaType.equals("tv", ignoreCase = true) || parsedInfo.isTv

            // Check if file exists on disk
            val f = File(dl.filePath)
            val fileExists = f.exists() && f.length() > 0

            val isManagedActiveOrPaused = dl.status.equals(DownloadStatus.PAUSED.name, ignoreCase = true) ||
                    dl.status.equals(DownloadStatus.DOWNLOADING.name, ignoreCase = true) ||
                    dl.status.equals(DownloadStatus.QUEUED.name, ignoreCase = true)

            // An incomplete download that was erroneously marked as COMPLETED (due to prior bug)
            val isErroneouslyCompleted = dl.status.equals(DownloadStatus.COMPLETED.name, ignoreCase = true) &&
                    dl.fileSizeBytes > 0 && fileExists && f.length() < dl.fileSizeBytes

            // Only mark COMPLETED if the download is not currently active/paused, not erroneously completed,
            // and the file on disk actually reaches or exceeds the expected file size (or already valid completed)
            val shouldMarkCompleted = !isManagedActiveOrPaused && !isErroneouslyCompleted && fileExists &&
                    !dl.status.equals("COMPLETED", ignoreCase = true) &&
                    (dl.fileSizeBytes > 0 && (f.length() >= dl.fileSizeBytes || dl.downloadedBytes >= dl.fileSizeBytes))

            val effectiveStatus = when {
                isErroneouslyCompleted -> DownloadStatus.PAUSED.name
                shouldMarkCompleted -> DownloadStatus.COMPLETED.name
                else -> dl.status
            }

            val effectiveDownloaded = when {
                isErroneouslyCompleted -> f.length()
                fileExists && dl.downloadedBytes <= 0L -> f.length()
                else -> dl.downloadedBytes
            }

            val effectiveTotal = when {
                dl.fileSizeBytes > 0L -> dl.fileSizeBytes
                shouldMarkCompleted && fileExists -> f.length()
                else -> dl.fileSizeBytes
            }

            if (!isTv) {
                if (shouldMarkCompleted || isErroneouslyCompleted) {
                    downloadDao.updateDownload(
                        dl.copy(
                            status = effectiveStatus,
                            downloadedBytes = effectiveDownloaded,
                            fileSizeBytes = effectiveTotal,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    repairedCount++
                }
                continue
            }

            // Never touch already-correct rows: valid numeric seriesId, seriesName, season, and episode
            val alreadyValid = !dl.seriesId.isNullOrBlank() &&
                    !SeriesKeyHelper.isFallbackKey(dl.seriesId) &&
                    !dl.seriesName.isNullOrBlank() &&
                    dl.seasonNumber != null &&
                    dl.episodeNumber != null &&
                    dl.mediaType.equals("tv", ignoreCase = true) &&
                    !shouldMarkCompleted &&
                    !isErroneouslyCompleted

            if (alreadyValid) {
                continue
            }

            val seriesNameRaw = dl.seriesName?.ifBlank { null }
                ?: parsedInfo.seriesName
                ?: dl.title.replace(Regex("""[._\s]+S\d+.*$""", RegexOption.IGNORE_CASE), "").trim().ifBlank { null }

            val season = dl.seasonNumber ?: parsedInfo.seasonNumber ?: 1
            val episode = dl.episodeNumber ?: parsedInfo.episodeNumber ?: 1
            val epName = dl.episodeName ?: "Episode $episode"

            // Determine if seriesId needs resolution
            var seriesId = dl.seriesId?.ifBlank { null }
            var canonicalSeriesName = seriesNameRaw ?: "Other / Unsorted"

            if (seriesNameRaw == null) {
                seriesId = SeriesKeyHelper.UNSORTED_KEY
            } else if (seriesId.isNullOrBlank() || SeriesKeyHelper.isFallbackKey(seriesId)) {
                // Try resolving numeric TMDB ID from mediaId if it starts with digits before '_'
                val idFromMedia = dl.mediaId.substringBefore('_').toLongOrNull()
                if (idFromMedia != null && idFromMedia > 0) {
                    seriesId = idFromMedia.toString()
                } else {
                    // Try online TMDB search
                    val resolvedId = resolveSeriesTmdbId(seriesNameRaw, parsedInfo.year)
                    if (resolvedId != null) {
                        seriesId = resolvedId.first.toString()
                        canonicalSeriesName = resolvedId.second
                    } else {
                        // Use deterministic fallback slug
                        seriesId = SeriesKeyHelper.buildFallbackSeriesKey(seriesNameRaw)
                    }
                }
            }

            val epTag = String.format(Locale.US, "S%02dE%02d", season, episode)
            val updatedMediaId = "${seriesId}_$epTag"
            val updatedTitle = if (dl.title.isBlank() || (dl.title.contains("S") && dl.title.contains("E"))) {
                "$canonicalSeriesName $epTag"
            } else {
                "${dl.title} $epTag"
            }

            val updatedEntity = dl.copy(
                mediaType = "tv",
                status = effectiveStatus,
                downloadedBytes = effectiveDownloaded,
                fileSizeBytes = effectiveTotal,
                seriesId = seriesId,
                seriesName = canonicalSeriesName,
                seasonNumber = season,
                episodeNumber = episode,
                episodeName = epName,
                mediaId = updatedMediaId,
                title = updatedTitle,
                updatedAt = System.currentTimeMillis()
            )

            if (updatedEntity.copy(updatedAt = dl.updatedAt) != dl) {
                downloadDao.updateDownload(updatedEntity)
                repairedCount++
            }
        }

        userPreferences?.setDownloadsRepairV6Done(true)
        Log.i(TAG, "Repair complete: imported $imported files, repaired $repairedCount rows.")

        // 3. Trigger offline metadata backfill for any titles missing local assets
        try {
            offlineMediaRepository.backfillPendingMetadata()
        } catch (e: Exception) {
            Log.w(TAG, "Offline metadata backfill error: ${e.message}")
        }

        RepairResult(imported, repairedCount, currentDownloads.size)
    }

    /**
     * Scans public Download/NisTV and app-private external download directories
     * for video files that do not currently exist in the downloads database.
     */
    suspend fun scanAndImportLocalFiles(): Int = withContext(Dispatchers.IO) {
        val existingDownloads = downloadDao.getAllDownloadsSync()
        val existingPaths = existingDownloads.map { 
            try { File(it.filePath).canonicalPath } catch (_: Exception) { it.filePath }
        }.toSet()
        val existingNames = existingDownloads.map { it.fileName.lowercase() }.toSet()

        val candidateDirs = mutableSetOf<File>()
        candidateDirs.addAll(additionalScanDirs)
        if (context != null) {
            try {
                candidateDirs.add(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NisTV"))
                candidateDirs.add(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NisTV_Downloads"))
                candidateDirs.add(File(Environment.getExternalStorageDirectory(), "Download/NisTV"))
                candidateDirs.add(File(Environment.getExternalStorageDirectory(), "Downloads/NisTV"))
                candidateDirs.add(File("/storage/emulated/0/Download/NisTV"))
                candidateDirs.add(File("/storage/emulated/0/Downloads/NisTV"))
                candidateDirs.add(File("/sdcard/Download/NisTV"))
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                    candidateDirs.add(File(it, "NisTV"))
                    candidateDirs.add(File(it, "NisTV_Downloads"))
                }
                context.filesDir?.let {
                    candidateDirs.add(File(it, "NisTV"))
                }
            } catch (_: Exception) {}
        }

        val candidateFiles = mutableListOf<File>()
        for (dir in candidateDirs) {
            try {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles()
                    if (files != null) {
                        for (f in files) {
                            if (f.isFile && f.length() > 0 && f.extension.lowercase() in VIDEO_EXTENSIONS) {
                                candidateFiles.add(f)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error listing files in $dir: ${e.message}")
            }
        }

        // Also query MediaStore.Video for any NisTV files
        try {
            if (context != null) {
                val projection = arrayOf(MediaStore.Video.Media.DATA)
                val selection = "${MediaStore.Video.Media.DATA} LIKE ?"
                val selectionArgs = arrayOf("%NisTV%")
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    while (cursor.moveToNext()) {
                        val path = cursor.getString(dataCol)
                        if (!path.isNullOrBlank()) {
                            val f = File(path)
                            if (f.exists() && f.isFile && f.length() > 0 && f.extension.lowercase() in VIDEO_EXTENSIONS) {
                                candidateFiles.add(f)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore scan fallback error: ${e.message}")
        }

        var importedCount = 0

        for (file in candidateFiles.distinctBy { it.name.lowercase() }) {
            val canonicalPath = try { file.canonicalPath } catch (_: Exception) { file.absolutePath }
            if (canonicalPath in existingPaths || file.name.lowercase() in existingNames) {
                continue
            }

            val parsed = MediaNameParser.parse(file.name)
            val id = UUID.randomUUID().toString()

            val newDownload = if (parsed.isTv) {
                val sName = parsed.seriesName ?: file.nameWithoutExtension
                val season = parsed.seasonNumber ?: 1
                val episode = parsed.episodeNumber ?: 1
                val epTag = String.format(Locale.US, "S%02dE%02d", season, episode)
                val seriesKey = SeriesKeyHelper.buildFallbackSeriesKey(sName)

                DownloadEntity(
                    id = id,
                    mediaId = "${seriesKey}_$epTag",
                    title = "$sName $epTag",
                    mediaType = "tv",
                    downloadUrl = file.toURI().toString(),
                    fileName = file.name,
                    filePath = file.absolutePath,
                    fileSizeBytes = file.length(),
                    downloadedBytes = file.length(),
                    status = DownloadStatus.COMPLETED.name,
                    createdAt = file.lastModified(),
                    updatedAt = file.lastModified(),
                    seasonNumber = season,
                    episodeNumber = episode,
                    episodeName = "Episode $episode",
                    seriesId = seriesKey,
                    seriesName = sName
                )
            } else {
                val title = parsed.movieTitle ?: file.nameWithoutExtension
                DownloadEntity(
                    id = id,
                    mediaId = "imported_${System.currentTimeMillis()}_${file.name.hashCode()}",
                    title = title,
                    mediaType = "movie",
                    downloadUrl = file.toURI().toString(),
                    fileName = file.name,
                    filePath = file.absolutePath,
                    fileSizeBytes = file.length(),
                    downloadedBytes = file.length(),
                    status = DownloadStatus.COMPLETED.name,
                    createdAt = file.lastModified(),
                    updatedAt = file.lastModified()
                )
            }

            downloadDao.insertDownload(newDownload)
            importedCount++
            Log.i(TAG, "Imported unindexed media file: ${file.name} as ${newDownload.mediaType} (id=${newDownload.id})")
        }

        importedCount
    }

    /**
     * Resolves pending series that currently use fallback keys by querying TMDB.
     */
    suspend fun resolvePendingFallbackSeries(): Unit = withContext(Dispatchers.IO) {
        val allDownloads = downloadDao.getAllDownloadsSync()
        val fallbackSeriesDownloads = allDownloads.filter { dl ->
            dl.mediaType.equals("tv", ignoreCase = true) &&
                    (dl.seriesId == null || SeriesKeyHelper.isFallbackKey(dl.seriesId))
        }

        if (fallbackSeriesDownloads.isEmpty()) return@withContext

        val bySeriesName = fallbackSeriesDownloads.groupBy { it.effectiveSeriesName ?: it.seriesName ?: it.title }

        for ((name, downloads) in bySeriesName) {
            val resolved = resolveSeriesTmdbId(name) ?: continue
            val tmdbId = resolved.first.toString()
            val canonicalTitle = resolved.second

            for (dl in downloads) {
                val season = dl.seasonNumber ?: 1
                val episode = dl.episodeNumber ?: 1
                val epTag = String.format(Locale.US, "S%02dE%02d", season, episode)

                val updated = dl.copy(
                    seriesId = tmdbId,
                    seriesName = canonicalTitle,
                    mediaId = "${tmdbId}_$epTag",
                    title = "$canonicalTitle $epTag",
                    updatedAt = System.currentTimeMillis()
                )
                downloadDao.updateDownload(updated)
            }
        }
    }

    /**
     * Searches TMDB TV series by name (and optional year) with confidence verification.
     * Returns Pair(tmdbId, canonicalTitle) or null if unresolved.
     */
    private suspend fun resolveSeriesTmdbId(query: String, year: Int? = null): Pair<Long, String>? {
        if (query.isBlank()) return null
        return try {
            val results = tmdbRepository.search(TmdbMediaType.TV, query)
            if (results.isEmpty()) return null

            // 1. Look for exact title match (case-insensitive)
            val exact = results.firstOrNull { it.title.equals(query, ignoreCase = true) }
            if (exact != null) {
                return Pair(exact.id, exact.title)
            }

            // 2. Look for year match if year was parsed
            if (year != null) {
                val yearMatch = results.firstOrNull { it.releaseYear?.toIntOrNull() == year }
                if (yearMatch != null) {
                    return Pair(yearMatch.id, yearMatch.title)
                }
            }

            // 3. Fallback to top result if reasonably close
            val top = results.first()
            if (top.title.contains(query, ignoreCase = true) || query.contains(top.title, ignoreCase = true)) {
                Pair(top.id, top.title)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "TMDB search failed for \"$query\": ${e.message}")
            null
        }
    }
}
