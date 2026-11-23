package com.nis.nistv

import androidx.compose.foundation.shape.RoundedCornerShape
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.NisButtonShape
import com.nis.nistv.core.designsystem.theme.NisChipShape
import com.nis.nistv.core.designsystem.theme.NisDialogShape
import com.nis.nistv.core.designsystem.theme.NisDimens
import com.nis.nistv.core.designsystem.theme.NisMediaCardShape
import com.nis.nistv.core.designsystem.theme.NisMiniPlayerShape
import com.nis.nistv.core.designsystem.theme.NisMotion
import com.nis.nistv.core.designsystem.theme.NisShapes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import androidx.compose.ui.unit.dp

/**
 * Stage 2 Component Library Restyle Design Guard.
 * Ensures strict enforcement of Cinematic Obsidian 2.0 component tokens,
 * 48dp minimum touch targets, directional icon autoMirroring, and zero
 * unauthorized hardcoded hex colors in core design system widgets.
 */
class ComponentDesignGuardTest {

    @Test
    fun testTouchTargetAccessibilityMinimum() {
        assertEquals("Standard touch target must be at least 48dp for accessibility", 48.dp, NisDimens.TouchTarget)
    }

    @Test
    fun testComponentShapesUniformity() {
        assertTrue("Button shape must be RoundedCornerShape", NisButtonShape is RoundedCornerShape)
        assertTrue("Dialog shape must be RoundedCornerShape", NisDialogShape is RoundedCornerShape)
        assertTrue("Media card shape must be RoundedCornerShape", NisMediaCardShape is RoundedCornerShape)
        assertTrue("Mini player shape must be RoundedCornerShape", NisMiniPlayerShape is RoundedCornerShape)
        assertTrue("Chip pill shape must be RoundedCornerShape", NisChipShape is RoundedCornerShape)

        // Verify shape tokens mapping to 5-step Material scale
        assertEquals(NisShapes.medium, NisButtonShape)
        assertEquals(NisShapes.medium, NisMediaCardShape)
        assertEquals(NisShapes.large, NisMiniPlayerShape)
        assertEquals(NisShapes.extraLarge, NisDialogShape)
    }

    @Test
    fun testDirectionalIconsHaveAutoMirrorEnabled() {
        assertTrue("NisIcons.Back must have autoMirror enabled for RTL", NisIcons.Back.autoMirror)
        assertTrue("NisIcons.SkipNext must have autoMirror enabled for RTL", NisIcons.SkipNext.autoMirror)
        assertTrue("NisIcons.SkipPrevious must have autoMirror enabled for RTL", NisIcons.SkipPrevious.autoMirror)
    }

    @Test
    fun testMotionTokenDurations() {
        assertEquals(120, NisMotion.DurationFastMs)
        assertEquals(220, NisMotion.DurationNormalMs)
        assertEquals(360, NisMotion.DurationSlowMs)
        assertEquals(0.7f, NisMotion.PressDampingRatio, 0.001f)
        assertEquals(800f, NisMotion.PressStiffness, 0.001f)
    }

    @Test
    fun testCoreComponentsHaveNoUnauthorizedHardcodedHexColors() {
        val componentsDir = listOf(
            File("src/main/java/com/nis/nistv/core/designsystem/component"),
            File("app/src/main/java/com/nis/nistv/core/designsystem/component")
        ).firstOrNull { it.exists() && it.isDirectory }

        assertNotNull("Components directory must exist", componentsDir)

        val exemptFiles = setOf(
            "NtvLogo.kt",
            "NisLogo.kt",
            "NisLogoGeometry.kt",
            "NisAnimatedSvgLogo.kt",
            "GoogleAuthGateDialog.kt", // Contains official Google "G" logo vector segment colors
            "LanguageOnboardingDialog.kt", // Contains Indian language accent badges
            "AfdsAuthGateDialog.kt" // Contains Google sign-in button branded colors
        )

        val violations = mutableListOf<String>()

        componentsDir?.listFiles { file -> file.extension == "kt" }?.forEach { file ->
            if (file.name !in exemptFiles) {
                file.readLines().forEachIndexed { lineIdx, line ->
                    if (line.contains("Color(0x") && !line.trim().startsWith("//")) {
                        violations.add("${file.name}:${lineIdx + 1}: $line")
                    }
                }
            }
        }

        assertTrue(
            "Found unauthorized hardcoded hex colors in core components:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }
}
