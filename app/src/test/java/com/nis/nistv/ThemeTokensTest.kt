package com.nis.nistv

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nis.nistv.core.designsystem.theme.NisAccentAmber
import com.nis.nistv.core.designsystem.theme.NisAccentCyan
import com.nis.nistv.core.designsystem.theme.NisAccentGreen
import com.nis.nistv.core.designsystem.theme.NisAccentGreenPressed
import com.nis.nistv.core.designsystem.theme.NisAccentPrimary
import com.nis.nistv.core.designsystem.theme.NisAccentPrimaryContainer
import com.nis.nistv.core.designsystem.theme.NisAccentPrimaryLight
import com.nis.nistv.core.designsystem.theme.NisAccentPrimaryPressed
import com.nis.nistv.core.designsystem.theme.NisAccentViolet
import com.nis.nistv.core.designsystem.theme.NisBackground
import com.nis.nistv.core.designsystem.theme.NisBorder
import com.nis.nistv.core.designsystem.theme.NisBorderSubtle
import com.nis.nistv.core.designsystem.theme.NisBrandGradient
import com.nis.nistv.core.designsystem.theme.NisBrandGradientSoft
import com.nis.nistv.core.designsystem.theme.NisButtonShape
import com.nis.nistv.core.designsystem.theme.NisCardBackground
import com.nis.nistv.core.designsystem.theme.NisCardBorder
import com.nis.nistv.core.designsystem.theme.NisCardHover
import com.nis.nistv.core.designsystem.theme.NisChannelCardShape
import com.nis.nistv.core.designsystem.theme.NisChipShape
import com.nis.nistv.core.designsystem.theme.NisDarkColorScheme
import com.nis.nistv.core.designsystem.theme.NisDarkExtraColors
import com.nis.nistv.core.designsystem.theme.NisDimens
import com.nis.nistv.core.designsystem.theme.NisDivider
import com.nis.nistv.core.designsystem.theme.NisError
import com.nis.nistv.core.designsystem.theme.NisExtraColors
import com.nis.nistv.core.designsystem.theme.NisFeaturedCardShape
import com.nis.nistv.core.designsystem.theme.NisImmersiveBackground
import com.nis.nistv.core.designsystem.theme.NisImmersiveSurface
import com.nis.nistv.core.designsystem.theme.NisLightBackground
import com.nis.nistv.core.designsystem.theme.NisLightColorScheme
import com.nis.nistv.core.designsystem.theme.NisLightExtraColors
import com.nis.nistv.core.designsystem.theme.NisLightPrimary
import com.nis.nistv.core.designsystem.theme.NisLightSurface
import com.nis.nistv.core.preferences.AppThemeConfig
import com.nis.nistv.core.designsystem.theme.NisLiveEmerald
import com.nis.nistv.core.designsystem.theme.NisLiveRed
import com.nis.nistv.core.designsystem.theme.NisMediaCardShape
import com.nis.nistv.core.designsystem.theme.NisMotion
import com.nis.nistv.core.designsystem.theme.NisNavbarBg
import com.nis.nistv.core.designsystem.theme.NisNavbarBorder
import com.nis.nistv.core.designsystem.theme.NisPlayerScrim
import com.nis.nistv.core.designsystem.theme.NisPosterMediaCardShape
import com.nis.nistv.core.designsystem.theme.NisScrimMedium
import com.nis.nistv.core.designsystem.theme.NisScrimSoft
import com.nis.nistv.core.designsystem.theme.NisScrimStrong
import com.nis.nistv.core.designsystem.theme.NisShapes
import com.nis.nistv.core.designsystem.theme.NisSmallControlShape
import com.nis.nistv.core.designsystem.theme.NisSpacing
import com.nis.nistv.core.designsystem.theme.NisSuccess
import com.nis.nistv.core.designsystem.theme.NisSurface
import com.nis.nistv.core.designsystem.theme.NisSurfaceElevated
import com.nis.nistv.core.designsystem.theme.NisSurfaceHigh
import com.nis.nistv.core.designsystem.theme.NisTextExtras
import com.nis.nistv.core.designsystem.theme.NisTextMuted
import com.nis.nistv.core.designsystem.theme.NisTextPrimary
import com.nis.nistv.core.designsystem.theme.NisTextSecondary
import com.nis.nistv.core.designsystem.theme.NisTypography
import com.nis.nistv.core.designsystem.theme.NisWarning
import com.nis.nistv.core.designsystem.theme.NtvAccent
import com.nis.nistv.core.designsystem.theme.NtvCanvas
import com.nis.nistv.core.designsystem.theme.NtvCardBorder
import com.nis.nistv.core.designsystem.theme.NtvCyan
import com.nis.nistv.core.designsystem.theme.NtvGold
import com.nis.nistv.core.designsystem.theme.NtvSurface
import com.nis.nistv.core.designsystem.theme.NtvSurfaceElevated
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTokensTest {

    @Test
    fun testAllColorTokensExistAndMatchObsidian20Palette() {
        assertEquals(Color(0xFF0B0D12), NisBackground)
        assertEquals(Color(0xFF131823), NisSurface)
        assertEquals(Color(0xFF1A2130), NisSurfaceElevated)
        assertEquals(Color(0xFF232C3F), NisSurfaceHigh)
        assertEquals(Color(0xFF2A3347), NisBorder)
        assertEquals(Color(0xFF1E2536), NisBorderSubtle)
        assertEquals(NisBorder, NisCardBorder)
        assertEquals(NisBorder, NisDivider)
        assertEquals(Color(0xFF202942), NisCardHover)
        assertEquals(Color(0xF00B0D12), NisNavbarBg)
        assertEquals(NisBorder, NisNavbarBorder)

        assertEquals(Color(0xFF7A8FFF), NisAccentPrimary)
        assertEquals(Color(0xFF9DAEFF), NisAccentPrimaryLight)
        assertEquals(Color(0xFF6377F0), NisAccentPrimaryPressed)
        assertEquals(Color(0xFF232C5E), NisAccentPrimaryContainer)
        assertEquals(Color(0xFF3CCBFF), NisAccentCyan)
        assertEquals(Color(0xFFA978FF), NisAccentViolet)

        assertEquals(Color(0xFFFF4D5E), NisLiveRed)
        assertEquals(Color(0xFFFF5C6C), NisError)
        assertEquals(Color(0xFFF5A524), NisAccentAmber)
        assertEquals(NisAccentAmber, NisWarning)
        assertEquals(Color(0xFF34D399), NisLiveEmerald)
        assertEquals(NisLiveEmerald, NisSuccess)

        assertEquals(Color(0xFFF4F6FB), NisTextPrimary)
        assertEquals(Color(0xFFA9B2C6), NisTextSecondary)
        assertEquals(Color(0xFF8B94AA), NisTextMuted)
    }

    @Test
    fun testDeprecatedGreenAliasesResolveToNewPrimary() {
        @Suppress("DEPRECATION")
        assertEquals("Deprecated NisAccentGreen must alias NisAccentPrimary", NisAccentPrimary, NisAccentGreen)
        @Suppress("DEPRECATION")
        assertEquals("Deprecated NisAccentGreenPressed must alias NisAccentPrimaryPressed", NisAccentPrimaryPressed, NisAccentGreenPressed)
    }

    @Test
    fun testLegacyStyleAliasesResolveCleanly() {
        assertEquals(NisSurface, NisCardBackground)
        assertEquals(NisBackground, NtvCanvas)
        assertEquals(NisSurface, NtvSurface)
        assertEquals(NisSurfaceElevated, NtvSurfaceElevated)
        assertEquals(NisCardBorder, NtvCardBorder)
        assertEquals(NisAccentPrimary, NtvAccent)
        assertEquals(NisAccentCyan, NtvCyan)
        assertEquals(NisAccentAmber, NtvGold)
    }

    @Test
    fun testBrandGradientAndScrimTokens() {
        assertEquals(3, NisBrandGradient.size)
        assertEquals(NisAccentCyan, NisBrandGradient[0])
        assertEquals(NisAccentPrimary, NisBrandGradient[1])
        assertEquals(NisAccentViolet, NisBrandGradient[2])

        assertEquals(3, NisBrandGradientSoft.size)
        assertEquals(Color(0x660B0D12), NisScrimSoft)
        assertEquals(Color(0xB30B0D12), NisScrimMedium)
        assertEquals(Color(0xDE0B0D12), NisScrimStrong)
        assertEquals(Color(0xD9000000), NisPlayerScrim)
    }

    @Test
    fun testMaterial3DarkColorSchemeWired() {
        val cs = NisDarkColorScheme
        assertEquals(NisAccentPrimary, cs.primary)
        assertEquals(NisBackground, cs.onPrimary)
        assertEquals(NisAccentPrimaryContainer, cs.primaryContainer)
        assertEquals(NisAccentCyan, cs.secondary)
        assertEquals(NisAccentViolet, cs.tertiary)
        assertEquals(NisBackground, cs.background)
        assertEquals(NisSurface, cs.surface)
        assertEquals(NisSurfaceElevated, cs.surfaceVariant)
        assertEquals(NisBorder, cs.outline)
        assertEquals(NisBorderSubtle, cs.outlineVariant)
        assertEquals(NisError, cs.error)
        assertEquals(NisBackground, cs.surfaceContainerLowest)
        assertEquals(NisSurface, cs.surfaceContainerLow)
        assertEquals(NisSurfaceElevated, cs.surfaceContainerHigh)
        assertEquals(NisSurfaceHigh, cs.surfaceContainerHighest)
    }

    @Test
    fun testExtraColorsContainerDefaults() {
        val extra = NisExtraColors()
        assertEquals(NisAccentCyan, extra.accentCyan)
        assertEquals(NisAccentViolet, extra.accentViolet)
        assertEquals(NisLiveRed, extra.live)
        assertEquals(NisSuccess, extra.success)
        assertEquals(NisWarning, extra.warning)
        assertEquals(NisTextMuted, extra.textMuted)
        assertEquals(NisBorderSubtle, extra.borderSubtle)
        assertEquals(NisScrimMedium, extra.scrimMedium)
    }

    @Test
    fun testShapesScaleAndComponentMappings() {
        assertNotNull(NisShapes.extraSmall)
        assertNotNull(NisShapes.small)
        assertNotNull(NisShapes.medium)
        assertNotNull(NisShapes.large)
        assertNotNull(NisShapes.extraLarge)

        assertEquals(NisMediaCardShape, NisPosterMediaCardShape)
        assertEquals(NisShapes.medium, NisButtonShape)
        assertEquals(NisShapes.medium, NisChannelCardShape)
        assertEquals(NisShapes.large, NisFeaturedCardShape)
        assertEquals(NisShapes.extraSmall, NisSmallControlShape)
    }

    @Test
    fun testSpacingScaleAndDimens() {
        val spacing = NisSpacing()
        assertEquals(4.dp, spacing.scale4)
        assertEquals(8.dp, spacing.scale8)
        assertEquals(12.dp, spacing.scale12)
        assertEquals(16.dp, spacing.scale16)
        assertEquals(20.dp, spacing.scale20)
        assertEquals(24.dp, spacing.scale24)
        assertEquals(32.dp, spacing.scale32)
        assertEquals(40.dp, spacing.scale40)
        assertEquals(48.dp, spacing.scale48)

        assertEquals(48.dp, NisDimens.TouchTarget)
        assertEquals(20.dp, NisDimens.IconSmall)
        assertEquals(24.dp, NisDimens.IconMedium)
        assertEquals(32.dp, NisDimens.IconLarge)
        assertEquals(840.dp, NisDimens.MaxContentWidth)
        assertEquals(16.dp, NisDimens.ScreenPaddingPhone)
        assertEquals(24.dp, NisDimens.ScreenPaddingTablet)
    }

    @Test
    fun testMotionTokens() {
        assertEquals(120, NisMotion.DurationFastMs)
        assertEquals(220, NisMotion.DurationNormalMs)
        assertEquals(360, NisMotion.DurationSlowMs)

        assertNotNull(NisMotion.EasingEmphasized)
        assertNotNull(NisMotion.EasingStandard)
        assertEquals(0.7f, NisMotion.PressDampingRatio, 0.001f)
        assertEquals(800f, NisMotion.PressStiffness, 0.001f)

        val spring = NisMotion.pressSpring<Float>()
        assertEquals(0.7f, spring.dampingRatio, 0.001f)
        assertEquals(800f, spring.stiffness, 0.001f)
    }

    @Test
    fun testTypographyScaleAndExtras() {
        assertEquals(32.sp, NisTypography.displayMedium.fontSize)
        assertEquals(28.sp, NisTypography.headlineLarge.fontSize)
        assertEquals(22.sp, NisTypography.headlineMedium.fontSize)
        assertEquals(20.sp, NisTypography.headlineSmall.fontSize)
        assertEquals(18.sp, NisTypography.titleLarge.fontSize)
        assertEquals(16.sp, NisTypography.titleMedium.fontSize)
        assertEquals(14.sp, NisTypography.titleSmall.fontSize)
        assertEquals(16.sp, NisTypography.bodyLarge.fontSize)
        assertEquals(14.sp, NisTypography.bodyMedium.fontSize)
        assertEquals(12.sp, NisTypography.bodySmall.fontSize)
        assertEquals(14.sp, NisTypography.labelLarge.fontSize)
        assertEquals(12.sp, NisTypography.labelMedium.fontSize)
        assertEquals(11.sp, NisTypography.labelSmall.fontSize)

        val extras = NisTextExtras()
        assertEquals(26.sp, extras.hero.fontSize)
        assertEquals(10.sp, extras.micro.fontSize)
        assertEquals(10.5.sp, extras.tagline.fontSize)
        assertEquals(12.sp, extras.mono.fontSize)
    }

    @Test
    fun testLightColorSchemeMapping() {
        assertEquals(NisLightPrimary, NisLightColorScheme.primary)
        assertEquals(Color.White, NisLightColorScheme.onPrimary)
        assertEquals(NisLightBackground, NisLightColorScheme.background)
        assertEquals(NisLightSurface, NisLightColorScheme.surface)
    }

    @Test
    fun testExtraColorsTiers() {
        assertTrue(NisDarkExtraColors.isDark)
        assertFalse(NisLightExtraColors.isDark)

        assertEquals(NisBackground, NisDarkExtraColors.background)
        assertEquals(NisLightBackground, NisLightExtraColors.background)
        assertEquals(NisSurface, NisDarkExtraColors.surface)
        assertEquals(NisLightSurface, NisLightExtraColors.surface)

        // Immersive surfaces must be dark in both
        assertEquals(NisImmersiveBackground, NisDarkExtraColors.immersiveBackground)
        assertEquals(NisImmersiveBackground, NisLightExtraColors.immersiveBackground)
        assertEquals(NisImmersiveSurface, NisDarkExtraColors.immersiveSurface)
        assertEquals(NisImmersiveSurface, NisLightExtraColors.immersiveSurface)
    }

    @Test
    fun testAppThemeConfigEnum() {
        assertEquals(3, AppThemeConfig.values().size)
        assertEquals(AppThemeConfig.SYSTEM, AppThemeConfig.valueOf("SYSTEM"))
        assertEquals(AppThemeConfig.DARK, AppThemeConfig.valueOf("DARK"))
        assertEquals(AppThemeConfig.LIGHT, AppThemeConfig.valueOf("LIGHT"))
    }
}
