package com.nis.nistv.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nis.nistv.core.designsystem.icon.NisIcons
import com.nis.nistv.core.designsystem.theme.*

data class IndianLanguageItem(
    val name: String,
    val nativeScript: String,
    val channelCount: Int,
    val accentColor: Color
)

val INDIAN_LANGUAGES = listOf(
    IndianLanguageItem("Hindi", "हिंदी", 328, Color(0xFFE50914)),
    IndianLanguageItem("Tamil", "தமிழ்", 132, Color(0xFF6366F1)),
    IndianLanguageItem("Telugu", "తెలుగు", 67, Color(0xFF00E5FF)),
    IndianLanguageItem("Bengali", "বাংলা", 85, Color(0xFFF59E0B)),
    IndianLanguageItem("Malayalam", "മലയാളം", 63, Color(0xFF10B981)),
    IndianLanguageItem("Kannada", "ಕನ್ನಡ", 28, Color(0xFFEC4899)),
    IndianLanguageItem("Punjabi", "ਪੰਜਾਬੀ", 45, Color(0xFF8B5CF6)),
    IndianLanguageItem("Marathi", "मराठी", 21, Color(0xFFF97316)),
    IndianLanguageItem("Urdu", "اردو", 77, Color(0xFF14B8A6)),
    IndianLanguageItem("Gujarati", "ગુજરાતી", 19, Color(0xFF06B6D4)),
    IndianLanguageItem("Assamese", "অসমীয়া", 16, Color(0xFF84CC16)),
    IndianLanguageItem("Odia", "ଓଡ଼ିଆ", 14, Color(0xFFEAB308)),
    IndianLanguageItem("Bhojpuri", "भोजपुरी", 10, Color(0xFFD946EF))
)

/**
 * First-Launch Indian TV Language Selection Dialog.
 * Prompts user on first open to pick their primary language,
 * re-ordering and recommending their language channels first.
 */
@Composable
fun LanguageOnboardingDialog(
    initialLanguage: String = "Tamil",
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLanguage by remember { mutableStateOf(initialLanguage) }

    Dialog(
        onDismissRequest = { /* Force explicit confirmation on first launch */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NisScrimStrong),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(NisDialogShape)
                    .background(NisTheme.colors.surfaceElevated)
                    .border(
                        width = 1.dp,
                        color = NisTheme.colors.borderSubtle,
                        shape = NisDialogShape
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Brand Dot
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(NisAccentPrimary, NisAccentCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NisIcons.LiveTv,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Choose Your Language",
                        color = NisTheme.colors.textPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Select your preferred language. We'll tailor your home feed and recommend your language channels first.",
                        color = NisTheme.colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Language 2-Column Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        items(INDIAN_LANGUAGES) { item ->
                            val isSelected = selectedLanguage.equals(item.name, ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(NisMediaCardShape)
                                    .background(
                                        if (isSelected) NisAccentPrimaryContainer else NisTheme.colors.surface
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) NisAccentCyan else NisTheme.colors.borderSubtle,
                                        shape = NisMediaCardShape
                                    )
                                    .nisPressable { selectedLanguage = item.name }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.nativeScript,
                                            color = if (isSelected) NisAccentCyan else NisTheme.colors.textPrimary,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(NisAccentCyan),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = NisIcons.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = item.name,
                                        color = NisTheme.colors.textPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "${item.channelCount} Channels",
                                        color = if (isSelected) NisAccentCyan.copy(alpha = 0.9f) else NisTheme.colors.textMuted,
                                        style = NisTheme.textExtras.micro,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Confirm Action Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(NisMediaCardShape)
                            .background(
                                Brush.horizontalGradient(NisBrandGradient)
                            )
                            .nisPressable {
                                onLanguageSelected(selectedLanguage)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Confirm & Explore in $selectedLanguage",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
