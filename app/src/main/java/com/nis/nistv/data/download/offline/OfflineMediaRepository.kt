package com.nis.nistv.data.download.offline

import android.content.Context
import android.util.Log
import com.nis.nistv.core.database.dao.DownloadDao
import com.nis.nistv.core.database.dao.OfflineMediaDao
import com.nis.nistv.core.database.entity.DownloadEntity
import com.nis.nistv.core.database.entity.OfflineCastMember
import com.nis.nistv.core.database.entity.OfflineCrewMember
import com.nis.nistv.core.database.entity.OfflineEpisodeEntity
import com.nis.nistv.core.database.entity.OfflineMediaEntity
import com.nis.nistv.core.database.entity.OfflineSeasonEntity
import com.nis.nistv.data.tmdb.model.TmdbCastMember
import com.nis.nistv.data.tmdb.model.TmdbEpisodeDto
import com.nis.nistv.data.tmdb.model.TmdbMediaType
import com.nis.nistv.data.tmdb.repository.TmdbRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

interface OfflineMediaRepository {
    val isSavingOfflineDetails: StateFlow<Boolean>
    fun getOfflineMedia(mediaType: String, tmdbId: Long): Flow<OfflineMediaEntity?>
    suspend fun getOfflineMediaSync(mediaType: String, tmdbId: Long): OfflineMediaEntity?
    fun getOfflineSeasons(seriesKey: String): Flow<List<OfflineSeasonEntity>>
    fun getOfflineEpisodesForSeason(seriesKey: String, seasonNumber: Int): Flow<List<OfflineEpisodeEntity>>
    fun getOfflineEpisodesForSeries(seriesKey: String): Flow<List<OfflineEpisodeEntity>>
    suspend fun saveFor(download: DownloadEntity, forceRefresh: Boolean = false)
    suspend fun backfillPendingMetadata()
    suspend fun onDownloadDeletedOrCancelled(deletedDownload: DownloadEntity)
    suspend fun deleteSeason(seriesKey: String, seasonNumber: Int)
}

class DefaultOfflineMediaRepository(
    private val context: Context,
    private val offlineMediaDao: OfflineMediaDao,
    private val downloadDao: DownloadDao,
    private val tmdbRepository: TmdbRepository,
    private val okHttpClient: OkHttpClient,
    baseFilesDir: File? = null
) : OfflineMediaRepository {

    companion object {
        private const val TAG = "OfflineMediaRepo"
        private const val BASE_IMG_URL = "https://image.tmdb.org/t/p"
    }

    private val _isSavingOfflineDetails = MutableStateFlow(false)
    override val isSavingOfflineDetails: StateFlow<Boolean> = _isSavingOfflineDetails.asStateFlow()

    private val rootFilesDir: File by lazy { baseFilesDir ?: context.filesDir }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun getOfflineMedia(mediaType: String, tmdbId: Long): Flow<OfflineMediaEntity?> {
        val id = if (mediaType.equals("tv", ignoreCase = true)) {
            SeriesKeyHelper.buildSeriesKey(tmdbId)
        } else {
            "movie_$tmdbId"
        }
        return offlineMediaDao.getMedia(id).flowOn(Dispatchers.IO)
    }

    override suspend fun getOfflineMediaSync(mediaType: String, tmdbId: Long): OfflineMediaEntity? = withContext(Dispatchers.IO) {
        val id = if (mediaType.equals("tv", ignoreCase = true)) {
            SeriesKeyHelper.buildSeriesKey(tmdbId)
        } else {
            "movie_$tmdbId"
        }
        offlineMediaDao.getMediaSync(id)
    }

    override fun getOfflineSeasons(seriesKey: String): Flow<List<OfflineSeasonEntity>> =
        offlineMediaDao.getSeasons(seriesKey).flowOn(Dispatchers.IO)

    override fun getOfflineEpisodesForSeason(
        seriesKey: String,
        seasonNumber: Int
    ): Flow<List<OfflineEpisodeEntity>> =
        offlineMediaDao.getEpisodesForSeason(seriesKey, seasonNumber).flowOn(Dispatchers.IO)

    override fun getOfflineEpisodesForSeries(seriesKey: String): Flow<List<OfflineEpisodeEntity>> =
        offlineMediaDao.getEpisodes(seriesKey).flowOn(Dispatchers.IO)

    /**
     * Resolves the app-private directory for storing offline media assets.
     * filesDir/offline_media/{key}/
     */
    fun getMediaAssetDir(mediaType: String, idKey: String): File {
        val folderKey = if (idKey.startsWith("${mediaType.lowercase()}_")) idKey else "${mediaType.lowercase()}_$idKey"
        val baseDir = File(rootFilesDir, "offline_media/$folderKey")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        return baseDir
    }

    fun getMediaAssetDir(mediaType: String, tmdbId: Long): File {
        return getMediaAssetDir(mediaType, tmdbId.toString())
    }

    /**
     * Downloads an image from TMDB into a target local file atomically (writing to temp first).
     * If file already exists and is non-empty, skips download.
     */
    private suspend fun downloadImageToFile(url: String?, targetFile: File, forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext null
        if (!forceRefresh && targetFile.exists() && targetFile.length() > 0) {
            return@withContext targetFile.absolutePath
        }

        targetFile.parentFile?.mkdirs()
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp_${System.currentTimeMillis()}")

        val request = Request.Builder().url(url).build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Failed to download image $url - HTTP ${response.code}")
                    return@withContext null
                }
                val body = response.body ?: return@withContext null
                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                if (tempFile.renameTo(targetFile)) {
                    return@withContext targetFile.absolutePath
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error downloading image $url: ${e.message}")
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
        null
    }

    override suspend fun saveFor(download: DownloadEntity, forceRefresh: Boolean): Unit = withContext(Dispatchers.IO) {
        val mediaType = download.mediaType.lowercase()
        try {
            if (mediaType == "tv") {
                saveTvMetadata(download, forceRefresh)
            } else {
                saveMovieMetadata(download, forceRefresh)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving offline metadata for ${download.title}: ${e.message}", e)
        }
    }

    private suspend fun saveMovieMetadata(download: DownloadEntity, forceRefresh: Boolean) {
        val tmdbId = resolveTmdbId(download) ?: run {
            // Try TMDB search for movie if numeric ID unknown
            val movieTitle = download.title.substringBefore(" (").trim()
            val searchResults = try {
                tmdbRepository.search(TmdbMediaType.MOVIE, movieTitle)
            } catch (_: Exception) { emptyList() }
            val match = searchResults.firstOrNull { it.title.equals(movieTitle, ignoreCase = true) }
                ?: searchResults.firstOrNull()
            match?.id ?: 0L
        }

        val assetKey = if (tmdbId > 0) tmdbId.toString() else download.id
        val assetDir = getMediaAssetDir("movie", assetKey)

        val details = if (tmdbId > 0) {
            try {
                tmdbRepository.getMovieDetails(tmdbId, forceRefresh = forceRefresh)
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch movie details from TMDB: ${e.message}. Using fallback download metadata.")
                null
            }
        } else null

        val posterUrl = details?.posterPath?.let { "$BASE_IMG_URL/w500$it" } ?: download.posterUrl
        val backdropUrl = details?.backdropPath?.let { "$BASE_IMG_URL/w1280$it" } ?: download.backdropUrl

        val localPosterPath = downloadImageToFile(posterUrl, File(assetDir, "poster.jpg"), forceRefresh)
        val localBackdropPath = downloadImageToFile(backdropUrl, File(assetDir, "backdrop.jpg"), forceRefresh)

        val castList = mutableListOf<OfflineCastMember>()
        val topCast = details?.credits?.cast?.take(15).orEmpty()
        for (cast in topCast) {
            val profileUrl = cast.profilePath?.let { "$BASE_IMG_URL/w185$it" }
            val localProfile = downloadImageToFile(profileUrl, File(assetDir, "profiles/cast_${cast.id}.jpg"), forceRefresh)
            castList.add(
                OfflineCastMember(
                    id = cast.id,
                    name = cast.name,
                    character = cast.character,
                    localProfilePath = localProfile
                )
            )
        }

        val crewList = mutableListOf<OfflineCrewMember>()
        val topCrew = details?.credits?.crew.orEmpty()
        topCrew.filter {
            it.job.equals("Director", ignoreCase = true) ||
                    it.job.equals("Writer", ignoreCase = true) ||
                    it.job.equals("Screenplay", ignoreCase = true)
        }.distinctBy { it.id to it.job }.forEach {
            crewList.add(
                OfflineCrewMember(
                    id = it.id,
                    name = it.name,
                    job = it.job,
                    department = it.department
                )
            )
        }

        val genresStr = details?.genres?.joinToString(", ") { it.name } ?: download.genres
        val entity = OfflineMediaEntity(
            id = "movie_$assetKey",
            tmdbId = tmdbId,
            mediaType = "movie",
            title = details?.title ?: download.title,
            originalTitle = details?.originalTitle,
            tagline = details?.tagline,
            overview = details?.overview?.ifBlank { null } ?: download.overview.orEmpty(),
            releaseDate = details?.releaseDate ?: download.releaseDate,
            status = details?.status,
            rating = details?.voteAverage ?: download.rating ?: 0.0,
            voteCount = details?.voteCount ?: 0,
            runtimeMinutes = details?.runtime ?: download.runtimeMinutes,
            genres = genresStr,
            originalLanguage = details?.originalLanguage,
            certification = null,
            numberOfSeasons = null,
            numberOfEpisodes = null,
            castJson = json.encodeToString(castList),
            crewJson = json.encodeToString(crewList),
            localPosterPath = localPosterPath,
            localBackdropPath = localBackdropPath,
            localLogoPath = null,
            savedAt = System.currentTimeMillis()
        )

        offlineMediaDao.insertMedia(entity)
        Log.i(TAG, "Successfully stored offline metadata for movie $assetKey (${entity.title})")
    }

    private suspend fun saveTvMetadata(download: DownloadEntity, forceRefresh: Boolean) {
        val seriesKey = SeriesKeyHelper.getSeriesKey(download.seriesId, download.effectiveSeriesName)
        var tmdbId = SeriesKeyHelper.extractTmdbId(seriesKey) ?: 0L

        // If TMDB ID is unresolved, attempt online TMDB search by series name
        if (tmdbId <= 0L) {
            val query = download.effectiveSeriesName ?: download.seriesName ?: download.title.substringBefore(" S")
            try {
                val results = tmdbRepository.search(TmdbMediaType.TV, query)
                val match = results.firstOrNull { it.title.equals(query, ignoreCase = true) }
                    ?: results.firstOrNull()
                if (match != null) {
                    tmdbId = match.id
                    val updated = download.copy(
                        seriesId = tmdbId.toString(),
                        seriesName = match.title,
                        updatedAt = System.currentTimeMillis()
                    )
                    downloadDao.updateDownload(updated)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not resolve series from TMDB: ${e.message}")
            }
        }

        val finalSeriesKey = if (tmdbId > 0) SeriesKeyHelper.buildSeriesKey(tmdbId) else seriesKey
        val assetDir = getMediaAssetDir("tv", finalSeriesKey)

        val tvDetails = if (tmdbId > 0) {
            try {
                tmdbRepository.getTvDetails(tmdbId, forceRefresh = forceRefresh)
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch TV details from TMDB: ${e.message}. Using fallback download metadata.")
                null
            }
        } else null

        val posterUrl = tvDetails?.posterPath?.let { "$BASE_IMG_URL/w500$it" } ?: download.posterUrl
        val backdropUrl = tvDetails?.backdropPath?.let { "$BASE_IMG_URL/w1280$it" } ?: download.backdropUrl

        val localPosterPath = downloadImageToFile(posterUrl, File(assetDir, "poster.jpg"), forceRefresh)
        val localBackdropPath = downloadImageToFile(backdropUrl, File(assetDir, "backdrop.jpg"), forceRefresh)

        val castList = mutableListOf<OfflineCastMember>()
        val topCast = tvDetails?.credits?.cast?.take(15).orEmpty()
        for (cast in topCast) {
            val profileUrl = cast.profilePath?.let { "$BASE_IMG_URL/w185$it" }
            val localProfile = downloadImageToFile(profileUrl, File(assetDir, "profiles/cast_${cast.id}.jpg"), forceRefresh)
            castList.add(
                OfflineCastMember(
                    id = cast.id,
                    name = cast.name,
                    character = cast.character,
                    localProfilePath = localProfile
                )
            )
        }

        val crewList = mutableListOf<OfflineCrewMember>()
        tvDetails?.createdBy.orEmpty().forEach { creator ->
            crewList.add(OfflineCrewMember(creator.id, creator.name, "Creator"))
        }
        val topCrew = tvDetails?.credits?.crew.orEmpty()
        topCrew.filter {
            it.job.equals("Director", ignoreCase = true) ||
                    it.job.equals("Writer", ignoreCase = true) ||
                    it.job.equals("Creator", ignoreCase = true)
        }.distinctBy { it.id to it.job }.forEach {
            if (crewList.none { c -> c.id == it.id && c.job == it.job }) {
                crewList.add(OfflineCrewMember(it.id, it.name, it.job, it.department))
            }
        }

        val genresStr = tvDetails?.genres?.joinToString(", ") { it.name } ?: download.genres
        val seriesTitle = tvDetails?.name ?: download.effectiveSeriesName ?: download.seriesName ?: download.title

        val mediaEntity = OfflineMediaEntity(
            id = finalSeriesKey,
            tmdbId = tmdbId,
            mediaType = "tv",
            title = seriesTitle,
            originalTitle = tvDetails?.originalName,
            tagline = tvDetails?.tagline,
            overview = tvDetails?.overview?.ifBlank { null } ?: download.overview.orEmpty(),
            releaseDate = tvDetails?.firstAirDate ?: download.releaseDate,
            status = tvDetails?.status,
            rating = tvDetails?.voteAverage ?: download.rating ?: 0.0,
            voteCount = tvDetails?.voteCount ?: 0,
            runtimeMinutes = download.runtimeMinutes,
            genres = genresStr,
            originalLanguage = tvDetails?.originalLanguage,
            certification = null,
            numberOfSeasons = tvDetails?.numberOfSeasons ?: download.seasonNumber ?: 1,
            numberOfEpisodes = tvDetails?.numberOfEpisodes ?: 0,
            castJson = json.encodeToString(castList),
            crewJson = json.encodeToString(crewList),
            localPosterPath = localPosterPath,
            localBackdropPath = localBackdropPath,
            localLogoPath = null,
            savedAt = System.currentTimeMillis()
        )
        offlineMediaDao.insertMedia(mediaEntity)

        // Save Seasons & Episodes
        val seasonNumber = download.seasonNumber ?: 1
        val seasonDetails = if (tmdbId > 0) {
            try {
                tmdbRepository.getTvSeason(tmdbId, seasonNumber)
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch Season $seasonNumber from TMDB: ${e.message}")
                null
            }
        } else null

        val seasonPosterUrl = tvDetails?.seasons?.firstOrNull { it.seasonNumber == seasonNumber }?.posterPath
            ?.let { "$BASE_IMG_URL/w500$it" }
        val localSeasonPoster = downloadImageToFile(seasonPosterUrl, File(assetDir, "seasons/s${seasonNumber}_poster.jpg"), forceRefresh)

        val seasonEntity = OfflineSeasonEntity(
            seriesKey = finalSeriesKey,
            seasonNumber = seasonNumber,
            name = seasonDetails?.name ?: "Season $seasonNumber",
            overview = null,
            airDate = null,
            episodeCount = seasonDetails?.episodes?.size ?: 0,
            localPosterPath = localSeasonPoster ?: localPosterPath
        )
        offlineMediaDao.insertSeasons(listOf(seasonEntity))

        val episodesList = mutableListOf<OfflineEpisodeEntity>()
        if (seasonDetails != null && seasonDetails.episodes.isNotEmpty()) {
            for (ep in seasonDetails.episodes) {
                val stillUrl = ep.stillPath?.let { "$BASE_IMG_URL/w300$it" }
                val localStill = downloadImageToFile(stillUrl, File(assetDir, "stills/s${seasonNumber}e${ep.episodeNumber}.jpg"), forceRefresh)
                episodesList.add(
                    OfflineEpisodeEntity(
                        seriesKey = finalSeriesKey,
                        seasonNumber = ep.seasonNumber,
                        episodeNumber = ep.episodeNumber,
                        name = ep.name,
                        overview = ep.overview,
                        airDate = ep.airDate,
                        runtimeMinutes = download.runtimeMinutes,
                        rating = ep.voteAverage,
                        localStillPath = localStill
                    )
                )
            }
        } else {
            val epNum = download.episodeNumber ?: 1
            episodesList.add(
                OfflineEpisodeEntity(
                    seriesKey = finalSeriesKey,
                    seasonNumber = seasonNumber,
                    episodeNumber = epNum,
                    name = download.episodeName ?: "Episode $epNum",
                    overview = download.overview.orEmpty(),
                    airDate = download.releaseDate,
                    runtimeMinutes = download.runtimeMinutes,
                    rating = download.rating ?: 0.0,
                    localStillPath = null
                )
            )
        }
        offlineMediaDao.insertEpisodes(episodesList)
        Log.i(TAG, "Successfully stored offline metadata for series $finalSeriesKey Season $seasonNumber (${episodesList.size} episodes)")
    }

    override suspend fun backfillPendingMetadata(): Unit = withContext(Dispatchers.IO) {
        _isSavingOfflineDetails.value = true
        try {
            val allDownloads = downloadDao.getAllDownloadsSync()
            for (dl in allDownloads) {
                try {
                    val mediaType = dl.mediaType.lowercase()
                    val key = if (mediaType == "tv") {
                        SeriesKeyHelper.getSeriesKey(dl.seriesId, dl.effectiveSeriesName)
                    } else {
                        val tmdbId = resolveTmdbId(dl) ?: 0L
                        if (tmdbId > 0) "movie_$tmdbId" else "movie_${dl.id}"
                    }

                    val existing = offlineMediaDao.getMediaSync(key)
                    val needsImages = existing != null && (
                            existing.localPosterPath?.let { !File(it).exists() } == true ||
                                    existing.localBackdropPath?.let { !File(it).exists() } == true
                            )

                    if (existing == null || needsImages) {
                        Log.i(TAG, "Backfilling offline metadata/assets for ${dl.title} ($key)")
                        saveFor(dl, forceRefresh = false)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping failed backfill item ${dl.title}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during backfill: ${e.message}")
        } finally {
            _isSavingOfflineDetails.value = false
        }
    }

    override suspend fun onDownloadDeletedOrCancelled(deletedDownload: DownloadEntity): Unit = withContext(Dispatchers.IO) {
        val mediaType = deletedDownload.mediaType.lowercase()
        val key = if (mediaType == "tv") {
            SeriesKeyHelper.getSeriesKey(deletedDownload.seriesId, deletedDownload.effectiveSeriesName)
        } else {
            val id = resolveTmdbId(deletedDownload) ?: return@withContext
            "movie_$id"
        }

        if (mediaType == "movie") {
            val all = downloadDao.getAllDownloadsSync()
            val remaining = all.filter { it.id != deletedDownload.id && resolveTmdbId(it) == resolveTmdbId(deletedDownload) && it.mediaType.lowercase() == "movie" }
            if (remaining.isEmpty()) {
                Log.i(TAG, "Cleaning up offline metadata and assets for movie $key (last download removed)")
                offlineMediaDao.deleteMedia(key)
                deleteAssetDir(mediaType, key)
            }
        } else if (mediaType == "tv") {
            val seriesIdStr = deletedDownload.seriesId ?: key
            val remainingSeriesDownloads = downloadDao.getDownloadsBySeriesSync(seriesIdStr)
                .filter { it.id != deletedDownload.id }

            if (remainingSeriesDownloads.isEmpty()) {
                Log.i(TAG, "Cleaning up offline metadata and assets for series $key (all episodes removed)")
                offlineMediaDao.deleteMedia(key)
                offlineMediaDao.deleteSeasons(key)
                offlineMediaDao.deleteEpisodes(key)
                deleteAssetDir(mediaType, key)
            } else {
                Log.i(TAG, "Preserving series offline metadata for $key (${remainingSeriesDownloads.size} episodes still downloaded)")
            }
        }
    }

    override suspend fun deleteSeason(seriesKey: String, seasonNumber: Int): Unit = withContext(Dispatchers.IO) {
        val seriesId = seriesKey.removePrefix("tv_")
        val seriesDownloads = downloadDao.getDownloadsBySeriesSync(seriesId)
        val targetSeasonDownloads = seriesDownloads.filter { it.seasonNumber == seasonNumber }

        for (dl in targetSeasonDownloads) {
            try {
                val file = File(dl.filePath)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
            downloadDao.deleteDownload(dl.id)
        }

        offlineMediaDao.deleteEpisodesForSeason(seriesKey, seasonNumber)
        offlineMediaDao.deleteSeason(seriesKey, seasonNumber)

        val remaining = downloadDao.getDownloadsBySeriesSync(seriesId)
        if (remaining.isEmpty()) {
            offlineMediaDao.deleteMedia(seriesKey)
            deleteAssetDir("tv", seriesKey)
        }
    }

    private fun deleteAssetDir(mediaType: String, key: String) {
        try {
            val dir = getMediaAssetDir(mediaType, key)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed deleting asset directory for $key: ${e.message}")
        }
    }

    private fun resolveTmdbId(download: DownloadEntity): Long? {
        val effectiveSeriesId = download.effectiveSeriesId
        if (!effectiveSeriesId.isNullOrBlank()) {
            return effectiveSeriesId.toLongOrNull()
        }
        val mediaId = download.mediaId
        return if (mediaId.contains("_")) {
            mediaId.substringBefore('_').toLongOrNull()
        } else {
            mediaId.toLongOrNull()
        }
    }
}
