package com.nis.nistv.feature.parental

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.nis.nistv.core.designsystem.theme.*

@Composable
fun ParentalPinDialog(
    title: String = "Enter Parental PIN",
    subtitle: String = "Please enter your 4-6 digit PIN to proceed.",
    isLockedOut: Boolean = false,
    lockoutRemainingSec: Long = 0L,
    errorMessage: String? = null,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
    onForgotPinClick: () -> Unit
) {
    var pinText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = NisDialogShape,
        containerColor = NisSurface,
        icon = {
            Icon(Icons.Default.Lock, contentDescription = null, tint = NisAccentPrimary)
        },
        title = {
            Text(title, color = NisTextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(subtitle, color = NisTextSecondary, style = MaterialTheme.typography.bodyMedium)

                if (isLockedOut) {
                    val mins = lockoutRemainingSec / 60
                    val secs = lockoutRemainingSec % 60
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Too many failed attempts. Locked out for %02d:%02d".format(mins, secs),
                        color = NisAccentPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("4-6 Digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = errorMessage != null,
                        supportingText = errorMessage?.let { { Text(it, color = NisAccentPrimary) } },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NisAccentPrimary,
                            unfocusedBorderColor = NisTextMuted,
                            focusedTextColor = NisTextPrimary,
                            unfocusedTextColor = NisTextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onForgotPinClick,
                    modifier = Modifier.padding(0.dp)
                ) {
                    Text("Forgot PIN? Reset with Screen Lock", color = NisTextMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            if (!isLockedOut) {
                TextButton(
                    onClick = {
                        if (pinText.length in 4..6) {
                            onConfirm(pinText)
                        }
                    },
                    enabled = pinText.length in 4..6
                ) {
                    Text("Confirm", color = if (pinText.length in 4..6) NisAccentPrimary else NisTextMuted, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = NisTextMuted)
            }
        }
    )
}
