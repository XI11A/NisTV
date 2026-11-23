package com.nis.nistv.feature.downloads.subtitles

import androidx.media3.common.MimeTypes
import java.io.File
import java.util.Locale

data class DiscoveredSubtitle(
    val file: File,
    val label: String,
    val languageTag: String?,
    val mimeType: String
)

object SidecarSubtitleDetector {

    private val SUPPORTED_EXTENSIONS = mapOf(
        "srt" to MimeTypes.APPLICATION_SUBRIP,
        "vtt" to MimeTypes.TEXT_VTT,
        "ass" to MimeTypes.TEXT_SSA,
        "ssa" to MimeTypes.TEXT_SSA
    )

    private val KNOWN_LANGUAGES = mapOf(
        "en" to ("English" to "en"),
        "eng" to ("English" to "en"),
        "english" to ("English" to "en"),
        "hi" to ("Hindi" to "hi"),
        "hin" to ("Hindi" to "hi"),
        "hindi" to ("Hindi" to "hi"),
        "ta" to ("Tamil" to "ta"),
        "tam" to ("Tamil" to "ta"),
        "tamil" to ("Tamil" to "ta"),
        "te" to ("Telugu" to "te"),
        "tel" to ("Telugu" to "te"),
        "telugu" to ("Telugu" to "te"),
        "ml" to ("Malayalam" to "ml"),
        "mal" to ("Malayalam" to "ml"),
        "malayalam" to ("Malayalam" to "ml"),
        "kn" to ("Kannada" to "kn"),
        "kan" to ("Kannada" to "kn"),
        "kannada" to ("Kannada" to "kn"),
        "bn" to ("Bengali" to "bn"),
        "ben" to ("Bengali" to "bn"),
        "bengali" to ("Bengali" to "bn"),
        "mr" to ("Marathi" to "mr"),
        "mar" to ("Marathi" to "mr"),
        "marathi" to ("Marathi" to "mr"),
        "es" to ("Spanish" to "es"),
        "spa" to ("Spanish" to "es"),
        "fr" to ("French" to "fr"),
        "fre" to ("French" to "fr")
    )

    fun parseLanguageTagAndLabel(fileName: String, baseVideoName: String): Pair<String, String?> {
        val nameWithoutExt = fileName.substringBeforeLast('.')
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)

        val suffix = if (nameWithoutExt.startsWith(baseVideoName, ignoreCase = true)) {
            nameWithoutExt.substring(baseVideoName.length).trim('.', '_', '-', ' ')
        } else {
            nameWithoutExt
        }

        val tokens = suffix.split('.', '_', '-', ' ')
        for (token in tokens) {
            val normalized = token.lowercase(Locale.ROOT)
            val match = KNOWN_LANGUAGES[normalized]
            if (match != null) {
                return "${match.first} (${ext.uppercase(Locale.ROOT)})" to match.second
            }
        }

        return (if (suffix.isNotBlank()) suffix else fileName) to null
    }

    fun findSidecarSubtitles(videoFile: File): List<DiscoveredSubtitle> {
        val parentDir = videoFile.parentFile ?: return emptyList()
        val baseName = videoFile.nameWithoutExtension

        val candidateDirs = listOfNotNull(
            parentDir,
            File(parentDir, "Subs"),
            File(parentDir, "subs"),
            File(parentDir, "Subtitles"),
            File(parentDir, "subtitles")
        ).filter { it.exists() && it.isDirectory }

        val discovered = mutableListOf<DiscoveredSubtitle>()

        for (dir in candidateDirs) {
            val files = dir.listFiles() ?: continue
            for (f in files) {
                if (f.isFile) {
                    val ext = f.extension.lowercase(Locale.ROOT)
                    val mime = SUPPORTED_EXTENSIONS[ext]
                    if (mime != null) {
                        val isCandidate = if (dir == parentDir) {
                            f.nameWithoutExtension.startsWith(baseName, ignoreCase = true)
                        } else {
                            true
                        }
                        if (isCandidate) {
                            val (label, tag) = parseLanguageTagAndLabel(f.name, baseName)
                            discovered.add(
                                DiscoveredSubtitle(
                                    file = f,
                                    label = label,
                                    languageTag = tag,
                                    mimeType = mime
                                )
                            )
                        }
                    }
                }
            }
        }

        return discovered.distinctBy { it.file.absolutePath }
    }
}
