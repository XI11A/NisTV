package com.nis.nistv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamClassifierTest {

    private fun classifyErrorMessage(errorCodeName: String, httpResponseCode: Int = 0): String {
        return when {
            httpResponseCode in 400..499 -> "HTTP $httpResponseCode: Channel stream not found or forbidden."
            httpResponseCode in 500..599 -> "HTTP $httpResponseCode: Stream server error. Try another source."
            errorCodeName.contains("TIMEOUT", ignoreCase = true) -> "Connection timed out. Checking backup source..."
            errorCodeName.contains("DECODER", ignoreCase = true) -> "Device video decoder unsupported for this stream format."
            else -> "Playback error: $errorCodeName"
        }
    }

    @Test
    fun testHttpErrorClassification() {
        val error404 = classifyErrorMessage("ERROR_CODE_IO_BAD_HTTP_STATUS", 404)
        assertTrue(error404.contains("not found or forbidden"))

        val error503 = classifyErrorMessage("ERROR_CODE_IO_BAD_HTTP_STATUS", 503)
        assertTrue(error503.contains("Stream server error"))
    }

    @Test
    fun testTimeoutClassification() {
        val errorTimeout = classifyErrorMessage("ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT")
        assertTrue(errorTimeout.contains("Connection timed out"))
    }

    @Test
    fun testDecoderClassification() {
        val errorDecoder = classifyErrorMessage("ERROR_CODE_DECODER_INIT_FAILED")
        assertTrue(errorDecoder.contains("decoder unsupported"))
    }
}
