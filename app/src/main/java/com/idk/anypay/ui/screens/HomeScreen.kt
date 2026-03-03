package com.idk.anypay.ui.screens

import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.idk.anypay.data.model.*
import com.idk.anypay.ui.theme.*
import java.util.concurrent.Executors
import kotlinx.coroutines.delay

// ─── Home Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    recentTransactions: List<Transaction>,
    lastBalance: Double,
    hasPhonePermission: Boolean,
    isAccessibilityEnabled: Boolean,
    hasOverlayPermission: Boolean = true,
    requiresRestrictedSettings: Boolean = false,
    onCheckBalance: () -> Unit,
    onScanToPay: (UpiPaymentInfo) -> Unit,
    onSendMoney: () -> Unit,
    onViewHistory: () -> Unit,
    hasCameraPermission: Boolean = false,
    onRequestCameraPermission: () -> Unit = {},
    onRequestPermissions: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppInfo: () -> Unit = {},
    onRequestOverlayPermission: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Setup banner ──────────────────────────────────────────────────
        if (!hasPhonePermission || !isAccessibilityEnabled || !hasOverlayPermission) {
            item {
                ServiceStatusCard(
                    hasPhonePermission = hasPhonePermission,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    hasOverlayPermission = hasOverlayPermission,
                    requiresRestrictedSettings = requiresRestrictedSettings,
                    onRequestPermissions = onRequestPermissions,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onOpenAppInfo = onOpenAppInfo,
                    onRequestOverlayPermission = onRequestOverlayPermission
                )
            }
        }

        // ── Embedded QR scanner ───────────────────────────────────────────
        item {
            EmbeddedQrScanner(
                hasCameraPermission    = hasCameraPermission,
                onRequestCameraPermission = onRequestCameraPermission,
                onScanResult           = onScanToPay
            )
        }

        // ── Quick actions ─────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Check Balance",
                    icon = Icons.Default.AccountBalance,
                    color = BalanceBlue,
                    onClick = onCheckBalance,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "Send Money",
                    icon = Icons.Default.ArrowUpward,
                    color = SendRed,
                    onClick = onSendMoney,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Recent transactions header ────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = onViewHistory,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("View all", style = MaterialTheme.typography.labelMedium)
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // ── Transaction list / empty state ────────────────────────────────
        if (recentTransactions.isEmpty()) {
            item { EmptyTransactionsState() }
        } else {
            items(recentTransactions) { TransactionItem(transaction = it) }
        }
    }
}

// ─── Service Status Card ──────────────────────────────────────────────────────

@Composable
private fun ServiceStatusCard(
    hasPhonePermission: Boolean,
    isAccessibilityEnabled: Boolean,
    hasOverlayPermission: Boolean,
    requiresRestrictedSettings: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(BorderStroke(1.dp, borderColor), MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Setup Required",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        if (!hasPhonePermission) PermissionRow(
            label = "Phone Permission",
            subtitle = "Required for USSD calls",
            actionLabel = "Grant",
            onClick = onRequestPermissions
        )

        // Accessibility – show two-step guide on Android 13+
        if (!isAccessibilityEnabled) {
            if (requiresRestrictedSettings) {
                // Step 1 – App Info → Allow restricted settings
                HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Accessibility Service — 2 steps needed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    // Step 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.error),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("1", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow Restricted Settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "App Info → ⋮ menu → Allow restricted settings",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenAppInfo,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Open", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    // Step 2
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("2", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Accessibility Service",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Accessibility → Installed services → AnyPay",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenAccessibilitySettings,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Enable", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else {
                PermissionRow(
                    label = "Accessibility Service",
                    subtitle = "Required for USSD automation",
                    actionLabel = "Enable",
                    onClick = onOpenAccessibilitySettings
                )
            }
        }

        if (!hasOverlayPermission) PermissionRow(
            label = "Overlay Permission",
            subtitle = "For seamless USSD experience",
            actionLabel = "Allow",
            onClick = onRequestOverlayPermission
        )
    }
}

@Composable
private fun PermissionRow(
    label: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
            )
        }
        Spacer(Modifier.width(12.dp))
        OutlinedButton(
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(actionLabel, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ─── Quick Action Card ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyTransactionsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                MaterialTheme.shapes.medium
            )
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Receipt,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = "No transactions yet",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Your transactions will appear here",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Transaction Item ─────────────────────────────────────────────────────────

@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: (() -> Unit)? = null
) {
    val iconColor = when (transaction.type) {
        TransactionType.SEND          -> SendRed
        TransactionType.RECEIVE       -> ReceiveGreen
        TransactionType.BALANCE_CHECK -> BalanceBlue
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                MaterialTheme.shapes.medium
            )
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(MaterialTheme.shapes.small)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                when (transaction.type) {
                    TransactionType.SEND          -> Icons.Default.ArrowUpward
                    TransactionType.RECEIVE       -> Icons.Default.ArrowDownward
                    TransactionType.BALANCE_CHECK -> Icons.Default.AccountBalance
                },
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (transaction.type) {
                    TransactionType.SEND          -> transaction.recipientVpa.ifEmpty { "Payment" }
                    TransactionType.RECEIVE       -> transaction.recipientName.ifEmpty { "Received" }
                    TransactionType.BALANCE_CHECK -> "Balance Check"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = transaction.formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.message.isNotEmpty() && transaction.type == TransactionType.SEND) {
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(Color(transaction.category.color).copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = transaction.category.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(transaction.category.color)
                        )
                    }
                }
            }
        }

        // Amount + status
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = transaction.formattedAmount,
                style = MaterialTheme.typography.titleSmall,
                color = transaction.amountColor
            )
            Spacer(Modifier.height(3.dp))
            val statusBg = when (transaction.status) {
                TransactionStatus.SUCCESS -> SuccessGreen.copy(alpha = 0.1f)
                TransactionStatus.FAILED  -> ErrorRed.copy(alpha = 0.1f)
                TransactionStatus.PENDING -> PendingAmber.copy(alpha = 0.1f)
            }
            val statusFg = when (transaction.status) {
                TransactionStatus.SUCCESS -> SuccessGreen
                TransactionStatus.FAILED  -> ErrorRed
                TransactionStatus.PENDING -> PendingAmber
            }
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(statusBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = transaction.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusFg
                )
            }
        }
    }
}

// ─── Embedded QR Scanner ─────────────────────────────────────────────────────

@Composable
private fun EmbeddedQrScanner(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    onScanResult: (UpiPaymentInfo) -> Unit
) {
    val lifecycleOwner  = LocalLifecycleOwner.current
    val context         = LocalContext.current
    // Reset scannedOnce after 2 s so the user can scan again without re-navigating
    var scannedOnce     by remember { mutableStateOf(false) }
    val cameraExecutor  = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(scannedOnce) {
        if (scannedOnce) {
            delay(2_000)
            scannedOnce = false
        }
    }

    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text     = "Scan QR to Pay",
            style    = MaterialTheme.typography.labelLarge,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(MaterialTheme.shapes.large)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), MaterialTheme.shapes.large)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                // key() forces the AndroidView to fully recreate when permission state changes
                key(hasCameraPermission) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                scaleType            = PreviewView.ScaleType.FILL_CENTER
                                implementationMode   = PreviewView.ImplementationMode.COMPATIBLE
                            }

                            val future = ProcessCameraProvider.getInstance(ctx)
                            future.addListener({
                                try {
                                    val provider = future.get()

                                    val preview = androidx.camera.core.Preview.Builder()
                                        .build()
                                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also { ia ->
                                            ia.setAnalyzer(cameraExecutor) { proxy ->
                                                if (!scannedOnce) {
                                                    processImage(proxy) { result ->
                                                        if (result != null && !scannedOnce) {
                                                            scannedOnce = true
                                                            onScanResult(result)
                                                        }
                                                    }
                                                } else {
                                                    proxy.close()
                                                }
                                            }
                                        }

                                    // Unbind everything before rebinding
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        analysis
                                    )
                                } catch (e: Exception) {
                                    Log.e("EmbeddedQr", "Camera init failed: ${e.message}", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Corner bracket viewfinder overlay
                QrCornerOverlay()

                // Bottom hint pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text  = if (scannedOnce) "✓ QR detected…" else "Point at a UPI QR code",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (scannedOnce) Color(0xFF22C55E) else Color.White
                    )
                }

            } else {
                // No permission — show placeholder + grant button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text  = "Camera access needed to scan QR",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    OutlinedButton(
                        onClick = onRequestCameraPermission,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Grant Camera Permission")
                    }
                }
            }
        }
    }
}

// ─── QR Corner Bracket Overlay ────────────────────────────────────────────────

@Composable
private fun QrCornerOverlay() {
    val stroke = MaterialTheme.colorScheme.primary
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val bracketLen = 36.dp.toPx()
        val thickness  = 3.dp.toPx()
        val margin     = 36.dp.toPx()
        val corners    = listOf(
            // top-left
            Triple(margin, margin, Pair(1f, 1f)),
            // top-right
            Triple(size.width - margin, margin, Pair(-1f, 1f)),
            // bottom-left
            Triple(margin, size.height - margin, Pair(1f, -1f)),
            // bottom-right
            Triple(size.width - margin, size.height - margin, Pair(-1f, -1f))
        )
        corners.forEach { (cx, cy, dir) ->
            val (dx, dy) = dir
            // horizontal arm
            drawLine(stroke, androidx.compose.ui.geometry.Offset(cx, cy),
                androidx.compose.ui.geometry.Offset(cx + dx * bracketLen, cy),
                strokeWidth = thickness, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            // vertical arm
            drawLine(stroke, androidx.compose.ui.geometry.Offset(cx, cy),
                androidx.compose.ui.geometry.Offset(cx, cy + dy * bracketLen),
                strokeWidth = thickness, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

private val sampleTransactions = listOf(
    Transaction(
        type = TransactionType.SEND,
        amount = 500.0,
        recipientVpa = "john@upi",
        recipientName = "John",
        status = TransactionStatus.SUCCESS,
        message = "Lunch zomato"
    ),
    Transaction(
        type = TransactionType.RECEIVE,
        amount = 1000.0,
        recipientName = "Alice",
        status = TransactionStatus.SUCCESS
    ),
    Transaction(
        type = TransactionType.BALANCE_CHECK,
        amount = 0.0,
        balance = 12345.67,
        status = TransactionStatus.SUCCESS
    )
)

@Preview(showBackground = true, name = "Home – Light / All Good")
@Composable
private fun HomeScreenPreview() {
    AnyPayTheme(darkTheme = false) {
        HomeScreen(
            recentTransactions = sampleTransactions,
            lastBalance = 12345.67,
            hasPhonePermission = true,
            isAccessibilityEnabled = true,
            hasOverlayPermission = true,
            onCheckBalance = {},
            onScanToPay = { _ -> },
            onSendMoney = {},
            onViewHistory = {},
            onRequestPermissions = {},
            onOpenAccessibilitySettings = {},
            onRequestOverlayPermission = {}
        )
    }
}

@Preview(showBackground = true, name = "Home – Dark / All Good")
@Composable
private fun HomeScreenDarkPreview() {
    AnyPayTheme(darkTheme = true) {
        HomeScreen(
            recentTransactions = sampleTransactions,
            lastBalance = 12345.67,
            hasPhonePermission = true,
            isAccessibilityEnabled = true,
            hasOverlayPermission = true,
            onCheckBalance = {},
            onScanToPay = { _ -> },
            onSendMoney = {},
            onViewHistory = {},
            onRequestPermissions = {},
            onOpenAccessibilitySettings = {},
            onRequestOverlayPermission = {}
        )
    }
}

@Preview(showBackground = true, name = "Home – Permissions Missing")
@Composable
private fun HomeScreenPermissionMissingPreview() {
    AnyPayTheme(darkTheme = false) {
        HomeScreen(
            recentTransactions = emptyList(),
            lastBalance = 0.0,
            hasPhonePermission = false,
            isAccessibilityEnabled = false,
            hasOverlayPermission = false,
            requiresRestrictedSettings = false,
            onCheckBalance = {},
            onScanToPay = { _ -> },
            onSendMoney = {},
            onViewHistory = {},
            onRequestPermissions = {},
            onOpenAccessibilitySettings = {},
            onRequestOverlayPermission = {}
        )
    }
}

@Preview(showBackground = true, name = "Home – Restricted Settings needed (Android 13+)")
@Composable
private fun HomeScreenRestrictedSettingsPreview() {
    AnyPayTheme(darkTheme = false) {
        HomeScreen(
            recentTransactions = emptyList(),
            lastBalance = 0.0,
            hasPhonePermission = true,
            isAccessibilityEnabled = false,
            hasOverlayPermission = true,
            requiresRestrictedSettings = true,
            onCheckBalance = {},
            onScanToPay = { _ -> },
            onSendMoney = {},
            onViewHistory = {},
            onRequestPermissions = {},
            onOpenAccessibilitySettings = {},
            onOpenAppInfo = {},
            onRequestOverlayPermission = {}
        )
    }
}

@Preview(showBackground = true, name = "Transaction Item")
@Composable
private fun TransactionItemPreview() {
    AnyPayTheme(darkTheme = false) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sampleTransactions.forEach { TransactionItem(transaction = it) }
        }
    }
}
