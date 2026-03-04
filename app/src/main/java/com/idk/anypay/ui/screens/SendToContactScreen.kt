package com.idk.anypay.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.idk.anypay.data.model.UpiPaymentInfo
import com.idk.anypay.service.UpiService
import com.idk.anypay.ui.theme.*

/**
 * Pay-to-contact screen — a clone of SendMoneyScreen that only accepts
 * a 10-digit mobile number (no UPI ID, no QR scan).
 * The payment flow is identical: it calls the same onSendMoney callback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendToContactScreen(
    operationState: UpiService.OperationState,
    lastUssdMessage: String?,
    initialRecipient: String = "",
    initialName: String = "",
    onSendMoney: (String, Double, String) -> Unit,
    onSendMoneyUpi: (String, Double, String) -> Unit = onSendMoney,
    onCancel: () -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onUpdateTransaction: ((com.idk.anypay.data.model.Transaction) -> Unit)? = null
) {
    var recipient by remember { mutableStateOf(initialRecipient) }
    var amount    by remember { mutableStateOf("") }
    var remarks   by remember { mutableStateOf(initialName) }

    LaunchedEffect(initialRecipient, initialName) {
        if (initialRecipient.isNotEmpty()) recipient = initialRecipient
        if (initialName.isNotEmpty())      remarks   = initialName
    }

    var hasUpdatedTransaction by remember { mutableStateOf(false) }

    // Detect recipient type
    val isUpiId          = recipient.contains("@")
    val isMobileNumber   = recipient.matches(Regex("^\\d{10}$"))
    val isRecipientValid = isUpiId || isMobileNumber
    val amountValue      = amount.toDoubleOrNull() ?: 0.0
    val isAmountValid    = amountValue > 0 && amountValue <= 100000
    val canSend          = isRecipientValid && isAmountValid &&
                           operationState is UpiService.OperationState.Idle
    val isProcessing     = operationState is UpiService.OperationState.InProgress
    val isComplete       = operationState is UpiService.OperationState.Success ||
                           operationState is UpiService.OperationState.Error

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
                title = {
                    Text(
                        if (initialName.isNotEmpty()) "Pay to $initialName"
                        else "Pay to Contact",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isProcessing) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        when {
            isComplete   -> CompletionScreen(
                operationState = operationState,
                onDone = { onReset(); onBack() }
            )
            isProcessing -> ProcessingScreen(
                message         = (operationState as? UpiService.OperationState.InProgress)?.message
                    ?: "Processing...",
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
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Recipient (Phone Number or UPI ID) ─────────────
                    OutlinedTextField(
                        value         = recipient,
                        onValueChange = { recipient = it },
                        label     = { Text("Phone Number or UPI ID", style = MaterialTheme.typography.bodyMedium) },
                        placeholder = { Text("9876543210 or user@upi", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = {
                            Icon(
                                if (isUpiId) Icons.Default.AlternateEmail
                                else Icons.Default.Phone,
                                null, modifier = Modifier.size(18.dp)
                            )
                        },
                        isError     = recipient.isNotEmpty() && !isRecipientValid,
                        supportingText = if (recipient.isNotEmpty() && !isRecipientValid) {
                            { Text("Enter a 10-digit mobile number or UPI ID (user@provider)",
                                style = MaterialTheme.typography.bodySmall) }
                        } else if (recipient.isNotEmpty() && isRecipientValid) {
                            { Text(
                                if (isUpiId) "✓ UPI ID"
                                else "✓ Mobile number",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            ) }
                        } else null,
                        singleLine      = true,
                        keyboardOptions = KeyboardOptions.Default,
                        shape  = MaterialTheme.shapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor   = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── Amount ────────────────────────────────────────────
                    OutlinedTextField(
                        value         = amount,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it
                        },
                        label     = { Text("Amount", style = MaterialTheme.typography.bodyMedium) },
                        placeholder = { Text("0.00", style = MaterialTheme.typography.bodyMedium) },
                        prefix    = { Text("₹ ") },
                        leadingIcon = { Icon(Icons.Default.CurrencyRupee, null, modifier = Modifier.size(18.dp)) },
                        isError     = amount.isNotEmpty() && !isAmountValid,
                        supportingText = {
                            Text(
                                when {
                                    amount.isNotEmpty() && amountValue <= 0     -> "Enter a valid amount"
                                    amount.isNotEmpty() && amountValue > 100000 -> "Maximum amount is ₹1,00,000"
                                    else -> "Maximum: ₹1,00,000 per transaction"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine      = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape  = MaterialTheme.shapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor   = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── Remarks ───────────────────────────────────────────
                    OutlinedTextField(
                        value         = remarks,
                        onValueChange = { if (it.length <= 50) remarks = it },
                        label     = { Text("Remarks (Optional)", style = MaterialTheme.typography.bodyMedium) },
                        placeholder = { Text("What's this for?", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Default.Notes, null, modifier = Modifier.size(18.dp)) },
                        supportingText = {
                            Text("${remarks.length}/50 characters",
                                style = MaterialTheme.typography.bodySmall)
                        },
                        singleLine = true,
                        shape  = MaterialTheme.shapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor   = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── Warning ───────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .border(
                                BorderStroke(1.dp, WarningYellow.copy(alpha = 0.4f)),
                                MaterialTheme.shapes.medium
                            )
                            .background(WarningYellow.copy(alpha = 0.06f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Warning, null, tint = WarningYellow,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Please verify details",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(2.dp))
                            Text("USSD transactions cannot be reversed. Double-check the number and amount.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // ── Send Button ───────────────────────────────────────
                    Button(
                        onClick  = {
                            val r = remarks.ifEmpty { "payment" }
                            if (isUpiId) {
                                // UPI ID → use SEND_MONEY flow (UPI ID path)
                                onSendMoneyUpi(recipient, amountValue, r)
                            } else {
                                // 10-digit number → use SEND_MONEY_MOBILE flow (Mobile No path)
                                onSendMoney(recipient, amountValue, r)
                            }
                        },
                        enabled  = canSend,
                        modifier = Modifier.fillMaxWidth(),
                        shape    = MaterialTheme.shapes.small,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = MaterialTheme.colorScheme.primary,
                            contentColor           = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            disabledContentColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    ) {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Send ₹${if (amountValue > 0) String.format("%,.2f", amountValue) else "0.00"}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}





