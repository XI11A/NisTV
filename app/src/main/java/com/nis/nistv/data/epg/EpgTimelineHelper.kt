package com.nis.nistv.data.epg

data class TimelineProgram(
    val title: String,
    val description: String = "",
    val startEpochMs: Long,
    val endEpochMs: Long,
    val isEstimated: Boolean = false,
    val isPlaceholder: Boolean = false
) {
    val durationMs: Long get() = (endEpochMs - startEpochMs).coerceAtLeast(0L)
}

object EpgTimelineHelper {

    private const val PLACEHOLDER_TITLE = "No Programme Information"

    fun buildEpgTimeline(
        programs: List<TimelineProgram>,
        windowStart: Long,
        windowEnd: Long
    ): List<TimelineProgram> {
        if (windowEnd <= windowStart) return emptyList()

        if (programs.isEmpty()) {
            return listOf(
                TimelineProgram(
                    title = PLACEHOLDER_TITLE,
                    startEpochMs = windowStart,
                    endEpochMs = windowEnd,
                    isEstimated = true,
                    isPlaceholder = true
                )
            )
        }

        // Sort by start time
        val sorted = programs.sortedBy { it.startEpochMs }
        val resolved = mutableListOf<TimelineProgram>()

        // 1. Resolve overlaps and filter out items outside the window
        var currentEnd = windowStart
        for (prog in sorted) {
            if (prog.endEpochMs <= windowStart || prog.startEpochMs >= windowEnd) {
                continue
            }

            val clampedStart = prog.startEpochMs.coerceAtLeast(windowStart)
            val clampedEnd = prog.endEpochMs.coerceAtMost(windowEnd)

            if (clampedEnd <= clampedStart) continue

            // If there's an overlap with previous item, resolve it
            if (resolved.isNotEmpty()) {
                val last = resolved.last()
                if (clampedStart < last.endEpochMs) {
                    // Overlap: adjust last item's end time to clampedStart
                    val trimmedLast = last.copy(endEpochMs = clampedStart)
                    resolved[resolved.size - 1] = trimmedLast
                }
            }

            resolved.add(prog.copy(startEpochMs = clampedStart, endEpochMs = clampedEnd))
        }

        if (resolved.isEmpty()) {
            return listOf(
                TimelineProgram(
                    title = PLACEHOLDER_TITLE,
                    startEpochMs = windowStart,
                    endEpochMs = windowEnd,
                    isEstimated = true,
                    isPlaceholder = true
                )
            )
        }

        // 2. Fill gaps with "No Programme Information"
        val resultWithGaps = mutableListOf<TimelineProgram>()
        var cursor = windowStart

        for (item in resolved) {
            if (item.startEpochMs > cursor) {
                // Gap detected before item
                resultWithGaps.add(
                    TimelineProgram(
                        title = PLACEHOLDER_TITLE,
                        startEpochMs = cursor,
                        endEpochMs = item.startEpochMs,
                        isEstimated = true,
                        isPlaceholder = true
                    )
                )
            }
            resultWithGaps.add(item)
            cursor = item.endEpochMs
        }

        if (cursor < windowEnd) {
            // Gap detected at the end
            resultWithGaps.add(
                TimelineProgram(
                    title = PLACEHOLDER_TITLE,
                    startEpochMs = cursor,
                    endEpochMs = windowEnd,
                    isEstimated = true,
                    isPlaceholder = true
                )
            )
        }

        return resultWithGaps
    }
}
