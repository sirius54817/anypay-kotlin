package com.idk.anypay.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.idk.anypay.data.model.UserCredentials
import com.idk.anypay.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    credentials: UserCredentials?,
    onUpdatePin: (String) -> Unit,
    onClearData: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showClearDataDialog  by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── Account ───────────────────────────────────────────────────────
        SettingsSection(title = "Account") {
            if (credentials != null) {
                SettingsItem(
                    icon     = Icons.Default.Phone,
                    title    = "Mobile Number",
                    subtitle = "+91 ${credentials.mobileNumber}"
                )
                SettingsDivider()
                SettingsItem(
                    icon     = Icons.Default.AccountBalance,
                    title    = "Bank",
                    subtitle = credentials.bankName
                )
                SettingsDivider()
                SettingsItem(
                    icon     = Icons.Default.Numbers,
                    title    = "IFSC Code",
                    subtitle = credentials.bankIfsc
                )
            } else {
                SettingsItem(
                    icon     = Icons.Default.Person,
                    title    = "Not set up",
                    subtitle = "Complete onboarding to add credentials"
                )
            }
        }

        // ── Security ──────────────────────────────────────────────────────
        SettingsSection(title = "Security") {
            SettingsItem(
                icon     = Icons.Default.Lock,
                title    = "Change UPI PIN",
                subtitle = "Update your UPI PIN",
                onClick  = { showChangePinDialog = true }
            )
            SettingsDivider()
            SettingsItem(
                icon     = Icons.Default.Accessibility,
                title    = "Accessibility Service",
                subtitle = "Enable for USSD automation",
                onClick  = onOpenAccessibilitySettings
            )
        }

        // ── Data ──────────────────────────────────────────────────────────
        SettingsSection(title = "Data") {
            SettingsItem(
                icon          = Icons.Default.DeleteForever,
                title         = "Clear All Data",
                subtitle      = "Remove all stored data and start fresh",
                onClick       = { showClearDataDialog = true },
                isDestructive = true
            )
        }

        // ── About ─────────────────────────────────────────────────────────
        SettingsSection(title = "About") {
            SettingsItem(
                icon     = Icons.Default.Info,
                title    = "Version",
                subtitle = "1.0.0"
            )
            SettingsDivider()
            SettingsItem(
                icon     = Icons.Default.Security,
                title    = "Privacy",
                subtitle = "All data stored locally on device"
            )
        }

        // ── Security notice ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text  = "Your data is secure",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "All credentials are encrypted using Android Keystore and never leave your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    if (showChangePinDialog) {
        ChangePinDialog(
            onDismiss = { showChangePinDialog = false },
            onConfirm = { newPin -> onUpdatePin(newPin); showChangePinDialog = false }
        )
    }
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon  = { Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed) },
            title = { Text("Clear All Data?") },
            text  = { Text("This will delete all your stored credentials and transaction history. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { onClearData(); showClearDataDialog = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape   = MaterialTheme.shapes.small
                ) { Text("Clear All") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearDataDialog = false },
                    shape   = MaterialTheme.shapes.small,
                    border  = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) { Text("Cancel") }
            }
        )
    }
}

// ─── Settings Section ─────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text     = title.uppercase(),
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface),
            content = content
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}

// ─── Settings Item ────────────────────────────────────────────────────────────

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    isDestructive: Boolean = false
) {
    val titleColor    = if (isDestructive) ErrorRed else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isDestructive) ErrorRed.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconColor     = if (isDestructive) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = titleColor)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor)
        }
        if (onClick != null) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─── Change PIN Dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var newPin     by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showPin    by remember { mutableStateOf(false) }
    val isValid = newPin.length in 4..6 && newPin == confirmPin

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change UPI PIN") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value               = newPin,
                    onValueChange       = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                    label               = { Text("New PIN") },
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions     = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    trailingIcon        = {
                        IconButton(onClick = { showPin = !showPin }) {
                            Icon(if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    singleLine = true,
                    shape      = MaterialTheme.shapes.small,
                    modifier   = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value               = confirmPin,
                    onValueChange       = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label               = { Text("Confirm PIN") },
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions     = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError             = confirmPin.isNotEmpty() && newPin != confirmPin,
                    supportingText      = if (confirmPin.isNotEmpty() && newPin != confirmPin)
                        { { Text("PINs do not match") } } else null,
                    singleLine = true,
                    shape      = MaterialTheme.shapes.small,
                    modifier   = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(newPin) }, enabled = isValid, shape = MaterialTheme.shapes.small) {
                Text("Update")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape  = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) { Text("Cancel") }
        }
    )
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Settings – Light With Credentials")
@Composable
private fun SettingsScreenPreview() {
    AnyPayTheme(darkTheme = false) {
        SettingsScreen(
            credentials = UserCredentials(
                mobileNumber    = "9876543210",
                upiPin          = "1234",
                bankName        = "State Bank of India",
                bankIfsc        = "SBIN0001234",
                cardLastSixDigits = "123456",
                cardExpiryMonth = "01",
                cardExpiryYear  = "28",
                isSetupComplete = true
            ),
            onUpdatePin = {}, onClearData = {}, onOpenAccessibilitySettings = {}
        )
    }
}

@Preview(showBackground = true, name = "Settings – Dark With Credentials")
@Composable
private fun SettingsScreenDarkPreview() {
    AnyPayTheme(darkTheme = true) {
        SettingsScreen(
            credentials = UserCredentials(
                mobileNumber    = "9876543210",
                upiPin          = "1234",
                bankName        = "State Bank of India",
                bankIfsc        = "SBIN0001234",
                cardLastSixDigits = "123456",
                cardExpiryMonth = "01",
                cardExpiryYear  = "28",
                isSetupComplete = true
            ),
            onUpdatePin = {}, onClearData = {}, onOpenAccessibilitySettings = {}
        )
    }
}

@Preview(showBackground = true, name = "Settings – No Credentials")
@Composable
private fun SettingsScreenNoCredsPreview() {
    AnyPayTheme(darkTheme = false) {
        SettingsScreen(
            credentials = null,
            onUpdatePin = {}, onClearData = {}, onOpenAccessibilitySettings = {}
        )
    }
}
