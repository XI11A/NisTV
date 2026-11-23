package com.nis.nistv

import com.nis.nistv.feature.player.components.TargetOrientation
import com.nis.nistv.feature.player.components.resolveTargetOrientation
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerOrientationTest {

    @Test
    fun phone_uprightAngle_resolvesPortrait() {
        assertEquals(TargetOrientation.PORTRAIT, resolveTargetOrientation(0, isNaturalLandscape = false))
        assertEquals(TargetOrientation.PORTRAIT, resolveTargetOrientation(15, isNaturalLandscape = false))
        assertEquals(TargetOrientation.PORTRAIT, resolveTargetOrientation(35, isNaturalLandscape = false))
        assertEquals(TargetOrientation.PORTRAIT, resolveTargetOrientation(340, isNaturalLandscape = false))
        assertEquals(TargetOrientation.PORTRAIT, resolveTargetOrientation(360, isNaturalLandscape = false))
    }

    @Test
    fun phone_reverseLandscapeAngle_resolvesReverseLandscape() {
        // ~90 degrees counter-clockwise rotation (tilted left, 180° flip from normal landscape)
        assertEquals(TargetOrientation.REVERSE_LANDSCAPE, resolveTargetOrientation(60, isNaturalLandscape = false))
        assertEquals(TargetOrientation.REVERSE_LANDSCAPE, resolveTargetOrientation(90, isNaturalLandscape = false))
        assertEquals(TargetOrientation.REVERSE_LANDSCAPE, resolveTargetOrientation(120, isNaturalLandscape = false))
    }

    @Test
    fun phone_normalLandscapeAngle_resolvesLandscape() {
        // ~270 degrees counter-clockwise rotation (standard landscape, tilted right)
        assertEquals(TargetOrientation.LANDSCAPE, resolveTargetOrientation(240, isNaturalLandscape = false))
        assertEquals(TargetOrientation.LANDSCAPE, resolveTargetOrientation(270, isNaturalLandscape = false))
        assertEquals(TargetOrientation.LANDSCAPE, resolveTargetOrientation(300, isNaturalLandscape = false))
    }

    @Test
    fun phone_180DegreeFlip_transitionsBetweenOppositeLandscapes() {
        val normalLandscape = resolveTargetOrientation(270, isNaturalLandscape = false)
        val flipped180 = resolveTargetOrientation(90, isNaturalLandscape = false)

        assertEquals(TargetOrientation.LANDSCAPE, normalLandscape)
        assertEquals(TargetOrientation.REVERSE_LANDSCAPE, flipped180)
    }

    @Test
    fun hysteresisBufferZones_resolveUnknownToPreventJitter() {
        // Angles in between discrete orientation buckets should not flap
        assertEquals(TargetOrientation.UNKNOWN, resolveTargetOrientation(45, isNaturalLandscape = false))
        assertEquals(TargetOrientation.UNKNOWN, resolveTargetOrientation(135, isNaturalLandscape = false))
        assertEquals(TargetOrientation.UNKNOWN, resolveTargetOrientation(180, isNaturalLandscape = false))
        assertEquals(TargetOrientation.UNKNOWN, resolveTargetOrientation(220, isNaturalLandscape = false))
        assertEquals(TargetOrientation.UNKNOWN, resolveTargetOrientation(315, isNaturalLandscape = false))
        assertEquals(TargetOrientation.UNKNOWN, resolveTargetOrientation(-1, isNaturalLandscape = false))
    }

    @Test
    fun tablet_naturalLandscape_resolvesCorrectly() {
        // Natural landscape tablets have 0° as Landscape and 90° as Portrait
        assertEquals(TargetOrientation.LANDSCAPE, resolveTargetOrientation(0, isNaturalLandscape = true))
        assertEquals(TargetOrientation.LANDSCAPE, resolveTargetOrientation(350, isNaturalLandscape = true))
        assertEquals(TargetOrientation.REVERSE_LANDSCAPE, resolveTargetOrientation(180, isNaturalLandscape = true))
        assertEquals(TargetOrientation.PORTRAIT, resolveTargetOrientation(90, isNaturalLandscape = true))
    }
}
