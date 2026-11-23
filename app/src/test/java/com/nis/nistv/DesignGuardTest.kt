package com.nis.nistv

import androidx.compose.foundation.shape.RoundedCornerShape
import com.nis.nistv.core.designsystem.theme.NisBadgeShape
import com.nis.nistv.core.designsystem.theme.NisBottomSheetShape
import com.nis.nistv.core.designsystem.theme.NisButtonShape
import com.nis.nistv.core.designsystem.theme.NisChannelCardShape
import com.nis.nistv.core.designsystem.theme.NisChipShape
import com.nis.nistv.core.designsystem.theme.NisDialogShape
import com.nis.nistv.core.designsystem.theme.NisFeaturedCardShape
import com.nis.nistv.core.designsystem.theme.NisMediaCardShape
import com.nis.nistv.core.designsystem.theme.NisMiniPlayerShape
import com.nis.nistv.core.designsystem.theme.NisPillShape
import com.nis.nistv.core.designsystem.theme.NisSideDrawerShape
import com.nis.nistv.core.designsystem.theme.NisThumbnailShape
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Stage 3 Design Guard Test Suite.
 * Enforces zero unauthorized hardcoded colors, font sizes, and ad-hoc shapes
 * across all screens and components in NisTV Revamped ("Cinematic Obsidian 2.0").
 */
class DesignGuardTest {

    private fun resolveSourceDirs(): List<File> {
        val candidates = listOf(
            File("app/src/main/java/com/nis/nistv"),
            File("src/main/java/com/nis/nistv")
        )
        val root = candidates.firstOrNull { it.exists() && it.isDirectory }
        assertNotNull("NisTV source root must exist", root)

        val featureDir = File(root, "feature")
        val componentDir = File(root, "core/designsystem/component")
        return listOfNotNull(
            featureDir.takeIf { it.exists() },
            componentDir.takeIf { it.exists() }
        )
    }

    private fun walkKotlinFiles(dirs: List<File>): List<File> {
        val files = mutableListOf<File>()
        dirs.forEach { dir ->
            dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { files.add(it) }
        }
        return files
    }

    @Test
    fun testNoUnauthorizedHardcodedColorsAcrossFeaturesAndComponents() {
        val dirs = resolveSourceDirs()
        val ktFiles = walkKotlinFiles(dirs)

        val exemptFiles = setOf(
            "NtvLogo.kt",
            "NisLogo.kt",
            "NisLogoGeometry.kt",
            "NisAnimatedSvgLogo.kt",
            "GoogleAuthGateDialog.kt", // Official Google "G" logo vector segment paths & #1F1F1F button text
            "AfdsAuthGateDialog.kt",   // Official Google sign-in button branded #1F1F1F text
            "LanguageOnboardingDialog.kt", // Regional language badge accent colors
            "DualSubtitleOverlay.kt"  // Video subtitle soft light yellow overlay
        )

        val violations = mutableListOf<String>()

        ktFiles.forEach { file ->
            if (file.name !in exemptFiles) {
                file.readLines().forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("//") && !trimmed.startsWith("*")) {
                        if (line.contains("Color(0x")) {
                            violations.add("${file.name}:${index + 1}: $line")
                        }
                    }
                }
            }
        }

        assertTrue(
            "Found unauthorized hardcoded hex colors in features/components:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun testNoUnauthorizedAdHocRoundedCornerShapesAcrossFeaturesAndComponents() {
        val dirs = resolveSourceDirs()
        val ktFiles = walkKotlinFiles(dirs)

        val violations = mutableListOf<String>()

        ktFiles.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (!trimmed.startsWith("//") && !trimmed.startsWith("*")) {
                    if (line.contains("RoundedCornerShape(")) {
                        violations.add("${file.name}:${index + 1}: $line")
                    }
                }
            }
        }

        assertTrue(
            "Found unauthorized ad-hoc RoundedCornerShape calls. Use design system shape tokens (NisShapes.*, NisCardShape, NisPillShape, etc.):\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun testNoUnauthorizedHardcodedFontSizesAcrossFeaturesAndComponents() {
        val dirs = resolveSourceDirs()
        val ktFiles = walkKotlinFiles(dirs)

        val exemptFiles = setOf(
            "NtvLogo.kt",              // Parameterized logo font sizing
            "NetflixLoadingScreen.kt", // Standalone NtvLogo wordmark invocation
            "GoogleAuthGateDialog.kt", // Standalone NtvLogo wordmark invocation
            "AfdsAuthGateDialog.kt",   // Standalone NtvLogo wordmark invocation
            "NisAnimatedSvgLogo.kt",   // Animated SVG logo geometry
            "OfflinePlayerTrackDialog.kt", // Dynamic runtime user font-size preference preview
            "DualSubtitleOverlay.kt"   // Video playback subtitle rendered text sizes
        )

        val fontSizeRegex = Regex("""fontSize\s*=\s*\d+""")
        val violations = mutableListOf<String>()

        ktFiles.forEach { file ->
            if (file.name !in exemptFiles) {
                file.readLines().forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("//") && !trimmed.startsWith("*")) {
                        if (fontSizeRegex.containsMatchIn(line)) {
                            violations.add("${file.name}:${index + 1}: $line")
                        }
                    }
                }
            }
        }

        assertTrue(
            "Found unauthorized hardcoded font sizes. Use MaterialTheme.typography.* or NisTheme.textExtras.*:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun testNoDeprecatedNisAccentGreenUsages() {
        val candidates = listOf(
            File("app/src/main/java/com/nis/nistv"),
            File("src/main/java/com/nis/nistv")
        )
        val root = candidates.firstOrNull { it.exists() && it.isDirectory }
        assertNotNull("NisTV source root must exist", root)

        val allKtFiles = root!!.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        val violations = mutableListOf<String>()

        allKtFiles.forEach { file ->
            if (file.name != "Color.kt") {
                file.readLines().forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("//") && !trimmed.startsWith("*")) {
                        if (line.contains("NisAccentGreen")) {
                            violations.add("${file.name}:${index + 1}: $line")
                        }
                    }
                }
            }
        }

        assertTrue(
            "Found deprecated NisAccentGreen usages. Migrate to NisAccentPrimary:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun testShapeTokensHierarchy() {
        assertTrue("NisBadgeShape must be RoundedCornerShape", NisBadgeShape is RoundedCornerShape)
        assertTrue("NisThumbnailShape must be RoundedCornerShape", NisThumbnailShape is RoundedCornerShape)
        assertTrue("NisButtonShape must be RoundedCornerShape", NisButtonShape is RoundedCornerShape)
        assertTrue("NisMediaCardShape must be RoundedCornerShape", NisMediaCardShape is RoundedCornerShape)
        assertTrue("NisChannelCardShape must be RoundedCornerShape", NisChannelCardShape is RoundedCornerShape)
        assertTrue("NisFeaturedCardShape must be RoundedCornerShape", NisFeaturedCardShape is RoundedCornerShape)
        assertTrue("NisMiniPlayerShape must be RoundedCornerShape", NisMiniPlayerShape is RoundedCornerShape)
        assertTrue("NisDialogShape must be RoundedCornerShape", NisDialogShape is RoundedCornerShape)
        assertTrue("NisBottomSheetShape must be RoundedCornerShape", NisBottomSheetShape is RoundedCornerShape)
        assertTrue("NisSideDrawerShape must be RoundedCornerShape", NisSideDrawerShape is RoundedCornerShape)
        assertTrue("NisChipShape must be RoundedCornerShape", NisChipShape is RoundedCornerShape)
        assertTrue("NisPillShape must be RoundedCornerShape", NisPillShape is RoundedCornerShape)
    }
}
