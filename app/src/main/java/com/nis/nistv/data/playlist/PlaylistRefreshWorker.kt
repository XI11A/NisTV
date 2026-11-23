package com.nis.nistv.data.playlist

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nis.nistv.NisTvApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaylistRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as? NisTvApplication ?: return@withContext Result.failure()
        val playlistId = inputData.getString("playlist_id") ?: return@withContext Result.failure()

        val playlistDao = app.container.database.playlistDao()
        val playlist = playlistDao.getPlaylistByIdSync(playlistId) ?: return@withContext Result.success()

        if (!playlist.enabled || !playlist.autoRefresh) {
            return@withContext Result.success()
        }

        val syncResult = app.container.playlistSyncManager.syncPlaylist(playlist)
        if (syncResult.isSuccess) {
            Result.success()
        } else {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
