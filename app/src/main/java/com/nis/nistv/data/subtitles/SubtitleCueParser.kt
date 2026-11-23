package com.nis.nistv.data.subtitles

data class SubtitleCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

object SubtitleCueParser {

    /**
     * Parses raw subtitle content in SRT, WebVTT, or ASS/SSA format into a list of [SubtitleCue]s.
     */
    fun parse(rawContent: String, extension: String? = null): List<SubtitleCue> {
        if (rawContent.isBlank()) return emptyList()

        // Strip leading UTF-8 BOM if present
        val content = rawContent.removePrefix("\uFEFF").trim()

        return when {
            extension.equals("ass", ignoreCase = true) || extension.equals("ssa", ignoreCase = true) ||
                    content.contains("[Events]", ignoreCase = true) -> parseAss(content)
            extension.equals("vtt", ignoreCase = true) ||
                    content.startsWith("WEBVTT", ignoreCase = true) -> parseVtt(content)
            else -> parseSrt(content)
        }
    }

    /**
     * Searches for the active cue at [positionMs] factoring in [delayMs].
     * (Positive delay shifts subtitle forward in time: subtitle appears later).
     */
    fun findActiveCue(cues: List<SubtitleCue>, positionMs: Long, delayMs: Long = 0L): SubtitleCue? {
        if (cues.isEmpty()) return null
        val effectivePosition = positionMs - delayMs
        return cues.firstOrNull { cue ->
            effectivePosition in cue.startTimeMs..cue.endTimeMs
        }
    }

    /**
     * SRT Parser
     */
    internal fun parseSrt(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val blocks = content.replace("\r\n", "\n").replace("\r", "\n").split(Regex("""\n\s*\n"""))

        val timeRegex = Regex("""(\d{1,2}):(\d{2}):(\d{2})[,.](\d{3})\s*-->\s*(\d{1,2}):(\d{2}):(\d{2})[,.](\d{3})""")

        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size < 2) continue

            // Find line with timestamp arrow
            val timeLineIndex = lines.indexOfFirst { it.contains("-->") }
            if (timeLineIndex == -1) continue

            val match = timeRegex.find(lines[timeLineIndex]) ?: continue
            val startMs = parseTimestampToMs(
                match.groupValues[1].toInt(),
                match.groupValues[2].toInt(),
                match.groupValues[3].toInt(),
                match.groupValues[4].toInt()
            )
            val endMs = parseTimestampToMs(
                match.groupValues[5].toInt(),
                match.groupValues[6].toInt(),
                match.groupValues[7].toInt(),
                match.groupValues[8].toInt()
            )

            val textLines = lines.drop(timeLineIndex + 1)
            val text = textLines.joinToString("\n").replace(Regex("""<[^>]*>"""), "").trim()

            if (text.isNotEmpty() && endMs > startMs) {
                cues.add(SubtitleCue(startMs, endMs, text))
            }
        }

        return cues.sortedBy { it.startTimeMs }
    }

    /**
     * WebVTT Parser
     */
    internal fun parseVtt(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val normalized = content.replace("\r\n", "\n").replace("\r", "\n")
        val blocks = normalized.split(Regex("""\n\s*\n"""))

        val vttRegex = Regex("""(?:(\d{1,2}):)?(\d{2}):(\d{2})[.](\d{3})\s*-->\s*(?:(\d{1,2}):)?(\d{2}):(\d{2})[.](\d{3})""")

        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val timeIndex = lines.indexOfFirst { it.contains("-->") }
            if (timeIndex == -1) continue

            val match = vttRegex.find(lines[timeIndex]) ?: continue
            val startHours = match.groupValues[1].ifEmpty { "0" }.toInt()
            val startMin = match.groupValues[2].toInt()
            val startSec = match.groupValues[3].toInt()
            val startMs = match.groupValues[4].toInt()

            val endHours = match.groupValues[5].ifEmpty { "0" }.toInt()
            val endMin = match.groupValues[6].toInt()
            val endSec = match.groupValues[7].toInt()
            val endMs = match.groupValues[8].toInt()

            val startTime = parseTimestampToMs(startHours, startMin, startSec, startMs)
            val endTime = parseTimestampToMs(endHours, endMin, endSec, endMs)

            val text = lines.drop(timeIndex + 1).joinToString("\n")
                .replace(Regex("""<[^>]*>"""), "")
                .trim()

            if (text.isNotEmpty() && endTime > startTime) {
                cues.add(SubtitleCue(startTime, endTime, text))
            }
        }

        return cues.sortedBy { it.startTimeMs }
    }

    /**
     * ASS/SSA Parser
     * Dialogue: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
     */
    internal fun parseAss(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.replace("\r\n", "\n").replace("\r", "\n").lines()

        var inEvents = false
        var textFieldIndex = 9 // Default ASS Dialogue index for Text

        val timeRegex = Regex("""(\d+):(\d{2}):(\d{2})\.(\d{2})""")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.equals("[Events]", ignoreCase = true)) {
                inEvents = true
                continue
            }
            if (!inEvents) continue

            if (trimmed.startsWith("Format:", ignoreCase = true)) {
                val formatHeaders = trimmed.substringAfter("Format:").split(",").map { it.trim().lowercase() }
                val idx = formatHeaders.indexOf("text")
                if (idx != -1) textFieldIndex = idx
                continue
            }

            if (trimmed.startsWith("Dialogue:", ignoreCase = true)) {
                val dialogueData = trimmed.substringAfter("Dialogue:").trim()
                val parts = dialogueData.split(",", limit = textFieldIndex + 1)
                if (parts.size <= textFieldIndex) continue

                val startStr = parts.getOrNull(1)?.trim() ?: continue
                val endStr = parts.getOrNull(2)?.trim() ?: continue
                val rawText = parts.getOrNull(textFieldIndex)?.trim() ?: continue

                val startMatch = timeRegex.find(startStr) ?: continue
                val endMatch = timeRegex.find(endStr) ?: continue

                // Centiseconds to milliseconds (e.g. 50 -> 500 ms)
                val startMs = parseTimestampToMs(
                    startMatch.groupValues[1].toInt(),
                    startMatch.groupValues[2].toInt(),
                    startMatch.groupValues[3].toInt(),
                    startMatch.groupValues[4].toInt() * 10
                )
                val endMs = parseTimestampToMs(
                    endMatch.groupValues[1].toInt(),
                    endMatch.groupValues[2].toInt(),
                    endMatch.groupValues[3].toInt(),
                    endMatch.groupValues[4].toInt() * 10
                )

                // Strip ASS styling tags {...} and handle line breaks
                val cleanText = rawText
                    .replace(Regex("""\{[^}]*\}"""), "")
                    .replace(Regex("""\\[Nn]"""), "\n")
                    .replace(Regex("""\\h"""), " ")
                    .trim()

                if (cleanText.isNotEmpty() && endMs > startMs) {
                    cues.add(SubtitleCue(startMs, endMs, cleanText))
                }
            }
        }

        return cues.sortedBy { it.startTimeMs }
    }

    private fun parseTimestampToMs(hours: Int, minutes: Int, seconds: Int, ms: Int): Long {
        return (hours * 3600_000L) + (minutes * 60_000L) + (seconds * 1000L) + ms
    }
}
