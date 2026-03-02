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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.idk.anypay.data.model.SUPPORTED_BANKS
import com.idk.anypay.data.model.UserCredentials
import com.idk.anypay.ui.theme.*

private val STEP_TITLES = listOf(
    "Mobile Number",
    "Set UPI PIN",
    "Bank Details",
    "Debit Card"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onComplete: (UserCredentials) -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }

    var mobileNumber       by remember { mutableStateOf("") }
    var upiPin             by remember { mutableStateOf("") }
    var confirmPin         by remember { mutableStateOf("") }
    var selectedBankIndex  by remember { mutableIntStateOf(-1) }
    var bankName           by remember { mutableStateOf("") }
    var bankIfsc           by remember { mutableStateOf("") }
    var cardLastSix        by remember { mutableStateOf("") }
    var cardExpiryMonth    by remember { mutableStateOf("") }
    var cardExpiryYear     by remember { mutableStateOf("") }
    var showPin            by remember { mutableStateOf(false) }
    var showConfirmPin     by remember { mutableStateOf(false) }
    var bankDropdownExpanded by remember { mutableStateOf(false) }

    val isMobileValid  = mobileNumber.length == 10 && mobileNumber.firstOrNull()?.let { it in '6'..'9' } == true
    val isPinValid     = upiPin.length in 4..6 && upiPin == confirmPin
    val isBankValid    = bankName.isNotBlank()
    val isCardValid    = cardLastSix.length == 6 && cardExpiryMonth.length == 2 && cardExpiryYear.length == 2
    val canProceed     = when (currentStep) { 0 -> isMobileValid; 1 -> isPinValid; 2 -> isBankValid; 3 -> isCardValid; else -> false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Up AnyPay", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Step indicator ────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text  = "Step ${currentStep + 1} of 4  ·  ${STEP_TITLES[currentStep]}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text  = "${((currentStep + 1) * 25)}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress    = { (currentStep + 1) / 4f },
                    modifier    = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(MaterialTheme.shapes.extraSmall),
                    color       = MaterialTheme.colorScheme.primary,
                    trackColor  = MaterialTheme.colorScheme.outline
                )
            }

            // ── Step content ──────────────────────────────────────────────
            when (currentStep) {
                0 -> MobileNumberStep(
                    mobileNumber  = mobileNumber,
                    onMobileChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) mobileNumber = it },
                    isValid = isMobileValid
                )
                1 -> PinSetupStep(
                    upiPin = upiPin, confirmPin = confirmPin,
                    showPin = showPin, showConfirmPin = showConfirmPin,
                    onPinChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) upiPin = it },
                    onConfirmPinChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                    onToggleShowPin = { showPin = !showPin },
                    onToggleShowConfirmPin = { showConfirmPin = !showConfirmPin },
                    isPinValid = upiPin.length in 4..6,
                    pinsMatch  = upiPin == confirmPin && confirmPin.isNotEmpty()
                )
                2 -> BankSelectionStep(
                    selectedBankIndex = selectedBankIndex,
                    bankName = bankName, bankIfsc = bankIfsc,
                    expanded = bankDropdownExpanded,
                    onExpandedChange = { bankDropdownExpanded = it },
                    onBankSelect = { index ->
                        selectedBankIndex = index
                        if (index >= 0) { bankName = SUPPORTED_BANKS[index].name; bankIfsc = SUPPORTED_BANKS[index].ifscPrefix + "0000000" }
                        bankDropdownExpanded = false
                    },
                    onBankNameChange = { bankName = it },
                    onIfscChange = { if (it.length <= 11) bankIfsc = it.uppercase() },
                    isBankValid = bankName.isNotBlank()
                )
                3 -> CardDetailsStep(
                    cardLastSix = cardLastSix, cardExpiryMonth = cardExpiryMonth, cardExpiryYear = cardExpiryYear,
                    onCardChange  = { if (it.length <= 6  && it.all { c -> c.isDigit() }) cardLastSix = it },
                    onMonthChange = { if (it.length <= 2  && it.all { c -> c.isDigit() }) cardExpiryMonth = it },
                    onYearChange  = { if (it.length <= 2  && it.all { c -> c.isDigit() }) cardExpiryYear  = it },
                    isCardValid   = cardLastSix.length == 6,
                    isExpiryValid = cardExpiryMonth.length == 2 && cardExpiryYear.length == 2
                )
            }

            Spacer(Modifier.weight(1f))

            // ── Navigation buttons ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick  = { currentStep-- },
                        modifier = Modifier.weight(1f),
                        shape    = MaterialTheme.shapes.small,
                        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Back")
                    }
                }
                Button(
                    onClick  = {
                        if (currentStep < 3) {
                            currentStep++
                        } else {
                            onComplete(
                                UserCredentials(
                                    mobileNumber      = mobileNumber,
                                    upiPin            = upiPin,
                                    bankName          = bankName,
                                    bankIfsc          = bankIfsc,
                                    cardLastSixDigits = cardLastSix,
                                    cardExpiryMonth   = cardExpiryMonth,
                                    cardExpiryYear    = cardExpiryYear,
                                    isSetupComplete   = true
                                )
                            )
                        }
                    },
                    enabled  = canProceed,
                    modifier = Modifier.weight(1f),
                    shape    = MaterialTheme.shapes.small
                ) {
                    Text(if (currentStep < 3) "Continue" else "Complete Setup")
                    if (currentStep < 3) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ─── Step: Mobile Number ──────────────────────────────────────────────────────

@Composable
private fun MobileNumberStep(
    mobileNumber: String,
    onMobileChange: (String) -> Unit,
    isValid: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepHeader(title = "Enter Your Mobile Number",
            subtitle = "This should be the mobile number linked to your bank account.")

        OutlinedTextField(
            value           = mobileNumber,
            onValueChange   = onMobileChange,
            label           = { Text("Mobile Number") },
            placeholder     = { Text("9876543210") },
            prefix          = { Text("+91 ") },
            leadingIcon     = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine      = true,
            isError         = mobileNumber.isNotEmpty() && !isValid,
            supportingText  = if (mobileNumber.isNotEmpty() && !isValid)
                { { Text("Enter a valid 10-digit mobile number starting with 6-9") } } else null,
            shape    = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Step: PIN Setup ──────────────────────────────────────────────────────────

@Composable
private fun PinSetupStep(
    upiPin: String, confirmPin: String,
    showPin: Boolean, showConfirmPin: Boolean,
    onPinChange: (String) -> Unit, onConfirmPinChange: (String) -> Unit,
    onToggleShowPin: () -> Unit, onToggleShowConfirmPin: () -> Unit,
    isPinValid: Boolean, pinsMatch: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepHeader(title = "Set Your UPI PIN",
            subtitle = "Create a 4–6 digit UPI PIN for transactions. Keep it secret!")

        OutlinedTextField(
            value                = upiPin,
            onValueChange        = onPinChange,
            label                = { Text("UPI PIN") },
            leadingIcon          = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp)) },
            trailingIcon         = {
                IconButton(onClick = onToggleShowPin) {
                    Icon(if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            },
            visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine           = true,
            isError              = upiPin.isNotEmpty() && !isPinValid,
            supportingText       = if (upiPin.isNotEmpty() && !isPinValid) { { Text("PIN must be 4–6 digits") } } else null,
            shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value                = confirmPin,
            onValueChange        = onConfirmPinChange,
            label                = { Text("Confirm UPI PIN") },
            leadingIcon          = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp)) },
            trailingIcon         = {
                IconButton(onClick = onToggleShowConfirmPin) {
                    Icon(if (showConfirmPin) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            },
            visualTransformation = if (showConfirmPin) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine           = true,
            isError              = confirmPin.isNotEmpty() && !pinsMatch,
            supportingText       = if (confirmPin.isNotEmpty() && !pinsMatch) { { Text("PINs do not match") } } else null,
            shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Step: Bank ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankSelectionStep(
    selectedBankIndex: Int, bankName: String, bankIfsc: String,
    expanded: Boolean, onExpandedChange: (Boolean) -> Unit,
    onBankSelect: (Int) -> Unit, onBankNameChange: (String) -> Unit,
    onIfscChange: (String) -> Unit, isBankValid: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepHeader(title = "Enter Your Bank Details",
            subtitle = "Select your bank or type its name.")

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
            OutlinedTextField(
                value       = bankName,
                onValueChange = onBankNameChange,
                label       = { Text("Bank Name") },
                placeholder = { Text("State Bank of India") },
                leadingIcon = { Icon(Icons.Default.AccountBalance, null, modifier = Modifier.size(18.dp)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                supportingText = { Text("Type or select your bank") },
                shape    = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                SUPPORTED_BANKS.forEachIndexed { index, bank ->
                    DropdownMenuItem(
                        text    = { Text(bank.name, style = MaterialTheme.typography.bodyMedium) },
                        onClick = { onBankSelect(index) }
                    )
                }
            }
        }
        OutlinedTextField(
            value           = bankIfsc,
            onValueChange   = onIfscChange,
            label           = { Text("IFSC Code (Optional)") },
            placeholder     = { Text("SBIN0001234") },
            leadingIcon     = { Icon(Icons.Default.Numbers, null, modifier = Modifier.size(18.dp)) },
            singleLine      = true,
            isError         = bankIfsc.isNotEmpty() && bankIfsc.length != 11,
            supportingText  = {
                if (bankIfsc.isNotEmpty() && bankIfsc.length != 11)
                    Text("IFSC code must be 11 characters")
                else
                    Text("Found on your cheque book or bank statement")
            },
            shape    = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Step: Card ───────────────────────────────────────────────────────────────

@Composable
private fun CardDetailsStep(
    cardLastSix: String, cardExpiryMonth: String, cardExpiryYear: String,
    onCardChange: (String) -> Unit, onMonthChange: (String) -> Unit, onYearChange: (String) -> Unit,
    isCardValid: Boolean, isExpiryValid: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StepHeader(title = "Debit Card Details",
            subtitle = "Enter the last 6 digits and expiry date for verification.")

        OutlinedTextField(
            value           = cardLastSix,
            onValueChange   = onCardChange,
            label           = { Text("Last 6 Digits") },
            placeholder     = { Text("123456") },
            leadingIcon     = { Icon(Icons.Default.CreditCard, null, modifier = Modifier.size(18.dp)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine      = true,
            isError         = cardLastSix.isNotEmpty() && !isCardValid,
            supportingText  = if (cardLastSix.isNotEmpty() && !isCardValid) { { Text("Enter exactly 6 digits") } } else null,
            shape    = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text  = "Expiry Date",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = cardExpiryMonth, onValueChange = onMonthChange,
                label = { Text("MM") }, placeholder = { Text("01") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = cardExpiryYear, onValueChange = onYearChange,
                label = { Text("YY") }, placeholder = { Text("28") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f)
            )
        }

        // Security note
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Security, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text  = "Your data is encrypted and stored securely on your device. We never send it to any server.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Step Header ─────────────────────────────────────────────────────────────

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground)
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Onboarding – Light Step 1")
@Composable
private fun OnboardingStep1Preview() {
    AnyPayTheme(darkTheme = false) { OnboardingScreen(onComplete = {}) }
}

@Preview(showBackground = true, name = "Onboarding – Dark Step 1")
@Composable
private fun OnboardingStep1DarkPreview() {
    AnyPayTheme(darkTheme = true) { OnboardingScreen(onComplete = {}) }
}
