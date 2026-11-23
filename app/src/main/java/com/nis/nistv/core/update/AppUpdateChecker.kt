package com.nis.nistv.core.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.nis.nistv.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val releaseDate: String,
    val minSdk: Int,
    val apkUrl: String,
    val apkSize: String,
    val fileSizeBytes: Long,
    val sha256: String,
    val changelog: List<String>,
    val forceUpdate: Boolean
)

class AppUpdateChecker(
    private val endpointUrl: String = DEFAULT_UPDATE_URL
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(
        currentVersionCode: Int = BuildConfig.VERSION_CODE
    ): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(endpointUrl)
                .header("User-Agent", "NisTV-Android/${BuildConfig.VERSION_NAME} (Linux; Android)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyString = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyString)

                val remoteVersionCode = json.optInt("versionCode", 0)
                if (remoteVersionCode > currentVersionCode) {
                    val changelogList = mutableListOf<String>()
                    val changelogArray = json.optJSONArray("changelog")
                    if (changelogArray != null) {
                        for (i in 0 until changelogArray.length()) {
                            changelogList.add(changelogArray.optString(i))
                        }
                    }

                    return@withContext UpdateInfo(
                        versionName = json.optString("versionName", "New Version"),
                        versionCode = remoteVersionCode,
                        releaseDate = json.optString("releaseDate", "Latest"),
                        minSdk = json.optInt("minSdk", 24),
                        apkUrl = json.optString("apkUrl", ""),
                        apkSize = json.optString("apkSize", "Unknown"),
                        fileSizeBytes = json.optLong("fileSizeBytes", 0L),
                        sha256 = json.optString("sha256", ""),
                        changelog = changelogList,
                        forceUpdate = json.optBoolean("forceUpdate", false)
                    )
                }
            }
        } catch (_: Exception) {
            // Silently fail on network/DNS errors
        }
        return@withContext null
    }

    fun startDownloadAndInstall(context: Context, updateInfo: UpdateInfo) {
        if (updateInfo.apkUrl.isBlank()) {
            Toast.makeText(context, "Download link unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        var startedInDownloadManager = false
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (dm != null) {
                val directUrl = if (updateInfo.apkUrl.endsWith(".apk")) {
                    updateInfo.apkUrl
                } else {
                    "https://nistv.xi11a.eu.org/downloads/nistv-v${updateInfo.versionName}.apk"
                }
                val request = DownloadManager.Request(Uri.parse(directUrl)).apply {
                    setTitle("NisTV v${updateInfo.versionName}")
                    setDescription("Downloading update package...")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setMimeType("application/vnd.android.package-archive")
                    setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "nistv-v${updateInfo.versionName}.apk"
                    )
                }
                dm.enqueue(request)
                startedInDownloadManager = true
                Toast.makeText(
                    context,
                    "Downloading NisTV v${updateInfo.versionName} in notifications...",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (_: Exception) {
            startedInDownloadManager = false
        }

        // If system DownloadManager is not available, launch the browser to the dedicated download landing page
        if (!startedInDownloadManager) {
            try {
                val landingUrl = if (updateInfo.apkUrl.contains("nistv.xi11a.eu.org")) {
                    "https://nistv.xi11a.eu.org/?download=started&v=${updateInfo.versionName}"
                } else {
                    updateInfo.apkUrl
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(landingUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to launch download: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val DEFAULT_UPDATE_URL = "https://nistv.xi11a.eu.org/version.json"
    }
}
