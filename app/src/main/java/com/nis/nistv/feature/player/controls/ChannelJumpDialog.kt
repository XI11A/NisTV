package com.nis.nistv.feature.player.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nis.nistv.core.designsystem.theme.*
import com.nis.nistv.core.designsystem.theme.NisTextPrimary

@Composable
fun ChannelJumpDialog(
    totalChannels: Int,
    contextTitle: String,
    onJump: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var inputNumber by remember { mutableStateOf("") }
    val parsed = inputNumber.toIntOrNull()
    val isValid = parsed != null && parsed in 1..totalChannels

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(NisDialogShape)
                .background(NisTheme.colors.surfaceElevated)
                .border(1.dp, NisTheme.colors.borderSubtle, NisDialogShape)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Jump to Channel",
                color = NisTheme.colors.textPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter channel number (1 to $totalChannels) in $contextTitle",
                color = NisTheme.colors.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = inputNumber,
                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) inputNumber = it },
                singleLine = true,
                placeholder = { Text("e.g. 12", color = NisTheme.colors.textMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NisAccentPrimary,
                    unfocusedBorderColor = NisTheme.colors.borderSubtle,
                    focusedTextColor = NisTheme.colors.textPrimary,
                    unfocusedTextColor = NisTheme.colors.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = NisTheme.colors.textMuted)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        if (parsed != null) {
                            onJump(parsed)
                        }
                    },
                    enabled = isValid
                ) {
                    Text(
                        "Jump",
                        color = if (isValid) NisAccentPrimary else NisTheme.colors.textMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
