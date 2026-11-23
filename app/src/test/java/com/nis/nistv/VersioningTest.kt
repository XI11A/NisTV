package com.nis.nistv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

class VersioningTest {

    private fun calculateVersionCode(major: Int, minor: Int, patch: Int): Int {
        return major * 10000 + minor * 100 + patch
    }

    private data class VersionState(val major: Int, val minor: Int, val patch: Int) {
        val versionName: String get() = "$major.$minor.$patch"
        val versionCode: Int get() = major * 10000 + minor * 100 + patch

        fun bump(type: String): VersionState {
            return when (type.lowercase()) {
                "major" -> VersionState(major + 1, 0, 0)
                "minor" -> VersionState(major, minor + 1, 0)
                "patch" -> VersionState(major, minor, patch + 1)
                else -> throw IllegalArgumentException("Invalid bump type: $type")
            }
        }
    }

    @Test
    fun testVersionCodeFormula() {
        assertEquals(10000, calculateVersionCode(1, 0, 0))
        assertEquals(10100, calculateVersionCode(1, 1, 0))
        assertEquals(10001, calculateVersionCode(1, 0, 1))
        assertEquals(10203, calculateVersionCode(1, 2, 3))
        assertEquals(20000, calculateVersionCode(2, 0, 0))
        assertEquals(21530, calculateVersionCode(2, 15, 30))
    }

    @Test
    fun testPatchBumpMaintainsMajorAndMinor() {
        val initial = VersionState(1, 2, 3)
        val bumped = initial.bump("patch")

        assertEquals(1, bumped.major)
        assertEquals(2, bumped.minor)
        assertEquals(4, bumped.patch)
        assertEquals("1.2.4", bumped.versionName)
        assertEquals(10204, bumped.versionCode)
    }

    @Test
    fun testMinorBumpResetsPatch() {
        val initial = VersionState(1, 2, 9)
        val bumped = initial.bump("minor")

        assertEquals(1, bumped.major)
        assertEquals(3, bumped.minor)
        assertEquals(0, bumped.patch)
        assertEquals("1.3.0", bumped.versionName)
        assertEquals(10300, bumped.versionCode)
    }

    @Test
    fun testMajorBumpResetsMinorAndPatch() {
        val initial = VersionState(1, 7, 12)
        val bumped = initial.bump("major")

        assertEquals(2, bumped.major)
        assertEquals(0, bumped.minor)
        assertEquals(0, bumped.patch)
        assertEquals("2.0.0", bumped.versionName)
        assertEquals(20000, bumped.versionCode)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidBumpTypeThrows() {
        val state = VersionState(1, 0, 0)
        state.bump("invalid")
    }

    @Test
    fun testVersionPropertiesFileIntegrity() {
        // Find version.properties by searching up if needed
        var candidate: File? = File("version.properties")
        if (candidate?.exists() != true) {
            candidate = File("../version.properties")
        }
        assertTrue("version.properties file must exist", candidate != null && candidate.exists())

        val properties = Properties().apply {
            candidate!!.inputStream().use { load(it) }
        }

        val majorStr = properties.getProperty("VERSION_MAJOR")
        val minorStr = properties.getProperty("VERSION_MINOR")
        val patchStr = properties.getProperty("VERSION_PATCH")
        val nameStr = properties.getProperty("VERSION_NAME")
        val codeStr = properties.getProperty("VERSION_CODE")

        assertTrue("VERSION_MAJOR must be present", !majorStr.isNullOrBlank())
        assertTrue("VERSION_MINOR must be present", !minorStr.isNullOrBlank())
        assertTrue("VERSION_PATCH must be present", !patchStr.isNullOrBlank())
        assertTrue("VERSION_NAME must be present", !nameStr.isNullOrBlank())
        assertTrue("VERSION_CODE must be present", !codeStr.isNullOrBlank())

        val major = majorStr.trim().toInt()
        val minor = minorStr.trim().toInt()
        val patch = patchStr.trim().toInt()
        val code = codeStr.trim().toInt()

        assertEquals("$major.$minor.$patch", nameStr.trim())
        assertEquals(major * 10000 + minor * 100 + patch, code)
        assertTrue("VERSION_CODE must be strictly greater than legacy code 1", code > 1)
    }
}
