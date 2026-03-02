package com.idk.anypay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.idk.anypay.ui.theme.*

@Composable
fun LockScreen(
    onAuthenticate: () -> Unit,
    errorMessage: String? = null
) {
    LaunchedEffect(Unit) { onAuthenticate() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── App icon ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text  = "AnyPay",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text  = "Offline UPI Payments",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(48.dp))

            // ── Error notice ──────────────────────────────────────────────
            if (errorMessage != null) {
                Text(
                    text      = errorMessage,
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(bottom = 20.dp)
                )
            }

            Button(
                onClick  = onAuthenticate,
                modifier = Modifier.fillMaxWidth(),
                shape    = MaterialTheme.shapes.small,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Unlock with Biometrics")
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text      = "Your credentials are encrypted and stored securely on this device",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Lock Screen – Light Default")
@Composable
private fun LockScreenPreview() {
    AnyPayTheme(darkTheme = false) {
        LockScreen(onAuthenticate = {})
    }
}

@Preview(showBackground = true, name = "Lock Screen – Dark Default")
@Composable
private fun LockScreenDarkPreview() {
    AnyPayTheme(darkTheme = true) {
        LockScreen(onAuthenticate = {})
    }
}

@Preview(showBackground = true, name = "Lock Screen – With Error")
@Composable
private fun LockScreenErrorPreview() {
    AnyPayTheme(darkTheme = false) {
        LockScreen(
            onAuthenticate = {},
            errorMessage   = "Authentication failed. Please try again."
        )
    }
}
