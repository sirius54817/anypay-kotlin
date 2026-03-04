# AnyPay Architecture & Flow Reference

> **Baseline commit:** `09d50d5` — "stable" (2026-03-03)
> **DO NOT modify** `UssdAccessibilityService.kt`, `UpiService.kt`, `UssdOverlayService.kt`, `SendMoneyScreen.kt`, or `CheckBalanceScreen.kt` timing/logic unless specifically fixing a proven bug.

---

## 1. Project Structure

```
com.idk.anypay/
├── MainActivity.kt                      # Entry point, permission launchers, AppContent
├── data/
│   ├── model/
│   │   └── Transaction.kt              # Transaction, UpiPaymentInfo, enums
│   └── repository/
│       └── SecureStorageRepository.kt   # Encrypted local storage
├── service/
│   ├── UpiService.kt                   # High-level UPI operations (check balance, send money)
│   ├── UssdAccessibilityService.kt     # Accessibility service — USSD dialog automation
│   └── UssdOverlayService.kt           # Full-screen overlay hiding USSD dialogs
└── ui/
    ├── navigation/
    │   └── MainNavigation.kt           # NavHost, routes, bottom nav, screen wiring
    ├── screens/
    │   ├── HomeScreen.kt               # Home — quick actions, recent transactions
    │   ├── SendMoneyScreen.kt          # Send money form + processing + completion
    │   ├── CheckBalanceScreen.kt       # Balance check + processing + result
    │   ├── QrScannerScreen.kt          # Camera-based QR scanner (ML Kit)
    │   ├── HistoryScreen.kt            # Transaction history
    │   ├── SettingsScreen.kt           # App settings
    │   ├── OnboardingScreen.kt         # First-time setup
    │   └── LockScreen.kt              # Biometric lock
    ├── theme/
    │   └── Theme.kt, Color.kt, Type.kt, Shape.kt
    └── viewmodel/
        └── AppViewModel.kt             # Central ViewModel
```

---

## 2. Payment Flow (CRITICAL — DO NOT BREAK)

### 2.1 Send Money Flow

```
User taps "Send Money" on HomeScreen
  → navigates to SendMoneyScreen (route: "send_money")
  → user fills: recipient (UPI ID / mobile), amount, remarks
  → taps "Send ₹X"
  → MainNavigation calls: onSendMoney(recipient, amount, remarks)
  → AppViewModel.sendMoney(recipient, amount, remarks)
  → UpiService.sendMoney(credentials, recipient, amount, remarks)
      1. Creates Transaction(PENDING)
      2. Sets operationState = InProgress
      3. Starts UssdOverlayService (full-screen cover)
      4. Calls UssdAccessibilityService.startSendMoney(...)
      5. Dials *99# via TelephonyManager.sendUssdRequest()
  → UssdAccessibilityService handles USSD dialog automation:
      (see Section 3 for timing details)
  → On completion: onOperationComplete callback fires
  → UpiService sets operationState = Success/Error
  → SendMoneyScreen shows CompletionScreen
```

### 2.2 Check Balance Flow

```
User taps "Check Balance" on HomeScreen
  → navigates to CheckBalanceScreen (route: "check_balance")
  → taps "Check Balance Now"
  → AppViewModel.checkBalance()
  → UpiService.checkBalance(credentials)
      1. Creates Transaction(BALANCE_CHECK, PENDING)
      2. Sets operationState = InProgress
      3. Starts UssdOverlayService
      4. Calls UssdAccessibilityService.startCheckBalance(...)
      5. Dials *99#
  → UssdAccessibilityService handles USSD automation
  → On completion → BalanceResultScreen
```

### 2.3 QR Scan → Send Money Flow

```
User taps "Scan to Pay" on HomeScreen
  → checks camera permission (requests if needed)
  → navigates to QrScannerScreen (route: "qr_scanner")
  → camera scans QR code → ML Kit decodes → UpiPaymentInfo.parse()
  → onScanResult callback fires in MainNavigation:
      pendingRecipient = paymentInfo.upiId
      pendingAmount    = paymentInfo.amount
      pendingRemarks   = paymentInfo.note or paymentInfo.name
  → navigates to SendMoneyScreen with pre-filled data
  → normal send money flow continues from there
```

---

## 3. USSD Accessibility Service — Timing & Delays (CRITICAL)

These timing constants are **tuned for real-device USSD reliability**. Changing them breaks payments.

| Constant                  | Value   | Purpose                                                    |
|---------------------------|---------|------------------------------------------------------------|
| `DIALOG_STABILIZE_MS`    | 200ms   | Wait after dialog change before reading content            |
| `EVENT_DEBOUNCE_MS`      | 100ms   | Ignore rapid-fire accessibility events                     |
| `TEXT_INJECTION_DELAY_MS` | 300ms   | Wait after typing text before clicking Send                |
| `POST_SEND_COOLDOWN_MS`  | 300ms   | Wait after clicking Send before processing next dialog     |
| `MIN_SEND_INTERVAL_MS`   | 300ms   | Minimum gap between two send actions                       |

### Dialog Processing Pipeline

```
AccessibilityEvent received
  → debounce (100ms)
  → read rootInActiveWindow
  → extract USSD message text
  → filter non-USSD content (contacts, dialer UI)
  → check if content is likely USSD (ussdIndicators list)
  → hash comparison (skip if same dialog already seen)
  → NEW dialog detected:
      → invoke onUssdResponse callback (UpiService updates overlay)
      → check isErrorMessage() → if error, abort immediately
  → schedule stabilization job (200ms delay):
      → processStabilizedDialog():
          → guard: already processing? skip
          → guard: already responded to this hash? skip
          → guard: too soon since last send? wait and retry
          → check isOperationComplete() → if complete, fire callback + dismiss
          → determineResponse() → returns text to send or null
          → if response found:
              → sendResponseWithTiming():
                  → find input field
                  → focus if needed (200ms delay)
                  → inject text via ACTION_SET_TEXT
                  → wait TEXT_INJECTION_DELAY_MS (300ms)
                  → click Send button
                  → wait POST_SEND_COOLDOWN_MS (300ms)
                  → release processing lock
```

### Send Money USSD Steps (determineResponse → handleSendMoney)

```
Step 0: Remarks prompt — checked FIRST (before hasNumberedOptions branching)
        If numbered menu → selects "Skip" option or sends "1"
        If free-text → sends remarks text, or "1" to skip if blank
Step 1: Select "Send Money" from numbered menu (e.g., "1")
Step 2: Select payment method — "UPI ID" or "Mobile No" sub-menu
Step 3: Enter recipient (UPI ID or mobile number)
Step 4: Enter amount
Step 5: Enter UPI PIN (when prompted)
→ Success/Error message → operation complete
```

### Check Balance USSD Steps

```
Step 1: Select "Check Balance" from numbered menu (e.g., "4")
Step 2: Enter UPI PIN (when prompted)
→ Balance displayed → operation complete
```

---

## 4. Error Detection Keywords

`isErrorMessage()` matches these in the USSD response (lowercase):
- incorrect, invalid, failed, declined, not registered
- connection problem, try again, unable to, could not, cannot
- blocked, expired, insufficient, invalid mmi
- payment address incorrect, beneficiary + incorrect

`isSuccessMessage()` matches:
- success, completed, balance is, available balance
- rs + balance (without "insufficient")

---

## 5. Overlay Service

- `UssdOverlayService.start(context, msg)` — shows full-screen overlay
- `UssdOverlayService.updateMessage(msg)` — updates text on overlay
- `UssdOverlayService.stop(context)` — removes overlay
- Uses `SYSTEM_ALERT_WINDOW` permission
- Compose-based UI with animation

---

## 6. Navigation Routes

| Route            | Screen               | Parameters                              |
|------------------|----------------------|-----------------------------------------|
| `home`           | HomeScreen           | —                                       |
| `history`        | HistoryScreen        | —                                       |
| `settings`       | SettingsScreen       | —                                       |
| `send_money`     | SendMoneyScreen      | uses pendingRecipient/Amount/Remarks    |
| `check_balance`  | CheckBalanceScreen   | —                                       |
| `qr_scanner`     | QrScannerScreen      | onScanResult → sets pending* → nav to send_money |

---

## 7. Key Data Models

### Transaction
- `type`: SEND, RECEIVE, BALANCE_CHECK
- `status`: PENDING, SUCCESS, FAILED
- `amount`, `recipientVpa`, `recipientName`, `balance`, `referenceId`, `message`
- `category`: auto-detected from message (Food, Transport, Shopping, etc.)

### UpiPaymentInfo
- Parsed from QR code URL: `upi://pay?pa=<ID>&pn=<NAME>&am=<AMOUNT>&tn=<NOTE>`
- `parse(url)` → `UpiPaymentInfo?`
- `isValidUpiId(id)`, `isValidMobileNumber(mobile)`, `isValidRecipient(input)`

### UssdOperation
- Tracks current USSD automation state
- Flags: `selectedMenuOption`, `selectedPaymentMethod`, `sentRecipient`, `sentAmount`, `sentPin`, `sentBank`, `sentCard`, `sentRemarks`
- `step` counter for progress tracking

---

## 8. Permission Model

| Permission               | Purpose                        | Launcher                        |
|--------------------------|--------------------------------|---------------------------------|
| `CALL_PHONE`            | Dial *99# USSD                 | phonePermissionLauncher         |
| `READ_PHONE_STATE`      | USSD session management        | phonePermissionLauncher         |
| `CAMERA`                | QR code scanning               | cameraPermissionLauncher        |
| `SYSTEM_ALERT_WINDOW`   | Overlay to hide USSD dialogs   | Settings intent                 |
| Accessibility Service   | Read/interact with USSD dialog | Accessibility Settings intent   |

---

## 9. Rules for Safe Modifications

1. **NEVER change timing constants** in UssdAccessibilityService without real-device testing
2. **NEVER modify** `isErrorMessage()`, `isSuccessMessage()`, `determineResponse()`, `handleSendMoney()`, `handleCheckBalance()` unless fixing a proven bug with logs
3. **NEVER change** the callback chain: `onUssdResponse` → `onOperationComplete` → `handleOperationComplete`
4. **UI-only changes** (HomeScreen, navigation, new screens) are safe as long as:
   - You don't change `onSendMoney(recipient, amount, remarks)` signature
   - You don't change `onCheckBalance()` signature
   - You don't change how `QrScannerScreen.onScanResult` sets `pendingRecipient/Amount/Remarks`
   - You keep the same `SendMoneyScreen` and `CheckBalanceScreen` parameters
5. **Adding new buttons/screens**: just add routes in `MainNavigation.kt`, pass callbacks through, navigate — the payment backend doesn't care about UI layout


