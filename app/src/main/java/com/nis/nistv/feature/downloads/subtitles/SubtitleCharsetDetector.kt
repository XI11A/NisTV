package com.nis.nistv.feature.downloads.subtitles

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

object SubtitleCharsetDetector {

    /**
     * Detects the character encoding of subtitle bytes and decodes into clean text.
     */
    fun decodeToText(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""

        // 1. Check for Byte Order Mark (BOM)
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16LE)
        }

        // 2. Attempt strict UTF-8 validation
        val utf8Decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)

        try {
            val charBuffer = utf8Decoder.decode(ByteBuffer.wrap(bytes))
            return charBuffer.toString()
        } catch (_: Exception) {
            // Malformed UTF-8 -> fallback to Windows-1252 / ISO-8859-1
        }

        // 3. Fallback to Windows-1252
        val win1252 = Charset.forName("windows-1252")
        return String(bytes, win1252)
    }

    /**
     * Converts raw subtitle bytes to clean UTF-8 encoded bytes without BOM.
     */
    fun convertToUtf8Bytes(rawBytes: ByteArray): ByteArray {
        val text = decodeToText(rawBytes)
        return text.toByteArray(StandardCharsets.UTF_8)
    }
}
