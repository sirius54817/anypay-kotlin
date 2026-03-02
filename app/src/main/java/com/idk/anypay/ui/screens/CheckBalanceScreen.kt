package com.idk.anypay.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.idk.anypay.service.UpiService
import com.idk.anypay.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckBalanceScreen(
    operationState: UpiService.OperationState,
    lastUssdMessage: String?,
    lastBalance: Double,
    onCheckBalance: () -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onUpdateTransaction: ((com.idk.anypay.data.model.Transaction) -> Unit)? = null
) {
    val isProcessing = operationState is UpiService.OperationState.InProgress
    val isComplete   = operationState is UpiService.OperationState.Success ||
                       operationState is UpiService.OperationState.Error

    var hasUpdatedTransaction by remember { mutableStateOf(false) }

    LaunchedEffect(isComplete) {
        if (isComplete && !hasUpdatedTransaction) {
            if (operationState is UpiService.OperationState.Success) {
                operationState.transaction?.let {
                    onUpdateTransaction?.invoke(it)
                    hasUpdatedTransaction = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check Balance", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isProcessing) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = MaterialTheme.colorScheme.background,
                    titleContentColor          = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        when {
            isComplete -> BalanceResultScreen(
                operationState = operationState,
                onDone = { onReset(); onBack() }
            )
            isProcessing -> ProcessingScreen(
                message         = (operationState as? UpiService.OperationState.InProgress)?.message ?: "Processing...",
                lastUssdMessage = lastUssdMessage,
                onCancel        = onCancel,
                modifier        = Modifier.padding(padding)
            )
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // ── Last known balance ────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Last Known Balance",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "₹${String.format("%,.2f", lastBalance)}",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── How it works ──────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "How it works",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "• Dials *99# USSD code\n" +
                                   "• Selects balance enquiry option\n" +
                                   "• You enter your UPI PIN when prompted\n" +
                                   "• Balance will be displayed and saved",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    Button(
                        onClick  = onCheckBalance,
                        modifier = Modifier.fillMaxWidth(),
                        shape    = MaterialTheme.shapes.small,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor   = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Check Balance Now")
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Balance check is free and does not cost any money",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Balance Result ───────────────────────────────────────────────────────────

@Composable
private fun BalanceResultScreen(
    operationState: UpiService.OperationState,
    onDone: () -> Unit
) {
    val isSuccess   = operationState is UpiService.OperationState.Success
    val balance     = remember(operationState) {
        (operationState as? UpiService.OperationState.Success)?.transaction?.balance
    }
    val accentColor = if (isSuccess) SuccessGreen else ErrorRed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(MaterialTheme.shapes.large)
                .background(accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        if (isSuccess) {
            if (balance != null) {
                Text(
                    text = "Your Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "₹${String.format("%,.2f", balance)}",
                    style = MaterialTheme.typography.displayMedium,
                    color = SuccessGreen
                )
            } else {
                Text(
                    text = "Balance Check Complete",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Please check your SMS for balance details",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "Failed to Check Balance",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Please try again later",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick  = onDone,
            modifier = Modifier.fillMaxWidth(),
            shape    = MaterialTheme.shapes.small
        ) {
            Text("Done")
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Check Balance – Light Idle")
@Composable
private fun CheckBalanceIdlePreview() {
    AnyPayTheme(darkTheme = false) {
        CheckBalanceScreen(
            operationState = UpiService.OperationState.Idle,
            lastUssdMessage = null,
            lastBalance = 12345.67,
            onCheckBalance = {}, onCancel = {}, onBack = {}, onReset = {}
        )
    }
}

@Preview(showBackground = true, name = "Check Balance – Dark Idle")
@Composable
private fun CheckBalanceIdleDarkPreview() {
    AnyPayTheme(darkTheme = true) {
        CheckBalanceScreen(
            operationState = UpiService.OperationState.Idle,
            lastUssdMessage = null,
            lastBalance = 12345.67,
            onCheckBalance = {}, onCancel = {}, onBack = {}, onReset = {}
        )
    }
}

@Preview(showBackground = true, name = "Check Balance – Processing")
@Composable
private fun CheckBalanceProcessingPreview() {
    AnyPayTheme(darkTheme = false) {
        CheckBalanceScreen(
            operationState  = UpiService.OperationState.InProgress("Sending USSD request..."),
            lastUssdMessage = "Please wait while we check your balance",
            lastBalance     = 12345.67,
            onCheckBalance  = {}, onCancel = {}, onBack = {}, onReset = {}
        )
    }
}

@Preview(showBackground = true, name = "Check Balance – Success")
@Composable
private fun CheckBalanceSuccessPreview() {
    AnyPayTheme(darkTheme = false) {
        CheckBalanceScreen(
            operationState = UpiService.OperationState.Success(
                message = "Balance check successful",
                transaction = com.idk.anypay.data.model.Transaction(
                    type    = com.idk.anypay.data.model.TransactionType.BALANCE_CHECK,
                    amount  = 0.0,
                    balance = 9876.54,
                    status  = com.idk.anypay.data.model.TransactionStatus.SUCCESS
                )
            ),
            lastUssdMessage = null,
            lastBalance     = 9876.54,
            onCheckBalance  = {}, onCancel = {}, onBack = {}, onReset = {}
        )
    }
}
