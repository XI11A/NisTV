package com.nis.nistv

import com.nis.nistv.feature.downloads.subtitles.SidecarSubtitleDetector
import com.nis.nistv.feature.downloads.subtitles.SubtitleCharsetDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class SubtitleEncodingTest {

    @Test
    fun utf8WithBom_decodesTamilTextCorrectly() {
        val tamilText = "1\n00:00:01,000 --> 00:00:04,000\nவணக்கம்! NisTV தமிழ் வசனங்கள்"
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val textBytes = tamilText.toByteArray(StandardCharsets.UTF_8)
        val combined = bom + textBytes

        val decoded = SubtitleCharsetDetector.decodeToText(combined)
        assertEquals(tamilText, decoded)

        val cleanUtf8Bytes = SubtitleCharsetDetector.convertToUtf8Bytes(combined)
        assertTrue("Output should not start with BOM", cleanUtf8Bytes[0] != 0xEF.toByte())
        assertEquals(tamilText, String(cleanUtf8Bytes, StandardCharsets.UTF_8))
    }

    @Test
    fun utf16LeWithBom_decodesDevanagariTextCorrectly() {
        val devanagariText = "1\n00:00:01,000 --> 00:00:04,000\nनमस्ते! NisTV हिंदी उपशीर्षक"
        val bom = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        val textBytes = devanagariText.toByteArray(StandardCharsets.UTF_16LE)
        val combined = bom + textBytes

        val decoded = SubtitleCharsetDetector.decodeToText(combined)
        assertEquals(devanagariText, decoded)

        val cleanUtf8Bytes = SubtitleCharsetDetector.convertToUtf8Bytes(combined)
        assertEquals(devanagariText, String(cleanUtf8Bytes, StandardCharsets.UTF_8))
    }

    @Test
    fun utf16BeWithBom_decodesTamilTextCorrectly() {
        val tamilText = "வணக்கம் உலகம்"
        val bom = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
        val textBytes = tamilText.toByteArray(StandardCharsets.UTF_16BE)
        val combined = bom + textBytes

        val decoded = SubtitleCharsetDetector.decodeToText(combined)
        assertEquals(tamilText, decoded)
    }

    @Test
    fun windows1252_decodesFallbackCorrectly() {
        val sampleText = "Café & Résumé in NisTV"
        val win1252Bytes = sampleText.toByteArray(Charset.forName("windows-1252"))

        val decoded = SubtitleCharsetDetector.decodeToText(win1252Bytes)
        assertEquals(sampleText, decoded)
    }

    @Test
    fun sidecarSubtitleDetector_parsesLanguagesFromFilenames() {
        val (labelTa, tagTa) = SidecarSubtitleDetector.parseLanguageTagAndLabel("Kantara.ta.srt", "Kantara")
        assertEquals("ta", tagTa)
        assertTrue(labelTa.contains("Tamil"))

        val (labelHi, tagHi) = SidecarSubtitleDetector.parseLanguageTagAndLabel("Jawan.Hindi.vtt", "Jawan")
        assertEquals("hi", tagHi)
        assertTrue(labelHi.contains("Hindi"))

        val (labelEn, tagEn) = SidecarSubtitleDetector.parseLanguageTagAndLabel("Inception_eng.srt", "Inception")
        assertEquals("en", tagEn)
        assertTrue(labelEn.contains("English"))
    }
}
