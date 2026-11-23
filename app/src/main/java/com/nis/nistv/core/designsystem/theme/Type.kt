package com.nis.nistv.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nis.nistv.R

// =============================================================================
// NisTV Revamped — Cinematic Obsidian 2.0 Typography System
// =============================================================================

/**
 * Static Inter font family weights (Regular 400, Medium 500, SemiBold 600, Bold 700).
 * Static TTFs are bundled in res/font to maintain compatibility with minSdk 24.
 */
val NisFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

/**
 * Centered line height style without trimming to guarantee multi-script safety
 * for complex South Asian scripts (Tamil, Devanagari/Hindi, Telugu, Kannada, Malayalam).
 */
val NisLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

/**
 * Complete, explicit Material 3 typography scale mapped to the Inter font family.
 * Body styles feature Normal (400) weight for long-form legibility on deep obsidian canvases.
 */
val NisTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp,
        lineHeightStyle = NisLineHeightStyle
    ),
    displayMedium = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
        lineHeightStyle = NisLineHeightStyle
    ),
    displaySmall = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
        lineHeightStyle = NisLineHeightStyle
    ),
    headlineLarge = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
        lineHeightStyle = NisLineHeightStyle
    ),
    headlineMedium = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
        lineHeightStyle = NisLineHeightStyle
    ),
    headlineSmall = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        lineHeightStyle = NisLineHeightStyle
    ),
    titleLarge = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = NisLineHeightStyle
    ),
    titleMedium = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = NisLineHeightStyle
    ),
    titleSmall = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = NisLineHeightStyle
    ),
    bodyLarge = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = NisLineHeightStyle
    ),
    bodyMedium = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = NisLineHeightStyle
    ),
    bodySmall = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
        lineHeightStyle = NisLineHeightStyle
    ),
    labelLarge = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = NisLineHeightStyle
    ),
    labelMedium = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        lineHeightStyle = NisLineHeightStyle
    ),
    labelSmall = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
        lineHeightStyle = NisLineHeightStyle
    )
)

/**
 * Extended typography tokens for custom display roles not covered by standard Material 3 slots.
 */
@Immutable
data class NisTextExtras(
    val hero: TextStyle = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
        lineHeightStyle = NisLineHeightStyle
    ),
    val micro: TextStyle = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.4.sp,
        lineHeightStyle = NisLineHeightStyle
    ),
    val tagline: TextStyle = TextStyle(
        fontFamily = NisFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.4.sp,
        lineHeightStyle = NisLineHeightStyle
    ),
    val mono: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        lineHeightStyle = NisLineHeightStyle
    )
)

val LocalNisTextExtras = staticCompositionLocalOf { NisTextExtras() }

// =============================================================================
// Previews for Multi-Script & Font Scaling Safety Verification
// =============================================================================

@Composable
private fun LanguageSafetySampleContent() {
    Column(
        modifier = Modifier
            .background(NisBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "சன் டிவி HD — நேரலை ஒளிபரப்பு மற்றும் திரைப்படங்கள்",
            style = NisTypography.titleMedium,
            color = NisTextPrimary
        )
        Text(
            text = "விறுவிறுப்பான தமிழ் பொழுதுபோக்கு நிகழ்ச்சிகள் உடனுக்குடன்",
            style = NisTypography.bodyMedium,
            color = NisTextSecondary
        )
        Text(
            text = "स्टार प्लस HD — लाइव प्रसारण एवं धारावाहिक",
            style = NisTypography.titleMedium,
            color = NisTextPrimary
        )
        Text(
            text = "भारत का सबसे लोकप्रिय मनोरंजन चैनल और ताज़ा कार्यक्रम",
            style = NisTypography.bodyMedium,
            color = NisTextSecondary
        )
        Text(
            text = "ఈటీవీ హెచ్‌డీ — ప్రత్యక్ష ప్రసారం మరియు సినిమాలు",
            style = NisTypography.titleMedium,
            color = NisTextPrimary
        )
        Text(
            text = "తెలుగు సాంస్కృతిక మరియు వినోద ప్రసారాలు",
            style = NisTypography.bodyMedium,
            color = NisTextSecondary
        )
    }
}

@Preview(name = "FontScale 1.0 (Normal)", fontScale = 1.0f, showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
fun NisTypographyLanguageSafetyNormalPreview() {
    NisTvTheme {
        LanguageSafetySampleContent()
    }
}

@Preview(name = "FontScale 1.3 (Large Accessibility)", fontScale = 1.3f, showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
fun NisTypographyLanguageSafetyLargePreview() {
    NisTvTheme {
        LanguageSafetySampleContent()
    }
}
