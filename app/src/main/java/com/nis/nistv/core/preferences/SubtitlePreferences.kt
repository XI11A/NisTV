package com.nis.nistv.core.preferences

import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.media3.ui.CaptionStyleCompat

data class SubtitleStylePreferences(
    val textSize: SubtitleTextSize = SubtitleTextSize.NORMAL,
    val textColor: SubtitleColor = SubtitleColor.WHITE,
    val backgroundColor: SubtitleBackground = SubtitleBackground.SEMI_TRANSPARENT,
    val edgeStyle: SubtitleEdgeStyle = SubtitleEdgeStyle.OUTLINE,
    val fontStyle: SubtitleFontStyle = SubtitleFontStyle.SANS_SERIF,
    val customPreviewText: String = "Here is how your subtitles will look during playback"
) {
    fun toCaptionStyleCompat(): CaptionStyleCompat {
        return CaptionStyleCompat(
            textColor.argb,
            backgroundColor.argb,
            AndroidColor.TRANSPARENT,
            edgeStyle.edgeType,
            AndroidColor.BLACK,
            fontStyle.typeface
        )
    }
}

enum class SubtitleTextSize(val label: String, val scale: Float) {
    SMALL("Small (75%)", 0.75f),
    NORMAL("Normal (100%)", 1.0f),
    LARGE("Large (125%)", 1.25f),
    EXTRA_LARGE("Extra Large (150%)", 1.5f)
}

enum class SubtitleColor(val label: String, val color: Color, val argb: Int) {
    WHITE("White", Color.White, AndroidColor.WHITE),
    YELLOW("Yellow", Color(0xFFFFEB3B), AndroidColor.YELLOW),
    CYAN("Cyan", Color(0xFF00E5FF), AndroidColor.CYAN),
    GREEN("Green", Color(0xFF00E676), AndroidColor.GREEN)
}

enum class SubtitleBackground(val label: String, val color: Color, val argb: Int) {
    TRANSPARENT("None", Color.Transparent, AndroidColor.TRANSPARENT),
    SEMI_TRANSPARENT("Semi-Black", Color(0x99000000), AndroidColor.argb(140, 0, 0, 0)),
    SOLID_BLACK("Solid Black", Color.Black, AndroidColor.BLACK)
}

enum class SubtitleEdgeStyle(val label: String, val edgeType: Int) {
    NONE("None", CaptionStyleCompat.EDGE_TYPE_NONE),
    OUTLINE("Outline", CaptionStyleCompat.EDGE_TYPE_OUTLINE),
    DROP_SHADOW("Drop Shadow", CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW),
    RAISED("Raised", CaptionStyleCompat.EDGE_TYPE_RAISED)
}

enum class SubtitleFontStyle(val label: String, val typeface: Typeface) {
    SANS_SERIF("Sans-Serif", Typeface.SANS_SERIF),
    SERIF("Serif", Typeface.SERIF),
    MONOSPACE("Monospace", Typeface.MONOSPACE),
    BOLD("Bold", Typeface.DEFAULT_BOLD)
}
