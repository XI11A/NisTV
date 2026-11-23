package com.nis.nistv.data.epg

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ScheduleItem(
    val title: String,
    val timeString: String,
    val desc: String,
    val isCurrent: Boolean
)

data class ProgrammeInfo(
    val title: String = "Live Broadcast",
    val description: String = "Continuous live telecast on NisTV.",
    val timeString: String = "NOW PLAYING",
    val progressPercent: Int = 45,
    val upNextTitle: String = "Upcoming Program",
    val upNextTime: String = "",
    val upcomingSchedule: List<ScheduleItem> = emptyList()
)

class EpgManager(private val context: Context) {

    private var epgJson: JSONObject? = null
    private var mappingJson: JSONObject? = null
    private var programsJson: JSONObject? = null

    private val xmltvDateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private var isLoaded = false

    suspend fun ensureLoaded() = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext
        try {
            val inputStream: InputStream = context.assets.open("epg/epg_data.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonStr = String(buffer, Charsets.UTF_8)
            val root = JSONObject(jsonStr)
            epgJson = root
            if (root.has("mapping") && root.has("programs")) {
                mappingJson = root.getJSONObject("mapping")
                programsJson = root.getJSONObject("programs")
            }
            isLoaded = true
        } catch (e: Exception) {
            epgJson = JSONObject()
            isLoaded = true
        }
    }

    suspend fun getProgrammeForChannel(
        channelId: String,
        channelName: String,
        category: String = "General"
    ): ProgrammeInfo = withContext(Dispatchers.IO) {
        ensureLoaded()

        val progs = findProgramArray(channelId, channelName)
        if (progs != null && progs.length() > 0) {
            val now = System.currentTimeMillis()
            var currentIndex = -1
            var currentTitle = channelName
            var currentDesc = "Enjoy live continuous broadcast on NisTV."
            var timeString = "NOW PLAYING"
            var progressPercent = 50

            for (i in 0 until progs.length()) {
                try {
                    val item = progs.getJSONObject(i)
                    val startStr = item.optString("start")
                    val stopStr = item.optString("stop")
                    val startTime = parseDate(startStr)
                    val stopTime = parseDate(stopStr)

                    if (now in startTime until stopTime) {
                        currentIndex = i
                        currentTitle = item.getString("title")
                        val desc = item.optString("desc", "")
                        currentDesc = if (desc.isNotEmpty()) desc else currentDesc

                        val startFormatted = displayTimeFormat.format(Date(startTime))
                        val stopFormatted = displayTimeFormat.format(Date(stopTime))
                        timeString = "$startFormatted - $stopFormatted"

                        val duration = stopTime - startTime
                        if (duration > 0) {
                            val elapsed = now - startTime
                            progressPercent = ((elapsed * 100) / duration).toInt().coerceIn(0, 100)
                        }
                        break
                    }
                } catch (_: Exception) {}
            }

            // If outside exact bounds, default to nearest item
            if (currentIndex == -1 && progs.length() > 0) {
                currentIndex = 0
                val item = progs.getJSONObject(0)
                currentTitle = item.optString("title", channelName)
                currentDesc = item.optString("desc", currentDesc)
                val st = parseDate(item.optString("start"))
                val et = parseDate(item.optString("stop"))
                timeString = "${displayTimeFormat.format(Date(st))} - ${displayTimeFormat.format(Date(et))}"
            }

            var upNextTitle = "Upcoming Show"
            var upNextTime = ""
            if (currentIndex + 1 < progs.length()) {
                val nextItem = progs.getJSONObject(currentIndex + 1)
                upNextTitle = nextItem.optString("title", "Upcoming Program")
                val nst = parseDate(nextItem.optString("start"))
                upNextTime = displayTimeFormat.format(Date(nst))
            }

            val upcoming = mutableListOf<ScheduleItem>()
            val maxIdx = (currentIndex + 8).coerceAtMost(progs.length())
            for (i in currentIndex until maxIdx) {
                try {
                    val item = progs.getJSONObject(i)
                    val t = item.optString("title", "Program")
                    val d = item.optString("desc", "")
                    val st = parseDate(item.optString("start"))
                    val et = parseDate(item.optString("stop"))
                    val tStr = "${displayTimeFormat.format(Date(st))} - ${displayTimeFormat.format(Date(et))}"
                    upcoming.add(ScheduleItem(t, tStr, d, i == currentIndex))
                } catch (_: Exception) {}
            }

            if (upcoming.isNotEmpty()) {
                return@withContext ProgrammeInfo(
                    title = currentTitle,
                    description = currentDesc,
                    timeString = timeString,
                    progressPercent = progressPercent,
                    upNextTitle = upNextTitle,
                    upNextTime = upNextTime,
                    upcomingSchedule = upcoming
                )
            }
        }

        // Dynamic category schedule fallback
        generateDynamicSchedule(channelName, category)
    }

    private fun findProgramArray(tvgId: String?, channelName: String?): JSONArray? {
        val root = epgJson ?: return null
        val mapping = mappingJson
        val programs = programsJson

        try {
            if (mapping != null && programs != null) {
                var targetId: String? = null

                if (!channelName.isNullOrBlank()) {
                    val k1 = channelName.lowercase().trim()
                    val k2 = cleanKey(channelName)
                    targetId = when {
                        mapping.has(k1) -> mapping.getString(k1)
                        mapping.has(k2) -> mapping.getString(k2)
                        else -> null
                    }
                }

                if (targetId == null && !tvgId.isNullOrBlank()) {
                    val k1 = tvgId.lowercase().trim()
                    val k2 = cleanKey(tvgId)
                    targetId = when {
                        mapping.has(k1) -> mapping.getString(k1)
                        mapping.has(k2) -> mapping.getString(k2)
                        else -> null
                    }
                }

                if (targetId != null && programs.has(targetId)) {
                    return programs.getJSONArray(targetId)
                }
            }

            if (!tvgId.isNullOrBlank() && root.has(tvgId)) {
                return root.getJSONArray(tvgId)
            }
            if (!channelName.isNullOrBlank() && root.has(channelName)) {
                return root.getJSONArray(channelName)
            }
        } catch (_: Exception) {}

        return null
    }

    private fun cleanKey(raw: String): String {
        var s = raw.lowercase()
        val strip = listOf("(1080p)", "(720p)", "(576p)", "(480p)", "hd", "sd", "uk", "apac", "us", "india", "live", "tv", "channel", ".in", "@")
        for (w in strip) {
            s = s.replace(w, "")
        }
        return s.replace(Regex("[^a-z0-9]"), "")
    }

    private fun generateDynamicSchedule(channelName: String, category: String): ProgrammeInfo {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        val cat = category.lowercase()
        val titles: Array<String>
        val descs: Array<String>

        when {
            cat.contains("news") -> {
                titles = arrayOf(
                    "Morning Express Bulletin", "Breakfast Headlines & Debate", "State Fast News 100",
                    "Special Midday Report", "Prime Time Focus", "The Big Debate Live",
                    "Top 50 News Roundup", "Late Night Special Edition"
                )
                descs = arrayOf(
                    "Top headlines from across the state and nation.", "In-depth panel debate on today's breaking story.",
                    "Non-stop 100 breaking news items in 30 minutes.", "Detailed midday news analysis and live field reports.",
                    "Comprehensive evening news report with expert analysis.", "Live debates featuring prominent politicians and journalists.",
                    "Fast-paced news recap of the day's major events.", "Late night exclusive stories and international developments."
                )
            }
            cat.contains("music") -> {
                titles = arrayOf(
                    "Morning Melodies & Hits", "Super Hit Chartbusters", "Golden Era Classics",
                    "Non-Stop Party Beats", "Melody Express", "Star Special Playlist",
                    "Late Night Soft Tracks", "Midnight Melodies"
                )
                descs = arrayOf(
                    "Kickstart your day with soothing, melodious hit songs.", "The hottest trending chartbusters of the week.",
                    "Timeless retro classics from evergreen legendary singers.", "High-energy beats to power through your afternoon.",
                    "A handpicked curation of soul-stirring melodies.", "Exclusive back-to-back songs of your favourite superstars.",
                    "Unwind with soft acoustic and relaxing ambient melodies.", "Continuous late-night romantic songs on NisTV."
                )
            }
            cat.contains("movie") -> {
                titles = arrayOf(
                    "Morning Matinee Special", "Blockbuster Premiere", "Superhit Afternoon Movie",
                    "Comedy Ka Gallatta", "Mega Blockbuster Evening", "Action Blast Prime Time",
                    "Late Night Thriller", "Midnight Cinema"
                )
                descs = arrayOf(
                    "A nostalgic, heart-warming morning family movie.", "Critically acclaimed blockbuster movie with star-studded cast.",
                    "Action-packed entertainer starring leading superstars.", "Hilarious comedy movie scenes that guarantee non-stop laughs.",
                    "Massive pan-Indian blockbuster with cutting-edge visuals.", "High-octane action thriller packed with thrilling stunts.",
                    "Suspense-filled edge-of-the-seat thriller.", "Popular late-night cinema to conclude your day."
                )
            }
            cat.contains("sport") -> {
                titles = arrayOf(
                    "Sports Center Morning", "Match Highlights & Analysis", "Live Game Preview",
                    "Cricket Extra Time", "Championship Arena", "Prime Time Sports Bulletin",
                    "Golden Moments of Sports", "Late Night Highlights"
                )
                descs = arrayOf(
                    "Daily morning sports news and scores from around the globe.", "Comprehensive replay and expert breakdown of recent games.",
                    "Pre-match discussion, team line-ups and player conditions.", "Special focus on Indian cricket and international series.",
                    "Live thrilling action from top sporting tournaments.", "Wrap-up of today's key sports developments and rankings.",
                    "Iconic classic sporting matches that made history.", "Late-night game recaps and interview soundbites."
                )
            }
            cat.contains("kid") -> {
                titles = arrayOf(
                    "Morning Cartoon Fiesta", "Toon Time Adventure", "Superhero Power Hour",
                    "Magical Kingdom Tales", "Afternoon Fun Laughs", "Animated Movie Feature",
                    "Bedtime Story Toons", "Night Animation Hour"
                )
                descs = arrayOf(
                    "Bright, colourful cartoons to wake up happy.", "Join lovable animated characters on thrilling adventures.",
                    "Action-packed animated superhero missions.", "Enchanting fairy tales and moral stories for kids.",
                    "Slapstick animated comedy that children love.", "Feature-length animated blockbuster presentation.",
                    "Gentle, soothing cartoons to wind down before sleep.", "Classic fun cartoons for the young at heart."
                )
            }
            else -> {
                titles = arrayOf(
                    "Devotional Beginnings", "Morning Serial Drama", "Celebrity Talk & Lifestyle",
                    "Afternoon Mega Serial", "Comedy Hour Extra", "Prime Time Drama Special",
                    "Maha Sangamam Mega Serial", "Late Night Reality Highlights"
                )
                descs = arrayOf(
                    "Soothing spiritual chants and divine blessings for the day.", "Emotional family drama following the journey of resilient heroes.",
                    "Fun-filled lifestyle segment with celebrity interviews.", "Engaging daily serial packed with twists and family emotions.",
                    "Laugh out loud with top comedy skits and witty humour.", "The state's highest-rated family serial drama.",
                    "Special joint episode bringing together favourite serial stars.", "Exclusive behind-the-scenes and best moments of the week."
                )
            }
        }

        val slot = (hour / 3) % titles.size
        val slotStartHour = (hour / 3) * 3
        val slotEndHour = slotStartHour + 3
        val timeString = "${formatHour(slotStartHour)} - ${formatHour(slotEndHour)}"

        val totalMins = 3 * 60
        val elapsedMins = ((hour - slotStartHour) * 60) + minute
        val progressPercent = ((elapsedMins * 100) / totalMins).coerceIn(5, 100)

        val nextSlot = (slot + 1) % titles.size
        val upNextTitle = titles[nextSlot]
        val upNextTime = formatHour(slotEndHour)

        val upcoming = mutableListOf<ScheduleItem>()
        for (i in 0 until 6) {
            val idx = (slot + i) % titles.size
            val sh = (slotStartHour + (i * 3)) % 24
            val eh = (sh + 3) % 24
            val tStr = "${formatHour(sh)} - ${formatHour(eh)}"
            upcoming.add(ScheduleItem(titles[idx], tStr, descs[idx], i == 0))
        }

        return ProgrammeInfo(
            title = titles[slot],
            description = descs[slot],
            timeString = timeString,
            progressPercent = progressPercent,
            upNextTitle = upNextTitle,
            upNextTime = upNextTime,
            upcomingSchedule = upcoming
        )
    }

    suspend fun getTimelineForChannel(
        channelId: String,
        channelName: String,
        category: String = "General",
        windowStart: Long,
        windowEnd: Long
    ): List<TimelineProgram> = withContext(Dispatchers.IO) {
        ensureLoaded()

        val progs = findProgramArray(channelId, channelName)
        val rawList = mutableListOf<TimelineProgram>()

        if (progs != null && progs.length() > 0) {
            for (i in 0 until progs.length()) {
                try {
                    val item = progs.getJSONObject(i)
                    val st = parseDate(item.optString("start"))
                    val et = parseDate(item.optString("stop"))
                    if (et > windowStart && st < windowEnd) {
                        rawList.add(
                            TimelineProgram(
                                title = item.optString("title", channelName),
                                description = item.optString("desc", ""),
                                startEpochMs = st,
                                endEpochMs = et,
                                isEstimated = false,
                                isPlaceholder = false
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
        }

        if (rawList.isEmpty()) {
            // Generate synthetic estimated blocks for the window in 30-min to 1-hour slots
            val calendar = Calendar.getInstance().apply { timeInMillis = windowStart }
            var cursor = windowStart
            val cat = category.lowercase()
            val titles = when {
                cat.contains("news") -> arrayOf("Live News Desk", "Headlines at This Hour", "World Today", "Special Report")
                cat.contains("music") -> arrayOf("Morning Hits", "Chartbusters", "Top Tracks", "Sound Lounge")
                cat.contains("movie") -> arrayOf("Cinema Showcase", "Feature Film", "Matinee Movie", "Blockbuster")
                cat.contains("sport") -> arrayOf("Sports Extra", "Live Highlights", "Match Action", "Game Desk")
                cat.contains("kid") -> arrayOf("Kids Hour", "Cartoons", "Adventures", "Toon Time")
                else -> arrayOf("Live Broadcast", "Channel Special", "Prime Show", "Featured Program")
            }

            var idx = 0
            while (cursor < windowEnd) {
                val blockDuration = 60 * 60 * 1000L // 1 hour block
                val nextCursor = (cursor + blockDuration).coerceAtMost(windowEnd)
                val title = titles[idx % titles.size]
                rawList.add(
                    TimelineProgram(
                        title = title,
                        description = "Continuous telecast on $channelName ($category)",
                        startEpochMs = cursor,
                        endEpochMs = nextCursor,
                        isEstimated = true,
                        isPlaceholder = false
                    )
                )
                cursor = nextCursor
                idx++
            }
        }

        EpgTimelineHelper.buildEpgTimeline(rawList, windowStart, windowEnd)
    }

    private fun formatHour(h24: Int): String {
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        val ampm = if (h24 >= 12) "PM" else "AM"
        return String.format(Locale.getDefault(), "%02d:00 %s", h12, ampm)
    }

    private fun parseDate(raw: String): Long {
        if (raw.isBlank()) return System.currentTimeMillis()
        return try {
            xmltvDateFormat.parse(raw)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
