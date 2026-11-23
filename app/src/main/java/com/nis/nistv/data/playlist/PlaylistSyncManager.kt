package com.nis.nistv.data.playlist

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nis.nistv.core.database.NisTvDatabase
import com.nis.nistv.core.database.entity.PlaylistEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class PlaylistSyncManager(
    private val context: Context,
    private val database: NisTvDatabase,
    private val okHttpClient: OkHttpClient
) {
    private val playlistDao = database.playlistDao()
    private val channelDao = database.channelDao()

    suspend fun addPlaylist(
        name: String,
        sourceType: String,
        urlOrPath: String,
        autoRefresh: Boolean = true
    ): Result<PlaylistEntity> = withContext(Dispatchers.IO) {
        try {
            val id = "pl_${UUID.randomUUID().toString().take(8)}"
            val entity = PlaylistEntity(
                id = id,
                name = name.trim().ifEmpty { "My Playlist" },
                sourceType = sourceType,
                url = urlOrPath.trim(),
                autoRefresh = autoRefresh,
                enabled = true
            )
            playlistDao.insertPlaylist(entity)
            val syncResult = syncPlaylist(entity)
            if (syncResult.isSuccess) {
                if (autoRefresh && sourceType == "URL") {
                    schedulePeriodicSync(id)
                }
                Result.success(entity)
            } else {
                Result.failure(syncResult.exceptionOrNull() ?: Exception("Failed to sync playlist"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncPlaylist(playlistId: String): Result<ParsedPlaylistResult> = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getPlaylistByIdSync(playlistId)
            ?: return@withContext Result.failure(Exception("Playlist not found: $playlistId"))
        syncPlaylist(playlist)
    }

    suspend fun syncPlaylist(playlist: PlaylistEntity): Result<ParsedPlaylistResult> = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = when (playlist.sourceType) {
                "URL" -> {
                    val request = Request.Builder()
                        .url(playlist.url)
                        .header("User-Agent", "NisTV/2.0")
                        .build()
                    val response = okHttpClient.newCall(request).execute()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                    }
                    response.body?.byteStream() ?: return@withContext Result.failure(Exception("Empty response body"))
                }
                "FILE" -> {
                    val uri = Uri.parse(playlist.url)
                    if (uri.scheme == "content") {
                        context.contentResolver.openInputStream(uri)
                            ?: return@withContext Result.failure(Exception("Could not open content URI: ${playlist.url}"))
                    } else {
                        val file = File(uri.path ?: playlist.url)
                        if (!file.exists()) {
                            return@withContext Result.failure(Exception("File not found: ${playlist.url}"))
                        }
                        file.inputStream()
                    }
                }
                else -> return@withContext Result.failure(Exception("Unsupported playlist sourceType: ${playlist.sourceType}"))
            }

            inputStream.use { stream ->
                val result = M3uParser.parse(
                    inputStream = stream,
                    playlistId = playlist.id,
                    playlistName = playlist.name
                )

                // Atomic database update: clear old channels for this playlist and insert parsed ones
                playlistDao.deleteChannelsForPlaylist(playlist.id)
                if (result.channels.isNotEmpty()) {
                    channelDao.insertChannels(result.channels)
                }
                if (result.streams.isNotEmpty()) {
                    channelDao.insertStreams(result.streams)
                }

                playlistDao.updatePlaylistSyncStatus(
                    id = playlist.id,
                    channelCount = result.importedCount,
                    lastSyncedAt = System.currentTimeMillis()
                )

                Result.success(result)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        cancelPeriodicSync(playlistId)
        playlistDao.deletePlaylistCascade(playlistId)
    }

    suspend fun setPlaylistEnabled(playlistId: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        playlistDao.setPlaylistEnabled(playlistId, enabled)
        if (enabled) {
            val playlist = playlistDao.getPlaylistByIdSync(playlistId)
            if (playlist?.autoRefresh == true && playlist.sourceType == "URL") {
                schedulePeriodicSync(playlistId)
            }
        } else {
            cancelPeriodicSync(playlistId)
        }
    }

    suspend fun renamePlaylist(playlistId: String, newName: String) = withContext(Dispatchers.IO) {
        playlistDao.updatePlaylistName(playlistId, newName.trim())
    }

    fun schedulePeriodicSync(playlistId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<PlaylistRefreshWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInputData(workDataOf("playlist_id" to playlistId))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "playlist_sync_$playlistId",
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }

    fun cancelPeriodicSync(playlistId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("playlist_sync_$playlistId")
    }
}
