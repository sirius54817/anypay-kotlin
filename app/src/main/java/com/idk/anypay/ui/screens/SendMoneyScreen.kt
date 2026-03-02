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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.idk.anypay.data.model.UpiPaymentInfo
import com.idk.anypay.service.UpiService
import com.idk.anypay.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMoneyScreen(
    operationState: UpiService.OperationState,
    lastUssdMessage: String?,
    initialRecipient: String = "",
    initialAmount: String = "",
    initialRemarks: String = "",
    onSendMoney: (String, Double, String) -> Unit,
    onCancel: () -> Unit,
    onScanQr: () -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onUpdateTransaction: ((com.idk.anypay.data.model.Transaction) -> Unit)? = null
) {
    var recipient by remember { mutableStateOf(initialRecipient) }
    var amount    by remember { mutableStateOf(initialAmount) }
    var remarks   by remember { mutableStateOf(initialRemarks) }

    LaunchedEffect(initialRecipient, initialAmount, initialRemarks) {
        if (initialRecipient.isNotEmpty()) recipient = initialRecipient
        if (initialAmount.isNotEmpty())    amount    = initialAmount
        if (initialRemarks.isNotEmpty())   remarks   = initialRemarks
    }

    var hasUpdatedTransaction by remember { mutableStateOf(false) }

    val isRecipientValid = UpiPaymentInfo.isValidRecipient(recipient)
    val amountValue      = amount.toDoubleOrNull() ?: 0.0
    val isAmountValid    = amountValue > 0 && amountValue <= 100000
    val canSend          = isRecipientValid && isAmountValid && operationState is UpiService.OperationState.Idle
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
                    Text("Send Money", style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isProcessing) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onScanQr, enabled = !isProcessing) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        when {
            isComplete  -> CompletionScreen(operationState = operationState, onDone = { onReset(); onBack() })
            isProcessing -> ProcessingScreen(
                message        = (operationState as? UpiService.OperationState.InProgress)?.message ?: "Processing...",
                lastUssdMessage = lastUssdMessage,
                onCancel       = onCancel,
                modifier       = Modifier.padding(padding)
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
                    // ── Recipient ─────────────────────────────────────────────
                    ShadcnTextField(
                        value           = recipient,
                        onValueChange   = { recipient = it },
                        label           = "UPI ID or Mobile Number",
                        placeholder     = "example@upi or 9876543210",
                        leadingIcon     = { Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp)) },
                        trailingIcon    = {
                            IconButton(onClick = onScanQr) {
                                Icon(Icons.Default.QrCodeScanner, "Scan QR", modifier = Modifier.size(18.dp))
                            }
                        },
                        isError         = recipient.isNotEmpty() && !isRecipientValid,
                        supportingText  = if (recipient.isNotEmpty() && !isRecipientValid)
                            "Enter valid UPI ID (user@provider) or 10-digit mobile" else null
                    )

                    // ── Amount ────────────────────────────────────────────────
                    ShadcnTextField(
                        value          = amount,
                        onValueChange  = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it
                        },
                        label          = "Amount",
                        placeholder    = "0.00",
                        prefix         = "₹ ",
                        leadingIcon    = { Icon(Icons.Default.CurrencyRupee, null, modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError        = amount.isNotEmpty() && !isAmountValid,
                        supportingText = when {
                            amount.isNotEmpty() && amountValue <= 0   -> "Enter a valid amount"
                            amount.isNotEmpty() && amountValue > 100000 -> "Maximum amount is ₹1,00,000"
                            else -> "Maximum: ₹1,00,000 per transaction"
                        }
                    )

                    // ── Remarks ───────────────────────────────────────────────
                    ShadcnTextField(
                        value          = remarks,
                        onValueChange  = { if (it.length <= 50) remarks = it },
                        label          = "Remarks (Optional)",
                        placeholder    = "What's this for?",
                        leadingIcon    = { Icon(Icons.Default.Notes, null, modifier = Modifier.size(18.dp)) },
                        supportingText = "${remarks.length}/50 characters"
                    )

                    // ── Warning notice ────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .border(BorderStroke(1.dp, WarningYellow.copy(alpha = 0.4f)), MaterialTheme.shapes.medium)
                            .background(WarningYellow.copy(alpha = 0.06f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Please verify details",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "USSD transactions cannot be reversed. Double-check the recipient and amount.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // ── Send Button ───────────────────────────────────────────
                    Button(
                        onClick  = { onSendMoney(recipient, amountValue, remarks.ifEmpty { "payment" }) },
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
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
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

// ─── Shared Processing Screen ─────────────────────────────────────────────────

@Composable
fun ProcessingScreen(
    message: String,
    lastUssdMessage: String?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Processing USSD Request",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (lastUssdMessage != null) {
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Text(
                    text = "USSD Response",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = lastUssdMessage.take(200),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        OutlinedButton(
            onClick = onCancel,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cancel")
        }
    }
}

// ─── Shared Completion Screen ─────────────────────────────────────────────────

@Composable
fun CompletionScreen(
    operationState: UpiService.OperationState,
    onDone: () -> Unit
) {
    val isSuccess = operationState is UpiService.OperationState.Success
    val message   = when (operationState) {
        is UpiService.OperationState.Success -> operationState.message
        is UpiService.OperationState.Error   -> operationState.message
        else -> ""
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

        Text(
            text = if (isSuccess) "Payment Successful!" else "Payment Failed",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (!isSuccess && message.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .border(BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)), MaterialTheme.shapes.medium)
                    .background(ErrorRed.copy(alpha = 0.05f))
                    .padding(14.dp)
            ) {
                Text("Error Details", style = MaterialTheme.typography.labelMedium, color = ErrorRed)
                Spacer(Modifier.height(6.dp))
                Text(message.take(200), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface)
            }
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

// ─── shadcn-style text field helper ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShadcnTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    prefix: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value          = value,
        onValueChange  = onValueChange,
        label          = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        placeholder    = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        prefix         = prefix?.let { { Text(it) } },
        leadingIcon    = leadingIcon,
        trailingIcon   = trailingIcon,
        isError        = isError,
        supportingText = supportingText?.let {
            { Text(it, style = MaterialTheme.typography.bodySmall) }
        },
        singleLine     = singleLine,
        keyboardOptions = keyboardOptions,
        shape          = MaterialTheme.shapes.small,
        colors         = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor    = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor  = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedContainerColor   = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Send Money – Light Idle")
@Composable
private fun SendMoneyIdlePreview() {
    AnyPayTheme(darkTheme = false) {
        SendMoneyScreen(
            operationState = UpiService.OperationState.Idle,
            lastUssdMessage = null,
            onSendMoney = { _, _, _ -> },
            onCancel = {}, onScanQr = {}, onBack = {}, onReset = {}
        )
    }
}

@Preview(showBackground = true, name = "Send Money – Dark Filled")
@Composable
private fun SendMoneyFilledDarkPreview() {
    AnyPayTheme(darkTheme = true) {
        SendMoneyScreen(
            operationState  = UpiService.OperationState.Idle,
            lastUssdMessage = null,
            initialRecipient = "john@upi",
            initialAmount    = "500",
            initialRemarks   = "Lunch split",
            onSendMoney = { _, _, _ -> },
            onCancel = {}, onScanQr = {}, onBack = {}, onReset = {}
        )
    }
}

@Preview(showBackground = true, name = "Send Money – Processing")
@Composable
private fun SendMoneyProcessingPreview() {
    AnyPayTheme(darkTheme = false) {
        SendMoneyScreen(
            operationState  = UpiService.OperationState.InProgress("Connecting to bank..."),
            lastUssdMessage = "Enter your UPI PIN",
            onSendMoney = { _, _, _ -> },
            onCancel = {}, onScanQr = {}, onBack = {}, onReset = {}
        )
    }
}

@Preview(showBackground = true, name = "Send Money – Success")
@Composable
private fun SendMoneySuccessPreview() {
    AnyPayTheme(darkTheme = false) {
        SendMoneyScreen(
            operationState = UpiService.OperationState.Success(
                message = "Payment of ₹500.00 sent to john@upi",
                transaction = com.idk.anypay.data.model.Transaction(
                    type = com.idk.anypay.data.model.TransactionType.SEND,
                    amount = 500.0,
                    recipientVpa = "john@upi",
                    status = com.idk.anypay.data.model.TransactionStatus.SUCCESS
                )
            ),
            lastUssdMessage = null,
            onSendMoney = { _, _, _ -> },
            onCancel = {}, onScanQr = {}, onBack = {}, onReset = {}
        )
    }
}

@Preview(showBackground = true, name = "Send Money – Error")
@Composable
private fun SendMoneyErrorPreview() {
    AnyPayTheme(darkTheme = false) {
        SendMoneyScreen(
            operationState = UpiService.OperationState.Error("Insufficient balance or timeout."),
            lastUssdMessage = null,
            onSendMoney = { _, _, _ -> },
            onCancel = {}, onScanQr = {}, onBack = {}, onReset = {}
        )
    }
}
