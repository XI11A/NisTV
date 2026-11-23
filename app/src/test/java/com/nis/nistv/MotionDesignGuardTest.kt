package com.nis.nistv

import androidx.compose.animation.core.SpringSpec
import com.nis.nistv.core.designsystem.theme.NisMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Stage 5 Motion & Interaction Design Guard Test.
 * Verifies Cinematic Obsidian 2.0 motion specifications, tactile spring parameters,
 * reduced-motion safety fallbacks, and performance constraints (draw-phase evaluation,
 * zero per-frame recomposition loops).
 */
class MotionDesignGuardTest {

    @Test
    fun testDurationTokensScale() {
        assertEquals("Fast duration token must be 120ms", 120, NisMotion.DurationFastMs)
        assertEquals("Normal duration token must be 220ms", 220, NisMotion.DurationNormalMs)
        assertEquals("Slow duration token must be 360ms", 360, NisMotion.DurationSlowMs)

        assertTrue(
            "Duration tokens must be ordered Fast < Normal < Slow",
            NisMotion.DurationFastMs < NisMotion.DurationNormalMs &&
                NisMotion.DurationNormalMs < NisMotion.DurationSlowMs
        )
    }

    @Test
    fun testTactileSpringSpecifications() {
        assertEquals(0.7f, NisMotion.PressDampingRatio, 0.001f)
        assertEquals(800f, NisMotion.PressStiffness, 0.001f)

        val pressSpring: SpringSpec<Float> = NisMotion.pressSpring()
        assertEquals(0.7f, pressSpring.dampingRatio, 0.001f)
        assertEquals(800f, pressSpring.stiffness, 0.001f)

        val itemPlacementSpring: SpringSpec<Float> = NisMotion.itemPlacementSpring()
        assertEquals(0.8f, itemPlacementSpring.dampingRatio, 0.001f)
        assertEquals(400f, itemPlacementSpring.stiffness, 0.001f)
    }

    @Test
    fun testTransitionsReducedMotionSafety() {
        // Assert transitions build valid instances for both normal and reduced-motion modes
        val dialogEnterNormal = NisMotion.dialogEnterTransition(reducedMotion = false)
        val dialogEnterReduced = NisMotion.dialogEnterTransition(reducedMotion = true)
        assertNotNull(dialogEnterNormal)
        assertNotNull(dialogEnterReduced)

        val dialogExitNormal = NisMotion.dialogExitTransition(reducedMotion = false)
        val dialogExitReduced = NisMotion.dialogExitTransition(reducedMotion = true)
        assertNotNull(dialogExitNormal)
        assertNotNull(dialogExitReduced)

        val sheetEnterNormal = NisMotion.sheetEnterTransition(reducedMotion = false)
        val sheetEnterReduced = NisMotion.sheetEnterTransition(reducedMotion = true)
        assertNotNull(sheetEnterNormal)
        assertNotNull(sheetEnterReduced)

        val sheetExitNormal = NisMotion.sheetExitTransition(reducedMotion = false)
        val sheetExitReduced = NisMotion.sheetExitTransition(reducedMotion = true)
        assertNotNull(sheetExitNormal)
        assertNotNull(sheetExitReduced)
    }

    @Test
    fun testPressableDrawPhaseEvaluation() {
        val file = File("src/main/java/com/nis/nistv/core/designsystem/component/NisPressable.kt")
        assertTrue("NisPressable.kt must exist", file.exists())
        val content = file.readText()

        assertTrue(
            "NisPressable must evaluate scale inside graphicsLayer draw lambda to avoid per-frame recomposition",
            content.contains("graphicsLayer {") && content.contains("scaleX =") && content.contains("scaleY =")
        )
        assertTrue(
            "NisPressable must use NisMotion.pressSpring for tactile bounce",
            content.contains("NisMotion.pressSpring()")
        )
        assertTrue(
            "NisPressable must support debounced click prevention",
            content.contains("debounced: Boolean = false")
        )
    }

    @Test
    fun testLiveBadgePulsingDrawPhaseEvaluation() {
        val file = File("src/main/java/com/nis/nistv/core/designsystem/component/NisBadge.kt")
        assertTrue("NisBadge.kt must exist", file.exists())
        val content = file.readText()

        assertTrue(
            "NisLiveBadge pulsing dot must use graphicsLayer draw lambda rather than .alpha()",
            content.contains("graphicsLayer { alpha =")
        )
        assertTrue(
            "NisCategoryChip must support animated indicator color transition",
            content.contains("tabIndicatorSpec")
        )
    }

    @Test
    fun testLinearProgressBarDrawPhaseEvaluation() {
        val file = File("src/main/java/com/nis/nistv/core/designsystem/component/NisLoadingIndicator.kt")
        assertTrue("NisLoadingIndicator.kt must exist", file.exists())
        val content = file.readText()

        assertTrue(
            "NisLinearProgressBar must provide progress via draw-phase lambda provider",
            content.contains("LinearProgressIndicator(") && content.contains("progress = { animatedProgress.value }")
        )
        assertTrue(
            "NisLinearProgressBar must check reduced-motion accessibility preference",
            content.contains("isReducedMotionEnabled")
        )
    }

    @Test
    fun testBottomNavigationTabAnimation() {
        val file = File("src/main/java/com/nis/nistv/core/navigation/NisNavigation.kt")
        assertTrue("NisNavigation.kt must exist", file.exists())
        val content = file.readText()

        assertTrue(
            "NisBottomNavigationBar must animate tab pill width and alpha using NisMotion",
            content.contains("tabIndicatorSpec") && content.contains("animateDpAsState")
        )
        assertTrue(
            "NisBottomNavigationBar indicator must use graphicsLayer for alpha",
            content.contains("graphicsLayer { alpha = indicatorAlpha }") ||
                content.contains("graphicsLayer { alpha = indicatorAlpha.value }")
        )
    }

    @Test
    fun testMiniPlayerDismissUsesMotionTokens() {
        val file = File("src/main/java/com/nis/nistv/feature/player/components/NisMiniPlayer.kt")
        assertTrue("NisMiniPlayer.kt must exist", file.exists())
        val content = file.readText()

        assertTrue(
            "NisMiniPlayer swipe dismiss must use NisMotion.DurationNormalMs and EasingEmphasized",
            content.contains("NisMotion.DurationNormalMs") && content.contains("NisMotion.EasingEmphasized")
        )
    }
}
