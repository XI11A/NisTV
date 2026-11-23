package com.nis.nistv.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.nis.nistv.MainActivity
import com.nis.nistv.R

object AppShortcutsHelper {

    private const val SHORTCUT_RESUME_ID = "shortcut_resume_channel"

    fun updateLastWatchedChannelShortcut(context: Context, channelId: String, channelName: String) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("shortcut_destination", "player")
                putExtra("channelId", channelId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val shortLabel = if (channelName.length > 15) {
                channelName.take(12) + "..."
            } else {
                channelName
            }

            val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_RESUME_ID)
                .setShortLabel("Resume: $shortLabel")
                .setLongLabel("Resume watching $channelName")
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_livetv))
                .setIntent(intent)
                .build()

            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        } catch (_: Exception) {
            // Shortcuts may fail on certain customized Android ROMs or restricted launchers
        }
    }
}
