package com.nis.nistv

import com.nis.nistv.data.subtitles.SubtitleCue
import com.nis.nistv.data.subtitles.SubtitleCueParser
import org.junit.Assert.*
import org.junit.Test

class SubtitleCueParserTest {

    @Test
    fun testParseSrtFormat() {
        val srtContent = """
            1
            00:00:01,000 --> 00:00:04,500
            Hello, welcome to NisTV!

            2
            00:00:05,000 --> 00:00:08,000
            Enjoy high definition streaming.
        """.trimIndent()

        val cues = SubtitleCueParser.parseSrt(srtContent)
        assertEquals(2, cues.size)
        assertEquals(1000L, cues[0].startTimeMs)
        assertEquals(4500L, cues[0].endTimeMs)
        assertEquals("Hello, welcome to NisTV!", cues[0].text)

        assertEquals(5000L, cues[1].startTimeMs)
        assertEquals(8000L, cues[1].endTimeMs)
        assertEquals("Enjoy high definition streaming.", cues[1].text)
    }

    @Test
    fun testParseVttFormat() {
        val vttContent = """
            WEBVTT

            00:00:02.500 --> 00:00:06.000
            This is WebVTT format.

            00:00:07.000 --> 00:00:10.250
            Seamless subtitle playback.
        """.trimIndent()

        val cues = SubtitleCueParser.parseVtt(vttContent)
        assertEquals(2, cues.size)
        assertEquals(2500L, cues[0].startTimeMs)
        assertEquals(6000L, cues[0].endTimeMs)
        assertEquals("This is WebVTT format.", cues[0].text)

        assertEquals(7000L, cues[1].startTimeMs)
        assertEquals(10250L, cues[1].endTimeMs)
        assertEquals("Seamless subtitle playback.", cues[1].text)
    }

    @Test
    fun testParseAssFormatAndStripTags() {
        val assContent = """
            [Script Info]
            Title: NisTV Test

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.50,0:00:04.20,Default,,0,0,0,,{\an8\b1}Top bold subtitle{\b0}
            Dialogue: 0,0:00:05.00,0:00:07.80,Default,,0,0,0,,Second dialogue with \Nline break.
        """.trimIndent()

        val cues = SubtitleCueParser.parseAss(assContent)
        assertEquals(2, cues.size)
        assertEquals(1500L, cues[0].startTimeMs)
        assertEquals(4200L, cues[0].endTimeMs)
        assertEquals("Top bold subtitle", cues[0].text)

        assertEquals(5000L, cues[1].startTimeMs)
        assertEquals(7800L, cues[1].endTimeMs)
        assertEquals("Second dialogue with \nline break.", cues[1].text)
    }

    @Test
    fun testTamilAndIndicScriptsPreservation() {
        val tamilSrt = """
            1
            00:00:01,000 --> 00:00:03,000
            வணக்கம், நல்வரவு!

            2
            00:00:04,000 --> 00:00:06,000
            नमस्ते, आपका स्वागत है!
        """.trimIndent()

        val cues = SubtitleCueParser.parseSrt(tamilSrt)
        assertEquals(2, cues.size)
        assertEquals("வணக்கம், நல்வரவு!", cues[0].text)
        assertEquals("नमस्ते, आपका स्वागत है!", cues[1].text)
    }

    @Test
    fun testFindActiveCueWithDelay() {
        val cues = listOf(
            SubtitleCue(startTimeMs = 2000L, endTimeMs = 5000L, text = "Cue 1"),
            SubtitleCue(startTimeMs = 6000L, endTimeMs = 9000L, text = "Cue 2")
        )

        // 1. Without delay (delayMs = 0)
        assertNull(SubtitleCueParser.findActiveCue(cues, 1500L, 0L))
        assertEquals("Cue 1", SubtitleCueParser.findActiveCue(cues, 2000L, 0L)?.text)
        assertEquals("Cue 1", SubtitleCueParser.findActiveCue(cues, 3500L, 0L)?.text)
        assertEquals("Cue 1", SubtitleCueParser.findActiveCue(cues, 5000L, 0L)?.text)
        assertNull(SubtitleCueParser.findActiveCue(cues, 5500L, 0L))
        assertEquals("Cue 2", SubtitleCueParser.findActiveCue(cues, 7000L, 0L)?.text)

        // 2. Positive delay (+500 ms): subtitle appears 500ms later in video time
        // Position 2000ms: adjusted = 2000 - 500 = 1500ms -> not active yet
        assertNull(SubtitleCueParser.findActiveCue(cues, 2000L, 500L))
        // Position 2500ms: adjusted = 2500 - 500 = 2000ms -> Cue 1 becomes active
        assertEquals("Cue 1", SubtitleCueParser.findActiveCue(cues, 2500L, 500L)?.text)

        // 3. Negative delay (-500 ms): subtitle appears 500ms earlier
        // Position 1500ms: adjusted = 1500 - (-500) = 2000ms -> Cue 1 is active
        assertEquals("Cue 1", SubtitleCueParser.findActiveCue(cues, 1500L, -500L)?.text)
    }

    @Test
    fun testBomStripping() {
        val bomContent = "\uFEFF1\n00:00:01,000 --> 00:00:02,000\nBOM Clean"
        val cues = SubtitleCueParser.parse(bomContent, "srt")
        assertEquals(1, cues.size)
        assertEquals("BOM Clean", cues[0].text)
    }
}
