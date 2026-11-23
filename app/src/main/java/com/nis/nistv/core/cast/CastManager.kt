package com.nis.nistv.core.cast

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.net.Uri
import android.provider.Settings
import android.view.Display
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Screencasting, Wireless Display detection, and External Stream Casting.
 */
class CastManager private constructor(private val appContext: Context) {

    private val displayManager: DisplayManager? =
        appContext.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

    private val _isExternalDisplayConnected = MutableStateFlow(false)
    val isExternalDisplayConnected: StateFlow<Boolean> = _isExternalDisplayConnected.asStateFlow()

    private val _connectedDisplayName = MutableStateFlow<String?>(null)
    val connectedDisplayName: StateFlow<String?> = _connectedDisplayName.asStateFlow()

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            checkPresentationDisplays()
        }

        override fun onDisplayRemoved(displayId: Int) {
            checkPresentationDisplays()
        }

        override fun onDisplayChanged(displayId: Int) {
            checkPresentationDisplays()
        }
    }

    init {
        try {
            displayManager?.registerDisplayListener(displayListener, null)
            checkPresentationDisplays()
        } catch (_: Exception) {
            // Ignore display listener failure on restricted environments
        }
    }

    /**
     * Checks if any external or wireless display is connected.
     */
    fun checkPresentationDisplays() {
        val dm = displayManager ?: return
        try {
            val presentationDisplays = dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            if (presentationDisplays.isNotEmpty()) {
                val display = presentationDisplays.first()
                _isExternalDisplayConnected.value = true
                _connectedDisplayName.value = display.name ?: "Wireless Display"
            } else {
                _isExternalDisplayConnected.value = false
                _connectedDisplayName.value = null
            }
        } catch (_: Exception) {
            _isExternalDisplayConnected.value = false
            _connectedDisplayName.value = null
        }
    }

    /**
     * Opens Android System Screencast / Cast / Smart View / Wireless Display settings.
     */
    fun openSystemCastSettings(context: Context) {
        val castIntents = listOf(
            Intent("android.settings.CAST_SETTINGS"),
            Intent("android.settings.WIFI_DISPLAY_SETTINGS"),
            Intent(Settings.ACTION_WIRELESS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        for (intent in castIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // Try next intent
            }
        }

        Toast.makeText(context, "Could not open Cast settings on this device", Toast.LENGTH_SHORT).show()
    }

    /**
     * Casts the active stream URL to external DLNA / Chromecast receiver apps (e.g. VLC, Web Video Caster, BubbleUPnP).
     */
    fun castStreamToExternalApp(context: Context, streamUrl: String, title: String) {
        if (streamUrl.isBlank()) {
            Toast.makeText(context, "No active stream to cast", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(streamUrl), "video/*")
                putExtra("title", title)
                putExtra(Intent.EXTRA_TITLE, title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Cast / Stream with:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No cast/video player apps found: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Copies active stream link to clipboard.
     */
    fun copyStreamLink(context: Context, streamUrl: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("NisTV Stream URL", streamUrl)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, "Stream URL copied to clipboard", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "Failed to copy stream URL", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: CastManager? = null

        fun getInstance(context: Context): CastManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CastManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
