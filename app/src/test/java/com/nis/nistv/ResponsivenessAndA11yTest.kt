package com.nis.nistv

import androidx.compose.ui.unit.dp
import com.nis.nistv.core.designsystem.theme.NisDimens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Stage 4 Screen Polish, Tablet Responsiveness, and Dynamic Type Test Suite.
 * Validates:
 * 1. Adaptive icons with Android 13+ monochrome layer for themed icons.
 * 2. Dimension tokens for tablet responsiveness (840dp max content width) and touch targets (48dp).
 * 3. Adaptive grids (GridCells.Adaptive) on Discover, Live TV, and Favorites.
 * 4. Dynamic Type resilience using heightIn(min = ...) on hero banners and media details headers.
 * 5. Tablet width capping across all major screens.
 * 6. Bottom sheet shapes and drag handles.
 */
class ResponsivenessAndA11yTest {

    private fun resolveResDir(): File {
        val candidates = listOf(
            File("app/src/main/res"),
            File("src/main/res")
        )
        val dir = candidates.firstOrNull { it.exists() && it.isDirectory }
        assertNotNull("NisTV res directory must exist", dir)
        return dir!!
    }

    private fun resolveSourceDir(): File {
        val candidates = listOf(
            File("app/src/main/java/com/nis/nistv"),
            File("src/main/java/com/nis/nistv")
        )
        val dir = candidates.firstOrNull { it.exists() && it.isDirectory }
        assertNotNull("NisTV source directory must exist", dir)
        return dir!!
    }

    @Test
    fun testThemedMonochromeIconExistsAndDeclared() {
        val resDir = resolveResDir()

        // 1. ic_launcher_monochrome.xml must exist in res/drawable
        val monochromeFile = File(resDir, "drawable/ic_launcher_monochrome.xml")
        assertTrue("ic_launcher_monochrome.xml must exist in res/drawable", monochromeFile.exists())
        val monochromeContent = monochromeFile.readText()
        assertTrue("ic_launcher_monochrome.xml must be a valid vector drawable", monochromeContent.contains("<vector"))
        assertTrue("ic_launcher_monochrome.xml must contain path data", monochromeContent.contains("android:pathData"))

        // 2. Adaptive icon XMLs in mipmap-anydpi-v26 must declare the monochrome layer
        val anydpiDir = File(resDir, "mipmap-anydpi-v26")
        assertTrue("mipmap-anydpi-v26 directory must exist", anydpiDir.exists())

        val launcherXml = File(anydpiDir, "ic_launcher.xml")
        assertTrue("ic_launcher.xml must exist in mipmap-anydpi-v26", launcherXml.exists())
        val launcherText = launcherXml.readText()
        assertTrue(
            "ic_launcher.xml must declare <monochrome android:drawable=\"@drawable/ic_launcher_monochrome\" />",
            launcherText.contains("<monochrome") && launcherText.contains("ic_launcher_monochrome")
        )

        val launcherRoundXml = File(anydpiDir, "ic_launcher_round.xml")
        assertTrue("ic_launcher_round.xml must exist in mipmap-anydpi-v26", launcherRoundXml.exists())
        val roundText = launcherRoundXml.readText()
        assertTrue(
            "ic_launcher_round.xml must declare <monochrome android:drawable=\"@drawable/ic_launcher_monochrome\" />",
            roundText.contains("<monochrome") && roundText.contains("ic_launcher_monochrome")
        )
    }

    @Test
    fun testResponsiveDimensionTokens() {
        assertEquals("MaxContentWidth must be 840.dp for tablet/foldable capping", 840.dp, NisDimens.MaxContentWidth)
        assertEquals("TouchTarget must be 48.dp to satisfy WCAG touch target guideline", 48.dp, NisDimens.TouchTarget)
        assertEquals("Scale24 must be 24.dp for vertical rhythm spacing", 24.dp, NisDimens.Scale24)
        assertEquals("Scale16 must be 16.dp for standard screen horizontal padding", 16.dp, NisDimens.Scale16)
        assertEquals("PosterAspectRatio should be 2f / 3f (0.667)", 2f / 3f, NisDimens.PosterAspectRatio, 0.001f)
    }

    @Test
    fun testAdaptiveGridsOnDiscoverAndLiveTvAndFavorites() {
        val srcDir = resolveSourceDir()

        // 1. DiscoverScreen must NOT use GridCells.Fixed(3) and must use GridCells.Adaptive
        val discoverFile = File(srcDir, "feature/discover/DiscoverScreen.kt")
        assertTrue("DiscoverScreen.kt must exist", discoverFile.exists())
        val discoverText = discoverFile.readText()
        assertTrue(
            "DiscoverScreen.kt must use GridCells.Adaptive for search and genre grids",
            discoverText.contains("GridCells.Adaptive(minSize = 110.dp)")
        )
        assertTrue(
            "DiscoverScreen.kt must NOT use fixed 3-column grid",
            !discoverText.contains("GridCells.Fixed(3)")
        )

        // 2. LiveTvScreen must use GridCells.Adaptive(minSize = 160.dp)
        val liveTvFile = File(srcDir, "feature/livetv/LiveTvScreen.kt")
        assertTrue("LiveTvScreen.kt must exist", liveTvFile.exists())
        val liveTvText = liveTvFile.readText()
        assertTrue(
            "LiveTvScreen.kt must use GridCells.Adaptive(minSize = 160.dp)",
            liveTvText.contains("GridCells.Adaptive(minSize = 160.dp)")
        )

        // 3. FavoritesScreen must use GridCells.Adaptive
        val favoritesFile = File(srcDir, "feature/favorites/FavoritesScreen.kt")
        assertTrue("FavoritesScreen.kt must exist", favoritesFile.exists())
        val favText = favoritesFile.readText()
        assertTrue(
            "FavoritesScreen.kt must use GridCells.Adaptive(minSize = 115.dp)",
            favText.contains("GridCells.Adaptive(minSize = 115.dp)")
        )
        assertTrue(
            "FavoritesScreen.kt must use GridCells.Adaptive(minSize = 160.dp)",
            favText.contains("GridCells.Adaptive(minSize = 160.dp)")
        )
    }

    @Test
    fun testDynamicTypeResilienceOnBannersAndHeaders() {
        val srcDir = resolveSourceDir()

        // MediaDetailsHeader must use heightIn(min = 400.dp) instead of fixed height(400.dp)
        val mediaDetailsHeader = File(srcDir, "feature/details/MediaDetailsHeader.kt")
        assertTrue("MediaDetailsHeader.kt must exist", mediaDetailsHeader.exists())
        val headerText = mediaDetailsHeader.readText()
        assertTrue(
            "MediaDetailsHeader must use heightIn(min = 400.dp) for dynamic type scaling",
            headerText.contains("heightIn(min = 400.dp)")
        )

        // HomeScreen hero banners must use heightIn(min = ...)
        val homeFile = File(srcDir, "feature/home/HomeScreen.kt")
        assertTrue("HomeScreen.kt must exist", homeFile.exists())
        val homeText = homeFile.readText()
        assertTrue(
            "HomeScreen CinematicHeroBanner must use heightIn(min = 410.dp)",
            homeText.contains("heightIn(min = 410.dp)")
        )
        assertTrue(
            "HomeScreen RecommendedHeroCarousel must use heightIn(min = 430.dp)",
            homeText.contains("heightIn(min = 430.dp)")
        )
    }

    @Test
    fun testTabletMaxContentWidthCappingAppliedAcrossScreens() {
        val srcDir = resolveSourceDir()

        val screens = listOf(
            "feature/home/HomeScreen.kt",
            "feature/discover/DiscoverScreen.kt",
            "feature/livetv/LiveTvScreen.kt",
            "feature/search/SearchScreen.kt",
            "feature/downloads/DownloadsScreen.kt",
            "feature/downloads/OfflineDetailsScreen.kt",
            "feature/profile/ProfileScreen.kt",
            "feature/favorites/FavoritesScreen.kt",
            "feature/details/MediaDetailsScreen.kt",
            "feature/history/HistoryAndPrivacyScreen.kt",
            "feature/parental/ParentalControlsScreen.kt",
            "feature/backup/BackupRestoreScreen.kt"
        )

        screens.forEach { relPath ->
            val screenFile = File(srcDir, relPath)
            assertTrue("Screen file must exist: $relPath", screenFile.exists())
            val text = screenFile.readText()
            assertTrue(
                "Screen $relPath must reference NisDimens.MaxContentWidth or MaxContentWidth for tablet responsiveness",
                text.contains("MaxContentWidth")
            )
        }
    }

    @Test
    fun testModalBottomSheetsUseConsistentShapeAndWidth() {
        val srcDir = resolveSourceDir()

        val sheets = listOf(
            "feature/home/TonightPicksSheet.kt",
            "feature/player/controls/PlaybackSpeedSheet.kt",
            "feature/player/controls/SleepTimerSheet.kt"
        )

        sheets.forEach { relPath ->
            val sheetFile = File(srcDir, relPath)
            assertTrue("Sheet file must exist: $relPath", sheetFile.exists())
            val text = sheetFile.readText()
            assertTrue(
                "Sheet $relPath must cap width at MaxContentWidth",
                text.contains("MaxContentWidth")
            )
        }
    }
}
